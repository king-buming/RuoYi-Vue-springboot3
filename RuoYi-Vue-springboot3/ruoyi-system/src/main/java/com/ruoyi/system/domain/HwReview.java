package com.ruoyi.system.domain;

import java.util.Date;
import jakarta.validation.constraints.NotNull;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 作业审核表 hw_review
 *
 * @author ruoyi
 */
public class HwReview extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 审核ID */
    private Long reviewId;

    /** 关联作业计划ID */
    private Long planId;

    /** 项目名称（冗余） */
    private String planName;

    /** 作业类型（冗余） */
    private String workType;

    /** 施工单位（冗余） */
    private String constructionUnit;

    /** 申请人 */
    private String applicant;

    /** 申请时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date applyTime;

    /** 审核状态（0待审核 1已通过 2已驳回） */
    private String reviewStatus;

    /** 审核意见 */
    private String reviewOpinion;

    /** 审核人 */
    private String reviewer;

    /** 审核时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date reviewTime;

    public Long getReviewId()
    {
        return reviewId;
    }

    public void setReviewId(Long reviewId)
    {
        this.reviewId = reviewId;
    }

    @NotNull(message = "作业计划ID不能为空")
    public Long getPlanId()
    {
        return planId;
    }

    public void setPlanId(Long planId)
    {
        this.planId = planId;
    }

    public String getPlanName()
    {
        return planName;
    }

    public void setPlanName(String planName)
    {
        this.planName = planName;
    }

    public String getWorkType()
    {
        return workType;
    }

    public void setWorkType(String workType)
    {
        this.workType = workType;
    }

    public String getConstructionUnit()
    {
        return constructionUnit;
    }

    public void setConstructionUnit(String constructionUnit)
    {
        this.constructionUnit = constructionUnit;
    }

    public String getApplicant()
    {
        return applicant;
    }

    public void setApplicant(String applicant)
    {
        this.applicant = applicant;
    }

    public Date getApplyTime()
    {
        return applyTime;
    }

    public void setApplyTime(Date applyTime)
    {
        this.applyTime = applyTime;
    }

    public String getReviewStatus()
    {
        return reviewStatus;
    }

    public void setReviewStatus(String reviewStatus)
    {
        this.reviewStatus = reviewStatus;
    }

    public String getReviewOpinion()
    {
        return reviewOpinion;
    }

    public void setReviewOpinion(String reviewOpinion)
    {
        this.reviewOpinion = reviewOpinion;
    }

    public String getReviewer()
    {
        return reviewer;
    }

    public void setReviewer(String reviewer)
    {
        this.reviewer = reviewer;
    }

    public Date getReviewTime()
    {
        return reviewTime;
    }

    public void setReviewTime(Date reviewTime)
    {
        this.reviewTime = reviewTime;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("reviewId", getReviewId())
            .append("planId", getPlanId())
            .append("planName", getPlanName())
            .append("workType", getWorkType())
            .append("constructionUnit", getConstructionUnit())
            .append("applicant", getApplicant())
            .append("applyTime", getApplyTime())
            .append("reviewStatus", getReviewStatus())
            .append("reviewOpinion", getReviewOpinion())
            .append("reviewer", getReviewer())
            .append("reviewTime", getReviewTime())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
