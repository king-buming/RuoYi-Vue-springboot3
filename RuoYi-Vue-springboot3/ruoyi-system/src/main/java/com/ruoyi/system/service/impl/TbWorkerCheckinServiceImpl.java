package com.ruoyi.system.service.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.system.mapper.TbWorkerCheckinMapper;
import com.ruoyi.system.domain.TbWorkerCheckin;
import com.ruoyi.system.service.ITbWorkerCheckinService;

@Service
public class TbWorkerCheckinServiceImpl implements ITbWorkerCheckinService
{
    @Autowired
    private TbWorkerCheckinMapper tbWorkerCheckinMapper;

    @Override
    public TbWorkerCheckin selectTbWorkerCheckinById(Long id) {
        return tbWorkerCheckinMapper.selectTbWorkerCheckinById(id);
    }

    @Override
    public List<TbWorkerCheckin> selectTbWorkerCheckinList(TbWorkerCheckin checkin) {
        return tbWorkerCheckinMapper.selectTbWorkerCheckinList(checkin);
    }

    @Override
    public int insertTbWorkerCheckin(TbWorkerCheckin checkin) {
        checkin.setCreateTime(DateUtils.getNowDate());
        return tbWorkerCheckinMapper.insertTbWorkerCheckin(checkin);
    }

    @Override
    public int updateTbWorkerCheckin(TbWorkerCheckin checkin) {
        checkin.setUpdateTime(DateUtils.getNowDate());
        return tbWorkerCheckinMapper.updateTbWorkerCheckin(checkin);
    }

    @Override
    public int deleteTbWorkerCheckinByIds(Long[] ids) {
        return tbWorkerCheckinMapper.deleteTbWorkerCheckinByIds(ids);
    }

    @Override
    public int deleteTbWorkerCheckinById(Long id) {
        return tbWorkerCheckinMapper.deleteTbWorkerCheckinById(id);
    }
}
