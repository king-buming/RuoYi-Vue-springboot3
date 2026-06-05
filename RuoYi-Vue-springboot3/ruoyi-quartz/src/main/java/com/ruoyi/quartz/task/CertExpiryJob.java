package com.ruoyi.quartz.task;

import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.ruoyi.system.domain.TbWorkerCert;
import com.ruoyi.system.mapper.TbWorkerCertMapper;

/**
 * 证件过期自动检测定时任务
 * 每天凌晨执行：过期证件自动标记、即将过期记录日志
 */
@Component("certExpiryJob")
public class CertExpiryJob
{
    @Autowired
    private TbWorkerCertMapper certMapper;

    /** 默认 30 天内到期提醒 */
    private static final int WARN_DAYS = 30;

    public void execute()
    {
        System.out.println("[CertExpiryJob] 开始扫描证件过期...");
        TbWorkerCert q = new TbWorkerCert();
        List<TbWorkerCert> all = certMapper.selectTbWorkerCertList(q);
        Date now = new Date();
        long warnMs = WARN_DAYS * 24L * 3600_000L;
        int expired = 0, warned = 0;

        for (TbWorkerCert c : all) {
            if (c.getExpireDate() == null || "3".equals(c.getAuditStatus())) continue;

            if (c.getExpireDate().before(now)) {
                // 已过期 → 标记
                c.setAuditStatus("3");
                certMapper.updateTbWorkerCert(c);
                System.out.println("[CertExpiryJob] 过期: certId=" + c.getId() + " expire=" + c.getExpireDate());
                expired++;
            } else if (c.getExpireDate().getTime() - now.getTime() < warnMs) {
                // 即将过期 → 仅日志提醒（后续可扩展消息推送）
                System.out.println("[CertExpiryJob] 即将过期: certId=" + c.getId()
                    + " expire=" + c.getExpireDate() + " 剩余天数="
                    + ((c.getExpireDate().getTime() - now.getTime()) / 86400000));
                warned++;
            }
        }
        System.out.println("[CertExpiryJob] 完成: 过期标记=" + expired + ", 即将过期提醒=" + warned);
    }
}
