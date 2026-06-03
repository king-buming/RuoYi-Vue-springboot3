# AI算法仓模块开发日志

## 基本信息
- 开发日期：2026-06-03
- 模块名称：AI算法仓（ailib）
- 后端框架：Spring Boot 3.5.x + MyBatis + Spring Security 6 + JWT
- 前端框架：Vue 2.6.12 + Element UI
- 数据库：MySQL 8.x，库名 `ry-vue`
- 开发者：AI 自动生成

---

## 一、数据库变更

### 1.1 新增业务表（2 张）

| 表名 | 说明 | 主要字段 |
|------|------|---------|
| `ai_model` | AI算法模型表 | model_id, model_name, model_type（target_detection/face_recognition/ppe_detection）, model_code（UNIQUE）, version, framework, model_path, config_json（TEXT）, metrics（TEXT）, input_format, output_format, provider, status（0未部署/1已部署/2运行中/3异常/4已下线）, description |
| `ai_face_register` | AI人脸注册表 | register_id, worker_id（关联tb_worker.id）, worker_name, face_img_url, face_feature（TEXT, AI特征向量JSON）, model_code（关联ai_model.model_code）, model_name, register_status（0未录入/1已录入/2录入失败）, register_time, fail_reason。UNIQUE(worker_id, model_code) |

**文件位置**：`sql/aiLib.sql`（独立文件，与 ry_20260417.sql、quartz.sql 并列）

### 1.2 菜单权限数据

在 `sys_menu` 表中插入：

| 层级 | 菜单名称 | 权限标识 | 说明 |
|------|---------|---------|------|
| 一级目录 | AI算法仓 | `ailib` | 侧边栏一级菜单（order_num=5，位于"作业管理"之后、"若依官网"之前） |
| 二级菜单 | 模型管理 | `ai:model:list` | `AiModel` 组件，路由 `ai/model/index` |
| 二级菜单 | 人脸注册 | `ai:face:list` | `AiFace` 组件，路由 `ai/face/index` |
| 按钮权限 | 模型查询/新增/修改/删除/部署/导出 | `ai:model:query/add/edit/remove/deploy/export` | 模型管理 CRUD + 部署按钮 |
| 按钮权限 | 注册查询/人脸录入/取消注册/批量录入 | `ai:face:query/register/cancel/batch` | 人脸注册操作按钮 |

> 同时更新 `sys_menu SET order_num = 6 WHERE menu_id = 4`（若依官网排序后移）

### 1.3 数据字典

| 字典类型 | 字典名称 | 字典值 |
|---------|---------|--------|
| `ai_model_type` | AI模型类型 | target_detection（目标识别）、face_recognition（人脸匹配）、ppe_detection（穿戴识别） |
| `ai_model_status` | AI模型状态 | 0未部署、1已部署、2运行中、3异常、4已下线 |
| `ai_framework` | AI框架 | pytorch、tensorflow、onnx、paddlepaddle |
| `ai_face_register_status` | AI人脸录入状态 | 0未录入、1已录入、2录入失败 |

### 1.4 示例数据

- AI模型 ×3：YOLOv8目标检测、ArcFace人脸匹配、PPE-Detect穿戴检测（均为占位模型，model_path指向占位路径，status='0'未部署）
- 人脸注册示例 ×2：张三（未录入，待操作员点击录入）、李四（已录入示例）

---

## 二、后端文件详述（15 个文件）

### 2.1 实体层（Domain）—— 2 个文件

| 文件 | 路径 | 关键设计 |
|------|------|---------|
| `AiModel.java` | `ruoyi-system/.../domain/` | 继承 `BaseEntity`。`modelName` getter 标注 `@NotBlank(message = "模型名称不能为空")`，`modelType`、`modelCode` 同理。`configJson` 和 `metrics` 使用 `String` 存储 JSON 文本，在 XML 中映射为 TEXT 类型。`status` 字段取值 0-4（未部署→已下线）。`toString()` 含所有字段及 BaseEntity 继承字段 |
| `AiFaceRegister.java` | `ruoyi-system/.../domain/` | 继承 `BaseEntity`。包含 AI 注册核心字段（registerId, workerId, workerName, faceImgUrl, faceFeature, modelCode, modelName, registerStatus, registerTime, failReason）。额外定义 2 个非持久化字段（collectTime, faceStatus）用于接收 tb_worker + tb_worker_face 联表查询结果。`registerTime` 标注 `@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")` |

### 2.2 数据访问层（Mapper）—— 4 个文件（2 接口 + 2 XML）

#### AiModelMapper

| 方法 | 说明 |
|------|------|
| `selectAiModelList(AiModel)` | 条件查询：`model_name LIKE`、`model_type =`、`status =`、`framework =`、`create_time BETWEEN`（params.beginTime/endTime），按 `create_time DESC` |
| `selectAiModelById(Long)` | 主键查询 |
| `selectAiModelByCode(String)` | 按 model_code 唯一查询（插入时唯一性校验、人脸注册时关联用） |
| `insertAiModel(AiModel)` | 插入，`useGeneratedKeys=true`，`create_time = sysdate()` |
| `updateAiModel(AiModel)` | 动态 `<set>` 更新，`update_time = sysdate()` |
| `deleteAiModelById(Long)` | 单删 |
| `deleteAiModelByIds(Long[])` | `<foreach>` 批量删除 |

**XML 文件**：`ruoyi-system/src/main/resources/mapper/system/AiModelMapper.xml`

#### AiFaceRegisterMapper（含专用业务查询）

| 方法 | SQL 逻辑 | 用途 |
|------|---------|------|
| `selectFaceRegisterList(workerName, registerStatus)` | 三表 LEFT JOIN：`tb_worker w LEFT JOIN tb_worker_face wf ON w.id = wf.worker_id LEFT JOIN ai_face_register afr ON w.id = afr.worker_id`，WHERE `w.del_flag = '0'`，支持 workerName 模糊搜索、registerStatus 精确筛选（NULL视为未录入） | **人脸注册列表**：展示所有人员的人脸注册情况 |
| `selectFaceRegisterById(Long)` | 按 register_id 联表查单条 | 注册详情 |
| `selectByWorkerIdAndModelCode(workerId, modelCode)` | 按 worker_id + model_code 联表查询 | **重复录入检查**：判断人员是否已录入到指定模型 |
| `insertAiFaceRegister` | 标准插入 | 创建注册记录 |
| `updateAiFaceRegister` | 动态更新（特征向量、状态、失败原因等） | 更新注册状态 |
| `deleteAiFaceRegisterById` / `deleteAiFaceRegisterByIds` | 标准删除 | 删除注册记录 |

**XML 文件**：`ruoyi-system/src/main/resources/mapper/system/AiFaceRegisterMapper.xml`

### 2.3 业务逻辑层（Service）—— 4 个文件（2 接口 + 2 实现）

#### IAiModelService / AiModelServiceImpl

标准 CRUD + 部署逻辑。insert 前校验 modelCode 唯一性，重复抛 `ServiceException("模型编码已存在")`。`deployAiModel(Long modelId)` 校验 status='0' 才可部署 → 设置 status='1'。

#### IAiFaceRegisterService / AiFaceRegisterServiceImpl — 核心业务逻辑

注入 5 个依赖：`AiFaceRegisterMapper`、`AiModelMapper`、`TbWorkerMapper`（已有）、`TbWorkerFaceMapper`（已有）、`IAiInferenceService`（AI推理桩）。

| 方法 | 业务规则 |
|------|---------|
| `registerFace(workerId, modelCode)` | ① 校验 AI 模型存在且 `model_type='face_recognition'` → ② 检查 `selectByWorkerIdAndModelCode`，已录入抛异常 → ③ 校验人员存在 → ④ 从 `tb_worker_face` 取最新人脸照片 URL，无照片抛异常 → ⑤ 调 `IAiInferenceService.extractFaceFeature()` 提取特征（当前桩返回失败）→ ⑥ 写入/更新 `ai_face_register`，status='1' → ⑦ 同步更新 `tb_worker.face_status='1'` |
| `cancelRegister(registerId)` | 校验存在 + status='1' → 设置 status='0' + 清空 faceFeature + failReason="操作员手动取消注册" |
| `batchRegister(workerIds, modelCode)` | 遍历调用 `registerFace`，捕获每个异常，返回成功数 |
| `deleteAiFaceRegisterByIds` | 委托 mapper |

### 2.4 控制器层（Controller）—— 2 个文件

| 文件 | 路径 | 端点 |
|------|------|------|
| `AiModelController.java` | `ruoyi-admin/.../controller/ai/` | `GET /ai/model/list`（分页列表）、`GET /ai/model/{modelId}`（详情）、`GET /ai/model/code/{modelCode}`（按编码查）、`POST /ai/model`（新增，try-catch ServiceException）、`PUT /ai/model`（修改）、`PUT /ai/model/deploy/{modelId}`（部署，try-catch）、`DELETE /ai/model/{modelIds}`（批量删除） |
| `AiFaceRegisterController.java` | `ruoyi-admin/.../controller/ai/` | `GET /ai/face/list`（分页列表，workerName+registerStatus参数）、`GET /ai/face/{registerId}`（详情）、`POST /ai/face/register/{workerId}?modelCode=`（单个录入，try-catch）、`POST /ai/face/batchRegister`（批量录入，请求体{workerIds, modelCode}，try-catch）、`PUT /ai/face/cancel/{registerId}`（取消注册，try-catch）、`DELETE /ai/face/{registerIds}`（删除） |

全部 Controller：
- 继承 `BaseController`（获取 `startPage()`、`getDataTable()`、`toAjax()`、`success()`、`error()` 等）
- 使用 `@PreAuthorize("@ss.hasPermi('ai:xxx:xxx')")` 进行方法级权限控制
- 写操作使用 `@Log(title = "AI模型/人脸注册", businessType = ...)` 记录操作日志
- 核心业务方法使用 try-catch 捕获 ServiceException → `error(e.getMessage())`

### 2.5 AI推理扩展点 — 3 个文件

| 文件 | 包路径 | 说明 |
|------|--------|------|
| `IAiInferenceService.java` | `ruoyi-common/.../service/` | AI推理服务接口，定义 `infer(modelCode, input)`、`extractFaceFeature(modelCode, faceImgUrl)`、`isModelAvailable(modelCode)`、`getModelStatus(modelCode)` 四个方法 |
| `AiInferenceResult.java` | `ruoyi-common/.../service/` | AI推理结果 DTO，字段：success(boolean)、modelCode、resultJson、processingTimeMs、errorMessage。静态工厂：success(modelCode, resultJson, ms) / fail(modelCode, errorMessage) |
| `StubAiInferenceService.java` | `ruoyi-framework/.../service/` | `@Component` 桩实现，实现 `IAiInferenceService`。`infer()` 返回 fail("AI推理服务尚未配置...")，`extractFaceFeature()` 返回 fail("AI推理服务尚未配置，无法提取人脸特征向量")，`isModelAvailable()` 返回 false，`getModelStatus()` 返回 {status: "not_configured"} |

### 2.6 已有文件依赖

| 已有 Mapper | 用途 |
|------------|------|
| `TbWorkerMapper` | `selectTbWorkerById()` 校验人员存在；`updateTbWorker()` 同步更新 face_status |
| `TbWorkerFaceMapper` | `selectTbWorkerFaceList()` 查询人员最新人脸照片 URL |

这些 Mapper 在 `ruoyi-system` 模块中，AI 算法仓的 Service 也在 `ruoyi-system`，直接 `@Autowired` 即可，无需修改 pom.xml。

---

## 三、前端文件详述（4 个文件）

### 3.1 API 请求模块（2 个文件）

| 文件 | 导出函数 | 对应后端接口 |
|------|---------|------------|
| `src/api/ai/model.js` | `listModel`, `getModel`, `getModelByCode`, `addModel`, `updateModel`, `deployModel`, `delModel` | `GET /ai/model/list`、`GET /ai/model/{id}`、`GET /ai/model/code/{code}`、`POST /ai/model`、`PUT /ai/model`、`PUT /ai/model/deploy/{id}`、`DELETE /ai/model/{ids}` |
| `src/api/ai/face.js` | `listFaceRegister`, `getFaceRegister`, `registerFace`, `batchRegister`, `cancelRegister`, `delFaceRegister` | `GET /ai/face/list`、`GET /ai/face/{id}`、`POST /ai/face/register/{workerId}`、`POST /ai/face/batchRegister`、`PUT /ai/face/cancel/{id}`、`DELETE /ai/face/{ids}` |

所有 API 文件使用 `@/utils/request`（Axios 封装，自动注入 Token、处理 401 跳转）。

### 3.2 Vue 页面组件（2 个文件）

#### 模型管理页面 — `src/views/ai/model/index.vue`

**主从布局**：左侧模型列表（50%宽） + 右侧详情面板（50%宽）。

**搜索区域**：模型名称（el-input）、模型类型（el-select，3种字典）、状态（el-select，5种字典）

**操作按钮**：新增（`ai:model:add`）、修改（`ai:model:edit`，需选中单行）、删除（`ai:model:remove`，需选中至少一行）、部署（`ai:model:deploy`，需选中单行）

**左侧表格列**：modelId、modelName、modelType（dict-tag）、version、framework（dict-tag）、status（el-tag颜色标签：info/success/warning/danger）、createTime。行点击 `handleRowClick` 加载详情到右侧。

**右侧详情面板**（el-card，v-if="currentModel"）：
- 标题栏：模型名称 + 状态标签
- 基本信息区 el-descriptions（2列，border）：模型编码、类型、版本、框架、提供方、创建时间、描述
- 模型路径区：el-input readonly + 复制按钮（navigator.clipboard.writeText）
- 输入/输出格式区 el-descriptions
- 参数配置区：configJson JSON.parse 后 el-table 展示 key-value，解析失败显示原文
- 评估指标区：metrics JSON.parse 后 el-table 展示 key-value

**新增/编辑弹窗**（650px宽）：
- 双列：模型名称+模型编码、模型类型+版本、AI框架+提供方
- 单列：模型路径、缩略图URL、输入格式+输出格式（并列textarea）
- 参数配置(textarea 4行) + 评估指标(textarea 4行)
- 状态(radio-group 使用字典) + 描述(textarea) + 备注(textarea)
- 表单校验：modelName/modelType/modelCode 必填

**字典注册**：`dicts: ['ai_model_type', 'ai_model_status', 'ai_framework']`

#### 人脸注册页面 — `src/views/ai/face/index.vue`

**搜索区域**：人员姓名（el-input）、录入状态（el-select，3种字典）

**操作按钮**：批量录入（`ai:face:batch`）、删除记录（`ai:face:cancel`）

**提示区**：el-alert type="info"，说明数据来源（tb_worker + tb_worker_face）和操作流程，提示当前为桩实现

**表格列**：
- workerId、workerName
- 人脸照片（el-image 60x60 缩略图，支持点击放大预览，无照片时显示 el-tag "无照片"）
- 人脸采集时间（collectTime）
- AI录入状态（registerStatus，dict-tag，NULL显示为"未录入"）
- 关联模型（modelName）
- 录入时间（registerTime）
- 失败原因（failReason，红色文字）
- 操作列（3种动态按钮）：
  - registerStatus ≠ '1' → "录入"按钮（primary，ai:face:register）。点击前前端检查 faceImgUrl 是否为空 → 为空提示"尚未上传人脸照片"不发起请求 → 不为空弹出确认框 → 调 registerFace API
  - registerStatus = '1' → "取消注册"按钮（warning，ai:face:cancel）
  - registerStatus = '2' → "重试"按钮（primary，ai:face:register）

**字典注册**：`dicts: ['ai_face_register_status']`

---

## 四、模块架构总览

```
后端（Spring Boot 3.5.x）
│
├── ruoyi-common（公共层）
│   ├── service/IAiInferenceService.java       ← AI推理接口（含人脸特征提取）
│   └── service/AiInferenceResult.java         ← 推理结果 DTO
│
├── ruoyi-system（业务层）
│   ├── domain/AiModel.java                    ← AI模型实体
│   ├── domain/AiFaceRegister.java             ← AI人脸注册实体（含联表字段）
│   ├── mapper/AiModelMapper.java + .xml       ← 模型 CRUD
│   ├── mapper/AiFaceRegisterMapper.java + .xml ← 人脸注册 CRUD + 三表联查
│   ├── service/IAiModelService.java           ← 模型服务接口
│   ├── service/IAiFaceRegisterService.java    ← 人脸注册服务接口（录入/取消/批量）
│   ├── service/impl/AiModelServiceImpl.java   ← 模型服务实现（含部署逻辑）
│   └── service/impl/AiFaceRegisterServiceImpl.java ← 人脸注册服务实现（含录入核心流程）
│       └── @Autowired TbWorkerMapper（已有）+ TbWorkerFaceMapper（已有）
│
├── ruoyi-framework（框架层）
│   └── service/StubAiInferenceService.java    ← AI推理桩实现
│
└── ruoyi-admin（控制层）
    └── controller/ai/
        ├── AiModelController.java             ← /ai/model/**
        └── AiFaceRegisterController.java      ← /ai/face/**

前端（Vue 2.6 + Element UI）
│
├── src/api/ai/
│   ├── model.js                              ← 模型管理 API 封装
│   └── face.js                               ← 人脸注册 API 封装
│
└── src/views/ai/
    ├── model/index.vue                       ← 模型管理页面（主从布局：列表+详情）
    └── face/index.vue                        ← 人脸注册页面（人员列表+录入/取消/重试）
```

---

## 五、验证清单

| # | 验证项 | 状态 |
|---|--------|------|
| 1 | 后端编译 | `mvn clean install -DskipTests` → `BUILD SUCCESS` ✅ |
| 2 | 数据库 SQL 追加 | `ai_model`、`ai_face_register` 表、菜单、字典、示例数据已追加到 `ry_20260417.sql` |
| 3 | 模型管理页面 | 待启动前后端验证（搜索/CRUD/部署/详情/复制路径/权限控制） |
| 4 | 人脸注册页面 | 待启动前后端验证（人员列表/录入/取消注册/批量录入/权限控制） |
| 5 | 字典数据 | 待验证 4 组字典在系统管理→字典管理中可见 |
| 6 | 菜单展示 | 待登录验证"AI算法仓"在"作业管理"和"若依官网"之间 |
| 7 | 权限验证 | 待登录 ry/123456 验证菜单不显示 |

---

## 六、替换真实模型指南

### 模型管理替换

当前 3 条示例模型的 `model_path` 指向占位路径（如 `/models/yolo_v8_construction.pt`），实际模型文件并不存在。

当真实模型训练完成后：
1. 将模型文件放到服务器指定目录（如 `/opt/ai-models/`）
2. 在模型管理页面修改对应模型的 `model_path` 为实际路径
3. 更新 `config_json`、`metrics` 为真实参数和指标
4. 点击"部署"按钮将模型状态设为已部署

### 人脸识别替换

1. 实现 `IAiInferenceService` 接口（替换 `StubAiInferenceService`）：
   - `extractFaceFeature()` 调用真实 ArcFace/MTCNN 模型提取 512 维特征向量
   - `infer()` 调用模型执行人脸比对
2. 在人脸注册页面，对"未录入"人员点击"录入" → 特征提取成功 → 自动写入 `ai_face_register` → 状态变为"已录入"
3. 此后摄像机打卡时，调用 `infer("arcface_r100_001", captureImage)` → 在底库中 1:N 检索 → 返回匹配人员

### 替换步骤总结

| 步骤 | 操作 |
|------|------|
| 1 | 将模型文件部署到服务器 |
| 2 | 在模型管理页面更新 model_path/config_json/metrics |
| 3 | 点击部署按钮 |
| 4 | 编写 `RealAiInferenceService implements IAiInferenceService` |
| 5 | 修改 `StubAiInferenceService` 的 `@Component` 为 `@Component("stubAiInferenceService")`，新实现用 `@Component`（或使用 `@Primary`） |
| 6 | 在人脸注册页面点击录入 → 验证特征提取成功 → 状态变为已录入 |
