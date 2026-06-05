package com.ruoyi.common.service;

import java.util.Map;

/**
 * AI推理服务接口
 * 当前为桩实现，后续接入真实AI模型时替换实现类即可
 *
 * @author ruoyi
 */
public interface IAiInferenceService
{
    /**
     * 执行模型推理
     * @param modelCode 模型编码
     * @param input 输入数据（图片base64、图片URL等）
     * @return 推理结果
     */
    AiInferenceResult infer(String modelCode, String input);

    /**
     * 从人脸图片提取特征向量（供人脸注册使用）
     * @param modelCode 人脸匹配模型编码
     * @param faceImgUrl 人脸图片URL
     * @return 推理结果，resultJson 包含特征向量JSON
     */
    AiInferenceResult extractFaceFeature(String modelCode, String faceImgUrl);

    /**
     * 检测图片中是否含有人脸
     * @param imagePath 图片文件路径
     * @return true检测到人脸 false未检测到
     */
    boolean detectFace(String imagePath);

    /**
     * 检测模型是否可用
     * @param modelCode 模型编码
     * @return true可用 false不可用
     */
    boolean isModelAvailable(String modelCode);

    /**
     * 获取模型状态信息
     * @param modelCode 模型编码
     * @return 模型状态信息Map
     */
    Map<String, Object> getModelStatus(String modelCode);
}
