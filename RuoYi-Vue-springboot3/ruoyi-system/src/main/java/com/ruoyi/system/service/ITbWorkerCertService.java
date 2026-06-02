package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.system.domain.TbWorkerCert;

/**
 * 资质证件Service接口
 *
 * @author ruoyi
 * @date 2026-06-01
 */
public interface ITbWorkerCertService
{
    /**
     * 查询资质证件
     *
     * @param id 资质证件主键
     * @return 资质证件
     */
    public TbWorkerCert selectTbWorkerCertById(Long id);

    /**
     * 查询资质证件列表
     *
     * @param tbWorkerCert 资质证件
     * @return 资质证件集合
     */
    public List<TbWorkerCert> selectTbWorkerCertList(TbWorkerCert tbWorkerCert);

    /**
     * 新增资质证件
     *
     * @param tbWorkerCert 资质证件
     * @return 结果
     */
    public int insertTbWorkerCert(TbWorkerCert tbWorkerCert);

    /**
     * 修改资质证件
     *
     * @param tbWorkerCert 资质证件
     * @return 结果
     */
    public int updateTbWorkerCert(TbWorkerCert tbWorkerCert);

    /**
     * 批量删除资质证件
     *
     * @param ids 需要删除的资质证件主键集合
     * @return 结果
     */
    public int deleteTbWorkerCertByIds(Long[] ids);

    /**
     * 删除资质证件信息
     *
     * @param id 资质证件主键
     * @return 结果
     */
    public int deleteTbWorkerCertById(Long id);
}
