<template>
  <view class="container">
    <!-- 用户信息栏 -->
    <view class="user-bar" v-if="workerName">
      <view class="user-left">
        <text class="name">{{ workerName }}</text>
        <text class="role">{{ roleLabel }}</text>
      </view>
      <text class="audit-status" :class="'audit-' + auditStatus">{{ auditLabel }}</text>
    </view>

    <!-- 功能宫格 -->
    <view class="grid-section">
      <view class="section-title">作业管理</view>
      <view class="grid-row">
        <view class="grid-item" v-for="(item, idx) in menus" :key="idx" @click="item.action">
          <view class="grid-icon" :style="{ background: item.bgColor }">
            <uni-icons :type="item.icon" size="28" color="#fff"></uni-icons>
          </view>
          <text class="grid-text">{{ item.text }}</text>
        </view>
      </view>
    </view>

    <!-- 快捷操作 -->
    <view class="quick-section">
      <view class="section-title">快捷操作</view>
      <view class="action-card" @click="goCreatePlan" v-if="canCreatePlan">
        <view class="action-icon" style="background:#e8f4ff"><uni-icons type="plus-filled" size="22" color="#007aff"></uni-icons></view>
        <view class="action-body">
          <text class="action-title">新建作业计划</text>
          <text class="action-desc">创建新的施工作业计划单</text>
        </view>
        <uni-icons type="arrowright" size="16" color="#ccc"></uni-icons>
      </view>
      <view class="action-card" @click="goReviewList" v-if="isApprover">
        <view class="action-icon" style="background:#e8f8ec"><uni-icons type="checkmarkempty" size="22" color="#34c759"></uni-icons></view>
        <view class="action-body">
          <text class="action-title">待审核计划</text>
          <text class="action-desc">审核施工方提交的作业计划</text>
        </view>
        <uni-icons type="arrowright" size="16" color="#ccc"></uni-icons>
      </view>
      <view class="action-card" @click="goRecords">
        <view class="action-icon" style="background:#fff3e0"><uni-icons type="list" size="22" color="#ff9500"></uni-icons></view>
        <view class="action-body">
          <text class="action-title">打卡记录</text>
          <text class="action-desc">查看所有历史打卡记录</text>
        </view>
        <uni-icons type="arrowright" size="16" color="#ccc"></uni-icons>
      </view>
    </view>

    <!-- 未登录提示 -->
    <view class="login-hint" v-if="!workerName">
      <text class="hint-text">请先登录以使用工作台功能</text>
      <button class="btn-login" @click="goLogin">前往登录</button>
    </view>
  </view>
</template>

<script>
import config from '@/config.js'

function authHeader() { return { 'Authorization': 'Bearer ' + (uni.getStorageSync('appToken') || '') } }

export default {
  data() {
    return {
      workerName: '',
      auditStatus: '0',
      unitType: '',
      roleCodes: [],
      roleLabel: '',
      menus: [
        { text: '作业计划', icon: 'paperplane-filled', bgColor: '#007aff', action: () => { uni.navigateTo({ url: '/pages/worker/plan/list' }) } },
        { text: '作业审核', icon: 'checkmarkempty', bgColor: '#34c759', action: () => { uni.navigateTo({ url: '/pages/worker/review/list' }) } },
        { text: '打卡记录', icon: 'list', bgColor: '#ff9500', action: () => { uni.navigateTo({ url: '/pages/worker/records' }) } }
      ]
    }
  },
  computed: {
    auditLabel() {
      const m = { '0': '待审核', '1': '已通过', '2': '已驳回' }
      return m[this.auditStatus] || '未认证'
    },
    canCreatePlan() {
      return this.unitType === '3' && !(this.roleCodes.length === 1 && this.roleCodes[0] === 'worker')
    },
    isApprover() {
      return this.roleCodes.includes('approver')
    }
  },
  onShow() { this.refresh() },
  methods: {
    async refresh() {
      const token = uni.getStorageSync('appToken')
      if (!token) { this.workerName = ''; return }
      try {
        const [e1, r1] = await uni.request({ url: config.baseUrl + '/app/auth/me', header: authHeader() })
        if (r1 && r1.data.code === 200) {
          const d = r1.data.data
          this.workerName = d.workerName
          this.auditStatus = d.auditStatus
          this.unitType = d.unitType
          this.roleCodes = d.roleCodes || []
          const roleNames = d.roleNames || []
          this.roleLabel = roleNames.length > 0 ? roleNames.join('、') : ''
        }
      } catch (e) {}
    },
    goCreatePlan() { uni.navigateTo({ url: '/pages/worker/plan/create' }) },
    goReviewList() { uni.navigateTo({ url: '/pages/worker/review/list' }) },
    goRecords() { uni.navigateTo({ url: '/pages/worker/records' }) },
    goLogin() { uni.reLaunch({ url: '/pages/worker/login' }) }
  }
}
</script>

<style>
.container { padding: 20rpx; min-height: 100vh; background: #f5f5f5; }

.user-bar { display: flex; justify-content: space-between; align-items: center; padding: 24rpx; background: #fff; border-radius: 12rpx; margin-bottom: 24rpx; box-shadow: 0 2rpx 8rpx rgba(0,0,0,0.04); }
.user-left { display: flex; flex-direction: column; }
.name { font-size: 36rpx; font-weight: bold; color: #333; }
.role { font-size: 24rpx; color: #999; margin-top: 4rpx; }
.audit-status { font-size: 22rpx; padding: 8rpx 16rpx; border-radius: 20rpx; }
.audit-0 { color: #ff9500; background: #fff3e0; }
.audit-1 { color: #34c759; background: #e8f8ec; }
.audit-2 { color: #ff3b30; background: #ffeaea; }

.grid-section { background: #fff; border-radius: 12rpx; padding: 24rpx; margin-bottom: 24rpx; box-shadow: 0 2rpx 8rpx rgba(0,0,0,0.04); }
.section-title { font-size: 30rpx; font-weight: bold; color: #333; margin-bottom: 20rpx; }
.grid-row { display: flex; justify-content: space-around; }
.grid-item { display: flex; flex-direction: column; align-items: center; }
.grid-icon { width: 96rpx; height: 96rpx; border-radius: 20rpx; display: flex; align-items: center; justify-content: center; margin-bottom: 12rpx; }
.grid-text { font-size: 26rpx; color: #333; }

.quick-section { background: #fff; border-radius: 12rpx; padding: 24rpx; margin-bottom: 24rpx; box-shadow: 0 2rpx 8rpx rgba(0,0,0,0.04); }
.action-card { display: flex; align-items: center; padding: 20rpx 0; border-bottom: 1rpx solid #f0f0f0; }
.action-card:last-child { border-bottom: none; }
.action-icon { width: 72rpx; height: 72rpx; border-radius: 16rpx; display: flex; align-items: center; justify-content: center; margin-right: 20rpx; }
.action-body { flex: 1; }
.action-title { font-size: 28rpx; font-weight: bold; color: #333; display: block; }
.action-desc { font-size: 24rpx; color: #999; margin-top: 4rpx; display: block; }

.login-hint { text-align: center; margin-top: 150rpx; }
.hint-text { font-size: 28rpx; color: #999; display: block; margin-bottom: 40rpx; }
.btn-login { width: 300rpx; background: #007aff; color: #fff; font-size: 30rpx; border-radius: 44rpx; }
</style>
