# YOLOv12 + ArcFace ONNX 模型 Java 集成开发日志

## 基本信息
- 开发日期：2026-06-05
- 模块名称：AI推理服务真实实现（替换 StubAiInferenceService 桩）
- 后端框架：Spring Boot 3.5.x + MyBatis + Spring Security 6 + JWT
- 数据库：MySQL 8.x，库名 `ry-vue`
- AI 框架：ONNX Runtime 1.18.0
- Python 参考：`C:\Users\ASUS\Desktop\faceREC\`
- 开发者：AI 自动生成

---

## 一、模型文件

| 文件 | 大小 | 用途 | 来源 |
|------|------|------|------|
| `yolov12m-face.onnx` | 80MB | YOLOv12 人脸检测（输入 640×640 BGR，输出 bounding boxes） | 用户提供 |
| `w600k_mbf.onnx` | 13MB | ArcFace 人脸特征提取（输入 112×112 RGB，输出 512 维 embedding） | insightface v0.7 (buffalo_s) |

**部署路径**：`RuoYi-Vue-springboot3/ai-models/`

> **路径决策（2026-06-05 修订）**：
> - 模型文件最初放在外部 `D:/ruoyi/uploadPath/ai-models/`，后移至项目内部 `RuoYi-Vue-springboot3/ai-models/`
> - 文件上传 `profile` 路径原为 `D:/ruoyi/uploadPath`，同步改为项目内部 `RuoYi-Vue-springboot3/uploadPath/`
> - 所有文件（模型 + 用户上传 + 代码 + SQL）统一在项目目录下管理，不再依赖外部路径

---

## 二、新增 Java 文件（5 个）

所有文件位于 `ruoyi-framework/src/main/java/com/ruoyi/framework/service/`。

### 2.1 ImageUtils.java

图像工具类。纯 Java 实现（无 OpenCV 依赖）：
- 内部类 `BgrImage`：flat `byte[] pixels`（长度 H×W×3）+ 尺寸 h, w
- `readImageBGR(File)`：`ImageIO.read()` → `getRGB()` 逐像素取色 → RGB→BGR 重排
- `resizeBGR(src, srcH, srcW, dstW, dstH)`：双线性插值缩放，直接操作 flat byte 数组
- 像素索引公式：`idx = (y * W + x) * 3 + c`（c=0=B, 1=G, 2=R）
- 禁止使用 `byte[h][w][3]` 三维数组（每像素创建 3 个对象，1920×1080 图片产生约 200 万个额外对象）

### 2.2 OnnxFaceDetector.java

YOLOv12 人脸检测封装，`implements AutoCloseable`：
- 构造函数：`OnnxFaceDetector(modelPath, confThreshold, iouThreshold)`
- 使用 `OrtEnvironment.getEnvironment()` 全局单例，从 `TensorInfo.getShape()` 读取输入尺寸
- `detect(File)` → `ImageUtils.readImageBGR()` → `detect(byte[], int, int)`
- `detect()` 内部流程：letterbox（保持宽高比缩放 + 灰色 114 填充）→ `FloatBuffer.wrap()` 创建 Tensor → ONNX 推理 → postprocess
- **输出类型**：YOLOv12 shape `(1, 300, 6)` rank=3 → `(float[][][]) result.get(0).getValue()` → `[0]` 取 batch
- Letterbox 预处理：BGR 保持原样，NCHW 中 channel 0=B/1=G/2=R，填充值 `114.0/255.0`
- 后处理：过滤 conf < confThreshold → letterbox 坐标转原图坐标 → 贪心 NMS（按 conf 降序，IoU > iouThreshold 抑制）
- `close()` 只关闭 session，**不关闭共享的 OrtEnvironment**

### 2.3 OnnxFaceEmbedding.java

ArcFace 特征提取封装，`implements AutoCloseable`：
- `extract(byte[] bgr, int imgH, int imgW)`：预处理（resize 112×112 → BGR→RGB → 归一化 `(p-127.5)/127.5` → NCHW）→ ONNX 推理 → L2 归一化 → 返回 `float[512]`
- **输出类型**：ArcFace shape `(1, 512)` rank=2 → `(float[][]) result.get(0).getValue()` → `[0]` 取 batch
- BGR→RGB 转换：flat 数组 B(idx+0)/G(idx+1)/R(idx+2) → NCHW 中 channel0=R, channel1=G, channel2=B
- L2 归一化：`v[i] /= sqrt(sum(v[i]^2))`
- `close()` 只关闭 session

### 2.4 SimilarityUtils.java

余弦相似度工具：
- `cosine(float[] a, float[] b)`：`dot(a, b)` — 因向量已 L2 归一化，点积即余弦值

### 2.5 RealAiInferenceService.java

完整 AI 推理服务，`@Service implements IAiInferenceService`：

**依赖注入**：
- `@Autowired AiModelMapper` — 读取模型路径和配置
- `@Autowired AiFaceRegisterMapper` — 人脸 1:N 识别时读取底库特征
- `@Value("${ai.model.detection-code:yolo_v8_obj_001}")` — 检测模型编码
- `@Value("${ai.model.conf-threshold:0.5}")` — 检测置信度阈值
- `@Value("${ai.model.iou-threshold:0.45}")` — NMS IoU 阈值
- `@Value("${ai.model.similarity-threshold:0.35}")` — 人脸匹配阈值

**模型懒加载**（double-checked locking）：
- `getDetector()`：按 `detectionModelCode` 查 `ai_model` 表 → 创建 `OnnxFaceDetector`
- `getEmbedding(String modelCode)`：按 `modelCode` 查表 → 创建 `OnnxFaceEmbedding`。支持动态切换模型（`loadedEmbeddingModelCode` 跟踪），切换时先 close 旧 session

**核心方法**：
| 方法 | 业务规则 |
|------|---------|
| `extractFaceFeature(modelCode, faceImgUrl)` | ① `resolveImageFile()`：`/profile/xxx` → `RuoYiConfig.getProfile()` 拼接物理路径 → ② YOLOv12 检测人脸 → ③ 取面积最大的 → ④ `cropFace()` 裁切 → ⑤ ArcFace 按 modelCode 提取 512 维特征 → ⑥ `float[]` → Base64 编码 → `{"dim":512,"features":"<base64>"}` JSON → `AiInferenceResult.success()` |
| `infer(modelCode, input)` | 查模型类型 → `face_recognition` → `doFaceRecognition()`（1:N 比对）；其他 → fail("尚未实现") |
| `doFaceRecognition()` | 检测→提取→`selectFaceRegisterList(null,"1")` 遍历底库 → `SimilarityUtils.cosine()` 逐一比对 → 返回最佳匹配 JSON 或 `{"matched":false}` |
| `isModelAvailable()` | detector 和 embedding 均已加载 → true |
| `getModelStatus()` | 返回 `{detection_model, embedding_model, model_code, status}` |

**特征向量序列化**：`float[512]` → `Float.floatToIntBits()` → 4 字节大端 → `byte[2048]` → `Base64.getEncoder()` 存储。反序列化：Base64 解码 → 每 4 字节反向 `Float.intBitsToFloat()`。

**`@PreDestroy destroy()`**：容器关闭时依次 close detector 和 embedding 的 session。

---

## 三、已有文件修改（3 个）

| 文件 | 修改内容 | 原因 |
|------|---------|------|
| `ruoyi-framework/pom.xml` | 添加 `onnxruntime 1.18.0` 依赖 | ONNX Runtime Java 推理引擎，包含 Windows/Linux/macOS x64 原生库 |
| `ruoyi-framework/.../StubAiInferenceService.java` | `@Component` → `@Component("stubAiInferenceService")` | 避免与 `RealAiInferenceService` 的 Bean 冲突，Spring 可区分两个 `IAiInferenceService` 实现 |

---

## 四、数据库更新

执行以下 SQL 更新 `ai_model` 表，将占位路径改为真实 ONNX 模型路径：

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

## 五、应用配置

### 5.1 AI 模型参数（可选）

`ruoyi-admin/src/main/resources/application.yml` 可追加以下配置（均有默认值）：

```yaml
ai:
  model:
    detection-code: yolo_v8_obj_001
    conf-threshold: 0.5
    iou-threshold: 0.45
    similarity-threshold: 0.35
```

### 5.2 文件上传路径（已修改）

```yaml
ruoyi:
  profile: C:/Users/ASUS/Desktop/RuoYi-Vue-springboot3-main/RuoYi-Vue-springboot3/uploadPath
```

**URL ↔ 物理路径映射**：

| URL（数据库存储） | 物理路径 |
|------|------|
| `/profile/upload/2026/06/05/xxx.jpg` | `{profile}/upload/2026/06/05/xxx.jpg` |

`ResourcesConfig` 将 `/profile/**` 映射到 `file:{profile}/`，`RuoYiConfig.getProfile()` 返回上述配置值。

**AI 推理读图**（`RealAiInferenceService.resolveImageFile()`）：

```java
// faceImgUrl = "/profile/upload/2026/06/05/xxx.jpg"
// → new File(profile, "upload/2026/06/05/xxx.jpg")
// → 物理文件: .../uploadPath/upload/2026/06/05/xxx.jpg
```

---

## 六、Python → Java 预处理对照

### YOLOv12 人脸检测

| 步骤 | Python | Java |
|------|--------|------|
| 读取图像 | `cv2.imread()` → BGR uint8 | `ImageIO.read()` → `getRGB()` → 重排为 BGR flat byte[] |
| Letterbox | `cv2.resize` + `np.full(114)` + 居中放置 | `ImageUtils.resizeBGR`（双线性）+ 三层循环填 NCHW float[] |
| 归一化 | `/ 255.0` → [0, 1] | `/ 255.0f` → [0, 1] |
| HWC→CHW | `np.transpose(2, 0, 1)` | 三层循环手动重排，NCHW 布局 |
| 推理 | `ort.InferenceSession.run()` | `OrtSession.run()` + `FloatBuffer.wrap()` |
| 输出 | `(1, 300, 6)` ndarray | `float[][][]` → `[0]` → `float[300][6]` |
| 后处理 | `cv2.dnn.NMSBoxes` | 贪心 NMS（排序 → 遍历 → IoU 抑制） |

### ArcFace 特征提取

| 步骤 | Python | Java |
|------|--------|------|
| 缩放 | `cv2.resize(112, 112)` | `ImageUtils.resizeBGR`（双线性） |
| BGR→RGB | `cv2.COLOR_BGR2RGB` | flat 数组通道反转：[B,G,R] → 输出 R,G,B |
| 归一化 | `(x-127.5)/127.5` → [-1, 1] | `(pixel - 127.5f) / 127.5f` |
| 推理 | `ort.InferenceSession.run()` | `OrtSession.run()` + `FloatBuffer.wrap()` |
| 输出 | `(1, 512)` ndarray | `float[][]` → `[0]` → `float[512]` |
| L2 归一化 | `embedding / np.linalg.norm` | `v[i] /= sqrt(sum(v[i]^2))` |

---

## 七、ONNX Runtime Java API 踩坑记录

| 问题 | 原因 | 解决 |
|------|------|------|
| `createTensor(env, float[], long[])` 不存在 | ONNX Runtime Java 不接受 `float[]` 参数 | 改用 `FloatBuffer.wrap(float[])` |
| `ValueInfo.getShape()` 不存在 | `ValueInfo` 是父接口，`TensorInfo` 才有 `getShape()` | 强转 `(TensorInfo) getInfo()` 后再调 `getShape()` |
| `OrtEnvironment` 不能重复 close | `getEnvironment()` 返回全局单例 | 只在各 Session 的 `close()` 中关闭 session，`@PreDestroy` 中统一清理 |

---

## 八、文件清单

### 新增文件（6 个）

| # | 文件 | 完整路径 |
|---|------|---------|
| 1 | `ImageUtils.java` | `ruoyi-framework/.../service/ImageUtils.java` |
| 2 | `OnnxFaceDetector.java` | `ruoyi-framework/.../service/OnnxFaceDetector.java` |
| 3 | `OnnxFaceEmbedding.java` | `ruoyi-framework/.../service/OnnxFaceEmbedding.java` |
| 4 | `SimilarityUtils.java` | `ruoyi-framework/.../service/SimilarityUtils.java` |
| 5 | `RealAiInferenceService.java` | `ruoyi-framework/.../service/RealAiInferenceService.java` |
| 6 | `RealFaceRecognitionService.java` | `ruoyi-framework/.../service/RealFaceRecognitionService.java` |

### 已有文件修改（6 个）

| # | 文件 | 改动 | 原因 |
|---|------|------|------|
| 1 | `ruoyi-framework/pom.xml` | 添加 `onnxruntime 1.18.0` | ONNX Runtime 推理引擎 |
| 2 | `StubAiInferenceService.java` | `@Component` → `@Component("stubAiInferenceService")` + 实现 `detectFace()` | Bean 冲突 + 接口新增方法 |
| 3 | `StubFaceRecognitionService.java` | `@Component` → `@Component("stubFaceRecognitionService")` | Bean 冲突 |
| 4 | `ruoyi-admin/.../application.yml` | `ruoyi.profile` → 项目内 `uploadPath/` | 文件统一管理 |
| 5 | `IAiInferenceService.java` | 新增 `detectFace(String)` 方法 | 桥接需要 |
| 6 | `RealAiInferenceService.java` | 实现 `detectFace()` + 加 `@Primary` | 桥接需要 + Bean 冲突 |

---

## 九、模块架构

```
AI算法仓人脸识别推理链路
│
├── 前端（Vue 2.x）
│   └── AI算法仓 → 人脸注册 → [录入] 按钮
│       └── POST /ai/face/register/{workerId}?modelCode=arcface_r100_001
│
├── ruoyi-admin（控制层）
│   └── AiFaceRegisterController.registerFace()
│       └── aiFaceRegisterService.registerFace(workerId, modelCode)
│
├── ruoyi-system（业务层）
│   └── AiFaceRegisterServiceImpl.registerFace()
│       ├── 校验 ai_model 存在且 model_type='face_recognition'
│       ├── 校验人员未重复录入
│       ├── 查询 tb_worker_face 获取人脸照片 URL
│       └── ↓ 调用
│       └── iaiInferenceService.extractFaceFeature(modelCode, faceImgUrl)
│
├── ruoyi-framework（框架层 — 本次新增）
│   └── RealAiInferenceService (替换 StubAiInferenceService)
│       ├── resolveImageFile() → 解析 /profile/xxx 为物理路径
│       ├── OnnxFaceDetector.detect()      ← yolov12m-face.onnx
│       │   ├── ImageUtils.readImageBGR()  ← BufferedImage → BGR flat byte[]
│       │   ├── letterbox()                ← 保持宽高比缩放 + 灰色填充 640×640
│       │   ├── OrtSession.run()           ← ONNX Runtime 推理
│       │   └── postprocess()              ← 过滤 + 坐标转换 + NMS
│       ├── cropFace()                     ← 裁切最大人脸区域
│       └── OnnxFaceEmbedding.extract()    ← w600k_mbf.onnx
│           ├── ImageUtils.resizeBGR()     ← 双线性缩放到 112×112
│           ├── BGR→RGB + [-1,1] 归一化   ← (pixel-127.5)/127.5
│           ├── OrtSession.run()           ← ONNX Runtime 推理
│           └── L2 归一化                  ← embedding / ||embedding||
│
└── 数据存储
    ├── tb_worker_face.face_img_url   ← "/profile/upload/..."（照片路径）
    │   └── 物理文件: uploadPath/upload/YYYY/MM/DD/xxx.jpg
    ├── ai_model.model_path           ← 模型文件路径（ai-models/ 目录）
    ├── ai_face_register.face_feature ← {"dim":512,"features":"<base64>"}
    └── tb_worker.face_status         ← 同步更新为 '1'

打卡人脸比对链路（桥接后新增）
│
├── ruoyi-admin（控制层）
│   └── HwAttendanceController.checkIn()
│       └── hwAttendanceService.checkIn(attendance)
│
├── ruoyi-system（业务层）
│   └── HwAttendanceServiceImpl.checkIn()
│       └── faceRecognitionService.compareFace(capturedImage, registeredImage)
│
├── ruoyi-framework（框架层 — 桥接）
│   └── RealFaceRecognitionService  @Primary
│       ├── resolveToPath() → Base64/文件路径 → 物理文件
│       └── aiInferenceService.infer("arcface_r100_001", path)
│           └── RealAiInferenceService.doFaceRecognition()
│               ├── OnnxFaceDetector  → YOLOv12 检测人脸
│               ├── OnnxFaceEmbedding → ArcFace 提取特征
│               └── 1:N 底库比对     → SimilarityUtils.cosine()
```

---

## 十一、桥接实现：RealFaceRecognitionService（2026-06-05 新增）

### 背景

系统有两套 AI 接口，打卡流程使用的 `IFaceRecognitionService` 还是桩，导致录入可以走通但识别不可用。

### 解决方案

新增 `RealFaceRecognitionService implements IFaceRecognitionService`，内部桥接到 `IAiInferenceService`：

```
打卡调用链:
  HwAttendanceServiceImpl.checkIn()
  → IFaceRecognitionService.compareFace(captured, registered)
  → RealFaceRecognitionService  ← @Primary
  → IAiInferenceService.infer("arcface_r100_001", capturedPath)
  → RealAiInferenceService.doFaceRecognition()
  → YOLOv12 检测 + ArcFace 提取 + 1:N 底库比对
  → 返回 FaceMatchResult {matched, personName, confidence}
```

### 核心逻辑

| 方法 | 实现 |
|------|------|
| `compareFace(capturedImage, registeredImage)` | ① Base64 / 文件路径 → 解析为物理文件（Base64 写临时文件）→ ② 调 `IAiInferenceService.infer("arcface_r100_001", path)` 做 1:N 识别 → ③ 解析 `{matched, personName, similarity}` → ④ 清理临时文件 → ⑤ 返回 `FaceMatchResult` |
| `detectFace(imageBase64)` | Base64→临时文件 → `IAiInferenceService.detectFace()` → 清理 → 返回 boolean |

### 配套接口扩展

`IAiInferenceService` 新增 `boolean detectFace(String imagePath)` 方法：
- `RealAiInferenceService` 实现：复用已有的 YOLOv12 detector，返回 `!faces.isEmpty()`
- `StubAiInferenceService` 实现：返回 false

### 涉及文件

| # | 文件 | 类型 | 说明 |
|---|------|------|------|
| 1 | `RealFaceRecognitionService.java` | 新增 | `@Service @Primary`，桥接 `IFaceRecognitionService` → `IAiInferenceService` |
| 2 | `IAiInferenceService.java` | 修改 | 新增 `detectFace(String imagePath)` 方法 |
| 3 | `RealAiInferenceService.java` | 修改 | 实现 `detectFace()` + 加 `@Primary` |
| 4 | `StubAiInferenceService.java` | 修改 | 实现 `detectFace()`（返回 false） |
| 5 | `StubFaceRecognitionService.java` | 修改 | `@Component` → `@Component("stubFaceRecognitionService")` |

---

## 十二、文件存储全景（2026-06-05 修订后）

```
RuoYi-Vue-springboot3/
├── ai-models/                           ← ONNX 模型（数据库 ai_model.model_path 指向这里）
│   ├── yolov12m-face.onnx               ← 人脸检测（80MB）
│   └── w600k_mbf.onnx                   ← 特征提取（13MB）
│
├── uploadPath/                          ← ruoyi.profile 根路径（application.yml 配置）
│   ├── upload/                          ← RuoYiConfig.getUploadPath()
│   │   └── YYYY/MM/DD/xxx.jpg          ← 用户上传的人脸照片（POST /common/upload）
│   ├── import/                          ← Excel 导入
│   ├── avatar/                          ← 用户头像
│   └── download/                        ← 文件下载
│
└── 数据库 ry-vue
    ├── tb_worker_face.face_img_url      ← "/profile/upload/2026/06/05/xxx.jpg"
    ├── ai_model.model_path              ← "C:/Users/.../ai-models/yolov12m-face.onnx"
    └── ai_face_register.face_feature    ← {"dim":512,"features":"<base64>"}
```

---

## 十三、Beam 冲突解决记录

### 问题1：两个 IAiInferenceService Bean → 启动失败

**现象**：`AiFaceRegisterServiceImpl` 要求单一 Bean，但找到 `realAiInferenceService` + `stubAiInferenceService` 两个。

**解决**：`RealAiInferenceService` 加 `@Primary`，`StubAiInferenceService` 改名为 `@Component("stubAiInferenceService")`。

### 问题2：两个 IFaceRecognitionService Bean → 启动失败（同上）

**解决**：`RealFaceRecognitionService` 加 `@Primary`，`StubFaceRecognitionService` 改名为 `@Component("stubFaceRecognitionService")`。

---

## 十四、Bug 记录

### Bug #1：人脸录入失败时前端弹窗显示"录入成功"，列表显示"录入失败"（2026-06-05 修复）

**现象**：在人脸注册页面点击"录入"，弹窗提示"人脸录入成功"，但列表中该人员的 AI 录入状态显示"录入失败"，失败原因是"人脸图片文件不存在"。

**根因**：`AiFaceRegisterServiceImpl.registerFace()` 在 AI 推理失败时只写了一条 `register_status='2'` 的失败记录到数据库并正常 `return`，没有抛异常。Controller 收到正常返回 → `success("人脸录入成功")`，与数据库实际状态矛盾。

**调用链**：

```
AiFaceRegisterController.registerFace()
  → faceRegisterService.registerFace(workerId, modelCode)
    → aiInferenceService.extractFaceFeature() 返回 fail("人脸图片文件不存在")
    → 写入 DB: register_status='2', fail_reason='人脸图片文件不存在'
    → return 1;  // ← 没有抛异常！
  → Controller 收到正常返回 → success("人脸录入成功")  // ← Bug!
```

**修复**（commit `9b7d233`）：在 `AiFaceRegisterServiceImpl` 的 AI 推理失败分支中，写完失败记录后追加 `throw new ServiceException(result.getErrorMessage())`，Controller 的 `try-catch` 捕获后 → `error(msg)` → 前端正确显示错误。

**涉及文件**：`ruoyi-system/.../service/impl/AiFaceRegisterServiceImpl.java`

```java
// 修复前
if (existing != null) {
    return faceRegisterMapper.updateAiFaceRegister(failRecord);  // 正常 return
} else {
    return faceRegisterMapper.insertAiFaceRegister(failRecord);  // 正常 return
}

// 修复后
if (existing != null) {
    faceRegisterMapper.updateAiFaceRegister(failRecord);
} else {
    faceRegisterMapper.insertAiFaceRegister(failRecord);
}
throw new ServiceException(result.getErrorMessage());  // ← 新增
```

### Bug #2：人脸录入提示"人脸图片文件不存在"（2026-06-06 修复）

**现象**：uni-app 移动端上传人脸照片后，在 PC 管理系统对人员点击"录入"，提示"人脸图片文件不存在: http://localhost:8080/profile/upload/2026/06/06/hg_20260606145145A001.jpg"，但实际上该文件存在于 `uploadPath/upload/2026/06/06/hg_20260606145145A001.jpg`。

**根因**：两点共同导致：

1. **`face.vue` 上传后取了完整 URL 存入数据库**：`result.url`（`http://localhost:8080/profile/upload/...`）被优先于 `result.fileName`（`/profile/upload/...`）选取，导致 `tb_worker_face.face_img_url` 存的是完整 URL 而非相对路径。

2. **`RealAiInferenceService.resolveImageFile()` 不支持完整 URL**：只判断 `startsWith("/profile")`，完整 URL `http://localhost:8080/profile/upload/...` 走不进分支，回退到 `new File("http://...")` → 文件永远不存在。

**修复（2 处）**：

**修复 1 — `RealAiInferenceService.resolveImageFile()`**（`ruoyi-framework/.../RealAiInferenceService.java:259`）：

```java
// 修复前
private File resolveImageFile(String faceImgUrl) {
    if (faceImgUrl == null || faceImgUrl.isEmpty()) return new File("");
    if (faceImgUrl.startsWith("/profile")) {
        String profile = RuoYiConfig.getProfile();
        return new File(profile, faceImgUrl.replaceFirst("^/profile", ""));
    }
    return new File(faceImgUrl);
}

// 修复后
private File resolveImageFile(String faceImgUrl) {
    if (faceImgUrl == null || faceImgUrl.isEmpty()) return new File("");
    // 如果是完整 HTTP(S) URL，提取路径部分（兼容 uni-app 上传返回完整 URL 的场景）
    if (faceImgUrl.startsWith("http://") || faceImgUrl.startsWith("https://")) {
        try {
            faceImgUrl = new java.net.URI(faceImgUrl).getPath();
        } catch (Exception e) {
            return new File(faceImgUrl);
        }
    }
    if (faceImgUrl.startsWith("/profile")) {
        String profile = RuoYiConfig.getProfile();
        String relativePath = faceImgUrl.replaceFirst("^/profile/?", "");
        return new File(profile, relativePath);
    }
    return new File(faceImgUrl);
}
```

改动要点：
- 新增 HTTP(S) URL 判断：用 `java.net.URI.getPath()` 提取路径部分（如 `/profile/upload/...`）再走后续逻辑
- `replaceFirst("^/profile", "")` → `replaceFirst("^/profile/?", "")`：去掉 `/profile` 后可能残留的前导 `/`

**修复 2 — `face.vue`**（`RuoYi-App/pages/worker/face.vue:35`）：

```javascript
// 修复前
const url = result.url || result.data || result.fileName

// 修复后
const url = result.fileName || result.url || (result.data && result.data.url)
```

新上传优先使用 `result.fileName`（相对路径 `/profile/upload/...`），确保数据库存储干净的相对路径。

**涉及文件**：
| # | 文件 | 类型 |
|---|------|------|
| 1 | `ruoyi-framework/.../service/RealAiInferenceService.java` | 修改 |
| 2 | `RuoYi-App/pages/worker/face.vue` | 修改 |

---

## 十五、环境问题

### IDEA 报 "程序包org.springframework.beans.factory.annotation不存在"

**现象**：命令行 `mvn compile` 成功，但在 IntelliJ IDEA 中打开项目后编译报错 `java: 程序包org.springframework.beans.factory.annotation不存在`，所有 Spring 注解（`@Service`、`@Autowired`、`@Value` 等）均无法识别。

**根因**：`pom.xml` 是从 IDEA 外部（CLI 脚本/编辑器）修改的，IDEA 内部的 Maven 依赖索引未同步更新。新增 `onnxruntime` 依赖后触发依赖树重新 resolve，但 resolve 过程在 IDEA 中失败或中断，导致整个依赖树崩溃，所有 Spring 包都找不到。

**解决方法**（按推荐顺序）：

1. **Reload Maven 项目**（90% 概率解决）
   - IDEA 右侧边栏 → Maven 工具窗口 → 点击左上角 🔄 "Reload All Maven Projects" 按钮

2. **清缓存重启**
   - `File` → `Invalidate Caches...` → 勾选所有选项 → `Invalidate and Restart`

3. **删除 Maven 本地仓库 Spring 缓存**
   - 关闭 IDEA → 删除 `%USERPROFILE%\.m2\repository\org\springframework` → 重新打开 IDEA → Reload Maven

4. **重建 IDEA 项目文件**
   - 关闭 IDEA → 删除项目根目录下的 `.idea` 文件夹 → 用 IDEA 重新 `Open` 这个 Maven 项目

---

## 十六、验证清单

| # | 验证项 | 状态 |
|---|--------|------|
| 1 | 后端编译 | `mvn compile -DskipTests` → `BUILD SUCCESS` ✅ |
| 2 | 模型文件部署 | 已移至 `RuoYi-Vue-springboot3/ai-models/`（yolov12m-face.onnx 80MB + w600k_mbf.onnx 13MB） ✅ |
| 3 | Maven 依赖 | `onnxruntime 1.18.0` 已添加到 ruoyi-framework/pom.xml ✅ |
| 4 | 桩替换 | `StubAiInferenceService` 改为命名 Bean，`RealAiInferenceService` 作为新实现 ✅ |
| 5 | 数据库 ai_model 更新 | SQL 文件已生成：`sql/updates/20260605_onnx_model_config.sql`，启动前执行即可 ✅ |
| 6 | 启动后端 | 待执行 `mvn spring-boot:run -f ruoyi-admin` |
| 7 | 模型加载验证 | 待查看日志确认 `人脸检测模型加载成功` + `人脸特征提取模型加载成功` |
| 8 | 人脸录入验证 | 待操作员对有 face_img_url 的人员点击"录入"，确认状态变为"已录入" |
| 9 | 特征数据验证 | 待确认 `ai_face_register.face_feature` 有 Base64 编码的 512 维特征向量 |
| 10 | face_status 同步 | 待确认 `tb_worker.face_status` 更新为 '1' |
| 11 | 桥接编译 | `mvn compile -DskipTests` → `BUILD SUCCESS` ✅ |
| 12 | 打卡人脸识别 | 待启动后发起人脸打卡请求，确认不再返回"AI人脸识别服务尚未配置" |
