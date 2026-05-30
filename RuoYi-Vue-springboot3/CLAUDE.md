# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目对话语言

在与本项目的所有对话中，请使用中文进行回复和交流。

---

## 行为准则

**1. 先思考再编码** — 陈述假设，如果不确定就提问。如果存在多种解释，呈现出来——不要默默选择。如果有更简单的方法，说出来。

**2. 简洁优先** — 用最少的代码解决问题，不做多余的抽象。不处理不可能发生的错误场景。如果 200 行的代码可以写成 50 行，重写它。

**3. 精准修改** — 只改动你必须改的。不要"改进"相邻代码、注释或格式。不要重构没有被破坏的东西。匹配已有代码风格。

**4. 目标驱动执行** — 把任务转化为可验证的目标，定义成功标准，循环直到验证通过。

---

## 构建与运行命令

### 后端（Spring Boot 3.5.x + JDK 17）

```bash
# Maven 编译（跳过测试，建议开启代理）
mvn clean install -DskipTests

# 启动后端服务（开发环境）
cd ruoyi-admin
mvn spring-boot:run

# 或者直接运行主类
# ruoyi-admin/src/main/java/com/ruoyi/RuoYiApplication.java
```

后端默认运行在 **8080** 端口。

### 前端（Vue 2.x + Element UI）

```bash
cd ruoyi-ui
npm install                          # 安装依赖（建议用 npmmirror 源）
npm run dev                          # 启动开发服务器（端口 80）
npm run build:prod                   # 生产构建
npm run build:stage                  # 测试环境构建
```

浏览器打开 `http://localhost` 访问前端。

### 环境依赖

| 组件 | 用途 | 默认端口 |
|------|------|---------|
| MySQL 8.x | 数据库 `ry-vue` | 3306 |
| Redis | 缓存/Token/验证码 | 6379 |

SQL 初始化文件位于 `sql/` 目录：
- `ry_20260417.sql` — 系统核心表数据
- `quartz.sql` — 定时任务表

### Git 代理

项目已配置 HTTP 代理 `127.0.0.1:7897`（`git config http.proxy` / `https.proxy`）。

### 默认账号

- 管理员：`admin` / `admin123`
- 普通用户：`ry` / `123456`
- Druid 监控面板：`ruoyi` / `123456`（访问 `/druid/`）

---

## 架构概览

本项目是 RuoYi（若依）v3.9.2 SpringBoot3 分支，基于 Spring Boot 3.5.x + Vue 2.x 前后端分离的 Java 快速开发框架。采用 Maven 多模块 + RBAC 权限模型。

### 模块分层（6 个 Maven 模块）

```
ruoyi-admin        [主入口] Controller 层、Spring Boot 启动、配置文件（YAML）
     ↓ 依赖
ruoyi-framework    [框架] Spring Security 6 认证鉴权、AOP 切面（日志/权限/防重/数据源切换）、多数据源
     ↓ 依赖
ruoyi-system       [业务] 实体 Domain、MyBatis Mapper、Service（核心 CRUD 逻辑）
     ↓ 依赖
ruoyi-common       [公共] 注解（@Log/@RepeatSubmit/@Sensitive 等）、工具类、枚举、XSS/Referer 过滤器、通用异常

ruoyi-quartz       [定时任务] 独立的 Quartz 调度模块，Controller 挂载在 /monitor/job
ruoyi-generator    [代码生成] 独立的代码生成器模块，Controller 挂载在 /tool/gen，基于 Velocity 模板
```

### 关键配置文件位置

| 文件 | 说明 |
|------|------|
| `ruoyi-admin/.../application.yml` | 主配置：端口、Redis、Token、XSS、SpringDoc、MyBatis、PageHelper |
| `ruoyi-admin/.../application-druid.yml` | 数据源：MySQL 连接、Druid 连接池、主从配置、慢SQL监控 |

### 后端核心模式

- **基类**：所有 Controller 继承 `BaseController`，提供分页、权限、日志等通用能力
- **统一响应**：`AjaxResult`（普通响应，`{code, msg, data}`）和 `TableDataInfo`（分页响应，`{code, msg, rows, total}`）
- **认证**：JWT Token，通过 `Authorization` 请求头传递，在 `ruoyi-framework/.../TokenService.java` 中签发和校验，默认 30 分钟过期
- **权限**：Spring Security 6（`SecurityFilterChain` Bean + `@EnableMethodSecurity`），方法级权限 `@PreAuthorize("@ss.hasPermi('system:user:list')")`，权限字符串格式为 `模块:实体:操作`，`ss` 是 `PermissionService` 的 Bean 名
- **数据权限**：`@DataScope` 注解 + `DataScopeAspect` AOP 切面，在 SQL 执行前动态注入 `WHERE` 条件限制数据范围
- **日志**：自定义 `@Log` 注解 + `LogAspect` AOP 自动记录到 `sys_oper_log` 表
- **防重提交**：`@RepeatSubmit` 注解 + `RepeatSubmitInterceptor` Redis 缓存拦截
- **限流**：`@RateLimiter` 注解 + `RateLimiterAspect` 基于 Redis 的令牌桶算法
- **全局异常**：`GlobalExceptionHandler` 统一拦截 `@RestControllerAdvice`，处理 `ServiceException`、校验异常等

### 前端核心模式

- `src/utils/request.js`：Axios 实例封装，自动注入 `Authorization` Token，统一处理 401 跳转登录页
- `src/api/`：API 请求模块，按 `system/`、`monitor/`、`tool/` 分子目录，与后端 Controller 一一对应
- `src/store/`：Vuex 模块（user、permission、app 等）
- `src/router/`：动态路由，由 `/getRouters` 接口根据用户权限返回菜单树
- `src/views/`：页面组件，目录结构对应路由
- `@/` 别名映射到 `src/`（vue.config.js 中配置）

### 前后端接口映射规则

Controller 的 `@RequestMapping` 路径在 Vue CLI devServer 中通过代理转发到后端：

- 前端调用 `/login` → 代理到后端 `POST /login`
- 前端调用 `/system/user/list` → 代理到后端 `GET /system/user/list`

代理配置在 `ruoyi-ui/vue.config.js` 中，`VUE_APP_BASE_API` 环境变量控制代理前缀。
