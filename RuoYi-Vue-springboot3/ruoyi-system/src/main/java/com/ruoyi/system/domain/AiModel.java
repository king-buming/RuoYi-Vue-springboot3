package com.ruoyi.system.domain;

import jakarta.validation.constraints.NotBlank;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * AI算法模型表 ai_model
 *
 * @author ruoyi
 */
public class AiModel extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 模型ID */
    private Long modelId;

    /** 模型名称 */
    private String modelName;

    /** 模型类型 */
    private String modelType;

    /** 模型编码 */
    private String modelCode;

    /** 版本号 */
    private String version;

    /** AI框架 */
    private String framework;

    /** 模型文件路径 */
    private String modelPath;

    /** 推理参数配置（JSON） */
    private String configJson;

    /** 评估指标（JSON） */
    private String metrics;

    /** 输入格式说明 */
    private String inputFormat;

    /** 输出格式说明 */
    private String outputFormat;

    /** 模型提供方 */
    private String provider;

    /** 缩略图URL */
    private String thumbnail;

    /** 状态 */
    private String status;

    /** 功能描述 */
    private String description;

    public Long getModelId() { return modelId; }
    public void setModelId(Long modelId) { this.modelId = modelId; }

    @NotBlank(message = "模型名称不能为空")
    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }

    @NotBlank(message = "模型类型不能为空")
    public String getModelType() { return modelType; }
    public void setModelType(String modelType) { this.modelType = modelType; }

    @NotBlank(message = "模型编码不能为空")
    public String getModelCode() { return modelCode; }
    public void setModelCode(String modelCode) { this.modelCode = modelCode; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public String getFramework() { return framework; }
    public void setFramework(String framework) { this.framework = framework; }

    public String getModelPath() { return modelPath; }
    public void setModelPath(String modelPath) { this.modelPath = modelPath; }

    public String getConfigJson() { return configJson; }
    public void setConfigJson(String configJson) { this.configJson = configJson; }

    public String getMetrics() { return metrics; }
    public void setMetrics(String metrics) { this.metrics = metrics; }

    public String getInputFormat() { return inputFormat; }
    public void setInputFormat(String inputFormat) { this.inputFormat = inputFormat; }

    public String getOutputFormat() { return outputFormat; }
    public void setOutputFormat(String outputFormat) { this.outputFormat = outputFormat; }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public String getThumbnail() { return thumbnail; }
    public void setThumbnail(String thumbnail) { this.thumbnail = thumbnail; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("modelId", getModelId())
            .append("modelName", getModelName())
            .append("modelType", getModelType())
            .append("modelCode", getModelCode())
            .append("version", getVersion())
            .append("framework", getFramework())
            .append("modelPath", getModelPath())
            .append("configJson", getConfigJson())
            .append("metrics", getMetrics())
            .append("inputFormat", getInputFormat())
            .append("outputFormat", getOutputFormat())
            .append("provider", getProvider())
            .append("status", getStatus())
            .append("description", getDescription())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
