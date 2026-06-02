package com.ruoyi.system.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 人员档案对象 tb_worker
 * 
 * @author ruoyi
 * @date 2026-06-01
 */
public class TbWorker extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 人员ID */
    private Long id;

    /** 姓名 */
    @Excel(name = "姓名")
    private String workerName;

    /** 手机号 */
    @Excel(name = "手机号")
    private String phone;

    /** 身份证号 */
    @Excel(name = "身份证号")
    private String idCard;

    /** 性别（字典 sys_user_sex：0男 1女 2未知） */
    @Excel(name = "性别", readConverterExp = "字=典,s=ys_user_sex：0男,1=女,2=未知")
    private String gender;

    /** 单位类型（字典 worker_unit_type：1管网 2第三方 3施工方） */
    @Excel(name = "单位类型")
    private String unitType;

    /** 人员状态（字典 worker_status：0在场 1离场 2禁用） */
    @Excel(name = "人员状态", readConverterExp = "字=典,w=orker_status：0在场,1=离场,2=禁用")
    private String status;

    /** 人脸录入状态（字典 worker_face_status：0未录入 1已录入） */
    @Excel(name = "人脸录入状态", readConverterExp = "字=典,w=orker_face_status：0未录入,1=已录入")
    private String faceStatus;

    /** 审核状态（字典 worker_audit_status：0待审核 1已通过 2已驳回 3已过期） */
    @Excel(name = "审核状态", readConverterExp = "字=典,w=orker_audit_status：0待审核,1=已通过,2=已驳回,3=已过期")
    private String auditStatus;

    /** 删除标志（0存在 2删除） */
    private String delFlag;

    public void setId(Long id) 
    {
        this.id = id;
    }

    public Long getId() 
    {
        return id;
    }

    public void setWorkerName(String workerName) 
    {
        this.workerName = workerName;
    }

    public String getWorkerName() 
    {
        return workerName;
    }

    public void setPhone(String phone) 
    {
        this.phone = phone;
    }

    public String getPhone() 
    {
        return phone;
    }

    public void setIdCard(String idCard) 
    {
        this.idCard = idCard;
    }

    public String getIdCard() 
    {
        return idCard;
    }

    public void setGender(String gender) 
    {
        this.gender = gender;
    }

    public String getGender() 
    {
        return gender;
    }

    public void setUnitType(String unitType)
    {
        this.unitType = unitType;
    }

    public String getUnitType()
    {
        return unitType;
    }

    public void setStatus(String status) 
    {
        this.status = status;
    }

    public String getStatus() 
    {
        return status;
    }

    public void setFaceStatus(String faceStatus) 
    {
        this.faceStatus = faceStatus;
    }

    public String getFaceStatus() 
    {
        return faceStatus;
    }

    public void setAuditStatus(String auditStatus) 
    {
        this.auditStatus = auditStatus;
    }

    public String getAuditStatus() 
    {
        return auditStatus;
    }

    public void setDelFlag(String delFlag) 
    {
        this.delFlag = delFlag;
    }

    public String getDelFlag() 
    {
        return delFlag;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("workerName", getWorkerName())
            .append("phone", getPhone())
            .append("idCard", getIdCard())
            .append("gender", getGender())
            .append("unitType", getUnitType())
            .append("status", getStatus())
            .append("faceStatus", getFaceStatus())
            .append("auditStatus", getAuditStatus())
            .append("delFlag", getDelFlag())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
