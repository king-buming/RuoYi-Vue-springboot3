package com.ruoyi.system.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.system.domain.HwPlanWorker;
import com.ruoyi.system.mapper.HwPlanWorkerMapper;
import com.ruoyi.system.service.IHwPlanWorkerService;

@Service
public class HwPlanWorkerServiceImpl implements IHwPlanWorkerService
{
    @Autowired
    private HwPlanWorkerMapper hwPlanWorkerMapper;

    @Override
    public List<HwPlanWorker> selectByPlanId(Long planId)
    {
        return hwPlanWorkerMapper.selectByPlanId(planId);
    }

    @Override
    public int batchInsert(List<HwPlanWorker> list)
    {
        return hwPlanWorkerMapper.batchInsert(list);
    }

    @Override
    public int deleteByPlanId(Long planId)
    {
        return hwPlanWorkerMapper.deleteByPlanId(planId);
    }

    @Override
    public int deleteById(Long id)
    {
        return hwPlanWorkerMapper.deleteById(id);
    }
}
