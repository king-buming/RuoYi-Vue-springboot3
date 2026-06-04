<template>
  <view class="container">
    <uni-forms ref="form" :modelValue="form" label-position="top">
      <uni-forms-item label="项目名称" required name="projectName">
        <uni-easyinput v-model="form.projectName" placeholder="请输入项目名称" />
      </uni-forms-item>

      <uni-forms-item label="市/县" name="cityCounty">
        <uni-easyinput v-model="form.cityCounty" placeholder="请输入市/县" />
      </uni-forms-item>

      <uni-forms-item label="施工点" name="constructionSite">
        <uni-easyinput v-model="form.constructionSite" placeholder="请输入施工点" />
      </uni-forms-item>

      <uni-forms-item label="作业类型" required name="workType">
        <picker :value="workTypeIndex" :range="workTypeNames" @change="onWorkTypeChange">
          <view class="picker-val" :class="{ placeholder: !form.workType }">
            {{ form.workType || '请选择作业类型' }}
          </view>
        </picker>
      </uni-forms-item>

      <uni-forms-item label="计划作业时间" name="planWorkTime">
        <picker mode="multiSelector" :value="dateTimeIndex" :range="dateTimeRange" @change="onDateTimeChange" @columnchange="onColumnChange">
          <view class="picker-val" :class="{ placeholder: !form.planWorkTime }">
            {{ form.planWorkTime || '请选择时间' }}
          </view>
        </picker>
      </uni-forms-item>

      <uni-forms-item label="施工单位" name="constructionUnit">
        <uni-easyinput v-model="form.constructionUnit" placeholder="请输入施工单位" />
      </uni-forms-item>

      <uni-forms-item label="作业内容" name="workContent">
        <uni-easyinput v-model="form.workContent" type="textarea" placeholder="请输入作业内容" />
      </uni-forms-item>

      <uni-forms-item label="备注" name="remark">
        <uni-easyinput v-model="form.remark" type="textarea" placeholder="请输入备注" />
      </uni-forms-item>
    </uni-forms>

    <view class="submit-bar">
      <button class="btn-submit" :disabled="submitting" @click="submit">
        {{ submitting ? '提交中...' : '提交计划' }}
      </button>
    </view>
  </view>
</template>

<script>
import config from '@/config.js'

function authHeader() { return { 'Authorization': 'Bearer ' + (uni.getStorageSync('appToken') || '') } }

function pad(n) { return n < 10 ? '0' + n : '' + n }

export default {
  data() {
    const now = new Date()
    const years = []; for (let y = now.getFullYear(); y <= now.getFullYear() + 2; y++) years.push('' + y)
    const months = []; for (let m = 1; m <= 12; m++) months.push(pad(m))
    const days = []; for (let d = 1; d <= 31; d++) days.push(pad(d))
    const hours = []; for (let h = 0; h < 24; h++) hours.push(pad(h))
    const mins = []; for (let m = 0; m < 60; m++) mins.push(pad(m))
    return {
      form: { projectName: '', cityCounty: '', constructionSite: '', workType: '', planWorkTime: '', constructionUnit: '', workContent: '', remark: '' },
      workTypeOptions: ['动土', '防腐', '检测', '临时用电', '受限空间', '机械作业', '修复', '点火'],
      dateTimeRange: [years, months, days, hours, mins],
      dateTimeValue: [0, now.getMonth(), now.getDate() - 1, now.getHours(), now.getMinutes()],
      submitting: false
    }
  },
  computed: {
    workTypeNames() { return this.workTypeOptions },
    workTypeIndex() {
      const i = this.workTypeOptions.indexOf(this.form.workType)
      return i >= 0 ? i : 0
    },
    dateTimeIndex() {
      return this.dateTimeValue
    }
  },
  methods: {
    onWorkTypeChange(e) {
      this.form.workType = this.workTypeOptions[e.detail.value]
    },
    onDateTimeChange(e) {
      const v = e.detail.value
      this.dateTimeValue = v
      const r = this.dateTimeRange
      this.form.planWorkTime = r[0][v[0]] + '-' + r[1][v[1]] + '-' + r[2][v[2]] + ' ' + r[3][v[3]] + ':' + r[4][v[4]] + ':00'
    },
    onColumnChange(e) {
      // 动态调整日期天数（简化：固定31天）
    },
    async submit() {
      if (!this.form.projectName) { uni.showToast({ title: '请输入项目名称', icon: 'none' }); return }
      if (!this.form.workType) { uni.showToast({ title: '请选择作业类型', icon: 'none' }); return }
      this.submitting = true
      try {
        const token = uni.getStorageSync('appToken')
        if (!token) { uni.reLaunch({ url: '/pages/worker/login' }); return }
        const [err, res] = await uni.request({
          url: config.baseUrl + '/app/homework/plan',
          method: 'POST',
          header: authHeader(),
          data: this.form
        })
        this.submitting = false
        if (res && res.data.code === 200) {
          uni.showToast({ title: '计划创建成功' })
          setTimeout(() => { uni.navigateBack() }, 1000)
        } else {
          uni.showToast({ title: (res && res.data.msg) || '创建失败', icon: 'none' })
        }
      } catch (e) { this.submitting = false; uni.showToast({ title: '网络错误', icon: 'none' }) }
    }
  }
}
</script>

<style>
.container { padding: 20rpx; min-height: 100vh; background: #f5f5f5; padding-bottom: 120rpx; }

.picker-val { padding: 20rpx 0; font-size: 28rpx; color: #333; border-bottom: 1rpx solid #e5e5e5; }
.picker-val.placeholder { color: #ccc; }

.submit-bar { position: fixed; bottom: 0; left: 0; right: 0; padding: 20rpx 30rpx; background: #fff; box-shadow: 0 -2rpx 8rpx rgba(0,0,0,0.04); }
.btn-submit { width: 100%; height: 88rpx; line-height: 88rpx; background: #007aff; color: #fff; font-size: 32rpx; border-radius: 12rpx; text-align: center; border: none; }
.btn-submit[disabled] { background: #ccc; }
</style>
