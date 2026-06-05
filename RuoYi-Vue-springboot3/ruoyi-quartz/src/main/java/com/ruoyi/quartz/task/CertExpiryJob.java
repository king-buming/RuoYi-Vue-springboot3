package com.ruoyi.quartz.task;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.ruoyi.system.domain.TbNotification;
import com.ruoyi.system.domain.TbWorkerCert;
import com.ruoyi.system.mapper.TbNotificationMapper;
import com.ruoyi.system.mapper.TbWorkerCertMapper;

/**
 * 证件过期自动检测定时任务
 * 每天凌晨执行：过期证件自动标记、7天内到期提醒
 */
@Component("certExpiryJob")
public class CertExpiryJob
{
    @Autowired
    private TbWorkerCertMapper certMapper;
    @Autowired
    private TbNotificationMapper notifMapper;

    /** 7天内到期提醒 */
    private static final int WARN_DAYS = 7;

    public void execute()
    {
        System.out.println("[CertExpiryJob] 开始扫描证件过期...");
        TbWorkerCert q = new TbWorkerCert();
        List<TbWorkerCert> all = certMapper.selectTbWorkerCertList(q);
        LocalDate today = LocalDate.now();
        int expired = 0, warned = 0;

        for (TbWorkerCert c : all) {
            if (c.getWorkerId() == null || c.getExpireDate() == null || "3".equals(c.getAuditStatus())) continue;

            LocalDate expireDate = c.getExpireDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            long daysUntil = ChronoUnit.DAYS.between(today, expireDate);
            String expireText = expireDate.toString();

            if (daysUntil < 0) {
                c.setAuditStatus("3");
                certMapper.updateTbWorkerCert(c);
                if (insertOnce(c, "cert_expire", "证件已过期", "您的证件已于 " + expireText + " 过期，请尽快更新")) {
                    expired++;
                }
            } else if (daysUntil <= WARN_DAYS) {
                String dayText = daysUntil == 0 ? "今天" : "剩余 " + daysUntil + " 天";
                if (insertOnce(c, "cert_warn", "证件即将到期", "您的证件将于 " + expireText + " 到期（" + dayText + "），请及时更新")) {
                    warned++;
                }
            }
        }
        System.out.println("[CertExpiryJob] 完成: 过期标记=" + expired + ", 即将过期提醒=" + warned);
    }

    private boolean insertOnce(TbWorkerCert cert, String type, String title, String content)
    {
        int exists = notifMapper.countByWorkerTypeBiz(cert.getWorkerId(), type, "cert", cert.getId());
        if (exists > 0) {
            return false;
        }
        TbNotification n = new TbNotification();
        n.setWorkerId(cert.getWorkerId());
        n.setType(type);
        n.setBizType("cert");
        n.setBizId(cert.getId());
        n.setTitle(title);
        n.setContent(content);
        notifMapper.insert(n);
        return true;
    }
}
