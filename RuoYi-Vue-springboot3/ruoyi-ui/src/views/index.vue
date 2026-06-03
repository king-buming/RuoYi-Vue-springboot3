<template>
  <div class="app-container home">
    <el-row :gutter="20">
      <el-col :sm="24" :lg="12" style="padding-left: 20px">
        <h2>智慧工地 AI 视觉安全闭环管理系统</h2>
        <p>施工安全监管基础平台 —— 人员实名制、角色规则、资质证件、审核流程、打卡记录。</p>
        <p><b>当前版本:</b> <span>Phase 1</span></p>
      </el-col>
      <el-col :sm="24" :lg="12" style="padding-left: 50px">
        <el-row>
          <el-col :span="12"><h2>技术选型</h2></el-col>
        </el-row>
        <el-row>
          <el-col :span="6">
            <h4>后端</h4>
            <ul><li>SpringBoot 3</li><li>Spring Security</li><li>JWT</li><li>MyBatis</li><li>Druid</li></ul>
          </el-col>
          <el-col :span="6">
            <h4>前端</h4>
            <ul><li>Vue 2</li><li>Element UI</li><li>Axios</li></ul>
          </el-col>
        </el-row>
      </el-col>
    </el-row>
    <el-divider />
    <el-row :gutter="20">
      <el-col :xs="24" :sm="24" :md="12" :lg="8">
        <el-card class="update-log">
          <div slot="header"><span>功能模块</span></div>
          <div class="body">
            <ul>
              <li>人员档案 — 实名信息、角色分配、单位类型</li>
              <li>角色规则 — 打卡签到规则、资质要求配置</li>
              <li>资质证件 — 身份证/证书上传、有效期管理</li>
              <li>人脸信息 — 人脸采集、AI特征值预留</li>
              <li>审核记录 — 审核通过/驳回自动回写状态</li>
              <li>打卡记录 — 签到/签退/点到、时间范围查询</li>
              <li>手机端 H5 — 工人登录、打卡、上传证件</li>
            </ul>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="24" :md="12" :lg="8">
        <el-card class="update-log">
          <div slot="header"><span>快捷入口</span></div>
          <div class="body">
            <p>📱 手机端: <a href="/mobile.html" target="_blank">mobile.html</a></p>
            <p>📋 业务流程: <a href="#">doc/Phase1_业务流程说明.md</a></p>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
export default {
  name: "Index",
  data() { return {} }
}
</script>

<style scoped lang="scss">
.home {
  font-size: 13px; color: #676a6c;
  h2 { margin-top: 10px; font-size: 26px; font-weight: 100; }
  h4 { margin-top: 0px; }
  p { margin-top: 10px; b { font-weight: 700; } }
  ul { padding: 0; margin: 0; list-style-type: none; li { padding: 6px 0; } }
}
</style>
