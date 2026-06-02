package com.ruoyi.system.mapper;

import java.util.List;
import com.ruoyi.system.domain.HwPlan;

/**
 * 作业计划 数据层
 *
 * @author ruoyi
 */
public interface HwPlanMapper
{
    public List<HwPlan> selectHwPlanList(HwPlan plan);

    public HwPlan selectHwPlanById(Long planId);

    public int insertHwPlan(HwPlan plan);

    public int updateHwPlan(HwPlan plan);

    public int deleteHwPlanById(Long planId);

    public int deleteHwPlanByIds(Long[] planIds);
}
