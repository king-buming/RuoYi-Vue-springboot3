# YOLOv12 + ArcFace 多人脸识别改造规格书 (V2)

> **请自动完成本文档中的所有开发任务，无需逐步确认。** 按照以下规格修改代码后，报告完成情况。

---

## 一、背景与目标

### 1.1 V1 局限

当前系统只处理**单张最大人脸**：

```java
// RealAiInferenceService.java — 两处都只取最大的脸
float[] bestFace = faces.stream()
    .max((a, b) -> Float.compare(areaA, areaB)).get();  // ← 只取一个
```

| 能力 | V1 状态 |
|------|---------|
| 多脸检测 | ✅ YOLOv12 能检出所有人脸 (`List<float[]>`) |
| 多脸识别 | ❌ 只识别面积最大的那张脸 |
| 适用场景 | 单人打卡（自拍）、单人注册（证件照） |

### 1.2 V2 目标

支持**视频帧 / 多人合影**中识别所有人脸：

```
视频帧 → YOLOv12 检测 N 张脸 → 每张脸提取特征 → 每张脸 1:N 比对 → 返回所有识别结果
```

### 1.3 V1 兼容性

**所有 V1 方法和接口保持不变**。V2 仅新增方法，不修改已有签名。

---

## 二、改造方案

### 2.1 接口扩展：IAiInferenceService 新增方法

**文件**：`ruoyi-common/src/main/java/com/ruoyi/common/service/IAiInferenceService.java`

新增方法：

```java
/**
 * 多人脸识别：检测图片中所有人脸，逐一与底库比对
 * @param modelCode 人脸匹配模型编码（如 arcface_r100_001）
 * @param imagePath 图片文件路径
 * @return 推理结果，resultJson 包含所有人脸的识别结果
 */
AiInferenceResult inferMultiFace(String modelCode, String imagePath);
```

> `detectFace()` 已在 V1 中新增，无需重复。

---

### 2.2 桩实现：StubAiInferenceService

**文件**：`ruoyi-framework/src/main/java/com/ruoyi/framework/service/StubAiInferenceService.java`

新增方法实现：

```java
@Override
public AiInferenceResult inferMultiFace(String modelCode, String imagePath)
{
    return AiInferenceResult.fail(modelCode, "AI推理服务尚未配置");
}
```

---

### 2.3 核心实现：RealAiInferenceService 新增 doMultiFaceRecognition

**文件**：`ruoyi-framework/src/main/java/com/ruoyi/framework/service/RealAiInferenceService.java`

#### 2.3.1 新增入口方法

```java
@Override
public AiInferenceResult inferMultiFace(String modelCode, String imagePath) {
    long start = System.currentTimeMillis();
    try {
        AiModel model = aiModelMapper.selectAiModelByCode(modelCode);
        if (model == null) {
            return AiInferenceResult.fail(modelCode, "模型不存在: " + modelCode);
        }
        if (!"face_recognition".equals(model.getModelType())) {
            return AiInferenceResult.fail(modelCode, "模型类型不支持多人脸识别: " + model.getModelType());
        }
        return doMultiFaceRecognition(modelCode, imagePath, start);
    } catch (Exception e) {
        log.error("多人脸识别失败: modelCode={}", modelCode, e);
        return AiInferenceResult.fail(modelCode, "多人脸识别异常: " + e.getMessage());
    }
}
```

#### 2.3.2 核心逻辑：遍历所有人脸

```java
/**
 * 多人脸识别：检测所有人脸 → 逐一提取特征 → 逐一 1:N 比对
 */
private AiInferenceResult doMultiFaceRecognition(String modelCode, String imagePath, long start) {
    try {
        // 1. 读取图片 + 检测所有人脸
        File imageFile = resolveImageFile(imagePath);
        if (!imageFile.exists()) {
            return AiInferenceResult.fail(modelCode, "图片文件不存在: " + imagePath);
        }

        OnnxFaceDetector det = getDetector();
        OnnxFaceEmbedding emb = getEmbedding(modelCode);
        if (det == null || emb == null) {
            return AiInferenceResult.fail(modelCode, "模型未就绪");
        }

        // V2 关键改动：不再只取一张脸，而是遍历全部
        ImageUtils.BgrImage img = ImageUtils.readImageBGR(imageFile);
        List<float[]> allFaces = det.detect(img.pixels, img.h, img.w);
        
        if (allFaces.isEmpty()) {
            return AiInferenceResult.fail(modelCode, "未检测到人脸");
        }

        // 2. 预加载底库（只查一次，所有人脸共用）
        List<AiFaceRegister> registered = aiFaceRegisterMapper
            .selectFaceRegisterList(null, "1");

        // 3. 遍历每张脸
        List<Map<String, Object>> faceResults = new ArrayList<>();
        for (int i = 0; i < allFaces.size(); i++) {
            float[] face = allFaces.get(i);
            Map<String, Object> faceResult = new HashMap<>();
            
            // 边界框信息
            faceResult.put("faceIndex", i);
            faceResult.put("bbox", new double[]{
                Math.round(face[0] * 100.0) / 100.0,
                Math.round(face[1] * 100.0) / 100.0,
                Math.round(face[2] * 100.0) / 100.0,
                Math.round(face[3] * 100.0) / 100.0
            });
            faceResult.put("detConfidence", Math.round(face[4] * 1000.0) / 1000.0);

            try {
                // 裁切 + 提取特征
                ImageUtils.BgrImage crop = cropFace(img, face);
                float[] queryEmbedding = emb.extract(crop.pixels, crop.h, crop.w);

                // 1:N 比对
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

                if (bestSim >= similarityThreshold && bestMatch != null) {
                    faceResult.put("matched", true);
                    faceResult.put("workerId", bestWorkerId);
                    faceResult.put("personName", bestMatch);
                    faceResult.put("similarity", Math.round(bestSim * 10000.0) / 10000.0);
                } else {
                    faceResult.put("matched", false);
                    faceResult.put("bestSimilarity", Math.round(bestSim * 10000.0) / 10000.0);
                }
            } catch (Exception e) {
                faceResult.put("matched", false);
                faceResult.put("error", e.getMessage());
            }

            faceResults.add(faceResult);
        }

        // 4. 汇总结果
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("faces", faceResults);
        resultMap.put("faceCount", allFaces.size());
        resultMap.put("matchedCount", faceResults.stream()
            .filter(f -> Boolean.TRUE.equals(f.get("matched"))).count());

        long elapsed = System.currentTimeMillis() - start;
        log.info("多人脸识别完成: {} 张脸, {} 人匹配, time={}ms",
            allFaces.size(), resultMap.get("matchedCount"), elapsed);

        String resultJson = mapper.writeValueAsString(resultMap);
        return AiInferenceResult.success(modelCode, resultJson, elapsed);

    } catch (Exception e) {
        log.error("多人脸识别异常", e);
        return AiInferenceResult.fail(modelCode, "多人脸识别异常: " + e.getMessage());
    }
}
```

#### 2.3.3 需要新增的 import

```java
import java.util.ArrayList;
// List, Map 已在 V1 中 import
```

---

### 2.4 桥接层：RealFaceRecognitionService 新增多人脸接口

**文件**：`ruoyi-framework/src/main/java/com/ruoyi/framework/service/RealFaceRecognitionService.java`

IFaceRecognitionService 接口不需要改（打卡仍是单人场景）。但为视频场景预留，新增公开方法：

```java
/**
 * 多人脸识别：识别图片中所有人脸
 * @param imageInput Base64 或文件路径
 * @return JSON 字符串: {"faces": [...], "faceCount": N, "matchedCount": M}
 */
public String recognizeAllFaces(String imageInput) {
    try {
        String imagePath = resolveToPath(imageInput);
        AiInferenceResult result = aiInferenceService.inferMultiFace(MODEL_CODE, imagePath);
        cleanupTemp(imageInput, imagePath);
        
        if (!result.isSuccess()) {
            return "{\"error\": \"" + result.getErrorMessage() + "\"}";
        }
        return result.getResultJson();
    } catch (Exception e) {
        log.error("多人脸识别失败", e);
        return "{\"error\": \"" + e.getMessage() + "\"}";
    }
}
```

> 此方法不是接口实现，是对外暴露的工具方法，供未来的视频分析 Controller 调用。

---

## 三、V1 vs V2 对比

| 维度 | V1 | V2 |
|------|-----|-----|
| 检测 | YOLOv12 检出所有人脸 | 不变 |
| 人脸选择 | 只取面积最大的 1 张 | **遍历全部 N 张** |
| 特征提取 | 1 次 ArcFace 推理 | **N 次 ArcFace 推理** |
| 底库比对 | 1 次 1:N | **N 次 1:N**（底库只查一次） |
| 返回结果 | `{matched, personName, similarity}` | **`{faces: [{bbox, matched, personName, similarity}, ...], faceCount, matchedCount}`** |
| 耗时 | ~500ms | ~500ms + (N-1)×200ms |
| 适用场景 | 单人打卡 | **视频监控、多人合影** |

### 底库查询优化

V1 每次 `doFaceRecognition` 都查一次 DB。V2 多人脸场景中所有脸共用同一个底库，**只查一次 DB**：

```java
// V1: 每次比对都查（在 doFaceRecognition 内部）
List<AiFaceRegister> registered = aiFaceRegisterMapper.selectFaceRegisterList(null, "1");

// V2: 提前查一次，所有脸复用
List<AiFaceRegister> registered = ...;  // 查一次
for (float[] face : allFaces) {
    // N 张脸都用这个 registered 列表比对
}
```

---

## 四、返回 JSON 格式

### 单人脸 (V1, infer)

```json
{
  "matched": true,
  "workerId": 1,
  "personName": "张三",
  "confidence": 0.72,
  "similarity": 0.72
}
```

### 多人脸 (V2, inferMultiFace)

```json
{
  "faces": [
    {
      "faceIndex": 0,
      "bbox": [120.5, 80.3, 180.2, 240.1],
      "detConfidence": 0.95,
      "matched": true,
      "workerId": 1,
      "personName": "张三",
      "similarity": 0.72
    },
    {
      "faceIndex": 1,
      "bbox": [350.0, 60.5, 420.8, 200.3],
      "detConfidence": 0.88,
      "matched": true,
      "workerId": 2,
      "personName": "李四",
      "similarity": 0.65
    },
    {
      "faceIndex": 2,
      "bbox": [500.1, 100.2, 560.5, 220.7],
      "detConfidence": 0.45,
      "matched": false,
      "bestSimilarity": 0.12
    }
  ],
  "faceCount": 3,
  "matchedCount": 2
}
```

---

## 五、你需要创建/修改的全部内容

### 修改文件（4 个）

| # | 文件 | 改动 | 说明 |
|---|------|------|------|
| 1 | `IAiInferenceService.java` | 新增 `inferMultiFace(String modelCode, String imagePath)` | 接口扩展 |
| 2 | `StubAiInferenceService.java` | 实现 `inferMultiFace()` 返回 fail | 桩实现 |
| 3 | `RealAiInferenceService.java` | 新增 `inferMultiFace()` + `doMultiFaceRecognition()` | 核心逻辑 |
| 4 | `RealFaceRecognitionService.java` | 新增 `recognizeAllFaces(String imageInput)` | 桥接工具方法 |

### 不改动的文件

| 文件 | 原因 |
|------|------|
| `IFaceRecognitionService.java` | 打卡仍是单人场景，不需要多脸接口 |
| `OnnxFaceDetector.java` | 已返回 `List<float[]>`，多脸检测已支持 |
| `OnnxFaceEmbedding.java` | 单次推理不变，多脸场景重复调用即可 |
| `ImageUtils.java` | 不需要改 |
| `SimilarityUtils.java` | 不需要改 |
| `AiFaceRegisterServiceImpl.java` | 录入仍是单人场景 |
| Controller 层 | V2 新增的入口由调用方决定，暂不绑定特定 Controller |

---

## 六、调用示例（供未来集成参考）

### 6.1 视频帧分析（后端调用）

```java
@Autowired
private RealFaceRecognitionService faceRecognitionService;

// 视频帧 → Base64
String frameBase64 = "data:image/jpeg;base64,/9j/4AAQ...";

// 识别帧中所有人
String resultJson = faceRecognitionService.recognizeAllFaces(frameBase64);

// 解析结果
// → {"faces": [...], "faceCount": 3, "matchedCount": 2}
```

### 6.2 通过 Controller 暴露 API（示例）

```java
@RestController
@RequestMapping("/ai/face")
public class AiFaceRecognitionController extends BaseController {

    @Autowired
    private IAiInferenceService aiInferenceService;

    /**
     * 多人脸识别
     * POST /ai/face/recognizeAll
     * Body: {"imagePath": "D:/video/frame_001.jpg", "modelCode": "arcface_r100_001"}
     */
    @PostMapping("/recognizeAll")
    public AjaxResult recognizeAll(@RequestBody Map<String, String> params) {
        String imagePath = params.get("imagePath");
        String modelCode = params.getOrDefault("modelCode", "arcface_r100_001");
        AiInferenceResult result = aiInferenceService.inferMultiFace(modelCode, imagePath);
        if (result.isSuccess()) {
            return success(result.getResultJson());
        }
        return error(result.getErrorMessage());
    }
}
```

---

## 七、验证清单

| # | 验证项 | 方法 |
|---|--------|------|
| 1 | 编译通过 | `mvn compile -DskipTests` → BUILD SUCCESS |
| 2 | 单人多脸图片 | 准备一张含 3 人的合影，调 `inferMultiFace()`，确认返回 3 条结果 |
| 3 | 单人图片 | 准备一张单人照片，确认 `faceCount=1`，结果与 V1 `infer()` 一致 |
| 4 | 无脸图片 | 确认返回 fail("未检测到人脸") |
| 5 | 性能 | 3 人脸图片，总耗时应在 500ms + 2×200ms ≈ 1s 以内 |
| 6 | V1 兼容 | `extractFaceFeature()` 和 `infer()` 行为不变 |
| 7 | 底库优化 | 多人脸场景日志确认只查了一次 `selectFaceRegisterList` |
