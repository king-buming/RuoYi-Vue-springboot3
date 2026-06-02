package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.system.domain.HwWorker;

/**
 * 施工人员 服务层
 *
 * @author ruoyi
 */
public interface IHwWorkerService
{
    public List<HwWorker> selectHwWorkerList(HwWorker worker);

    public HwWorker selectHwWorkerById(Long workerId);

    public int insertHwWorker(HwWorker worker);

    public int updateHwWorker(HwWorker worker);

    public int deleteHwWorkerById(Long workerId);

    public int deleteHwWorkerByIds(Long[] workerIds);
}
