package com.ruoyi.framework.service;

import org.springframework.stereotype.Component;
import com.ruoyi.common.service.FaceMatchResult;
import com.ruoyi.common.service.IFaceRecognitionService;

/**
 * 人脸识别服务桩实现
 *
 * @author ruoyi
 */
@Component
public class StubFaceRecognitionService implements IFaceRecognitionService
{
    @Override
    public FaceMatchResult compareFace(String capturedImage, String registeredImage)
    {
        return FaceMatchResult.fail("AI人脸识别服务尚未配置");
    }

    @Override
    public boolean detectFace(String imageBase64)
    {
        return false;
    }
}
