-- ============================================================
-- 作业管理模块 V3 数据库迁移 —— 新增作业审核模块
-- 日期：2026-06-04
-- ============================================================

-- 1. 修改 hw_plan.status 注释：从 4 状态改为 5 状态
ALTER TABLE hw_plan MODIFY COLUMN status CHAR(1) DEFAULT '0' COMMENT '状态（0待审核 1待执行 2进行中 3已完成 4已取消）';

-- 2. 新建 hw_review 作业审核表
DROP TABLE IF EXISTS hw_review;
CREATE TABLE hw_review (
  review_id           BIGINT(20)   NOT NULL AUTO_INCREMENT  COMMENT '审核ID',
  plan_id             BIGINT(20)   NOT NULL                  COMMENT '关联作业计划ID',
  plan_name           VARCHAR(200) DEFAULT ''                COMMENT '项目名称（冗余）',
  work_type           VARCHAR(20)  DEFAULT ''                COMMENT '作业类型（冗余）',
  construction_unit   VARCHAR(200) DEFAULT ''                COMMENT '施工单位（冗余）',
  applicant           VARCHAR(64)  DEFAULT ''                COMMENT '申请人（计划创建者）',
  apply_time          DATETIME     DEFAULT NULL              COMMENT '申请时间',
  review_status       CHAR(1)      DEFAULT '0'               COMMENT '审核状态（0待审核 1已通过 2已驳回）',
  review_opinion      VARCHAR(500) DEFAULT ''                COMMENT '审核意见',
  reviewer            VARCHAR(64)  DEFAULT ''                COMMENT '审核人',
  review_time         DATETIME     DEFAULT NULL              COMMENT '审核时间',
  create_by           VARCHAR(64)  DEFAULT ''                COMMENT '创建者',
  create_time         DATETIME                               COMMENT '创建时间',
  update_by           VARCHAR(64)  DEFAULT ''                COMMENT '更新者',
  update_time         DATETIME                               COMMENT '更新时间',
  remark              VARCHAR(500) DEFAULT NULL              COMMENT '备注',
  PRIMARY KEY (review_id),
  INDEX idx_plan_id (plan_id),
  INDEX idx_review_status (review_status)
) ENGINE=InnoDB COMMENT='作业审核表';

-- 3. sys_menu 菜单变更

-- 3.1 查询作业管理目录ID
SET @parent_id = (SELECT menu_id FROM sys_menu WHERE menu_name = '作业管理' AND parent_id = 0);

-- 3.2 将原"作业打卡"的 order_num 从 2 改为 3（为新审核菜单让位）
UPDATE sys_menu SET order_num = 3 WHERE parent_id = @parent_id AND menu_name = '作业打卡';

-- 3.3 插入二级菜单"作业审核"（order_num=2，排在作业计划和作业打卡之间）
INSERT INTO sys_menu VALUES
((SELECT MAX(menu_id)+1 FROM sys_menu m), '作业审核', @parent_id, '2', 'review', 'homework/review/index', '', 'HwReview', 1, 0, 'C', '0', '0', 'homework:review:list', 'edit', 'admin', SYSDATE(), '', NULL, '作业审核菜单');

-- 3.4 按钮权限（审核查询 / 审核通过 / 审核驳回）
SET @review_id = (SELECT menu_id FROM sys_menu WHERE perms = 'homework:review:list');
INSERT INTO sys_menu VALUES
(@review_id+1,  '审核查询', @review_id, '1', '#', '', '', '', 1, 0, 'F', '0', '0', 'homework:review:query',     '#', 'admin', SYSDATE(), '', NULL, ''),
(@review_id+2,  '审核通过', @review_id, '2', '#', '', '', '', 1, 0, 'F', '0', '0', 'homework:review:approve',   '#', 'admin', SYSDATE(), '', NULL, ''),
(@review_id+3,  '审核驳回', @review_id, '3', '#', '', '', '', 1, 0, 'F', '0', '0', 'homework:review:reject',    '#', 'admin', SYSDATE(), '', NULL, '');
