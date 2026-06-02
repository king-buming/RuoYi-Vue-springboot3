package com.ruoyi.system.domain;

import java.util.Date;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 打卡记录表 hw_attendance
 *
 * @author ruoyi
 */
public class HwAttendance extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 打卡记录ID */
    private Long attendanceId;

    /** 关联作业计划ID */
    private Long planId;

    /** 打卡人员用户ID */
    private Long userId;

    /** 打卡人员姓名 */
    private String userName;

    /** 打卡类型 */
    private String checkType;

    /** 打卡方式 */
    private String checkMethod;

    /** 打卡时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date checkTime;

    /** 打卡位置 */
    private String location;

    /** 打卡状态 */
    private String checkStatus;

    /** 失败原因 */
    private String failReason;

    /** 人脸抓拍图URL */
    private String faceImage;

    public Long getAttendanceId()
    {
        return attendanceId;
    }

    public void setAttendanceId(Long attendanceId)
    {
        this.attendanceId = attendanceId;
    }

    @NotNull(message = "作业计划不能为空")
    public Long getPlanId()
    {
        return planId;
    }

    public void setPlanId(Long planId)
    {
        this.planId = planId;
    }

    public Long getUserId()
    {
        return userId;
    }

    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

    public String getUserName()
    {
        return userName;
    }

    public void setUserName(String userName)
    {
        this.userName = userName;
    }

    @NotBlank(message = "打卡类型不能为空")
    public String getCheckType()
    {
        return checkType;
    }

    public void setCheckType(String checkType)
    {
        this.checkType = checkType;
    }

    public String getCheckMethod()
    {
        return checkMethod;
    }

    public void setCheckMethod(String checkMethod)
    {
        this.checkMethod = checkMethod;
    }

    public Date getCheckTime()
    {
        return checkTime;
    }

    public void setCheckTime(Date checkTime)
    {
        this.checkTime = checkTime;
    }

    public String getLocation()
    {
        return location;
    }

    public void setLocation(String location)
    {
        this.location = location;
    }

    public String getCheckStatus()
    {
        return checkStatus;
    }

    public void setCheckStatus(String checkStatus)
    {
        this.checkStatus = checkStatus;
    }

    public String getFailReason()
    {
        return failReason;
    }

    public void setFailReason(String failReason)
    {
        this.failReason = failReason;
    }

    public String getFaceImage()
    {
        return faceImage;
    }

    public void setFaceImage(String faceImage)
    {
        this.faceImage = faceImage;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("attendanceId", getAttendanceId())
            .append("planId", getPlanId())
            .append("userId", getUserId())
            .append("userName", getUserName())
            .append("checkType", getCheckType())
            .append("checkMethod", getCheckMethod())
            .append("checkTime", getCheckTime())
            .append("location", getLocation())
            .append("checkStatus", getCheckStatus())
            .append("failReason", getFailReason())
            .append("faceImage", getFaceImage())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
