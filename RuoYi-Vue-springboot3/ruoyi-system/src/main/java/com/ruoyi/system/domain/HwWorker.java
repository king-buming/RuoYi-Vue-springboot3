package com.ruoyi.system.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 施工人员表 hw_worker
 *
 * @author ruoyi
 */
public class HwWorker extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 人员ID */
    private Long workerId;

    /** 姓名 */
    private String workerName;

    /** 身份证号 */
    private String idCard;

    /** 手机号 */
    private String phone;

    /** 人员角色 */
    private String roleType;

    /** 单位类型 */
    private String unitType;

    /** 固定工点 */
    private String isFixedSite;

    /** 打卡规则编码 */
    private String checkRule;

    /** 资质证件名称 */
    private String qualification;

    /** 资质证件上传URL */
    private String qualFileUrl;

    /** 资质审核状态 */
    private String qualStatus;

    /** 人脸底图URL */
    private String faceImage;

    /** 人脸注册状态 */
    private String faceStatus;

    /** 微信公众号openid */
    private String openId;

    /** 状态（0正常 1停用） */
    private String status;

    public Long getWorkerId()
    {
        return workerId;
    }

    public void setWorkerId(Long workerId)
    {
        this.workerId = workerId;
    }

    public String getWorkerName()
    {
        return workerName;
    }

    public void setWorkerName(String workerName)
    {
        this.workerName = workerName;
    }

    public String getIdCard()
    {
        return idCard;
    }

    public void setIdCard(String idCard)
    {
        this.idCard = idCard;
    }

    public String getPhone()
    {
        return phone;
    }

    public void setPhone(String phone)
    {
        this.phone = phone;
    }

    public String getRoleType()
    {
        return roleType;
    }

    public void setRoleType(String roleType)
    {
        this.roleType = roleType;
    }

    public String getUnitType()
    {
        return unitType;
    }

    public void setUnitType(String unitType)
    {
        this.unitType = unitType;
    }

    public String getIsFixedSite()
    {
        return isFixedSite;
    }

    public void setIsFixedSite(String isFixedSite)
    {
        this.isFixedSite = isFixedSite;
    }

    public String getCheckRule()
    {
        return checkRule;
    }

    public void setCheckRule(String checkRule)
    {
        this.checkRule = checkRule;
    }

    public String getQualification()
    {
        return qualification;
    }

    public void setQualification(String qualification)
    {
        this.qualification = qualification;
    }

    public String getQualFileUrl()
    {
        return qualFileUrl;
    }

    public void setQualFileUrl(String qualFileUrl)
    {
        this.qualFileUrl = qualFileUrl;
    }

    public String getQualStatus()
    {
        return qualStatus;
    }

    public void setQualStatus(String qualStatus)
    {
        this.qualStatus = qualStatus;
    }

    public String getFaceImage()
    {
        return faceImage;
    }

    public void setFaceImage(String faceImage)
    {
        this.faceImage = faceImage;
    }

    public String getFaceStatus()
    {
        return faceStatus;
    }

    public void setFaceStatus(String faceStatus)
    {
        this.faceStatus = faceStatus;
    }

    public String getOpenId()
    {
        return openId;
    }

    public void setOpenId(String openId)
    {
        this.openId = openId;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("workerId", getWorkerId())
            .append("workerName", getWorkerName())
            .append("idCard", getIdCard())
            .append("phone", getPhone())
            .append("roleType", getRoleType())
            .append("unitType", getUnitType())
            .append("isFixedSite", getIsFixedSite())
            .append("checkRule", getCheckRule())
            .append("qualification", getQualification())
            .append("qualFileUrl", getQualFileUrl())
            .append("qualStatus", getQualStatus())
            .append("faceImage", getFaceImage())
            .append("faceStatus", getFaceStatus())
            .append("openId", getOpenId())
            .append("status", getStatus())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
