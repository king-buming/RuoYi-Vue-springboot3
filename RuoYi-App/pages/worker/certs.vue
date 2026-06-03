<template>
  <view class="container">
    <view class="top-bar"><text class="name">我的资质</text></view>
    <view class="list">
      <view class="item" v-for="(c, i) in list" :key="i">
        <view class="item-info">
          <text class="cname">{{ certLabels[c.certType] || c.certType }}{{ c.remark ? ' (' + c.remark + ')' : '' }}</text>
          <text class="cno">编号: {{ c.certNo || '-' }}</text>
          <text class="cexpire">到期: {{ c.expireDate || '-' }}</text>
        </view>
        <text :class="'badge ' + auditCls(c.auditStatus)">{{ auditLabel(c.auditStatus) }}</text>
      </view>
      <view class="empty" v-if="list.length === 0">暂无资质记录</view>
    </view>
  </view>
</template>

<script>
import config from '@/config.js'
function authHeader() { return { 'Authorization': 'Bearer ' + (uni.getStorageSync('appToken') || '') } }
export default {
  data() { return { list: [] } },
  computed: {
    certLabels() { return { id_card:'身份证', safe_cert:'安全员证', electric_cert:'电工证', supervisor_cert:'监理证', guardian_cert:'作业监护证', insurance:'保险' } }
  },
  onShow() { this.load() },
  methods: {
    async load() {
      try {
        const [e, r] = await uni.request({ url: config.baseUrl + '/app/worker/certs', header: authHeader() })
        if (r && r.data.code === 200) this.list = r.data.data
      } catch (e) {}
    },
    auditLabel(s) { const m = {'0':'待审核','1':'已通过','2':'已驳回','3':'已过期'}; return m[s] || s },
    auditCls(s) { const m = {'0':'warn','1':'ok','2':'err','3':'err'}; return m[s] || '' }
  }
}
</script>

<style scoped>
.container { padding: 20rpx; min-height: 100vh; background: #f5f5f5; }
.top-bar { background: #fff; border-radius: 12rpx; padding: 20rpx; margin-bottom: 16rpx; }
.name { font-size: 36rpx; font-weight: bold; }
.item { background: #fff; border-radius: 12rpx; padding: 20rpx; margin-bottom: 12rpx; display: flex; justify-content: space-between; align-items: center; }
.cname { font-size: 28rpx; font-weight: bold; color: #333; display: block; }
.cno { font-size: 24rpx; color: #666; margin-top: 6rpx; }
.cexpire { font-size: 24rpx; color: #ff9500; margin-top: 4rpx; }
.badge { font-size: 22rpx; padding: 4rpx 16rpx; border-radius: 20rpx; }
.badge.warn { background: #fff3e0; color: #ff9500; } .badge.ok { background: #e8f5e9; color: #34c759; } .badge.err { background: #ffebee; color: #ff3b30; }
.empty { text-align: center; margin-top: 80rpx; color: #999; font-size: 28rpx; }
</style>
