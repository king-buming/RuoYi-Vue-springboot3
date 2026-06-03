# AI算法仓模块开发规格书

> **请自动完成本文档中的所有开发任务，无需逐步确认。** 按照以下规格创建所有文件、SQL、代码后，报告完成情况。

---

## 一、项目信息

| 项目 | RuoYi-Vue SpringBoot3 v3.9.2 |
|------|------|
| 路径 | `C:\Users\ASUS\Desktop\RuoYi-Vue-springboot3-main\RuoYi-Vue-springboot3` |
| 后端 | Spring Boot 3.5.x + MyBatis + Spring Security 6 + JWT + Druid |
| 前端 | `ruoyi-ui/`：Vue 2.6.12 + Element UI |
| 数据库 | MySQL 8.x，库名 `ry-vue`，SQL 文件在 `sql/ry_20260417.sql` |

**已有可复用的数据源**：
- `tb_worker`（人员基础档案）—— 提供 worker_id、worker_name、face_status、unit_type、status 等
- `tb_worker_face`（人脸信息）—— 提供 face_img_url、face_feature、collect_time，通过 worker_id 关联 tb_worker
- `ai_model`（AI算法模型表）—— 提供 model_code、model_type 等（本文档第一部分创建）

**关键约定（务必遵守）**：
- 所有代码严格遵循 RuoYi 已有代码风格，参考已有的 SysPost/HwPlan 等模块
- Controller 继承 `BaseController`，分页用 `startPage()` + `getDataTable()`，普通响应用 `AjaxResult`
- 权限注解 `@PreAuthorize("@ss.hasPermi('ai:实体:操作')")`
- Mapper 是纯接口不加注解，SQL 写在 `resources/mapper/system/` 下同名 XML
- 前端菜单来自数据库 `sys_menu` 表 → 后端 `/getRouters` 接口 → 前端动态渲染，**不要硬编码菜单**
- 表名 `ai_xxx`，Java 类名 `AiXxx`，实体继承 `BaseEntity`（自带 createBy/createTime/updateBy/updateTime/remark/params）

---

## 二、需求概述

### 2.1 侧边栏菜单变更

```
首页 → 系统管理 → 系统监控 → 系统工具 → 作业管理 → AI算法仓 → 若依官网
```

"AI算法仓"下含两个子菜单：**模型管理**、**人脸注册**。

### 2.2 两个子模块

| 子模块 | 路径 | 权限前缀 | 功能 |
|--------|------|---------|------|
| 模型管理 | `/ai/model/**` | `ai:model:*` | AI 模型元数据管理（CRUD + 部署），含示例占位模型 |
| 人脸注册 | `/ai/face/**` | `ai:face:*` | 从人员管理模块同步人员数据，将人脸录入 AI 模型底库，使模型能够识别该人员 |

### 2.3 模型管理 — 模型类型

| # | 类型编码 | 类型名称 | 用途说明 |
|---|---------|---------|---------|
| 1 | `target_detection` | 目标识别 | 接收摄像机图片流，识别画面中的人员、车辆、安全帽、反光衣等目标 |
| 2 | `face_recognition` | 人脸匹配 | 基于目标识别结果，对人脸目标进行特征提取与底库比对，返回人员身份 |
| 3 | `ppe_detection` | 穿戴识别 | 识别安全帽、反光衣等 PPE 穿戴情况，输出未穿戴告警 |

### 2.4 模型管理 — 状态流转

```
未部署(0) → 已部署(1) → 运行中(2) → 异常(3) / 已下线(4)
```

### 2.5 人脸注册 — 核心业务逻辑

**数据来源**：从人员管理模块（`tb_worker` + `tb_worker_face` 表）读取已上传人脸照片的人员数据。

**页面展示**：
- 以表格形式展示所有人员（姓名、人员ID、人脸照片缩略图、人脸采集时间、AI录入状态、关联模型）
- 操作员可筛选：按录入状态（未录入/已录入/录入失败）、按姓名搜索
- 每条记录展示该人员在 `tb_worker_face` 中的最新人脸照片

**"录入"操作流程**（操作员点击录入按钮）：
1. 检查该人员是否在 `tb_worker_face` 中有人脸照片 → 无人脸照片则提示"该人员尚未上传人脸照片，请先在人员管理模块完成人脸采集"
2. 检查是否已录入到 AI 模型（查 `ai_face_register` 表中是否已有 `worker_id + model_code` 且 `register_status='1'`） → 已录入则提示"该人员已录入，无需重复操作"
3. 调用 AI 推理服务（当前为桩实现）对人脸照片进行特征提取
4. 将提取的特征向量写入 `ai_face_register` 表，状态设为 `1`（已录入）
5. 同步更新 `tb_worker.face_status = '1'`（如果之前为 '0'）
6. 操作员看到状态变为"已录入"，此后该人员可被 AI 人脸匹配模型识别

**设计理念**：
- `tb_worker_face` 是原始人脸数据存储（用户上传的照片），`ai_face_register` 是 AI 模型底库注册记录（模型能否识别该人员）
- 两表分离：人员管理模块负责照片采集，AI 算法仓负责模型注册
- 当前 AI 特征提取为桩实现（返回模拟特征向量），真实模型训练好后替换即可

### 2.6 人脸注册 — 录入状态

```
未录入(0) → 已录入(1) / 录入失败(2)
```

---

## 三、你需要创建/修改的全部内容

---

### 第一部分：数据库 SQL（新建 `sql/aiLib.sql` 独立文件）

#### 1.1 表一：ai_model（AI算法模型表）

```sql
DROP TABLE IF EXISTS ai_model;
CREATE TABLE ai_model (
  model_id           BIGINT(20)     NOT NULL AUTO_INCREMENT  COMMENT '模型ID',
  model_name         VARCHAR(100)   NOT NULL                  COMMENT '模型名称',
  model_type         VARCHAR(50)    NOT NULL                  COMMENT '模型类型（target_detection人脸检测/face_recognition人脸匹配/ppe_detection穿戴识别）',
  model_code         VARCHAR(50)    NOT NULL                  COMMENT '模型编码（唯一标识，格式如 yolo_v8_obj_001）',
  version            VARCHAR(20)    DEFAULT ''                COMMENT '版本号（如 1.0.0）',
  framework          VARCHAR(50)    DEFAULT ''                COMMENT 'AI框架（PyTorch/TensorFlow/ONNX/PaddlePaddle）',
  model_path         VARCHAR(500)   DEFAULT ''                COMMENT '模型文件路径或URI',
  config_json        TEXT                                     COMMENT '模型推理参数配置（JSON格式，如置信度阈值、输入尺寸、NMS阈值等）',
  metrics            TEXT                                     COMMENT '模型评估指标（JSON格式，如mAP、Accuracy、Precision、Recall、F1-Score、FPS等）',
  input_format       VARCHAR(200)   DEFAULT ''                COMMENT '输入格式说明（如：图片base64, 640x640 RGB）',
  output_format      VARCHAR(200)   DEFAULT ''                COMMENT '输出格式说明（如：[{bbox, class, confidence}, ...]）',
  provider           VARCHAR(100)   DEFAULT ''                COMMENT '模型提供方（如：内部训练/第三方/开源预训练）',
  thumbnail          VARCHAR(500)   DEFAULT ''                COMMENT '模型缩略图/架构图URL',
  status             CHAR(1)        DEFAULT '0'               COMMENT '状态（0未部署 1已部署 2运行中 3异常 4已下线）',
  description        VARCHAR(2000)  DEFAULT ''                COMMENT '模型功能描述',
  create_by          VARCHAR(64)    DEFAULT ''                COMMENT '创建者',
  create_time        DATETIME                                 COMMENT '创建时间',
  update_by          VARCHAR(64)    DEFAULT ''                COMMENT '更新者',
  update_time        DATETIME                                 COMMENT '更新时间',
  remark             VARCHAR(500)   DEFAULT NULL              COMMENT '备注',
  PRIMARY KEY (model_id),
  UNIQUE KEY uk_model_code (model_code)
) ENGINE=InnoDB COMMENT='AI算法模型表';
```

#### 1.2 表二：ai_face_register（AI人脸注册表）

```sql
DROP TABLE IF EXISTS ai_face_register;
CREATE TABLE ai_face_register (
  register_id        BIGINT(20)     NOT NULL AUTO_INCREMENT  COMMENT '注册记录ID',
  worker_id          BIGINT(20)     NOT NULL                  COMMENT '人员ID（关联tb_worker.id）',
  worker_name        VARCHAR(50)    NOT NULL                  COMMENT '人员姓名（冗余，方便查询）',
  face_img_url       VARCHAR(500)   DEFAULT ''                COMMENT '人脸照片URL（从tb_worker_face同步）',
  face_feature       TEXT                                     COMMENT '人脸特征向量（AI模型提取，JSON格式存储）',
  model_code         VARCHAR(50)    NOT NULL                  COMMENT '关联模型编码（关联ai_model.model_code，如 arcface_r100_001）',
  model_name         VARCHAR(100)   DEFAULT ''                COMMENT '关联模型名称（冗余）',
  register_status    CHAR(1)        DEFAULT '0'               COMMENT '录入状态（0未录入 1已录入 2录入失败）',
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
) ENGINE=InnoDB COMMENT='AI人脸注册表（记录人脸是否已录入AI模型底库）';
```

#### 1.3 sys_menu 菜单数据

```sql
-- 1. 更新若依官网排序号（如已更新则跳过）
UPDATE sys_menu SET order_num = 6 WHERE menu_id = 4;

-- 2. 一级目录"AI算法仓"
INSERT INTO sys_menu VALUES
((SELECT MAX(menu_id)+1 FROM sys_menu m), 'AI算法仓', '0', '5', 'ailib', NULL, '', '', 1, 0, 'M', '0', '0', '', 'guide', 'admin', SYSDATE(), '', NULL, 'AI算法仓目录');

-- 3. 二级菜单
SET @ailib_id = (SELECT menu_id FROM sys_menu WHERE menu_name = 'AI算法仓' AND parent_id = 0);

INSERT INTO sys_menu VALUES
(@ailib_id+1, '模型管理', @ailib_id, '1', 'model', 'ai/model/index', '', 'AiModel', 1, 0, 'C', '0', '0', 'ai:model:list', 'build', 'admin', SYSDATE(), '', NULL, 'AI模型管理菜单'),
(@ailib_id+2, '人脸注册', @ailib_id, '2', 'face', 'ai/face/index',   '', 'AiFace',   1, 0, 'C', '0', '0', 'ai:face:list',   'user',  'admin', SYSDATE(), '', NULL, 'AI人脸注册菜单');

-- 4. 模型管理按钮权限（注意：menu_id 用更大的偏移量避免与人脸注册按钮碰撞）
SET @model_id = (SELECT menu_id FROM sys_menu WHERE perms = 'ai:model:list');

INSERT INTO sys_menu VALUES
(@model_id+1,  '模型查询', @model_id, '1', '#', '', '', '', 1, 0, 'F', '0', '0', 'ai:model:query',    '#', 'admin', SYSDATE(), '', NULL, ''),
(@model_id+2,  '模型新增', @model_id, '2', '#', '', '', '', 1, 0, 'F', '0', '0', 'ai:model:add',      '#', 'admin', SYSDATE(), '', NULL, ''),
(@model_id+3,  '模型修改', @model_id, '3', '#', '', '', '', 1, 0, 'F', '0', '0', 'ai:model:edit',     '#', 'admin', SYSDATE(), '', NULL, ''),
(@model_id+4,  '模型删除', @model_id, '4', '#', '', '', '', 1, 0, 'F', '0', '0', 'ai:model:remove',   '#', 'admin', SYSDATE(), '', NULL, ''),
(@model_id+5,  '模型部署', @model_id, '5', '#', '', '', '', 1, 0, 'F', '0', '0', 'ai:model:deploy',   '#', 'admin', SYSDATE(), '', NULL, ''),
(@model_id+11, '模型导出', @model_id, '6', '#', '', '', '', 1, 0, 'F', '0', '0', 'ai:model:export',   '#', 'admin', SYSDATE(), '', NULL, '');

-- 5. 人脸注册按钮权限
SET @face_id = (SELECT menu_id FROM sys_menu WHERE perms = 'ai:face:list');

INSERT INTO sys_menu VALUES
(@face_id+1, '注册查询', @face_id, '1', '#', '', '', '', 1, 0, 'F', '0', '0', 'ai:face:query',    '#', 'admin', SYSDATE(), '', NULL, ''),
(@face_id+2, '人脸录入', @face_id, '2', '#', '', '', '', 1, 0, 'F', '0', '0', 'ai:face:register', '#', 'admin', SYSDATE(), '', NULL, ''),
(@face_id+3, '取消注册', @face_id, '3', '#', '', '', '', 1, 0, 'F', '0', '0', 'ai:face:cancel',   '#', 'admin', SYSDATE(), '', NULL, ''),
(@face_id+4, '批量录入', @face_id, '4', '#', '', '', '', 1, 0, 'F', '0', '0', 'ai:face:batch',    '#', 'admin', SYSDATE(), '', NULL, '');
```

#### 1.4 数据字典

```sql
-- 字典组1：AI模型类型
INSERT INTO sys_dict_type VALUES (NULL, 'ai_model_type', 'AI模型类型', '0', 'admin', SYSDATE(), '', NULL, 'AI算法仓-模型类型字典');
SET @dt1 = (SELECT dict_id FROM sys_dict_type WHERE dict_type = 'ai_model_type');
INSERT INTO sys_dict_data VALUES
(NULL, 1, '目标识别', 'target_detection', 'ai_model_type', '', '', 'N', '0', 'admin', SYSDATE(), '', NULL, ''),
(NULL, 2, '人脸匹配', 'face_recognition', 'ai_model_type', '', '', 'N', '0', 'admin', SYSDATE(), '', NULL, ''),
(NULL, 3, '穿戴识别', 'ppe_detection',    'ai_model_type', '', '', 'N', '0', 'admin', SYSDATE(), '', NULL, '');

-- 字典组2：AI模型状态
INSERT INTO sys_dict_type VALUES (NULL, 'ai_model_status', 'AI模型状态', '0', 'admin', SYSDATE(), '', NULL, 'AI算法仓-模型状态字典');
SET @dt2 = (SELECT dict_id FROM sys_dict_type WHERE dict_type = 'ai_model_status');
INSERT INTO sys_dict_data VALUES
(NULL, 1, '未部署', '0', 'ai_model_status', '', '', 'N', '0', 'admin', SYSDATE(), '', NULL, ''),
(NULL, 2, '已部署', '1', 'ai_model_status', '', '', 'N', '0', 'admin', SYSDATE(), '', NULL, ''),
(NULL, 3, '运行中', '2', 'ai_model_status', '', '', 'N', '0', 'admin', SYSDATE(), '', NULL, ''),
(NULL, 4, '异常',   '3', 'ai_model_status', '', '', 'N', '0', 'admin', SYSDATE(), '', NULL, ''),
(NULL, 5, '已下线', '4', 'ai_model_status', '', '', 'N', '0', 'admin', SYSDATE(), '', NULL, '');

-- 字典组3：AI框架
INSERT INTO sys_dict_type VALUES (NULL, 'ai_framework', 'AI框架', '0', 'admin', SYSDATE(), '', NULL, 'AI算法仓-AI框架字典');
SET @dt3 = (SELECT dict_id FROM sys_dict_type WHERE dict_type = 'ai_framework');
INSERT INTO sys_dict_data VALUES
(NULL, 1, 'PyTorch',      'pytorch',      'ai_framework', '', '', 'N', '0', 'admin', SYSDATE(), '', NULL, ''),
(NULL, 2, 'TensorFlow',   'tensorflow',   'ai_framework', '', '', 'N', '0', 'admin', SYSDATE(), '', NULL, ''),
(NULL, 3, 'ONNX',         'onnx',         'ai_framework', '', '', 'N', '0', 'admin', SYSDATE(), '', NULL, ''),
(NULL, 4, 'PaddlePaddle', 'paddlepaddle', 'ai_framework', '', '', 'N', '0', 'admin', SYSDATE(), '', NULL, '');

-- 字典组4：AI人脸录入状态
INSERT INTO sys_dict_type VALUES (NULL, 'ai_face_register_status', 'AI人脸录入状态', '0', 'admin', SYSDATE(), '', NULL, 'AI算法仓-人脸录入状态字典');
SET @dt4 = (SELECT dict_id FROM sys_dict_type WHERE dict_type = 'ai_face_register_status');
INSERT INTO sys_dict_data VALUES
(NULL, 1, '未录入',   '0', 'ai_face_register_status', '', '', 'N', '0', 'admin', SYSDATE(), '', NULL, ''),
(NULL, 2, '已录入',   '1', 'ai_face_register_status', '', '', 'N', '0', 'admin', SYSDATE(), '', NULL, ''),
(NULL, 3, '录入失败', '2', 'ai_face_register_status', '', '', 'N', '0', 'admin', SYSDATE(), '', NULL, '');
```

#### 1.5 示例模型数据（占位，等待真实模型训练完成后替换 model_path）

```sql
INSERT INTO ai_model VALUES
(1, 'YOLOv8-施工场景目标检测', 'target_detection', 'yolo_v8_obj_001', '8.0.0', 'pytorch',
 '/models/yolo_v8_construction.pt',
 '{"confidence_threshold": 0.5, "iou_threshold": 0.45, "input_size": "640x640", "max_detections": 100, "classes": ["person","vehicle","hardhat","vest","face"]}',
 '{"mAP50": 0.892, "mAP50-95": 0.675, "precision": 0.87, "recall": 0.85, "f1_score": 0.86, "fps": 45, "test_dataset": "construction_coco_5000", "test_date": "2026-05-15"}',
 '图片base64或图片URL，640x640 RGB三通道', '[{"bbox":[x1,y1,x2,y2],"class":"person","confidence":0.95},...]',
 '内部训练', '', '0',
 '基于YOLOv8在施工场景数据集上微调的目标检测模型，可同时检测人员、车辆、安全帽、反光衣、人脸5类目标。部署时替换为实际模型文件路径即可。',
 'admin', SYSDATE(), '', NULL, '示例模型-待替换为真实训练模型');

INSERT INTO ai_model VALUES
(2, 'ArcFace-施工人员人脸匹配', 'face_recognition', 'arcface_r100_001', '1.0.0', 'onnx',
 '/models/arcface_resnet100.onnx',
 '{"embedding_dim": 512, "similarity_threshold": 0.65, "input_size": "112x112", "normalize": true, "preprocess": "mtcnn_detect_align"}',
 '{"accuracy": 0.965, "precision": 0.94, "recall": 0.93, "f1_score": 0.935, "far@1e-4": 0.001, "test_dataset": "worker_face_2000", "test_date": "2026-05-20"}',
 '对齐后的人脸图片base64，112x112 RGB', '{"matched":true/false,"person_name":"张三","confidence":0.92,"worker_id":1}',
 '开源预训练+内部微调', '', '0',
 '基于ArcFace ResNet100的人脸识别模型，支持1:1验证和1:N检索。人员通过"人脸注册"页面录入底库后即可被本模型识别。部署时替换为实际模型文件路径即可。',
 'admin', SYSDATE(), '', NULL, '示例模型-待替换为真实训练模型');

INSERT INTO ai_model VALUES
(3, 'PPE-Detect-安全穿戴识别', 'ppe_detection', 'ppe_detect_v1_001', '1.0.0', 'pytorch',
 '/models/ppe_detect_v1.pt',
 '{"confidence_threshold": 0.45, "input_size": "640x640", "classes": ["helmet_on","helmet_off","vest_on","vest_off"], "alert_on": ["helmet_off","vest_off"]}',
 '{"mAP50": 0.918, "mAP50-95": 0.723, "precision": 0.91, "recall": 0.89, "f1_score": 0.90, "fps": 38, "test_dataset": "ppe_dataset_3000", "test_date": "2026-05-25"}',
 '图片base64或图片URL，640x640 RGB三通道', '[{"bbox":[x1,y1,x2,y2],"class":"helmet_off","confidence":0.88,"alert":true},...]',
 '内部训练', '', '0',
 '安全帽+反光衣穿戴检测模型，识别人员是否佩戴安全帽、穿着反光衣。对未穿戴情况输出告警标记。部署时替换为实际模型文件路径即可。',
 'admin', SYSDATE(), '', NULL, '示例模型-待替换为真实训练模型');
```

#### 1.6 人脸注册示例数据（关联已有的测试人员）

```sql
-- 假设 tb_worker 中已有 id=1（张三）、id=2（李四）的测试数据，且 tb_worker_face 中已有对应的人脸照片
-- 为张三（worker_id=1）创建一条"未录入"记录（等待操作员点击录入）
-- 为李四（worker_id=2）创建一条"已录入"记录（演示已完成录入的效果）

INSERT INTO ai_face_register VALUES
(1, 1, '张三', '/profile/upload/2026/06/01/face_zhangsan.jpg', NULL,
 'arcface_r100_001', 'ArcFace-施工人员人脸匹配',
 '0', NULL, '', 'admin', SYSDATE(), '', NULL, '待操作员点击录入');

INSERT INTO ai_face_register VALUES
(2, 2, '李四', '/profile/upload/2026/06/01/face_lisi.jpg',
 '{"dim":512,"features":"<placeholder_vector_base64>"}',
 'arcface_r100_001', 'ArcFace-施工人员人脸匹配',
 '1', SYSDATE(), '', 'admin', SYSDATE(), 'admin', SYSDATE(), '已录入示例-后续真实模型替换特征向量');
```

---

### 第二部分：后端 Java 文件

请在以下位置创建文件。**每个模块完全参考 RuoYi 已有的 SysPost/HwPlan 模式**。

---

#### 模块 A：AI模型管理（AiModel）

**AiModel.java** — `ruoyi-system/src/main/java/com/ruoyi/system/domain/AiModel.java`
- 继承 `BaseEntity`
- 字段：`modelId(Long)`, `modelName(String)`, `modelType(String)`, `modelCode(String)`, `version(String)`, `framework(String)`, `modelPath(String)`, `configJson(String)`, `metrics(String)`, `inputFormat(String)`, `outputFormat(String)`, `provider(String)`, `thumbnail(String)`, `status(String)`, `description(String)`
- `getModelName()` 上加 `@NotBlank(message = "模型名称不能为空")`
- `getModelType()` 上加 `@NotBlank(message = "模型类型不能为空")`
- `getModelCode()` 上加 `@NotBlank(message = "模型编码不能为空")`
- `toString()` 使用 `ToStringBuilder` + `MULTI_LINE_STYLE`

**AiModelMapper.java** — `ruoyi-system/src/main/java/com/ruoyi/system/mapper/AiModelMapper.java`
- 纯接口，7 个方法：
  - `selectAiModelList(AiModel)` — 条件查询，支持 `model_name LIKE`、`model_type =`、`status =`、`framework =`、`create_time BETWEEN`
  - `selectAiModelById(Long)` — 按 ID 查询
  - `selectAiModelByCode(String)` — 按 modelCode 查询（唯一性校验用，人脸注册时关联用）
  - `insertAiModel(AiModel)` — 新增
  - `updateAiModel(AiModel)` — 更新
  - `deleteAiModelById(Long)` — 单删
  - `deleteAiModelByIds(Long[])` — 批量删除

**AiModelMapper.xml** — `ruoyi-system/src/main/resources/mapper/system/AiModelMapper.xml`
- namespace 指向 AiModelMapper 全限定名
- resultMap 映射所有字段（下划线→驼峰）
- `selectAiModelList`：条件查询，参数类型 AiModel，结果按 `create_time DESC`
- `selectAiModelById`：按 `model_id`
- `selectAiModelByCode`：按 `model_code`
- `insertAiModel`：插入所有字段，`create_time = sysdate()`
- `updateAiModel`：`<set>` + `<if>`，WHERE `model_id`
- `deleteAiModelById` / `deleteAiModelByIds`：WHERE `model_id`

**IAiModelService.java** — `ruoyi-system/src/main/java/com/ruoyi/system/service/IAiModelService.java`
- 8 个方法签名（7 个 CRUD + 1 个部署）

**AiModelServiceImpl.java** — `ruoyi-system/src/main/java/com/ruoyi/system/service/impl/AiModelServiceImpl.java`
- `@Service`，`@Autowired AiModelMapper`
- insert 前校验 modelCode 唯一性 → 重复抛 `ServiceException("模型编码已存在")`
- `deployAiModel(Long modelId)`：校验存在 + status='0' → 设置 status='1' → 更新（桩实现，后续接真实模型加载逻辑）

**AiModelController.java** — `ruoyi-admin/src/main/java/com/ruoyi/web/controller/ai/AiModelController.java`
- `@RestController`，`@RequestMapping("/ai/model")`，继承 `BaseController`
- `GET /list` → `@PreAuthorize("@ss.hasPermi('ai:model:list')")` + `startPage()` + `getDataTable()`
- `GET /{modelId}` → `@PreAuthorize("@ss.hasPermi('ai:model:query')")` + `success(model)`
- `GET /code/{modelCode}` → `@PreAuthorize("@ss.hasPermi('ai:model:query')")` + `success(model)`
- `POST /` → `@PreAuthorize("@ss.hasPermi('ai:model:add')")` + `@Log` + `@Validated` + try-catch → `toAjax(rows)`
- `PUT /` → `@PreAuthorize("@ss.hasPermi('ai:model:edit')")` + `@Log` + `toAjax(rows)`
- `PUT /deploy/{modelId}` → `@PreAuthorize("@ss.hasPermi('ai:model:deploy')")` + `@Log` + `toAjax(rows)`
- `DELETE /{modelIds}` → `@PreAuthorize("@ss.hasPermi('ai:model:remove')")` + `@Log` + `toAjax(rows)`

---

#### 模块 B：AI人脸注册（AiFaceRegister）

**核心说明**：本模块的数据来源于 `tb_worker`（人员基础信息）+ `tb_worker_face`（人脸照片），录入结果写入 `ai_face_register`。

**AiFaceRegister.java** — `ruoyi-system/src/main/java/com/ruoyi/system/domain/AiFaceRegister.java`
- 继承 `BaseEntity`
- 字段：`registerId(Long)`, `workerId(Long)`, `workerName(String)`, `faceImgUrl(String)`, `faceFeature(String)`, `modelCode(String)`, `modelName(String)`, `registerStatus(String)`, `registerTime(Date)`, `failReason(String)`
- `getRegisterTime()` 上加 `@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")`
- `toString()` 使用 `ToStringBuilder` + `MULTI_LINE_STYLE`

**AiFaceRegisterMapper.java** — `ruoyi-system/src/main/java/com/ruoyi/system/mapper/AiFaceRegisterMapper.java`
- 纯接口，新增人脸注册专用查询方法：
```java
public interface AiFaceRegisterMapper {

    /**
     * 查询人脸注册列表（LEFT JOIN tb_worker + tb_worker_face，展示所有人员的人脸注册情况）
     * @param params 查询参数 Map，支持：
     *   - workerName: 人员姓名模糊搜索
     *   - registerStatus: 录入状态精确筛选（NULL时查询所有状态）
     * @return 人脸注册VO列表
     */
    List<AiFaceRegister> selectFaceRegisterList(@Param("workerName") String workerName,
                                                 @Param("registerStatus") String registerStatus);

    /** 按 register_id 查询单条 */
    AiFaceRegister selectFaceRegisterById(Long registerId);

    /** 按 worker_id + model_code 查询（判断是否已录入） */
    AiFaceRegister selectByWorkerIdAndModelCode(@Param("workerId") Long workerId,
                                                 @Param("modelCode") String modelCode);

    /** 新增注册记录 */
    int insertAiFaceRegister(AiFaceRegister entity);

    /** 更新注册记录（写入特征向量、状态等） */
    int updateAiFaceRegister(AiFaceRegister entity);

    /** 按 register_id 删除 */
    int deleteAiFaceRegisterById(Long registerId);

    /** 批量删除 */
    int deleteAiFaceRegisterByIds(Long[] registerIds);
}
```

**AiFaceRegisterMapper.xml** — `ruoyi-system/src/main/resources/mapper/system/AiFaceRegisterMapper.xml`
- namespace 指向 AiFaceRegisterMapper 全限定名
- resultMap 映射所有字段（下划线→驼峰）
- **`selectFaceRegisterList`**（核心查询）：
  ```xml
  <select id="selectFaceRegisterList" resultMap="AiFaceRegisterResult">
      SELECT
          w.id           AS worker_id,
          w.worker_name,
          w.face_status,
          wf.face_img_url,
          wf.collect_time,
          afr.register_id,
          afr.face_feature,
          afr.model_code,
          afr.model_name,
          afr.register_status,
          afr.register_time,
          afr.fail_reason
      FROM tb_worker w
      LEFT JOIN tb_worker_face wf ON w.id = wf.worker_id
      LEFT JOIN ai_face_register afr ON w.id = afr.worker_id
      WHERE w.del_flag = '0'
      <if test="workerName != null and workerName != ''">
          AND w.worker_name LIKE CONCAT('%', #{workerName}, '%')
      </if>
      <if test="registerStatus != null and registerStatus != ''">
          AND (afr.register_status = #{registerStatus}
               OR (afr.register_status IS NULL AND #{registerStatus} = '0'))
      </if>
      ORDER BY w.id ASC
  </select>
  ```
  > **说明**：三表 LEFT JOIN 确保所有有效人员都出现在列表中。未在 `ai_face_register` 中有记录的人员，`register_status` 返回 NULL，前端视为"未录入"。
- `selectFaceRegisterById`：按 `register_id` 查单条
- `selectByWorkerIdAndModelCode`：按 `worker_id` + `model_code`
- `insertAiFaceRegister`：插入所有字段，`create_time = sysdate()`
- `updateAiFaceRegister`：`<set>` + `<if>`，WHERE `register_id`
- `deleteAiFaceRegisterById` / `deleteAiFaceRegisterByIds`：WHERE `register_id`

**IAiFaceRegisterService.java** — `ruoyi-system/src/main/java/com/ruoyi/system/service/IAiFaceRegisterService.java`
```java
public interface IAiFaceRegisterService {

    /** 查询人脸注册列表（含tb_worker联表数据，分页） */
    List<AiFaceRegister> selectFaceRegisterList(String workerName, String registerStatus);

    /** 按ID查询 */
    AiFaceRegister selectFaceRegisterById(Long registerId);

    /**
     * 执行人脸录入（核心方法）
     * 1. 校验人员存在且有人脸照片
     * 2. 校验未重复录入
     * 3. 调AI推理服务提取特征向量（当前为桩实现）
     * 4. 写入/更新 ai_face_register 表
     * 5. 同步更新 tb_worker.face_status = '1'
     *
     * @param workerId  人员ID
     * @param modelCode AI模型编码（人脸匹配类型模型）
     * @return 录入结果
     */
    int registerFace(Long workerId, String modelCode);

    /** 取消注册（将状态改回未录入，清空特征向量） */
    int cancelRegister(Long registerId);

    /** 批量录入 */
    int batchRegister(Long[] workerIds, String modelCode);

    /** 删除注册记录 */
    int deleteAiFaceRegisterByIds(Long[] registerIds);
}
```

**AiFaceRegisterServiceImpl.java** — `ruoyi-system/src/main/java/com/ruoyi/system/service/impl/AiFaceRegisterServiceImpl.java`
- `@Service`，`@Autowired AiFaceRegisterMapper`，`@Autowired TbWorkerMapper`（已有的），`@Autowired IAiInferenceService`（AI推理桩）
- `registerFace(Long workerId, String modelCode)` 核心业务逻辑：
  1. 调 `AiModelMapper.selectAiModelByCode(modelCode)` 校验模型存在且 `model_type='face_recognition'`，否则抛异常
  2. 调 `AiFaceRegisterMapper.selectByWorkerIdAndModelCode(workerId, modelCode)` — 如已有 `register_status='1'` 则抛 `ServiceException("该人员已录入到模型，无需重复操作")`
  3. 调 `TbWorkerMapper.selectTbWorkerById(workerId)` 校验人员存在
  4. 调已有的 `TbWorkerFaceMapper` 查询该人员的 `face_img_url`（取最新一条），如无则抛 `ServiceException("该人员尚未上传人脸照片，请先在人员管理模块完成人脸采集")`
  5. 调 `IAiInferenceService.infer(modelCode, faceImgUrl)` 提取人脸特征向量 → 当前桩返回 `AiInferenceResult.fail(...)`，抛 `ServiceException("AI推理服务尚未配置，请联系管理员部署真实模型后重试")`
  6. 如已有旧记录（status='0'或'2'）→ update；否则 → insert。写入特征向量、状态='1'、录入时间
  7. 调 `TbWorkerMapper` 更新 `face_status = '1'`（如果当前为 '0'）
- `cancelRegister(Long registerId)`：校验存在 + status='1' → 设置 status='0' + 清空 feature + 写 fail_reason="操作员手动取消注册"
- `batchRegister(Long[] workerIds, String modelCode)`：遍历调用 `registerFace`，捕获每个的异常，汇总返回成功/失败数

**AiFaceRegisterController.java** — `ruoyi-admin/src/main/java/com/ruoyi/web/controller/ai/AiFaceRegisterController.java`
- `@RestController`，`@RequestMapping("/ai/face")`，继承 `BaseController`
- `GET /list` → `@PreAuthorize("@ss.hasPermi('ai:face:list')")` + `startPage()` + `getDataTable()`
- `GET /{registerId}` → `@PreAuthorize("@ss.hasPermi('ai:face:query')")` + `success(record)`
- `POST /register/{workerId}` → `@PreAuthorize("@ss.hasPermi('ai:face:register')")` + `@Log(title="人脸注册", businessType=INSERT)`
  - 请求参数：`@PathVariable Long workerId`, `@RequestParam(defaultValue = "arcface_r100_001") String modelCode`
  - try-catch ServiceException → `error(e.getMessage())`，成功 → `success("人脸录入成功")`
- `POST /batchRegister` → `@PreAuthorize("@ss.hasPermi('ai:face:batch')")` + `@Log(title="人脸注册", businessType=INSERT)`
  - 请求体：`{workerIds: [1,2,3], modelCode: "arcface_r100_001"}` → `success("批量录入完成：成功N人，失败M人")`
- `PUT /cancel/{registerId}` → `@PreAuthorize("@ss.hasPermi('ai:face:cancel')")` + `@Log(title="人脸注册", businessType=UPDATE)` + `toAjax(rows)`
- `DELETE /{registerIds}` → `@PreAuthorize("@ss.hasPermi('ai:face:remove')")` + 暂不设置权限字符串（如需要可在 sys_menu 追加）+ `toAjax(rows)`

---

#### 模块 C：AI推理服务接口（桩实现，为后续真实模型接入预留扩展点）

**IAiInferenceService.java** — `ruoyi-common/src/main/java/com/ruoyi/common/service/IAiInferenceService.java`
```java
package com.ruoyi.common.service;

import java.util.Map;

/**
 * AI推理服务接口
 * 当前为桩实现，后续接入真实AI模型时替换实现类即可
 */
public interface IAiInferenceService {

    /**
     * 执行模型推理
     * @param modelCode 模型编码
     * @param input 输入数据（图片base64、图片URL等）
     * @return AiInferenceResult 推理结果
     */
    AiInferenceResult infer(String modelCode, String input);

    /**
     * 从人脸图片提取特征向量（供人脸注册使用）
     * @param modelCode 人脸匹配模型编码
     * @param faceImgUrl 人脸图片URL
     * @return AiInferenceResult.resultJson 包含特征向量JSON
     */
    AiInferenceResult extractFaceFeature(String modelCode, String faceImgUrl);

    /**
     * 检测模型是否可用
     * @param modelCode 模型编码
     * @return true可用 false不可用
     */
    boolean isModelAvailable(String modelCode);

    /**
     * 获取模型状态信息
     * @param modelCode 模型编码
     * @return 模型状态信息Map
     */
    Map<String, Object> getModelStatus(String modelCode);
}
```

**AiInferenceResult.java** — `ruoyi-common/src/main/java/com/ruoyi/common/service/AiInferenceResult.java`
- 字段：`success(boolean)`, `modelCode(String)`, `resultJson(String)`（推理结果JSON）, `processingTimeMs(long)`, `errorMessage(String)`
- 静态工厂：`success(modelCode, resultJson, processingTimeMs)`、`fail(modelCode, errorMessage)`
- getter/setter + `toString()`

**StubAiInferenceService.java** — `ruoyi-framework/src/main/java/com/ruoyi/framework/service/StubAiInferenceService.java`
- `@Component`，实现 `IAiInferenceService`
- 所有方法返回桩数据：
  - `infer()` → `fail(modelCode, "AI推理服务尚未配置，请先部署真实模型后替换桩实现")`
  - `extractFaceFeature()` → `fail(modelCode, "AI推理服务尚未配置，无法提取人脸特征向量")`
  - `isModelAvailable()` → `false`
  - `getModelStatus()` → `Map.of("status", "not_configured", "message", "AI推理服务尚未配置")`

---

### 第三部分：前端文件

**所有前端文件位于** `RuoYi-Vue-springboot3/ruoyi-ui/src/`

#### API 模块（2 个文件）

**`api/ai/model.js`** — 模型管理 API
```js
import request from '@/utils/request'

export function listModel(query) {
  return request({ url: '/ai/model/list', method: 'get', params: query })
}
export function getModel(modelId) {
  return request({ url: '/ai/model/' + modelId, method: 'get' })
}
export function getModelByCode(modelCode) {
  return request({ url: '/ai/model/code/' + modelCode, method: 'get' })
}
export function addModel(data) {
  return request({ url: '/ai/model', method: 'post', data: data })
}
export function updateModel(data) {
  return request({ url: '/ai/model', method: 'put', data: data })
}
export function deployModel(modelId) {
  return request({ url: '/ai/model/deploy/' + modelId, method: 'put' })
}
export function delModel(modelIds) {
  return request({ url: '/ai/model/' + modelIds, method: 'delete' })
}
```

**`api/ai/face.js`** — 人脸注册 API
```js
import request from '@/utils/request'

// 查询人脸注册列表（含tb_worker联表数据）
export function listFaceRegister(query) {
  return request({ url: '/ai/face/list', method: 'get', params: query })
}

// 查询单条注册记录
export function getFaceRegister(registerId) {
  return request({ url: '/ai/face/' + registerId, method: 'get' })
}

// 单个人脸录入（必须指定模型编码，默认 arcface_r100_001）
export function registerFace(workerId, modelCode) {
  return request({
    url: '/ai/face/register/' + workerId,
    method: 'post',
    params: { modelCode: modelCode || 'arcface_r100_001' }
  })
}

// 批量人脸录入
export function batchRegister(data) {
  return request({ url: '/ai/face/batchRegister', method: 'post', data: data })
}

// 取消注册
export function cancelRegister(registerId) {
  return request({ url: '/ai/face/cancel/' + registerId, method: 'put' })
}

// 删除注册记录
export function delFaceRegister(registerIds) {
  return request({ url: '/ai/face/' + registerIds, method: 'delete' })
}
```

---

#### Vue 页面（2 个文件）

#### 页面一：`views/ai/model/index.vue` — AI模型管理

采用**主从布局**：上方搜索+按钮栏，左侧模型列表，右侧模型详情面板。

**页面结构**：
```
搜索栏（模型名称 + 模型类型(dict) + 状态(dict) + 搜索/重置按钮）
按钮栏（新增/修改/删除/部署，v-hasPermi）
el-row
├── el-col :span="12"：el-table 模型列表
│   └── 列：modelId, modelName, modelType(dict-tag), version, framework, status(el-tag颜色标签), createTime
└── el-col :span="12"：el-card 模型详情面板（v-if="currentModel"）
    ├── 标题栏（模型名称 + 状态标签）
    ├── 基本信息区 el-descriptions（编码、类型、版本、框架、提供方、描述）
    ├── 模型路径区 + el-button 复制按钮
    ├── 输入/输出格式
    ├── 参数配置区（configJson JSON.parse 后 el-table 展示 key-value）
    └── 评估指标区（metrics JSON.parse 后 el-table 展示 key-value）
```

**新增/编辑弹窗**：
- 模型名称(必填)、模型编码(必填)、模型类型(select必填)、版本、框架(select dict)、模型路径、提供方、缩略图URL、输入格式(textarea)、输出格式(textarea)、参数配置(textarea 6行 placeholder提示JSON格式)、评估指标(textarea 6行 placeholder提示JSON格式)、状态(radio dict)、描述(textarea)

**关键 methods**：`getList()`, `handleQuery()`, `resetQuery()`, `handleAdd()`, `handleUpdate(row)`, `submitForm()`, `handleDelete(row)`, `handleDeploy(row)`, `handleRowClick(row)`, `cancel()`, `reset()`, `handleSelectionChange()`, `parseJsonSafe(str)`, `copyPath(path)`

---

#### 页面二：`views/ai/face/index.vue` — AI人脸注册

**这是本次开发的重点页面。**

页面展示所有人员的人脸注册情况，操作员可以看到哪些人已上传人脸但尚未录入AI模型，并点击"录入"按钮触发AI学习。

**页面结构**：
```
搜索栏（人员姓名 + 录入状态(dict ai_face_register_status) + 搜索/重置按钮）
按钮栏（批量录入/取消注册/删除，v-hasPermi）
提示区 el-alert（type="info"：说明操作流程和数据来源）

el-table（@selection-change）
├── el-table-column type="selection"
├── 人员ID（worker_id）
├── 姓名（worker_name）
├── 人脸照片（el-image缩略图 60x60，点击可放大 el-image-viewer）
├── 人脸采集时间（collect_time，来自tb_worker_face）
├── AI录入状态（register_status，dict-tag 颜色：未录入=info灰，已录入=success绿，录入失败=danger红）
├── 关联模型（model_name，未录入显示"-"）
├── 录入时间（register_time）
├── 失败原因（fail_reason，录入失败时显示）
└── 操作列
    ├── el-button "录入"（v-if="scope.row.registerStatus !== '1'"，v-hasPermi="['ai:face:register']"）
    │   └── 逻辑：先检查 face_img_url 是否为空 → 为空提示"该人员尚未上传人脸照片" → 不为空则弹出确认框"确认将[姓名]的人脸录入到AI识别模型？" → 确认后调 registerFace API
    ├── el-button "取消注册"（v-if="scope.row.registerStatus === '1'"，v-hasPermi="['ai:face:cancel']"）
    └── el-button "重试"（v-if="scope.row.registerStatus === '2'"，点击重新录入）
```

**关键交互细节**：
- 点击"录入"按钮时，前端先判断 `scope.row.faceImgUrl` 是否存在：不存在则 `this.$message.warning("该人员尚未上传人脸照片，请先在人员管理模块完成人脸采集")`，不发起请求
- 存在则 `this.$confirm("确认将 [" + row.workerName + "] 的人脸录入到AI识别模型？录入后该人员可被AI人脸匹配识别。", "确认录入")` → 确认后调 API
- 录入成功后 `this.$message.success("人脸录入成功")` + `getList()` 刷新
- 录入失败（AI服务未配置）时，后端返回 `error("AI推理服务尚未配置...")`，前端显示错误提示，该人员状态保持"未录入"
- 批量录入：选中多行 → 点击"批量录入" → 确认 → 调 `batchRegister` → 显示结果（成功X人，失败Y人）
- 列表默认按 `worker_id` 升序，`register_status IS NULL` 的记录前端显示为"未录入"（默认值`'0'`）

**标准 data 结构**：
```javascript
data() {
  return {
    loading: false,
    ids: [],
    single: true,
    multiple: true,
    total: 0,
    list: [],
    queryParams: {
      pageNum: 1,
      pageSize: 10,
      workerName: undefined,
      registerStatus: undefined
    },
    // 批量录入用
    batchForm: {
      workerIds: [],
      modelCode: 'arcface_r100_001'
    }
  }
}
```

**标准 methods**：`getList()`, `handleQuery()`, `resetQuery()`, `handleRegister(row)`, `handleBatchRegister()`, `handleCancelRegister(row)`, `handleRetry(row)`, `handleDelete(row)`, `handleSelectionChange()`

---

### 第四部分：依赖注入说明

在 `AiFaceRegisterServiceImpl` 中需要注入以下已有 Mapper（来自人员管理模块）：
- `TbWorkerMapper` — 查询人员信息、更新 face_status
- `TbWorkerFaceMapper` — 查询人员人脸照片 URL（取最新一条）
- `AiModelMapper` — 校验模型存在且类型正确

这些 Mapper 已在项目中存在，直接 `@Autowired` 即可。

**注意**：`ruoyi-quartz` 模块如需使用 system 的 Mapper，需在 `pom.xml` 中添加依赖。但 AI 算法仓的 Service 层在 `ruoyi-system` 模块中，本身就包含这些 Mapper，无需额外配置。

---

## 四、验证清单

所有文件创建完成后，执行以下验证：

### 模型管理验证
1. **编译后端**：`cd RuoYi-Vue-springboot3 && mvn clean install -DskipTests`，确认无编译错误
2. **启动后端 + 前端**，登录 admin/admin123
3. **菜单展示**：侧边栏出现"AI算法仓" → 展开 → "模型管理" + "人脸注册"
4. **模型列表**：显示 3 条示例模型（YOLOv8、ArcFace、PPE-Detect）
5. **模型详情**：点击行 → 右侧面板展示完整元数据，参数配置和指标结构化显示
6. **模型 CRUD**：新增 → 修改 → 编码重复校验 → 删除
7. **模型部署**：未部署模型点击部署 → 状态变为已部署
8. **复制路径**：详情面板点击复制按钮 → 路径已复制

### 人脸注册验证（核心）
9. **人脸注册页**：点击"人脸注册"菜单 → 进入页面 → 显示所有人员列表（来自 tb_worker，含人脸数据）
10. **列表数据**：确认列表展示人员ID、姓名、人脸照片缩略图（如有）、采集时间、AI录入状态（张三=未录入，李四=已录入）
11. **点击录入（无人脸照片）**：对有 face_img_url 为空的人员点击"录入" → 提示"该人员尚未上传人脸照片，请先在人员管理模块完成人脸采集"
12. **点击录入（有人脸照片）**：对张三（face_img_url 不为空，register_status='0'）点击"录入" → 弹出确认框 → 确认 → 后端返回错误"AI推理服务尚未配置"（桩行为，符合预期） → 提示用户"AI推理服务尚未配置，请联系管理员部署真实模型后重试"
13. **重复录入拦截**：对李四（register_status='1'）→ "录入"按钮不显示，改为显示"取消注册"
14. **取消注册**：对李四点击"取消注册" → 状态变为"未录入" → 特征向量被清空
15. **批量录入**：选中多行未录入人员 → 点击"批量录入" → 确认 → 显示汇总结果
16. **权限验证**：退出 admin，登录 ry/123456，确认菜单不显示"AI算法仓"
17. **字典数据**：确认 `ai_face_register_status` 字典存在且有三个值（未录入/已录入/录入失败）

### 替换真实模型后的验证（后续执行）
18. 替换 `StubAiInferenceService` 为真实实现后，再次点击"录入" → 特征向量成功写入 `ai_face_register` → 状态变为"已录入" → `tb_worker.face_status` 同步更新为 '1'

---

## 五、完成后生成开发日志

所有任务完成并验证通过后，在项目根目录生成 `开发日志-AI算法仓.md` 文件，记录本次开发的完整信息：

```
# AI算法仓模块开发日志

## 基本信息
- 开发日期：YYYY-MM-DD
- 模块名称：AI算法仓（ailib）
- 开发者：AI 自动生成

## 新增文件

### 数据库
- sql/aiLib.sql（独立文件）
  — 2 张表建表（ai_model + ai_face_register）
  — sys_menu 菜单数据（1 个一级目录 + 2 个二级菜单 + 10 条按钮权限）
  — 4 组字典数据（模型类型、模型状态、AI框架、人脸录入状态）
  — 3 条示例模型数据 + 2 条人脸注册示例数据

### 后端 Java（共 N 个文件）

#### 模型管理（6 个文件）
- ruoyi-system/.../domain/AiModel.java — AI模型实体
- ruoyi-system/.../mapper/AiModelMapper.java — AI模型Mapper接口
- ruoyi-system/.../resources/mapper/system/AiModelMapper.xml — AI模型Mapper XML
- ruoyi-system/.../service/IAiModelService.java — AI模型Service接口
- ruoyi-system/.../service/impl/AiModelServiceImpl.java — AI模型Service实现
- ruoyi-admin/.../controller/ai/AiModelController.java — AI模型Controller

#### 人脸注册（6 个文件）
- ruoyi-system/.../domain/AiFaceRegister.java — AI人脸注册实体
- ruoyi-system/.../mapper/AiFaceRegisterMapper.java — AI人脸注册Mapper接口
- ruoyi-system/.../resources/mapper/system/AiFaceRegisterMapper.xml — AI人脸注册Mapper XML（三表LEFT JOIN）
- ruoyi-system/.../service/IAiFaceRegisterService.java — AI人脸注册Service接口
- ruoyi-system/.../service/impl/AiFaceRegisterServiceImpl.java — AI人脸注册Service实现（含录入/取消/批量逻辑）
- ruoyi-admin/.../controller/ai/AiFaceRegisterController.java — AI人脸注册Controller

#### AI推理扩展点（3 个文件）
- ruoyi-common/.../service/IAiInferenceService.java — AI推理服务接口（含extractFaceFeature人脸特征提取方法）
- ruoyi-common/.../service/AiInferenceResult.java — AI推理结果DTO
- ruoyi-framework/.../service/StubAiInferenceService.java — AI推理桩实现

### 前端（共 4 个文件）
- ruoyi-ui/src/api/ai/model.js — 模型管理 API
- ruoyi-ui/src/api/ai/face.js — 人脸注册 API
- ruoyi-ui/src/views/ai/model/index.vue — 模型管理页面（主从布局）
- ruoyi-ui/src/views/ai/face/index.vue — 人脸注册页面（人员列表+录入操作）

## 数据库变更
- 新增表：ai_model（AI算法模型表）
- 新增表：ai_face_register（AI人脸注册表）
- sys_menu：新增 1 个一级目录 "AI算法仓" + 2 个二级菜单 + 10 条按钮权限
- sys_menu：更新若依官网 order_num
- sys_dict_type + sys_dict_data：新增 4 组字典
- ai_model 示例数据：3 条
- ai_face_register 示例数据：2 条

## 模块架构
AI算法仓 (ailib)
├── 模型管理 (model) — AiModelController /ai/model/**
├── 人脸注册 (face)  — AiFaceRegisterController /ai/face/**
│   └── 数据源：tb_worker + tb_worker_face（人员管理模块）
│   └── 录入流程：读取人脸照片 → AI提取特征 → 写入底库 → 同步face_status
└── AI推理 (inference) — IAiInferenceService + StubAiInferenceService（扩展点）

## 替换真实模型指南

### 模型管理替换
当前 3 条示例模型的 model_path 指向占位路径（如 /models/yolo_v8_construction.pt），实际模型文件并不存在。
当真实模型训练完成后：
1. 将模型文件放到服务器指定目录
2. 在模型管理页面修改对应模型的 model_path 为实际路径
3. 更新 config_json、metrics 为真实数据
4. 点击"部署"按钮

### 人脸识别替换
1. 实现 `IAiInferenceService` 接口（替换 `StubAiInferenceService`）：
   - `extractFaceFeature()` 调用真实 ArcFace/MTCNN 模型提取 512 维特征向量
   - `infer()` 调用模型执行人脸比对
2. 在人脸注册页面，对"未录入"人员点击"录入" → 特征提取成功 → 自动写入 ai_face_register → 状态变为"已录入"
3. 此后摄像机打卡时，调用 `infer("arcface_r100_001", captureImage)` → 在底库中 1:N 检索 → 返回匹配人员
```

> **要求**：生成时请把 `N` 替换为实际文件数量，把 `YYYY-MM-DD` 替换为实际日期，确保所有路径与实际创建的文件一致。如果开发过程中修改了任何已有文件，也一并在日志中注明。
