package com.ruoyi.system.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.domain.HwWorker;
import com.ruoyi.system.mapper.HwWorkerMapper;
import com.ruoyi.system.service.IHwWorkerService;

/**
 * 施工人员 服务层处理
 *
 * @author ruoyi
 */
@Service
public class HwWorkerServiceImpl implements IHwWorkerService
{
    @Autowired
    private HwWorkerMapper hwWorkerMapper;

    @Override
    public List<HwWorker> selectHwWorkerList(HwWorker worker)
    {
        return hwWorkerMapper.selectHwWorkerList(worker);
    }

    @Override
    public HwWorker selectHwWorkerById(Long workerId)
    {
        return hwWorkerMapper.selectHwWorkerById(workerId);
    }

    @Override
    public int insertHwWorker(HwWorker worker)
    {
        worker.setCreateBy(SecurityUtils.getUsername());
        return hwWorkerMapper.insertHwWorker(worker);
    }

    @Override
    public int updateHwWorker(HwWorker worker)
    {
        worker.setUpdateBy(SecurityUtils.getUsername());
        return hwWorkerMapper.updateHwWorker(worker);
    }

    @Override
    public int deleteHwWorkerById(Long workerId)
    {
        return hwWorkerMapper.deleteHwWorkerById(workerId);
    }

    @Override
    public int deleteHwWorkerByIds(Long[] workerIds)
    {
        return hwWorkerMapper.deleteHwWorkerByIds(workerIds);
    }
}
