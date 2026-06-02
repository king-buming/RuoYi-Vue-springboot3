package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.system.domain.HwPlan;

/**
 * 作业计划 服务层
 *
 * @author ruoyi
 */
public interface IHwPlanService
{
    public List<HwPlan> selectHwPlanList(HwPlan plan);

    public HwPlan selectHwPlanById(Long planId);

    public int insertHwPlan(HwPlan plan);

    public int updateHwPlan(HwPlan plan);

    public int deleteHwPlanById(Long planId);

    public int deleteHwPlanByIds(Long[] planIds);

    public int changeStatus(Long planId, String status);
}
