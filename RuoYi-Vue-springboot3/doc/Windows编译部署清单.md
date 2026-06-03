# Windows 编译部署 uni-app 手机前端 —— 操作清单

## 前提

- 确保 WSL 里后端已在跑：`curl http://localhost:8080` 返回 200
- 确保 WSL 里前端已在跑：`curl http://localhost:8081` 返回 200

---

## 第一步：安装 HBuilderX

1. 打开浏览器 → https://www.dcloud.io/hbuilderx.html
2. 下载 **Windows 标准版**（免费）
3. 解压到任意目录（如 `D:\HBuilderX`），双击 `HBuilderX.exe` 打开

---

## 第二步：打开项目

1. HBuilderX 菜单栏 → 文件 → 打开目录
2. 在地址栏输入：`\\wsl$\Ubuntu\home\zzz\project\RuoYi-Vue-springboot3\RuoYi-App`
3. 点「选择文件夹」
4. 左侧项目树应显示 `pages`、`config.js`、`App.vue` 等文件

---

## 第三步：修改配置

1. 左侧项目树 → 双击 `config.js`
2. 找到 `baseUrl` 这一行，改成：

```js
// 本地开发
baseUrl: 'http://localhost:8080',

// 部署到服务器时改成：baseUrl: 'https://你的域名'
```

3. 保存（Ctrl+S）

---

## 第四步：编译 H5

1. HBuilderX 顶部菜单 → 运行 → 运行到浏览器 → Chrome
2. 等待编译完成（左下角有进度）
3. 编译成功后浏览器自动打开 H5 页面
4. 编译产物在项目目录下的 `dist/build/h5/` 文件夹

> 如果报错，先确认 HBuilderX 是**以管理员身份运行**

---

## 第五步：部署到服务器

把 `dist/build/h5/` 复制到服务器 Nginx 目录下：

```bash
# 在服务器上
scp -r dist/build/h5/* root@你的服务器IP:/usr/share/nginx/html/
```

或者直接在 Windows 上用 FTP 工具上传。

Nginx 配置示例（需配 HTTPS）：

```nginx
server {
    listen 443 ssl;
    server_name worker.你的域名.com;
    ssl_certificate /etc/ssl/xxx.pem;
    ssl_certificate_key /etc/ssl/xxx.key;

    root /usr/share/nginx/html;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }
}
```

> `index.html` 就是编译出来的入口文件，打开就是手机前端。

---

## 验证

- 浏览器打开 `https://worker.你的域名.com` → 应看到登录页面
- 用手机号 + 身份证后6位登录 → 能打卡、上传、查看记录 → ✅

---

## 常见问题

| 问题 | 解决 |
|------|------|
| 编译报错 | 关掉杀毒软件，以管理员身份运行 HBuilderX |
| 页面空白 | 检查 `config.js` 的 `baseUrl` 是否指向正确的后端地址 |
| 无法登录 | 后端 `/app/auth/login` 是否正常；Redis 是否启动 |
| 上传失败 | 后端 `application.yml` 的 `ruoyi.profile` 路径是否正确 |
