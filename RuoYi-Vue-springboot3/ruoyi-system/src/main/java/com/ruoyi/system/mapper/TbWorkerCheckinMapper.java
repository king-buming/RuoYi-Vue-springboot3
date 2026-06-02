package com.ruoyi.system.mapper;

import java.util.List;
import com.ruoyi.system.domain.TbWorkerCheckin;

public interface TbWorkerCheckinMapper
{
    public TbWorkerCheckin selectTbWorkerCheckinById(Long id);
    public List<TbWorkerCheckin> selectTbWorkerCheckinList(TbWorkerCheckin checkin);
    public int insertTbWorkerCheckin(TbWorkerCheckin checkin);
    public int updateTbWorkerCheckin(TbWorkerCheckin checkin);
    public int deleteTbWorkerCheckinById(Long id);
    public int deleteTbWorkerCheckinByIds(Long[] ids);
    public int deleteTbWorkerCheckinByWorkerId(Long workerId);
}
