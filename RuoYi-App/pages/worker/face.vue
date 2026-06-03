<template>
  <view class="container">
    <view class="top-bar"><text class="name">人脸信息</text></view>
    <view class="status-box" v-if="faceInfo">
      <image v-if="faceInfo.faceImgUrl" :src="baseUrl + faceInfo.faceImgUrl" class="face-img" mode="aspectFill" />
      <text class="face-status ok">✅ 已录入人脸</text>
      <text class="collect-time">采集时间: {{ faceInfo.collectTime || '-' }}</text>
    </view>
    <view class="status-box" v-else>
      <text class="face-status warn">⚠ 未录入人脸</text>
    </view>
    <button class="btn" @click="takeFace">📷 上传人脸照片</button>
  </view>
</template>

<script>
import config from '@/config.js'
import upload from '@/utils/upload'
function authHeader() { return { 'Authorization': 'Bearer ' + (uni.getStorageSync('appToken') || '') } }
export default {
  data() { return { faceInfo: null, baseUrl: config.baseUrl } },
  onShow() { this.load() },
  methods: {
    async load() {
      try {
        const [e, r] = await uni.request({ url: config.baseUrl + '/app/worker/face', header: authHeader() })
        if (r && r.data.code === 200 && r.data.data) this.faceInfo = r.data.data
      } catch (e) {}
    },
    takeFace() {
      uni.chooseImage({ count: 1, sourceType: ['camera', 'album'], success: async (res) => {
        uni.showLoading({ title: '上传中...' })
        try {
          const result = await upload({ url: '/common/upload', filePath: res.tempFilePaths[0] })
          const url = result.url || result.data || result.fileName
          const [e, r] = await uni.request({ url: config.baseUrl + '/app/worker/face', method: 'POST', header: authHeader(), data: { faceImgUrl: url } })
          uni.hideLoading()
          if (r && r.data.code === 200) { uni.showToast({ title: '上传成功' }); this.load() }
          else { uni.showToast({ title: r.data.msg || '失败', icon: 'none' }) }
        } catch (e) { uni.hideLoading(); uni.showToast({ title: '上传失败', icon: 'none' }) }
      }})
    }
  }
}
</script>

<style scoped>
.container { padding: 20rpx; min-height: 100vh; background: #f5f5f5; }
.top-bar { background: #fff; border-radius: 12rpx; padding: 20rpx; margin-bottom: 16rpx; }
.name { font-size: 36rpx; font-weight: bold; }
.status-box { background: #fff; border-radius: 12rpx; padding: 40rpx; text-align: center; margin-bottom: 20rpx; }
.face-img { width: 300rpx; height: 300rpx; border-radius: 12rpx; margin-bottom: 16rpx; }
.face-status { font-size: 32rpx; display: block; margin-bottom: 8rpx; }
.face-status.ok { color: #34c759; } .face-status.warn { color: #ff9500; }
.collect-time { font-size: 24rpx; color: #999; }
.btn { background: #007aff; color: #fff; border-radius: 8rpx; margin-top: 20rpx; font-size: 32rpx; }
</style>
