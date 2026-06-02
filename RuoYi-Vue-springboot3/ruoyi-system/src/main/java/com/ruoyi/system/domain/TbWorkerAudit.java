package com.ruoyi.system.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 审核记录对象 tb_worker_audit
 *
 * @author ruoyi
 * @date 2026-06-01
 */
public class TbWorkerAudit extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long id;

    /** 审核业务类型（worker人员 / cert资质） */
    @Excel(name = "审核业务类型")
    private String bizType;

    /** 业务数据ID（人员ID 或 证件ID） */
    @Excel(name = "业务数据ID")
    private Long bizId;

    /** 关联人员ID */
    @Excel(name = "关联人员ID")
    private Long workerId;

    /** 审核结果（字典 worker_audit_status） */
    @Excel(name = "审核结果")
    private String auditStatus;

    /** 审核意见 / 驳回原因 */
    @Excel(name = "审核意见")
    private String auditOpinion;

    /** 审核人 */
    @Excel(name = "审核人")
    private String auditor;

    /** 审核时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "审核时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date auditTime;

    public void setId(Long id)
    {
        this.id = id;
    }

    public Long getId()
    {
        return id;
    }

    public void setBizType(String bizType)
    {
        this.bizType = bizType;
    }

    public String getBizType()
    {
        return bizType;
    }

    public void setBizId(Long bizId)
    {
        this.bizId = bizId;
    }

    public Long getBizId()
    {
        return bizId;
    }

    public void setWorkerId(Long workerId)
    {
        this.workerId = workerId;
    }

    public Long getWorkerId()
    {
        return workerId;
    }

    public void setAuditStatus(String auditStatus)
    {
        this.auditStatus = auditStatus;
    }

    public String getAuditStatus()
    {
        return auditStatus;
    }

    public void setAuditOpinion(String auditOpinion)
    {
        this.auditOpinion = auditOpinion;
    }

    public String getAuditOpinion()
    {
        return auditOpinion;
    }

    public void setAuditor(String auditor)
    {
        this.auditor = auditor;
    }

    public String getAuditor()
    {
        return auditor;
    }

    public void setAuditTime(Date auditTime)
    {
        this.auditTime = auditTime;
    }

    public Date getAuditTime()
    {
        return auditTime;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("bizType", getBizType())
            .append("bizId", getBizId())
            .append("workerId", getWorkerId())
            .append("auditStatus", getAuditStatus())
            .append("auditOpinion", getAuditOpinion())
            .append("auditor", getAuditor())
            .append("auditTime", getAuditTime())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
