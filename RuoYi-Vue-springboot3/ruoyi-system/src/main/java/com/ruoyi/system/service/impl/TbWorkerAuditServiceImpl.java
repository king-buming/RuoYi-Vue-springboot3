package com.ruoyi.system.service.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.system.mapper.TbWorkerAuditMapper;
import com.ruoyi.system.mapper.TbWorkerCertMapper;
import com.ruoyi.system.mapper.TbWorkerMapper;
import com.ruoyi.system.domain.TbWorker;
import com.ruoyi.system.domain.TbWorkerAudit;
import com.ruoyi.system.domain.TbWorkerCert;
import com.ruoyi.system.service.ITbWorkerAuditService;

/**
 * 审核记录Service业务层处理（含审核驱动业务状态）
 *
 * @author ruoyi
 * @date 2026-06-01
 */
@Service
public class TbWorkerAuditServiceImpl implements ITbWorkerAuditService
{
    @Autowired
    private TbWorkerAuditMapper tbWorkerAuditMapper;
    @Autowired
    private TbWorkerMapper tbWorkerMapper;
    @Autowired
    private TbWorkerCertMapper tbWorkerCertMapper;

    @Override
    public TbWorkerAudit selectTbWorkerAuditById(Long id) {
        return tbWorkerAuditMapper.selectTbWorkerAuditById(id);
    }

    @Override
    public List<TbWorkerAudit> selectTbWorkerAuditList(TbWorkerAudit tbWorkerAudit) {
        return tbWorkerAuditMapper.selectTbWorkerAuditList(tbWorkerAudit);
    }

    /**
     * 新增审核记录 → 同步回写业务主表状态（同一事务）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertTbWorkerAudit(TbWorkerAudit a)
    {
        // worker 类型审核：bizId 就是 workerId
        if ("worker".equals(a.getBizType()) && a.getWorkerId() != null) {
            a.setBizId(a.getWorkerId());
        }
        a.setCreateTime(DateUtils.getNowDate());
        int rows = tbWorkerAuditMapper.insertTbWorkerAudit(a);
        syncStatus(a);
        return rows;
    }

    /**
     * 修改审核记录 → 同步回写业务主表状态（同一事务）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateTbWorkerAudit(TbWorkerAudit a)
    {
        a.setUpdateTime(DateUtils.getNowDate());
        int rows = tbWorkerAuditMapper.updateTbWorkerAudit(a);
        syncStatus(a);
        return rows;
    }

    @Override
    public int deleteTbWorkerAuditByIds(Long[] ids) {
        return tbWorkerAuditMapper.deleteTbWorkerAuditByIds(ids);
    }

    @Override
    public int deleteTbWorkerAuditById(Long id) {
        return tbWorkerAuditMapper.deleteTbWorkerAuditById(id);
    }

    /** 审核结果回写到对应业务主表 */
    private void syncStatus(TbWorkerAudit a) {
        if (a.getBizId() == null) return;
        if ("worker".equals(a.getBizType())) {
            TbWorker w = tbWorkerMapper.selectTbWorkerById(a.getBizId());
            if (w != null) {
                w.setAuditStatus(a.getAuditStatus());
                tbWorkerMapper.updateTbWorker(w);
            }
        } else if ("cert".equals(a.getBizType())) {
            TbWorkerCert c = tbWorkerCertMapper.selectTbWorkerCertById(a.getBizId());
            if (c != null) {
                c.setAuditStatus(a.getAuditStatus());
                tbWorkerCertMapper.updateTbWorkerCert(c);
            }
        }
    }
}
