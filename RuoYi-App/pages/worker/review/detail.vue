<template>
  <view class="container">
    <!-- 审核状态头 -->
    <view class="status-header" :class="'review-bg-' + review.reviewStatus">
      <text class="status-text">{{ getReviewStatusLabel(review.reviewStatus) }}</text>
    </view>

    <!-- 计划信息 -->
    <view class="section">
      <view class="section-title">计划信息</view>
      <view class="info-item"><text class="label">项目名称</text><text class="val">{{ plan.projectName || '-' }}</text></view>
      <view class="info-item"><text class="label">施工点</text><text class="val">{{ plan.constructionSite || '-' }}</text></view>
      <view class="info-item"><text class="label">作业类型</text><text class="val">{{ plan.workType || '-' }}</text></view>
      <view class="info-item"><text class="label">计划时间</text><text class="val">{{ formatTime(plan.planWorkTime) }}</text></view>
      <view class="info-item"><text class="label">施工单位</text><text class="val">{{ plan.constructionUnit || '-' }}</text></view>
      <view class="info-item"><text class="label">作业内容</text><text class="val multiline">{{ plan.workContent || '-' }}</text></view>
    </view>

    <!-- 审核信息 -->
    <view class="section">
      <view class="section-title">审核信息</view>
      <view class="info-item"><text class="label">申请人</text><text class="val">{{ review.applicant || '-' }}</text></view>
      <view class="info-item"><text class="label">申请时间</text><text class="val">{{ formatTime(review.applyTime) }}</text></view>
      <view class="info-item" v-if="review.reviewer"><text class="label">审核人</text><text class="val">{{ review.reviewer }}</text></view>
      <view class="info-item" v-if="review.reviewTime"><text class="label">审核时间</text><text class="val">{{ formatTime(review.reviewTime) }}</text></view>
      <view class="info-item" v-if="review.reviewOpinion"><text class="label">审核意见</text><text class="val multiline">{{ review.reviewOpinion }}</text></view>
    </view>

    <!-- 参与人员 -->
    <view class="section">
      <view class="section-title">参与人员 ({{ workers.length }})</view>
      <view class="worker-tags" v-if="workers.length > 0">
        <view class="worker-tag" v-for="w in workers" :key="w.id">{{ w.workerName }}</view>
      </view>
      <text v-else class="empty-hint">暂无人员</text>
    </view>

    <!-- 审核操作区（仅待审核时显示） -->
    <view class="section" v-if="review.reviewStatus === '0'">
      <view class="section-title">审核操作</view>
      <uni-easyinput v-model="reviewOpinion" type="textarea" placeholder="请输入审核意见（选填）" />
      <view class="btn-group">
        <button class="btn-approve" @click="doApprove">审核通过</button>
        <button class="btn-reject" @click="doReject">审核驳回</button>
      </view>
    </view>
  </view>
</template>

<script>
import config from '@/config.js'

function authHeader() { return { 'Authorization': 'Bearer ' + (uni.getStorageSync('appToken') || '') } }

export default {
  data() {
    return {
      review: {},
      plan: {},
      workers: [],
      reviewOpinion: '',
      reviewStatusLabels: { '0': '待审核', '1': '已通过', '2': '已驳回' }
    }
  },
  onLoad(options) {
    if (options.reviewId) this.loadDetail(Number(options.reviewId))
  },
  methods: {
    async loadDetail(reviewId) {
      const token = uni.getStorageSync('appToken'); if (!token) return
      try {
        const [err, res] = await uni.request({
          url: config.baseUrl + '/app/homework/review/' + reviewId,
          header: authHeader()
        })
        if (res && res.data.code === 200) {
          const d = res.data.data
          this.review = d.review || {}
          this.plan = d.plan || {}
          this.workers = d.workers || []
        }
      } catch (e) { uni.showToast({ title: '加载失败', icon: 'none' }) }
    },
    async doApprove() {
      uni.showModal({
        title: '确认审核通过',
        content: '确定通过此作业计划申请吗？',
        success: async (res) => {
          if (!res.confirm) return
          const [err, resp] = await uni.request({
            url: config.baseUrl + '/app/homework/review/approve',
            method: 'PUT',
            header: authHeader(),
            data: { reviewId: this.review.reviewId, reviewOpinion: this.reviewOpinion || '移动端审核通过' }
          })
          if (resp && resp.data.code === 200) {
            uni.showToast({ title: '审核通过' })
            setTimeout(() => { uni.navigateBack() }, 1000)
          } else {
            uni.showToast({ title: (resp && resp.data.msg) || '操作失败', icon: 'none' })
          }
        }
      })
    },
    async doReject() {
      uni.showModal({
        title: '确认审核驳回',
        content: '确定驳回此作业计划申请吗？',
        editable: true,
        placeholderText: '请输入驳回原因',
        success: async (res) => {
          if (!res.confirm) return
          const [err, resp] = await uni.request({
            url: config.baseUrl + '/app/homework/review/reject',
            method: 'PUT',
            header: authHeader(),
            data: { reviewId: this.review.reviewId, reviewOpinion: res.content || this.reviewOpinion || '移动端审核驳回' }
          })
          if (resp && resp.data.code === 200) {
            uni.showToast({ title: '已驳回' })
            setTimeout(() => { uni.navigateBack() }, 1000)
          } else {
            uni.showToast({ title: (resp && resp.data.msg) || '操作失败', icon: 'none' })
          }
        }
      })
    },
    getReviewStatusLabel(v) { return this.reviewStatusLabels[v] || v },
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
.review-bg-0 { background: #ff9500; }
.review-bg-1 { background: #34c759; }
.review-bg-2 { background: #ff3b30; }

.section { background: #fff; margin: 20rpx; border-radius: 12rpx; padding: 24rpx; }
.section-title { font-size: 30rpx; font-weight: bold; color: #333; margin-bottom: 16rpx; padding-bottom: 16rpx; border-bottom: 1rpx solid #f0f0f0; }
.info-item { display: flex; padding: 10rpx 0; font-size: 28rpx; }
.info-item .label { color: #999; width: 160rpx; flex-shrink: 0; }
.info-item .val { color: #333; flex: 1; }
.info-item .val.multiline { white-space: pre-wrap; }

.worker-tags { display: flex; flex-wrap: wrap; gap: 12rpx; }
.worker-tag { padding: 8rpx 20rpx; background: #e8f4ff; color: #007aff; border-radius: 8rpx; font-size: 26rpx; }
.empty-hint { color: #ccc; font-size: 26rpx; }

.btn-group { display: flex; gap: 20rpx; margin-top: 30rpx; }
.btn-approve, .btn-reject { flex: 1; font-size: 32rpx; color: #fff; padding: 24rpx; border-radius: 12rpx; text-align: center; border: none; }
.btn-approve { background: #34c759; }
.btn-reject { background: #ff3b30; }
</style>
