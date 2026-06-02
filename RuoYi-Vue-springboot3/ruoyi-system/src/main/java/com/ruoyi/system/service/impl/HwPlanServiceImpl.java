package com.ruoyi.system.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.domain.HwPlan;
import com.ruoyi.system.mapper.HwPlanMapper;
import com.ruoyi.system.service.IHwPlanService;

/**
 * 作业计划 服务层处理
 *
 * @author ruoyi
 */
@Service
public class HwPlanServiceImpl implements IHwPlanService
{
    @Autowired
    private HwPlanMapper hwPlanMapper;

    @Override
    public List<HwPlan> selectHwPlanList(HwPlan plan)
    {
        return hwPlanMapper.selectHwPlanList(plan);
    }

    @Override
    public HwPlan selectHwPlanById(Long planId)
    {
        return hwPlanMapper.selectHwPlanById(planId);
    }

    @Override
    public int insertHwPlan(HwPlan plan)
    {
        plan.setCreateBy(SecurityUtils.getUsername());
        return hwPlanMapper.insertHwPlan(plan);
    }

    @Override
    public int updateHwPlan(HwPlan plan)
    {
        plan.setUpdateBy(SecurityUtils.getUsername());
        return hwPlanMapper.updateHwPlan(plan);
    }

    @Override
    public int deleteHwPlanById(Long planId)
    {
        return hwPlanMapper.deleteHwPlanById(planId);
    }

    @Override
    public int deleteHwPlanByIds(Long[] planIds)
    {
        return hwPlanMapper.deleteHwPlanByIds(planIds);
    }

    @Override
    public int changeStatus(Long planId, String status)
    {
        HwPlan plan = hwPlanMapper.selectHwPlanById(planId);
        if (plan == null)
        {
            throw new ServiceException("作业计划不存在");
        }
        String current = plan.getStatus();
        if ("3".equals(current) && !"0".equals(status))
        {
            throw new ServiceException("已取消的计划不可变更状态");
        }
        if ("2".equals(current))
        {
            throw new ServiceException("已完成的计划不可变更状态");
        }
        if ("1".equals(status) && !"0".equals(current))
        {
            throw new ServiceException("只有待执行的计划才能开始作业");
        }
        if ("2".equals(status) && !"1".equals(current))
        {
            throw new ServiceException("只有进行中的计划才能标记完成");
        }
        plan.setStatus(status);
        plan.setUpdateBy(SecurityUtils.getUsername());
        return hwPlanMapper.updateHwPlan(plan);
    }
}
