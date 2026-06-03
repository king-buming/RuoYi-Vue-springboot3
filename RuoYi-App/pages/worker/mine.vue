<template>
  <view class="container">
    <view class="card" v-if="info.workerId">
      <view class="row"><text class="lbl">姓名</text><text class="val">{{ info.workerName }}</text></view>
      <view class="row"><text class="lbl">手机号</text><text class="val">{{ info.phone }}</text></view>
      <view class="row"><text class="lbl">审核状态</text><text class="val"><text :class="'badge '+auditCls(info.auditStatus)">{{ auditLabel(info.auditStatus) }}</text></text></view>
      <view class="row"><text class="lbl">人脸录入</text><text class="val">{{ info.faceStatus==='1'?'✅ 已录入':'⚠ 未录入' }}</text></view>
    </view>

    <view class="section-title">身份证</view>
    <view class="list" v-if="idcards.length">
      <view class="item" v-for="(c,i) in idcards" :key="i">
        <image v-if="c.certImg" :src="c.certImg" class="thumb"/>
        <view class="item-info">
          <text class="name">{{ c.remark||'身份证' }}</text>
          <text class="sub">编号: {{ c.certNo||'-' }}  到期: {{ c.expireDate||'-' }}</text>
        </view>
        <text :class="'badge '+auditCls(c.auditStatus)">{{ auditLabel(c.auditStatus) }}</text>
      </view>
    </view>
    <view class="empty" v-else>暂无，<text class="link" @click="goPage('idcard')">上传身份证</text></view>

    <view class="section-title">资质证书</view>
    <view class="list" v-if="certs.length">
      <view class="item" v-for="(c,i) in certs" :key="i">
        <image v-if="c.certImg" :src="c.certImg" class="thumb"/>
        <view class="item-info">
          <text class="name">{{ certName(c.certType) }}</text>
          <text class="sub">编号: {{ c.certNo||'-' }}  到期: {{ c.expireDate||'-' }}</text>
        </view>
        <text :class="'badge '+auditCls(c.auditStatus)">{{ auditLabel(c.auditStatus) }}</text>
      </view>
    </view>
    <view class="empty" v-else>暂无，<text class="link" @click="goPage('upload')">上传资质</text></view>

    <button class="btn" @click="goPage('face')">📷 人脸上传</button>
    <view style="height:30px"></view>
  </view>
</template>

<script>
import config from '@/config.js'
function authHeader(){return{'Authorization':'Bearer '+(uni.getStorageSync('appToken')||'')}}
const labels={0:['待审核','warn'],1:['已通过','ok'],2:['已驳回','err'],3:['已过期','err']}
let certMap={}
export default {
  data(){return{info:{},idcards:[],certs:[]}},
  onShow(){this.load()},
  methods:{
    async load(){
      const token=uni.getStorageSync('appToken');if(!token)return
      try{
        const[e,r]=await uni.request({url:config.baseUrl+'/app/auth/me',header:authHeader()})
        if(r&&r.data.code===200)this.info=r.data.data
      }catch(e){}
      // 加载字典
      if(!Object.keys(certMap).length){
        try{const[e,r]=await uni.request({url:config.baseUrl+'/app/common/dicts?types=worker_cert_type'})
        if(r&&r.data.code===200){(r.data.data['worker_cert_type']||[]).forEach(o=>{certMap[o.value]=o.label})}}catch(e){}
      }
      // 加载证件
      try{
        const[e,r]=await uni.request({url:config.baseUrl+'/app/worker/certs',header:authHeader()})
        if(r&&r.data.code===200){
          const all=r.data.data||[]
          this.idcards=all.filter(c=>c.certType==='id_card')
          this.certs=all.filter(c=>c.certType!=='id_card')
        }
      }catch(e){}
    },
    auditLabel(s){return(labels[s]||[s,''])[0]},
    auditCls(s){return(labels[s]||[s,''])[1]},
    certName(t){return certMap[t]||t},
    goPage(p){uni.navigateTo({url:'/pages/worker/'+p})}
  }
}
</script>

<style>
.container{padding:20rpx;min-height:100vh;background:#f5f5f5}
.card{background:#fff;border-radius:12rpx;padding:20rpx;margin-bottom:16rpx}
.row{display:flex;justify-content:space-between;padding:12rpx 0;border-bottom:1px solid #f0f0f0;font-size:28rpx}
.row:last-child{border:none}.lbl{color:#999}.val{color:#333;font-weight:500}
.section-title{font-size:30rpx;font-weight:bold;color:#333;margin:20rpx 0 12rpx}
.list{margin-bottom:12rpx}
.item{background:#fff;border-radius:12rpx;padding:16rpx;margin-bottom:10rpx;display:flex;align-items:center;gap:12rpx}
.thumb{width:80rpx;height:55rpx;border-radius:6rpx;object-fit:cover}
.item-info{flex:1}.name{font-size:28rpx;font-weight:bold;color:#333;display:block}
.sub{font-size:22rpx;color:#666;margin-top:4rpx}
.badge{font-size:22rpx;padding:4rpx 16rpx;border-radius:20rpx}
.badge.warn{background:#fff3e0;color:#ff9500}.badge.ok{background:#e8f5e9;color:#34c759}.badge.err{background:#ffebee;color:#ff3b30}
.empty{text-align:center;color:#999;font-size:26rpx;padding:16rpx}
.link{color:#007aff}
.btn{background:#007aff;color:#fff;border-radius:8rpx;margin-top:16rpx;font-size:30rpx}
</style>
