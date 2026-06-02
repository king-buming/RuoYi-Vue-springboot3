package com.ruoyi.system.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.system.domain.HwPlanVideo;
import com.ruoyi.system.mapper.HwPlanVideoMapper;
import com.ruoyi.system.service.IHwPlanVideoService;

@Service
public class HwPlanVideoServiceImpl implements IHwPlanVideoService
{
    @Autowired
    private HwPlanVideoMapper hwPlanVideoMapper;

    @Override
    public List<HwPlanVideo> selectByPlanId(Long planId)
    {
        return hwPlanVideoMapper.selectByPlanId(planId);
    }

    @Override
    public int insertHwPlanVideo(HwPlanVideo planVideo)
    {
        return hwPlanVideoMapper.insertHwPlanVideo(planVideo);
    }

    @Override
    public int deleteById(Long id)
    {
        return hwPlanVideoMapper.deleteById(id);
    }

    @Override
    public int deleteByPlanId(Long planId)
    {
        return hwPlanVideoMapper.deleteByPlanId(planId);
    }
}
