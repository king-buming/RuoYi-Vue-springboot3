package com.ruoyi.framework.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.service.AiInferenceResult;
import com.ruoyi.common.service.FaceMatchResult;
import com.ruoyi.common.service.IAiInferenceService;
import com.ruoyi.common.service.IFaceRecognitionService;

/**
 * 真实人脸识别服务 —— 桥接 IFaceRecognitionService（打卡比对）到 IAiInferenceService（ONNX 推理）
 */
@Service
@Primary
public class RealFaceRecognitionService implements IFaceRecognitionService {

    private static final Logger log = LoggerFactory.getLogger(RealFaceRecognitionService.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    private static final String MODEL_CODE = "arcface_r100_001";
    private static final String BASE64_PREFIX = "data:image/";

    @Autowired
    private IAiInferenceService aiInferenceService;

    @Override
    public FaceMatchResult compareFace(String capturedImage, String registeredImage) {
        try {
            // 1. 将抓拍图解析为可用的文件路径
            String capturedPath = resolveToPath(capturedImage);

            // 2. 调用 AI 推理做 1:N 人脸识别
            AiInferenceResult result = aiInferenceService.infer(MODEL_CODE, capturedPath);

            // 3. 清理临时文件
            cleanupTemp(capturedImage, capturedPath);

            if (!result.isSuccess()) {
                return FaceMatchResult.fail(result.getErrorMessage());
            }

            // 4. 解析识别结果
            @SuppressWarnings("unchecked")
            Map<String, Object> map = mapper.readValue(result.getResultJson(), Map.class);

            Boolean matched = (Boolean) map.get("matched");
            if (matched == null || !matched) {
                return FaceMatchResult.fail("人脸不匹配，未在底库中找到该人员");
            }

            double confidence = ((Number) map.getOrDefault("similarity", 0)).doubleValue();
            String personName = (String) map.getOrDefault("personName", "");

            FaceMatchResult faceResult = FaceMatchResult.success(confidence);
            faceResult.setPersonName(personName);
            log.info("人脸比对成功: {} (confidence={})", personName, confidence);
            return faceResult;

        } catch (Exception e) {
            log.error("人脸比对异常", e);
            return FaceMatchResult.fail("人脸比对异常: " + e.getMessage());
        }
    }

    @Override
    public boolean detectFace(String imageBase64) {
        try {
            String imagePath = resolveToPath(imageBase64);
            boolean detected = aiInferenceService.detectFace(imagePath);
            cleanupTemp(imageBase64, imagePath);
            return detected;
        } catch (Exception e) {
            log.warn("人脸检测异常: {}", e.getMessage());
            return false;
        }
    }

    // ========== 辅助方法 ==========

    /**
     * 将输入（Base64 或文件路径）解析为物理文件路径
     */
    private String resolveToPath(String input) throws IOException {
        if (input == null || input.isEmpty()) {
            throw new IOException("输入图片为空");
        }
        // 已经是物理文件路径
        if (new File(input).exists()) {
            return input;
        }
        // Base64 data URL 或纯 Base64
        return base64ToTempFile(input);
    }

    /**
     * Base64 → 临时文件，返回路径
     */
    private String base64ToTempFile(String base64) throws IOException {
        byte[] data;
        String suffix = ".jpg";

        if (base64.startsWith(BASE64_PREFIX)) {
            // data:image/jpeg;base64,xxx  or  data:image/png;base64,xxx
            int commaIdx = base64.indexOf(',');
            if (commaIdx < 0) throw new IOException("无效的 Base64 格式");
            // 从头部提取格式
            String header = base64.substring(0, commaIdx);
            if (header.contains("png")) suffix = ".png";
            else if (header.contains("gif")) suffix = ".gif";
            data = Base64.getDecoder().decode(base64.substring(commaIdx + 1));
        } else {
            // 纯 Base64
            data = Base64.getDecoder().decode(base64);
        }

        File tempFile = File.createTempFile("face_capture_", suffix);
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            fos.write(data);
        }
        return tempFile.getAbsolutePath();
    }

    /**
     * 清理临时文件
     */
    private void cleanupTemp(String input, String resolvedPath) {
        if (input == null || input.equals(resolvedPath)) return;
        try {
            File f = new File(resolvedPath);
            if (f.exists() && f.getName().startsWith("face_capture_")) {
                f.delete();
            }
        } catch (Exception ignored) {}
    }
}
