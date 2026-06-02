package com.ruoyi.system.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.system.domain.HwPlanWorker;

public interface HwPlanWorkerMapper
{
    public List<HwPlanWorker> selectByPlanId(Long planId);
    public int batchInsert(@Param("list") List<HwPlanWorker> list);
    public int deleteByPlanId(Long planId);
    public int deleteById(Long id);
}
