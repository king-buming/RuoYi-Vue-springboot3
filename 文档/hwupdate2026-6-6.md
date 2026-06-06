# 作业管理模块 — 去掉审核流程改造规格书

> **已完成。** 以下为完整的改造记录。

---

## 一、项目信息

| 项目 | RuoYi-Vue SpringBoot3 v3.9.2 |
|------|------|
| 后端路径 | `C:\Users\ASUS\Desktop\RuoYi-Vue-springboot3-main\RuoYi-Vue-springboot3` |
| uni-app 路径 | `C:\Users\ASUS\Desktop\RuoYi-Vue-springboot3-main\RuoYi-App` |
| PC 前端路径 | `C:\Users\ASUS\Desktop\RuoYi-Vue-springboot3-main\RuoYi-Vue-springboot3\ruoyi-ui` |
| 后端 | Spring Boot 3.5.x + MyBatis + Spring Security 6 + JWT + Druid |
| PC 前端 | Vue 2.6.12 + Element UI |
| uni-app | Vue 2.6.14 + uni-ui |
| 数据库 | MySQL 8.x，库名 `ry-vue` |

---

## 二、需求概述

### 2.1 改造目标

彻底移除作业审核模块。作业计划创建后直接进入"待执行"状态，无需人工审核。

**目标结构**：

```
作业管理
├── 作业计划
└── 作业打卡
```

（原来有"作业审核"，已删除）

### 2.2 状态流转变化

| 阶段 | 旧流程 | 新流程 |
|------|--------|--------|
| 创建 | status='0'（待审核），自动创建 hw_review | status='1'（待执行），无审核记录 |
| 审核 | 批准人审核通过/驳回 | **跳过** |
| 打卡 | 审核通过后方可打卡 | 创建后直接可打卡 |
| 取消恢复 | 恢复为 status='0'（待审核） | 恢复为 status='1'（待执行） |

---

## 三、改动清单

### 3.1 修改的文件（12 个）

| # | 文件 | 类型 | 说明 |
|---|------|------|------|
| 1 | `ruoyi-system/.../domain/HwPlan.java` | 修改 | status 注释：去掉"0待审核" |
| 2 | `ruoyi-system/.../service/impl/HwPlanServiceImpl.java` | 修改 | 去掉 HwReview 依赖；insert 改 status='1'；changeStatus 去掉审核相关 case |
| 3 | `ruoyi-system/.../service/impl/HwAttendanceServiceImpl.java` | 修改 | checkIn/checkOut 去掉 status='0' 拦截 |
| 4 | `ruoyi-admin/.../controller/app/AppHomeworkController.java` | 修改 | 删除 4 个审核端点；去掉 review 注入和查询 |
| 5 | `ruoyi-ui/src/views/homework/plan/index.vue` | 修改 | 状态选项去"待审核"；恢复按钮改"恢复为待执行" |
| 6 | `RuoYi-App/pages/worker/workbench.vue` | 修改 | 删除"作业审核"宫格 + "待审核计划"快捷入口 + isApprover |
| 7 | `RuoYi-App/pages.json` | 修改 | 删除 review/list、review/detail 路由 |
| 8 | `RuoYi-App/permission.js` | 修改 | 删除 review 路由白名单 |
| 9 | `RuoYi-App/pages/worker/plan/list.vue` | 修改 | statusOptions + statusLabels 去掉"待审核" |
| 10 | `RuoYi-App/pages/worker/plan/detail.vue` | 修改 | 删除审核信息 section + review data + getReviewStatusLabel |
| 11 | `RuoYi-App/mobile.html` | 修改 | 删除宫格入口 + 快捷操作 + 审核页面 + 4 个 JS 函数 + plan 审核信息 + 恢复按钮 |
| 12 | `RuoYi-App/static/index.html` | 修改 | 同上（与 mobile.html 同步） |

### 3.2 删除的文件（10 个）

| # | 文件 | 类型 |
|---|------|------|
| 1 | `ruoyi-system/.../domain/HwReview.java` | 实体 |
| 2 | `ruoyi-system/.../mapper/HwReviewMapper.java` | Mapper 接口 |
| 3 | `ruoyi-system/.../resources/mapper/system/HwReviewMapper.xml` | Mapper XML |
| 4 | `ruoyi-system/.../service/IHwReviewService.java` | Service 接口 |
| 5 | `ruoyi-system/.../service/impl/HwReviewServiceImpl.java` | Service 实现 |
| 6 | `ruoyi-admin/.../controller/homework/HwReviewController.java` | Controller |
| 7 | `ruoyi-ui/src/views/homework/review/index.vue` | PC 端审核页面 |
| 8 | `ruoyi-ui/src/api/homework/review.js` | PC 端审核 API |
| 9 | `RuoYi-App/pages/worker/review/list.vue` | uni-app 审核列表 |
| 10 | `RuoYi-App/pages/worker/review/detail.vue` | uni-app 审核详情 |

### 3.3 数据库变更

**文件**：`sql/updates/20260606_remove_review.sql`（幂等）

```sql
-- 1. 将现有待审核计划改为待执行
UPDATE hw_plan SET status = '1', update_by = 'SYSTEM', update_time = SYSDATE()
WHERE status = '0';

-- 2. 修改 status 字段注释和默认值
ALTER TABLE hw_plan MODIFY COLUMN status CHAR(1) DEFAULT '1'
  COMMENT '状态（1待执行 2进行中 3已完成 4已取消）';

-- 3. 删除审核菜单及按钮权限
DELETE FROM sys_menu WHERE perms IN (
    'homework:review:list',
    'homework:review:query',
    'homework:review:approve',
    'homework:review:reject'
);

-- 4. 删除审核表
DROP TABLE IF EXISTS hw_review;
```

### 3.4 uni-app 编译产物 H5 清理

`mobile.html` 和 `static/index.html` 是 HBuilderX 编译生成的单文件 H5，需同步清理：

| 清理项 | 数量 |
|--------|------|
| 删除"作业审核"宫格入口 | 1 个 gcard |
| 删除"待审核计划"快捷操作卡片 | 1 个 quickReview div |
| 删除审核列表页 HTML | `reviewListPage` div |
| 删除审核详情页 HTML | `reviewDetailPage` div |
| 删除审核 JS 函数 | `loadReviewList`、`showReviewDetail`、`doQuickReview`、`doReview`（4 个函数） |
| 删除 `rvLabels` 变量 | 1 个 |
| 删除 `isApprover` + `quickReview` JS | 2 行 |
| `statusLabels` 去掉 `0:'待审核'` | 1 处 |
| plan 筛选去掉 `'0','待审核'` | 1 处 |
| plan 详情去审核信息展示 | ~10 行 |
| 恢复按钮改为 `'1','恢复为待执行'` | 1 处 |
| 宫格布局调整 | 将"人员状态"从第二行移到第一行，使 5 个入口呈 3+2 对称布局 |

---

## 四、后端核心改动细节

### 4.1 HwPlanServiceImpl.insertHwPlan()

```java
// 修改前
plan.setStatus("0");
int rows = hwPlanMapper.insertHwPlan(plan);
HwReview review = new HwReview(); ... hwReviewMapper.insertHwReview(review);
return rows;

// 修改后
plan.setStatus("1");
return hwPlanMapper.insertHwPlan(plan);
```

### 4.2 HwPlanServiceImpl.changeStatus()

- 删除 `case "0"`（恢复为待审核）
- `case "1"` 条件改为 `!"4".equals(current)`（只有已取消可恢复为待执行）
- 已取消检查改为 `!"1".equals(status)`（只能恢复为待执行）

### 4.3 HwAttendanceServiceImpl

checkIn / checkOut 各删除：

```java
if ("0".equals(plan.getStatus()))
    throw new ServiceException("该计划尚未通过审核，无法打卡");
```

### 4.4 AppHomeworkController

删除的端点：

| 原 URL | 方法 |
|--------|------|
| `GET /app/homework/review/list` | 审核列表 |
| `GET /app/homework/review/{id}` | 审核详情 |
| `PUT /app/homework/review/approve` | 审核通过 |
| `PUT /app/homework/review/reject` | 审核驳回 |

planDetail 去掉 `review` 字段返回。

---

## 五、状态流转（最终）

```
insertHwPlan → status='1' (待执行)
  → 首次打卡 → status='2' (进行中)
  → 标记完成 → status='3' (已完成)
  → 任意步骤可取消 → status='4' (已取消)
  → 已取消可恢复 → status='1' (待执行)
```

---

## 六、验证清单

| # | 验证项 | 状态 |
|---|--------|------|
| 1 | 后端编译 | `mvn compile -DskipTests` → `BUILD SUCCESS`（ruoyi-admin: 46→45 源文件） ✅ |
| 2 | PC 端创建计划（待执行） | 待启动验证 |
| 3 | 无审核记录生成 | 待启动验证 |
| 4 | 打卡不拦截"待审核" | 待启动验证 |
| 5 | 状态流转正确 | 待启动验证 |
| 6 | uni-app 工作台无审核入口 | 待启动验证 |
| 7 | uni-app H5 无审核残留 | `reviewList`/`reviewDetail`/`doReview` 等关键字全文 0 匹配 ✅ |

---

## 七、最终模块结构

```
作业管理 (homework)
├── 作业计划 (plan)       → /homework/plan/**
└── 作业打卡 (attendance)  → /homework/attendance/**
```

> 规格书编写日期：2026-06-06
> 基于：`secondHomework.md`、`firstHomeworkV2.md`、`HWAPP.md`、`app作业管理开发日志.md`
