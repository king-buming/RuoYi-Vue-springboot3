<template>
  <view class="container">
    <!-- 状态筛选栏 -->
    <scroll-view scroll-x class="filter-bar">
      <view class="filter-tag" v-for="s in reviewStatusOptions" :key="s.value"
        :class="{ active: currentStatus === s.value }" @click="filterByStatus(s.value)">
        {{ s.label }}
      </view>
    </scroll-view>

    <!-- 审核卡片列表 -->
    <view class="list" v-if="reviewList.length > 0">
      <view class="card" v-for="r in reviewList" :key="r.reviewId" @click="goDetail(r.reviewId)">
        <view class="card-header">
          <text class="plan-name">{{ r.planName }}</text>
          <text class="review-tag" :class="'review-' + r.reviewStatus">{{ getReviewStatusLabel(r.reviewStatus) }}</text>
        </view>
        <view class="card-body">
          <view class="info-row"><text class="label">申请人</text><text class="val">{{ r.applicant || '-' }}</text></view>
          <view class="info-row"><text class="label">作业类型</text><text class="val">{{ r.workType || '-' }}</text></view>
          <view class="info-row"><text class="label">申请时间</text><text class="val">{{ formatTime(r.applyTime) }}</text></view>
        </view>
        <!-- 待审核时显示快捷操作 -->
        <view class="card-footer" v-if="r.reviewStatus === '0'">
          <text class="action-btn approve" @click.stop="quickApprove(r)">通过</text>
          <text class="action-btn reject" @click.stop="quickReject(r)">驳回</text>
        </view>
        <view class="card-footer" v-else>
          <text class="time">{{ r.reviewer ? '审核人: ' + r.reviewer : '' }}</text>
          <uni-icons type="arrowright" size="16" color="#ccc"></uni-icons>
        </view>
      </view>
    </view>
    <view class="empty" v-else>
      <text>暂无审核记录</text>
      <text class="empty-sub">您可能需要作业批准人权限才能查看审核列表</text>
    </view>
  </view>
</template>

<script>
import config from '@/config.js'

function authHeader() { return { 'Authorization': 'Bearer ' + (uni.getStorageSync('appToken') || '') } }

export default {
  data() {
    return {
      reviewList: [],
      currentStatus: '',
      reviewStatusOptions: [
        { label: '全部', value: '' },
        { label: '待审核', value: '0' },
        { label: '已通过', value: '1' },
        { label: '已驳回', value: '2' }
      ],
      reviewStatusLabels: { '0': '待审核', '1': '已通过', '2': '已驳回' }
    }
  },
  onShow() { this.loadList() },
  onPullDownRefresh() { this.loadList().then(() => uni.stopPullDownRefresh()) },
  methods: {
    async loadList() {
      const token = uni.getStorageSync('appToken'); if (!token) return
      try {
        const [err, res] = await uni.request({
          url: config.baseUrl + '/app/homework/review/list',
          header: authHeader(),
          data: { pageNum: 1, pageSize: 50, reviewStatus: this.currentStatus || undefined }
        })
        if (res && res.data.code === 200) this.reviewList = res.data.rows || []
      } catch (e) {}
    },
    filterByStatus(s) { this.currentStatus = s; this.loadList() },
    goDetail(id) { uni.navigateTo({ url: '/pages/worker/review/detail?reviewId=' + id }) },
    getReviewStatusLabel(v) { return this.reviewStatusLabels[v] || v },
    quickApprove(r) {
      uni.showModal({
        title: '审核通过',
        content: '确定通过"' + r.planName + '"的作业计划申请吗？',
        success: async (res) => {
          if (res.confirm) {
            const [err, resp] = await uni.request({
              url: config.baseUrl + '/app/homework/review/approve',
              method: 'PUT',
              header: authHeader(),
              data: { reviewId: r.reviewId, reviewOpinion: '移动端审核通过' }
            })
            if (resp && resp.data.code === 200) {
              uni.showToast({ title: '审核通过' })
              this.loadList()
            } else {
              uni.showToast({ title: (resp && resp.data.msg) || '操作失败', icon: 'none' })
            }
          }
        }
      })
    },
    quickReject(r) {
      uni.showModal({
        title: '审核驳回',
        content: '确定驳回"' + r.planName + '"的作业计划申请吗？',
        editable: true,
        placeholderText: '请输入驳回原因',
        success: async (res) => {
          if (res.confirm) {
            const [err, resp] = await uni.request({
              url: config.baseUrl + '/app/homework/review/reject',
              method: 'PUT',
              header: authHeader(),
              data: { reviewId: r.reviewId, reviewOpinion: res.content || '移动端审核驳回' }
            })
            if (resp && resp.data.code === 200) {
              uni.showToast({ title: '已驳回' })
              this.loadList()
            } else {
              uni.showToast({ title: (resp && resp.data.msg) || '操作失败', icon: 'none' })
            }
          }
        }
      })
    },
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
.plan-name { font-size: 32rpx; font-weight: bold; color: #333; flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.review-tag { font-size: 22rpx; padding: 6rpx 14rpx; border-radius: 6rpx; color: #fff; flex-shrink: 0; margin-left: 16rpx; }
.review-0 { background: #ff9500; }
.review-1 { background: #34c759; }
.review-2 { background: #ff3b30; }

.info-row { display: flex; font-size: 26rpx; padding: 6rpx 0; }
.info-row .label { color: #999; width: 140rpx; flex-shrink: 0; }
.info-row .val { color: #333; flex: 1; }

.card-footer { display: flex; justify-content: flex-end; align-items: center; margin-top: 12rpx; padding-top: 12rpx; border-top: 1rpx solid #f0f0f0; gap: 20rpx; }
.card-footer .time { font-size: 24rpx; color: #ccc; flex: 1; }
.action-btn { font-size: 26rpx; padding: 8rpx 28rpx; border-radius: 28rpx; color: #fff; }
.action-btn.approve { background: #34c759; }
.action-btn.reject { background: #ff3b30; }

.empty { text-align: center; margin-top: 100rpx; color: #999; font-size: 28rpx; }
.empty-sub { display: block; font-size: 24rpx; color: #ccc; margin-top: 12rpx; }
</style>
