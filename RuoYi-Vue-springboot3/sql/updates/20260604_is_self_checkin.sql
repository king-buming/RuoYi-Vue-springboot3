-- =============================================================
-- 变更说明：角色规则新增 is_self_checkin 字段（区分自主/被动打卡）
-- 依据    ：甲方需求——施工人员跟随班前喊话签到，不自主打卡
-- 作者    ：Zisaac52
-- 日期    ：2026-06-04
-- 执行    ：mysql -u <本地账号> -p ry-vue < 本文件
-- =============================================================
USE `ry-vue`;

-- 1. 加字段
ALTER TABLE `tb_worker_role` ADD COLUMN `is_self_checkin` char(1) DEFAULT '1' COMMENT '是否自主打卡(0跟随班前喊话 1自主签到签退)' AFTER `need_cert`;

-- 2. 施工人员设为被动打卡
UPDATE `tb_worker_role` SET `is_self_checkin` = '0' WHERE `role_code` = 'worker';