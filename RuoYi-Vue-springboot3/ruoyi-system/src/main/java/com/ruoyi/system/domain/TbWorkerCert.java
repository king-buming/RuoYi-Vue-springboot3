package com.ruoyi.system.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 资质证件对象 tb_worker_cert
 *
 * @author ruoyi
 * @date 2026-06-01
 */
public class TbWorkerCert extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long id;

    /** 人员ID */
    @Excel(name = "人员ID")
    private Long workerId;

    /** 证件类型（字典 worker_cert_type） */
    @Excel(name = "证件类型")
    private String certType;

    /** 证件编号 */
    @Excel(name = "证件编号")
    private String certNo;

    /** 发证日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "发证日期", width = 30, dateFormat = "yyyy-MM-dd")
    private Date issueDate;

    /** 过期日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "过期日期", width = 30, dateFormat = "yyyy-MM-dd")
    private Date expireDate;

    /** 证件图片URL */
    @Excel(name = "证件图片URL")
    private String certImg;

    /** 审核状态（字典 worker_audit_status） */
    @Excel(name = "审核状态")
    private String auditStatus;

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

    public void setCertType(String certType)
    {
        this.certType = certType;
    }

    public String getCertType()
    {
        return certType;
    }

    public void setCertNo(String certNo)
    {
        this.certNo = certNo;
    }

    public String getCertNo()
    {
        return certNo;
    }

    public void setIssueDate(Date issueDate)
    {
        this.issueDate = issueDate;
    }

    public Date getIssueDate()
    {
        return issueDate;
    }

    public void setExpireDate(Date expireDate)
    {
        this.expireDate = expireDate;
    }

    public Date getExpireDate()
    {
        return expireDate;
    }

    public void setCertImg(String certImg)
    {
        this.certImg = certImg;
    }

    public String getCertImg()
    {
        return certImg;
    }

    public void setAuditStatus(String auditStatus)
    {
        this.auditStatus = auditStatus;
    }

    public String getAuditStatus()
    {
        return auditStatus;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("workerId", getWorkerId())
            .append("certType", getCertType())
            .append("certNo", getCertNo())
            .append("issueDate", getIssueDate())
            .append("expireDate", getExpireDate())
            .append("certImg", getCertImg())
            .append("auditStatus", getAuditStatus())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
