-- 修复 hw_plan_worker.role_type 列宽度不足的问题
-- 原因：V3 二级人员选择器传入的是 role_code（如 'applicant'），原 CHAR(1) 只能存单字符
ALTER TABLE hw_plan_worker MODIFY COLUMN role_type VARCHAR(20) DEFAULT '' COMMENT '角色编码（冗余）';
