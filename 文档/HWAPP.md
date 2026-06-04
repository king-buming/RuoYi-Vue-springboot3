# 作业管理模块 — uni-app 移动端（小程序/H5）规格书

> **请自动完成本文档中的所有开发任务，无需逐步确认。** 按照以下规格创建所有文件、代码后，报告完成情况。

---

## 一、项目信息

| 项目 | RuoYi-App uni-app 移动端 |
|------|------|
| 路径 | `C:\Users\ASUS\Desktop\RuoYi-Vue-springboot3-main\RuoYi-App` |
| 后端路径 | `C:\Users\ASUS\Desktop\RuoYi-Vue-springboot3-main\RuoYi-Vue-springboot3` |
| 框架 | uni-app (Vue 2.6.14) + uni-ui 1.5.0 |
| 后端 | Spring Boot 3.5.x + MyBatis + Spring Security 6 |
| 数据库 | MySQL 8.x，库名 `ry-vue`（表已存在，无需新增） |
| 认证 | HMAC-SHA256 AppToken（AppTokenFilter + AppTokenUtil），7天有效期，独立于 Spring Security JWT |

**关键约定（务必遵守）**：
- 所有 uni-app 代码遵循现有 `pages/worker/*` 页面的代码风格（参考 `checkin.vue`、`records.vue`）
- 使用 `config.js` 的 `baseUrl` + `uni.request` 发起请求，手动构建 `authHeader()`
- 页面间导航使用 `uni.navigateTo` / `uni.redirectTo` / `uni.reLaunch` / `uni.switchTab`
- 后端新增 APP 接口统一放在 `controller/app/` 包下，路径前缀 `/app/homework/*`
- APP 接口继承 `BaseController`，使用 AppTokenFilter 鉴权（通过 `AppTokenUtil.getWorkerId(request)` 获取当前工人ID）
- 响应格式统一为 `AjaxResult {code, msg, data}`，分页为 `TableDataInfo {code, msg, rows, total}`
- 表 `hw_plan`、`hw_review` 已存在，**无需创建新表**

---

## 二、需求概述

### 2.1 背景

当前 RuoYi-App 移动端仅实现了工人打卡（签到/签退）和人员信息查看功能。后台管理系统中已有的**作业计划**和**作业审核**模块（详见 `secondHomework.md` V3 版本）尚未在移动端实现。

本次需求：在 uni-app 移动端的工作台上添加**作业计划**和**作业审核**功能，让施工方管理人员和作业批准人可以在手机端完成计划创建和审核操作。

### 2.2 业务流程（与后台一致）

```
作业计划（创建） → 作业审核（人工审核） → 作业打卡（进场/离场）
```

- 作业计划创建后，状态为"待审核(0)"，系统自动生成审核记录
- 作业批准人（或管理员）在移动端审核通过 → 计划状态变为"待执行(1)" → 可进入打卡
- 审核驳回 → 计划状态变为"已取消(4)"

### 2.3 角色与权限（移动端）

| 操作 | 允许角色 | 校验方式 |
|------|---------|---------|
| 查看作业计划列表 | 所有已登录工人 | AppToken 验证通过即可 |
| 创建作业计划 | 施工方人员（unit_type='3'，非普通施工人员）+ 系统管理员 | Controller 中校验 worker 的 unit_type 和 role_code |
| 查看审核列表 | 作业批准人 + 系统管理员 | Controller 中校验 worker 的 role_code |
| 审核通过/驳回 | 作业批准人 + 系统管理员 | Controller 中校验 worker 的 role_code |

### 2.4 底部 Tab 栏变更

**当前**：

| Tab | 页面 | 文字 |
|-----|------|------|
| 1 | pages/worker/checkin | 打卡 |
| 2 | pages/worker/records | 记录 |
| 3 | pages/worker/mine | 我的 |

**变更后**：

| Tab | 页面 | 文字 | 说明 |
|-----|------|------|------|
| 1 | pages/worker/checkin | 打卡 | 不变 |
| 2 | pages/worker/workbench | **工作台** | 原"记录"改名，内容改为工作台首页 |
| 3 | pages/worker/mine | 我的 | 不变 |

### 2.5 页面导航结构

```
工作台（Tab2，原"记录"）
├── 作业计划（宫格入口）
│   ├── 计划列表 → 计划详情
│   └── 新建计划 → 表单提交
├── 作业审核（宫格入口）
│   ├── 审核列表 → 审核详情
│   └── 审核操作（通过/驳回）
└── 打卡记录（宫格入口）→ 原 records.vue 内容
```

### 2.6 作业计划状态（5 状态，与后台一致）

| 状态值 | 状态名 | 移动端展示颜色 |
|--------|--------|:--:|
| 0 | 待审核 | 橙色 warning |
| 1 | 待执行 | 蓝色 info |
| 2 | 进行中 | 绿色 primary |
| 3 | 已完成 | 灰色 success |
| 4 | 已取消 | 红色 danger |

### 2.7 审核状态（3 状态）

| 状态值 | 状态名 | 移动端展示颜色 |
|--------|--------|:--:|
| 0 | 待审核 | 橙色 |
| 1 | 已通过 | 绿色 |
| 2 | 已驳回 | 红色 |

---

## 三、你需要创建/修改的全部内容

### 第一部分：后端 — 新增 APP 接口

> 注：`hw_plan`、`hw_review` 表及对应的 Domain/Mapper/Service 已在后台管理系统中存在，**无需创建或修改**。只需新增移动端专属 Controller。

#### 1.1 AppHomeworkController.java（新建）

路径：`ruoyi-admin/src/main/java/com/ruoyi/web/controller/app/AppHomeworkController.java`

`@RestController`，`@RequestMapping("/app/homework")`，继承 `BaseController`。

注入依赖：
```java
@Autowired
private IHwPlanService hwPlanService;
@Autowired
private IHwReviewService hwReviewService;
@Autowired
private HwPlanMapper hwPlanMapper;
@Autowired
private HwReviewMapper hwReviewMapper;
@Autowired
private TbWorkerMapper tbWorkerMapper;
@Autowired
private TbWorkerRoleRelMapper tbWorkerRoleRelMapper;
@Autowired
private HwPlanWorkerMapper hwPlanWorkerMapper;
```

**所有接口通过 `AppTokenUtil.getWorkerId(request)` 获取当前登录工人ID**，再查 `TbWorker` 获取人员信息。

**端点列表（8 个）**：

##### 1.1.1 GET /app/homework/plan/list — 我的计划列表

```java
@GetMapping("/plan/list")
public TableDataInfo planList(@RequestParam(defaultValue = "1") Integer pageNum,
                              @RequestParam(defaultValue = "10") Integer pageSize,
                              @RequestParam(required = false) String status) {
    Long workerId = AppTokenUtil.getWorkerId(ServletUtils.getRequest());
    // 分页查询：当前 worker 创建的计划 或 当前 worker 参与的计划
    startPage();
    HwPlan query = new HwPlan();
    if (StringUtils.isNotEmpty(status)) query.setStatus(status);
    // 查当前 worker 相关的计划（通过 hw_plan_worker 关联 或 createBy）
    List<HwPlan> list = hwPlanMapper.selectHwPlanListByWorkerId(workerId, status);
    return getDataTable(list);
}
```

> 需要在 `HwPlanMapper.java` 和 `HwPlanMapper.xml` 中新增 `selectHwPlanListByWorkerId` 方法（见第三部分）。

##### 1.1.2 GET /app/homework/plan/{planId} — 计划详情

```java
@GetMapping("/plan/{planId}")
public AjaxResult planDetail(@PathVariable Long planId) {
    HwPlan plan = hwPlanMapper.selectHwPlanById(planId);
    if (plan == null) return error("计划不存在");
    // 查询关联的参与人员
    List<HwPlanWorker> workers = hwPlanWorkerMapper.selectByPlanId(planId);
    Map<String, Object> result = new HashMap<>();
    result.put("plan", plan);
    result.put("workers", workers);
    return success(result);
}
```

##### 1.1.3 POST /app/homework/plan — 创建作业计划

```java
@PostMapping("/plan")
public AjaxResult createPlan(@Validated @RequestBody HwPlan plan) {
    try {
        Long workerId = AppTokenUtil.getWorkerId(ServletUtils.getRequest());
        TbWorker worker = tbWorkerMapper.selectTbWorkerById(workerId);
        if (worker == null) return error("未找到您的人员信息");
        // 校验创建者权限：施工方（unit_type='3'）且非普通施工人员
        if (!"3".equals(worker.getUnitType())) return error("仅施工方人员可创建作业计划");
        List<String> roleCodes = tbWorkerRoleRelMapper.selectRoleCodesByWorkerId(workerId);
        if (roleCodes.isEmpty() || (roleCodes.size() == 1 && "worker".equals(roleCodes.get(0))))
            return error("普通施工人员无权创建作业计划，请联系施工方管理人员");
        
        plan.setCreateBy(worker.getWorkerName());
        hwPlanService.insertHwPlan(plan);  // 自动创建审核记录 + 设置status='0'
        return success(plan.getPlanId());
    } catch (ServiceException e) {
        return error(e.getMessage());
    }
}
```

##### 1.1.4 GET /app/homework/review/list — 审核列表

```java
@GetMapping("/review/list")
public TableDataInfo reviewList(@RequestParam(defaultValue = "1") Integer pageNum,
                                @RequestParam(defaultValue = "10") Integer pageSize,
                                @RequestParam(required = false) String reviewStatus) {
    // 仅作业批准人或管理员可见审核列表
    Long workerId = AppTokenUtil.getWorkerId(ServletUtils.getRequest());
    List<String> roleCodes = tbWorkerRoleRelMapper.selectRoleCodesByWorkerId(workerId);
    boolean isApprover = roleCodes.contains("approver");
    if (!isApprover) return getDataTable(Collections.emptyList());  // 非批准人返回空列表
    
    startPage();
    HwReview query = new HwReview();
    if (StringUtils.isNotEmpty(reviewStatus)) query.setReviewStatus(reviewStatus);
    List<HwReview> list = hwReviewMapper.selectHwReviewList(query);
    return getDataTable(list);
}
```

##### 1.1.5 GET /app/homework/review/{reviewId} — 审核详情

```java
@GetMapping("/review/{reviewId}")
public AjaxResult reviewDetail(@PathVariable Long reviewId) {
    HwReview review = hwReviewMapper.selectHwReviewById(reviewId);
    if (review == null) return error("审核记录不存在");
    // 同时返回关联的计划详情和参与人员
    HwPlan plan = hwPlanMapper.selectHwPlanById(review.getPlanId());
    List<HwPlanWorker> workers = hwPlanWorkerMapper.selectByPlanId(review.getPlanId());
    Map<String, Object> result = new HashMap<>();
    result.put("review", review);
    result.put("plan", plan);
    result.put("workers", workers);
    return success(result);
}
```

##### 1.1.6 PUT /app/homework/review/approve — 审核通过

```java
@PutMapping("/review/approve")
public AjaxResult approveReview(@RequestBody Map<String, Object> body) {
    try {
        Long workerId = AppTokenUtil.getWorkerId(ServletUtils.getRequest());
        // 校验审核权限
        List<String> roleCodes = tbWorkerRoleRelMapper.selectRoleCodesByWorkerId(workerId);
        if (!roleCodes.contains("approver")) return error("无审核权限，仅作业批准人可审核");
        
        Long reviewId = Long.valueOf(body.get("reviewId").toString());
        String opinion = body.get("reviewOpinion") != null ? body.get("reviewOpinion").toString() : "";
        hwReviewService.approve(reviewId, opinion);
        return success();
    } catch (ServiceException e) {
        return error(e.getMessage());
    }
}
```

##### 1.1.7 PUT /app/homework/review/reject — 审核驳回

```java
@PutMapping("/review/reject")
public AjaxResult rejectReview(@RequestBody Map<String, Object> body) {
    try {
        Long workerId = AppTokenUtil.getWorkerId(ServletUtils.getRequest());
        List<String> roleCodes = tbWorkerRoleRelMapper.selectRoleCodesByWorkerId(workerId);
        if (!roleCodes.contains("approver")) return error("无审核权限，仅作业批准人可审核");
        
        Long reviewId = Long.valueOf(body.get("reviewId").toString());
        String opinion = body.get("reviewOpinion") != null ? body.get("reviewOpinion").toString() : "";
        hwReviewService.reject(reviewId, opinion);
        return success();
    } catch (ServiceException e) {
        return error(e.getMessage());
    }
}
```

##### 1.1.8 GET /app/homework/plan/{planId}/workers — 计划参与人员

```java
@GetMapping("/plan/{planId}/workers")
public AjaxResult planWorkers(@PathVariable Long planId) {
    List<HwPlanWorker> workers = hwPlanWorkerMapper.selectByPlanId(planId);
    return success(workers);
}
```

---

#### 1.2 HwPlanMapper.java — 新增方法（修改现有文件）

路径：`ruoyi-system/src/main/java/com/ruoyi/system/mapper/HwPlanMapper.java`

新增方法签名：
```java
/**
 * 按工人ID查询关联的作业计划（包含该工人创建的 + 参与的）
 */
List<HwPlan> selectHwPlanListByWorkerId(@Param("workerId") Long workerId,
                                         @Param("status") String status);
```

#### 1.3 HwPlanMapper.xml — 新增 SQL（修改现有文件）

路径：`ruoyi-system/src/main/resources/mapper/system/HwPlanMapper.xml`

新增 SQL：
```xml
<select id="selectHwPlanListByWorkerId" resultMap="HwPlanResult">
    select distinct p.* from hw_plan p
    left join hw_plan_worker pw on p.plan_id = pw.plan_id
    where (pw.worker_id = #{workerId}
           or p.create_by = (select worker_name from tb_worker where id = #{workerId}))
    <if test="status != null and status != ''">
        and p.status = #{status}
    </if>
    order by p.create_time desc
</select>
```

#### 1.4 HwPlanWorkerMapper.java — 新增方法（修改现有文件）

路径：`ruoyi-system/src/main/java/com/ruoyi/system/mapper/HwPlanWorkerMapper.java`

新增方法签名：
```java
/**
 * 按计划ID查询关联的参与人员列表
 */
List<HwPlanWorker> selectByPlanId(@Param("planId") Long planId);
```

#### 1.5 HwPlanWorkerMapper.xml — 新增 SQL（修改现有文件）

路径：`ruoyi-system/src/main/resources/mapper/system/HwPlanWorkerMapper.xml`

新增 SQL：
```xml
<select id="selectByPlanId" parameterType="Long" resultMap="HwPlanWorkerResult">
    select * from hw_plan_worker where plan_id = #{planId}
</select>
```

> 注：如 `selectByPlanId` 方法已存在，跳过此步。

---

### 第三部分：前端 — uni-app 页面

所有新建页面位于 `RuoYi-App/pages/worker/` 下。

#### 3.0 工作台页面 — 改造原 records.vue

原 `pages/worker/records.vue` → 改造为工作台首页。

路径：`RuoYi-App/pages/worker/workbench.vue`（新建），原 `records.vue` **不删除**（打卡记录页面保留，从工作台宫格跳转进入）。

**页面结构**：

```html
<template>
  <view class="container">
    <!-- 用户信息栏 -->
    <view class="user-bar">
      <view class="user-info">
        <text class="name">{{ workerName || '未登录' }}</text>
        <text class="role">{{ roleLabel }}</text>
      </view>
      <text class="audit-status">{{ auditLabel }}</text>
    </view>

    <!-- 功能宫格 -->
    <view class="grid-section">
      <view class="section-title">作业管理</view>
      <uni-grid :column="3" :showBorder="false" :square="false">
        <uni-grid-item v-for="(item, index) in menus" :key="index" @click="item.action">
          <view class="grid-item">
            <view class="grid-icon" :style="{ background: item.bgColor }">
              <uni-icons :type="item.icon" size="28" color="#fff"></uni-icons>
            </view>
            <text class="grid-text">{{ item.text }}</text>
          </view>
        </uni-grid-item>
      </uni-grid>
    </view>

    <!-- 施工快捷入口 -->
    <view class="grid-section" v-if="canCreatePlan">
      <view class="section-title">快捷操作</view>
      <view class="quick-actions">
        <view class="action-card" @click="goCreatePlan">
          <uni-icons type="plus-filled" size="24" color="#007aff"></uni-icons>
          <text class="action-text">新建作业计划</text>
          <text class="action-desc">创建新的施工作业计划单</text>
        </view>
        <view class="action-card" @click="goReviewList" v-if="isApprover">
          <uni-icons type="checkmarkempty" size="24" color="#34c759"></uni-icons>
          <text class="action-text">待审核计划</text>
          <text class="action-desc">审核施工方提交的作业计划</text>
        </view>
      </view>
    </view>
  </view>
</template>
```

**data 结构**：
```javascript
data() {
  return {
    workerName: '',
    workerRole: '',
    auditStatus: '',
    roleCodes: [],
    menus: [
      { text: '作业计划', icon: 'paperplane-filled', bgColor: '#007aff', action: () => { uni.navigateTo({ url: '/pages/worker/plan/list' }) } },
      { text: '作业审核', icon: 'checkmarkempty', bgColor: '#34c759', action: () => { uni.navigateTo({ url: '/pages/worker/review/list' }) } },
      { text: '打卡记录', icon: 'list', bgColor: '#ff9500', action: () => { uni.navigateTo({ url: '/pages/worker/records' }) } }
    ]
  }
}
```

**computed**：
```javascript
computed: {
  auditLabel() {
    const m = { '0': '待审核', '1': '已通过', '2': '已驳回' }
    return m[this.auditStatus] || '未认证'
  },
  roleLabel() {
    return this.workerRole || ''
  },
  canCreatePlan() {
    // 施工方(unit_type='3')且非普通施工人员(role_code不只有worker)
    return this.roleCodes.length > 0 && !(this.roleCodes.length === 1 && this.roleCodes[0] === 'worker')
  },
  isApprover() {
    return this.roleCodes.includes('approver')
  }
}
```

**methods**：
- `onShow()` → 检查 appToken，调 `/app/auth/me` 获取工人信息，调 `/app/worker/profile` 获取角色列表
- `goCreatePlan()` → `uni.navigateTo({ url: '/pages/worker/plan/create' })`
- `goReviewList()` → `uni.navigateTo({ url: '/pages/worker/review/list' })`
- `emptyAction()` → `uni.showToast({ title: '功能开发中', icon: 'none' })`

**样式**：参考现有 checkin.vue 的 `user-bar` 风格。宫格使用 `uni-grid` 组件。`.quick-actions` 使用卡片式布局。

#### 3.1 作业计划列表页

路径：`RuoYi-App/pages/worker/plan/list.vue`

**页面结构**：
```html
<template>
  <view class="container">
    <!-- 状态筛选栏 -->
    <view class="filter-bar">
      <view class="filter-tag" v-for="s in statusOptions" :key="s.value"
        :class="{ active: currentStatus === s.value }" @click="filterByStatus(s.value)">
        {{ s.label }}
      </view>
    </view>

    <!-- 计划卡片列表 -->
    <view class="list" v-if="planList.length > 0">
      <view class="card" v-for="p in planList" :key="p.planId" @click="goDetail(p.planId)">
        <view class="card-header">
          <text class="project-name">{{ p.projectName }}</text>
          <text class="status-tag" :class="'status-' + p.status">{{ getStatusLabel(p.status) }}</text>
        </view>
        <view class="card-body">
          <view class="info-row"><text class="label">施工点</text><text>{{ p.constructionSite || '-' }}</text></view>
          <view class="info-row"><text class="label">作业类型</text><text>{{ p.workType || '-' }}</text></view>
          <view class="info-row"><text class="label">计划时间</text><text>{{ formatTime(p.planWorkTime) }}</text></view>
          <view class="info-row"><text class="label">施工单位</text><text>{{ p.constructionUnit || '-' }}</text></view>
        </view>
        <view class="card-footer">
          <text class="time">{{ formatTime(p.createTime) }}</text>
          <uni-icons type="arrowright" size="16" color="#ccc"></uni-icons>
        </view>
      </view>
    </view>
    <view class="empty" v-else>暂无作业计划</view>

    <!-- 新建按钮（仅施工方可建） -->
    <uni-fab v-if="canCreate" ref="fab" :pattern="{ buttonColor: '#007aff' }" @click="goCreate" />
  </view>
</template>
```

**data**：
```javascript
data() {
  return {
    planList: [],
    currentStatus: '',
    canCreate: false,
    statusOptions: [
      { label: '全部', value: '' },
      { label: '待审核', value: '0' },
      { label: '待执行', value: '1' },
      { label: '进行中', value: '2' },
      { label: '已完成', value: '3' },
      { label: '已取消', value: '4' }
    ]
  }
}
```

**methods**：
- `onShow()` → `loadPlans()`
- `loadPlans()` → `uni.request({ url: baseUrl + '/app/homework/plan/list', params: { pageNum, pageSize, status } })`
- `filterByStatus(s)` → 设置 `currentStatus`，重新加载
- `goDetail(id)` → `uni.navigateTo({ url: '/pages/worker/plan/detail?planId=' + id })`
- `goCreate()` → `uni.navigateTo({ url: '/pages/worker/plan/create' })`
- `getStatusLabel(v)` → 从 `statusLabels` 映射获取中文名
- `formatTime(t)` → 日期格式化

#### 3.2 作业计划详情页

路径：`RuoYi-App/pages/worker/plan/detail.vue`

**页面结构**：
```html
<template>
  <view class="container">
    <!-- 状态头 -->
    <view class="status-header" :class="'bg-status-' + plan.status">
      <text class="status-text">{{ getStatusLabel(plan.status) }}</text>
    </view>

    <!-- 计划信息 -->
    <view class="section">
      <view class="section-title">基本信息</view>
      <view class="info-item"><text class="label">项目名称</text><text>{{ plan.projectName }}</text></view>
      <view class="info-item"><text class="label">施工点</text><text>{{ plan.constructionSite || '-' }}</text></view>
      <view class="info-item"><text class="label">作业类型</text><text>{{ plan.workType || '-' }}</text></view>
      <view class="info-item"><text class="label">计划时间</text><text>{{ formatTime(plan.planWorkTime) }}</text></view>
      <view class="info-item"><text class="label">施工单位</text><text>{{ plan.constructionUnit || '-' }}</text></view>
      <view class="info-item"><text class="label">作业内容</text><text>{{ plan.workContent || '-' }}</text></view>
    </view>

    <!-- 参与人员 -->
    <view class="section">
      <view class="section-title">参与人员 ({{ workers.length }})</view>
      <view class="worker-tags">
        <view class="worker-tag" v-for="w in workers" :key="w.id">
          {{ w.workerName }}<text class="role-name">{{ w.roleType }}</text>
        </view>
        <text v-if="workers.length === 0" class="empty-hint">暂无人员</text>
      </view>
    </view>

    <!-- 审核信息（如果有关联审核记录） -->
    <view class="section" v-if="review">
      <view class="section-title">审核信息</view>
      <view class="info-item"><text class="label">审核状态</text>
        <text :class="'review-status-' + review.reviewStatus">{{ getReviewStatusLabel(review.reviewStatus) }}</text>
      </view>
      <view class="info-item" v-if="review.reviewer"><text class="label">审核人</text><text>{{ review.reviewer }}</text></view>
      <view class="info-item" v-if="review.reviewTime"><text class="label">审核时间</text><text>{{ formatTime(review.reviewTime) }}</text></view>
      <view class="info-item" v-if="review.reviewOpinion"><text class="label">审核意见</text><text>{{ review.reviewOpinion }}</text></view>
    </view>
  </view>
</template>
```

**data**：`plan: {}`, `workers: []`, `review: null`

**methods**：
- `onLoad(options)` → 取 `options.planId`，并行加载计划详情 + 参与人员
- `loadDetail(planId)` → `Promise.all([GET /app/homework/plan/{planId}, GET /app/homework/plan/{planId}/workers])`

#### 3.3 新建作业计划页

路径：`RuoYi-App/pages/worker/plan/create.vue`

**页面结构**：
```html
<template>
  <view class="container">
    <uni-forms ref="form" :modelValue="form" label-position="top">
      <uni-forms-item label="项目名称" required name="projectName">
        <uni-easyinput v-model="form.projectName" placeholder="请输入项目名称" />
      </uni-forms-item>

      <uni-forms-item label="市/县" name="cityCounty">
        <uni-easyinput v-model="form.cityCounty" placeholder="请输入市/县" />
      </uni-forms-item>

      <uni-forms-item label="施工点" name="constructionSite">
        <uni-easyinput v-model="form.constructionSite" placeholder="请输入施工点" />
      </uni-forms-item>

      <uni-forms-item label="作业类型" required name="workType">
        <uni-data-select v-model="form.workType" :localdata="workTypeOptions" placeholder="请选择作业类型" />
      </uni-forms-item>

      <uni-forms-item label="计划作业时间" name="planWorkTime">
        <uni-datetime-picker v-model="form.planWorkTime" type="datetime" placeholder="请选择时间" />
      </uni-forms-item>

      <uni-forms-item label="施工单位" name="constructionUnit">
        <uni-easyinput v-model="form.constructionUnit" placeholder="请输入施工单位" />
      </uni-forms-item>

      <uni-forms-item label="作业内容" name="workContent">
        <uni-easyinput v-model="form.workContent" type="textarea" placeholder="请输入作业内容" />
      </uni-forms-item>

      <uni-forms-item label="备注" name="remark">
        <uni-easyinput v-model="form.remark" type="textarea" placeholder="请输入备注" />
      </uni-forms-item>
    </uni-forms>

    <!-- 参与人员选择 -->
    <view class="section">
      <view class="section-title">参与人员 <text class="sub">（选填，可创建后补加）</text></view>
      <!-- 简化版：暂用文本输入，后续可改为滚动多选 -->
      <uni-easyinput v-model="workerInput" type="textarea" placeholder="输入人员姓名，逗号分隔" />
    </view>

    <view class="submit-bar">
      <button class="btn-submit" @click="submit" :disabled="submitting">
        {{ submitting ? '提交中...' : '提交计划' }}
      </button>
    </view>
  </view>
</template>
```

**data**：
```javascript
data() {
  return {
    form: {
      projectName: '', cityCounty: '', constructionSite: '',
      workType: '', planWorkTime: '', constructionUnit: '', workContent: '', remark: ''
    },
    workerInput: '',
    workTypeOptions: [
      { value: '动土', text: '动土作业' }, { value: '防腐', text: '防腐作业' },
      { value: '检测', text: '检测作业' }, { value: '临时用电', text: '临时用电' },
      { value: '受限空间', text: '受限空间' }, { value: '机械作业', text: '机械作业' },
      { value: '修复', text: '修复作业' }, { value: '点火', text: '点火作业' }
    ],
    submitting: false
  }
}
```

**methods**：
- `submit()` → 表单校验 → `uni.request({ url: baseUrl + '/app/homework/plan', method: 'POST', data: form })` → 成功 toast + `uni.navigateBack()`

#### 3.4 作业审核列表页

路径：`RuoYi-App/pages/worker/review/list.vue`

**页面结构**：
```html
<template>
  <view class="container">
    <!-- 状态筛选栏 -->
    <view class="filter-bar">
      <view class="filter-tag" v-for="s in reviewStatusOptions" :key="s.value"
        :class="{ active: currentStatus === s.value }" @click="filterByStatus(s.value)">
        {{ s.label }}
      </view>
    </view>

    <!-- 审核卡片列表 -->
    <view class="list" v-if="reviewList.length > 0">
      <view class="card" v-for="r in reviewList" :key="r.reviewId" @click="goDetail(r.reviewId)">
        <view class="card-header">
          <text class="plan-name">{{ r.planName }}</text>
          <text class="review-tag" :class="'review-' + r.reviewStatus">{{ getReviewStatusLabel(r.reviewStatus) }}</text>
        </view>
        <view class="card-body">
          <view class="info-row"><text class="label">申请人</text><text>{{ r.applicant || '-' }}</text></view>
          <view class="info-row"><text class="label">作业类型</text><text>{{ r.workType || '-' }}</text></view>
          <view class="info-row"><text class="label">申请时间</text><text>{{ formatTime(r.applyTime) }}</text></view>
        </view>
        <!-- 待审核时显示操作按钮 -->
        <view class="card-footer" v-if="r.reviewStatus === '0'">
          <text class="action-btn approve" @click.stop="approve(r)">通过</text>
          <text class="action-btn reject" @click.stop="reject(r)">驳回</text>
        </view>
      </view>
    </view>
    <view class="empty" v-else>暂无审核记录</view>
  </view>
</template>
```

**data**：`reviewList: []`, `currentStatus: ''`, `reviewStatusOptions: [{ label:'全部',value:'' }, { label:'待审核',value:'0' }, { label:'已通过',value:'1' }, { label:'已驳回',value:'2' }]`

**methods**：
- `loadList()` → `GET /app/homework/review/list`
- `goDetail(id)` → `uni.navigateTo({ url: '/pages/worker/review/detail?reviewId=' + id })`
- `approve(r)` / `reject(r)` → 快捷操作弹窗输入意见 → 调 API

#### 3.5 作业审核详情页

路径：`RuoYi-App/pages/worker/review/detail.vue`

**页面结构**：
```html
<template>
  <view class="container">
    <!-- 审核状态头 -->
    <view class="status-header" :class="'review-bg-' + review.reviewStatus">
      <text>{{ getReviewStatusLabel(review.reviewStatus) }}</text>
    </view>

    <!-- 计划信息（只读，复用 plan/detail 的 section 样式） -->
    <view class="section">
      <view class="section-title">计划信息</view>
      <!-- 同 plan/detail.vue 的信息展示 -->
    </view>

    <!-- 参与人员 -->
    <view class="section">
      <view class="section-title">参与人员</view>
      <!-- 同 plan/detail.vue 的人员标签 -->
    </view>

    <!-- 审核操作区（仅待审核时显示） -->
    <view class="review-actions" v-if="review.reviewStatus === '0'">
      <view class="section-title">审核意见</view>
      <uni-easyinput v-model="reviewOpinion" type="textarea" placeholder="请输入审核意见（选填）" />
      <view class="btn-group">
        <button class="btn-approve" @click="doApprove">审核通过</button>
        <button class="btn-reject" @click="doReject">审核驳回</button>
      </view>
    </view>
  </view>
</template>
```

**methods**：
- `onLoad(options)` → `loadDetail(options.reviewId)`
- `loadDetail(id)` → `GET /app/homework/review/{id}` → 解析 plan + workers + review
- `doApprove()` → `PUT /app/homework/review/approve { reviewId, reviewOpinion }` → msgSuccess → `uni.navigateBack()`
- `doReject()` → `PUT /app/homework/review/reject { reviewId, reviewOpinion }` → msgSuccess → `uni.navigateBack()`

#### 3.6 打卡记录页（原 records.vue 保留）

路径：`RuoYi-App/pages/worker/records.vue`（**不变**，仅从工作台宫格跳转进入）

---

### 第四部分：前端 — 修改现有文件

#### 4.1 pages.json — 路由配置

修改内容：

1. **Tab "记录" 改为 "工作台"**：将第二个 tab 的 `text` 从 `"记录"` 改为 `"工作台"`，`pagePath` 从 `pages/worker/records` 改为 `pages/worker/workbench`
2. **新增页面路由**：

```json
{
  "pages": [
    // ... 现有路由保持不变 ...
    {
      "path": "pages/worker/workbench",
      "style": { "navigationBarTitleText": "工作台" }
    }, {
      "path": "pages/worker/plan/list",
      "style": { "navigationBarTitleText": "作业计划" }
    }, {
      "path": "pages/worker/plan/detail",
      "style": { "navigationBarTitleText": "计划详情" }
    }, {
      "path": "pages/worker/plan/create",
      "style": { "navigationBarTitleText": "新建计划" }
    }, {
      "path": "pages/worker/review/list",
      "style": { "navigationBarTitleText": "作业审核" }
    }, {
      "path": "pages/worker/review/detail",
      "style": { "navigationBarTitleText": "审核详情" }
    }
    // ... 其余路由 ...
  ]
}
```

完整 pages.json 修改：见 `第五部分：pages.json 完整变更`。

#### 4.2 permission.js — 路由白名单更新

路径：`RuoYi-App/permission.js`

在免登录白名单中新增：
```javascript
const whiteList = [
  '/pages/worker/login', '/pages/login', '/pages/register',
  '/pages/common/webview/index',
  '/pages/worker/checkin', '/pages/worker/mine',
  '/pages/worker/workbench',       // 新增：工作台
  '/pages/worker/records',         // 保留：打卡记录（非tab页面）
  '/pages/worker/idcard', '/pages/worker/upload', '/pages/worker/face',
  '/pages/worker/plan/list', '/pages/worker/plan/detail', '/pages/worker/plan/create',     // 新增
  '/pages/worker/review/list', '/pages/worker/review/detail'                                 // 新增
]
```

---

### 第五部分：pages.json 完整变更

```json
{
  "pages": [{
    "path": "pages/worker/login",
    "style": { "navigationBarTitleText": "智慧工地" }
  }, {
    "path": "pages/worker/checkin",
    "style": { "navigationBarTitleText": "我要打卡" }
  }, {
    "path": "pages/worker/records",
    "style": { "navigationBarTitleText": "打卡记录" }
  }, {
    "path": "pages/worker/workbench",
    "style": { "navigationBarTitleText": "工作台" }
  }, {
    "path": "pages/worker/mine",
    "style": { "navigationBarTitleText": "我的" }
  }, {
    "path": "pages/worker/idcard",
    "style": { "navigationBarTitleText": "上传身份证" }
  }, {
    "path": "pages/worker/upload",
    "style": { "navigationBarTitleText": "上传资质" }
  }, {
    "path": "pages/worker/plan/list",
    "style": { "navigationBarTitleText": "作业计划" }
  }, {
    "path": "pages/worker/plan/detail",
    "style": { "navigationBarTitleText": "计划详情" }
  }, {
    "path": "pages/worker/plan/create",
    "style": { "navigationBarTitleText": "新建计划" }
  }, {
    "path": "pages/worker/review/list",
    "style": { "navigationBarTitleText": "作业审核" }
  }, {
    "path": "pages/worker/review/detail",
    "style": { "navigationBarTitleText": "审核详情" }
  }, {
    "path": "pages/login",
    "style": { "navigationBarTitleText": "登录" }
  }, {
    "path": "pages/index",
    "style": { "navigationBarTitleText": "若依移动端框架", "navigationStyle": "custom" }
  }, {
    "path": "pages/work/index",
    "style": { "navigationBarTitleText": "工作台" }
  }, {
    "path": "pages/mine/index",
    "style": { "navigationBarTitleText": "我的" }
  }, {
    "path": "pages/common/webview/index",
    "style": { "navigationBarTitleText": "浏览网页" }
  }],
  "tabBar": {
    "color": "#999999",
    "selectedColor": "#007aff",
    "borderStyle": "white",
    "backgroundColor": "#ffffff",
    "list": [{
      "pagePath": "pages/worker/checkin",
      "iconPath": "static/images/tabbar/home.png",
      "selectedIconPath": "static/images/tabbar/home_.png",
      "text": "打卡"
    }, {
      "pagePath": "pages/worker/workbench",
      "iconPath": "static/images/tabbar/work.png",
      "selectedIconPath": "static/images/tabbar/work_.png",
      "text": "工作台"
    }, {
      "pagePath": "pages/worker/mine",
      "iconPath": "static/images/tabbar/mine.png",
      "selectedIconPath": "static/images/tabbar/mine_.png",
      "text": "我的"
    }]
  },
  "globalStyle": {
    "navigationBarTextStyle": "black",
    "navigationBarTitleText": "智慧工地",
    "navigationBarBackgroundColor": "#FFFFFF"
  }
}
```

---

## 四、验证清单

所有文件创建完成后，执行以下验证：

| # | 验证项 | 验证方法 |
|---|--------|---------|
| 1 | 后端编译 | `mvn compile -DskipTests` → `BUILD SUCCESS` |
| 2 | uni-app 编译 | HBuilderX 中运行到浏览器，确认无编译错误 |
| 3 | 底部 Tab 变更 | 确认第二个 Tab 文字为"工作台"，点击进入工作台页面 |
| 4 | 工作台宫格 | 工作台显示"作业计划""作业审核""打卡记录"三个入口 |
| 5 | 计划列表 | 点击"作业计划"→ 显示计划卡片列表 → 状态筛选正常工作 |
| 6 | 计划详情 | 点击计划卡片 → 显示完整计划信息 + 参与人员标签 |
| 7 | 创建计划 | 施工方人员点击新建 → 填写表单 → 提交成功 → toast 提示 |
| 8 | 创建计划权限 | 普通施工人员（仅 worker 角色）→ 创建时提示"无权创建" |
| 9 | 审核列表 | 作业批准人进入"作业审核"→ 显示审核记录列表 |
| 10 | 审核列表权限 | 非批准人进入 → 列表为空（或提示无权限） |
| 11 | 审核详情 | 点击审核卡片 → 显示计划详情 + 参与人员 + 审核意见输入框 |
| 12 | 审核通过 | 输入审核意见 → 点击"审核通过"→ toast 成功 → 计划状态变为"待执行" |
| 13 | 审核驳回 | 输入驳回原因 → 点击"审核驳回"→ toast 成功 → 计划状态变为"已取消" |
| 14 | 重复审核拦截 | 对已审核记录再次操作 → toast 提示"该记录已审核" |
| 15 | 打卡记录 | 从工作台点击"打卡记录"→ 跳转到原 records 页面，正常显示打卡记录 |
| 16 | 未登录拦截 | 清除 appToken → 刷新 → 自动跳转登录页 |

---

## 五、总文件改动清单

### 新增文件（共 8 个）

| # | 文件 | 类型 | 说明 |
|---|------|------|------|
| 1 | `AppHomeworkController.java` | 后端 Controller | `/app/homework/*` 移动端作业管理接口 |
| 2 | `pages/worker/workbench.vue` | uni-app 页面 | 工作台首页（宫格导航） |
| 3 | `pages/worker/plan/list.vue` | uni-app 页面 | 计划列表（卡片 + 状态筛选 + 新建按钮） |
| 4 | `pages/worker/plan/detail.vue` | uni-app 页面 | 计划详情（信息展示 + 人员标签） |
| 5 | `pages/worker/plan/create.vue` | uni-app 页面 | 新建计划表单 |
| 6 | `pages/worker/review/list.vue` | uni-app 页面 | 审核列表（卡片 + 快捷操作） |
| 7 | `pages/worker/review/detail.vue` | uni-app 页面 | 审核详情 + 通过/驳回操作 |

### 修改文件（共 6 个）

| # | 文件 | 改动内容 |
|---|------|---------|
| 1 | `HwPlanMapper.java` | 新增 `selectHwPlanListByWorkerId` 方法 |
| 2 | `HwPlanMapper.xml` | 新增 `selectHwPlanListByWorkerId` SQL（关联 hw_plan_worker） |
| 3 | `HwPlanWorkerMapper.java` | 新增 `selectByPlanId` 方法（如不存在） |
| 4 | `HwPlanWorkerMapper.xml` | 新增 `selectByPlanId` SQL（如不存在） |
| 5 | `pages.json` | Tab "记录"→"工作台"；新增 5 个页面路由 |
| 6 | `permission.js` | 白名单新增 6 个路由 |

### 数据库变更

**无需变更**。`hw_plan`、`hw_review`、`hw_plan_worker` 表均已存在。

---

## 六、模块架构

```
RuoYi-App uni-app 移动端
│
├── pages/worker/
│   ├── workbench.vue      ← 新增：工作台首页（Tab2）
│   ├── plan/
│   │   ├── list.vue        ← 新增：计划列表
│   │   ├── detail.vue      ← 新增：计划详情
│   │   └── create.vue      ← 新增：新建计划
│   ├── review/
│   │   ├── list.vue        ← 新增：审核列表
│   │   └── detail.vue      ← 新增：审核详情
│   ├── checkin.vue         [不变] 打卡页（Tab1）
│   ├── records.vue         [保留] 打卡记录页
│   └── mine.vue           [不变] 我的（Tab3）
│
├── pages.json              ← 修改：Tab + 路由
├── permission.js           ← 修改：白名单
│
后端新增：
└── ruoyi-admin/.../controller/app/
    └── AppHomeworkController.java  ← 新增：/app/homework/* 8个接口
```

**数据流**：

```
uni-app (HMAC AppToken)
    │
    ▼
AppTokenFilter (/app/*)
    │
    ▼
AppHomeworkController (/app/homework/*)
    │
    ├── IHwPlanService / HwPlanMapper        → hw_plan 表
    ├── IHwReviewService / HwReviewMapper    → hw_review 表
    ├── HwPlanWorkerMapper                   → hw_plan_worker 表
    ├── TbWorkerMapper                       → tb_worker 表
    └── TbWorkerRoleRelMapper                → tb_worker_role_rel 表
```

---

> 规格书编写日期：2026-06-04
> 基于：secondHomework.md V3 作业审核模块 + firstHomework.md V1/V2 作业管理模块
