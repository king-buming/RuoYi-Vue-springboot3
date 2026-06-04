<template>
  <view class="container">
    <!-- 状态筛选栏 -->
    <scroll-view scroll-x class="filter-bar">
      <view class="filter-tag" v-for="s in statusOptions" :key="s.value"
        :class="{ active: currentStatus === s.value }" @click="filterByStatus(s.value)">
        {{ s.label }}
      </view>
    </scroll-view>

    <!-- 计划卡片列表 -->
    <view class="list" v-if="planList.length > 0">
      <view class="card" v-for="p in planList" :key="p.planId" @click="goDetail(p.planId)">
        <view class="card-header">
          <text class="project-name">{{ p.projectName }}</text>
          <text class="status-tag" :class="'status-' + p.status">{{ getStatusLabel(p.status) }}</text>
        </view>
        <view class="card-body">
          <view class="info-row"><text class="label">施工点</text><text class="val">{{ p.constructionSite || '-' }}</text></view>
          <view class="info-row"><text class="label">作业类型</text><text class="val">{{ p.workType || '-' }}</text></view>
          <view class="info-row"><text class="label">计划时间</text><text class="val">{{ formatTime(p.planWorkTime) }}</text></view>
        </view>
        <view class="card-footer">
          <text class="time">{{ formatTime(p.createTime) }}</text>
          <uni-icons type="arrowright" size="16" color="#ccc"></uni-icons>
        </view>
      </view>
    </view>
    <view class="empty" v-else>
      <text>暂无作业计划</text>
      <text class="empty-sub">施工方管理人员可点击下方按钮新建计划</text>
    </view>

    <!-- 新建按钮 -->
    <view class="fab-btn" v-if="canCreate" @click="goCreate">
      <uni-icons type="plus-filled" size="28" color="#fff"></uni-icons>
    </view>
  </view>
</template>

<script>
import config from '@/config.js'

function authHeader() { return { 'Authorization': 'Bearer ' + (uni.getStorageSync('appToken') || '') } }

export default {
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
      ],
      statusLabels: { '0': '待审核', '1': '待执行', '2': '进行中', '3': '已完成', '4': '已取消' }
    }
  },
  onShow() {
    this.loadPlans()
    this.checkCanCreate()
  },
  onPullDownRefresh() { this.loadPlans().then(() => uni.stopPullDownRefresh()) },
  methods: {
    async loadPlans() {
      const token = uni.getStorageSync('appToken'); if (!token) return
      try {
        const [err, res] = await uni.request({
          url: config.baseUrl + '/app/homework/plan/list',
          header: authHeader(),
          data: { pageNum: 1, pageSize: 50, status: this.currentStatus || undefined }
        })
        if (res && res.data.code === 200) this.planList = res.data.rows || []
      } catch (e) {}
    },
    async checkCanCreate() {
      const token = uni.getStorageSync('appToken'); if (!token) return
      try {
        const [err, res] = await uni.request({ url: config.baseUrl + '/app/auth/me', header: authHeader() })
        if (res && res.data.code === 200) {
          const d = res.data.data
          const roles = d.roleCodes || []
          this.canCreate = d.unitType === '3' && !(roles.length === 1 && roles[0] === 'worker')
        }
      } catch (e) {}
    },
    filterByStatus(s) { this.currentStatus = s; this.loadPlans() },
    goDetail(id) { uni.navigateTo({ url: '/pages/worker/plan/detail?planId=' + id }) },
    goCreate() { uni.navigateTo({ url: '/pages/worker/plan/create' }) },
    getStatusLabel(v) { return this.statusLabels[v] || v },
    formatTime(t) {
      if (!t) return '-'; const d = new Date(t); const p = n => n < 10 ? '0' + n : '' + n
      return d.getFullYear() + '-' + p(d.getMonth() + 1) + '-' + p(d.getDate()) + ' ' + p(d.getHours()) + ':' + p(d.getMinutes())
    }
  }
}
</script>

<style>
.container { padding: 20rpx; min-height: 100vh; background: #f5f5f5; }

.filter-bar { white-space: nowrap; padding: 16rpx 0; margin-bottom: 16rpx; }
.filter-tag { display: inline-block; padding: 10rpx 24rpx; margin-right: 16rpx; background: #fff; border-radius: 28rpx; font-size: 26rpx; color: #666; }
.filter-tag.active { background: #007aff; color: #fff; }

.card { background: #fff; border-radius: 12rpx; padding: 24rpx; margin-bottom: 16rpx; box-shadow: 0 2rpx 8rpx rgba(0,0,0,0.04); }
.card-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16rpx; }
.project-name { font-size: 32rpx; font-weight: bold; color: #333; flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.status-tag { font-size: 22rpx; padding: 6rpx 14rpx; border-radius: 6rpx; color: #fff; flex-shrink: 0; margin-left: 16rpx; }
.status-0 { background: #ff9500; }
.status-1 { background: #007aff; }
.status-2 { background: #34c759; }
.status-3 { background: #8e8e93; }
.status-4 { background: #ff3b30; }

.info-row { display: flex; font-size: 26rpx; padding: 6rpx 0; }
.info-row .label { color: #999; width: 140rpx; flex-shrink: 0; }
.info-row .val { color: #333; flex: 1; }

.card-footer { display: flex; justify-content: space-between; align-items: center; margin-top: 12rpx; padding-top: 12rpx; border-top: 1rpx solid #f0f0f0; }
.card-footer .time { font-size: 24rpx; color: #ccc; }

.empty { text-align: center; margin-top: 100rpx; color: #999; font-size: 28rpx; }
.empty-sub { display: block; font-size: 24rpx; color: #ccc; margin-top: 12rpx; }

.fab-btn { position: fixed; bottom: 60rpx; right: 40rpx; width: 100rpx; height: 100rpx; background: #007aff; border-radius: 50%; display: flex; align-items: center; justify-content: center; box-shadow: 0 4rpx 16rpx rgba(0,122,255,0.4); }
</style>
