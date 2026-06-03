<template>
  <view class="container">
    <view class="top-bar"><text class="name">{{ info.workerName || '加载中...' }}</text></view>
    <view class="card" v-if="info.workerId">
      <view class="row"><text class="lbl">姓名</text><text class="val">{{ info.workerName }}</text></view>
      <view class="row"><text class="lbl">手机号</text><text class="val">{{ info.phone }}</text></view>
      <view class="row"><text class="lbl">性别</text><text class="val">{{ genderLabel }}</text></view>
      <view class="row"><text class="lbl">单位类型</text><text class="val">{{ unitLabel }}</text></view>
      <view class="row"><text class="lbl">审核状态</text><text class="val"><text :class="'badge '+auditCls">{{ auditLabel }}</text></text></view>
      <view class="row"><text class="lbl">人脸录入</text><text class="val">{{ info.faceStatus === '1' ? '已录入' : '未录入' }}</text></view>
    </view>
    <view class="empty" v-else>请先登录</view>
  </view>
</template>

<script>
import config from '@/config.js'
function authHeader() { return { 'Authorization': 'Bearer ' + (uni.getStorageSync('appToken') || '') } }
export default {
  data() { return { info: {} } },
  computed: {
    genderLabel() { const m = {'0':'男','1':'女','2':'未知'}; return m[this.info.gender] || this.info.gender },
    unitLabel() { const m = {'1':'管网','2':'第三方','3':'施工方'}; return m[this.info.unitType] || this.info.unitType },
    auditLabel() { const m = {'0':'待审核','1':'已通过','2':'已驳回'}; return m[this.info.auditStatus] || this.info.auditStatus },
    auditCls() { const m = {'0':'warn','1':'ok','2':'err'}; return m[this.info.auditStatus] || '' }
  },
  onShow() { this.load() },
  methods: {
    async load() {
      try {
        const [e, r] = await uni.request({ url: config.baseUrl + '/app/auth/me', header: authHeader() })
        if (r && r.data.code === 200) this.info = r.data.data
      } catch (e) {}
    }
  }
}
</script>

<style>
.container { padding: 20rpx; min-height: 100vh; background: #f5f5f5; }
.top-bar { background: #fff; border-radius: 12rpx; padding: 20rpx; margin-bottom: 16rpx; }
.name { font-size: 36rpx; font-weight: bold; }
.card { background: #fff; border-radius: 12rpx; padding: 20rpx; }
.row { display: flex; justify-content: space-between; padding: 12rpx 0; border-bottom: 1px solid #f0f0f0; font-size: 28rpx; }
.row:last-child { border: none; }
.lbl { color: #999; } .val { color: #333; font-weight: 500; }
.badge { font-size: 22rpx; padding: 4rpx 16rpx; border-radius: 20rpx; }
.badge.warn { background: #fff3e0; color: #ff9500; } .badge.ok { background: #e8f5e9; color: #34c759; } .badge.err { background: #ffebee; color: #ff3b30; }
.empty { text-align: center; margin-top: 100rpx; color: #999; }
</style>
