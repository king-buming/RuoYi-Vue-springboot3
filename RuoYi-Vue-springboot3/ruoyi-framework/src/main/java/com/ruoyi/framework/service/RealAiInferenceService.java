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
import org.springframework.context.annotation.Primary;
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
@Primary
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
    public boolean detectFace(String imagePath) {
        try {
            File imageFile = resolveImageFile(imagePath);
            if (!imageFile.exists()) return false;
            OnnxFaceDetector det = getDetector();
            if (det == null) return false;
            List<float[]> faces = det.detect(imageFile);
            return !faces.isEmpty();
        } catch (Exception e) {
            log.warn("人脸检测异常: {}", e.getMessage());
            return false;
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
