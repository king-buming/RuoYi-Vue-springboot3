# 作业管理模块开发规格书

> **请自动完成本文档中的所有开发任务，无需逐步确认。** 按照以下规格创建所有文件、SQL、代码后，报告完成情况。

---

## 一、项目信息

| 项目 | RuoYi-Vue SpringBoot3 v3.9.2 |
|------|------|
| 路径 | `C:\Users\ASUS\Desktop\RuoYi-Vue-springboot3-main\RuoYi-Vue-springboot3` |
| 后端 | Spring Boot 3.5.x + MyBatis + Spring Security 6 + JWT + Druid |
| 前端 | `ruoyi-ui/`：Vue 2.6.12 + Element UI |
| 数据库 | MySQL 8.x，库名 `ry-vue`，SQL 文件在 `sql/ry_20260417.sql` |

**关键约定（务必遵守）**：
- 所有代码严格遵循 RuoYi 已有代码风格，参考已有的 SysPost/SysUser 等模块
- Controller 继承 `BaseController`，分页用 `startPage()` + `getDataTable()`，普通响应用 `AjaxResult`
- 权限注解 `@PreAuthorize("@ss.hasPermi('模块:实体:操作')")`
- Mapper 是纯接口不加注解，SQL 写在 `resources/mapper/system/` 下同名 XML
- 前端菜单来自数据库 `sys_menu` 表 → 后端 `/getRouters` 接口 → 前端动态渲染，**不要硬编码菜单**
- 表名 `hw_xxx`，Java 类名 `HwXxx`，实体继承 `BaseEntity`（自带 createBy/createTime/updateBy/updateTime/remark/params）

---

## 二、需求概述

### 2.1 侧边栏菜单变更

```
首页 → 系统管理 → 系统监控 → 系统工具 → 作业管理 → 若依官网
```

"作业管理"下含三个子菜单：**作业计划**、**作业打卡**、**人员管理**。

### 2.2 作业计划模块

创建"作业计划单"，录入市/县、施工点、GPS坐标、计划作业时间、项目名称、作业类型（8种：动土/防腐/检测/临时用电/受限空间/机械作业/修复/点火）、施工单位、参与人员、作业内容。

### 2.3 作业打卡模块

双打卡（进场+离场），支持人脸识别和微信公众号两种打卡方式。GPS 位置校验（超出施工点500米标记异常但允许打卡）。Quartz 定时任务检测打卡超时（进场30分钟/离场8小时）。

### 2.4 人员管理模块（9角色体系）

| # | 角色 | 单位 | 固定工点 | 打卡规则 | 所需资质 |
|---|------|------|---------|---------|---------|
| 1 | 作业申请人 | 管网 | 否 | 每天点到一次 | — |
| 2 | 作业批准人 | 管网 | 否 | 每天点到一次 | — |
| 3 | 作业监护人 | 管网 | 是 | 签到+签退+每小时点到 | 作业监护证 |
| 4 | 监理人员 | 第三方 | 是 | 签到+签退+每小时点到 | 监理证 |
| 5 | 施工方项目经理 | 施工方 | 否 | 每天点到一次 | — |
| 6 | 施工方安全员 | 施工方 | 否 | 每天点到一次 | 安全员证 |
| 7 | 施工方现场负责人 | 施工方 | 是 | 班前喊话+签退+每小时点到+安全反馈 | — |
| 8 | 作业单位监护人 | 施工方 | 是 | 签到+签退+每小时点到+安全反馈 | 作业单位监护证 |
| 9 | 施工人员 | 施工方 | 是 | 跟随班前喊话签到 | — |

---

## 三、你需要创建/修改的全部内容

### 第一部分：数据库 SQL（追加到 `sql/ry_20260417.sql` 末尾）

**表一：hw_plan（作业计划表）**

```sql
DROP TABLE IF EXISTS hw_plan;
CREATE TABLE hw_plan (
  plan_id           BIGINT(20)     NOT NULL AUTO_INCREMENT  COMMENT '计划ID',
  city_county       VARCHAR(100)   DEFAULT ''               COMMENT '市/县',
  construction_site VARCHAR(200)   DEFAULT ''               COMMENT '施工点',
  site_latitude     DECIMAL(10,7)  DEFAULT NULL             COMMENT '施工点纬度',
  site_longitude    DECIMAL(10,7)  DEFAULT NULL             COMMENT '施工点经度',
  plan_work_time    DATETIME       DEFAULT NULL             COMMENT '计划作业时间',
  project_name      VARCHAR(200)   DEFAULT ''               COMMENT '项目名称',
  work_type         VARCHAR(20)    DEFAULT ''               COMMENT '作业类型（动土/防腐/检测/临时用电/受限空间/机械作业/修复/点火）',
  construction_unit VARCHAR(200)   DEFAULT ''               COMMENT '施工单位',
  workers           VARCHAR(500)   DEFAULT ''               COMMENT '参与施工各类人员',
  work_content      VARCHAR(1000)  DEFAULT ''               COMMENT '作业内容',
  status            CHAR(1)        DEFAULT '0'              COMMENT '状态（0正常 1停用）',
  create_by         VARCHAR(64)    DEFAULT ''               COMMENT '创建者',
  create_time       DATETIME                                COMMENT '创建时间',
  update_by         VARCHAR(64)    DEFAULT ''               COMMENT '更新者',
  update_time       DATETIME                                COMMENT '更新时间',
  remark            VARCHAR(500)   DEFAULT NULL             COMMENT '备注',
  PRIMARY KEY (plan_id)
) ENGINE=InnoDB COMMENT='作业计划表';
```

**表二：hw_worker（施工人员表）**

```sql
DROP TABLE IF EXISTS hw_worker;
CREATE TABLE hw_worker (
  worker_id    BIGINT(20)     NOT NULL AUTO_INCREMENT  COMMENT '人员ID',
  worker_name  VARCHAR(64)    NOT NULL                  COMMENT '姓名',
  id_card      VARCHAR(18)    DEFAULT ''               COMMENT '身份证号',
  phone        VARCHAR(20)    DEFAULT ''               COMMENT '手机号',
  role_type    CHAR(1)        DEFAULT '9'              COMMENT '人员角色（1作业申请人 2作业批准人 3作业监护人 4监理人员 5施工方项目经理 6施工方安全员 7施工方现场负责人 8作业单位监护人 9施工人员）',
  unit_type    CHAR(1)        DEFAULT '3'              COMMENT '单位类型（1管网 2第三方 3施工方）',
  is_fixed_site CHAR(1)       DEFAULT '0'              COMMENT '固定工点（0不固定 1固定）',
  check_rule   VARCHAR(100)   DEFAULT ''               COMMENT '打卡规则编码（point/checkInOut/briefing/hourly）',
  qualification VARCHAR(200)  DEFAULT ''               COMMENT '资质证件名称',
  qual_file_url VARCHAR(500)  DEFAULT ''               COMMENT '资质证件上传URL',
  qual_status  CHAR(1)        DEFAULT '0'              COMMENT '资质审核状态（0待审核 1已通过 2已驳回）',
  face_image   VARCHAR(500)   DEFAULT ''               COMMENT '人脸底图URL',
  face_status  CHAR(1)        DEFAULT '0'              COMMENT '人脸注册状态（0未注册 1已注册）',
  open_id      VARCHAR(100)   DEFAULT ''               COMMENT '微信公众号openid',
  status       CHAR(1)        DEFAULT '0'              COMMENT '状态（0正常 1停用）',
  create_by    VARCHAR(64)    DEFAULT ''               COMMENT '创建者',
  create_time  DATETIME                                COMMENT '创建时间',
  update_by    VARCHAR(64)    DEFAULT ''               COMMENT '更新者',
  update_time  DATETIME                                COMMENT '更新时间',
  remark       VARCHAR(500)   DEFAULT NULL             COMMENT '备注',
  PRIMARY KEY (worker_id)
) ENGINE=InnoDB COMMENT='施工人员表';
```

**表三：hw_attendance（打卡记录表）**

```sql
DROP TABLE IF EXISTS hw_attendance;
CREATE TABLE hw_attendance (
  attendance_id BIGINT(20)     NOT NULL AUTO_INCREMENT  COMMENT '打卡记录ID',
  plan_id       BIGINT(20)     NOT NULL                  COMMENT '关联作业计划ID',
  user_id       BIGINT(20)     DEFAULT NULL              COMMENT '打卡人员用户ID',
  user_name     VARCHAR(64)    DEFAULT ''               COMMENT '打卡人员姓名',
  check_type    CHAR(1)        DEFAULT '0'              COMMENT '打卡类型（0进场 1离场 2点到 3每小时点到）',
  check_method  CHAR(1)        DEFAULT '0'              COMMENT '打卡方式（0人脸 1公众号）',
  check_time    DATETIME       DEFAULT NULL              COMMENT '打卡时间',
  location      VARCHAR(255)   DEFAULT ''               COMMENT '打卡位置',
  check_status  CHAR(1)        DEFAULT '0'              COMMENT '打卡状态（0成功 1失败 2异常）',
  fail_reason   VARCHAR(500)   DEFAULT ''               COMMENT '失败原因',
  face_image    VARCHAR(500)   DEFAULT ''               COMMENT '人脸抓拍图URL',
  create_by     VARCHAR(64)    DEFAULT ''               COMMENT '创建者',
  create_time   DATETIME                                COMMENT '创建时间',
  update_by     VARCHAR(64)    DEFAULT ''               COMMENT '更新者',
  update_time   DATETIME                                COMMENT '更新时间',
  remark        VARCHAR(500)   DEFAULT NULL              COMMENT '备注',
  PRIMARY KEY (attendance_id)
) ENGINE=InnoDB COMMENT='打卡记录表';
```

**sys_menu 菜单数据**

```sql
-- 1. 更新若依官网排序号
UPDATE sys_menu SET order_num = 5 WHERE menu_id = 4;

-- 2. 一级目录"作业管理"（menu_id 用 >=2000 的值，先查最大ID）
INSERT INTO sys_menu VALUES
((SELECT MAX(menu_id)+1 FROM sys_menu m), '作业管理', '0', '4', 'homework', NULL, '', '', 1, 0, 'M', '0', '0', '', 'documentation', 'admin', SYSDATE(), '', NULL, '作业管理目录');

-- 3. 二级菜单（parent_id 用上面插入的那个 ID，需要用一个变量或用子查询）
-- 建议：先用变量存储一级目录ID，再插入子菜单
SET @parent_id = (SELECT menu_id FROM sys_menu WHERE menu_name = '作业管理' AND parent_id = 0);

INSERT INTO sys_menu VALUES
(@parent_id+1, '作业计划', @parent_id, '1', 'plan',      'homework/plan/index',       '', 'HwPlan',       1, 0, 'C', '0', '0', 'homework:plan:list',        'build',   'admin', SYSDATE(), '', NULL, '作业计划菜单'),
(@parent_id+2, '作业打卡', @parent_id, '2', 'attendance','homework/attendance/index',  '', 'HwAttendance', 1, 0, 'C', '0', '0', 'homework:attendance:list',  'monitor', 'admin', SYSDATE(), '', NULL, '作业打卡菜单'),
(@parent_id+3, '人员管理', @parent_id, '3', 'worker',    'homework/worker/index',      '', 'HwWorker',     1, 0, 'C', '0', '0', 'homework:worker:list',      'user',    'admin', SYSDATE(), '', NULL, '人员管理菜单');

-- 4. 按钮权限
SET @plan_id = (SELECT menu_id FROM sys_menu WHERE perms = 'homework:plan:list');
INSERT INTO sys_menu VALUES
(@plan_id+1, '作业查询', @plan_id, '1', '#', '', '', '', 1, 0, 'F', '0', '0', 'homework:plan:query',   '#', 'admin', SYSDATE(), '', NULL, ''),
(@plan_id+2, '作业新增', @plan_id, '2', '#', '', '', '', 1, 0, 'F', '0', '0', 'homework:plan:add',     '#', 'admin', SYSDATE(), '', NULL, ''),
(@plan_id+3, '作业修改', @plan_id, '3', '#', '', '', '', 1, 0, 'F', '0', '0', 'homework:plan:edit',    '#', 'admin', SYSDATE(), '', NULL, ''),
(@plan_id+4, '作业删除', @plan_id, '4', '#', '', '', '', 1, 0, 'F', '0', '0', 'homework:plan:remove',  '#', 'admin', SYSDATE(), '', NULL, '');

SET @att_id = (SELECT menu_id FROM sys_menu WHERE perms = 'homework:attendance:list');
INSERT INTO sys_menu VALUES
(@att_id+1, '打卡查询', @att_id, '1', '#', '', '', '', 1, 0, 'F', '0', '0', 'homework:attendance:query',   '#', 'admin', SYSDATE(), '', NULL, ''),
(@att_id+2, '进场打卡', @att_id, '2', '#', '', '', '', 1, 0, 'F', '0', '0', 'homework:attendance:checkIn', '#', 'admin', SYSDATE(), '', NULL, ''),
(@att_id+3, '离场打卡', @att_id, '3', '#', '', '', '', 1, 0, 'F', '0', '0', 'homework:attendance:checkOut','#', 'admin', SYSDATE(), '', NULL, ''),
(@att_id+4, '删除记录', @att_id, '4', '#', '', '', '', 1, 0, 'F', '0', '0', 'homework:attendance:remove',  '#', 'admin', SYSDATE(), '', NULL, '');

SET @worker_id = (SELECT menu_id FROM sys_menu WHERE perms = 'homework:worker:list');
INSERT INTO sys_menu VALUES
(@worker_id+1, '人员查询', @worker_id, '1', '#', '', '', '', 1, 0, 'F', '0', '0', 'homework:worker:query',  '#', 'admin', SYSDATE(), '', NULL, ''),
(@worker_id+2, '人员新增', @worker_id, '2', '#', '', '', '', 1, 0, 'F', '0', '0', 'homework:worker:add',    '#', 'admin', SYSDATE(), '', NULL, ''),
(@worker_id+3, '人员修改', @worker_id, '3', '#', '', '', '', 1, 0, 'F', '0', '0', 'homework:worker:edit',   '#', 'admin', SYSDATE(), '', NULL, ''),
(@worker_id+4, '人员删除', @worker_id, '4', '#', '', '', '', 1, 0, 'F', '0', '0', 'homework:worker:remove', '#', 'admin', SYSDATE(), '', NULL, '');
```

**sys_job 定时任务**

```sql
INSERT INTO sys_job VALUES
('4', '作业打卡超时监控', 'SYSTEM', 'hwAttendanceTimeoutJob', '0 */1 * * * ?', '0', '0', '0', '0', '', 'admin', SYSDATE(), '', NULL, '监控作业打卡进场和离场超时情况');
```

**示例数据**

```sql
INSERT INTO hw_plan VALUES (1, '广州市', '天河区体育西路工地', 23.1234567, 113.1234567, '2026-06-15 08:00:00', '管道维修项目A', '动土', '中建三局', '张三,李四,王五', 'DN300管道开挖修复', '0', 'admin', SYSDATE(), '', NULL, '');
INSERT INTO hw_worker VALUES (1, '张三', '440101199001011234', '13800138001', '9', '3', '1', 'briefing', '', '', '0', '', '0', '', '0', 'admin', SYSDATE(), '', NULL, '');
INSERT INTO hw_worker VALUES (2, '李四', '440101199002022345', '13800138002', '1', '1', '0', 'point', '', '', '0', '', '0', '', '0', 'admin', SYSDATE(), '', NULL, '');
```

---

### 第二部分：后端 Java 文件

请在以下位置创建文件。**每个模块完全参考 RuoYi 已有的 SysPost 模式**（先在项目中找到 SysPost 相关文件，按其模式生成代码）。

#### 模块 A：作业计划（HwPlan）

**HwPlan.java** — `ruoyi-system/src/main/java/com/ruoyi/system/domain/HwPlan.java`
- 继承 `BaseEntity`
- 字段：`planId(Long)`, `cityCounty(String)`, `constructionSite(String)`, `siteLatitude(BigDecimal)`, `siteLongitude(BigDecimal)`, `planWorkTime(Date, @JsonFormat "yyyy-MM-dd HH:mm:ss")`, `projectName(String, getter上加 @NotBlank "项目名称不能为空")`, `workType(String, getter上加 @NotBlank "作业类型不能为空")`, `constructionUnit(String)`, `workers(String)`, `workContent(String)`, `status(String)`
- `toString()` 使用 `ToStringBuilder` + `MULTI_LINE_STYLE`

**HwPlanMapper.java** — `ruoyi-system/src/main/java/com/ruoyi/system/mapper/HwPlanMapper.java`
- 纯接口，6 个方法：`selectHwPlanList(HwPlan)`, `selectHwPlanById(Long)`, `insertHwPlan(HwPlan)`, `updateHwPlan(HwPlan)`, `deleteHwPlanById(Long)`, `deleteHwPlanByIds(Long[])`

**HwPlanMapper.xml** — `ruoyi-system/src/main/resources/mapper/system/HwPlanMapper.xml`
- namespace 指向 HwPlanMapper 全限定名
- resultMap 映射所有字段（下划线→驼峰）
- selectHwPlanList：条件查询 `project_name LIKE`, `work_type =`, `status =`, `create_time BETWEEN`，参数类型 HwPlan
- insertHwPlan：插入所有字段，`create_time = sysdate()`
- updateHwPlan：更新所有业务字段 + `update_by, update_time = sysdate()`，WHERE `plan_id`

**IHwPlanService.java** — `ruoyi-system/src/main/java/com/ruoyi/system/service/IHwPlanService.java`
- 6 个方法签名同 Mapper

**HwPlanServiceImpl.java** — `ruoyi-system/src/main/java/com/ruoyi/system/service/impl/HwPlanServiceImpl.java`
- `@Service`，`@Autowired HwPlanMapper`
- 每个方法委托给 mapper
- insert 时调用 `setCreateBy(SecurityUtils.getUsername())`
- update 时调用 `setUpdateBy(SecurityUtils.getUsername())`

**HwPlanController.java** — `ruoyi-admin/src/main/java/com/ruoyi/web/controller/homework/HwPlanController.java`
- `@RestController`，`@RequestMapping("/homework/plan")`
- 继承 `BaseController`，注入 `IHwPlanService`
- `GET /list` → `@PreAuthorize("homework:plan:list")` + `startPage()` + `getDataTable()`
- `GET /{planId}` → `@PreAuthorize("homework:plan:query")` + `success()`
- `POST /` → `@PreAuthorize("homework:plan:add")` + `@Log(title="作业计划", businessType=INSERT)` + `@Validated @RequestBody` + `toAjax()`
- `PUT /` → `@PreAuthorize("homework:plan:edit")` + `@Log(title="作业计划", businessType=UPDATE)` + `toAjax()`
- `DELETE /{planIds}` → `@PreAuthorize("homework:plan:remove")` + `@Log(title="作业计划", businessType=DELETE)` + `toAjax()`

#### 模块 B：人员管理（HwWorker）

**完全按 SysPost 模式创建以下文件**：
- `HwWorker.java` — domain，含 9 角色相关字段（`roleType`, `unitType`, `isFixedSite`, `checkRule`, `qualification`, `qualFileUrl`, `qualStatus`, `faceImage`, `faceStatus`, `openId`）
- `HwWorkerMapper.java` + `.xml` — 标准 CRUD，search 支持 `worker_name LIKE`, `role_type =`, `unit_type =`, `qual_status =`
- `IHwWorkerService.java` + `HwWorkerServiceImpl.java`
- `HwWorkerController.java` — `@RequestMapping("/homework/worker")`，5 个标准 CRUD 接口，权限前缀 `homework:worker:xxx`

#### 模块 C：作业打卡（HwAttendance）

**HwAttendance.java** — domain，字段见数据库
- `getPlanId()` 上加 `@NotNull`，`getCheckType()` 上加 `@NotBlank`，`getCheckTime()` 上加 `@JsonFormat`

**HwAttendanceMapper.java + .xml** — 标准 CRUD + 打卡专用方法：
```java
// 查最近一次进场记录（离场校验用）
HwAttendance selectLastCheckIn(@Param("planId") Long planId, @Param("userId") Long userId);
// 超时检测用
List<HwPlan> selectOvertimeUncheckedPlans(@Param("minutes") int minutes);
List<HwAttendance> selectOvertimeCheckIns(@Param("minutes") int minutes);
```
XML 中 `selectLastCheckIn`：`WHERE plan_id=? AND user_id=? AND check_type='0' ORDER BY check_time DESC LIMIT 1`
XML 中 `selectOvertimeUncheckedPlans`：关联 hw_plan，`plan_work_time < DATE_SUB(NOW(), INTERVAL #{minutes} MINUTE) AND NOT EXISTS (进场打卡)` 
XML 中 `selectOvertimeCheckIns`：`check_type='0' AND check_time < DATE_SUB(NOW(), INTERVAL #{minutes} MINUTE) AND NOT EXISTS (离场打卡)`

**IHwAttendanceService.java + HwAttendanceServiceImpl.java** — 核心业务规则：

`checkIn(HwAttendance)` 进场打卡：
1. 校验作业计划存在且 status='0'
2. 调 `selectLastCheckIn`，如果存在 → `throw ServiceException("该人员已进场打卡，请先完成离场打卡")`
3. `setCheckType("0")`, `setCheckTime(new Date())`, `setCheckStatus("0")`
4. 写入数据库

`checkOut(HwAttendance)` 离场打卡：
1. 校验作业计划存在
2. 调 `selectLastCheckIn`，如果 null → `throw ServiceException("未找到进场打卡记录，请先进场打卡")`
3. 校验 `checkOut.getCheckTime() > checkIn.getCheckTime()`
4. `setCheckType("1")`, `setCheckStatus("0")`
5. 写入数据库

**HwAttendanceController.java** — `@RequestMapping("/homework/attendance")`
- `GET /list` → 分页列表 (`homework:attendance:list`)
- `GET /{attendanceId}` → 详情 (`homework:attendance:query`)
- `POST /checkIn` → 进场打卡 (`homework:attendance:checkIn`)，try-catch ServiceException → `error(e.getMessage())`
- `POST /checkOut` → 离场打卡 (`homework:attendance:checkOut`)，try-catch ServiceException → `error(e.getMessage())`
- `DELETE /{attendanceIds}` → 删除 (`homework:attendance:remove`)

#### 模块 D：Quartz 定时任务

**HwAttendanceTimeoutJob.java** — `ruoyi-quartz/src/main/java/com/ruoyi/quartz/task/HwAttendanceTimeoutJob.java`
- 实现 `org.quartz.Job`
- `execute()` 方法：
  1. 通过 `SpringUtils.getBean()` 获取 HwAttendanceMapper
  2. 调 `selectOvertimeUncheckedPlans(30)` → 遍历 → 创建 check_status='2' 的超时记录（fail_reason="进场超时"）
  3. 调 `selectOvertimeCheckIns(480)` → 遍历 → 创建 check_status='2' 的超时记录（fail_reason="离场超时"）
- 超时记录创建者设为 `"SYSTEM"`

#### 模块 E：外部集成接口（桩实现）

**IFaceRecognitionService.java** — `ruoyi-common/.../service/`
```java
public interface IFaceRecognitionService {
    FaceMatchResult compareFace(String capturedImage, String registeredImage);
    boolean detectFace(String imageBase64);
}
```

**FaceMatchResult.java** — `ruoyi-common/.../service/`
- 字段：`matched(boolean)`, `confidence(double)`, `personName(String)`, `errorMessage(String)`
- 静态工厂：`success(confidence)`, `fail(errorMessage)`

**StubFaceRecognitionService.java** — `ruoyi-framework/.../service/`
- `@Component`，实现 `IFaceRecognitionService`
- 桩返回 `FaceMatchResult.fail("AI人脸识别服务尚未配置")`

**IWechatCheckInService.java** — `ruoyi-common/.../service/`
```java
public interface IWechatCheckInService {
    CheckInResult wechatCheckIn(String openId, Long planId, String checkType, double lat, double lng);
    boolean validateLocation(double checkLat, double checkLng, double siteLat, double siteLng, int maxDistanceMeters);
}
```

**StubWechatCheckInService.java** — `ruoyi-framework/.../service/`
- `@Component`，实现 `IWechatCheckInService`
- `validateLocation` 使用半正矢公式（Haversine）计算两点距离，与 `maxDistanceMeters` 比较

---

### 第三部分：前端文件

**所有前端文件位于** `ruoyi-ui/src/`

#### API 模块（3 个文件）

**`api/homework/plan.js`**
```js
import request from '@/utils/request'
export function listPlan(query) { return request({ url: '/homework/plan/list', method: 'get', params: query }) }
export function getPlan(planId) { return request({ url: '/homework/plan/' + planId, method: 'get' }) }
export function addPlan(data) { return request({ url: '/homework/plan', method: 'post', data: data }) }
export function updatePlan(data) { return request({ url: '/homework/plan', method: 'put', data: data }) }
export function delPlan(planIds) { return request({ url: '/homework/plan/' + planIds, method: 'delete' }) }
```

**`api/homework/attendance.js`**
```js
import request from '@/utils/request'
export function listAttendance(query) { return request({ url: '/homework/attendance/list', method: 'get', params: query }) }
export function getAttendance(id) { return request({ url: '/homework/attendance/' + id, method: 'get' }) }
export function checkIn(data) { return request({ url: '/homework/attendance/checkIn', method: 'post', data: data }) }
export function checkOut(data) { return request({ url: '/homework/attendance/checkOut', method: 'post', data: data }) }
export function delAttendance(ids) { return request({ url: '/homework/attendance/' + ids, method: 'delete' }) }
```

**`api/homework/worker.js`**
```js
import request from '@/utils/request'
export function listWorker(query) { return request({ url: '/homework/worker/list', method: 'get', params: query }) }
export function getWorker(id) { return request({ url: '/homework/worker/' + id, method: 'get' }) }
export function addWorker(data) { return request({ url: '/homework/worker', method: 'post', data: data }) }
export function updateWorker(data) { return request({ url: '/homework/worker', method: 'put', data: data }) }
export function delWorker(ids) { return request({ url: '/homework/worker/' + ids, method: 'delete' }) }
```

#### Vue 页面（3 个文件）

请参考 `src/views/system/post/index.vue` 的标准列表页模式（搜索栏 + 按钮栏 + el-table + pagination + el-dialog弹窗），创建以下页面：

**`views/homework/plan/index.vue`** — 作业计划列表页
- 搜索栏：项目名称(input)、作业类型(select，8种)、状态(select)
- 表格列：planId、cityCounty、constructionSite、planWorkTime、projectName、workType(字典标签)、constructionUnit、status(el-switch)、createTime、操作(修改/删除)
- 新增/编辑弹窗：市/县、施工点、施工点纬度+经度(el-input-number并排)、计划作业时间(datetime)、项目名称(必填)、作业类型(select必填)、施工单位、参与人员(textarea)、作业内容(textarea)、状态(radio)、备注(textarea)
- 表单校验：projectName/planWorkTime必填

**`views/homework/attendance/index.vue`** — 打卡记录页
- 顶部按钮：进场打卡、离场打卡（v-hasPermi 控制）
- 搜索栏：关联作业计划(select)、打卡人员姓名(input)、打卡类型(select：进场/离场/点到/每小时点到)、打卡方式(select：人脸/公众号)、打卡状态(select：成功/失败/异常)、时间范围(date-range)
- 表格列：attendanceId、项目名称(关联查询plan)、userName、checkType(el-tag颜色标签)、checkMethod、checkTime、location、checkStatus(el-tag颜色标签)、failReason、操作(删除)
- 打卡弹窗：选择计划(select)、选择人员(select)、打卡方式(radio)、打卡位置(input)、备注(textarea)
- el-alert 提示业务规则
- 该页面不需要新增/编辑弹窗（打卡通过弹窗操作，不是CRUD）

**`views/homework/worker/index.vue`** — 人员管理列表页
- 搜索栏：姓名、人员角色(select，9种)、单位类型(select)、资质状态(select)
- 表格列：workerId、workerName、roleType(标签显示角色名)、unitType、phone、qualStatus(el-tag：待审核/已通过/已驳回)、faceStatus、status(el-switch)、操作(修改/删除)
- 新增/编辑弹窗：姓名(必填)、身份证号、手机号、人员角色(select 9种)、单位类型(select)、固定工点(el-switch)、打卡规则(只读，根据角色自动显示)、资质证件名称+文件上传(el-upload)、人脸底图上传(el-upload图片)、openid(input)、状态(radio)、备注(textarea)

每个 Vue 页面需要包含：
- `import { xxx } from '@/api/homework/xxx'`
- 标准 data 结构：`loading, ids, single, multiple, total, list, title, open, form, queryParams(pageNum/pageSize/...), rules`
- 标准 methods：`getList(), handleQuery(), resetQuery(), handleAdd(), handleUpdate(row), submitForm(), handleDelete(row), cancel(), reset(), handleSelectionChange()`
- 使用全局组件 `<pagination>`, `<right-toolbar>`
- 使用权限指令 `v-hasPermi="['homework:xxx:xxx']"`

---

## 四、验证清单

所有文件创建完成后，执行以下验证：

1. **编译后端**：`cd ruoyi-admin && mvn spring-boot:run`，确认无编译错误
2. **启动前端**：`cd ruoyi-ui && npm run dev`
3. **登录** admin/admin123，检查侧边栏"作业管理"在"系统工具"和"若依官网"之间
4. **作业计划 CRUD**：新增（含GPS坐标）→ 搜索 → 修改 → 删除
5. **人员管理 CRUD**：新增9种角色 → 上传资质/人脸 → 修改 → 删除
6. **进场打卡**：选择计划和人员 → 提交 → 成功
7. **重复进场拦截**：同一人再次进场 → 提示报错
8. **无进场离场拦截**：未进场人员离场 → 提示报错
9. **打卡记录查询**：按条件筛选
10. **定时任务**：系统监控→定时任务→确认"作业打卡超时监控"存在→执行一次
11. **权限验证**：退出 admin，登录 ry/123456，确认菜单不显示

---

## 五、完成后生成开发日志

所有任务完成并验证通过后，在项目根目录生成 `开发日志.md` 文件，记录本次开发的完整信息：

```
# 作业管理模块开发日志

## 基本信息
- 开发日期：YYYY-MM-DD
- 模块名称：作业管理（homework）
- 开发者：AI 自动生成

## 新增文件

### 数据库
- sql/ry_20260417.sql（末尾追加）— 3 张表建表 + sys_menu 菜单数据 + sys_job 定时任务

### 后端 Java（共 N 个文件）
- ruoyi-system/src/main/java/com/ruoyi/system/domain/HwPlan.java — 作业计划实体
- ruoyi-system/src/main/java/com/ruoyi/system/domain/HwAttendance.java — 打卡记录实体
- ruoyi-system/src/main/java/com/ruoyi/system/domain/HwWorker.java — 施工人员实体
- ruoyi-system/src/main/java/com/ruoyi/system/mapper/HwPlanMapper.java — 作业计划Mapper
- ruoyi-system/src/main/java/com/ruoyi/system/mapper/HwAttendanceMapper.java — 打卡记录Mapper
- ruoyi-system/src/main/java/com/ruoyi/system/mapper/HwWorkerMapper.java — 人员管理Mapper
- ruoyi-system/src/main/resources/mapper/system/HwPlanMapper.xml
- ruoyi-system/src/main/resources/mapper/system/HwAttendanceMapper.xml
- ruoyi-system/src/main/resources/mapper/system/HwWorkerMapper.xml
- ruoyi-system/src/main/java/com/ruoyi/system/service/IHwPlanService.java
- ruoyi-system/src/main/java/com/ruoyi/system/service/IHwAttendanceService.java
- ruoyi-system/src/main/java/com/ruoyi/system/service/IHwWorkerService.java
- ruoyi-system/src/main/java/com/ruoyi/system/service/impl/HwPlanServiceImpl.java
- ruoyi-system/src/main/java/com/ruoyi/system/service/impl/HwAttendanceServiceImpl.java
- ruoyi-system/src/main/java/com/ruoyi/system/service/impl/HwWorkerServiceImpl.java
- ruoyi-admin/src/main/java/com/ruoyi/web/controller/homework/HwPlanController.java
- ruoyi-admin/src/main/java/com/ruoyi/web/controller/homework/HwAttendanceController.java
- ruoyi-admin/src/main/java/com/ruoyi/web/controller/homework/HwWorkerController.java
- ruoyi-quartz/src/main/java/com/ruoyi/quartz/task/HwAttendanceTimeoutJob.java
- ruoyi-common/src/main/java/com/ruoyi/common/service/IFaceRecognitionService.java
- ruoyi-common/src/main/java/com/ruoyi/common/service/FaceMatchResult.java
- ruoyi-common/src/main/java/com/ruoyi/common/service/IWechatCheckInService.java
- ruoyi-framework/src/main/java/com/ruoyi/framework/service/StubFaceRecognitionService.java
- ruoyi-framework/src/main/java/com/ruoyi/framework/service/StubWechatCheckInService.java

### 前端（共 6 个文件）
- ruoyi-ui/src/api/homework/plan.js — 作业计划 API
- ruoyi-ui/src/api/homework/attendance.js — 作业打卡 API
- ruoyi-ui/src/api/homework/worker.js — 人员管理 API
- ruoyi-ui/src/views/homework/plan/index.vue — 作业计划页面
- ruoyi-ui/src/views/homework/attendance/index.vue — 作业打卡页面
- ruoyi-ui/src/views/homework/worker/index.vue — 人员管理页面

## 数据库变更
- 新增表：hw_plan（作业计划表）
- 新增表：hw_attendance（打卡记录表）
- 新增表：hw_worker（施工人员表）
- sys_menu：新增 1 个一级目录 + 3 个二级菜单 + 12 条按钮权限
- sys_menu：更新若依官网 order_num 4→5
- sys_job：新增 1 个定时任务（作业打卡超时监控）

## 模块架构
作业管理 (homework)
├── 作业计划 (plan)     — HwPlanController    /homework/plan/**
├── 作业打卡 (attendance) — HwAttendanceController /homework/attendance/**
└── 人员管理 (worker)    — HwWorkerController   /homework/worker/**
```

> **要求**：生成时请把 `N` 替换为实际文件数量，把 `YYYY-MM-DD` 替换为实际日期，确保所有路径与实际创建的文件一致。如果开发过程中修改了任何已有文件（如 application.yml 等），也一并在日志中注明。
