package com.ruoyi.system.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * AI人脸注册表 ai_face_register
 *
 * @author ruoyi
 */
public class AiFaceRegister extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 注册记录ID */
    private Long registerId;

    /** 人员ID */
    private Long workerId;

    /** 人员姓名 */
    private String workerName;

    /** 人脸照片URL */
    private String faceImgUrl;

    /** 人脸特征向量（JSON） */
    private String faceFeature;

    /** 关联模型编码 */
    private String modelCode;

    /** 关联模型名称 */
    private String modelName;

    /** 录入状态 */
    private String registerStatus;

    /** 录入时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date registerTime;

    /** 录入失败原因 */
    private String failReason;

    // ==== tb_worker_face 联表字段（仅列表展示用，不持久化到本表） ====
    /** 人脸采集时间（来自 tb_worker_face.collect_time） */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date collectTime;

    /** 人员人脸录入状态（来自 tb_worker.face_status） */
    private String faceStatus;

    public Long getRegisterId() { return registerId; }
    public void setRegisterId(Long registerId) { this.registerId = registerId; }

    public Long getWorkerId() { return workerId; }
    public void setWorkerId(Long workerId) { this.workerId = workerId; }

    public String getWorkerName() { return workerName; }
    public void setWorkerName(String workerName) { this.workerName = workerName; }

    public String getFaceImgUrl() { return faceImgUrl; }
    public void setFaceImgUrl(String faceImgUrl) { this.faceImgUrl = faceImgUrl; }

    public String getFaceFeature() { return faceFeature; }
    public void setFaceFeature(String faceFeature) { this.faceFeature = faceFeature; }

    public String getModelCode() { return modelCode; }
    public void setModelCode(String modelCode) { this.modelCode = modelCode; }

    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }

    public String getRegisterStatus() { return registerStatus; }
    public void setRegisterStatus(String registerStatus) { this.registerStatus = registerStatus; }

    public Date getRegisterTime() { return registerTime; }
    public void setRegisterTime(Date registerTime) { this.registerTime = registerTime; }

    public String getFailReason() { return failReason; }
    public void setFailReason(String failReason) { this.failReason = failReason; }

    public Date getCollectTime() { return collectTime; }
    public void setCollectTime(Date collectTime) { this.collectTime = collectTime; }

    public String getFaceStatus() { return faceStatus; }
    public void setFaceStatus(String faceStatus) { this.faceStatus = faceStatus; }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("registerId", getRegisterId())
            .append("workerId", getWorkerId())
            .append("workerName", getWorkerName())
            .append("faceImgUrl", getFaceImgUrl())
            .append("modelCode", getModelCode())
            .append("modelName", getModelName())
            .append("registerStatus", getRegisterStatus())
            .append("registerTime", getRegisterTime())
            .append("failReason", getFailReason())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
