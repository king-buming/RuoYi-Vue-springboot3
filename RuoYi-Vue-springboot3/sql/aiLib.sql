-- ============================================================
-- AI算法仓模块 SQL（可重复执行，每次执行前先清理旧数据）
-- 数据库：ry-vue
-- ============================================================
USE `ry-vue`;

-- ---------------------------------------------------------
-- 0. 清理旧数据（确保可重复执行）
-- ---------------------------------------------------------
-- 清理 sys_menu（按 perms 前缀删除 AI 相关菜单）
DELETE FROM sys_menu WHERE perms LIKE 'ai:%';
DELETE FROM sys_menu WHERE parent_id IN (SELECT m.menu_id FROM (SELECT menu_id FROM sys_menu WHERE menu_name = 'AI算法仓' AND parent_id = 0) AS m);
DELETE FROM sys_menu WHERE menu_name = 'AI算法仓' AND parent_id = 0;

-- 清理数据字典（先删 data 再删 type）
DELETE FROM sys_dict_data WHERE dict_type LIKE 'ai_%';
DELETE FROM sys_dict_type WHERE dict_type LIKE 'ai_%';

-- 清理业务表
DELETE FROM ai_face_register;
DELETE FROM ai_model;

-- ---------------------------------------------------------
-- 1. ai_model —— AI算法模型表
-- ---------------------------------------------------------
DROP TABLE IF EXISTS ai_model;
CREATE TABLE IF NOT EXISTS ai_model (
  model_id           BIGINT(20)     NOT NULL AUTO_INCREMENT  COMMENT '模型ID',
  model_name         VARCHAR(100)   NOT NULL                  COMMENT '模型名称',
  model_type         VARCHAR(50)    NOT NULL                  COMMENT '模型类型',
  model_code         VARCHAR(50)    NOT NULL                  COMMENT '模型编码（唯一标识）',
  version            VARCHAR(20)    DEFAULT ''                COMMENT '版本号',
  framework          VARCHAR(50)    DEFAULT ''                COMMENT 'AI框架',
  model_path         VARCHAR(500)   DEFAULT ''                COMMENT '模型文件路径或URI',
  config_json        TEXT                                     COMMENT '模型推理参数配置',
  metrics            TEXT                                     COMMENT '模型评估指标',
  input_format       VARCHAR(200)   DEFAULT ''                COMMENT '输入格式说明',
  output_format      VARCHAR(200)   DEFAULT ''                COMMENT '输出格式说明',
  provider           VARCHAR(100)   DEFAULT ''                COMMENT '模型提供方',
  thumbnail          VARCHAR(500)   DEFAULT ''                COMMENT '模型缩略图URL',
  status             CHAR(1)        DEFAULT '0'               COMMENT '状态',
  description        VARCHAR(2000)  DEFAULT ''                COMMENT '模型功能描述',
  create_by          VARCHAR(64)    DEFAULT ''                COMMENT '创建者',
  create_time        DATETIME                                 COMMENT '创建时间',
  update_by          VARCHAR(64)    DEFAULT ''                COMMENT '更新者',
  update_time        DATETIME                                 COMMENT '更新时间',
  remark             VARCHAR(500)   DEFAULT NULL              COMMENT '备注',
  PRIMARY KEY (model_id),
  UNIQUE KEY uk_model_code (model_code)
) ENGINE=InnoDB COMMENT='AI算法模型表';

-- ---------------------------------------------------------
-- 2. ai_face_register —— AI人脸注册表
-- ---------------------------------------------------------
DROP TABLE IF EXISTS ai_face_register;
CREATE TABLE IF NOT EXISTS ai_face_register (
  register_id        BIGINT(20)     NOT NULL AUTO_INCREMENT  COMMENT '注册记录ID',
  worker_id          BIGINT(20)     NOT NULL                  COMMENT '人员ID',
  worker_name        VARCHAR(50)    NOT NULL                  COMMENT '人员姓名',
  face_img_url       VARCHAR(500)   DEFAULT ''                COMMENT '人脸照片URL',
  face_feature       TEXT                                     COMMENT '人脸特征向量',
  model_code         VARCHAR(50)    NOT NULL                  COMMENT '关联模型编码',
  model_name         VARCHAR(100)   DEFAULT ''                COMMENT '关联模型名称',
  register_status    CHAR(1)        DEFAULT '0'               COMMENT '录入状态',
  register_time      DATETIME       DEFAULT NULL              COMMENT '录入时间',
  fail_reason        VARCHAR(500)   DEFAULT ''                COMMENT '录入失败原因',
  create_by          VARCHAR(64)    DEFAULT ''                COMMENT '创建者',
  create_time        DATETIME                                 COMMENT '创建时间',
  update_by          VARCHAR(64)    DEFAULT ''                COMMENT '更新者',
  update_time        DATETIME                                 COMMENT '更新时间',
  remark             VARCHAR(500)   DEFAULT NULL              COMMENT '备注',
  PRIMARY KEY (register_id),
  KEY idx_worker_id (worker_id),
  KEY idx_model_code (model_code),
  UNIQUE KEY uk_worker_model (worker_id, model_code)
) ENGINE=InnoDB COMMENT='AI人脸注册表';

-- ---------------------------------------------------------
-- 3. sys_menu 菜单数据
-- ---------------------------------------------------------
UPDATE sys_menu SET order_num = 6 WHERE menu_id = 4;

-- 一级目录
INSERT INTO sys_menu VALUES
((SELECT MAX(menu_id)+1 FROM sys_menu m), 'AI算法仓', '0', '5', 'ailib', NULL, '', '', 1, 0, 'M', '0', '0', '', 'guide', 'admin', SYSDATE(), '', NULL, 'AI算法仓目录');

-- 二级菜单（用变量存父ID）
SET @ailib_id = (SELECT menu_id FROM sys_menu WHERE menu_name = 'AI算法仓' AND parent_id = 0 LIMIT 1);

INSERT INTO sys_menu VALUES
(@ailib_id+1, '模型管理', @ailib_id, '1', 'model', 'ai/model/index', '', 'AiModel', 1, 0, 'C', '0', '0', 'ai:model:list', 'build', 'admin', SYSDATE(), '', NULL, 'AI模型管理菜单'),
(@ailib_id+2, '人脸注册', @ailib_id, '2', 'face', 'ai/face/index',   '', 'AiFace',   1, 0, 'C', '0', '0', 'ai:face:list',   'user',  'admin', SYSDATE(), '', NULL, 'AI人脸注册菜单');

-- 模型管理按钮权限
-- 按钮权限使用大偏移量避免菜单ID碰撞
-- 已知：@ailib_id=N, @model_id=N+1, @face_id=N+2
-- 模型按钮：N+101 起（避开 N+1~N+2 菜单自身）
-- 人脸按钮：N+201 起
SET @model_id = (SELECT menu_id FROM sys_menu WHERE perms = 'ai:model:list' LIMIT 1);

INSERT INTO sys_menu VALUES
(@model_id+101, '模型查询', @model_id, '1', '#', '', '', '', 1, 0, 'F', '0', '0', 'ai:model:query',    '#', 'admin', SYSDATE(), '', NULL, ''),
(@model_id+102, '模型新增', @model_id, '2', '#', '', '', '', 1, 0, 'F', '0', '0', 'ai:model:add',      '#', 'admin', SYSDATE(), '', NULL, ''),
(@model_id+103, '模型修改', @model_id, '3', '#', '', '', '', 1, 0, 'F', '0', '0', 'ai:model:edit',     '#', 'admin', SYSDATE(), '', NULL, ''),
(@model_id+104, '模型删除', @model_id, '4', '#', '', '', '', 1, 0, 'F', '0', '0', 'ai:model:remove',   '#', 'admin', SYSDATE(), '', NULL, ''),
(@model_id+105, '模型部署', @model_id, '5', '#', '', '', '', 1, 0, 'F', '0', '0', 'ai:model:deploy',   '#', 'admin', SYSDATE(), '', NULL, ''),
(@model_id+106, '模型导出', @model_id, '6', '#', '', '', '', 1, 0, 'F', '0', '0', 'ai:model:export',   '#', 'admin', SYSDATE(), '', NULL, '');

SET @face_id = (SELECT menu_id FROM sys_menu WHERE perms = 'ai:face:list' LIMIT 1);

INSERT INTO sys_menu VALUES
(@face_id+201, '注册查询', @face_id, '1', '#', '', '', '', 1, 0, 'F', '0', '0', 'ai:face:query',    '#', 'admin', SYSDATE(), '', NULL, ''),
(@face_id+202, '人脸录入', @face_id, '2', '#', '', '', '', 1, 0, 'F', '0', '0', 'ai:face:register', '#', 'admin', SYSDATE(), '', NULL, ''),
(@face_id+203, '取消注册', @face_id, '3', '#', '', '', '', 1, 0, 'F', '0', '0', 'ai:face:cancel',   '#', 'admin', SYSDATE(), '', NULL, ''),
(@face_id+204, '批量录入', @face_id, '4', '#', '', '', '', 1, 0, 'F', '0', '0', 'ai:face:batch',    '#', 'admin', SYSDATE(), '', NULL, '');

-- ---------------------------------------------------------
-- 4. 数据字典
-- ---------------------------------------------------------

INSERT INTO sys_dict_type VALUES (NULL, 'ai_model_type', 'AI模型类型', '0', 'admin', SYSDATE(), '', NULL, 'AI算法仓-模型类型字典');
INSERT INTO sys_dict_data VALUES
(NULL, 1, '目标识别', 'target_detection', 'ai_model_type', '', '', 'N', '0', 'admin', SYSDATE(), '', NULL, ''),
(NULL, 2, '人脸匹配', 'face_recognition', 'ai_model_type', '', '', 'N', '0', 'admin', SYSDATE(), '', NULL, ''),
(NULL, 3, '穿戴识别', 'ppe_detection',    'ai_model_type', '', '', 'N', '0', 'admin', SYSDATE(), '', NULL, '');

INSERT INTO sys_dict_type VALUES (NULL, 'ai_model_status', 'AI模型状态', '0', 'admin', SYSDATE(), '', NULL, 'AI算法仓-模型状态字典');
INSERT INTO sys_dict_data VALUES
(NULL, 1, '未部署', '0', 'ai_model_status', '', '', 'N', '0', 'admin', SYSDATE(), '', NULL, ''),
(NULL, 2, '已部署', '1', 'ai_model_status', '', '', 'N', '0', 'admin', SYSDATE(), '', NULL, ''),
(NULL, 3, '运行中', '2', 'ai_model_status', '', '', 'N', '0', 'admin', SYSDATE(), '', NULL, ''),
(NULL, 4, '异常',   '3', 'ai_model_status', '', '', 'N', '0', 'admin', SYSDATE(), '', NULL, ''),
(NULL, 5, '已下线', '4', 'ai_model_status', '', '', 'N', '0', 'admin', SYSDATE(), '', NULL, '');

INSERT INTO sys_dict_type VALUES (NULL, 'ai_framework', 'AI框架', '0', 'admin', SYSDATE(), '', NULL, 'AI算法仓-AI框架字典');
INSERT INTO sys_dict_data VALUES
(NULL, 1, 'PyTorch',      'pytorch',      'ai_framework', '', '', 'N', '0', 'admin', SYSDATE(), '', NULL, ''),
(NULL, 2, 'TensorFlow',   'tensorflow',   'ai_framework', '', '', 'N', '0', 'admin', SYSDATE(), '', NULL, ''),
(NULL, 3, 'ONNX',         'onnx',         'ai_framework', '', '', 'N', '0', 'admin', SYSDATE(), '', NULL, ''),
(NULL, 4, 'PaddlePaddle', 'paddlepaddle', 'ai_framework', '', '', 'N', '0', 'admin', SYSDATE(), '', NULL, '');

INSERT INTO sys_dict_type VALUES (NULL, 'ai_face_register_status', 'AI人脸录入状态', '0', 'admin', SYSDATE(), '', NULL, 'AI算法仓-人脸录入状态字典');
INSERT INTO sys_dict_data VALUES
(NULL, 1, '未录入',   '0', 'ai_face_register_status', '', '', 'N', '0', 'admin', SYSDATE(), '', NULL, ''),
(NULL, 2, '已录入',   '1', 'ai_face_register_status', '', '', 'N', '0', 'admin', SYSDATE(), '', NULL, ''),
(NULL, 3, '录入失败', '2', 'ai_face_register_status', '', '', 'N', '0', 'admin', SYSDATE(), '', NULL, '');

-- ---------------------------------------------------------
-- 5. 示例模型数据（占位）
-- ---------------------------------------------------------
INSERT INTO ai_model VALUES
(1, 'YOLOv8-施工场景目标检测', 'target_detection', 'yolo_v8_obj_001', '8.0.0', 'pytorch',
 '/models/yolo_v8_construction.pt',
 '{"confidence_threshold":0.5,"iou_threshold":0.45,"input_size":"640x640","classes":["person","vehicle","hardhat","vest","face"]}',
 '{"mAP50":0.892,"mAP50-95":0.675,"precision":0.87,"recall":0.85,"f1_score":0.86,"fps":45}',
 '图片base64或图片URL，640x640 RGB三通道', '[{"bbox":[x1,y1,x2,y2],"class":"person","confidence":0.95}]',
 '内部训练', '', '0',
 '基于YOLOv8在施工场景数据集上微调的目标检测模型。部署时替换为实际模型文件路径即可。',
 'admin', SYSDATE(), '', NULL, '示例模型-待替换为真实训练模型');

INSERT INTO ai_model VALUES
(2, 'ArcFace-施工人员人脸匹配', 'face_recognition', 'arcface_r100_001', '1.0.0', 'onnx',
 '/models/arcface_resnet100.onnx',
 '{"embedding_dim":512,"similarity_threshold":0.65,"input_size":"112x112","normalize":true}',
 '{"accuracy":0.965,"precision":0.94,"recall":0.93,"f1_score":0.935}',
 '对齐后的人脸图片base64，112x112 RGB', '{"matched":true,"person_name":"张三","confidence":0.92,"worker_id":1}',
 '开源预训练+内部微调', '', '0',
 '基于ArcFace ResNet100的人脸识别模型。人员通过人脸注册页面录入底库后即可被本模型识别。',
 'admin', SYSDATE(), '', NULL, '示例模型-待替换为真实训练模型');

INSERT INTO ai_model VALUES
(3, 'PPE-Detect-安全穿戴识别', 'ppe_detection', 'ppe_detect_v1_001', '1.0.0', 'pytorch',
 '/models/ppe_detect_v1.pt',
 '{"confidence_threshold":0.45,"input_size":"640x640","classes":["helmet_on","helmet_off","vest_on","vest_off"]}',
 '{"mAP50":0.918,"mAP50-95":0.723,"precision":0.91,"recall":0.89,"f1_score":0.90,"fps":38}',
 '图片base64或图片URL，640x640 RGB三通道', '[{"bbox":[x1,y1,x2,y2],"class":"helmet_off","confidence":0.88,"alert":true}]',
 '内部训练', '', '0',
 '安全帽+反光衣穿戴检测模型，识别人员是否佩戴安全帽、穿着反光衣。',
 'admin', SYSDATE(), '', NULL, '示例模型-待替换为真实训练模型');

-- ---------------------------------------------------------
-- 6. 人脸注册示例数据
-- ---------------------------------------------------------
INSERT INTO ai_face_register VALUES
(1, 1, '张三', '/profile/upload/2026/06/01/face_zhangsan.jpg', NULL,
 'arcface_r100_001', 'ArcFace-施工人员人脸匹配',
 '0', NULL, '', 'admin', SYSDATE(), '', NULL, '待操作员点击录入');

INSERT INTO ai_face_register VALUES
(2, 2, '李四', '/profile/upload/2026/06/01/face_lisi.jpg',
 '{"dim":512,"features":"<placeholder_vector_base64>"}',
 'arcface_r100_001', 'ArcFace-施工人员人脸匹配',
 '1', SYSDATE(), '', 'admin', SYSDATE(), 'admin', SYSDATE(), '已录入示例');
