-- =====================================================
-- 作业管理去掉审核模块 — 数据库变更
-- 日期：2026-06-06
-- 说明：彻底移除作业审核模块，计划创建后直接进入待执行
-- 可重复执行（幂等）
-- =====================================================

-- 1. 将现有 status='0'（待审核）的计划改为 status='1'（待执行）
UPDATE hw_plan SET status = '1', update_by = 'SYSTEM', update_time = SYSDATE()
WHERE status = '0';

-- 2. 修改 status 字段注释和默认值
ALTER TABLE hw_plan MODIFY COLUMN status CHAR(1) DEFAULT '1' COMMENT '状态（1待执行 2进行中 3已完成 4已取消）';

-- 3. 删除作业审核菜单及按钮权限（sys_menu）
DELETE FROM sys_menu WHERE perms IN (
    'homework:review:list',
    'homework:review:query',
    'homework:review:approve',
    'homework:review:reject'
);

-- 4. 删除作业审核表
DROP TABLE IF EXISTS hw_review;

-- 5. 删除"若依官网"菜单（menu_id=4）
DELETE FROM sys_menu WHERE menu_id = 4;
