<template>
  <view class="container">
    <!-- 状态头 -->
    <view class="status-header" :class="'bg-status-' + plan.status">
      <text class="status-text">{{ getStatusLabel(plan.status) }}</text>
    </view>

    <!-- 基本信息 -->
    <view class="section">
      <view class="section-title">基本信息</view>
      <view class="info-item"><text class="label">项目名称</text><text class="val">{{ plan.projectName || '-' }}</text></view>
      <view class="info-item"><text class="label">市/县</text><text class="val">{{ plan.cityCounty || '-' }}</text></view>
      <view class="info-item"><text class="label">施工点</text><text class="val">{{ plan.constructionSite || '-' }}</text></view>
      <view class="info-item"><text class="label">GPS坐标</text><text class="val">{{ gpsText }}</text></view>
      <view class="info-item"><text class="label">作业类型</text><text class="val">{{ plan.workType || '-' }}</text></view>
      <view class="info-item"><text class="label">计划时间</text><text class="val">{{ formatTime(plan.planWorkTime) }}</text></view>
      <view class="info-item"><text class="label">施工单位</text><text class="val">{{ plan.constructionUnit || '-' }}</text></view>
      <view class="info-item"><text class="label">作业内容</text><text class="val multiline">{{ plan.workContent || '-' }}</text></view>
      <view class="info-item"><text class="label">备注</text><text class="val">{{ plan.remark || '-' }}</text></view>
    </view>

    <!-- 参与人员 -->
    <view class="section">
      <view class="section-title">参与人员 ({{ workers.length }})</view>
      <view class="worker-tags" v-if="workers.length > 0">
        <view class="worker-tag" v-for="w in workers" :key="w.id">{{ w.workerName }}</view>
      </view>
      <text v-else class="empty-hint">暂无人员</text>
    </view>

  </view>
</template>

<script>
import config from '@/config.js'

function authHeader() { return { 'Authorization': 'Bearer ' + (uni.getStorageSync('appToken') || '') } }

export default {
  data() {
    return {
      plan: {},
      workers: [],
      statusLabels: { '1': '待执行', '2': '进行中', '3': '已完成', '4': '已取消' }
    }
  },
  computed: {
    gpsText() {
      const p = this.plan
      if (p.siteLatitude && p.siteLongitude) return p.siteLatitude + ', ' + p.siteLongitude
      return '-'
    }
  },
  onLoad(options) {
    if (options.planId) this.loadDetail(Number(options.planId))
  },
  methods: {
    async loadDetail(planId) {
      const token = uni.getStorageSync('appToken'); if (!token) return
      try {
        const [err, res] = await uni.request({
          url: config.baseUrl + '/app/homework/plan/' + planId,
          header: authHeader()
        })
        if (res && res.data.code === 200) {
          const d = res.data.data
          this.plan = d.plan || {}
          this.workers = d.workers || []
        }
      } catch (e) { uni.showToast({ title: '加载失败', icon: 'none' }) }
    },
    getStatusLabel(v) { return this.statusLabels[v] || v },
    formatTime(t) {
      if (!t) return '-'; const d = new Date(t); const p = n => n < 10 ? '0' + n : '' + n
      return d.getFullYear() + '-' + p(d.getMonth() + 1) + '-' + p(d.getDate()) + ' ' + p(d.getHours()) + ':' + p(d.getMinutes())
    }
  }
}
</script>

<style>
.container { min-height: 100vh; background: #f5f5f5; padding-bottom: 40rpx; }

.status-header { padding: 40rpx 30rpx; text-align: center; }
.status-header .status-text { font-size: 36rpx; color: #fff; font-weight: bold; }
.bg-status-0 { background: #ff9500; }
.bg-status-1 { background: #007aff; }
.bg-status-2 { background: #34c759; }
.bg-status-3 { background: #8e8e93; }
.bg-status-4 { background: #ff3b30; }

.section { background: #fff; margin: 20rpx; border-radius: 12rpx; padding: 24rpx; }
.section-title { font-size: 30rpx; font-weight: bold; color: #333; margin-bottom: 16rpx; padding-bottom: 16rpx; border-bottom: 1rpx solid #f0f0f0; }
.info-item { display: flex; padding: 10rpx 0; font-size: 28rpx; }
.info-item .label { color: #999; width: 160rpx; flex-shrink: 0; }
.info-item .val { color: #333; flex: 1; }
.info-item .val.multiline { white-space: pre-wrap; }

.worker-tags { display: flex; flex-wrap: wrap; gap: 12rpx; }
.worker-tag { padding: 8rpx 20rpx; background: #e8f4ff; color: #007aff; border-radius: 8rpx; font-size: 26rpx; }
.empty-hint { color: #ccc; font-size: 26rpx; }

.review-status-0 { color: #ff9500; }
.review-status-1 { color: #34c759; }
.review-status-2 { color: #ff3b30; }
</style>
