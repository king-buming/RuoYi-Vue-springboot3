# APP 作业管理模块开发日志

## 基本信息
- 开发日期：2026-06-04
- 模块名称：作业管理（homework）APP 移动端 — 工作台计划+审核
- 后端框架：Spring Boot 3.5.x + MyBatis
- 前端框架：uni-app (Vue 2.6.14) + uni-ui 1.5.0
- 数据库：MySQL 8.x，库名 `ry-vue`（无变更）
- 认证：HMAC-SHA256 AppToken（AppTokenFilter + AppTokenUtil）
- 基于：HWAPP.md 规格书
- 开发者：AI 自动生成

---

## 概述

在 RuoYi-App uni-app 移动端新增作业管理功能，将后台管理系统的**作业计划**和**作业审核**模块移植到移动端工作台。核心变更包括：

1. **工作台改造**：原底部 Tab "记录"改为"工作台"，作为作业管理功能入口
2. **作业计划模块**：移动端计划列表（卡片式 + 状态筛选）、计划详情、新建计划表单
3. **作业审核模块**：移动端审核列表 + 审核详情 + 通过/驳回操作
4. **移动端 API**：新增 `/app/homework/*` 8 个接口，复用现有 HMAC AppToken 鉴权
5. **角色权限**：计划创建仅施工方（非普通施工人员），审核仅作业批准人

---

## 一、数据库变更

**无变更**。完全复用已有的 `hw_plan`、`hw_review`、`hw_plan_worker` 表，V3 版本已建好。

---

## 二、后端新增/修改文件

### 2.1 新增文件（1 个）

| # | 文件 | 路径 | 说明 |
|---|------|------|------|
| 1 | `AppHomeworkController.java` | `ruoyi-admin/.../controller/app/` | 移动端作业管理接口，继承 BaseController，`@RequestMapping("/app/homework")`，注入 IHwPlanService / IHwReviewService / HwPlanMapper / HwReviewMapper / HwPlanWorkerMapper / TbWorkerMapper / TbWorkerRoleRelMapper |

**8 个 API 端点**：

| 端点 | 方法 | 说明 | 权限 |
|------|------|------|------|
| `/app/homework/plan/list` | GET | 当前工人相关的计划列表（创建的+参与的），分页 | AppToken 登录即可 |
| `/app/homework/plan/{planId}` | GET | 计划详情（含参与人员+审核记录） | AppToken 登录即可 |
| `/app/homework/plan` | POST | 创建作业计划 | 施工方（unit_type='3'）+ 非 worker 角色 |
| `/app/homework/plan/{planId}/workers` | GET | 查询计划关联的参与人员 | AppToken 登录即可 |
| `/app/homework/review/list` | GET | 审核列表（分页+状态筛选） | 作业批准人（role_code=approver） |
| `/app/homework/review/{reviewId}` | GET | 审核详情（含计划信息+参与人员） | AppToken 登录即可 |
| `/app/homework/review/approve` | PUT | 审核通过 | 作业批准人（role_code=approver） |
| `/app/homework/review/reject` | PUT | 审核驳回 | 作业批准人（role_code=approver） |

**权限校验逻辑**：

- 所有接口通过 `AppTokenUtil.getWorkerId(request)` 提取当前工人ID，返回 null 则返回 401
- 创建计划：查 TbWorker → 校验 unitType='3'（施工方）→ 查 roleCodes → 排除仅含 'worker' 的普通施工人员
- 审核列表：查 roleCodes → 不含 'approver' 则返回空列表
- 审核操作：查 roleCodes → 不含 'approver' 则返回"无审核权限"
- 审核通过/驳回调用现有 `IHwReviewService.approve()` / `IHwReviewService.reject()`，异常由 `try-catch ServiceException` 捕获

### 2.2 修改文件（2 个）

| # | 文件 | 改动内容 |
|---|------|---------|
| 1 | `HwPlanMapper.java` | 新增 `selectHwPlanListByWorkerId(@Param("workerId") Long workerId, @Param("status") String status)` 方法 |
| 2 | `HwPlanMapper.xml` | 新增 `selectHwPlanListByWorkerId` SQL：`join hw_plan_worker` + `join tb_worker`，按 workerId 查关联计划（创建的+参与的），按 status 过滤，`order by create_time desc` |

> `HwPlanWorkerMapper.selectByPlanId` 方法在 V2 版本已存在，无需新增。

---

## 三、前端新增/修改文件（uni-app）

### 3.1 新增文件（6 个）

| # | 文件 | 路径 | 说明 |
|---|------|------|------|
| 1 | `workbench.vue` | `pages/worker/` | 工作台首页（Tab2）。用户信息栏（姓名+角色+审核状态）、宫格导航（作业计划/作业审核/打卡记录）、快捷操作卡片（新建计划/待审核计划，按角色显示/隐藏）。data 含 `menus` 宫格配置、`roleCodes`/`unitType` 角色信息。computed: `canCreatePlan`（施工方非worker）、`isApprover`（含approver角色）。onShow 调 `/app/auth/me` 获取用户信息 |
| 2 | `plan/list.vue` | `pages/worker/plan/` | 计划列表页。scroll-view 状态筛选栏（全部/待审核/待执行/进行中/已完成/已取消）、卡片式计划列表（项目名称+状态标签+施工点+作业类型+计划时间）、悬浮 FAB 新建按钮（仅施工方可建）。onShow 调 `/app/homework/plan/list`，support 下拉刷新 |
| 3 | `plan/detail.vue` | `pages/worker/plan/` | 计划详情页。彩色状态头（5种状态不同背景色）、基本信息区（项目名称/市县/施工点/GPS/作业类型/计划时间/施工单位/作业内容/备注）、参与人员标签区、审核信息区（审核状态/申请人/审核人/审核时间/审核意见）。onLoad 调 `/app/homework/plan/{planId}` |
| 4 | `plan/create.vue` | `pages/worker/plan/` | 新建计划表单。uni-forms 表单组件（项目名称必填、市县、施工点、作业类型 picker、计划时间 multiSelector 日期选择器、施工单位、作业内容 textarea、备注 textarea）、底部固定提交按钮。submit 调 `POST /app/homework/plan`，权限不足时 toast 提示错误消息 |
| 5 | `review/list.vue` | `pages/worker/review/` | 审核列表页。状态筛选栏（全部/待审核/已通过/已驳回）、审核卡片（项目名称+审核状态标签+申请人+作业类型+申请时间）、待审核卡片显示快捷"通过""驳回"按钮（uni.showModal 确认弹窗，驳回时可输入原因）。onShow 调 `/app/homework/review/list`，非批准人返回空列表 |
| 6 | `review/detail.vue` | `pages/worker/review/` | 审核详情页。彩色审核状态头、计划信息区、审核信息区（申请人/申请时间/审核人/审核时间/审核意见）、参与人员标签区、审核操作区（仅待审核显示：textarea 审核意见 + "审核通过""审核驳回"双按钮，均经 uni.showModal 二次确认）。onLoad 调 `/app/homework/review/{reviewId}` |

**页面编码模式**（统一遵循）：
- 所有页面使用 `config.js` 的 `baseUrl` + `uni.request` 发起请求
- 手动构建 `authHeader()`：`{ 'Authorization': 'Bearer ' + uni.getStorageSync('appToken') }`
- 时间格式化 `formatTime(t)`：统一的 `yyyy-MM-dd HH:mm` 格式
- 样式使用 rpx 单位，卡片白底圆角 + 阴影，状态标签彩色背景白色文字

### 3.2 修改文件（2 个）

| # | 文件 | 改动内容 |
|---|------|---------|
| 1 | `pages.json` | **① Tab 变更**：第二个 Tab 的 `text` 从"记录"改为"工作台"，`pagePath` 从 `pages/worker/records` 改为 `pages/worker/workbench`；**② 新增路由**：`pages/worker/workbench`、`pages/worker/plan/list`、`pages/worker/plan/detail`、`pages/worker/plan/create`、`pages/worker/review/list`、`pages/worker/review/detail`（共6个）；**③ 保留** `pages/worker/records` 路由（打卡记录页，从工作台宫格跳转进入） |
| 2 | `permission.js` | 白名单 `whiteList` 新增 6 个路由：`/pages/worker/workbench`、`/pages/worker/plan/list`、`/pages/worker/plan/detail`、`/pages/worker/plan/create`、`/pages/worker/review/list`、`/pages/worker/review/detail` |

---

## 四、各功能实现要点

### 4.1 工作台（Tab2 改造）

- **原状**：Tab2 为"记录"，指向 `pages/worker/records`，仅展示打卡记录列表
- **改造后**：Tab2 改为"工作台"，指向 `pages/worker/workbench`，作为作业管理功能入口
- **用户信息栏**：显示当前登录工人的姓名、角色（多个角色用"、"分隔）、审核状态标签
- **宫格导航**：3 个入口——作业计划、作业审核、打卡记录，使用 `uni-icons` 彩色圆角方块
- **快捷操作**：根据角色动态显示"新建作业计划"和"待审核计划"入口卡片
- **角色检测**：调 `/app/auth/me` 获取 unitType 和 roleIds，computed 计算 `canCreatePlan` 和 `isApprover`
- **原打卡记录页保留**：从工作台宫格"打卡记录"跳转进入，路径 `pages/worker/records`

### 4.2 作业计划（移动端）

- **列表页**：scroll-view 横向滚动状态筛选标签（6种状态+全部），卡片式布局展示计划摘要（项目名称、状态标签、施工点、作业类型、计划时间），点击进入详情，FAB 悬浮按钮新建计划
- **详情页**：彩色状态头横幅（5种状态对应5种颜色：橙/蓝/绿/灰/红），分区展示基本信息、参与人员（蓝色标签列表）、审核信息（审核状态+审核人+审核时间+审核意见）
- **新建页**：uni-forms 表单，项目名称/作业类型必填，作业类型使用 picker 选择器（8种），计划时间使用 multiSelector 多列日期时间选择器，底部固定提交栏

### 4.3 作业审核（移动端）

- **列表页**：状态筛选（全部/待审核/已通过/已驳回），审核卡片展示项目名称（审核状态标签）、申请人、作业类型、申请时间。待审核的卡片在底部直接显示"通过""驳回"快捷按钮
- **快捷操作**：通过 → uni.showModal 确认弹窗 → 调 PUT approve；驳回 → uni.showModal 可编辑弹窗（可输入驳回原因）→ 调 PUT reject
- **详情页**：彩色审核状态头横幅（橙/绿/红），分区展示计划信息、审核信息、参与人员标签。仅待审核状态显示审核操作区（textarea 审核意见 + 通过/驳回双按钮）
- **二次确认**：通过和驳回均有 uni.showModal 确认，驳回支持输入原因

### 4.4 移动端权限控制

| 操作 | 权限要求 | 无权限时的表现 |
|------|---------|--------------|
| 查看计划列表 | 已登录 | 未登录跳转登录页 |
| 查看计划详情 | 已登录 | 未登录跳转登录页 |
| 创建计划 | 施工方（unit_type='3'）且非普通施工人员 | toast 提示"普通施工人员无权创建" |
| 查看审核列表 | 作业批准人（role_code='approver'） | 返回空列表 |
| 审核通过/驳回 | 作业批准人（role_code='approver'） | toast 提示"无审核权限" |

### 4.5 计划列表 SQL（关键查询）

`selectHwPlanListByWorkerId` 同时查工人**创建的计划**和**参与的计划**：

```sql
SELECT DISTINCT p.* FROM hw_plan p
LEFT JOIN hw_plan_worker pw ON p.plan_id = pw.plan_id
LEFT JOIN tb_worker w ON w.id = #{workerId}
WHERE (pw.worker_id = #{workerId} OR p.create_by = w.worker_name)
ORDER BY p.create_time DESC
```

---

## 五、模块架构

```
RuoYi-App uni-app 移动端
│
├── pages/worker/
│   ├── workbench.vue          ← 新增：工作台首页（Tab2）
│   ├── plan/
│   │   ├── list.vue           ← 新增：计划列表（卡片+状态筛选）
│   │   ├── detail.vue         ← 新增：计划详情
│   │   └── create.vue         ← 新增：新建计划表单
│   ├── review/
│   │   ├── list.vue           ← 新增：审核列表（卡片+快捷操作）
│   │   └── detail.vue         ← 新增：审核详情+通过/驳回
│   ├── records.vue            [保留] 打卡记录页
│   ├── checkin.vue            [不变] 打卡页（Tab1）
│   └── mine.vue               [不变] 我的（Tab3）
│
├── pages.json                 ← 修改：Tab+"工作台" + 5个新页面路由
└── permission.js              ← 修改：白名单新增6个路由

后端（Spring Boot 3.5.x）
│
├── ruoyi-admin/.../controller/app/
│   └── AppHomeworkController.java  ← 新增：/app/homework/* 8个接口
│
├── ruoyi-system/.../mapper/
│   └── HwPlanMapper.java          ← 修改：新增 selectHwPlanListByWorkerId
│   └── HwPlanMapper.xml           ← 修改：新增 SQL（关联 plan_worker + worker）
│
└── [复用已有] HwPlan / HwReview / HwPlanWorker / TbWorker / TbWorkerRoleRel

数据流：
uni-app (HMAC AppToken)
    │
    ▼
AppTokenFilter → /app/homework/*
    │
    ▼
AppHomeworkController
    │
    ├── IHwPlanService → HwPlanMapper → hw_plan / hw_plan_worker
    ├── IHwReviewService → HwReviewMapper → hw_review
    ├── TbWorkerMapper → tb_worker
    └── TbWorkerRoleRelMapper → tb_worker_role_rel
```

---

## 六、编译验证

```
mvn compile -DskipTests → BUILD SUCCESS
（7 个模块全部编译通过，无错误）
```

---

## 七、部署步骤

1. 后端无需重启（新增 Controller 可热加载，或重新启动 RuoYiApplication）
2. 在 HBuilderX 中打开 `RuoYi-App` 项目
3. 配置 `config.js` 中 `baseUrl` 指向后端地址（默认 `http://localhost:8080`）
4. HBuilderX 运行到浏览器（H5 模式）或真机调试
5. 使用施工方人员手机号登录 → 确认底部"工作台" Tab → 验证各功能：
   - 工作台宫格显示"作业计划""作业审核""打卡记录"
   - 施工方可新建计划，普通施工人员被拦截提示
   - 作业批准人可查看审核列表，进行通过/驳回操作
   - 打卡记录入口正常跳转原打卡记录页

---

## 八、变更统计

| 类别 | 数量 |
|------|:---:|
| 新增后端文件 | 1（AppHomeworkController） |
| 修改后端文件 | 2（HwPlanMapper.java + .xml） |
| 新增前端页面 | 6（workbench + plan×3 + review×2） |
| 修改前端文件 | 2（pages.json + permission.js） |
| 新增数据库表 | 0（复用已有） |
| 新增菜单项 | 0（移动端自有路由，不依赖 sys_menu） |
| 新增 API 端点 | 11（8 基础 + 3 CRUD） |
| 新增代码行数 | 约 1450 行（后端 350 + 前端 1100） |

---

## 九、Bug 修复（2026-06-04 第二轮）

### Bug 1：HBuilderX 编译后工作台仍显示旧页面（5张旧卡片）

**现象**：HBuilderX 编译 uni-app 后，点击"工作台" Tab 仍显示原来的"打卡记录、人员状态、班前喊话、统一签退、消息通知"5 张卡片，看不到新增的作业管理功能。

**根因分析**：

RuoYi-App 中存在**两套独立的移动端 UI 实现**：

| 实现 | 文件 | 用途 |
|------|------|------|
| uni-app Vue 源文件 | `pages/worker/*.vue` + `pages.json` | 需 HBuilderX 编译，但产出依赖 `static/index.html` 作为 HTML 模板 |
| 静态 H5 SPA | `static/index.html` + `mobile.html` | HBuilderX H5 dev server 将此文件作为入口模板加载，其中的硬编码页面直接覆盖 Vue 编译结果 |

HBuilderX 的 H5 dev server 使用 `static/index.html` 作为入口 HTML 模板。该文件内含一个完整的手写 SPA（包含所有页面的 HTML + CSS + JS），浏览器加载后直接渲染其中的静态内容，**不会执行 uni-app 的 Vue 页面编译结果**。因此只修改 `pages/worker/*.vue` 源文件无法改变用户看到的界面。

**修复内容**：

直接修改 `RuoYi-App/static/index.html`（`mobile.html` 同步），将作业管理功能集成到静态 SPA 中：

| 区域 | 改动 |
|------|------|
| CSS | 新增筛选栏、计划卡片、状态标签、审核标签、详情区、FAB 按钮等样式（~50行） |
| 工作台页面 | 旧 5 张卡片 → 两排 3 宫格：第一排「作业计划/作业审核/打卡记录」，第二排「人员状态/班前喊话/消息通知」+ 快捷操作区（按角色显示/隐藏） |
| 新增 5 个页面 div | `planListPage`（计划列表+状态筛选+卡片+FAB）、`planDetailPage`（详情+人员标签+审核信息）、`planCreatePage`（表单）、`reviewListPage`（审核列表+快捷通过/驳回）、`reviewDetailPage`（详情+审核操作） |
| showTab() | 新增 `planList`/`planDetail`/`planCreate`/`reviewList`/`reviewDetail` 页面切换分支，`work` 页面新增 `loadWorkbench()` 调用 |
| 新增 JS 函数（~200行） | `loadWorkbench`（加载用户信息+角色权限+显示快捷操作）、`loadPlanList`/`showPlanDetail`/`doCreatePlan`（计划 CRUD）、`loadReviewList`/`showReviewDetail`/`doQuickReview`/`doReview`（审核操作） |

**同步文件**：`static/index.html` → 覆盖 `mobile.html`（两份文件保持一致）。

### Bug 2：uni-app Vue 源文件中角色权限判断使用错误的字段类型

**现象**：`workbench.vue` 和 `plan/list.vue` 中的 `canCreatePlan` / `isApprover` 判断永远不生效。

**根因**：代码使用 `d.roleIds`（后端返回的数字数组，如 `[1,2,3]`）与字符串 `'approver'` 和 `'worker'` 做比较，类型不匹配导致判断始终为 false。

**修复**：

| 文件 | 行 | 改动 |
|------|------|------|
| `AppAuthController.java` | 72-77 | `/app/auth/me` 接口新增返回 `roleCodes` 字段（字符串数组，如 `['applicant','approver','worker']`） |
| `workbench.vue` | 106 | `d.roleIds` → `d.roleCodes` |
| `plan/list.vue` | 86 | `d.roleIds` → `d.roleCodes` |

### Bug 3：静态 HTML 中角色权限判断同样使用了错误的字段

**现象**：`static/index.html` 的 `loadWorkbench()` 中 `isApprover` 判断失效。

**根因**：使用 `workerInfo.roleIds`（数字数组）检查 `includes('approver')`。

**修复**：改为使用 `workerInfo.roleCodes`（字符串数组），依赖 Bug 2 修复后的 `/app/auth/me` 接口返回的 `roleCodes` 字段。

### Bug 4：移动端作业计划缺少编辑/删除/状态变更功能

**现象**：点击进入作业计划详情页后，只能查看计划信息，没有像网页端一样的编辑、删除和状态流转操作按钮。

**根因**：初始版本的 `AppHomeworkController` 只提供了 `GET /plan/list`、`GET /plan/{planId}`、`POST /plan` 三个核心端点，缺少修改、删除和状态变更接口。前端 `static/index.html` 也因此未添加对应的 UI 操作按钮。

**修复内容**：

**后端（AppHomeworkController）新增 3 个端点**：

| 端点 | 方法 | 说明 | 权限 |
|------|------|------|------|
| `/app/homework/plan` | PUT | 修改计划（仅创建者可改，已完成/已取消不可改） | 创建者 |
| `/app/homework/plan/{planId}` | DELETE | 删除计划（仅创建者，进行中不可删） | 创建者 |
| `/app/homework/plan/changeStatus` | PUT | 变更计划状态（调用已有 changeStatus 5 状态流转逻辑） | 创建者 |

新增 `isPlanOwner(workerId, plan)` 辅助方法：通过 `TbWorker.workerName` 与 `HwPlan.createBy` 比较判断创建者身份。

**前端（static/index.html）修改**：

| 改动 | 说明 |
|------|------|
| `planCreatePage` 改写 | 表单页支持**新建/编辑双模式**：全局变量 `planEditId` 标记编辑模式，标题和按钮文字动态切换，提交时根据模式调 POST 或 PUT |
| `resetPlanForm()` | 新增：清空所有表单字段，重置编辑标记，恢复标题和按钮文字 |
| `editPlan(planId)` | 新增：调 API 加载计划数据 → 预填表单 → 切换到编辑模式 → 跳转表单页 |
| `doSubmitPlan()` | 新增（替代原 `doCreatePlan`）：统一处理新建和编辑提交 |
| `deletePlan(planId, pname)` | 新增：二次确认后调 DELETE 接口删除 |
| `changePlanStatus(planId, status, label)` | 新增：二次确认后调 PUT changeStatus 接口 |
| 计划详情页底部操作区 | 新增"操作"section，根据当前状态动态渲染按钮：**编辑**（非3/4）、**开始作业**（状态1）、**标记完成**（状态2）、**取消计划**（0/1/2）、**恢复为待审核**（4）、**删除**（非2） |

**状态-按钮对应关系**：

| 当前状态 | 可用操作 |
|---------|---------|
| 0 待审核 | 编辑、取消计划、删除 |
| 1 待执行 | 编辑、开始作业、取消计划、删除 |
| 2 进行中 | 编辑、标记完成、取消计划 |
| 3 已完成 | —（不可操作） |
| 4 已取消 | 恢复为待审核、删除 |

**编译验证**：`mvn compile -DskipTests` → BUILD SUCCESS

### Bug 5：移动端计划列表条数与网页端不一致 + FAB 新建按钮不显示

**现象**：网页端显示 8 条计划，移动端只显示 2 条；且计划列表页右下角没有新建计划的 FAB 浮动按钮。

**根因分析**：

| 问题 | 根因 |
|------|------|
| 计划条数不匹配 | `AppHomeworkController.planList()` 调用了 `selectHwPlanListByWorkerId`，只查当前工人创建或参与的计划。网页端 `HwPlanController.list()` 调用 `selectHwPlanList`，返回全部计划 |
| FAB 按钮不显示 | `loadWorkbench()` 中根据角色权限设置 FAB 的 `display`，但该函数仅在进入工作台 Tab 时调用。直接从工作台宫格进入 `planList` 页面时不会触发 `loadWorkbench()`，FAB 保持初始的 `display:none` |

**修复**：

| 文件 | 改动 |
|------|------|
| `AppHomeworkController.java` | `planList()` 改为调用 `selectHwPlanList(query)`（与网页端一致），通过 `HwPlan` 对象传递 `status` 筛选条件 |
| `static/index.html` → `loadPlanList()` | 函数开头新增权限检查逻辑：若 `workerInfo` 为空则先调 `loadWorkbench()` 加载；否则直接根据 `roleCodes` 和 `unitType` 判断 `canCreate`，动态设置 FAB 的 `display` |

**编译验证**：`mvn compile -DskipTests` → BUILD SUCCESS

**补充修复（2026-06-04 第三轮）**：FAB 按钮方案在 HBuilderX dev server 中未能正常显示。改为将"新增"按钮直接渲染到筛选栏最右侧，作为筛选标签的一部分。

| 文件 | 改动 |
|------|------|
| `static/index.html` → `loadPlanList()` | 移除 FAB 按钮的 `display` 动态控制逻辑；在筛选栏 `innerHTML` 末尾追加蓝色"+ 新增"标签（`style="background:#007aff;color:#fff;font-weight:bold"`，内联 `onclick="resetPlanForm();showPage('planCreate')"`），始终显示 |
| `static/index.html` → 页面模板 | 移除计划列表页右下角的 `<div class="fab-btn">` FAB 浮动按钮元素 |
| `static/index.html` → `loadWorkbench()` | 移除 `document.getElementById('planFab').style.display` 相关代码 |

筛选栏最终布局：`[全部] [待审核] [待执行] [进行中] [已完成] [已取消] [+ 新增]`

### Bug 6：移动端新增计划表单缺少字段

**现象**：移动端新建/编辑计划时，表单缺少网页端已有的"纬度(GPS)"、"经度(GPS)"和"参与人员"字段。

**根因**：初始版本的 `planCreatePage` 表单仅列出了基本字段，遗漏了 GPS 坐标和参与人员输入。

**修复**：

| 文件 | 改动 |
|------|------|
| `static/index.html` → planCreatePage | 在施工点后新增 `planLat`（`type="number" step="0.0000001"`）和 `planLng` 两个 `<input>`，新增参与人员 `<textarea id="planWorkers">` |
| `resetPlanForm()` | 新增清空 `planLat`、`planLng`、`planWorkers` |
| `editPlan()` | 新增预填 `planLat`（`p.siteLatitude`）、`planLng`（`p.siteLongitude`）、`planWorkers`（`p.workers`） |
| `doSubmitPlan()` | 请求体新增 `siteLatitude`、`siteLongitude`、`workers` 字段 |

### Bug 7：参与人员改为二级角色选择器

**现象**：参与人员使用纯文本 textarea 输入，与网页端的二级角色联动选择器体验差距大。

**修复**：

**后端**：

| 文件 | 改动 |
|------|------|
| `AppHomeworkController.java` | 新增 `GET /app/homework/rolesWithWorkers` 端点，调用 `TbWorkerRoleRelMapper.selectWorkersByRole()` 返回按角色分组的人员数据（`roleCode, roleName, workerId, workerName`） |

**前端（static/index.html）**：

| 改动 | 说明 |
|------|------|
| 表单 HTML | textarea 替换为点击触发区 `.worker-pick`（显示"已选 N 人"或"点击选择参与人员"）+ 已选人员标签 `.worker-tags` |
| 弹窗 HTML | 新增 `#workerPickerOverlay`：底部弹出 70vh 面板，左栏角色列表 `.picker-left` + 右栏人员勾选 `.picker-right`，顶部"取消/选择参与人员/确定"三栏 |
| CSS | 新增 `.picker-overlay`（全屏遮罩）、`.picker-box`（底部弹窗）、`.picker-left`/`.picker-right`（左右分栏）、`.w-check`（圆形复选框）等样式 |
| JS 数据 | 全局变量 `allRoleWorkers`（API 返回原始数据）、`roleGroups`（按 roleCode 分组）、`activeRoleIdx`（当前选中角色索引）、`checkedWorkerIds`（Set，已选工人 ID 集合） |
| JS 函数（9个） | `openWorkerPicker`（首次打开时调 API 加载数据+分组）、`closeWorkerPicker`、`renderRoleList`/`selectRole`（角色列表渲染+切换）、`renderWorkerList`/`toggleWorker`（人员列表渲染+勾选切换）、`confirmWorkerSelection`/`updateWorkerDisplay`（确认选择+刷新展示区）、`getSelectedWorkerNames`（导出逗号分隔名称）、`prefillWorkerSelection`（编辑时预填，按名称反查 ID 并勾选） |
| `doSubmitPlan()` | `workers` 字段改为调 `getSelectedWorkerNames()` 获取选中人员 |
| `resetPlanForm()` | 改为 `checkedWorkerIds.clear(); updateWorkerDisplay()` |
| `editPlan()` | 改为 `prefillWorkerSelection(p.workers)` |

**交互流程**：
```
点击「点击选择参与人员」
        ┌──────────────────────────────┐
        │  取消     选择参与人员    确定  │
        ├────────┬─────────────────────┤
        │ 作业申请人 │ ☑ 张三             │
        │▶作业批准人│ ☐ 李四             │
        │ 作业监护人 │                    │
        │ ...     │  点击勾选/取消勾选   │
        └────────┴─────────────────────┘
选中后显示：已选 3 人  →  [张三] [李四] [王五]
```

**编译验证**：`mvn compile -DskipTests` → BUILD SUCCESS

### Bug 8：网页端新增计划时 hw_plan_worker 写入报 Data truncation

**现象**：在网页端新增作业计划时，保存参与人员时报错：
```
Data truncation: Data too long for column 'role_type' at row 1
```

**根因**：`hw_plan_worker.role_type` 列定义为 `CHAR(1)`，只能存储单字符（原设计存数字 `'1'`-`'9'`）。V3 二级人员选择器改为传入 `role_code`（如 `'applicant'`、`'approver'`、`'guardian'` 等多字符字符串），远超列宽限制。

**修复**：

```sql
ALTER TABLE hw_plan_worker MODIFY COLUMN role_type VARCHAR(20) DEFAULT '' COMMENT '角色编码（冗余）';
```

迁移脚本：`sql/updates/update_20260604_role_type_fix.sql`

---

> 开发日期：2026-06-04
> 基于规格书：HWAPP.md
> 业务流程：工作台 → 作业计划（创建/查看）→ 作业审核（通过/驳回）→ 作业打卡
