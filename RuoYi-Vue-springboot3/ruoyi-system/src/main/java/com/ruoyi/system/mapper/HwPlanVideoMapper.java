package com.ruoyi.system.mapper;

import java.util.List;
import com.ruoyi.system.domain.HwPlanVideo;

public interface HwPlanVideoMapper
{
    public List<HwPlanVideo> selectByPlanId(Long planId);
    public int insertHwPlanVideo(HwPlanVideo planVideo);
    public int deleteById(Long id);
    public int deleteByPlanId(Long planId);
}
