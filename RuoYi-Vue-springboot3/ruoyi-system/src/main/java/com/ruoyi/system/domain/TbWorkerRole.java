package com.ruoyi.system.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 角色规则对象 tb_worker_role
 *
 * @author ruoyi
 * @date 2026-06-01
 */
public class TbWorkerRole extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 角色规则ID */
    private Long id;

    /** 角色编码 */
    @Excel(name = "角色编码")
    private String roleCode;

    /** 角色名称 */
    @Excel(name = "角色名称")
    private String roleName;

    /** 单位类型（字典 worker_unit_type：1管网 2第三方 3施工方） */
    @Excel(name = "单位类型")
    private String unitType;

    /** 是否固定工点（0否 1是） */
    @Excel(name = "是否固定工点", readConverterExp = "0=否,1=是")
    private String fixedSiteFlag;

    /** 是否需要签到（0否 1是） */
    @Excel(name = "是否需要签到", readConverterExp = "0=否,1=是")
    private String needSignIn;

    /** 是否需要签退（0否 1是） */
    @Excel(name = "是否需要签退", readConverterExp = "0=否,1=是")
    private String needSignOut;

    /** 是否需要点到（0否 1是） */
    @Excel(name = "是否需要点到", readConverterExp = "0=否,1=是")
    private String needHourlyCheck;

    /** 点到间隔（分钟） */
    @Excel(name = "点到间隔(分钟)")
    private Integer hourlyInterval;

    /** 是否需要资质（0否 1是） */
    @Excel(name = "是否需要资质", readConverterExp = "0=否,1=是")
    private String needCert;

    /** 所需资质类型（字典 worker_cert_type） */
    @Excel(name = "所需资质类型")
    private String certType;

    /** 是否自主打卡（0跟随班前喊话 1自主签到签退） */
    @Excel(name = "自主打卡", readConverterExp = "0=否,1=是")
    private String isSelfCheckin;

    /** 状态（0正常 1停用） */
    @Excel(name = "状态", readConverterExp = "0=正常,1=停用")
    private String status;

    public void setId(Long id)
    {
        this.id = id;
    }

    public Long getId()
    {
        return id;
    }

    public void setRoleCode(String roleCode)
    {
        this.roleCode = roleCode;
    }

    public String getRoleCode()
    {
        return roleCode;
    }

    public void setRoleName(String roleName)
    {
        this.roleName = roleName;
    }

    public String getRoleName()
    {
        return roleName;
    }

    public void setUnitType(String unitType)
    {
        this.unitType = unitType;
    }

    public String getUnitType()
    {
        return unitType;
    }

    public void setFixedSiteFlag(String fixedSiteFlag)
    {
        this.fixedSiteFlag = fixedSiteFlag;
    }

    public String getFixedSiteFlag()
    {
        return fixedSiteFlag;
    }

    public void setNeedSignIn(String needSignIn)
    {
        this.needSignIn = needSignIn;
    }

    public String getNeedSignIn()
    {
        return needSignIn;
    }

    public void setNeedSignOut(String needSignOut)
    {
        this.needSignOut = needSignOut;
    }

    public String getNeedSignOut()
    {
        return needSignOut;
    }

    public void setNeedHourlyCheck(String needHourlyCheck)
    {
        this.needHourlyCheck = needHourlyCheck;
    }

    public String getNeedHourlyCheck()
    {
        return needHourlyCheck;
    }

    public void setHourlyInterval(Integer hourlyInterval)
    {
        this.hourlyInterval = hourlyInterval;
    }

    public Integer getHourlyInterval()
    {
        return hourlyInterval;
    }

    public void setNeedCert(String needCert)
    {
        this.needCert = needCert;
    }

    public String getNeedCert()
    {
        return needCert;
    }

    public void setCertType(String certType)
    {
        this.certType = certType;
    }

    public String getCertType()
    {
        return certType;
    }

    public void setIsSelfCheckin(String isSelfCheckin) { this.isSelfCheckin = isSelfCheckin; }
    public String getIsSelfCheckin() { return isSelfCheckin; }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public String getStatus()
    {
        return status;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("roleCode", getRoleCode())
            .append("roleName", getRoleName())
            .append("unitType", getUnitType())
            .append("fixedSiteFlag", getFixedSiteFlag())
            .append("needSignIn", getNeedSignIn())
            .append("needSignOut", getNeedSignOut())
            .append("needHourlyCheck", getNeedHourlyCheck())
            .append("hourlyInterval", getHourlyInterval())
            .append("needCert", getNeedCert())
            .append("certType", getCertType())
            .append("isSelfCheckin", getIsSelfCheckin())
            .append("status", getStatus())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
