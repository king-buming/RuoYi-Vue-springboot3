package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.system.domain.TbWorkerAudit;

/**
 * 审核记录Service接口
 *
 * @author ruoyi
 * @date 2026-06-01
 */
public interface ITbWorkerAuditService
{
    /**
     * 查询审核记录
     *
     * @param id 审核记录主键
     * @return 审核记录
     */
    public TbWorkerAudit selectTbWorkerAuditById(Long id);

    /**
     * 查询审核记录列表
     *
     * @param tbWorkerAudit 审核记录
     * @return 审核记录集合
     */
    public List<TbWorkerAudit> selectTbWorkerAuditList(TbWorkerAudit tbWorkerAudit);

    /**
     * 新增审核记录
     *
     * @param tbWorkerAudit 审核记录
     * @return 结果
     */
    public int insertTbWorkerAudit(TbWorkerAudit tbWorkerAudit);

    /**
     * 修改审核记录
     *
     * @param tbWorkerAudit 审核记录
     * @return 结果
     */
    public int updateTbWorkerAudit(TbWorkerAudit tbWorkerAudit);

    /**
     * 批量删除审核记录
     *
     * @param ids 需要删除的审核记录主键集合
     * @return 结果
     */
    public int deleteTbWorkerAuditByIds(Long[] ids);

    /**
     * 删除审核记录信息
     *
     * @param id 审核记录主键
     * @return 结果
     */
    public int deleteTbWorkerAuditById(Long id);
}
