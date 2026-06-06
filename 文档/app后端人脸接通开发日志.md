# uni-app 打卡人脸识别集成开发日志

## 基本信息
- 开发日期：2026-06-06
- 模块名称：uni-app 打卡人脸识别（app → AI 推理）
- 后端框架：Spring Boot 3.5.x + MyBatis + Spring Security 6 + JWT
- AI 框架：ONNX Runtime 1.18.0（YOLOv12 + ArcFace）
- 前端框架：uni-app (Vue 2.6.14)
- 数据库：MySQL 8.x，库名 `ry-vue`
- 开发者：AI 自动生成

---

## 一、需求背景

### 1.1 现状问题

uni-app 打卡页面（`checkin.vue`）的签到/签退流程：

```
用户点击"签到" → 拍照上传 → POST /app/checkin/signIn → 直接写入打卡记录 → "签到成功"
```

**没有任何人脸验证**，上传任意图片都能打卡成功。

### 1.2 改造目标

```
用户点击"签到" → 拍照上传 → POST /app/checkin/signIn
  → YOLOv12 检测人脸 → ArcFace 提取 512 维特征 → 1:N 底库比对
  → 校验匹配 workerId 是否为当前登录用户
  → 通过 → 写入打卡记录 → "签到成功，人脸验证通过"
  → 不通过 → 拒绝 → "人脸验证失败..."
```

### 1.3 与现有系统的关系

- `HwAttendanceController`（PC 端作业打卡）已通过 `RealFaceRecognitionService.compareFace()` 走人脸识别链路。
- 本次改造让 uni-app 的 `AppCheckinController` 也接入同一套 `IFaceRecognitionService` → `IAiInferenceService` → ONNX 推理链路。
- 人脸底库复用已有的 `ai_face_register` 表（PC 端「AI 算法仓 → 人脸注册」录入）。

---

## 二、后端文件修改详述（3 个文件）

### 2.1 FaceMatchResult.java — 新增 workerId 字段

**文件**：`ruoyi-common/src/main/java/com/ruoyi/common/service/FaceMatchResult.java`

**改动**：

| 项目 | 说明 |
|------|------|
| 新增字段 | `private Long workerId;` |
| 新增 getter | `getWorkerId()` |
| 新增 setter | `setWorkerId(Long)` |
| 新增工厂方法 | `success(double confidence, Long workerId)` — 同时设置 matched=true + confidence + workerId |

**设计要点**：原有 `success(double confidence)` 方法保留，向后兼容（打卡比对场景不需要 workerId 的调用方不受影响）。新工厂方法供 `RealFaceRecognitionService` 透传 AI 推理返回的 `workerId`。

### 2.2 RealFaceRecognitionService.java — 补齐 workerId + 兼容 URL 入参

**文件**：`ruoyi-framework/src/main/java/com/ruoyi/framework/service/RealFaceRecognitionService.java`

#### 改动 1：compareFace() 解析 workerId

```java
// 修改前
FaceMatchResult faceResult = FaceMatchResult.success(confidence);
faceResult.setPersonName(personName);

// 修改后
Long workerId = map.get("workerId") != null ? ((Number) map.get("workerId")).longValue() : null;
FaceMatchResult faceResult = FaceMatchResult.success(confidence, workerId);
faceResult.setPersonName(personName);
```

AI 推理返回的 JSON 中 `workerId` 是底库匹配到的人员 ID，上层 Controller 用此值校验打卡照片是否与当前用户一致。

#### 改动 2：resolveToPath() 兼容 URL/相对路径

```java
// 修改前：只支持物理路径和 Base64
private String resolveToPath(String input) throws IOException {
    if (input == null || input.isEmpty()) throw new IOException("输入图片为空");
    if (new File(input).exists()) return input;
    return base64ToTempFile(input);
}

// 修改后：新增 HTTP URL 和 /profile 路径解析
private String resolveToPath(String input) throws IOException {
    if (input == null || input.isEmpty()) throw new IOException("输入图片为空");
    // 完整 HTTP(S) URL → 提取路径部分递归解析
    if (input.startsWith("http://") || input.startsWith("https://")) {
        String path = new java.net.URI(input).getPath();
        if (path != null && !path.isEmpty()) return resolveToPath(path);
    }
    // /profile/xxx 相对路径 → 拼接物理路径
    if (input.startsWith("/profile")) {
        String relativePath = input.replaceFirst("^/profile/?", "");
        input = RuoYiConfig.getProfile() + File.separator + relativePath;
    }
    if (new File(input).exists()) return input;
    return base64ToTempFile(input);
}
```

**为什么需要兼容 URL**：uni-app 通过 `/common/upload` 上传照片后，返回的 `url` 和 `fileName` 可能是完整 HTTP URL（`http://localhost:8080/profile/upload/...`）或相对路径（`/profile/upload/...`）。`compareFace()` 被 `AppCheckinController` 调用时，入参 `capturedImage` 就是这些值，必须能正确解析为物理文件路径。

### 2.3 AppCheckinController.java — 注入人脸识别 + 打卡前验证

**文件**：`ruoyi-admin/src/main/java/com/ruoyi/web/controller/app/AppCheckinController.java`

#### 改动 1：新增依赖注入

```java
@Autowired private com.ruoyi.common.service.IFaceRecognitionService faceRecognitionService;
```

#### 改动 2：在 doCheck() 中插入人脸验证

插入位置：`photoUrl` 非空校验之后、查今日记录之前。

```java
// 人脸识别验证
try {
    FaceMatchResult fr = faceRecognitionService.compareFace(photoUrl, null);
    if (!fr.isMatched()) {
        return AjaxResult.error("人脸验证失败，未在底库中找到匹配人员，请先在PC端「AI算法仓→人脸注册」录入人脸");
    }
    if (fr.getWorkerId() == null || !fr.getWorkerId().equals(wid)) {
        String hint = fr.getPersonName() != null && !fr.getPersonName().isEmpty()
            ? "（识别为：" + fr.getPersonName() + "）" : "";
        return AjaxResult.error("人脸验证不通过，打卡照片与本人不符" + hint);
    }
} catch (ServiceException e) {
    return AjaxResult.error(e.getMessage());
} catch (Exception e) {
    return AjaxResult.error("人脸识别服务异常，请稍后重试");
}
```

**业务规则**：

| 场景 | 返回提示 |
|------|---------|
| 底库无匹配 | `"人脸验证失败，未在底库中找到匹配人员，请先在PC端「AI算法仓→人脸注册」录入人脸"` |
| 匹配到其他人 | `"人脸验证不通过，打卡照片与本人不符（识别为：XXX）"` |
| 匹配到本人 | 通过，继续写入打卡记录 |
| AI 服务异常 | `"人脸识别服务异常，请稍后重试"` |
| 照片中无脸 | ServiceException `"未检测到人脸"`（由 `RealAiInferenceService` 抛出） |

**执行顺序**：Token 校验 → 角色打卡规则校验 → photoUrl 非空 → **人脸验证** → 今日重复校验 → 写入。

---

## 三、前端文件修改详述（1 个文件）

### 3.1 checkin.vue — 修正 URL 取值 + 优化提示

**文件**：`RuoYi-App/pages/worker/checkin.vue`

#### 改动 1：captureCheckinPhoto() 中 URL 取值修正

```javascript
// 修改前
const url = result.url || result.data || result.fileName

// 修改后
const url = result.fileName || result.url || (result.data && result.data.url)
```

**原因**：`/common/upload` 返回的 `result.url` 是完整 HTTP URL（`http://localhost:8080/profile/upload/...`），存入数据库不干净。优先取 `result.fileName`（相对路径 `/profile/upload/...`），后端 `RealFaceRecognitionService.resolveToPath()` 已兼容两种格式。

#### 改动 2：错误提示优化

```javascript
// 修改前
uni.showToast({ title: (res && res.data && res.data.msg) || '失败', icon: 'none' })

// 修改后
uni.showToast({ title: (res && res.data && res.data.msg) || '失败', icon: 'none', duration: 3000 })
```

`duration` 从默认 1500ms 增加到 3000ms，确保人脸验证失败的长文本提示有足够时间阅读。成功提示追加"人脸验证通过"字样。

---

## 四、调用链路

```
uni-app 打卡页面 (checkin.vue)
  │
  ├── 拍照 → uni.chooseImage()
  ├── 上传 → POST /common/upload
  │         返回 { fileName: "/profile/upload/YYYY/MM/DD/xxx.jpg", url: "http://..." }
  │
  └── 打卡 → POST /app/checkin/signIn { photoUrl, latitude, longitude }
              │
              ▼
        AppCheckinController.doCheck()
              │
              ├── 1. Token 校验 + 角色打卡规则校验
              ├── 2.【新增】faceRecognitionService.compareFace(photoUrl, null)
              │       │
              │       ▼
              │   RealFaceRecognitionService.compareFace()
              │       │
              │       ├── resolveToPath(photoUrl)
              │       │   ├── http://... → URI.getPath() → 提取路径
              │       │   ├── /profile/... → RuoYiConfig.getProfile() + relativePath
              │       │   ├── 物理路径已存在 → 直接返回
              │       │   └── Base64 → 写临时文件
              │       │
              │       └── aiInferenceService.infer("arcface_r100_001", path)
              │             │
              │             ▼
              │         RealAiInferenceService.doFaceRecognition()
              │             ├── YOLOv12 (yolov12m-face.onnx 80MB) → 检测人脸
              │             ├── ArcFace (w600k_mbf.onnx 13MB) → 提取 512 维特征
              │             ├── 1:N 遍历 ai_face_register 底库 (status='1')
              │             └── 返回 { matched, workerId, personName, similarity }
              │
              ├── 3. fr.isMatched() && fr.getWorkerId() == currentWorkerId → 通过
              ├── 4. 不满足 → AjaxResult.error(...)
              └── 5. 通过 → 写入 tb_worker_checkin → success
```

---

## 五、人脸底库要求

签到前必须先完成人脸录入（PC 端「AI 算法仓 → 人脸注册 → 录入」），确保：

- `tb_worker_face` 中有当前人员的 `face_img_url`（人脸照片路径）
- `ai_face_register` 中有 `worker_id` 关联且 `register_status='1'` 的记录
- `ai_face_register.face_feature` 有有效的 512 维特征向量（Base64 编码 JSON）

否则 1:N 匹配不会命中当前用户，打卡会被拒绝。

---

## 六、验证清单

| # | 验证项 | 验证方法 | 状态 |
|---|--------|---------|------|
| 1 | 后端编译 | `mvn compile -DskipTests` → `BUILD SUCCESS` | ✅ |
| 2 | 已录入人脸人员签到 | 手机端签到 → 拍照 → 提示"签到成功，人脸验证通过" → `tb_worker_checkin` 有记录 | 待启动验证 |
| 3 | 未录入人脸人员签到 | 手机端签到 → 拍照 → 提示"未在底库中找到匹配人员" | 待启动验证 |
| 4 | 他人照片冒签 | 人员 A 登录，用人员 B 照片签到 → 提示"打卡照片与本人不符（识别为：B）" | 待启动验证 |
| 5 | 无脸照片签到 | 对天花板拍照签到 → 提示"未检测到人脸" | 待启动验证 |
| 6 | 签退同样验证 | 签退操作同样经过人脸验证 | 待启动验证 |
| 7 | 重复签到拦截 | 今日已签到 → 再次签到 → 提示"今日已签到，请勿重复提交"（人脸验证前拦截） | 待启动验证 |
| 8 | AI 服务未就绪 | 停掉模型后签到 → 提示"AI推理服务未就绪" | 待启动验证 |

---

## 七、修改文件清单

| # | 文件 | 类型 | 改动要点 |
|---|------|------|---------|
| 1 | `ruoyi-common/.../service/FaceMatchResult.java` | 修改 | 新增 `workerId` 字段 + getter/setter + 带 `workerId` 的 `success()` 工厂方法 |
| 2 | `ruoyi-framework/.../service/RealFaceRecognitionService.java` | 修改 | `compareFace()` 解析 `workerId` 并使用新工厂方法；`resolveToPath()` 新增 HTTP URL 和 `/profile` 路径解析 |
| 3 | `ruoyi-admin/.../controller/app/AppCheckinController.java` | 修改 | 注入 `IFaceRecognitionService`；`doCheck()` 中新增人脸验证逻辑 |
| 4 | `RuoYi-App/pages/worker/checkin.vue` | 修改 | `captureCheckinPhoto()` 优先取 `result.fileName`；错误 toast duration 延长到 3000ms；成功提示追加"人脸验证通过" |

### 未改动的相关文件

| 文件 | 原因 |
|------|------|
| `IFaceRecognitionService.java` | 接口签名不变，只增强实现 |
| `IAiInferenceService.java` | 接口已足够，不需要新增方法 |
| `RealAiInferenceService.java` | Bug #2 修复中已完善（URL 解析 + 前导 `/` 修正），本次无需再改 |
| `AiFaceRegisterServiceImpl.java` | 人脸录入流程不变 |
| `tb_worker_checkin` 表 | 无需变更，打卡记录只存 `photo_url`，人脸验证结果不单独存储 |
| `ai_face_register` 表 | 已存在，作为 1:N 比对的底库 |

---

> 开发日期：2026-06-06
> 基于：
> - `appToyolo.md` — uni-app 打卡人脸识别集成规格书
> - `yoloTojava开发日志.md` — YOLOv12 + ArcFace ONNX 模型 Java 集成
> - `AI算法仓开发日志.md` — AI 算法仓模块（人脸注册/模型管理）
