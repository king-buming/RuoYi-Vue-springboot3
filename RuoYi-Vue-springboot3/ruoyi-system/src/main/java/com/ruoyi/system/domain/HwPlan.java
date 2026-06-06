package com.ruoyi.system.domain;

import java.math.BigDecimal;
import java.util.Date;
import jakarta.validation.constraints.NotBlank;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 作业计划表 hw_plan
 *
 * @author ruoyi
 */
public class HwPlan extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 计划ID */
    private Long planId;

    /** 市/县 */
    private String cityCounty;

    /** 施工点 */
    private String constructionSite;

    /** 施工点纬度 */
    private BigDecimal siteLatitude;

    /** 施工点经度 */
    private BigDecimal siteLongitude;

    /** 计划作业时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date planWorkTime;

    /** 项目名称 */
    private String projectName;

    /** 作业类型 */
    private String workType;

    /** 施工单位 */
    private String constructionUnit;

    /** 参与施工各类人员 */
    private String workers;

    /** 作业内容 */
    private String workContent;

    /** 状态（1待执行 2进行中 3已完成 4已取消） */
    private String status;

    public Long getPlanId()
    {
        return planId;
    }

    public void setPlanId(Long planId)
    {
        this.planId = planId;
    }

    public String getCityCounty()
    {
        return cityCounty;
    }

    public void setCityCounty(String cityCounty)
    {
        this.cityCounty = cityCounty;
    }

    public String getConstructionSite()
    {
        return constructionSite;
    }

    public void setConstructionSite(String constructionSite)
    {
        this.constructionSite = constructionSite;
    }

    public BigDecimal getSiteLatitude()
    {
        return siteLatitude;
    }

    public void setSiteLatitude(BigDecimal siteLatitude)
    {
        this.siteLatitude = siteLatitude;
    }

    public BigDecimal getSiteLongitude()
    {
        return siteLongitude;
    }

    public void setSiteLongitude(BigDecimal siteLongitude)
    {
        this.siteLongitude = siteLongitude;
    }

    public Date getPlanWorkTime()
    {
        return planWorkTime;
    }

    public void setPlanWorkTime(Date planWorkTime)
    {
        this.planWorkTime = planWorkTime;
    }

    @NotBlank(message = "项目名称不能为空")
    public String getProjectName()
    {
        return projectName;
    }

    public void setProjectName(String projectName)
    {
        this.projectName = projectName;
    }

    @NotBlank(message = "作业类型不能为空")
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

    public String getWorkers()
    {
        return workers;
    }

    public void setWorkers(String workers)
    {
        this.workers = workers;
    }

    public String getWorkContent()
    {
        return workContent;
    }

    public void setWorkContent(String workContent)
    {
        this.workContent = workContent;
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
            .append("planId", getPlanId())
            .append("cityCounty", getCityCounty())
            .append("constructionSite", getConstructionSite())
            .append("siteLatitude", getSiteLatitude())
            .append("siteLongitude", getSiteLongitude())
            .append("planWorkTime", getPlanWorkTime())
            .append("projectName", getProjectName())
            .append("workType", getWorkType())
            .append("constructionUnit", getConstructionUnit())
            .append("workers", getWorkers())
            .append("workContent", getWorkContent())
            .append("status", getStatus())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
