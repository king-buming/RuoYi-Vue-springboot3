# 作业管理去掉审核流程开发日志

## 基本信息
- 开发日期：2026-06-06
- 模块名称：作业管理（homework）— 去掉审核流程
- 后端框架：Spring Boot 3.5.x + MyBatis + Spring Security 6 + JWT
- 前端框架：Vue 2.6.12 + Element UI（PC 端）/ uni-app（移动端）
- 数据库：MySQL 8.x，库名 `ry-vue`
- 开发者：AI 自动生成

---

## 一、需求背景

当前作业计划创建后自动进入"待审核(0)"状态，需作业批准人审核通过后方可进行打卡。现因业务流程调整，**去掉作业审核环节**，计划创建后直接进入"待执行(1)"状态。

### 状态流转对比

```
旧流程：
  创建 → 待审核(0) → 审核通过 → 待执行(1) → 进行中(2) → 已完成(3)
              ↘ 审核驳回 → 已取消(4)

新流程：
  创建 → 待执行(1) → 进行中(2) → 已完成(3)
              ↘ 取消 → 已取消(4) → 恢复 → 待执行(1)
```

---

## 二、数据库变更

**无新增表**。保留 `hw_review` 表用于历史数据查询。

```sql
-- 可选：更新 hw_plan.status 字段注释
ALTER TABLE hw_plan MODIFY COLUMN status CHAR(1) DEFAULT '1' COMMENT '状态（1待执行 2进行中 3已完成 4已取消）';
```

---

## 三、后端文件修改详述（4 个文件）

### 3.1 HwPlan.java — 更新 status 字段注释

**文件**：`ruoyi-system/src/main/java/com/ruoyi/system/domain/HwPlan.java`

| 项目 | 修改前 | 修改后 |
|------|--------|--------|
| status 注释 | `状态（0待审核 1待执行 2进行中 3已完成 4已取消）` | `状态（1待执行 2进行中 3已完成 4已取消）` |

### 3.2 HwPlanServiceImpl.java — 核心改动

**文件**：`ruoyi-system/src/main/java/com/ruoyi/system/service/impl/HwPlanServiceImpl.java`

#### 改动 1：移除 HwReview 依赖

- 删除 `@Autowired private HwReviewMapper hwReviewMapper;`
- 删除 `import com.ruoyi.system.domain.HwReview;`
- 删除 `import com.ruoyi.system.mapper.HwReviewMapper;`
- 删除 `import java.util.Date;`

#### 改动 2：insertHwPlan 不再创建审核记录

```java
// 修改前：status='0'，自动创建 HwReview
plan.setStatus("0");
int rows = hwPlanMapper.insertHwPlan(plan);
HwReview review = new HwReview(); ... hwReviewMapper.insertHwReview(review);
return rows;

// 修改后：status='1'，不创建审核记录
plan.setStatus("1");
return hwPlanMapper.insertHwPlan(plan);
```

#### 改动 3：changeStatus 状态流转简化

| 变更 | 说明 |
|------|------|
| 删除 `case "0"` | 不再支持恢复为"待审核" |
| `case "1"` 条件改为 `!"4".equals(current)` | 只有已取消(4)可恢复为待执行(1) |
| `case "4"` 取消条件去掉 `'0'` | status='0' 不再存在 |
| 已取消检查改为 `!"1".equals(status)` | 已取消只能恢复为待执行 |

### 3.3 HwAttendanceServiceImpl.java — 去掉待审核拦截

**文件**：`ruoyi-system/src/main/java/com/ruoyi/system/service/impl/HwAttendanceServiceImpl.java`

在 `checkIn()` 和 `checkOut()` 各删除以下拦截逻辑：

```java
// 删除
if ("0".equals(plan.getStatus()))
    throw new ServiceException("该计划尚未通过审核，无法打卡");
```

status='3'（已完成）和 status='4'（已取消）的拦截保留。

### 3.4 AppHomeworkController.java — 删除审核端点

**文件**：`ruoyi-admin/src/main/java/com/ruoyi/web/controller/app/AppHomeworkController.java`

#### 删除的注入

- `@Autowired private IHwReviewService hwReviewService;`
- `@Autowired private HwReviewMapper hwReviewMapper;`

#### 删除的 import

- `HwReview`、`HwReviewMapper`、`IHwReviewService`

#### 删除的端点（共 6 个方法）

| 方法 | 原 URL | 说明 |
|------|--------|------|
| `reviewList()` | `GET /app/homework/review/list` | 审核列表 |
| `reviewDetail()` | `GET /app/homework/review/{reviewId}` | 审核详情 |
| `approveReview()` | `PUT /app/homework/review/approve` | 审核通过 |
| `rejectReview()` | `PUT /app/homework/review/reject` | 审核驳回 |

#### planDetail 去 review 查询

```java
// 删除
HwReview review = hwReviewMapper.selectHwReviewByPlanId(planId);
result.put("review", review);
```

---

## 四、PC 前端文件修改详述（1 个文件）

### 4.1 plan/index.vue — 状态选项去掉待审核

**文件**：`ruoyi-ui/src/views/homework/plan/index.vue`

| 改动项 | 修改前 | 修改后 |
|--------|--------|--------|
| statusOptions | 含 `{ label: '待审核', value: '0' }` | 删除此项 |
| 表格状态列 el-tag | `['待审核','待执行','进行中','已完成','已取消'][Number(status)]` | `['待执行','进行中','已完成','已取消'][Number(status)-1]` |
| 取消计划条件 | `['0','1','2'].includes(status)` | `['1','2'].includes(status)` |
| 恢复按钮 | `status='4' command="0"` → "恢复为待审核" | `status='4' command="1"` → "恢复为待执行" |
| handleStatusChange labels | 含 `'0': '待审核'` | 删除此项 |

---

## 五、uni-app 移动端文件修改详述（5 个文件）

### 5.1 workbench.vue — 删除审核入口

**文件**：`RuoYi-App/pages/worker/workbench.vue`

| 改动 | 说明 |
|------|------|
| menus 数组删除 `作业审核` | 宫格从 4 个减为 3 个：作业计划、打卡记录、消息通知 |
| 删除 `isApprover` computed | 不再需要判断审核权限 |
| 删除 `goReviewList()` method | 不再有审核列表入口 |
| 删除"待审核计划"快捷操作卡片 | 整个 v-if="isApprover" 的 action-card |

### 5.2 pages.json — 删除 review 路由

**文件**：`RuoYi-App/pages.json`

删除 2 个路由条目：
- `pages/worker/review/list`
- `pages/worker/review/detail`

### 5.3 permission.js — 删除 review 白名单

**文件**：`RuoYi-App/permission.js`

白名单中删除 `'/pages/worker/review/list', '/pages/worker/review/detail'`。

### 5.4 plan/list.vue — 状态筛选去掉待审核

**文件**：`RuoYi-App/pages/worker/plan/list.vue`

| 改动 | 修改前 | 修改后 |
|------|--------|--------|
| statusOptions | 含 `{ label: '待审核', value: '0' }` | 删除此项 |
| statusLabels | 含 `'0': '待审核'` | 删除此项 |

### 5.5 plan/detail.vue — 去掉审核信息区域

**文件**：`RuoYi-App/pages/worker/plan/detail.vue`

| 改动 | 说明 |
|------|------|
| 删除审核信息 section 模板 | 整个 `v-if="review"` 区块 |
| data 中删除 `review: null` | 不再存储审核数据 |
| data 中删除 `reviewStatusLabels` | 不再需要审核状态映射 |
| data 中 statusLabels 去掉 `'0': '待审核'` | 状态映射更新 |
| loadDetail 中去掉 `this.review = d.review` | 不再从接口获取审核数据 |
| 删除 `getReviewStatusLabel()` method | 不再需要 |

---

## 六、未改动的文件

| 文件 | 原因 |
|------|------|
| `ruoyi-system/.../domain/HwReview.java` | 历史审核记录实体保留 |
| `ruoyi-system/.../mapper/HwReviewMapper.java` | 历史审核记录查询保留 |
| `ruoyi-system/.../service/IHwReviewService.java` | 接口保留 |
| `ruoyi-system/.../service/impl/HwReviewServiceImpl.java` | 实现保留 |
| `ruoyi-admin/.../controller/homework/HwReviewController.java` | PC 端历史记录查看保留 |
| `ruoyi-admin/.../controller/homework/HwPlanController.java` | 接口不变，底层 Service 行为已改 |
| `IHwPlanService.java` | 接口签名不变 |
| 数据库 `hw_review` 表 | 历史审核记录保留 |
| `ruoyi-quartz/.../task/HwAttendanceTimeoutJob.java` | 不涉及审核逻辑 |
| `RuoYi-App/pages/worker/review/list.vue` | 保留文件，不影响编译 |
| `RuoYi-App/pages/worker/review/detail.vue` | 保留文件，不影响编译 |

---

## 七、模块架构

```
后端改动：
  HwPlanServiceImpl.insertHwPlan() → status='1'，不创建 HwReview
  HwPlanServiceImpl.changeStatus() → 去掉审核相关状态转换（0↔1、0↔4）
  HwAttendanceServiceImpl.checkIn/checkOut() → 去掉 status='0' 拦截
  AppHomeworkController → 去掉 6 个审核端点 + review 查询

PC 前端改动：
  plan/index.vue → 状态选项 5→4 个，恢复按钮改为"恢复为待执行"

uni-app 改动：
  workbench.vue → 宫格 4→3 个，删除审核快捷入口
  pages.json → 删除 2 个 review 路由
  permission.js → 删除 2 个 review 白名单
  plan/list.vue → 状态筛选 6→5 个
  plan/detail.vue → 去掉审核信息区块
```

---

## 八、验证清单

| # | 验证项 | 状态 |
|---|--------|------|
| 1 | 后端编译 | `mvn compile -DskipTests` → `BUILD SUCCESS` ✅ |
| 2 | PC 端创建计划 | 待启动验证：新建计划 → 状态为"待执行" |
| 3 | 无审核记录生成 | 待启动验证：创建计划后 `hw_review` 表无新记录 |
| 4 | PC 端打卡 | 待启动验证：创建后直接进场打卡，不提示"尚未通过审核" |
| 5 | PC 端状态流转 | 待启动验证：待执行→（打卡）→进行中→标记完成→已完成 |
| 6 | PC 端取消+恢复 | 待启动验证：取消→已取消→恢复→待执行 |
| 7 | uni-app 工作台 | 待启动验证：宫格"作业计划""打卡记录""消息通知"三项 |
| 8 | uni-app 无审核入口 | 待启动验证：工作台无"待审核计划"快捷操作 |
| 9 | uni-app 计划列表 | 待启动验证：状态筛选无"待审核" |
| 10 | uni-app 计划详情 | 待启动验证：无审核信息区域 |
| 11 | uni-app 编译 | 待 HBuilderX 验证无编译错误 |

---

## 九、修改文件清单

| # | 文件 | 类型 | 说明 |
|---|------|------|------|
| 1 | `ruoyi-system/.../domain/HwPlan.java` | 修改 | status 字段注释去掉"0待审核" |
| 2 | `ruoyi-system/.../service/impl/HwPlanServiceImpl.java` | 修改 | 去掉 HwReview 自动创建 + status 改为 "1" + changeStatus 简化 |
| 3 | `ruoyi-system/.../service/impl/HwAttendanceServiceImpl.java` | 修改 | checkIn/checkOut 去掉 status='0' 拦截 |
| 4 | `ruoyi-admin/.../controller/app/AppHomeworkController.java` | 修改 | 删除 6 个审核端点 + 去掉 review 注入和查询 |
| 5 | `ruoyi-ui/src/views/homework/plan/index.vue` | 修改 | 状态选项去掉"待审核"，恢复按钮改为"恢复为待执行" |
| 6 | `RuoYi-App/pages/worker/workbench.vue` | 修改 | 删除"作业审核"宫格 + "待审核计划"快捷入口 |
| 7 | `RuoYi-App/pages.json` | 修改 | 删除 review/list、review/detail 路由 |
| 8 | `RuoYi-App/permission.js` | 修改 | 删除 review 路由白名单 |
| 9 | `RuoYi-App/pages/worker/plan/list.vue` | 修改 | 状态筛选去掉"待审核" |
| 10 | `RuoYi-App/pages/worker/plan/detail.vue` | 修改 | 去掉审核信息展示区域 + 相关 data/methods |

---

## 十、审核模块彻底删除（2026-06-06 追加）

### 10.1 删除文件清单（10 个）

| # | 文件 | 类型 |
|---|------|------|
| 1 | `ruoyi-system/.../domain/HwReview.java` | 删除 — 审核实体 |
| 2 | `ruoyi-system/.../mapper/HwReviewMapper.java` | 删除 — 审核 Mapper 接口 |
| 3 | `ruoyi-system/.../resources/mapper/system/HwReviewMapper.xml` | 删除 — 审核 Mapper XML |
| 4 | `ruoyi-system/.../service/IHwReviewService.java` | 删除 — 审核 Service 接口 |
| 5 | `ruoyi-system/.../service/impl/HwReviewServiceImpl.java` | 删除 — 审核 Service 实现 |
| 6 | `ruoyi-admin/.../controller/homework/HwReviewController.java` | 删除 — 审核 Controller |
| 7 | `ruoyi-ui/src/views/homework/review/index.vue` | 删除 — PC 端审核页面 |
| 8 | `ruoyi-ui/src/api/homework/review.js` | 删除 — PC 端审核 API |
| 9 | `RuoYi-App/pages/worker/review/list.vue` | 删除 — uni-app 审核列表 |
| 10 | `RuoYi-App/pages/worker/review/detail.vue` | 删除 — uni-app 审核详情 |

### 10.2 数据库清理 SQL

```sql
-- 删除审核菜单及按钮权限
DELETE FROM sys_menu WHERE perms IN (
    'homework:review:list', 'homework:review:query',
    'homework:review:approve', 'homework:review:reject'
);

-- 删除审核表
DROP TABLE IF EXISTS hw_review;
```

**文件**：`sql/updates/20260606_remove_review.sql`

### 10.3 验证

编译通过：`mvn compile -DskipTests` → `BUILD SUCCESS`（ruoyi-admin: 46→45 源文件） ✅

### 10.4 最终模块架构

```
作业管理 (homework)  ← sys_menu 一级目录
├── 作业计划 (plan)       ← /homework/plan/**
└── 作业打卡 (attendance)  ← /homework/attendance/**
```

> 原"作业审核"模块全部移除：菜单数据、后端代码（6 个 Java/XML）、PC 前端（2 个）、uni-app（2 个）、数据库表（`hw_review` 已删）。

> 开发日期：2026-06-06
