<template>
  <view class="container">
    <view class="user-bar">
      <text class="name">{{ workerName || '未登录' }}</text>
      <text class="status">{{ auditLabel }}</text>
    </view>
    <view class="clock">{{ now }}</view>
    <view class="date">{{ today }}</view>
    <view class="today-status">
      <text>签到：{{ hasSignIn ? signInTime : '未签到' }}</text>
      <text>签退：{{ hasSignOut ? signOutTime : '未签退' }}</text>
    </view>
    <view class="photo-area" @click="takePhoto">
      <image v-if="photoUrl" :src="photoUrl" class="photo" mode="aspectFill" />
      <text v-else class="photo-placeholder">点击拍照</text>
    </view>
    <view class="btn-group">
      <button class="btn signin" @click="doSignIn" :disabled="hasSignIn || loading">签 到</button>
      <button class="btn signout" @click="doSignOut" :disabled="hasSignOut || loading">签 退</button>
    </view>
    <view class="records-link" @click="goRecords">查看打卡记录 →</view>
  </view>
</template>

<script>
import config from '@/config.js'
import upload from '@/utils/upload'
function pad(n) { return n < 10 ? '0' + n : '' + n }
function authHeader() { return { 'Authorization': 'Bearer ' + (uni.getStorageSync('appToken') || '') } }

export default {
  data() { return { workerName: '', auditStatus: '', now: '', today: '', hasSignIn: false, hasSignOut: false, signInTime: '', signOutTime: '', photoUrl: '', loading: false, _timer: null } },
  computed: {
    auditLabel() {
      const m = { '0': '待审核', '1': '已通过', '2': '已驳回' }
      return m[this.auditStatus] || ''
    }
  },
  onShow() { this.refresh(); this._timer = setInterval(() => { this.updateTime() }, 1000) },
  onHide() { if (this._timer) { clearInterval(this._timer); this._timer = null } },
  methods: {
    updateTime() {
      const d = new Date()
      this.now = pad(d.getHours()) + ':' + pad(d.getMinutes()) + ':' + pad(d.getSeconds())
      this.today = d.getFullYear() + '-' + pad(d.getMonth() + 1) + '-' + pad(d.getDate())
    },
    async refresh() {
      this.updateTime()
      const token = uni.getStorageSync('appToken')
      if (!token) { uni.reLaunch({ url: '/pages/worker/login' }); return }
      try {
        const [e1, r1] = await uni.request({ url: config.baseUrl + '/app/auth/me', header: authHeader() })
        if (r1 && r1.data.code === 200) { this.workerName = r1.data.data.workerName; this.auditStatus = r1.data.data.auditStatus }
      } catch (e) {}
      try {
        const [e2, r2] = await uni.request({ url: config.baseUrl + '/app/checkin/today', header: authHeader() })
        if (r2 && r2.data.code === 200) {
          const d = r2.data.data; this.hasSignIn = d.hasSignIn; this.signInTime = d.signInTime || ''; this.hasSignOut = d.hasSignOut; this.signOutTime = d.signOutTime || ''
        }
      } catch (e) {}
    },
    takePhoto() {
      uni.chooseImage({ count: 1, success: async (res) => {
        uni.showLoading({ title: '上传中...' })
        try {
          const result = await upload({ url: '/common/upload', filePath: res.tempFilePaths[0] })
          this.photoUrl = result.url || result.data || result.fileName
          uni.hideLoading()
          uni.showToast({ title: '照片已上传', icon: 'success', duration: 1000 })
        } catch (e) { uni.hideLoading(); uni.showToast({ title: '上传失败', icon: 'none' }) }
      }})
    },
    async doCheck(action) {
      this.loading = true
      try {
        const [err, res] = await uni.request({ url: config.baseUrl + '/app/checkin/' + action, method: 'POST', header: authHeader(), data: { checkMethod: 'H5', photoUrl: this.photoUrl } })
        this.loading = false
        if (res.data.code === 200) { uni.showToast({ title: action === 'signIn' ? '签到成功' : '签退成功' }); this.photoUrl = ''; this.refresh() }
        else { uni.showToast({ title: res.data.msg || '失败', icon: 'none' }) }
      } catch (e) { this.loading = false; uni.showToast({ title: '网络错误', icon: 'none' }) }
    },
    doSignIn() { this.doCheck('signIn') },
    doSignOut() { this.doCheck('signOut') },
    goRecords() { uni.navigateTo({ url: '/pages/worker/records' }) }
  }
}
</script>

<style>
.container { padding: 30rpx; min-height: 100vh; background: #f5f5f5; }
.user-bar { display: flex; justify-content: space-between; align-items: center; padding: 20rpx; background: #fff; border-radius: 12rpx; margin-bottom: 30rpx; }
.name { font-size: 36rpx; font-weight: bold; }
.status { font-size: 24rpx; color: #007aff; padding: 6rpx 16rpx; background: #e8f4ff; border-radius: 6rpx; }
.clock { font-size: 80rpx; text-align: center; font-weight: bold; color: #333; margin-top: 40rpx; }
.date { text-align: center; color: #999; font-size: 28rpx; margin-bottom: 30rpx; }
.today-status { background: #fff; border-radius: 12rpx; padding: 20rpx; margin-bottom: 20rpx; display: flex; justify-content: space-around; font-size: 26rpx; color: #666; }
.photo-area { width: 200rpx; height: 200rpx; background: #e5e5e5; border-radius: 12rpx; margin: 20rpx auto; display: flex; align-items: center; justify-content: center; overflow: hidden; }
.photo { width: 100%; height: 100%; }
.photo-placeholder { color: #999; font-size: 28rpx; }
.btn-group { display: flex; gap: 20rpx; margin-top: 40rpx; }
.btn { flex: 1; font-size: 32rpx; border-radius: 12rpx; color: #fff; padding: 24rpx; }
.signin { background: #007aff; }
.signout { background: #ff3b30; }
.records-link { text-align: center; margin-top: 50rpx; color: #007aff; font-size: 28rpx; }
</style>
