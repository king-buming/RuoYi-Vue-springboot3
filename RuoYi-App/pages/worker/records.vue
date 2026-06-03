<template>
  <view class="container">
    <view class="list" v-if="list.length > 0">
      <view class="item" v-for="(r, i) in list" :key="i">
        <view class="item-left">
          <view class="type-tag" :class="r.checkType === '1' ? 'signin' : r.checkType === '2' ? 'signout' : 'checkin'">
            {{ r.checkType === '1' ? '签到' : r.checkType === '2' ? '签退' : '点到' }}
          </view>
          <view class="time">{{ formatTime(r.checkTime) }}</view>
        </view>
        <view class="item-right"><text class="method">{{ r.checkMethod || '-' }}</text></view>
      </view>
    </view>
    <view class="empty" v-else>暂无打卡记录</view>
  </view>
</template>

<script>
import config from '@/config.js'
function authHeader() { return { 'Authorization': 'Bearer ' + (uni.getStorageSync('appToken') || '') } }

export default {
  data() { return { list: [] } },
  onShow() { this.loadRecords() },
  methods: {
    async loadRecords() {
      const token = uni.getStorageSync('appToken'); if (!token) return
      try {
        const [err, res] = await uni.request({ url: config.baseUrl + '/app/checkin/list', header: authHeader() })
        if (res && res.data.code === 200) this.list = res.data.data
      } catch (e) {}
    },
    formatTime(t) {
      if (!t) return ''; const d = new Date(t); const p = n => n < 10 ? '0' + n : '' + n
      return d.getFullYear() + '-' + p(d.getMonth() + 1) + '-' + p(d.getDate()) + ' ' + p(d.getHours()) + ':' + p(d.getMinutes())
    }
  }
}
</script>

<style>
.container { padding: 20rpx; min-height: 100vh; background: #f5f5f5; }
.item { display: flex; justify-content: space-between; align-items: center; background: #fff; padding: 24rpx; border-radius: 12rpx; margin-bottom: 16rpx; }
.type-tag { font-size: 24rpx; color: #fff; padding: 6rpx 16rpx; border-radius: 6rpx; }
.type-tag.signin { background: #34c759; } .type-tag.signout { background: #ff3b30; } .type-tag.checkin {background: #ff9500; }
.time { font-size: 28rpx; color: #333; margin-top: 8rpx; }
.method { font-size: 24rpx; color: #999; }
.empty { text-align: center; margin-top: 100rpx; color: #999; font-size: 28rpx; }
</style>
