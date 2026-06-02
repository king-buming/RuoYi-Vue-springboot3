package com.ruoyi.system.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.system.domain.HwWorker;

/**
 * 施工人员 数据层
 *
 * @author ruoyi
 */
public interface HwWorkerMapper
{
    public List<HwWorker> selectHwWorkerList(HwWorker worker);

    public HwWorker selectHwWorkerById(Long workerId);

    public int insertHwWorker(HwWorker worker);

    public int updateHwWorker(HwWorker worker);

    public int deleteHwWorkerById(Long workerId);

    public int deleteHwWorkerByIds(Long[] workerIds);

    public HwWorker selectHwWorkerByUserId(@Param("userId") Long userId);
}
