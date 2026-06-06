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
        plan.setStatus("1");
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
        // 已完成不可再变更
        if ("3".equals(current))
        {
            throw new ServiceException("已完成的计划不可变更状态");
        }
        // 已取消只能恢复为待执行
        if ("4".equals(current) && !"1".equals(status))
        {
            throw new ServiceException("已取消的计划只能恢复为待执行");
        }
        switch (status)
        {
            case "1":
                if (!"4".equals(current))
                    throw new ServiceException("只有已取消的计划才能恢复为待执行");
                break;
            case "2":
                if (!"1".equals(current))
                    throw new ServiceException("只有待执行的计划才能开始作业");
                break;
            case "3":
                if (!"2".equals(current))
                    throw new ServiceException("只有进行中的计划才能标记完成");
                break;
            case "4":
                if ("3".equals(current) || "4".equals(current))
                    throw new ServiceException("当前状态不可取消");
                break;
            default:
                throw new ServiceException("无效的状态值");
        }
        plan.setStatus(status);
        plan.setUpdateBy(SecurityUtils.getUsername());
        return hwPlanMapper.updateHwPlan(plan);
    }
}
