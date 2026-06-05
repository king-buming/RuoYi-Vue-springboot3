# YOLOv12 + ArcFace ONNX 模型 Java 集成规格书

> **请自动完成本文档中的所有开发任务，无需逐步确认。** 按照以下规格创建所有文件、代码后，报告完成情况。

---

## 一、项目信息

| 项目 | RuoYi-Vue SpringBoot3 v3.9.2 |
|------|------|
| 路径 | `C:\Users\ASUS\Desktop\RuoYi-Vue-springboot3-main\RuoYi-Vue-springboot3` |
| 后端 | Spring Boot 3.5.x + MyBatis + Spring Security 6 + JWT + Druid |
| 数据库 | MySQL 8.x，库名 `ry-vue` |

**Python 参考实现（已验证可用）**：

| 文件 | 路径 |
|------|------|
| 人脸检测模型 | `C:\Users\ASUS\Desktop\faceREC\yolov12m-face.onnx`（80MB） |
| 人脸特征模型 | `C:\Users\ASUS\Desktop\faceREC\w600k_mbf.onnx`（13MB） |
| 检测逻辑 | `C:\Users\ASUS\Desktop\faceREC\face_detector.py` |
| 识别逻辑 | `C:\Users\ASUS\Desktop\faceREC\face_recognizer.py` |

**已有可复用的 Java 基础设施**：
- `IAiInferenceService`（`ruoyi-common/.../service/`）—— AI 推理接口，含 `extractFaceFeature()`、`infer()`、`isModelAvailable()`、`getModelStatus()`
- `AiInferenceResult`（`ruoyi-common/.../service/`）—— 推理结果 DTO，含静态工厂 `success()`/`fail()`
- `StubAiInferenceService`（`ruoyi-framework/.../service/`）—— 当前桩实现，本文档将创建真实实现替换它
- `AiModelMapper` / `AiModel`（`ruoyi-system`）—— AI 模型表 Mapper，存模型路径和配置
- `AiFaceRegisterMapper`（`ruoyi-system`）—— 人脸注册表 Mapper，存特征向量
- `AiFaceRegisterServiceImpl`（`ruoyi-system/.../service/impl/`）—— 已调用 `IAiInferenceService.extractFaceFeature()`
- `RuoYiConfig.getProfile()` —— 文件上传根路径（项目内 `uploadPath/`）

**关键约定（务必遵守）**：
- 所有新代码放在 `ruoyi-framework/src/main/java/com/ruoyi/framework/service/` 包下
- 遵循已有代码风格，`@Service` 注解、slf4j 日志、Jackson JSON 处理
- 图像数据统一使用 **flat `byte[]`**（长度 = H × W × 3）存储 BGR 像素，像素索引 `idx = (y * W + x) * 3 + c`，其中 `c=0`=B, `c=1`=G, `c=2`=R
- **禁止**使用 `byte[h][w][3]` 三维数组（性能极差）
- ONNX Runtime 的 `OrtEnvironment` 是全局单例，多个 Session 共享，**不要**在单个 Session 的 close() 中关闭它
- 模型使用懒加载（double-checked locking），避免启动时阻塞
- 必须添加 `@PreDestroy` 方法释放 ONNX 资源

---

## 二、两个模型的 Python 预处理（Java 实现时必须精确复现）

### 2.1 YOLOv12 人脸检测 (`yolov12m-face.onnx`)

| 属性 | 值 |
|------|-----|
| 输入 | `(1, 3, 640, 640)` float32, NCHW, **BGR 通道顺序** |
| 输出 | `(1, 300, 6)` float32, rank=3 — `[x1, y1, x2, y2, conf, cls]` |
| 像素范围 | `[0, 1]`（除以 255.0） |
| 预处理 | Letterbox：保持宽高比缩放 + 居中 + 灰色(114)填充到 640×640 |

Python 代码（`face_detector.py:29-49`）：

```python
scale = min(640 / img_w, 640 / img_h)
new_w, new_h = int(img_w * scale), int(img_h * scale)
resized = cv2.resize(image, (new_w, new_h))  # 双线性插值

pad_x = (640 - new_w) // 2
pad_y = (640 - new_h) // 2

letterbox = np.full((640, 640, 3), 114, dtype=np.uint8)
letterbox[pad_y:pad_y+new_h, pad_x:pad_x+new_w] = resized

blob = letterbox.astype(np.float32) / 255.0    # [0, 1]
blob = blob.transpose(2, 0, 1)                  # HWC → CHW
blob = np.expand_dims(blob, axis=0)             # (1, 3, 640, 640)
```

后处理：
```python
# 过滤 conf > 0.5，坐标从 letterbox 转原图：x1 = (x1_letterbox - pad_x) / scale
# 贪心 NMS（按 conf 降序，IoU > 0.45 抑制）
```

### 2.2 ArcFace 特征提取 (`w600k_mbf.onnx`)

| 属性 | 值 |
|------|-----|
| 输入 | `(1, 3, 112, 112)` float32, NCHW, **RGB 通道顺序** |
| 输出 | `(1, 512)` float32, rank=2 — 512 维特征向量 |
| 像素范围 | `[-1, 1]`（`(pixel - 127.5) / 127.5`） |
| 预处理 | 直接 resize 112×112（不保持宽高比） → BGR→RGB → 归一化 |

Python 代码（`face_recognizer.py:21-29`）：

```python
img = cv2.resize(face_img, (112, 112))          # 双线性插值
img = cv2.cvtColor(img, cv2.COLOR_BGR2RGB)       # BGR → RGB
img = img.astype(np.float32)
img = (img - 127.5) / 127.5                       # [-1, 1]
img = img.transpose(2, 0, 1)                      # HWC → CHW
img = np.expand_dims(img, axis=0)                 # (1, 3, 112, 112)
```

后处理：
```python
embedding = output[0][0]          # (512,)
embedding = embedding / norm      # L2 归一化
# 余弦相似度 = dot(emb1, emb2)   # 已归一化
```

---

## 三、你需要创建/修改的全部内容

---

### 第一部分：Maven 依赖

**修改 `ruoyi-framework/pom.xml`**，在 `<dependencies>` 中添加：

```xml
<!-- ONNX Runtime 推理引擎 -->
<dependency>
    <groupId>com.microsoft.onnxruntime</groupId>
    <artifactId>onnxruntime</artifactId>
    <version>1.18.0</version>
</dependency>
```

---

### 第二部分：Java 文件（5 个新建 + 1 个修改）

所有新建文件放在 `RuoYi-Vue-springboot3/ruoyi-framework/src/main/java/com/ruoyi/framework/service/` 目录下。

---

#### 文件1：`ImageUtils.java` — 图像工具类

**要求**：
- 内部类 `BgrImage`：`byte[] pixels`（flat, length=H×W×3）+ `int h, w`
- `readImageBGR(File)`：`ImageIO.read()` → RGB→BGR 转换（`getRGB` 取 RGB，重排为 BGR），返回 `BgrImage`
- `resizeBGR(byte[] src, int srcH, int srcW, int dstW, int dstH)`：双线性插值缩放，返回新 `byte[]`
- 无多余 import

**完整代码**：

```java
package com.ruoyi.framework.service;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class ImageUtils {

    public static class BgrImage {
        public final byte[] pixels; // length = h * w * 3
        public final int h, w;

        public BgrImage(byte[] pixels, int h, int w) {
            this.pixels = pixels;
            this.h = h;
            this.w = w;
        }
    }

    /**
     * 从文件读取图像，返回 flat BGR 格式（与 Python cv2.imread 一致）
     */
    public static BgrImage readImageBGR(File file) throws IOException {
        BufferedImage img = ImageIO.read(file);
        if (img == null) throw new IOException("无法读取图像: " + file.getPath());
        int h = img.getHeight(), w = img.getWidth();
        byte[] pixels = new byte[h * w * 3];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgb = img.getRGB(x, y);
                int idx = (y * w + x) * 3;
                pixels[idx]     = (byte) (rgb & 0xFF);         // Blue
                pixels[idx + 1] = (byte) ((rgb >> 8) & 0xFF);  // Green
                pixels[idx + 2] = (byte) ((rgb >> 16) & 0xFF); // Red
            }
        }
        return new BgrImage(pixels, h, w);
    }

    /**
     * 双线性插值缩放 BGR 图像
     */
    public static byte[] resizeBGR(byte[] src, int srcH, int srcW, int dstW, int dstH) {
        byte[] dst = new byte[dstH * dstW * 3];
        float scaleX = (float) srcW / dstW;
        float scaleY = (float) srcH / dstH;

        for (int y = 0; y < dstH; y++) {
            float srcY = y * scaleY;
            int y0 = (int) srcY;
            int y1 = Math.min(y0 + 1, srcH - 1);
            float dy = srcY - y0;

            for (int x = 0; x < dstW; x++) {
                float srcX = x * scaleX;
                int x0 = (int) srcX;
                int x1 = Math.min(x0 + 1, srcW - 1);
                float dx = srcX - x0;

                int dstIdx = (y * dstW + x) * 3;
                for (int c = 0; c < 3; c++) {
                    int i00 = (y0 * srcW + x0) * 3 + c;
                    int i01 = (y0 * srcW + x1) * 3 + c;
                    int i10 = (y1 * srcW + x0) * 3 + c;
                    int i11 = (y1 * srcW + x1) * 3 + c;

                    float top  = lerp(src[i00] & 0xFF, src[i01] & 0xFF, dx);
                    float bot  = lerp(src[i10] & 0xFF, src[i11] & 0xFF, dx);
                    dst[dstIdx + c] = (byte) Math.round(lerp(top, bot, dy));
                }
            }
        }
        return dst;
    }

    private static float lerp(float a, float b, float t) { return a + (b - a) * t; }
}
```

---

#### 文件2：`OnnxFaceDetector.java` — YOLOv12 人脸检测封装

**要求**：
- 实现 `AutoCloseable`，`close()` **只关 session，不关 OrtEnvironment**
- 构造函数：加载 ONNX 模型，读取输入 shape 中的 H/W
- `detect(File)` → 读图 → `detect(byte[], int, int)`
- `detect(byte[] bgr, int imgH, int imgW)`：letterbox → 推理 → postprocess(NMS)
- **输出类型**：YOLOv12 输出 shape `(1, 300, 6)` rank=3 → 强转 `(float[][][])` 再取 `[0]` 得 `float[][]`
- letterbox：BGR 保持原样（不转换），NCHW 中 channel 0=B,1=G,2=R，填充值 `114.0/255.0`

**完整代码**：

```java
package com.ruoyi.framework.service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.nio.FloatBuffer;
import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import ai.onnxruntime.TensorInfo;

public class OnnxFaceDetector implements AutoCloseable {

    private final OrtEnvironment env;
    private final OrtSession session;
    private final String inputName;
    private final float confThreshold;
    private final float iouThreshold;
    private final int inputW, inputH;

    public OnnxFaceDetector(String modelPath, float confThreshold, float iouThreshold)
            throws OrtException {
        this.confThreshold = confThreshold;
        this.iouThreshold = iouThreshold;
        this.env = OrtEnvironment.getEnvironment();
        this.session = env.createSession(modelPath, new OrtSession.SessionOptions());
        this.inputName = session.getInputNames().iterator().next();
        TensorInfo info = (TensorInfo) session.getInputInfo().get(inputName).getInfo();
        long[] shape = info.getShape();
        this.inputH = (int) shape[2];
        this.inputW = (int) shape[3];
    }

    public List<float[]> detect(File imageFile) throws IOException, OrtException {
        ImageUtils.BgrImage img = ImageUtils.readImageBGR(imageFile);
        return detect(img.pixels, img.h, img.w);
    }

    public List<float[]> detect(byte[] bgr, int imgH, int imgW) throws OrtException {
        LetterboxResult lb = letterbox(bgr, imgH, imgW);

        long[] inputShape = {1, 3, inputH, inputW};
        try (OnnxTensor tensor = OnnxTensor.createTensor(env, FloatBuffer.wrap(lb.data), inputShape)) {
            OrtSession.Result result = session.run(
                Collections.singletonMap(inputName, tensor));

            // 输出 shape (1, 300, 6)，rank=3 → float[][][]
            float[][][] raw = (float[][][]) result.get(0).getValue();
            float[][] detections = raw[0]; // (300, 6)

            return postprocess(detections, lb.scale, lb.padX, lb.padY, imgW, imgH);
        }
    }

    private static class LetterboxResult {
        float[] data;
        float scale;
        int padX, padY;
    }

    private LetterboxResult letterbox(byte[] bgr, int imgH, int imgW) {
        float scale = Math.min((float) inputW / imgW, (float) inputH / imgH);
        int newW = (int) (imgW * scale);
        int newH = (int) (imgH * scale);

        byte[] resized = ImageUtils.resizeBGR(bgr, imgH, imgW, newW, newH);

        int padX = (inputW - newW) / 2;
        int padY = (inputH - newH) / 2;

        float[] data = new float[3 * inputH * inputW];
        for (int c = 0; c < 3; c++) {
            for (int y = 0; y < inputH; y++) {
                for (int x = 0; x < inputW; x++) {
                    int nchwIdx = c * inputH * inputW + y * inputW + x;
                    if (y >= padY && y < padY + newH && x >= padX && x < padX + newW) {
                        int srcIdx = ((y - padY) * newW + (x - padX)) * 3 + c;
                        data[nchwIdx] = (resized[srcIdx] & 0xFF) / 255.0f;
                    } else {
                        data[nchwIdx] = 114.0f / 255.0f;
                    }
                }
            }
        }

        LetterboxResult r = new LetterboxResult();
        r.data = data;
        r.scale = scale;
        r.padX = padX;
        r.padY = padY;
        return r;
    }

    private List<float[]> postprocess(float[][] detections, float scale,
                                       int padX, int padY, int imgW, int imgH) {
        List<float[]> faces = new ArrayList<>();

        for (float[] det : detections) {
            float conf = det[4];
            if (conf < confThreshold) continue;

            float x1 = Math.max(0, (det[0] - padX) / scale);
            float y1 = Math.max(0, (det[1] - padY) / scale);
            float x2 = Math.min(imgW, (det[2] - padX) / scale);
            float y2 = Math.min(imgH, (det[3] - padY) / scale);

            if (x2 > x1 && y2 > y1) {
                faces.add(new float[]{x1, y1, x2, y2, conf});
            }
        }
        return nms(faces);
    }

    private List<float[]> nms(List<float[]> faces) {
        if (faces.size() <= 1) return faces;

        faces.sort((a, b) -> Float.compare(b[4], a[4]));

        List<float[]> kept = new ArrayList<>();
        boolean[] suppressed = new boolean[faces.size()];

        for (int i = 0; i < faces.size(); i++) {
            if (suppressed[i]) continue;
            kept.add(faces.get(i));
            for (int j = i + 1; j < faces.size(); j++) {
                if (suppressed[j]) continue;
                if (iou(faces.get(i), faces.get(j)) > iouThreshold) {
                    suppressed[j] = true;
                }
            }
        }
        return kept;
    }

    private float iou(float[] a, float[] b) {
        float ix1 = Math.max(a[0], b[0]);
        float iy1 = Math.max(a[1], b[1]);
        float ix2 = Math.min(a[2], b[2]);
        float iy2 = Math.min(a[3], b[3]);
        if (ix2 <= ix1 || iy2 <= iy1) return 0;
        float inter = (ix2 - ix1) * (iy2 - iy1);
        float areaA = (a[2] - a[0]) * (a[3] - a[1]);
        float areaB = (b[2] - b[0]) * (b[3] - b[1]);
        return inter / (areaA + areaB - inter);
    }

    @Override
    public void close() throws OrtException {
        session.close(); // 不关闭共享的 OrtEnvironment 单例
    }
}
```

---

#### 文件3：`OnnxFaceEmbedding.java` — ArcFace 特征提取封装

**要求**：
- 实现 `AutoCloseable`，`close()` **只关 session，不关 OrtEnvironment**
- `extract(byte[] bgr, int imgH, int imgW)`：预处理（resize→BGR2RGB→归一化[-1,1]→NCHW）→ 推理 → L2 归一化 → 返回 `float[512]`
- **输出类型**：ArcFace 输出 shape `(1, 512)` rank=2 → 强转 `(float[][])` 再取 `[0]`
- 预处理中 BGR→RGB 转换：flat 数组 B(idx+0)/G(idx+1)/R(idx+2) → NCHW 中 channel0=R, channel1=G, channel2=B

**完整代码**：

```java
package com.ruoyi.framework.service;

import java.nio.FloatBuffer;
import java.util.Collections;
import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;

public class OnnxFaceEmbedding implements AutoCloseable {

    private static final int INPUT_SIZE = 112;
    private static final int EMBEDDING_DIM = 512;

    private final OrtEnvironment env;
    private final OrtSession session;
    private final String inputName;

    public OnnxFaceEmbedding(String modelPath) throws OrtException {
        this.env = OrtEnvironment.getEnvironment();
        this.session = env.createSession(modelPath, new OrtSession.SessionOptions());
        this.inputName = session.getInputNames().iterator().next();
    }

    /**
     * 提取 512 维特征向量（已 L2 归一化）
     */
    public float[] extract(byte[] bgr, int imgH, int imgW) throws OrtException {
        float[] blob = preprocess(bgr, imgH, imgW);

        long[] inputShape = {1, 3, INPUT_SIZE, INPUT_SIZE};
        try (OnnxTensor tensor = OnnxTensor.createTensor(env, FloatBuffer.wrap(blob), inputShape)) {
            OrtSession.Result result = session.run(
                Collections.singletonMap(inputName, tensor));

            // 输出 shape (1, 512)，rank=2 → float[][]
            float[][] raw = (float[][]) result.get(0).getValue();
            float[] embedding = raw[0]; // (512,)

            l2Normalize(embedding);
            return embedding;
        }
    }

    /**
     * BGR flat → resize 112×112 → BGR→RGB → normalize [-1,1] → NCHW float[]
     */
    private float[] preprocess(byte[] bgr, int srcH, int srcW) {
        byte[] resized = ImageUtils.resizeBGR(bgr, srcH, srcW, INPUT_SIZE, INPUT_SIZE);

        float[] data = new float[3 * INPUT_SIZE * INPUT_SIZE];
        for (int y = 0; y < INPUT_SIZE; y++) {
            for (int x = 0; x < INPUT_SIZE; x++) {
                int idx = (y * INPUT_SIZE + x) * 3;
                // BGR → RGB：通道反转
                float r = norm(resized[idx + 2] & 0xFF);
                float g = norm(resized[idx + 1] & 0xFF);
                float b = norm(resized[idx]     & 0xFF);

                int base = y * INPUT_SIZE + x;
                data[0 * INPUT_SIZE * INPUT_SIZE + base] = r;
                data[1 * INPUT_SIZE * INPUT_SIZE + base] = g;
                data[2 * INPUT_SIZE * INPUT_SIZE + base] = b;
            }
        }
        return data;
    }

    private static float norm(int pixel) { return (pixel - 127.5f) / 127.5f; }

    private static void l2Normalize(float[] v) {
        float sum = 0;
        for (float x : v) sum += x * x;
        float norm = (float) Math.sqrt(sum);
        if (norm > 0) {
            for (int i = 0; i < v.length; i++) v[i] /= norm;
        }
    }

    public int getEmbeddingDim() { return EMBEDDING_DIM; }

    @Override
    public void close() throws OrtException {
        session.close(); // 不关闭共享的 OrtEnvironment 单例
    }
}
```

---

#### 文件4：`SimilarityUtils.java` — 余弦相似度

```java
package com.ruoyi.framework.service;

public class SimilarityUtils {

    /**
     * 两个已 L2 归一化的向量 → 余弦相似度 ∈ [-1, 1]
     */
    public static float cosine(float[] a, float[] b) {
        float dot = 0;
        for (int i = 0; i < a.length; i++) dot += a[i] * b[i];
        return dot;
    }
}
```

---

#### 文件5：`RealAiInferenceService.java` — 完整 AI 推理服务（核心）

**要求**：
- `@Service`，实现 `IAiInferenceService`，替换 `StubAiInferenceService`
- `@Autowired AiModelMapper` + `@Autowired AiFaceRegisterMapper`
- `@Value` 注入配置：`ai.model.detection-code`（默认 `yolo_v8_obj_001`）、`ai.model.conf-threshold`（默认 0.5）、`ai.model.iou-threshold`（默认 0.45）、`ai.model.similarity-threshold`（默认 0.35）
- 懒加载 ONNX 模型（double-checked locking），`getEmbedding(String modelCode)` 按 modelCode 查 `ai_model` 动态加载
- `extractFaceFeature(modelCode, faceImgUrl)`：
  1. `resolveImageFile()` 解析 `/profile/xxx` → 物理路径（用 `RuoYiConfig.getProfile()`）
  2. YOLOv12 检测人脸 → 取面积最大的
  3. 裁切人脸区域（`cropFace`）
  4. ArcFace 按 modelCode 提取特征向量
  5. `float[]` → Base64 编码 → `{"dim":512,"features":"<base64>"}` JSON → `AiInferenceResult.success()`
- `infer(modelCode, input)`：如果是 `face_recognition` 类型 → `doFaceRecognition()`（1:N 比对，遍历 `selectFaceRegisterList(null,"1")`）。其他类型返回 fail("尚未实现")
- `doFaceRecognition()`：检测→提取→遍历底库计算余弦相似度→返回最佳匹配 JSON
- `isModelAvailable()` / `getModelStatus()`：返回模型加载状态
- **`@PreDestroy destroy()`**：依次 close detector 和 embedding 的 session
- **特征向量序列化**：`float[]` → `ByteBuffer` → Base64 存储；反序列化时 Base64 → `ByteBuffer` → `float[]`

**完整代码**：

```java
package com.ruoyi.framework.service;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.config.RuoYiConfig;
import com.ruoyi.common.service.AiInferenceResult;
import com.ruoyi.common.service.IAiInferenceService;
import com.ruoyi.system.domain.AiFaceRegister;
import com.ruoyi.system.domain.AiModel;
import com.ruoyi.system.mapper.AiFaceRegisterMapper;
import com.ruoyi.system.mapper.AiModelMapper;

@Service
public class RealAiInferenceService implements IAiInferenceService {

    private static final Logger log = LoggerFactory.getLogger(RealAiInferenceService.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private AiModelMapper aiModelMapper;

    @Autowired
    private AiFaceRegisterMapper aiFaceRegisterMapper;

    @Value("${ai.model.detection-code:yolo_v8_obj_001}")
    private String detectionModelCode;

    @Value("${ai.model.conf-threshold:0.5}")
    private float confThreshold;

    @Value("${ai.model.iou-threshold:0.45}")
    private float iouThreshold;

    @Value("${ai.model.similarity-threshold:0.35}")
    private float similarityThreshold;

    private volatile OnnxFaceDetector detector;
    private volatile OnnxFaceEmbedding embedding;
    private volatile String loadedEmbeddingModelCode;
    private final Object detectorLock = new Object();
    private final Object embeddingLock = new Object();

    @Override
    public AiInferenceResult extractFaceFeature(String modelCode, String faceImgUrl) {
        long start = System.currentTimeMillis();

        try {
            File faceFile = resolveImageFile(faceImgUrl);
            if (!faceFile.exists()) {
                return AiInferenceResult.fail(modelCode, "人脸图片文件不存在: " + faceImgUrl);
            }

            OnnxFaceDetector det = getDetector();
            if (det == null) {
                return AiInferenceResult.fail(modelCode, "人脸检测模型未就绪");
            }

            List<float[]> faces = det.detect(faceFile);
            if (faces.isEmpty()) {
                return AiInferenceResult.fail(modelCode, "未检测到人脸");
            }

            float[] bestFace = faces.stream()
                .max((a, b) -> {
                    float areaA = (a[2] - a[0]) * (a[3] - a[1]);
                    float areaB = (b[2] - b[0]) * (b[3] - b[1]);
                    return Float.compare(areaA, areaB);
                }).get();

            ImageUtils.BgrImage img = ImageUtils.readImageBGR(faceFile);
            ImageUtils.BgrImage crop = cropFace(img, bestFace);

            OnnxFaceEmbedding emb = getEmbedding(modelCode);
            if (emb == null) {
                return AiInferenceResult.fail(modelCode, "人脸特征提取模型未就绪: " + modelCode);
            }

            float[] feature = emb.extract(crop.pixels, crop.h, crop.w);

            Map<String, Object> resultMap = new HashMap<>();
            resultMap.put("dim", 512);
            resultMap.put("features", floatsToBase64(feature));
            String resultJson = mapper.writeValueAsString(resultMap);

            long elapsed = System.currentTimeMillis() - start;
            log.info("人脸特征提取成功: modelCode={}, dim=512, time={}ms", modelCode, elapsed);
            return AiInferenceResult.success(modelCode, resultJson, elapsed);

        } catch (Exception e) {
            log.error("人脸特征提取失败: modelCode={}, faceImgUrl={}", modelCode, faceImgUrl, e);
            return AiInferenceResult.fail(modelCode, "特征提取异常: " + e.getMessage());
        }
    }

    @Override
    public AiInferenceResult infer(String modelCode, String input) {
        long start = System.currentTimeMillis();

        try {
            AiModel model = aiModelMapper.selectAiModelByCode(modelCode);
            if (model == null) {
                return AiInferenceResult.fail(modelCode, "模型不存在: " + modelCode);
            }

            if ("face_recognition".equals(model.getModelType())) {
                return doFaceRecognition(modelCode, input, start);
            } else {
                return AiInferenceResult.fail(modelCode,
                    "模型类型 " + model.getModelType() + " 的推理逻辑尚未实现");
            }
        } catch (Exception e) {
            log.error("推理失败: modelCode={}", modelCode, e);
            return AiInferenceResult.fail(modelCode, "推理异常: " + e.getMessage());
        }
    }

    private AiInferenceResult doFaceRecognition(String modelCode, String imagePath, long start) {
        try {
            File imageFile = resolveImageFile(imagePath);
            if (!imageFile.exists()) {
                return AiInferenceResult.fail(modelCode, "图片文件不存在: " + imagePath);
            }

            OnnxFaceDetector det = getDetector();
            OnnxFaceEmbedding emb = getEmbedding(modelCode);
            if (det == null || emb == null) {
                return AiInferenceResult.fail(modelCode, "模型未就绪");
            }

            List<float[]> faces = det.detect(imageFile);
            if (faces.isEmpty()) {
                return AiInferenceResult.fail(modelCode, "未检测到人脸");
            }

            float[] bestFace = faces.stream()
                .max((a, b) -> {
                    float areaA = (a[2] - a[0]) * (a[3] - a[1]);
                    float areaB = (b[2] - b[0]) * (b[3] - b[1]);
                    return Float.compare(areaA, areaB);
                }).get();

            ImageUtils.BgrImage img = ImageUtils.readImageBGR(imageFile);
            ImageUtils.BgrImage crop = cropFace(img, bestFace);
            float[] queryEmbedding = emb.extract(crop.pixels, crop.h, crop.w);

            List<AiFaceRegister> registered = aiFaceRegisterMapper
                .selectFaceRegisterList(null, "1");

            String bestMatch = null;
            float bestSim = -1;
            Long bestWorkerId = null;

            for (AiFaceRegister reg : registered) {
                if (reg.getFaceFeature() == null || reg.getFaceFeature().isEmpty()) continue;
                float[] stored = parseFeatureFromJson(reg.getFaceFeature());
                if (stored == null) continue;

                float sim = SimilarityUtils.cosine(stored, queryEmbedding);
                if (sim > bestSim) {
                    bestSim = sim;
                    bestMatch = reg.getWorkerName();
                    bestWorkerId = reg.getWorkerId();
                }
            }

            long elapsed = System.currentTimeMillis() - start;

            if (bestSim >= similarityThreshold && bestMatch != null) {
                Map<String, Object> resultMap = new HashMap<>();
                resultMap.put("matched", true);
                resultMap.put("workerId", bestWorkerId);
                resultMap.put("personName", bestMatch);
                double simRound = Math.round(bestSim * 10000) / 10000.0;
                resultMap.put("confidence", simRound);
                resultMap.put("similarity", simRound);

                String resultJson = mapper.writeValueAsString(resultMap);
                log.info("人脸识别成功: {} → {} (sim={})", imagePath, bestMatch, bestSim);
                return AiInferenceResult.success(modelCode, resultJson, elapsed);
            } else {
                Map<String, Object> resultMap = new HashMap<>();
                resultMap.put("matched", false);
                resultMap.put("bestSimilarity", Math.round(bestSim * 10000) / 10000.0);

                String resultJson = mapper.writeValueAsString(resultMap);
                return AiInferenceResult.success(modelCode, resultJson, elapsed);
            }

        } catch (Exception e) {
            log.error("人脸识别异常", e);
            return AiInferenceResult.fail(modelCode, "人脸识别异常: " + e.getMessage());
        }
    }

    @Override
    public boolean isModelAvailable(String modelCode) {
        return getDetector() != null && getEmbedding(modelCode) != null;
    }

    @Override
    public Map<String, Object> getModelStatus(String modelCode) {
        Map<String, Object> status = new HashMap<>();
        boolean detReady = getDetector() != null;
        boolean embReady = getEmbedding(modelCode) != null;
        status.put("detection_model", detReady ? "loaded" : "not_loaded");
        status.put("embedding_model", embReady ? "loaded" : "not_loaded");
        status.put("model_code", modelCode);
        status.put("status", (detReady && embReady) ? "ready" : "not_ready");
        return status;
    }

    // ========== 辅助方法 ==========

    private ImageUtils.BgrImage cropFace(ImageUtils.BgrImage img, float[] box) {
        int x1 = Math.max(0, (int) box[0]);
        int y1 = Math.max(0, (int) box[1]);
        int x2 = Math.min(img.w, (int) box[2]);
        int y2 = Math.min(img.h, (int) box[3]);
        int cropW = x2 - x1, cropH = y2 - y1;
        byte[] crop = new byte[cropH * cropW * 3];
        for (int y = 0; y < cropH; y++) {
            for (int x = 0; x < cropW; x++) {
                int srcIdx = ((y1 + y) * img.w + (x1 + x)) * 3;
                int dstIdx = (y * cropW + x) * 3;
                crop[dstIdx]     = img.pixels[srcIdx];
                crop[dstIdx + 1] = img.pixels[srcIdx + 1];
                crop[dstIdx + 2] = img.pixels[srcIdx + 2];
            }
        }
        return new ImageUtils.BgrImage(crop, cropH, cropW);
    }

    private File resolveImageFile(String faceImgUrl) {
        if (faceImgUrl == null || faceImgUrl.isEmpty()) return new File("");
        if (faceImgUrl.startsWith("/profile")) {
            String profile = RuoYiConfig.getProfile();
            return new File(profile, faceImgUrl.replaceFirst("^/profile", ""));
        }
        return new File(faceImgUrl);
    }

    // ========== 模型懒加载 ==========

    private OnnxFaceDetector getDetector() {
        if (detector == null) {
            synchronized (detectorLock) {
                if (detector == null) {
                    try {
                        AiModel model = aiModelMapper.selectAiModelByCode(detectionModelCode);
                        if (model != null && model.getModelPath() != null
                                && !model.getModelPath().isEmpty()) {
                            detector = new OnnxFaceDetector(
                                model.getModelPath(), confThreshold, iouThreshold);
                            log.info("人脸检测模型加载成功: {} (conf={}, iou={})",
                                model.getModelPath(), confThreshold, iouThreshold);
                        } else {
                            log.error("未找到人脸检测模型配置或路径为空: code={}", detectionModelCode);
                        }
                    } catch (Exception e) {
                        log.error("加载人脸检测模型失败: code={}", detectionModelCode, e);
                    }
                }
            }
        }
        return detector;
    }

    private OnnxFaceEmbedding getEmbedding(String modelCode) {
        if (embedding == null || !modelCode.equals(loadedEmbeddingModelCode)) {
            synchronized (embeddingLock) {
                if (embedding == null || !modelCode.equals(loadedEmbeddingModelCode)) {
                    try {
                        AiModel model = aiModelMapper.selectAiModelByCode(modelCode);
                        if (model != null && model.getModelPath() != null
                                && !model.getModelPath().isEmpty()) {
                            if (embedding != null) {
                                try { embedding.close(); } catch (Exception ignored) {}
                            }
                            embedding = new OnnxFaceEmbedding(model.getModelPath());
                            loadedEmbeddingModelCode = modelCode;
                            log.info("人脸特征提取模型加载成功: {}, code={}",
                                model.getModelPath(), modelCode);
                        } else {
                            log.error("未找到人脸特征提取模型配置或路径为空: code={}", modelCode);
                        }
                    } catch (Exception e) {
                        log.error("加载人脸特征提取模型失败: code={}", modelCode, e);
                    }
                }
            }
        }
        return embedding;
    }

    // ========== 特征向量序列化 ==========

    private static String floatsToBase64(float[] arr) {
        byte[] bytes = new byte[arr.length * 4];
        for (int i = 0; i < arr.length; i++) {
            int bits = Float.floatToIntBits(arr[i]);
            bytes[i * 4]     = (byte) (bits >> 24);
            bytes[i * 4 + 1] = (byte) (bits >> 16);
            bytes[i * 4 + 2] = (byte) (bits >> 8);
            bytes[i * 4 + 3] = (byte) (bits);
        }
        return java.util.Base64.getEncoder().encodeToString(bytes);
    }

    private static float[] parseFeatureFromJson(String json) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = mapper.readValue(json, Map.class);
            String b64 = (String) map.get("features");
            if (b64 == null) return null;
            byte[] bytes = java.util.Base64.getDecoder().decode(b64);
            float[] arr = new float[bytes.length / 4];
            for (int i = 0; i < arr.length; i++) {
                arr[i] = Float.intBitsToFloat(
                    ((bytes[i * 4] & 0xFF) << 24) |
                    ((bytes[i * 4 + 1] & 0xFF) << 16) |
                    ((bytes[i * 4 + 2] & 0xFF) << 8) |
                    (bytes[i * 4 + 3] & 0xFF));
            }
            return arr;
        } catch (Exception e) {
            return null;
        }
    }

    @PreDestroy
    public void destroy() {
        if (detector != null) {
            try { detector.close(); } catch (Exception e) { log.warn("关闭检测模型失败", e); }
        }
        if (embedding != null) {
            try { embedding.close(); } catch (Exception e) { log.warn("关闭特征提取模型失败", e); }
        }
        log.info("AI推理服务已关闭");
    }
}
```

---

#### 文件6（修改）：`StubAiInferenceService.java` — 添加 Bean 名称避免冲突

**文件位置**：`RuoYi-Vue-springboot3/ruoyi-framework/src/main/java/com/ruoyi/framework/service/StubAiInferenceService.java`

将原来的 `@Component` 改为 `@Component("stubAiInferenceService")`，使 Spring 能区分两个 `IAiInferenceService` 实现。或者在 `RealAiInferenceService` 上加 `@Primary`。

---

### 第三部分：数据库更新

部署前执行以下 SQL，更新 `ai_model` 表中两条示例记录指向真实 ONNX 模型文件路径：

```sql
UPDATE ai_model SET
  model_path = 'C:/Users/ASUS/Desktop/RuoYi-Vue-springboot3-main/RuoYi-Vue-springboot3/ai-models/yolov12m-face.onnx',
  framework = 'onnx',
  version = 'v12-m',
  config_json = '{"confidence_threshold":0.5,"iou_threshold":0.45,"input_size":"640x640"}',
  metrics = '{"mAP50":0.92,"fps":45}',
  status = '1'
WHERE model_code = 'yolo_v8_obj_001';

UPDATE ai_model SET
  model_path = 'C:/Users/ASUS/Desktop/RuoYi-Vue-springboot3-main/RuoYi-Vue-springboot3/ai-models/w600k_mbf.onnx',
  framework = 'onnx',
  version = 'w600k_mbf',
  config_json = '{"embedding_dim":512,"similarity_threshold":0.35,"input_size":"112x112"}',
  metrics = '{"accuracy":0.965,"far@1e-4":0.001}',
  status = '1'
WHERE model_code = 'arcface_r100_001';
```

---

### 第四部分：模型文件部署

将两个 `.onnx` 文件复制到项目 ai-models 目录：

```bash
mkdir -p 项目根目录/ai-models/
cp C:/Users/ASUS/Desktop/faceREC/yolov12m-face.onnx 项目根目录/ai-models/
cp C:/Users/ASUS/Desktop/faceREC/w600k_mbf.onnx 项目根目录/ai-models/
```

---

### 第五部分：应用配置（可选）

如需自定义阈值，在 `ruoyi-admin/src/main/resources/application.yml` 中添加：

```yaml
ai:
  model:
    detection-code: yolo_v8_obj_001
    conf-threshold: 0.5
    iou-threshold: 0.45
    similarity-threshold: 0.35
```

> 所有配置项均有默认值，不添加也可运行。

---

## 四、新增文件总览

| # | 文件 | 路径 | 说明 |
|---|------|------|------|
| 1 | `ImageUtils.java` | `ruoyi-framework/.../service/` | 图像读写 + 双线性缩放（flat byte[] + BGR） |
| 2 | `OnnxFaceDetector.java` | `ruoyi-framework/.../service/` | YOLOv12 ONNX 人脸检测（letterbox + NMS） |
| 3 | `OnnxFaceEmbedding.java` | `ruoyi-framework/.../service/` | ArcFace ONNX 特征提取（L2 归一化） |
| 4 | `SimilarityUtils.java` | `ruoyi-framework/.../service/` | 余弦相似度 |
| 5 | `RealAiInferenceService.java` | `ruoyi-framework/.../service/` | 完整 AI 推理服务（含人脸 1:N 识别 + @PreDestroy） |

**已有文件修改**：

| 文件 | 改动 |
|------|------|
| `ruoyi-framework/pom.xml` | 添加 `onnxruntime` 依赖 |
| `StubAiInferenceService.java` | `@Component` → `@Component("stubAiInferenceService")`（或给新实现加 `@Primary`，或直接删除此文件） |

---

## 五、依赖注入说明

`RealAiInferenceService` 需要注入以下已有 Mapper（来自 `ruoyi-system` 模块）：
- `AiModelMapper` — 读取模型路径和配置
- `AiFaceRegisterMapper` — 人脸 1:N 识别时读取底库特征向量

`ruoyi-framework` 已经依赖 `ruoyi-system`，直接 `@Autowired` 即可，无需修改 pom.xml。

---

## 六、验证清单

所有文件创建完成后，按顺序执行：

### 编译验证
```bash
cd RuoYi-Vue-springboot3
export JAVA_HOME="C:/Program Files/Eclipse Adoptium/jdk-20.0.2.9-hotspot"
mvn clean install -DskipTests
```
确认 `BUILD SUCCESS`，无编译错误。

### 启动验证
1. 确保模型文件已复制到 `项目根目录/ai-models/`
2. 确保数据库 `ai_model` 表 `model_path` 已更新为实际路径
3. 启动后端 `mvn spring-boot:run -f ruoyi-admin`
4. 登录 admin/admin123 → AI算法仓 → 模型管理
5. 确认 YOLOv12 和 ArcFace 模型状态可查看
6. 进入 AI算法仓 → 人脸注册
7. 对有 `face_img_url` 的人员点击"录入"
8. 查看日志：`人脸检测模型加载成功` → `人脸特征提取模型加载成功` → `人脸特征提取成功: modelCode=arcface_r100_001, dim=512, time=XXXms`
9. 确认该人员状态变为"已录入"，`ai_face_register.face_feature` 有 Base64 特征数据
10. 确认 `tb_worker.face_status` 同步更新为 '1'

### 桩替换验证
11. 确认 `StubAiInferenceService` 不再是 `IAiInferenceService` 的默认实现（检查日志中没有"AI推理服务尚未配置"的警告）

---

## 七、常见问题快速排查

| 现象 | 原因 | 解决 |
|------|------|------|
| `UnsatisfiedLinkError` | ONNX Runtime 原生库缺失 | 检查 `onnxruntime` 版本与 OS 匹配，Windows 需 VC++ Redist 2019+ |
| `ClassCastException` on `getValue()` | ONNX 输出 rank 判断错误 | YOLO rank=3→`float[][][]`，ArcFace rank=2→`float[][]` |
| 首次推理极慢（3-5s） | ONNX Runtime JIT 编译 + 模型加载 | 正常现象，后续推理进入毫秒级 |
| 识别准确率极低 | 相似度阈值太高 | 调低 `ai.model.similarity-threshold`（0.25） |
| 大图 OOM | 4000×3000 原始照片内存占用大 | 先 resize 到 1920×1080 再送入模型 |
| Bean 冲突 | 两个 `IAiInferenceService` 实现 | 给桩加 `@Component("stubAiInferenceService")` 或给新实现加 `@Primary` |
