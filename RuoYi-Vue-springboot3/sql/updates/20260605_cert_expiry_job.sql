-- 证件过期检测定时任务
USE `ry-vue`;
INSERT IGNORE INTO sys_job (job_name, job_group, invoke_target, cron_expression, misfire_policy, concurrent, status, create_by, create_time, update_by, update_time, remark)
VALUES ('证件过期自动检测', 'SYSTEM', 'certExpiryJob.execute', '0 0 2 * * ?', '1', '0', '0', 'admin', NOW(), '', NOW(), '每天凌晨2点扫描tb_worker_cert，过期自动标记');
