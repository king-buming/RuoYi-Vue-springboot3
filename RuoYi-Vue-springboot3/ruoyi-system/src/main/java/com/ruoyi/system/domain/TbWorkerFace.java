package com.ruoyi.system.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 人脸信息对象 tb_worker_face
 *
 * @author ruoyi
 * @date 2026-06-01
 */
public class TbWorkerFace extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long id;

    /** 人员ID */
    @Excel(name = "人员ID")
    private Long workerId;

    /** 人脸照片URL */
    @Excel(name = "人脸照片URL")
    private String faceImgUrl;

    /** 人脸特征值（当前阶段可空，后续 AI 生成） */
    private String faceFeature;

    /** 采集时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "采集时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date collectTime;

    public void setId(Long id)
    {
        this.id = id;
    }

    public Long getId()
    {
        return id;
    }

    public void setWorkerId(Long workerId)
    {
        this.workerId = workerId;
    }

    public Long getWorkerId()
    {
        return workerId;
    }

    public void setFaceImgUrl(String faceImgUrl)
    {
        this.faceImgUrl = faceImgUrl;
    }

    public String getFaceImgUrl()
    {
        return faceImgUrl;
    }

    public void setFaceFeature(String faceFeature)
    {
        this.faceFeature = faceFeature;
    }

    public String getFaceFeature()
    {
        return faceFeature;
    }

    public void setCollectTime(Date collectTime)
    {
        this.collectTime = collectTime;
    }

    public Date getCollectTime()
    {
        return collectTime;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("workerId", getWorkerId())
            .append("faceImgUrl", getFaceImgUrl())
            .append("faceFeature", getFaceFeature())
            .append("collectTime", getCollectTime())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
