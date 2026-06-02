package com.ruoyi.system.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 作业计划人员关联表 hw_plan_worker
 *
 * @author ruoyi
 */
public class HwPlanWorker extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long planId;
    private Long workerId;
    private String workerName;
    private String roleType;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getPlanId() { return planId; }
    public void setPlanId(Long planId) { this.planId = planId; }
    public Long getWorkerId() { return workerId; }
    public void setWorkerId(Long workerId) { this.workerId = workerId; }
    public String getWorkerName() { return workerName; }
    public void setWorkerName(String workerName) { this.workerName = workerName; }
    public String getRoleType() { return roleType; }
    public void setRoleType(String roleType) { this.roleType = roleType; }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("planId", getPlanId())
            .append("workerId", getWorkerId())
            .append("workerName", getWorkerName())
            .append("roleType", getRoleType())
            .toString();
    }
}
