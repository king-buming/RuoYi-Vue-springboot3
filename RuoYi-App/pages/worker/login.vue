<template>
  <view class="container">
    <view class="header">
      <text class="title">智慧工地</text>
      <text class="subtitle">施工人员入口</text>
    </view>
    <view class="form">
      <view class="input-group">
        <text class="label">手机号</text>
        <input v-model="phone" type="number" placeholder="请输入手机号" maxlength="11" />
      </view>
      <view class="input-group">
        <text class="label">密码</text>
        <input v-model="idCardLast6" type="password" placeholder="身份证号后6位" maxlength="6" />
      </view>
      <button class="btn" @click="doLogin" :disabled="loading">{{ loading ? '登录中...' : '登录' }}</button>
    </view>
    <view class="tip">登录后 7 天内自动保持登录</view>
  </view>
</template>

<script>
import config from '@/config.js'
export default {
  data() { return { phone: '', idCardLast6: '', loading: false } },
  methods: {
    async doLogin() {
      if (!this.phone || !this.idCardLast6) { uni.showToast({ title: '请填写完整', icon: 'none' }); return }
      this.loading = true
      try {
        const [err, res] = await uni.request({
          url: config.baseUrl + '/app/auth/login', method: 'POST',
          data: { phone: this.phone, idCardLast6: this.idCardLast6 }
        })
        this.loading = false
        if (res.data.code === 200) {
          uni.setStorageSync('appToken', res.data.data.token)
          uni.setStorageSync('workerName', res.data.data.workerName)
          uni.showToast({ title: '登录成功' })
          setTimeout(() => { uni.reLaunch({ url: '/pages/worker/checkin' }) }, 500)
        } else {
          uni.showToast({ title: res.data.msg || '登录失败', icon: 'none' })
        }
      } catch (e) { this.loading = false; uni.showToast({ title: '网络错误', icon: 'none' }) }
    }
  }
}
</script>

<style scoped>
.container { padding: 60rpx 40rpx; min-height: 100vh; background: #f5f5f5; }
.header { text-align: center; margin-bottom: 60rpx; }
.title { font-size: 48rpx; font-weight: bold; color: #333; display: block; }
.subtitle { font-size: 28rpx; color: #999; margin-top: 10rpx; display: block; }
.form { background: #fff; border-radius: 16rpx; padding: 40rpx; }
.input-group { margin-bottom: 30rpx; }
.label { font-size: 28rpx; color: #666; display: block; margin-bottom: 10rpx; }
.input-group input { border: 1px solid #e5e5e5; border-radius: 8rpx; padding: 20rpx; font-size: 30rpx; }
.btn { background: #007aff; color: #fff; border-radius: 8rpx; margin-top: 20rpx; font-size: 32rpx; }
.tip { text-align: center; color: #ccc; font-size: 24rpx; margin-top: 30rpx; }
</style>
