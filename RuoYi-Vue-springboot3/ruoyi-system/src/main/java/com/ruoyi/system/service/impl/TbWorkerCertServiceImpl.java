package com.ruoyi.system.service.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.system.mapper.TbWorkerCertMapper;
import com.ruoyi.system.domain.TbWorkerCert;
import com.ruoyi.system.service.ITbWorkerCertService;

/**
 * 资质证件Service业务层处理
 *
 * @author ruoyi
 * @date 2026-06-01
 */
@Service
public class TbWorkerCertServiceImpl implements ITbWorkerCertService
{
    @Autowired
    private TbWorkerCertMapper tbWorkerCertMapper;

    /**
     * 查询资质证件
     *
     * @param id 资质证件主键
     * @return 资质证件
     */
    @Override
    public TbWorkerCert selectTbWorkerCertById(Long id)
    {
        return tbWorkerCertMapper.selectTbWorkerCertById(id);
    }

    /**
     * 查询资质证件列表
     *
     * @param tbWorkerCert 资质证件
     * @return 资质证件
     */
    @Override
    public List<TbWorkerCert> selectTbWorkerCertList(TbWorkerCert tbWorkerCert)
    {
        return tbWorkerCertMapper.selectTbWorkerCertList(tbWorkerCert);
    }

    /**
     * 新增资质证件
     *
     * @param tbWorkerCert 资质证件
     * @return 结果
     */
    @Override
    public int insertTbWorkerCert(TbWorkerCert tbWorkerCert)
    {
        tbWorkerCert.setCreateTime(DateUtils.getNowDate());
        return tbWorkerCertMapper.insertTbWorkerCert(tbWorkerCert);
    }

    /**
     * 修改资质证件
     *
     * @param tbWorkerCert 资质证件
     * @return 结果
     */
    @Override
    public int updateTbWorkerCert(TbWorkerCert tbWorkerCert)
    {
        tbWorkerCert.setUpdateTime(DateUtils.getNowDate());
        return tbWorkerCertMapper.updateTbWorkerCert(tbWorkerCert);
    }

    /**
     * 批量删除资质证件
     *
     * @param ids 需要删除的资质证件主键
     * @return 结果
     */
    @Override
    public int deleteTbWorkerCertByIds(Long[] ids)
    {
        return tbWorkerCertMapper.deleteTbWorkerCertByIds(ids);
    }

    /**
     * 删除资质证件信息
     *
     * @param id 资质证件主键
     * @return 结果
     */
    @Override
    public int deleteTbWorkerCertById(Long id)
    {
        return tbWorkerCertMapper.deleteTbWorkerCertById(id);
    }
}
