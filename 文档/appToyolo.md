# uni-app 打卡人脸识别集成规格书

> **请自动完成本文档中的所有开发任务，无需逐步确认。** 按照以下规格修改代码后，报告完成情况。

---

## 一、项目信息

| 项目 | RuoYi-App uni-app 移动端 + 后端 AI 推理 |
|------|------|
| uni-app 路径 | `C:\Users\ASUS\Desktop\RuoYi-Vue-springboot3-main\RuoYi-App` |
| 后端路径 | `C:\Users\ASUS\Desktop\RuoYi-Vue-springboot3-main\RuoYi-Vue-springboot3` |
| 框架 | uni-app (Vue 2.6.14) + Spring Boot 3.5.x + MyBatis + ONNX Runtime 1.18.0 |
| AI 模型 | YOLOv12 人脸检测 (`yolov12m-face.onnx`) + ArcFace 特征提取 (`w600k_mbf.onnx`) |
| 数据库 | MySQL 8.x，库名 `ry-vue` |

---

## 二、需求概述

### 2.1 现状问题

当前 uni-app 打卡页面（`checkin.vue`）的签到/签退流程：

```
用户点击"签到" → 拍照上传 → POST /app/checkin/signIn → 直接写入打卡记录 → 提示"签到成功"
```

**没有任何人脸验证**，上传任意图片都能打卡成功，形同虚设。

### 2.2 改造目标

```
用户点击"签到" → 拍照上传 → POST /app/checkin/signIn
  → YOLOv12 检测人脸 → ArcFace 提取特征 → 1:N 底库比对
  → 校验匹配人员是否为当前登录用户
  → 通过 → 写入打卡记录 → "签到成功，人脸验证通过"
  → 不通过 → 拒绝 → "人脸验证失败，请确认是本人操作"
```

### 2.3 改造范围

| 端 | 改动 | 说明 |
|----|------|------|
| 后端 | `AppCheckinController.doCheck()` | 注入人脸识别服务，打卡前做人脸比对 |
| 后端 | `FaceMatchResult.java` | 新增 `workerId` 字段 |
| 后端 | `RealFaceRecognitionService.java` | `compareFace()` 补齐 `workerId`、兼容 URL 入参 |
| 前端 | `checkin.vue` | 修正上传 URL 取值、优化人脸验证失败的提示 |

---

## 三、业务规则

### 3.1 打卡人脸验证流程

```
1. 用户签到/签退 → 拍照上传到 /common/upload → 拿到 photoUrl
2. POST /app/checkin/signIn { photoUrl, latitude, longitude }
3. Controller 校验 Token → 校验角色打卡规则（不变）
4. 【新增】调用 IFaceRecognitionService.compareFace(photoUrl, null)
   → 内部 YOLOv12 检测 + ArcFace 提取特征 + 1:N 底库比对
   → 返回 FaceMatchResult { matched, workerId, personName, confidence }
5. 【新增】校验匹配结果：
   a. !matched → 拒绝："人脸验证失败，未在底库中找到匹配人员，请先在人脸注册页面录入人脸"
   b. matched 但 workerId != 当前用户 → 拒绝："人脸验证不通过，打卡照片与本人不符（识别为：{personName}）"
   c. matched 且 workerId == 当前用户 → 通过
6. 写入 tb_worker_checkin → 返回成功
```

### 3.2 人脸未录入的降级处理

如果底库中没有当前用户的人脸特征（`tb_worker_face.face_img_url` 为空 或 `ai_face_register` 中无 `register_status='1'` 的记录），`compareFace()` 调用 `infer()` 做 1:N 比对，此时最佳匹配不会是当前用户。Controller 收到 `!matched` 或 `workerId` 不匹配，统一返回错误提示让用户先去录入人脸。

### 3.3 异常处理

- AI 模型未加载 → `ServiceException("AI推理服务未就绪，请联系管理员")`
- 照片中无人脸 → `ServiceException("未检测到人脸，请正对摄像头拍照")`
- 照片 URL 解析失败 → `ServiceException("打卡照片读取失败，请重新拍照上传")`

---

## 四、你需要创建/修改的全部内容

### 第一部分：后端 — 修改现有文件（4 个）

#### 1.1 FaceMatchResult.java — 新增 workerId 字段

**文件**：`ruoyi-common/src/main/java/com/ruoyi/common/service/FaceMatchResult.java`

新增字段和 getter/setter：

```java
private Long workerId;

public Long getWorkerId() { return workerId; }
public void setWorkerId(Long workerId) { this.workerId = workerId; }
```

新增带 workerId 的工厂方法：

```java
public static FaceMatchResult success(double confidence, Long workerId)
{
    FaceMatchResult result = new FaceMatchResult();
    result.setMatched(true);
    result.setConfidence(confidence);
    result.setWorkerId(workerId);
    return result;
}
```

> 原有 `success(double confidence)` 方法保留，向后兼容。

#### 1.2 RealFaceRecognitionService.java — 补齐 workerId + 兼容 URL 入参

**文件**：`ruoyi-framework/src/main/java/com/ruoyi/framework/service/RealFaceRecognitionService.java`

##### 1.2.1 修改 compareFace() — 解析结果时读取 workerId

```java
// 修改前（约第 55-65 行）
Boolean matched = (Boolean) map.get("matched");
if (matched == null || !matched) {
    return FaceMatchResult.fail("人脸不匹配，未在底库中找到该人员");
}
double confidence = ((Number) map.getOrDefault("similarity", 0)).doubleValue();
String personName = (String) map.getOrDefault("personName", "");
FaceMatchResult faceResult = FaceMatchResult.success(confidence);
faceResult.setPersonName(personName);

// 修改后
Boolean matched = (Boolean) map.get("matched");
if (matched == null || !matched) {
    return FaceMatchResult.fail("人脸不匹配，未在底库中找到该人员");
}
double confidence = ((Number) map.getOrDefault("similarity", 0)).doubleValue();
String personName = (String) map.getOrDefault("personName", "");
Long workerId = map.get("workerId") != null ? ((Number) map.get("workerId")).longValue() : null;
FaceMatchResult faceResult = FaceMatchResult.success(confidence, workerId);
faceResult.setPersonName(personName);
```

##### 1.2.2 修改 resolveToPath() — 兼容 URL/相对路径

```java
// 修改前
private String resolveToPath(String input) throws IOException {
    if (input == null || input.isEmpty()) {
        throw new IOException("输入图片为空");
    }
    if (new File(input).exists()) {
        return input;
    }
    return base64ToTempFile(input);
}

// 修改后
private String resolveToPath(String input) throws IOException {
    if (input == null || input.isEmpty()) {
        throw new IOException("输入图片为空");
    }
    // 完整 HTTP(S) URL → 提取路径部分后解析
    if (input.startsWith("http://") || input.startsWith("https://")) {
        try {
            String path = new java.net.URI(input).getPath();
            if (path != null && !path.isEmpty()) {
                return resolveToPath(path);  // 递归解析路径部分
            }
        } catch (Exception ignored) {}
    }
    // /profile/xxx 相对路径 → 拼接物理路径
    if (input.startsWith("/profile")) {
        String relativePath = input.replaceFirst("^/profile/?", "");
        input = com.ruoyi.common.config.RuoYiConfig.getProfile()
                + java.io.File.separator + relativePath;
    }
    if (new File(input).exists()) {
        return input;
    }
    return base64ToTempFile(input);
}
```

#### 1.3 AppCheckinController.java — 注入人脸识别 + 打卡前验证

**文件**：`ruoyi-admin/src/main/java/com/ruoyi/web/controller/app/AppCheckinController.java`

##### 1.3.1 新增依赖注入

```java
@Autowired
private com.ruoyi.common.service.IFaceRecognitionService faceRecognitionService;
```

##### 1.3.2 在 doCheck() 方法中，规则校验之后、写库之前，插入人脸验证

```java
// ========== 新增：人脸识别验证 ==========
try {
    com.ruoyi.common.service.FaceMatchResult fr = faceRecognitionService.compareFace(photoUrl, null);
    if (!fr.isMatched()) {
        return AjaxResult.error("人脸验证失败，未在底库中找到匹配人员，请先在PC端「AI算法仓→人脸注册」录入人脸");
    }
    if (fr.getWorkerId() == null || !fr.getWorkerId().equals(wid)) {
        String hint = fr.getPersonName() != null && !fr.getPersonName().isEmpty()
            ? "（识别为：" + fr.getPersonName() + "）" : "";
        return AjaxResult.error("人脸验证不通过，打卡照片与本人不符" + hint);
    }
} catch (com.ruoyi.common.exception.ServiceException e) {
    return AjaxResult.error(e.getMessage());
} catch (Exception e) {
    return AjaxResult.error("人脸识别服务异常，请稍后重试");
}
// ========== 人脸验证结束 ==========
```

插入位置：在 `photoUrl.trim().isEmpty()` 校验之后，`TbWorkerCheckin q = ...` 查今日记录之前。

---

### 第二部分：前端 — 修改 uni-app 页面（1 个）

#### 2.1 checkin.vue — 修正上传 URL 取值 + 优化错误提示

**文件**：`RuoYi-App/pages/worker/checkin.vue`

##### 2.1.1 修正 captureCheckinPhoto() 中的 URL 取值（同 face.vue 修复）

```javascript
// 修改前（第 86 行）
const url = result.url || result.data || result.fileName

// 修改后
const url = result.fileName || result.url || (result.data && result.data.url)
```

##### 2.1.2 优化 doCheck() 中的人脸验证失败提示

```javascript
// 修改前（第 138-139 行）
if (res && res.data && res.data.code === 200) { uni.showToast({ title: action === 'signIn' ? '签到成功' : '签退成功' }); ... }
else { uni.showToast({ title: (res && res.data && res.data.msg) || '失败', icon: 'none' }) }

// 修改后
if (res && res.data && res.data.code === 200) {
  uni.showToast({ title: (action === 'signIn' ? '签到成功' : '签退成功') + '，人脸验证通过' });
  this.photoUrl = ''; this.refresh();
} else {
  const msg = (res && res.data && res.data.msg) || '失败';
  uni.showToast({ title: msg, icon: 'none', duration: 3000 });
}
```

---

## 五、验证清单

| # | 验证项 | 验证方法 |
|---|--------|---------|
| 1 | 后端编译 | `mvn compile -DskipTests` → `BUILD SUCCESS` |
| 2 | 已录入人脸人员签到 | 手机端签到 → 拍照 → 提交 → 提示"签到成功，人脸验证通过" → `tb_worker_checkin` 有记录 |
| 3 | 未录入人脸人员签到 | 手机端签到 → 拍照 → 提交 → 提示"未在底库中找到匹配人员" |
| 4 | 他人照片冒签 | 人员 A 登录，用人员 B 的照片签到 → 提示"打卡照片与本人不符" |
| 5 | 无人脸照片签到 | 对天花板拍照签到 → 提示"未检测到人脸" |
| 6 | 签退同样验证 | 签退操作同样经过人脸验证 |
| 7 | 已签到重复签到拦截 | 今日已签到 → 再次签到 → 提示"今日已签到，请勿重复提交"（在人脸验证之前拦截） |
| 8 | AI 服务未就绪 | 停掉模型后签到 → 提示"AI推理服务未就绪" |

---

## 六、总文件改动清单

| # | 文件 | 类型 | 说明 |
|---|------|------|------|
| 1 | `ruoyi-common/.../service/FaceMatchResult.java` | 修改 | 新增 `workerId` 字段 + 带 `workerId` 的工厂方法 |
| 2 | `ruoyi-framework/.../service/RealFaceRecognitionService.java` | 修改 | 补齐 workerId 解析 + `resolveToPath()` 兼容 URL |
| 3 | `ruoyi-admin/.../controller/app/AppCheckinController.java` | 修改 | 注入人脸识别 + doCheck() 中增加验证 |
| 4 | `RuoYi-App/pages/worker/checkin.vue` | 修改 | 修正 URL 取值 + 优化提示 |

### 不改动的文件

| 文件 | 原因 |
|------|------|
| `IAiInferenceService.java` | 接口已足够，不需要新增方法 |
| `IFaceRecognitionService.java` | 接口签名不变，只增强实现 |
| `RealAiInferenceService.java` | 已在 Bug #2 修复中完善，本次无需再改 |
| `AiFaceRegisterServiceImpl.java` | 人脸录入流程不变 |
| 数据库表 | 无需变更，使用已有的 `tb_worker_checkin`、`ai_face_register`、`tb_worker_face` |

---

## 七、调用链路

```
uni-app 打卡页面 (checkin.vue)
  │
  ├── 拍照 → uni.chooseImage()
  ├── 上传 → POST /common/upload → 文件存入 uploadPath/upload/YYYY/MM/DD/xxx.jpg
  │         返回 { fileName: "/profile/upload/...", url: "http://..." }
  │
  └── 打卡 → POST /app/checkin/signIn { photoUrl: "/profile/upload/...", ... }
              │
              ▼
        AppCheckinController.doCheck()
              │
              ├── 1. Token 校验 + 角色打卡规则校验（不变）
              ├── 2.【新增】IFaceRecognitionService.compareFace(photoUrl, null)
              │       │
              │       ▼
              │   RealFaceRecognitionService.compareFace()
              │       │
              │       ├── resolveToPath() → URL/Base64 → 物理文件
              │       ├── IAiInferenceService.infer("arcface_r100_001", path)
              │       │       │
              │       │       ▼
              │       │   RealAiInferenceService.doFaceRecognition()
              │       │       ├── YOLOv12 检测人脸
              │       │       ├── ArcFace 提取 512 维特征
              │       │       ├── 1:N 遍历 ai_face_register 底库（status='1'）
              │       │       └── 返回最佳匹配 { matched, workerId, personName, similarity }
              │       │
              │       └── 返回 FaceMatchResult { matched, workerId, personName, confidence }
              │
              ├── 3. matched=true && workerId==当前用户 → 通过
              ├── 4. 否则 → 返回错误
              └── 5. 写入 tb_worker_checkin → 返回成功
```

---

## 八、注意事项

### 8.1 与 HwAttendance 打卡的关系

`HwAttendanceController`（PC 端作业打卡）已通过 `RealFaceRecognitionService.compareFace()` 走人脸识别。本次改造让 uni-app 的 `AppCheckinController` 也接入同一套识别链路，两端统一。

### 8.2 人脸底库要求

签到前必须先完成人脸录入（PC 端「AI 算法仓 → 人脸注册 → 录入」），即 `ai_face_register` 表中有 `register_status='1'` 且 `face_feature` 有有效的特征向量。否则 1:N 匹配不会命中当前用户。

### 8.3 性能预估

单次打卡人脸验证耗时 = YOLOv12 检测（~300ms） + ArcFace 特征提取（~100ms） + 底库遍历比对（随注册人数线性增长，每人 ~0.1ms）。百人级底库总耗时约 500ms。

---

> 规格书编写日期：2026-06-06
> 基于：
> - `yoloTojava开发日志.md` — YOLOv12 + ArcFace ONNX 模型 Java 集成
> - `AI算法仓开发日志.md` — AI 算法仓模块（人脸注册/模型管理）
> - `yoloTojavaV2.md` — YOLOv12 + ArcFace 多人脸识别改造 V2
> - `firstHomework.md` — 作业管理模块开发规格书（格式参考）
