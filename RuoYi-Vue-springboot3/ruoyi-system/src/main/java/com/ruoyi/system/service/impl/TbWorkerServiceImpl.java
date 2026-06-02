package com.ruoyi.system.service.impl;

import java.util.*;
import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.core.domain.entity.SysDictData;
import com.ruoyi.system.mapper.SysDictDataMapper;
import com.ruoyi.system.mapper.TbWorkerMapper;
import com.ruoyi.system.mapper.TbWorkerCertMapper;
import com.ruoyi.system.mapper.TbWorkerRoleMapper;
import com.ruoyi.system.domain.TbWorker;
import com.ruoyi.system.domain.TbWorkerCert;
import com.ruoyi.system.domain.TbWorkerRole;
import com.ruoyi.system.service.ITbWorkerService;

/**
 * 人员档案Service（含角色规则校验）
 */
@Service
public class TbWorkerServiceImpl implements ITbWorkerService
{
    @Autowired private TbWorkerMapper tbWorkerMapper;
    @Autowired private TbWorkerRoleMapper tbWorkerRoleMapper;
    @Autowired private TbWorkerCertMapper tbWorkerCertMapper;
    @Autowired private SysDictDataMapper sysDictDataMapper;

    @Override public TbWorker selectTbWorkerById(Long id) { return tbWorkerMapper.selectTbWorkerById(id); }
    @Override public List<TbWorker> selectTbWorkerList(TbWorker w) { return tbWorkerMapper.selectTbWorkerList(w); }
    @Override public int insertTbWorker(TbWorker w) { w.setCreateTime(DateUtils.getNowDate()); return tbWorkerMapper.insertTbWorker(w); }
    @Override public int updateTbWorker(TbWorker w) { w.setUpdateTime(DateUtils.getNowDate()); return tbWorkerMapper.updateTbWorker(w); }
    @Override public int deleteTbWorkerByIds(Long[] ids) { return tbWorkerMapper.deleteTbWorkerByIds(ids); }
    @Override public int deleteTbWorkerById(Long id) { return tbWorkerMapper.deleteTbWorkerById(id); }
    @Override public List<TbWorker> selectWorkerOptions() { return tbWorkerMapper.selectWorkerOptions(); }
    @Override public List<TbWorker> selectActiveWorkerOptions() { return tbWorkerMapper.selectActiveWorkerOptions(); }
    @Override public boolean checkIdCardUnique(String idCard, Long excludeId) { return tbWorkerMapper.checkIdCardUnique(idCard, excludeId) == 0; }

    @Override public boolean isWorkerActive(Long id) {
        if (id == null) return false;
        TbWorker w = tbWorkerMapper.selectTbWorkerById(id);
        return w != null && "0".equals(w.getDelFlag()) && !"2".equals(w.getStatus());
    }

    /**
     * 统一角色规则校验入口（弱约束）。
     * 检查每个角色的资质要求，证件必须「已通过」且「未过期」才算有效。
     * 如有缺失 → 将该人员 audit_status 退回 '0'，返回中文缺失列表。
     * @return 缺失资质的描述；空列表=全部合规
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<String> validateRoleRequirements(Long workerId, List<Long> roleIds) {
        List<String> missing = new ArrayList<>();
        if (roleIds == null || roleIds.isEmpty()) return missing;

        // 1) 加载该人员的有效证件（仅已通过+未过期）
        TbWorkerCert qc = new TbWorkerCert(); qc.setWorkerId(workerId);
        List<TbWorkerCert> certs = tbWorkerCertMapper.selectTbWorkerCertList(qc);
        Date now = new Date();
        List<TbWorkerCert> validCerts = certs.stream()
            .filter(c -> "1".equals(c.getAuditStatus()))                  // 已通过
            .filter(c -> c.getExpireDate() == null || c.getExpireDate().after(now)) // 未过期
            .collect(java.util.stream.Collectors.toList());

        // 2) 加载资质字典中文标签
        SysDictData dd = new SysDictData(); dd.setDictType("worker_cert_type");
        Map<String, String> certLabels = new HashMap<>();
        sysDictDataMapper.selectDictDataList(dd).forEach(d -> certLabels.put(d.getDictValue(), d.getDictLabel()));

        // 3) 逐角色校验
        for (Long rid : roleIds) {
            TbWorkerRole role = tbWorkerRoleMapper.selectTbWorkerRoleById(rid);
            if (role == null || !"1".equals(role.getNeedCert()) || role.getCertType() == null || role.getCertType().isEmpty())
                continue;
            boolean has = validCerts.stream().anyMatch(c -> role.getCertType().equals(c.getCertType()));
            if (!has) {
                String cn = certLabels.getOrDefault(role.getCertType(), role.getCertType());
                missing.add(role.getRoleName() + " 需要 " + cn);
            }
        }

        // 4) 弱约束：有缺失 → 打回待审核
        if (!missing.isEmpty()) {
            TbWorker w = tbWorkerMapper.selectTbWorkerById(workerId);
            if (w != null) { w.setAuditStatus("0"); tbWorkerMapper.updateTbWorker(w); }
        }
        return missing;
    }
}
