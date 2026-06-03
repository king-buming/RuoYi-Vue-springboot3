# 项目对话语言

在与本项目的所有对话中，请使用中文进行回复和交流。

---

# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 行为准则

**1. 先思考再编码** — 陈述假设，如果不确定就提问。如果存在多种解释，呈现出来——不要默默选择。如果有更简单的方法，说出来。

**2. 简洁优先** — 用最少的代码解决问题，不做多余的抽象。不处理不可能发生的错误场景。如果 200 行的代码可以写成 50 行，重写它。

**3. 精准修改** — 只改动你必须改的。不要"改进"相邻代码、注释或格式。不要重构没有被破坏的东西。匹配已有代码风格。

**4. 目标驱动执行** — 把任务转化为可验证的目标，定义成功标准，循环直到验证通过。

---

## 项目概述

RuoYi（若依）v3.9.2 SpringBoot3 分支 —— 基于 Spring Boot 3.5.x + Vue 2.x 前后端分离的 Java 快速开发框架。Maven 多模块 + RBAC 权限模型。

**代码路径**：`RuoYi-Vue-springboot3/` 子目录下

## 构建与运行

### 后端（Spring Boot 3.5.x + JDK 17，可用 JDK 20 编译）

```bash
# 设置 JAVA_HOME（系统默认 JDK 8，需切换到 JDK 20）
export JAVA_HOME="C:/Program Files/Eclipse Adoptium/jdk-20.0.2.9-hotspot"

# Maven 编译
cd RuoYi-Vue-springboot3
mvn clean install -DskipTests

# 启动后端（开发环境）
cd ruoyi-admin
mvn spring-boot:run
```

Maven 路径：`C:/Program Files/JetBrains/IntelliJ IDEA 2026.1.2/plugins/maven/lib/maven3/bin/mvn`

后端默认运行在 **8080** 端口。

### 前端（Vue 2.6.12 + Element UI）

```bash
cd ruoyi-ui
npm install
npm run dev           # 开发服务器，端口 80
npm run build:prod    # 生产构建
```

浏览器打开 `http://localhost`，代理自动转发 API 请求到后端 8080。

### 环境依赖

| 组件 | 地址 | 用途 |
|------|------|------|
| MySQL 8.x | localhost:3306 | 数据库 `ry-vue` |
| Redis | `172.25.157.5:6379` | 缓存/Token/验证码（无密码） |

> Redis 中验证码 key 格式：`captcha_codes:{uuid}`，值为 JSON 字符串（如 `"13"`），读取后需 `strip('"')` 去掉引号。

### 默认账号

| 用户 | 密码 | 用途 |
|------|------|------|
| admin | admin123 | 超级管理员 |
| ry | 123456 | 普通用户 |
| ruoyi | 123456 | Druid 监控面板（访问 `/druid/`） |

### Git 代理与 SSL

项目已配置 HTTP 代理 `127.0.0.1:7897`。推送/拉取时如遇到 `schannel: failed to receive handshake` 错误，使用 openssl 后端：

```bash
# 拉取
git -c http.sslBackend=openssl fetch origin <branch>

# 推送（如 openssl 仍报 TLS 错误，关闭 SSL 校验）
git -c http.sslVerify=false push -u origin <branch>
```

---

## 架构概览

### Maven 模块（6 个）

```
ruoyi-admin        [主入口] Controller、Spring Boot 启动类、YAML 配置
     ↓
ruoyi-framework    [框架] Security 6 认证鉴权、AOP（日志/权限/防重/数据源）、多数据源
     ↓
ruoyi-system       [业务] Domain 实体、MyBatis Mapper + XML、Service
     ↓
ruoyi-common       [公共] 注解、工具类、枚举、过滤器、通用异常、BaseEntity/BaseController

ruoyi-quartz       [定时任务] 依赖 ruoyi-common + ruoyi-system，Controller 在 /monitor/job
ruoyi-generator    [代码生成] Controller 在 /tool/gen，基于 Velocity 模板
```

### 关键配置文件

| 文件 | 内容 |
|------|------|
| `ruoyi-admin/.../application.yml` | 端口、Redis、Token、XSS、SpringDoc、MyBatis、PageHelper |
| `ruoyi-admin/.../application-druid.yml` | MySQL 连接、Druid 连接池、主从、慢SQL |

### SQL 初始化

- `sql/ry_20260417.sql` — 系统核心表 + 作业管理模块表（hw_plan/hw_attendance/hw_plan_worker/hw_plan_video）+ 人员信息管理模块表（tb_worker系列7张表）+ 菜单权限 + 字典数据
- `sql/quartz.sql` — 定时任务表
- `sql/test_data_0603.sql` — 人员管理+作业管理测试数据（8人、9角色、3计划、6打卡记录）
- `sql/updates/` — 结构化数据库迁移脚本（按时间戳命名，同伴 git pull 后按序执行）

---

## 后端开发模式（重要约定）

### 分层规范

新增模块时参考 `SysPost`（`ruoyi-system/.../service/impl/SysPostServiceImpl.java`）作为模板。

| 层 | 位置 | 规则 |
|----|------|------|
| Controller | `ruoyi-admin/.../controller/{module}/` | 继承 `BaseController`，`@RequestMapping`，`@PreAuthorize` |
| Service 接口 | `ruoyi-system/.../service/I{Name}Service.java` | 定义业务方法签名 |
| Service 实现 | `ruoyi-system/.../service/impl/{Name}ServiceImpl.java` | `@Service`，`@Autowired` Mapper |
| Mapper 接口 | `ruoyi-system/.../mapper/{Name}Mapper.java` | 纯接口，不加任何注解 |
| Mapper XML | `ruoyi-system/.../resources/mapper/system/{Name}Mapper.xml` | namespace 指向全限定名 |
| Domain | `ruoyi-system/.../domain/{Name}.java` | 继承 `BaseEntity`，@Excel 注解 |

### 基类和约定

- **BaseEntity**：自带 `createBy`/`createTime`/`updateBy`/`updateTime`/`remark`/`params` Map
- **BaseController**：`startPage()` → service → `getDataTable(list)`（分页），`success(data)`/`error(msg)`（普通响应），`toAjax(int rows)`（写操作）
- **统一响应**：`AjaxResult {code, msg, data}` 和 `TableDataInfo {code, msg, rows, total}`
- **权限字符串格式**：`模块:实体:操作`，如 `homework:plan:list`
- **Service 中获取当前用户**：`SecurityUtils.getUsername()`，设置到 `createBy`/`updateBy`
- **实体 Getter 上放校验注解**：`@NotBlank`/`@NotNull` 放在 getter 方法上，不在字段上
- **日期格式**：`@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")`
- **toString()**：使用 `ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)`
- **XML 动态 SQL**：INSERT 用 `sysdate()`，UPDATE 用 `<set>` + `<if>`，批量删除用 `<foreach>`
- **全局异常**：业务异常抛 `ServiceException` → Controller 中 `try-catch` → `error(e.getMessage())`
- **分页**：前端传 `pageNum`/`pageSize`，后端 `startPage()` 自动调用 PageHelper

### Quartz 定时任务

- `sys_job` 表 `invoke_target` 格式：`beanName.methodName`（如 `hwAttendanceTimeoutJob.execute`）
- 任务类用 `@Component("beanName")`，方法为 public void 无参
- **必须**在 `ruoyi-quartz/pom.xml` 添加 `ruoyi-system` 依赖才能引用 system 模块的 Mapper 和实体

### 新增 SQL 建表规范

- 表名 `hw_xxx`，Java 类名 `HwXxx`
- 继承 `BaseEntity` 的通用字段：`create_by VARCHAR(64)`, `create_time DATETIME`, `update_by VARCHAR(64)`, `update_time DATETIME`, `remark VARCHAR(500)`
- 状态字段：`status CHAR(1) DEFAULT '0' COMMENT '0正常 1停用'`
- 主键：`BIGINT(20) NOT NULL AUTO_INCREMENT`

---

## 前端开发模式

### 目录结构

```
src/views/{module}/{entity}/index.vue   Vue 页面组件
src/api/{module}/{entity}.js            API 请求模块
```

### Vue 页面模板结构

标准列表页 = 搜索表单 + 按钮栏（v-hasPermi）+ 表格 + 分页 + 弹窗对话框：

```javascript
// 标准 data 结构
loading, ids, single, multiple, showSearch, total, list, title, open,
queryParams: { pageNum: 1, pageSize: 10, ... },
form: {},
rules: { ... }

// 标准方法
getList(), handleQuery(), resetQuery(), handleAdd(), handleUpdate(row),
submitForm(), handleDelete(row), cancel(), reset(), handleSelectionChange()

// 全局组件
<pagination>, <right-toolbar>, <dict-tag>
```

### API 模块

每个函数导出对应一个 Controller 端点，使用 `import request from '@/utils/request'`（自动注入 Token，处理 401）。

---

## 常见陷阱（从实际开发中总结）

### 1. MySQL DATETIME 秒级精度 vs Java Date 毫秒

MySQL `DATETIME` 只存到秒，Java `Date` 有毫秒。**时间比较必须截断到秒**，否则同一秒内的进出场会误判：

```java
// ❌ 错误：直接比较
if (checkOutTime.before(checkInTime)) { ... }

// ✅ 正确：截断到秒
long outSec = checkOutTime.getTime() / 1000;
long inSec = checkInTime.getTime() / 1000;
if (outSec < inSec) { ... }
```

### 2. `@Validated` 不能用于部分字段的接口

Controller 的 checkIn/checkOut 方法**不应该**加 `@Validated`，因为 `checkType`（标注了 `@NotBlank`）和 `checkTime` 由 Service 层自动设置，请求体中不传。加了会导致校验失败报"参数不能为空"。

### 3. Controller insert 返回 int 而非 ID

`insertHwPlan` 返回 `int`（受影响行数），不是新记录的 ID。需要获取 ID 时通过搜索项目名称等方式找到新记录。

### 4. sys_job 的 invoke_target 格式

必须是 `beanName.methodName`（如 `hwAttendanceTimeoutJob.execute`），不能只写类名。对应的类用 `@Component("hwAttendanceTimeoutJob")`，方法为 `public void execute()`。

### 5. sys_menu INSERT 的 ID 冲突

当子菜单用 `@parent_id+1,+2,+3` 连续 ID 时，按钮权限必须用更大的偏移量（如 `+11, +101, +201`），避免不同按钮组之间互相碰撞。

### 6. sys_menu 建表列数

`sys_menu` 表有 20 列，但原规格书中的 `sys_job` INSERT 有 15 个值（多了 `'0'` 和 `''`），导致 `Column count doesn't match`。修正为 13 个值：`(job_id, job_name, job_group, invoke_target, cron_expression, misfire_policy, concurrent, status, create_by, create_time, update_by, update_time, remark)`。

---

## 作业管理模块（homework）

### 子模块

| 子模块 | 路径 | 权限前缀 |
|--------|------|---------|
| 作业计划 | `/homework/plan/**` | `homework:plan:*` |
| 作业打卡 | `/homework/attendance/**` | `homework:attendance:*` |

数据库表：`hw_plan`、`hw_attendance`、`hw_plan_worker`（关联 `tb_worker.id`）、`hw_plan_video`。建表 SQL 在 `ry_20260417.sql`。

> **人员管理**已从作业管理模块移除，统一使用外部独立模块「人员信息管理」（`tb_worker` 系列表）。作业打卡中的人脸校验通过 `TbWorkerMapper` + `TbWorkerFaceMapper` 获取人员底图。

### 打卡业务规则（HwAttendanceServiceImpl）

**进场打卡**：
1. 校验 `hw_plan` 存在且 `status='0'`
2. 调 `selectLastCheckIn(planId, userId)` → 如已存在 → `ServiceException("该人员已进场打卡，请先完成离场打卡")`
3. 设置 `checkType="0"`, `checkTime=new Date()`, `checkStatus="0"` → 写入

**离场打卡**：
1. 校验 `hw_plan` 存在
2. 调 `selectLastCheckIn` → 如 null → `ServiceException("未找到进场打卡记录，请先进场打卡")`
3. 调 `selectLastCheckOut` → 如离场时间 > 进场时间 → `ServiceException("该人员已离场打卡，请勿重复操作")`
4. 校验 `outSec >= inSec`（秒级截断比较） → 写入 `checkType="1"`

### Quartz 超时检测

`HwAttendanceTimeoutJob.execute()` 每分钟执行：
- 进场超时：`selectOvertimeUncheckedPlans(30)` → 写 `check_status='2'`, `fail_reason="进场超时"`
- 离场超时：`selectOvertimeCheckIns(480)` → 写 `check_status='2'`, `fail_reason="离场超时"`

### 集成接口（桩实现）

- `IFaceRecognitionService` / `StubFaceRecognitionService` — 人脸识别（返回 `fail("AI人脸识别服务尚未配置")`）
- `IWechatCheckInService` / `StubWechatCheckInService` — 微信打卡（含 Haversine GPS 距离校验，地球半径 6371000m）

---

## 人员信息管理模块（worker）

独立顶级模块，是作业管理模块的人员数据底座。**作业管理不再自带人员管理，所有人员数据统一从此模块获取。**

### 数据库表（7张 tb_worker 系列）

| 表名 | 说明 | 关键字段 |
|------|------|---------|
| `tb_worker` | 人员基础档案（核心） | id, worker_name, phone, id_card, gender, dept_id, status(0在场/1离场/2禁用), face_status, audit_status, unit_type(1管网/2第三方/3施工方), del_flag |
| `tb_worker_role` | 角色规则 | role_code, role_name, need_sign_in/out, need_hourly_check, hourly_interval, need_cert, cert_type |
| `tb_worker_role_rel` | 人员-角色多对多 | worker_id, role_id（UNIQUE约束） |
| `tb_worker_cert` | 资质证件 | cert_type, cert_no, issue_date/expire_date, cert_img, audit_status |
| `tb_worker_face` | 人脸信息 | worker_id, face_img_url, face_feature（TEXT，AI预留） |
| `tb_worker_audit` | 审核记录 | biz_type(worker/cert), biz_id, audit_status, audit_opinion |
| `tb_worker_checkin` | 打卡记录 | check_type(1签到/2签退/3点到), check_method, latitude/longitude, photo_url, helmet_flag, vest_flag |

### 关键业务规则

- **审核回写**：新增/修改审核记录时，自动同步对应主表的 `audit_status` 字段（事务保证）
- **逻辑删除**：`delFlag='2'` + `status='2'`（同时禁用），保留历史数据
- **弱约束资质校验**：角色分配时校验资质需求，缺失时人员标记为"待审核"并返回提示，但不阻断分配
- **主键映射**：`tb_worker.id` 是人员唯一标识，`hw_plan_worker.worker_id` 和 `hw_attendance.user_id` 都关联此字段

### 后端路径

```
ruoyi-admin/.../controller/worker/   ← 6 个管理后台 Controller（TbWorker/TbWorkerRole/TbWorkerCert/TbWorkerFace/TbWorkerAudit/TbWorkerCheckin）
ruoyi-admin/.../controller/app/      ← 4 个移动端 Controller（AppAuth/AppWorker/AppCheckin/AppTokenUtil）
ruoyi-system/.../domain/             ← 6 个实体类
ruoyi-system/.../mapper/             ← 7 个 Mapper 接口
ruoyi-system/.../service/            ← 6 组 Service 接口+实现
ruoyi-system/.../resources/mapper/system/  ← 7 个 MyBatis XML
```

### 前端路径

```
ruoyi-ui/src/views/worker/   ← 6 个子页面（worker/role/cert/face/audit/checkin）
ruoyi-ui/src/api/worker/     ← 6 个 API 模块
```

### 菜单权限前缀

`worker:worker:*`、`worker:role:*`、`worker:cert:*`、`worker:face:*`、`worker:audit:*`、`worker:checkin:*`

---

## RuoYi-App（uni-app 移动端）

基于 uni-app 的工人端 H5/App，位于项目根目录 `RuoYi-App/`。

### 页面结构

```
pages/worker/
├── login.vue       # 手机号+身份证后6位登录
├── profile.vue     # 人员档案首页
├── checkin.vue     # 签到/签退打卡
├── records.vue     # 历史打卡记录
├── certs.vue       # 资质证件查看
├── face.vue        # 人脸采集上传
├── idcard.vue      # 身份证上传
├── upload.vue      # 通用文件上传
└── mine.vue        # 我的（个人信息+资质+设置）
```

### 配置

- API 地址在 `config.js` 的 `baseUrl`（默认 `http://localhost:8080`）
- 使用 HMAC-SHA256 Token 鉴权（`AppTokenUtil`），7天有效期，独立于 Spring Security JWT
- `/app/**` 接口在 `SecurityConfig` 中已放行，使用 `AppTokenFilter` 独立鉴权

### 构建与同步

uni-app 通过 HBuilderX 编译为 H5，产出覆盖到 `ruoyi-ui/public/mobile.html`。开发时在 HBuilderX 中编辑 `RuoYi-App/` 目录，完成后运行：

```bash
./sync-uni.sh   # 将 HBuilderX 改动同步回 git 仓库
```
