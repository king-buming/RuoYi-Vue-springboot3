# 作业管理模块 V3 — 新增作业审核模块规格书

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

### 2.1 业务流程变更

**旧流程**：

```
作业计划（创建） → 作业打卡（进场/离场）
```

**新流程**：

```
作业计划（创建） → 作业审核（人工审核） → 作业打卡（进场/离场）
```

作业计划创建后，系统自动将作业信息传入作业审核模块，生成一条待审核记录。由作业批准人（或管理员）进行人工审核，审核通过后计划状态变为"待执行"，方可进入作业打卡环节。

### 2.2 权限控制

| 操作 | 允许角色 | 说明 |
|------|---------|------|
| 创建作业计划 | 施工方人员（非普通施工人员）+ 系统管理员 | 施工方人员指 `unit_type='3'` 且角色不是施工人员（`role_type != '9'`） |
| 审核作业计划 | 作业批准人 + 系统管理员 | 作业批准人即 TbWorker 中 `role_type='2'` 的人员 |
| 作业打卡 | 审核通过后所有参与人员 | checkIn/checkOut 需校验计划状态为"待执行"或"进行中" |

### 2.3 侧边栏菜单变更

```
首页 → 系统管理 → 系统监控 → 系统工具 → 作业管理 → 若依官网
```

"作业管理"下含三个子菜单：**作业计划**、**作业审核**（新增）、**作业打卡**。

> 原"作业打卡" order_num 从 2 改为 3，新"作业审核"占 order_num=2。

### 2.4 作业计划状态流转

| 状态值 | 状态名 | 说明 |
|--------|--------|------|
| 0 | 待审核 | 计划创建后初始状态，等待审核 |
| 1 | 待执行 | 审核通过，可以开始打卡 |
| 2 | 进行中 | 已有人进场打卡，作业进行中 |
| 3 | 已完成 | 作业正常完成 |
| 4 | 已取消 | 审核驳回或主动取消 |

**流转规则**：

```
待审核(0) ──审核通过──→ 待执行(1) ──开始作业──→ 进行中(2) ──标记完成──→ 已完成(3)
    │                      │                        │
    └──审核驳回/取消──→ 已取消(4)  ←──取消──┘  ←──取消──┘
```

- 0→1：审核通过（由审核模块触发）
- 0→4：审核驳回 或 创建者主动取消
- 1→2：首次进场打卡时自动触发
- 1→4：创建者主动取消
- 2→3：创建者标记完成
- 2→4：创建者主动取消
- 3→?：已完成不可再变更
- 4→0：管理员可恢复已取消的计划到待审核状态

### 2.5 作业审核流程

```
作业计划创建 → 自动生成审核记录（review_status='0'）
                    ↓
          作业批准人/管理员查看审核列表
                    ↓
          ┌──── 审核通过 ────┬──── 审核驳回 ────┐
          ↓                  ↓                  ↓
    plan.status='1'    plan.status='4'    填写审核意见
    review_status='1'  review_status='2'
          ↓
     进入作业打卡
```

---

## 三、你需要创建/修改的全部内容

### 第一部分：数据库 SQL（追加到 `sql/ry_20260417.sql` 末尾）

#### 1.1 修改 hw_plan.status 列注释

```sql
ALTER TABLE hw_plan MODIFY COLUMN status CHAR(1) DEFAULT '0' COMMENT '状态（0待审核 1待执行 2进行中 3已完成 4已取消）';
```

#### 1.2 新建 hw_review（作业审核表）

```sql
DROP TABLE IF EXISTS hw_review;
CREATE TABLE hw_review (
  review_id       BIGINT(20)   NOT NULL AUTO_INCREMENT  COMMENT '审核ID',
  plan_id         BIGINT(20)   NOT NULL                  COMMENT '关联作业计划ID',
  plan_name       VARCHAR(200) DEFAULT ''                COMMENT '项目名称（冗余）',
  work_type       VARCHAR(20)  DEFAULT ''                COMMENT '作业类型（冗余）',
  construction_unit VARCHAR(200) DEFAULT ''              COMMENT '施工单位（冗余）',
  applicant       VARCHAR(64)  DEFAULT ''                COMMENT '申请人（计划创建者）',
  apply_time      DATETIME     DEFAULT NULL              COMMENT '申请时间',
  review_status   CHAR(1)      DEFAULT '0'               COMMENT '审核状态（0待审核 1已通过 2已驳回）',
  review_opinion  VARCHAR(500) DEFAULT ''                COMMENT '审核意见',
  reviewer        VARCHAR(64)  DEFAULT ''                COMMENT '审核人',
  review_time     DATETIME     DEFAULT NULL              COMMENT '审核时间',
  create_by       VARCHAR(64)  DEFAULT ''                COMMENT '创建者',
  create_time     DATETIME                               COMMENT '创建时间',
  update_by       VARCHAR(64)  DEFAULT ''                COMMENT '更新者',
  update_time     DATETIME                               COMMENT '更新时间',
  remark          VARCHAR(500) DEFAULT NULL              COMMENT '备注',
  PRIMARY KEY (review_id),
  INDEX idx_plan_id (plan_id),
  INDEX idx_review_status (review_status)
) ENGINE=InnoDB COMMENT='作业审核表';
```

#### 1.3 sys_menu 菜单数据

```sql
-- 1. 查询作业管理目录ID
SET @parent_id = (SELECT menu_id FROM sys_menu WHERE menu_name = '作业管理' AND parent_id = 0);

-- 2. 将原"作业打卡"的 order_num 从 2 改为 3（为新审核菜单让位）
UPDATE sys_menu SET order_num = 3 WHERE parent_id = @parent_id AND menu_name = '作业打卡';

-- 3. 插入二级菜单"作业审核"（order_num=2，排在作业计划和作业打卡之间）
INSERT INTO sys_menu VALUES
((SELECT MAX(menu_id)+1 FROM sys_menu m), '作业审核', @parent_id, '2', 'review', 'homework/review/index', '', 'HwReview', 1, 0, 'C', '0', '0', 'homework:review:list', 'edit', 'admin', SYSDATE(), '', NULL, '作业审核菜单');

-- 4. 按钮权限（审核查询 / 审核通过 / 审核驳回）
SET @review_id = (SELECT menu_id FROM sys_menu WHERE perms = 'homework:review:list');
INSERT INTO sys_menu VALUES
(@review_id+1, '审核查询', @review_id, '1', '#', '', '', '', 1, 0, 'F', '0', '0', 'homework:review:query',     '#', 'admin', SYSDATE(), '', NULL, ''),
(@review_id+2, '审核通过', @review_id, '2', '#', '', '', '', 1, 0, 'F', '0', '0', 'homework:review:approve',   '#', 'admin', SYSDATE(), '', NULL, ''),
(@review_id+3, '审核驳回', @review_id, '3', '#', '', '', '', 1, 0, 'F', '0', '0', 'homework:review:reject',    '#', 'admin', SYSDATE(), '', NULL, '');
```

---

### 第二部分：后端 — 新增文件（模块 D：作业审核 HwReview）

#### 2.1 HwReview.java — Domain

路径：`ruoyi-system/src/main/java/com/ruoyi/system/domain/HwReview.java`

- 继承 `BaseEntity`
- 字段：

| 字段 | 类型 | 说明 |
|------|------|------|
| reviewId | Long | 审核ID（主键） |
| planId | Long | 关联作业计划ID（getter 上加 `@NotNull "作业计划ID不能为空"`） |
| planName | String | 项目名称（冗余） |
| workType | String | 作业类型（冗余） |
| constructionUnit | String | 施工单位（冗余） |
| applicant | String | 申请人 |
| applyTime | Date | 申请时间（`@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")`） |
| reviewStatus | String | 审核状态（0待审核 1已通过 2已驳回） |
| reviewOpinion | String | 审核意见 |
| reviewer | String | 审核人 |
| reviewTime | Date | 审核时间（`@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")`） |

- `toString()` 使用 `ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)`

#### 2.2 HwReviewMapper.java — Mapper 接口

路径：`ruoyi-system/src/main/java/com/ruoyi/system/mapper/HwReviewMapper.java`

纯接口，7 个方法：

```java
public interface HwReviewMapper {
    List<HwReview> selectHwReviewList(HwReview hwReview);
    HwReview selectHwReviewById(Long reviewId);
    HwReview selectHwReviewByPlanId(Long planId);
    int insertHwReview(HwReview hwReview);
    int updateHwReview(HwReview hwReview);
    int deleteHwReviewById(Long reviewId);
    int deleteHwReviewByIds(Long[] reviewIds);
}
```

#### 2.3 HwReviewMapper.xml — MyBatis XML

路径：`ruoyi-system/src/main/resources/mapper/system/HwReviewMapper.xml`

- namespace 指向 `com.ruoyi.system.mapper.HwReviewMapper`
- resultMap 映射所有字段（下划线→驼峰）
- `selectHwReviewList`：条件查询 `plan_name LIKE`, `review_status =`, `applicant =`, `apply_time BETWEEN`，参数类型 HwReview，按 `apply_time DESC` 排序
- `selectHwReviewByPlanId`：`WHERE plan_id = #{planId}`
- `insertHwReview`：插入所有字段，`create_time = sysdate()`
- `updateHwReview`：更新 `review_status, review_opinion, reviewer, review_time, update_by, update_time = sysdate()`，WHERE `review_id`

#### 2.4 IHwReviewService.java — Service 接口

路径：`ruoyi-system/src/main/java/com/ruoyi/system/service/IHwReviewService.java`

```java
public interface IHwReviewService {
    List<HwReview> selectHwReviewList(HwReview hwReview);
    HwReview selectHwReviewById(Long reviewId);
    HwReview selectHwReviewByPlanId(Long planId);
    int insertHwReview(HwReview hwReview);
    int updateHwReview(HwReview hwReview);
    int deleteHwReviewById(Long reviewId);
    int deleteHwReviewByIds(Long[] reviewIds);
    int approve(Long reviewId, String opinion);
    int reject(Long reviewId, String opinion);
}
```

#### 2.5 HwReviewServiceImpl.java — Service 实现

路径：`ruoyi-system/src/main/java/com/ruoyi/system/service/impl/HwReviewServiceImpl.java`

- `@Service`，注入 `HwReviewMapper` 和 `HwPlanMapper`
- 标准 CRUD 方法委托给 Mapper
- insert 时调用 `setCreateBy(SecurityUtils.getUsername())`
- update 时调用 `setUpdateBy(SecurityUtils.getUsername())`

**核心方法 — `approve(Long reviewId, String opinion)`**：

```java
@Override
public int approve(Long reviewId, String opinion) {
    HwReview review = hwReviewMapper.selectHwReviewById(reviewId);
    if (review == null) {
        throw new ServiceException("审核记录不存在");
    }
    if (!"0".equals(review.getReviewStatus())) {
        throw new ServiceException("该记录已审核，请勿重复操作");
    }
    // 更新审核记录
    review.setReviewStatus("1");  // 已通过
    review.setReviewOpinion(opinion);
    review.setReviewer(SecurityUtils.getUsername());
    review.setReviewTime(new Date());
    review.setUpdateBy(SecurityUtils.getUsername());
    hwReviewMapper.updateHwReview(review);
    // 更新作业计划状态：待审核(0) → 待执行(1)
    HwPlan plan = hwPlanMapper.selectHwPlanById(review.getPlanId());
    if (plan == null) {
        throw new ServiceException("关联的作业计划不存在");
    }
    plan.setStatus("1");
    plan.setUpdateBy(SecurityUtils.getUsername());
    return hwPlanMapper.updateHwPlan(plan);
}
```

**核心方法 — `reject(Long reviewId, String opinion)`**：

```java
@Override
public int reject(Long reviewId, String opinion) {
    HwReview review = hwReviewMapper.selectHwReviewById(reviewId);
    if (review == null) {
        throw new ServiceException("审核记录不存在");
    }
    if (!"0".equals(review.getReviewStatus())) {
        throw new ServiceException("该记录已审核，请勿重复操作");
    }
    // 更新审核记录
    review.setReviewStatus("2");  // 已驳回
    review.setReviewOpinion(opinion);
    review.setReviewer(SecurityUtils.getUsername());
    review.setReviewTime(new Date());
    review.setUpdateBy(SecurityUtils.getUsername());
    hwReviewMapper.updateHwReview(review);
    // 更新作业计划状态：待审核(0) → 已取消(4)
    HwPlan plan = hwPlanMapper.selectHwPlanById(review.getPlanId());
    if (plan == null) {
        throw new ServiceException("关联的作业计划不存在");
    }
    plan.setStatus("4");
    plan.setUpdateBy(SecurityUtils.getUsername());
    return hwPlanMapper.updateHwPlan(plan);
}
```

#### 2.6 HwReviewController.java — Controller

路径：`ruoyi-admin/src/main/java/com/ruoyi/web/controller/homework/HwReviewController.java`

- `@RestController`，`@RequestMapping("/homework/review")`
- 继承 `BaseController`，注入 `IHwReviewService`
- `GET /list` → `@PreAuthorize("@ss.hasPermi('homework:review:list')")` + `startPage()` + `getDataTable()`
- `GET /{reviewId}` → `@PreAuthorize("@ss.hasPermi('homework:review:query')")` + `success()`
- `PUT /approve` → `@PreAuthorize("@ss.hasPermi('homework:review:approve')")` + `@Log(title="作业审核", businessType=UPDATE)` + try-catch ServiceException → `error(e.getMessage())`，请求体包含 `reviewId` 和 `reviewOpinion`
- `PUT /reject` → `@PreAuthorize("@ss.hasPermi('homework:review:reject')")` + `@Log(title="作业审核", businessType=UPDATE)` + try-catch ServiceException → `error(e.getMessage())`，请求体包含 `reviewId` 和 `reviewOpinion`
- `DELETE /{reviewIds}` → `@PreAuthorize("@ss.hasPermi('homework:review:reject')")` + `@Log(title="作业审核", businessType=DELETE)` + `toAjax()`

---

### 第三部分：后端 — 修改现有文件

#### 3.1 HwPlan.java — 修改 status 字段注释

路径：`ruoyi-system/src/main/java/com/ruoyi/system/domain/HwPlan.java`

```java
/** 状态（0待审核 1待执行 2进行中 3已完成 4已取消） */
private String status;
```

#### 3.2 IHwPlanService.java — 新增方法签名

```java
/**
 * 变更作业计划状态（含流转规则校验）
 * @param planId 计划ID
 * @param status 目标状态（0待审核 1待执行 2进行中 3已完成 4已取消）
 */
public int changeStatus(Long planId, String status);
```

#### 3.3 HwPlanServiceImpl.java — 修改 insertHwPlan + changeStatus

**修改 `insertHwPlan`（插入后自动创建审核记录）**：

注入新增依赖：
```java
@Autowired
private HwReviewMapper hwReviewMapper;
```

修改 insert 方法：
```java
@Override
public int insertHwPlan(HwPlan hwPlan) {
    hwPlan.setCreateBy(SecurityUtils.getUsername());
    hwPlan.setStatus("0");  // 初始状态：待审核
    int rows = hwPlanMapper.insertHwPlan(hwPlan);
    // 自动创建审核记录
    HwReview review = new HwReview();
    review.setPlanId(hwPlan.getPlanId());
    review.setPlanName(hwPlan.getProjectName());
    review.setWorkType(hwPlan.getWorkType());
    review.setConstructionUnit(hwPlan.getConstructionUnit());
    review.setApplicant(hwPlan.getCreateBy());
    review.setApplyTime(new Date());
    review.setReviewStatus("0");
    review.setCreateBy(hwPlan.getCreateBy());
    hwReviewMapper.insertHwReview(review);
    return rows;
}
```

**修改 `changeStatus`（适配5状态流转规则）**：

```java
@Override
public int changeStatus(Long planId, String status) {
    HwPlan plan = hwPlanMapper.selectHwPlanById(planId);
    if (plan == null) {
        throw new ServiceException("作业计划不存在");
    }
    String current = plan.getStatus();
    // 已完成不可再变更
    if ("3".equals(current)) {
        throw new ServiceException("已完成的计划不可变更状态");
    }
    // 已取消不可再变更（除管理员恢复操作外）
    if ("4".equals(current) && !"0".equals(status)) {
        throw new ServiceException("已取消的计划不可变更状态");
    }
    // 流转规则校验
    switch (status) {
        case "1":  // 待执行 ← 仅可从待审核转入（审核通过由 approve 方法触发，此处校验）
            if (!"0".equals(current)) {
                throw new ServiceException("只有待审核的计划才能标记为待执行");
            }
            break;
        case "2":  // 进行中 ← 仅可从待执行转入
            if (!"1".equals(current)) {
                throw new ServiceException("只有待执行的计划才能开始作业");
            }
            break;
        case "3":  // 已完成 ← 仅可从进行中转入
            if (!"2".equals(current)) {
                throw new ServiceException("只有进行中的计划才能标记完成");
            }
            break;
        case "4":  // 已取消 ← 可从待审核/待执行/进行中转入
            if ("3".equals(current) || "4".equals(current)) {
                throw new ServiceException("当前状态不可取消");
            }
            break;
        case "0":  // 恢复为待审核 ← 仅管理员可用，从已取消恢复
            if (!"4".equals(current)) {
                throw new ServiceException("只有已取消的计划才能恢复为待审核");
            }
            break;
        default:
            throw new ServiceException("无效的状态值");
    }
    plan.setStatus(status);
    plan.setUpdateBy(SecurityUtils.getUsername());
    return hwPlanMapper.updateHwPlan(plan);
}
```

#### 3.4 HwPlanController.java — 修改 add 方法增加创建者权限校验

注入新增依赖：
```java
@Autowired
private TbWorkerMapper tbWorkerMapper;
@Autowired
private TbWorkerRoleRelMapper tbWorkerRoleRelMapper;
```

修改 `add` 方法，在插入前校验创建者身份：

```java
@PreAuthorize("@ss.hasPermi('homework:plan:add')")
@Log(title = "作业计划", businessType = BusinessType.INSERT)
@PostMapping
public AjaxResult add(@Validated @RequestBody HwPlan hwPlan) {
    try {
        // 校验创建者权限：必须是施工方（非普通施工人员）或系统管理员
        validatePlanCreator();
        return toAjax(hwPlanService.insertHwPlan(hwPlan));
    } catch (ServiceException e) {
        return error(e.getMessage());
    }
}
```

新增私有方法 `validatePlanCreator()`：

```java
/**
 * 校验作业计划创建者权限
 * 允许：系统管理员 或 施工方人员（unit_type='3' 且角色不是施工人员 role_type!='9'）
 */
private void validatePlanCreator() {
    // 系统管理员直接放行
    if (SecurityUtils.isAdmin(SecurityUtils.getUserId())) {
        return;
    }
    // 查询当前用户对应的人员信息
    String userName = SecurityUtils.getUsername();
    TbWorker worker = tbWorkerMapper.selectTbWorkerByPhone(userName);
    if (worker == null) {
        // 尝试按姓名查询
        List<TbWorker> workers = tbWorkerMapper.selectTbWorkerList(new TbWorker());
        worker = workers.stream()
            .filter(w -> userName.equals(w.getWorkerName()))
            .findFirst().orElse(null);
    }
    if (worker == null) {
        throw new ServiceException("未找到您的人员信息，无法创建作业计划");
    }
    if (!"3".equals(worker.getUnitType())) {
        throw new ServiceException("仅施工方人员可创建作业计划");
    }
    // 查询人员角色，排除普通施工人员(role_type='9')
    List<TbWorkerRoleRel> roles = tbWorkerRoleRelMapper.selectByWorkerId(worker.getId());
    boolean isRegularWorker = roles.stream()
        .allMatch(r -> "9".equals(r.getRoleCode()));
    if (isRegularWorker || roles.isEmpty()) {
        throw new ServiceException("普通施工人员无权创建作业计划，请联系施工方管理人员");
    }
}
```

> 注：如果 `TbWorkerMapper` 和 `TbWorkerRoleRelMapper` 缺少上述方法，需同步补齐。`selectTbWorkerByPhone` 在 Mapper 中新增 `WHERE phone = #{phone} AND del_flag = '0'`；`selectByWorkerId` 在 `TbWorkerRoleRelMapper` 中新增 `WHERE worker_id = #{workerId}`。

#### 3.5 HwAttendanceServiceImpl.java — checkIn/checkOut 增加计划状态校验

在 `checkIn` 方法的"校验作业计划存在且 status 正常"步骤中，将校验条件从 `status='0'` 改为：

```java
HwPlan plan = hwPlanMapper.selectHwPlanById(hwAttendance.getPlanId());
if (plan == null) {
    throw new ServiceException("作业计划不存在");
}
// 只有待执行(1)或进行中(2)的计划才能打卡
if (!"1".equals(plan.getStatus()) && !"2".equals(plan.getStatus())) {
    if ("0".equals(plan.getStatus())) {
        throw new ServiceException("该计划尚未通过审核，无法打卡");
    } else if ("4".equals(plan.getStatus())) {
        throw new ServiceException("该计划已取消，无法打卡");
    } else if ("3".equals(plan.getStatus())) {
        throw new ServiceException("该计划已完成，无法打卡");
    }
}

// 首次进场打卡时，将计划状态从 待执行(1) 自动切换为 进行中(2)
if ("0".equals(hwAttendance.getCheckType()) && "1".equals(plan.getStatus())) {
    plan.setStatus("2");
    plan.setUpdateBy("SYSTEM");
    hwPlanMapper.updateHwPlan(plan);
}
```

`checkOut` 方法同理增加状态校验（复用相同逻辑，仅去掉自动切换状态的代码）。

#### 3.6 TbWorkerMapper.java — 新增方法（如不存在）

```java
TbWorker selectTbWorkerByPhone(@Param("phone") String phone);
```

#### 3.7 TbWorkerMapper.xml — 新增 SQL（如不存在）

```xml
<select id="selectTbWorkerByPhone" parameterType="string" resultMap="TbWorkerResult">
    select * from tb_worker where phone = #{phone} and del_flag = '0' limit 1
</select>
```

#### 3.8 TbWorkerRoleRelMapper.java — 新增方法（如不存在）

```java
List<TbWorkerRoleRel> selectByWorkerId(@Param("workerId") Long workerId);
```

#### 3.9 TbWorkerRoleRelMapper.xml — 新增 SQL（如不存在）

```xml
<select id="selectByWorkerId" parameterType="long" resultMap="TbWorkerRoleRelResult">
    select * from tb_worker_role_rel where worker_id = #{workerId}
</select>
```

---

### 第四部分：前端 — 新增文件

#### 4.1 API 模块 `api/homework/review.js`

路径：`ruoyi-ui/src/api/homework/review.js`

```js
import request from '@/utils/request'

export function listReview(query) {
  return request({ url: '/homework/review/list', method: 'get', params: query })
}

export function getReview(reviewId) {
  return request({ url: '/homework/review/' + reviewId, method: 'get' })
}

export function approveReview(data) {
  return request({ url: '/homework/review/approve', method: 'put', data: data })
}

export function rejectReview(data) {
  return request({ url: '/homework/review/reject', method: 'put', data: data })
}

export function delReview(reviewIds) {
  return request({ url: '/homework/review/' + reviewIds, method: 'delete' })
}
```

#### 4.2 Vue 页面 `views/homework/review/index.vue`

路径：`ruoyi-ui/src/views/homework/review/index.vue`

参照 `src/views/system/post/index.vue` 的标准列表页模式，功能说明：

**搜索栏**：
- 项目名称（input）
- 审核状态（select：待审核/已通过/已驳回）
- 申请人（input）
- 申请时间范围（date-range）

**表格列**：
- reviewId（审核ID）
- planName（项目名称，可点击跳转作业计划详情）
- workType（作业类型，字典标签）
- constructionUnit（施工单位）
- applicant（申请人）
- applyTime（申请时间，格式化）
- reviewStatus（审核状态，el-tag 颜色标签：待审核=info、已通过=success、已驳回=danger）
- reviewer（审核人）
- reviewTime（审核时间）
- reviewOpinion（审核意见，tooltip 展示）
- 操作列：审核通过(v-hasPermi="['homework:review:approve']"，仅待审核状态显示)、审核驳回(v-hasPermi="['homework:review:reject']"，仅待审核状态显示)、删除(v-hasPermi="['homework:review:reject']")

**审核弹窗**（el-dialog，title="作业审核"）：
- 显示计划关键信息（只读）：项目名称、作业类型、施工单位、作业地点、计划作业时间、参与人员、作业内容
- 审核意见（textarea，`v-model="reviewForm.reviewOpinion"`）
- 底部按钮：通过(type="success")、驳回(type="danger")、取消

**data 结构**：
```javascript
data() {
  return {
    loading: false,
    ids: [],
    single: true,
    multiple: true,
    total: 0,
    list: [],
    title: '',
    open: false,
    queryParams: {
      pageNum: 1,
      pageSize: 10,
      planName: undefined,
      reviewStatus: undefined,
      applicant: undefined,
      applyTimeStart: undefined,
      applyTimeEnd: undefined
    },
    reviewForm: {
      reviewId: undefined,
      reviewOpinion: ''
    },
    currentPlan: {}  // 被审核的计划详情
  }
}
```

**标准 methods**：
- `getList()`, `handleQuery()`, `resetQuery()`
- `handleApprove(row)` → 打开审核弹窗，加载计划详情，设置 `reviewForm.reviewId = row.reviewId`
- `handleReject(row)` → 同上
- `submitApprove()` → `approveReview({ reviewId, reviewOpinion })` → 关闭弹窗 → `getList()` → `$modal.msgSuccess('审核通过')`
- `submitReject()` → `rejectReview({ reviewId, reviewOpinion })` → 关闭弹窗 → `getList()` → `$modal.msgSuccess('已驳回')`
- `handleDelete(row)` → `$modal.confirm` → `delReview(row.reviewId)` → `getList()`
- `handleSelectionChange()`, `cancel()`, `reset()`

---

### 第五部分：前端 — 修改现有文件

#### 5.1 `views/homework/plan/index.vue` — 状态列 + 操作列调整

**status 数据更新**（替换原有的 4 状态为 5 状态）：

```javascript
statusOptions: [
  { label: '待审核', value: '0', type: 'warning' },
  { label: '待执行', value: '1', type: 'info' },
  { label: '进行中', value: '2', type: 'primary' },
  { label: '已完成', value: '3', type: 'success' },
  { label: '已取消', value: '4', type: 'danger' }
]
```

**表格 status 列**（支持 5 种状态标签）：

```html
<el-table-column label="状态" align="center" prop="status" width="100">
  <template slot-scope="scope">
    <el-tag :type="['warning','info','primary','success','danger'][Number(scope.row.status)]">
      {{ ['待审核','待执行','进行中','已完成','已取消'][Number(scope.row.status)] }}
    </el-tag>
  </template>
</el-table-column>
```

**操作列状态变更按钮**（适配 5 状态流转）：

```html
<el-dropdown v-if="scope.row.status !== '3'"
  @command="(cmd) => handleStatusChange(scope.row, cmd)" style="margin-right:5px">
  <el-button type="warning" size="mini">
    状态<i class="el-icon-arrow-down el-icon--right"></i>
  </el-button>
  <el-dropdown-menu slot="dropdown">
    <el-dropdown-item v-if="scope.row.status === '1'" command="2">开始作业</el-dropdown-item>
    <el-dropdown-item v-if="scope.row.status === '2'" command="3">标记完成</el-dropdown-item>
    <el-dropdown-item v-if="['0','1','2'].includes(scope.row.status)" command="4">取消计划</el-dropdown-item>
    <el-dropdown-item v-if="scope.row.status === '4'" command="0">恢复为待审核</el-dropdown-item>
  </el-dropdown-menu>
</el-dropdown>
```

**操作列增加"审核状态"查看**（显示关联的审核记录状态）：

```html
<el-button type="info" size="mini" icon="el-icon-document"
  @click="handleViewReview(scope.row)">审核</el-button>
```

methods 新增：
```javascript
handleViewReview(row) {
  this.$router.push({ path: '/homework/review', query: { planId: row.planId } });
}
```

#### 5.2 `views/homework/attendance/index.vue` — 增加审核状态提示

**created 中增加计划状态校验提示**：
- 在选择计划的下拉框中，仅加载状态为"待执行(1)"和"进行中(2)"的计划
- 或在 el-alert 中增加提示文字："仅审核通过的作业计划可进行打卡"

**加载计划列表时过滤状态**：
```javascript
loadPlanOptions() {
  listPlan({ pageNum: 1, pageSize: 999 }).then(r => {
    // 仅显示待执行(1)和进行中(2)的计划
    this.planOptions = (r.rows || []).filter(p => p.status === '1' || p.status === '2');
  });
}
```

#### 5.3 `api/homework/plan.js` — 无需修改（changeStatus 复用 updatePlan）

现有的 `updatePlan` 函数直接支持 `changeStatus` 接口调用（传 `{ planId, status }`）。

---

## 四、验证清单

所有文件创建完成后，执行以下验证：

1. **编译后端**：`mvn compile -DskipTests`，确认无编译错误
2. **启动后端**：`cd ruoyi-admin && mvn spring-boot:run`，确认启动成功
3. **启动前端**：`cd ruoyi-ui && npm run dev`
4. **菜单验证**：登录 admin/admin123，检查侧边栏"作业管理"下出现三个子菜单：**作业计划**、**作业审核**、**作业打卡**，顺序正确
5. **作业计划创建**：新增计划 → 确认初始状态为"待审核"（status='0'）
6. **审核记录自动生成**：新增计划后 → 切换到作业审核页面 → 确认新审核记录出现，状态为"待审核"
7. **创建者权限校验**：使用普通施工人员身份登录 → 创建计划 → 应提示无权创建
8. **审核通过**：使用作业批准人或管理员身份 → 审核页面 → 点击"审核通过" → 填写意见 → 确认计划状态变为"待执行"
9. **审核驳回**：对另一条待审核记录点"审核驳回" → 填写驳回原因 → 确认计划状态变为"已取消"
10. **重复审核拦截**：对已审核的记录再次点通过/驳回 → 应提示"该记录已审核"
11. **待审核计划打卡拦截**：选择一个待审核状态(0)的计划进行进场打卡 → 应提示"该计划尚未通过审核，无法打卡"
12. **待执行计划打卡**：选择一个待执行状态(1)的计划 → 进场打卡成功 → 确认计划状态自动变为"进行中"(2)
13. **状态流转**：计划页操作列 → 验证各状态按钮显示/隐藏逻辑正确
14. **权限验证**：退出 admin，登录 ry/123456 → 确认审核按钮受权限控制
15. **数据库确认**：查询 `hw_review` 表 → 确认审核记录数据完整

---

## 五、总文件改动清单

### 新增文件（共 7 个）

| # | 文件 | 类型 | 说明 |
|---|------|------|------|
| 1 | `HwReview.java` | Domain | 作业审核实体 |
| 2 | `HwReviewMapper.java` | Mapper 接口 | |
| 3 | `HwReviewMapper.xml` | Mapper XML | |
| 4 | `IHwReviewService.java` | Service 接口 | |
| 5 | `HwReviewServiceImpl.java` | Service 实现 | 含 approve/reject 核心逻辑 |
| 6 | `HwReviewController.java` | Controller | `/homework/review/**` |
| 7 | `ruoyi-ui/src/api/homework/review.js` | 前端 API | |
| 8 | `ruoyi-ui/src/views/homework/review/index.vue` | 前端页面 | 审核列表 + 审核弹窗 |

### 修改文件（共 11 个）

| # | 文件 | 改动内容 |
|---|------|---------|
| 1 | `HwPlan.java` | status 字段注释改为 5 状态 |
| 2 | `IHwPlanService.java` | 新增 `changeStatus()` 方法签名 |
| 3 | `HwPlanServiceImpl.java` | 插入 `HwReviewMapper`；修改 insertHwPlan（自动创建审核记录）；新增 changeStatus（5 状态流转规则） |
| 4 | `HwPlanController.java` | 注入 TbWorkerMapper + TbWorkerRoleRelMapper；新增 `validatePlanCreator()` 校验创建者权限 |
| 5 | `HwAttendanceServiceImpl.java` | checkIn/checkOut 增加计划状态校验（仅待执行/进行中可打卡）；首次进场自动切换状态 |
| 6 | `TbWorkerMapper.java` | 新增 `selectTbWorkerByPhone()` |
| 7 | `TbWorkerMapper.xml` | 新增对应 SQL |
| 8 | `TbWorkerRoleRelMapper.java` | 新增 `selectByWorkerId()` |
| 9 | `TbWorkerRoleRelMapper.xml` | 新增对应 SQL |
| 10 | `plan/index.vue` | status 更新为 5 状态标签 + 状态按钮适配 + 新增"审核"按钮 |
| 11 | `attendance/index.vue` | 计划选择器过滤 + 增加审核状态提示 |

### 数据库变更

| 操作 | 内容 |
|------|------|
| ALTER | `hw_plan.status` 注释改为 5 状态（0待审核 1待执行 2进行中 3已完成 4已取消） |
| 新增表 | `hw_review`（作业审核表，含 plan_id 索引） |
| 新增菜单 | 1 个二级菜单（作业审核）+ 3 条按钮权限 |
| 修改菜单 | 作业打卡 order_num 从 2 改为 3 |

---

## 六、模块架构

```
作业管理 (homework)
├── 作业计划 (plan)       — HwPlanController      /homework/plan/**
├── 作业审核 (review)     — HwReviewController    /homework/review/**    ← 新增
└── 作业打卡 (attendance) — HwAttendanceController /homework/attendance/**
```

**数据关系**：

```
hw_plan ──────┬── hw_review（plan_id → hw_plan.plan_id，审核记录）  ← 新增
               ├── hw_attendance（plan_id → hw_plan.plan_id）
               ├── hw_plan_worker（plan_id → hw_plan.plan_id，worker_id → tb_worker.id）
               └── hw_plan_video（plan_id → hw_plan.plan_id）
```

**状态流转驱动关系**：

```
作业计划创建 ──自动生成──→ 作业审核记录
                              ↓
                    作业批准人/管理员审核
                        ↓           ↓
                     审核通过      审核驳回
                        ↓           ↓
                计划→待执行(1)  计划→已取消(4)
                        ↓
                  进场打卡触发
                        ↓
                计划→进行中(2)
                        ↓
                  标记完成
                        ↓
                计划→已完成(3)
```

---

## 七、完成后生成开发日志

所有任务完成并验证通过后，在 `文档/` 目录生成 `开发日志_secondHomework.md` 文件，记录本次开发的完整信息：

```
# 作业管理模块 V3 开发日志 — 新增作业审核

## 基本信息
- 开发日期：YYYY-MM-DD
- 模块名称：作业管理（homework）V3 — 作业审核
- 开发者：AI 自动生成

## 新增文件

### 数据库
- sql/ry_20260417.sql（末尾追加）— 1 张建表 + sys_menu 菜单数据

### 后端 Java（共 N 个文件）
- ruoyi-system/src/main/java/com/ruoyi/system/domain/HwReview.java — 作业审核实体
- ruoyi-system/src/main/java/com/ruoyi/system/mapper/HwReviewMapper.java — 作业审核Mapper
- ruoyi-system/src/main/resources/mapper/system/HwReviewMapper.xml
- ruoyi-system/src/main/java/com/ruoyi/system/service/IHwReviewService.java
- ruoyi-system/src/main/java/com/ruoyi/system/service/impl/HwReviewServiceImpl.java
- ruoyi-admin/src/main/java/com/ruoyi/web/controller/homework/HwReviewController.java

### 前端（共 2 个文件）
- ruoyi-ui/src/api/homework/review.js — 作业审核 API
- ruoyi-ui/src/views/homework/review/index.vue — 作业审核页面

## 修改文件（共 N 个）
- [列出所有被修改的文件及改动内容摘要]

## 数据库变更
- ALTER：hw_plan.status 注释改为 5 状态
- 新增表：hw_review（作业审核表）
- sys_menu：新增 1 个二级菜单（作业审核）+ 3 条按钮权限
- sys_menu：更新作业打卡 order_num 2→3

## 模块架构
作业管理 (homework)
├── 作业计划 (plan)       — HwPlanController      /homework/plan/**
├── 作业审核 (review)     — HwReviewController    /homework/review/**      ← 新增
└── 作业打卡 (attendance) — HwAttendanceController /homework/attendance/**
```

> **要求**：生成时请把 `N` 替换为实际文件数量，把 `YYYY-MM-DD` 替换为实际日期，确保所有路径与实际创建的文件一致。
