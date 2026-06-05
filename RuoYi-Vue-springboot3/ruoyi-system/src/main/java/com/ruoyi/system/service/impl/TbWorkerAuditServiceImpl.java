package com.ruoyi.system.service.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.system.mapper.TbWorkerAuditMapper;
import com.ruoyi.system.mapper.TbWorkerCertMapper;
import com.ruoyi.system.mapper.TbWorkerMapper;
import com.ruoyi.system.mapper.TbNotificationMapper;
import com.ruoyi.system.mapper.TbWorkerRoleRelMapper;
import com.ruoyi.system.domain.TbNotification;
import com.ruoyi.system.domain.TbWorker;
import com.ruoyi.system.domain.TbWorkerAudit;
import com.ruoyi.system.domain.TbWorkerCert;
import com.ruoyi.system.service.ITbWorkerAuditService;
import com.ruoyi.system.service.ITbWorkerService;

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
    @Autowired
    private TbWorkerRoleRelMapper roleRelMapper;
    @Autowired
    private ITbWorkerService workerService;
    @Autowired
    private TbNotificationMapper notifMapper;

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
        if ("worker".equals(a.getBizType()) && a.getWorkerId() != null) {
            a.setBizId(a.getWorkerId());
        }
        // 审核通过时：检查该人员所有角色需要的资质是否齐全且已通过
        if ("worker".equals(a.getBizType()) && "1".equals(a.getAuditStatus()) && a.getBizId() != null) {
            List<Long> roleIds = roleRelMapper.selectRoleIdsByWorkerId(a.getBizId());
            List<String> missing = workerService.validateRoleRequirements(a.getBizId(), roleIds);
            if (!missing.isEmpty()) {
                a.setAuditStatus("0"); // 缺资质，降为待审核
            } else {
                // 资质齐全 → 同步该人员所有证书为已通过
                TbWorkerCert qc = new TbWorkerCert(); qc.setWorkerId(a.getBizId());
                tbWorkerCertMapper.selectTbWorkerCertList(qc).forEach(c -> {
                    c.setAuditStatus("1"); tbWorkerCertMapper.updateTbWorkerCert(c);
                });
            }
        }
        a.setCreateTime(DateUtils.getNowDate());
        int rows = tbWorkerAuditMapper.insertTbWorkerAudit(a);
        syncStatus(a);
        // 发送通知
        if ("worker".equals(a.getBizType()) && a.getWorkerId() != null) {
            TbNotification n = new TbNotification();
            n.setWorkerId(a.getWorkerId());
            n.setBizType("audit");
            n.setBizId(a.getId());
            if ("1".equals(a.getAuditStatus())) {
                n.setType("audit_pass"); n.setTitle("人员审核已通过"); n.setContent(a.getAuditOpinion() != null ? a.getAuditOpinion() : "您的资料审核已通过");
            } else if ("2".equals(a.getAuditStatus())) {
                n.setType("audit_reject"); n.setTitle("人员审核已驳回"); n.setContent(a.getAuditOpinion() != null ? a.getAuditOpinion() : "您的资料审核已被驳回，请重新提交");
            }
            if (n.getType() != null) notifMapper.insert(n);
        }
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
            if (w != null) { w.setAuditStatus(a.getAuditStatus()); tbWorkerMapper.updateTbWorker(w); }
        } else if ("cert".equals(a.getBizType())) {
            TbWorkerCert c = tbWorkerCertMapper.selectTbWorkerCertById(a.getBizId());
            if (c != null) {
                c.setAuditStatus(a.getAuditStatus());
                tbWorkerCertMapper.updateTbWorkerCert(c);
            }
        }
    }
}
