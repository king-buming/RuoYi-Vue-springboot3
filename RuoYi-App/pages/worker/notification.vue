<template>
  <view class="container">
    <view class="summary" v-if="list.length > 0">
      <text class="summary-text">未读消息 {{ unreadCount }} 条</text>
      <text class="summary-action" v-if="unreadCount > 0" @click="markAllRead">全部标为已读</text>
    </view>

    <view class="list" v-if="list.length > 0">
      <view class="item" v-for="n in list" :key="n.id" :class="{ unread: n.isRead === '0' }" @click="openItem(n)">
        <view class="item-head">
          <view class="title-wrap">
            <text class="dot" v-if="n.isRead === '0'"></text>
            <text class="title">{{ n.title || '消息通知' }}</text>
          </view>
          <text class="read-tag" :class="n.isRead === '0' ? 'tag-unread' : 'tag-read'">{{ n.isRead === '0' ? '未读' : '已读' }}</text>
        </view>
        <text class="content">{{ n.content || '-' }}</text>
        <view class="item-foot">
          <text class="type">{{ typeLabel(n.type) }}</text>
          <text class="time">{{ n.createTime || '' }}</text>
        </view>
      </view>
    </view>

    <view class="empty" v-else>
      <text>暂无消息通知</text>
    </view>
  </view>
</template>

<script>
import config from '@/config.js'

function authHeader() { return { 'Authorization': 'Bearer ' + (uni.getStorageSync('appToken') || '') } }

export default {
  data() {
    return {
      list: []
    }
  },
  computed: {
    unreadCount() {
      return this.list.filter(n => n.isRead === '0').length
    }
  },
  onShow() { this.loadList() },
  onPullDownRefresh() { this.loadList().then(() => uni.stopPullDownRefresh()) },
  methods: {
    async loadList() {
      const token = uni.getStorageSync('appToken')
      if (!token) return
      try {
        const [err, res] = await uni.request({ url: config.baseUrl + '/app/notification/list', header: authHeader() })
        if (res && res.data.code === 200) {
          this.list = (res.data.data || []).map(item => ({
            ...item,
            isRead: this.normalizeReadStatus(item)
          }))
        } else {
          uni.showToast({ title: (res && res.data.msg) || '消息加载失败', icon: 'none' })
        }
      } catch (e) {
        uni.showToast({ title: '消息加载失败', icon: 'none' })
      }
    },
    async openItem(item) {
      if (item.isRead !== '0') return
      await this.markRead(item)
    },
    async markRead(item) {
      try {
        const [err, res] = await uni.request({
          url: config.baseUrl + '/app/notification/' + item.id + '/read',
          method: 'PUT',
          header: authHeader()
        })
        if (res && res.data.code === 200) {
          item.isRead = '1'
          this.syncWorkbenchTabBadge(this.unreadCount)
        }
      } catch (e) {}
    },
    async markAllRead() {
      const unreadList = this.list.filter(n => n.isRead === '0')
      for (const item of unreadList) {
        await this.markRead(item)
      }
    },
    typeLabel(type) {
      const m = { audit: '审核通知', checkin: '打卡提醒', system: '系统消息' }
      return m[type] || '系统消息'
    },
    normalizeReadStatus(item) {
      const value = item.isRead != null ? item.isRead : item.is_read
      return String(value == null ? '0' : value)
    },
    syncWorkbenchTabBadge(count) {
      try {
        if (count > 0) {
          uni.setTabBarBadge({ index: 1, text: count > 99 ? '99+' : String(count) })
        } else {
          uni.removeTabBarBadge({ index: 1 })
          uni.hideTabBarRedDot({ index: 1 })
        }
      } catch (e) {}
    }
  }
}
</script>

<style>
.container { padding: 20rpx; min-height: 100vh; background: #f5f5f5; }
.summary { display: flex; justify-content: space-between; align-items: center; padding: 20rpx 24rpx; background: #fff; border-radius: 12rpx; margin-bottom: 16rpx; }
.summary-text { font-size: 26rpx; color: #666; }
.summary-action { font-size: 26rpx; color: #007aff; }
.item { background: #fff; padding: 24rpx; border-radius: 12rpx; margin-bottom: 16rpx; border-left: 8rpx solid transparent; }
.item.unread { border-left-color: #ff3b30; }
.item-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 14rpx; }
.title-wrap { display: flex; align-items: center; flex: 1; min-width: 0; }
.dot { width: 20rpx; height: 20rpx; background: #ff3b30; border-radius: 50%; margin-right: 14rpx; flex-shrink: 0; box-shadow: 0 0 0 6rpx rgba(255,59,48,0.12); }
.title { font-size: 30rpx; font-weight: bold; color: #333; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.read-tag { min-width: 74rpx; text-align: center; font-size: 23rpx; padding: 6rpx 16rpx; border-radius: 8rpx; margin-left: 18rpx; flex-shrink: 0; border: 1rpx solid transparent; }
.tag-unread { color: #ff3b30; background: #fff2f0; border-color: #ffccc7; }
.tag-read { color: #666; background: #f5f5f5; border-color: #ddd; }
.content { display: block; font-size: 26rpx; color: #666; line-height: 1.6; margin-bottom: 16rpx; }
.item-foot { display: flex; justify-content: space-between; align-items: center; }
.type { font-size: 24rpx; color: #007aff; }
.time { font-size: 24rpx; color: #999; }
.empty { text-align: center; margin-top: 120rpx; color: #999; font-size: 28rpx; }
</style>
