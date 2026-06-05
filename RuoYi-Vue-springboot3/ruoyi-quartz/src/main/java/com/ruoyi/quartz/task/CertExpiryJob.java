package com.ruoyi.quartz.task;

import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.ruoyi.system.domain.TbNotification;
import com.ruoyi.system.domain.TbWorkerCert;
import com.ruoyi.system.mapper.TbNotificationMapper;
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
    @Autowired
    private TbNotificationMapper notifMapper;

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
                c.setAuditStatus("3");
                certMapper.updateTbWorkerCert(c);
                TbNotification n = new TbNotification();
                n.setWorkerId(c.getWorkerId()); n.setType("cert_expire"); n.setBizType("cert"); n.setBizId(c.getId());
                n.setTitle("证件已过期"); n.setContent("您的证件已于 " + new java.text.SimpleDateFormat("yyyy-MM-dd").format(c.getExpireDate()) + " 过期，请尽快更新");
                notifMapper.insert(n);
                expired++;
            } else if (c.getExpireDate().getTime() - now.getTime() < warnMs) {
                TbNotification n = new TbNotification();
                n.setWorkerId(c.getWorkerId()); n.setType("cert_warn"); n.setBizType("cert"); n.setBizId(c.getId());
                n.setTitle("证件即将到期"); n.setContent("您的证件将于 " + new java.text.SimpleDateFormat("yyyy-MM-dd").format(c.getExpireDate()) + " 到期（剩余 " + ((c.getExpireDate().getTime() - now.getTime())/86400000) + " 天），请及时更新");
                notifMapper.insert(n);
                warned++;
            }
        }
        System.out.println("[CertExpiryJob] 完成: 过期标记=" + expired + ", 即将过期提醒=" + warned);
    }
}
