package com.ruoyi.common.service;

/**
 * 人脸识别服务接口
 *
 * @author ruoyi
 */
public interface IFaceRecognitionService
{
    FaceMatchResult compareFace(String capturedImage, String registeredImage);
    boolean detectFace(String imageBase64);
}
