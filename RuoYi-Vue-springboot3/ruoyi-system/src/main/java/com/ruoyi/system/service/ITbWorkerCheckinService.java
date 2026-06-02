package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.system.domain.TbWorkerCheckin;

public interface ITbWorkerCheckinService
{
    public TbWorkerCheckin selectTbWorkerCheckinById(Long id);
    public List<TbWorkerCheckin> selectTbWorkerCheckinList(TbWorkerCheckin checkin);
    public int insertTbWorkerCheckin(TbWorkerCheckin checkin);
    public int updateTbWorkerCheckin(TbWorkerCheckin checkin);
    public int deleteTbWorkerCheckinByIds(Long[] ids);
    public int deleteTbWorkerCheckinById(Long id);
}
