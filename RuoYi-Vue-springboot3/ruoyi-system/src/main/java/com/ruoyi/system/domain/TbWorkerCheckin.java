package com.ruoyi.system.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 打卡记录对象 tb_worker_checkin
 *
 * @author ruoyi
 * @date 2026-06-02
 */
public class TbWorkerCheckin extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long id;

    @Excel(name = "人员ID")
    private Long workerId;

    @Excel(name = "打卡时角色ID")
    private Long roleId;

    /** 打卡类型（字典 worker_check_type：1签到 2签退 3点到） */
    @Excel(name = "打卡类型")
    private String checkType;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "打卡时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date checkTime;

    /** 打卡方式（AI / 公众号 / 手动） */
    @Excel(name = "打卡方式")
    private String checkMethod;

    @Excel(name = "工点ID")
    private Long siteId;

    private java.math.BigDecimal latitude;
    private java.math.BigDecimal longitude;

    @Excel(name = "现场照片URL")
    private String photoUrl;

    /** AI识别结果（JSON，后续阶段） */
    private String aiResult;

    /** 安全帽（0未戴 1已戴） */
    @Excel(name = "安全帽")
    private String helmetFlag;

    /** 反光衣（0未穿 1已穿） */
    @Excel(name = "反光衣")
    private String vestFlag;

    // getters & setters
    public void setId(Long id) { this.id = id; }
    public Long getId() { return id; }

    public void setWorkerId(Long workerId) { this.workerId = workerId; }
    public Long getWorkerId() { return workerId; }

    public void setRoleId(Long roleId) { this.roleId = roleId; }
    public Long getRoleId() { return roleId; }

    public void setCheckType(String checkType) { this.checkType = checkType; }
    public String getCheckType() { return checkType; }

    public void setCheckTime(Date checkTime) { this.checkTime = checkTime; }
    public Date getCheckTime() { return checkTime; }

    public void setCheckMethod(String checkMethod) { this.checkMethod = checkMethod; }
    public String getCheckMethod() { return checkMethod; }

    public void setSiteId(Long siteId) { this.siteId = siteId; }
    public Long getSiteId() { return siteId; }
    public void setLatitude(java.math.BigDecimal v) { this.latitude = v; }
    public java.math.BigDecimal getLatitude() { return latitude; }
    public void setLongitude(java.math.BigDecimal v) { this.longitude = v; }
    public java.math.BigDecimal getLongitude() { return longitude; }

    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }
    public String getPhotoUrl() { return photoUrl; }

    public void setAiResult(String aiResult) { this.aiResult = aiResult; }
    public String getAiResult() { return aiResult; }

    public void setHelmetFlag(String helmetFlag) { this.helmetFlag = helmetFlag; }
    public String getHelmetFlag() { return helmetFlag; }

    public void setVestFlag(String vestFlag) { this.vestFlag = vestFlag; }
    public String getVestFlag() { return vestFlag; }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("workerId", getWorkerId())
            .append("roleId", getRoleId())
            .append("checkType", getCheckType())
            .append("checkTime", getCheckTime())
            .append("checkMethod", getCheckMethod())
            .append("siteId", getSiteId())
            .append("photoUrl", getPhotoUrl())
            .append("helmetFlag", getHelmetFlag())
            .append("vestFlag", getVestFlag())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("remark", getRemark())
            .toString();
    }
}
