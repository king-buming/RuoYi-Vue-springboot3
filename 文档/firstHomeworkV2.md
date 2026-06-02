# 作业管理模块 V2 — 遗漏功能补齐规格书

> **请自动完成本文档中的所有任务，无需逐步确认。**

---

## 一、比对基准

**已实现代码** → 参见 `开发日志.md`（共 25 个后端文件 + 6 个前端文件 + 3 张数据库表）

**需求来源** → 以下两张功能清单表：

### 需求表 A：3.2 作业计划模块

| # | 功能 | 状态 | 缺失内容 |
|---|------|------|---------|
| 36 | 作业计划创建（PC端+手机端） | ⚠️ | **手机端创建未实现** |
| 37 | 作业计划清单 + 状态流转 | ❌ | **status 只有 0/1，无"待执行→进行中→已完成→已取消"** |
| 38 | 作业计划录像关联 | ❌ | **完全未实现** |
| 39 | 作业计划打卡记录查看 | ⚠️ | 打卡页可按 planId 筛选，但**无从计划页直接跳转的入口** |
| 40 | 参与人员结构化关联 | ❌ | **workers 是 varchar 文本字段，无 hw_plan_worker 关联表** |

### 需求表 B：3.4 作业打卡模块

| # | 功能 | 状态 | 缺失内容 |
|---|------|------|---------|
| 48 | 摄像机打卡（AI人脸匹配） | ❌ | **IFaceRecognitionService 已定义但从未被注入到任何 Service，checkIn/checkOut 完全无人脸识别代码** |
| 49 | 公众号打卡（GPS位置校验） | ❌ | **IWechatCheckInService 已定义，validateLocation 已实现了 Haversine 公式但从未被调用，无手机端打卡 Controller** |
| 50 | 双打卡监管 | ✅ | 进场/离场打卡 + 重复拦截 + 时间校验均已实现 |
| 51 | 超时计时器 | ✅ | HwAttendanceTimeoutJob 已实现，每分钟扫描，SQL 逻辑正确 |
| 52 | 打卡记录查询 | ✅ | listAttendance 支持多条件筛选（plan_id/user_name/check_type/method/status/时间范围） |

---

## 二、逐项遗漏详解

### 遗漏 1：hw_plan.status 状态流转（需求 #37）⚠️ P0

**当前代码**：
- `HwPlan.java`：`private String status; // 注释：状态（0正常 1停用）`
- `HwPlanServiceImpl.java`：**无 changeStatus 方法**
- 前端：`el-switch` 只读显示 0/1

**缺失**：作业生命周期状态（`0待执行 → 1进行中 → 2已完成 → 3已取消`）

**需改动的文件**：

#### 1.1 数据库

```sql
ALTER TABLE hw_plan MODIFY COLUMN status CHAR(1) DEFAULT '0' COMMENT '状态（0待执行 1进行中 2已完成 3已取消）';
```

#### 1.2 HwPlan.java — 修改 status 字段注释

```java
/** 状态（0待执行 1进行中 2已完成 3已取消） */
private String status;
```

#### 1.3 IHwPlanService.java — 新增方法

```java
/**
 * 变更作业计划状态（含流转规则校验）
 * @param planId 计划ID
 * @param status 目标状态（0待执行 1进行中 2已完成 3已取消）
 */
public int changeStatus(Long planId, String status);
```

#### 1.4 HwPlanServiceImpl.java — 新增实现（插入到 updateHwPlan 方法后面）

```java
@Override
public int changeStatus(Long planId, String status) {
    HwPlan plan = hwPlanMapper.selectHwPlanById(planId);
    if (plan == null) {
        throw new ServiceException("作业计划不存在");
    }
    String current = plan.getStatus();
    // 已取消不可再变更（除非是恢复操作）
    if ("3".equals(current) && !"0".equals(status)) {
        throw new ServiceException("已取消的计划不可变更状态");
    }
    // 已完成不可再变更（除非是恢复操作）
    if ("2".equals(current)) {
        throw new ServiceException("已完成的计划不可变更状态");
    }
    // 流转规则：待执行→进行中→已完成，或任意→已取消
    if ("1".equals(status) && !"0".equals(current)) {
        throw new ServiceException("只有待执行的计划才能开始作业");
    }
    if ("2".equals(status) && !"1".equals(current)) {
        throw new ServiceException("只有进行中的计划才能标记完成");
    }
    plan.setStatus(status);
    plan.setUpdateBy(SecurityUtils.getUsername());
    return hwPlanMapper.updateHwPlan(plan);
}
```

#### 1.5 HwPlanController.java — 新增接口

```java
@PreAuthorize("@ss.hasPermi('homework:plan:edit')")
@Log(title = "作业计划", businessType = BusinessType.UPDATE)
@PutMapping("/changeStatus")
public AjaxResult changeStatus(@RequestBody HwPlan hwPlan) {
    try {
        return toAjax(hwPlanService.changeStatus(hwPlan.getPlanId(), hwPlan.getStatus()));
    } catch (ServiceException e) {
        return error(e.getMessage());
    }
}
```

#### 1.6 前端 plan/index.vue — status 列改为标签 + 操作列增加状态按钮

**data 中修改**：
```javascript
statusOptions: [
  { label: '待执行', value: '0', type: 'info' },
  { label: '进行中', value: '1', type: 'primary' },
  { label: '已完成', value: '2', type: 'success' },
  { label: '已取消', value: '3', type: 'danger' }
]
```

**表格 status 列**（替换原来的 el-switch）：
```html
<el-table-column label="状态" align="center" prop="status" width="100">
  <template slot-scope="scope">
    <el-tag :type="['info','primary','success','danger'][Number(scope.row.status)]">
      {{ ['待执行','进行中','已完成','已取消'][Number(scope.row.status)] }}
    </el-tag>
  </template>
</el-table-column>
```

**操作列增加状态变更按钮**（在修改/删除前）：
```html
<el-dropdown v-if="scope.row.status !== '2' && scope.row.status !== '3'"
  @command="(cmd) => handleStatusChange(scope.row, cmd)" style="margin-right:5px">
  <el-button type="warning" size="mini">
    状态<i class="el-icon-arrow-down el-icon--right"></i>
  </el-button>
  <el-dropdown-menu slot="dropdown">
    <el-dropdown-item v-if="scope.row.status === '0'" command="1">开始作业</el-dropdown-item>
    <el-dropdown-item v-if="scope.row.status === '1'" command="2">标记完成</el-dropdown-item>
    <el-dropdown-item command="3">取消计划</el-dropdown-item>
  </el-dropdown-menu>
</el-dropdown>
```

**methods 新增**：
```javascript
handleStatusChange(row, newStatus) {
  const labels = { '0': '待执行', '1': '进行中', '2': '已完成', '3': '已取消' };
  this.$modal.confirm('确认将【' + row.projectName + '】状态变更为"' + labels[newStatus] + '"？')
    .then(() => updatePlan({ planId: row.planId, status: newStatus }))
    .then(() => { this.getList(); this.$modal.msgSuccess('状态变更成功'); });
}
```

---

### 遗漏 2：参与人员结构化关联（需求 #40）❌ P0

**当前代码**：
- `hw_plan.workers` 类型 `varchar(500)`，前端用 textarea 手动输入人名
- 全局搜索 `hw_plan_worker` — **零结果**，表不存在

**缺失**：hw_plan_worker 关联表，支持从 hw_worker 表中选择人员并建立结构化关联

**需新增/改动的文件**：

#### 2.1 数据库：新建 hw_plan_worker 表

```sql
DROP TABLE IF EXISTS hw_plan_worker;
CREATE TABLE hw_plan_worker (
  id          BIGINT(20)   NOT NULL AUTO_INCREMENT  COMMENT '关联ID',
  plan_id     BIGINT(20)   NOT NULL                  COMMENT '作业计划ID',
  worker_id   BIGINT(20)   NOT NULL                  COMMENT '人员ID',
  worker_name VARCHAR(64)  DEFAULT ''               COMMENT '人员姓名（冗余）',
  role_type   CHAR(1)      DEFAULT ''               COMMENT '人员角色（冗余）',
  create_by   VARCHAR(64)  DEFAULT ''               COMMENT '创建者',
  create_time DATETIME                               COMMENT '创建时间',
  PRIMARY KEY (id),
  INDEX idx_plan_id (plan_id),
  INDEX idx_worker_id (worker_id)
) ENGINE=InnoDB COMMENT='作业计划人员关联表';
```

#### 2.2 新建 HwPlanWorker.java

路径：`ruoyi-system/src/main/java/com/ruoyi/system/domain/HwPlanWorker.java`
- 继承 `BaseEntity`
- 字段：`id(Long)`, `planId(Long)`, `workerId(Long)`, `workerName(String)`, `roleType(String)`

#### 2.3 新建 HwPlanWorkerMapper.java + XML

路径：`ruoyi-system/src/main/java/com/ruoyi/system/mapper/HwPlanWorkerMapper.java`  
XML：`ruoyi-system/src/main/resources/mapper/system/HwPlanWorkerMapper.xml`

```java
public interface HwPlanWorkerMapper {
    List<HwPlanWorker> selectByPlanId(Long planId);
    int batchInsert(@Param("list") List<HwPlanWorker> list);
    int deleteByPlanId(Long planId);
    int deleteById(Long id);
}
```

XML 中 batchInsert 用 `<foreach collection="list" item="item" separator=",">` 批量插入。

#### 2.4 新建 IHwPlanWorkerService.java + HwPlanWorkerServiceImpl.java

路径：`ruoyi-system/.../service/`、`ruoyi-system/.../service/impl/`  
标准 `@Service`，注入 `HwPlanWorkerMapper`，4 个方法委托给 Mapper。

#### 2.5 HwPlanController.java — 新增 2 个接口

```java
@Autowired
private IHwPlanWorkerService planWorkerService;

// 查询关联人员
@PreAuthorize("@ss.hasPermi('homework:plan:query')")
@GetMapping("/{planId}/workers")
public AjaxResult getWorkers(@PathVariable Long planId) {
    return success(planWorkerService.selectByPlanId(planId));
}

// 保存关联人员（全量替换：先删后批量插入）
@PreAuthorize("@ss.hasPermi('homework:plan:edit')")
@PostMapping("/{planId}/workers")
public AjaxResult saveWorkers(@PathVariable Long planId, @RequestBody List<HwPlanWorker> workers) {
    planWorkerService.deleteByPlanId(planId);
    if (!CollectionUtils.isEmpty(workers)) {
        workers.forEach(w -> {
            w.setPlanId(planId);
            w.setCreateBy(SecurityUtils.getUsername());
        });
        planWorkerService.batchInsert(workers);
    }
    return success();
}
```

#### 2.6 前端 plan.js — 新增 2 个 API 函数

```javascript
export function getPlanWorkers(planId) {
  return request({ url: '/homework/plan/' + planId + '/workers', method: 'get' })
}
export function savePlanWorkers(planId, workerList) {
  return request({ url: '/homework/plan/' + planId + '/workers', method: 'post', data: workerList })
}
```

#### 2.7 前端 plan/index.vue — 弹窗中参与人员改为选择器

**替换原来的 `<el-input v-model="form.workers" type="textarea">`** 为：

```html
<el-form-item label="参与人员">
  <el-select v-model="selectedWorkers" multiple filterable placeholder="请选择参与人员"
    value-key="workerId" style="width:100%">
    <el-option v-for="w in workerOptions" :key="w.workerId"
      :label="w.workerName + ' (' + roleLabel(w.roleType) + ')'"
      :value="{ workerId: w.workerId, workerName: w.workerName, roleType: w.roleType }" />
  </el-select>
  <el-tag v-for="w in selectedWorkers" :key="w.workerId" closable style="margin:2px"
    @close="removeWorker(w.workerId)">
    {{ w.workerName }}({{ roleLabel(w.roleType) }})
  </el-tag>
</el-form-item>
```

**data 新增**：`selectedWorkers: []`, `workerOptions: []`

**methods 新增**：
```javascript
loadWorkerOptions() {
  listWorker({ pageNum: 1, pageSize: 999, status: '0' }).then(r => { this.workerOptions = r.rows; });
},
roleLabel(type) {
  const m = {'1':'作业申请人','2':'作业批准人','3':'作业监护人','4':'监理人员',
    '5':'施工方项目经理','6':'施工方安全员','7':'施工方现场负责人','8':'作业单位监护人','9':'施工人员'};
  return m[type] || type;
},
removeWorker(id) { this.selectedWorkers = this.selectedWorkers.filter(w => w.workerId !== id); }
```

**handleAdd/handleUpdate 修改** — 打开弹窗时加载可选人员，编辑时回填已选人员：
```javascript
handleAdd() {
  this.reset();
  this.loadWorkerOptions();
  this.selectedWorkers = [];
  this.open = true;
  this.title = '添加作业计划';
},
handleUpdate(row) {
  this.reset();
  this.loadWorkerOptions();
  getPlanWorkers(row.planId).then(r => { this.selectedWorkers = r.data || []; });
  this.form = JSON.parse(JSON.stringify(row));
  this.open = true;
  this.title = '修改作业计划';
}
```

**submitForm 修改** — 提交表单后保存人员关联：
```javascript
submitForm() {
  this.$refs.form.validate(valid => {
    if (!valid) return;
    const save = this.form.planId ? updatePlan(this.form) : addPlan(this.form);
    save.then(r => {
      if (r.code === 200) {
        const pid = this.form.planId || r.data;
        return savePlanWorkers(pid, this.selectedWorkers);
      }
    }).then(() => {
      this.$modal.msgSuccess('保存成功');
      this.open = false;
      this.getList();
    });
  });
}
```

---

### 遗漏 3：摄像机打卡——AI 人脸识别未接入（需求 #48）❌ P1

**当前代码**：
- `IFaceRecognitionService` 已定义（`compareFace` / `detectFace`）
- `StubFaceRecognitionService` 桩实现已存在（`@Component`，返回 `fail("AI人脸识别服务尚未配置")`）
- **但 `HwAttendanceServiceImpl` 只注入了 `HwAttendanceMapper` 和 `HwPlanMapper`，从未注入 `IFaceRecognitionService`**
- **`checkIn` 方法中没有任何 AI 人脸识别代码**

**缺失**：checkIn 流程中缺少人脸比对逻辑，即使配置了真实 AI 服务也无法生效

**需改动的文件**：

#### 3.1 HwAttendanceServiceImpl.java

**注入人脸识别服务**（在已有 @Autowired 后面加）：
```java
@Autowired(required = false)
private IFaceRecognitionService faceRecognitionService;

@Autowired
private HwWorkerMapper hwWorkerMapper;
```

**修改 checkIn 方法**（在设置 checkStatus 之后、insertHwAttendance 之前插入）：
```java
// === 人脸打卡：调用 AI 人脸比对（插入在 setCheckStatus("0") 之后 ===
if ("0".equals(hwAttendance.getCheckMethod()) && faceRecognitionService != null) {
    if (StringUtils.isEmpty(hwAttendance.getFaceImage())) {
        hwAttendance.setCheckStatus("1");
        hwAttendance.setFailReason("人脸打卡需要上传抓拍图片");
    } else {
        HwWorker worker = hwWorkerMapper.selectHwWorkerById(hwAttendance.getUserId());
        if (worker == null || StringUtils.isEmpty(worker.getFaceImage())) {
            hwAttendance.setCheckStatus("1");
            hwAttendance.setFailReason("未找到该人员的人脸底图，请先在人员管理中上传");
        } else {
            FaceMatchResult result = faceRecognitionService.compareFace(
                hwAttendance.getFaceImage(), worker.getFaceImage());
            if (!result.isMatched()) {
                hwAttendance.setCheckStatus("1");
                hwAttendance.setFailReason("人脸识别不匹配：" + result.getErrorMessage());
            }
        }
    }
}
// === 人脸打卡结束 ===
```

> 注：`@Autowired(required = false)` 确保 AI 服务未配置时不影响基础打卡功能。

#### 3.2 HwWorkerMapper.java — 新增方法

```java
HwWorker selectHwWorkerByUserId(@Param("userId") Long userId);
```

#### 3.3 HwWorkerMapper.xml — 新增 SQL

```xml
<select id="selectHwWorkerByUserId" parameterType="long" resultMap="HwWorkerResult">
    select * from hw_worker where worker_id = #{userId} and status = '0'
</select>
```

---

### 遗漏 4：公众号打卡——手机端入口 + GPS 校验未接入（需求 #49）❌ P1

**当前代码**：
- `IWechatCheckInService` 已定义（`wechatCheckIn` / `validateLocation`）
- `StubWechatCheckInService` 桩实现已存在，`validateLocation` 实现了完整的 **Haversine 公式** GPS 距离计算
- **但 `HwAttendanceServiceImpl` 中从未注入 `IWechatCheckInService`，没有手机端打卡 Controller**
- 全局搜索 `WechatAttendanceController` — **零结果**

**缺失**：
1. GPS 位置校验代码未接入 checkIn/checkOut 流程
2. 无手机端打卡 Controller（`/wechat/attendance/checkIn` 等）

**需改动的文件**：

#### 4.1 HwAttendanceServiceImpl.java — 注入 + 位置校验

**注入**（在已有注入后面加）：
```java
@Autowired(required = false)
private IWechatCheckInService wechatCheckInService;
```

**修改 checkIn 方法**（在 AI 人脸比对代码之后、insertHwAttendance 之前插入）：
```java
// === 公众号打卡：GPS 位置校验 ===
if ("1".equals(hwAttendance.getCheckMethod()) && wechatCheckInService != null) {
    if (StringUtils.isEmpty(hwAttendance.getLocation())) {
        hwAttendance.setCheckStatus("2");
        hwAttendance.setFailReason("公众号打卡需要GPS位置信息");
    } else {
        HwPlan plan = hwPlanMapper.selectHwPlanById(hwAttendance.getPlanId());
        try {
            String[] coords = hwAttendance.getLocation().split(",");
            double checkLng = Double.parseDouble(coords[0].trim());
            double checkLat = Double.parseDouble(coords[1].trim());
            if (plan.getSiteLatitude() != null && plan.getSiteLongitude() != null) {
                boolean inRange = wechatCheckInService.validateLocation(
                    checkLat, checkLng, plan.getSiteLatitude().doubleValue(),
                    plan.getSiteLongitude().doubleValue(), 500);
                if (!inRange) {
                    hwAttendance.setCheckStatus("2");
                    hwAttendance.setFailReason("GPS位置超出施工点500米范围");
                }
            }
        } catch (Exception e) {
            hwAttendance.setCheckStatus("2");
            hwAttendance.setFailReason("GPS坐标格式解析失败");
        }
    }
}
// === GPS位置校验结束 ===
```

> 同样在 checkOut 方法中也加入 GPS 校验代码（复用相同逻辑）。

#### 4.2 新建 WechatAttendanceController.java

路径：`ruoyi-admin/src/main/java/com/ruoyi/web/controller/homework/WechatAttendanceController.java`

```java
package com.ruoyi.web.controller.homework;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.domain.HwAttendance;
import com.ruoyi.system.service.IHwAttendanceService;

@RestController
@RequestMapping("/wechat/attendance")
public class WechatAttendanceController extends BaseController {

    @Autowired
    private IHwAttendanceService hwAttendanceService;

    // 手机端进场打卡
    @PostMapping("/checkIn")
    public AjaxResult checkIn(@RequestBody HwAttendance hwAttendance) {
        try {
            hwAttendance.setCheckType("0");
            return toAjax(hwAttendanceService.checkIn(hwAttendance));
        } catch (ServiceException e) {
            return error(e.getMessage());
        }
    }

    // 手机端离场打卡
    @PostMapping("/checkOut")
    public AjaxResult checkOut(@RequestBody HwAttendance hwAttendance) {
        try {
            hwAttendance.setCheckType("1");
            return toAjax(hwAttendanceService.checkOut(hwAttendance));
        } catch (ServiceException e) {
            return error(e.getMessage());
        }
    }

    // 手机端查询我的打卡记录
    @GetMapping("/myList")
    public TableDataInfo myList() {
        startPage();
        HwAttendance query = new HwAttendance();
        query.setUserId(getUserId());
        return getDataTable(hwAttendanceService.selectHwAttendanceList(query));
    }
}
```

---

### 遗漏 5：从作业计划页跳转打卡记录（需求 #39）⚠️ P0

**当前代码**：
- attendance/index.vue 中 planId 是手动输入框（`el-input-number`），不支持路由参数
- plan/index.vue 操作列只有"修改"和"删除"按钮，无"打卡"入口

**需改动的文件**：

#### 5.1 前端 plan/index.vue — 操作列新增"打卡"按钮

```html
<el-button type="primary" size="mini" icon="el-icon-view"
  @click="handleViewAttendance(scope.row)">打卡</el-button>
```

```javascript
handleViewAttendance(row) {
  this.$router.push({ path: '/homework/attendance', query: { planId: row.planId } });
}
```

#### 5.2 前端 attendance/index.vue — 支持路由参数

**created 中增加**：
```javascript
created() {
  if (this.$route.query.planId) {
    this.queryParams.planId = Number(this.$route.query.planId);
  }
  this.getList();
}
```

---

### 遗漏 6：作业计划录像关联（需求 #38）❌ P1

**当前代码**：全局搜索 `hw_plan_video` — **零结果**，完全未实现

**需新增的文件**：

#### 6.1 数据库：新建 hw_plan_video 表

```sql
DROP TABLE IF EXISTS hw_plan_video;
CREATE TABLE hw_plan_video (
  id          BIGINT(20)   NOT NULL AUTO_INCREMENT  COMMENT '关联ID',
  plan_id     BIGINT(20)   NOT NULL                  COMMENT '作业计划ID',
  record_id   BIGINT(20)   NOT NULL                  COMMENT '录像记录ID',
  record_name VARCHAR(200) DEFAULT ''               COMMENT '录像文件名（冗余）',
  start_time  DATETIME     DEFAULT NULL              COMMENT '录像开始时间（冗余）',
  end_time    DATETIME     DEFAULT NULL              COMMENT '录像结束时间（冗余）',
  create_by   VARCHAR(64)  DEFAULT ''               COMMENT '创建者',
  create_time DATETIME                               COMMENT '创建时间',
  PRIMARY KEY (id),
  INDEX idx_plan_id (plan_id)
) ENGINE=InnoDB COMMENT='作业计划录像关联表';
```

#### 6.2 新建文件

- `HwPlanVideo.java`（Domain，继承 BaseEntity）
- `HwPlanVideoMapper.java` + XML（selectByPlanId / insert / deleteById）
- `IHwPlanVideoService.java` + Impl

#### 6.3 HwPlanController.java — 新增录像关联接口

```java
@GetMapping("/{planId}/videos")
public AjaxResult getVideos(@PathVariable Long planId) { ... }

@PostMapping("/{planId}/videos")
public AjaxResult bindVideo(@PathVariable Long planId, @RequestBody HwPlanVideo pv) { ... }

@DeleteMapping("/{planId}/videos/{id}")
public AjaxResult unbindVideo(@PathVariable Long id) { ... }
```

---

### 遗漏 7：手机端创建作业计划（需求 #36 手机端部分）⚠️ P2

**当前代码**：全局搜索 `WechatPlanController` — **零结果**

**需新建**：`WechatPlanController.java`

```java
@RestController
@RequestMapping("/wechat/plan")
public class WechatPlanController extends BaseController {
    @Autowired private IHwPlanService hwPlanService;

    @PostMapping("/create")
    public AjaxResult create(@Validated @RequestBody HwPlan hwPlan) {
        hwPlan.setCreateBy(SecurityUtils.getUsername());
        hwPlan.setStatus("0");
        return toAjax(hwPlanService.insertHwPlan(hwPlan));
    }

    @GetMapping("/myList")
    public TableDataInfo myList() { ... }
}
```

---

## 三、V2 总文件改动清单

### 新增文件（共 10 个）

| 文件 | 类型 | 说明 |
|------|------|------|
| `HwPlanWorker.java` | Domain | 计划人员关联实体 |
| `HwPlanWorkerMapper.java` | Mapper 接口 | |
| `HwPlanWorkerMapper.xml` | Mapper XML | |
| `IHwPlanWorkerService.java` | Service 接口 | |
| `HwPlanWorkerServiceImpl.java` | Service 实现 | |
| `HwPlanVideo.java` | Domain | 计划录像关联实体 |
| `HwPlanVideoMapper.java` | Mapper 接口 | |
| `HwPlanVideoMapper.xml` | Mapper XML | |
| `IHwPlanVideoService.java` | Service 接口 | |
| `HwPlanVideoServiceImpl.java` | Service 实现 | |
| `WechatAttendanceController.java` | Controller | 手机端打卡 `@RequestMapping("/wechat/attendance")` |
| `WechatPlanController.java` | Controller | 手机端计划 `@RequestMapping("/wechat/plan")` |

### 修改文件（共 10 个）

| 文件 | 改动 |
|------|------|
| `hw_plan` 表 | ALTER 修改 status 注释 |
| `HwPlan.java` | 修改 status 字段注释 |
| `IHwPlanService.java` | 新增 `changeStatus()` |
| `HwPlanServiceImpl.java` | 新增 `changeStatus()` 实现（含流转规则） |
| `HwPlanController.java` | 新增 5 接口：`PUT /changeStatus`, `GET /{planId}/workers`, `POST /{planId}/workers`, `GET /{planId}/videos`, `POST /{planId}/videos` |
| `HwAttendanceServiceImpl.java` | **注入 IFaceRecognitionService + IWechatCheckInService**；checkIn/checkOut 中加入 AI 人脸比对 + GPS 位置校验代码 |
| `HwWorkerMapper.java` | 新增 `selectHwWorkerByUserId()` |
| `HwWorkerMapper.xml` | 新增对应 SQL |
| `plan/index.vue` | status 列改标签+状态按钮；参与人员改选择器；新增"打卡"按钮 |
| `attendance/index.vue` | 支持路由参数 planId |
| `plan.js` | 新增 `getPlanWorkers()`, `savePlanWorkers()` |

### 新建数据库表（共 2 张）

| 表名 | 说明 |
|------|------|
| `hw_plan_worker` | 作业计划人员关联表（plan_id + worker_id + worker_name + role_type） |
| `hw_plan_video` | 作业计划录像关联表（plan_id + record_id + record_name + start/end_time） |

---

## 四、遗漏严重程度排序

| 优先级 | 遗漏项 | 影响 |
|--------|--------|------|
| **P0** | 状态流转（#37） | 作业计划无法标记进行中/已完成/已取消，业务闭环断裂 |
| **P0** | 人员关联（#40） | workers 字段存文本而非结构化数据，无法按角色筛选关联人员 |
| **P0** | 打卡入口跳转（#39） | 无法从计划页一键查看该计划的打卡记录 |
| **P1** | AI人脸接入（#48） | IFaceRecognitionService 定义但未使用，checkIn 缺失人脸比对流程 |
| **P1** | GPS校验接入（#49） | validateLocation 已实现 Haversine 但未使用，无手机端 Controller |
| **P1** | 录像关联（#38） | 完全未实现 |
| **P2** | 手机端创建（#36） | 仅 PC 端可创建计划 |
