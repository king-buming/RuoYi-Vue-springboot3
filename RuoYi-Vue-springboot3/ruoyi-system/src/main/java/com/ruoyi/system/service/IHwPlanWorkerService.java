package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.system.domain.HwPlanWorker;

public interface IHwPlanWorkerService
{
    public List<HwPlanWorker> selectByPlanId(Long planId);
    public int batchInsert(List<HwPlanWorker> list);
    public int deleteByPlanId(Long planId);
    public int deleteById(Long id);
}
