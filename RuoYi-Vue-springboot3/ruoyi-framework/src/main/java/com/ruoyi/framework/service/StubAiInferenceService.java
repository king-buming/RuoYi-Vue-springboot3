package com.ruoyi.framework.service;

import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;
import com.ruoyi.common.service.AiInferenceResult;
import com.ruoyi.common.service.IAiInferenceService;

/**
 * AI推理服务桩实现
 * 后续接入真实AI模型时替换此实现类即可
 *
 * @author ruoyi
 */
@Component("stubAiInferenceService")
public class StubAiInferenceService implements IAiInferenceService
{
    @Override
    public boolean detectFace(String imagePath)
    {
        return false;
    }

    @Override
    public AiInferenceResult infer(String modelCode, String input)
    {
        return AiInferenceResult.fail(modelCode, "AI推理服务尚未配置，请先部署真实模型后替换桩实现");
    }

    @Override
    public AiInferenceResult extractFaceFeature(String modelCode, String faceImgUrl)
    {
        return AiInferenceResult.fail(modelCode, "AI推理服务尚未配置，无法提取人脸特征向量。请先部署真实模型后替换桩实现");
    }

    @Override
    public boolean isModelAvailable(String modelCode)
    {
        return false;
    }

    @Override
    public Map<String, Object> getModelStatus(String modelCode)
    {
        Map<String, Object> status = new HashMap<>();
        status.put("status", "not_configured");
        status.put("message", "AI推理服务尚未配置");
        return status;
    }
}
