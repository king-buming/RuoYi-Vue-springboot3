<template>
  <view class="container">
    <view class="page-title">{{ certId ? '修改资质' : '上传资质' }}</view>
    <view class="form">
      <view class="input-group">
        <text class="label">证件类型</text>
        <picker :range="typeLabels" :value="typeIdx" @change="onType">{{ typeLabels[typeIdx]||'请选择' }}</picker>
      </view>
      <view class="input-group">
        <text class="label">证件编号</text>
        <input v-model="certNo" placeholder="请输入证件编号"/>
      </view>
      <view class="input-group">
        <text class="label">发证日期</text>
        <picker mode="date" :value="issueDate" @change="onIssue">{{ issueDate||'请选择' }}</picker>
      </view>
      <view class="input-group">
        <text class="label">过期日期</text>
        <picker mode="date" :value="expireDate" @change="onExpire">{{ expireDate||'请选择' }}</picker>
      </view>
      <view class="input-group">
        <text class="label">证件照片</text>
        <view class="img-list">
          <view v-for="(url,i) in imgUrls" :key="i" class="img-item">
            <image :src="url" class="preview" @click.stop="previewImg(i)"/>
            <text class="del" @click="delImg(i)">✕</text>
          </view>
          <view class="upload-box" @click="pickImg" v-if="imgUrls.length<6">
            <text class="placeholder">+ 添加照片</text>
          </view>
        </view>
      </view>
      <button class="btn" @click="submit" :disabled="loading">{{loading?'提交中...':(certId?'提交修改':'提交')}}</button>
      <button v-if="certId" class="delete-btn" @click="deleteCert" :disabled="loading">删除该资质</button>
    </view>
  </view>
</template>

<script>
import config from '@/config.js'
import upload from '@/utils/upload'
function authHeader(){return{'Authorization':'Bearer '+(uni.getStorageSync('appToken')||'')}}
let typeMap={}
export default {
  data(){return{certId:null,detailLoaded:false,typeLabels:[],typeValues:[],typeIdx:-1,certNo:'',issueDate:'',expireDate:'',imgUrls:[],loading:false}},
  onLoad(options){
    this.certId=options.certId||null
  },
  async onShow(){
    await this.loadDict()
    if(this.certId&&!this.detailLoaded)await this.loadDetail()
  },
  methods:{
    async loadDict(){
      if(!Object.keys(typeMap).length){
        try{const[e,r]=await uni.request({url:config.baseUrl+'/app/common/dicts?types=worker_cert_type'})
        if(r&&r.data.code===200){
          const list=(r.data.data['worker_cert_type']||[]).filter(o=>o.value!=='id_card')
          this.typeLabels=list.map(o=>o.label);this.typeValues=list.map(o=>o.value)
          list.forEach(o=>{typeMap[o.value]=o.label})
        }}catch(e){}
      }else{
        this.typeValues=Object.keys(typeMap).filter(v=>v!=='id_card')
        this.typeLabels=this.typeValues.map(v=>typeMap[v])
      }
    },
    async loadDetail(){
      try{
        const[e,r]=await uni.request({url:config.baseUrl+'/app/worker/certs/'+this.certId,header:authHeader()})
        if(r&&r.data.code===200){
          const c=r.data.data||{}
          this.certNo=c.certNo||''
          this.issueDate=c.issueDate||''
          this.expireDate=c.expireDate||''
          this.imgUrls=c.certImg?String(c.certImg).split(',').filter(Boolean):[]
          const idx=this.typeValues.indexOf(c.certType)
          if(idx>=0)this.typeIdx=idx
          this.detailLoaded=true
        }else{
          uni.showToast({title:(r&&r.data&&r.data.msg)||'证件不存在',icon:'none'})
        }
      }catch(e){uni.showToast({title:'加载失败',icon:'none'})}
    },
    onType(e){this.typeIdx=Number(e.detail.value)},
    onIssue(e){this.issueDate=e.detail.value},
    onExpire(e){this.expireDate=e.detail.value},
    delImg(i){this.imgUrls.splice(i,1)},
    previewImg(i){
      if(this.imgUrls.length)uni.previewImage({urls:this.imgUrls,current:this.imgUrls[i]})
    },
    pickImg(){
      uni.chooseImage({count:6-this.imgUrls.length,sourceType:['camera','album'],success:async res=>{
        uni.showLoading({title:'上传中...'})
        for(const fp of res.tempFilePaths){
          try{const r=await upload({url:'/common/upload',filePath:fp});this.imgUrls.push(r.url||r.data||r.fileName)}catch(e){}
        }
        uni.hideLoading()
      }})
    },
    async submit(){
      if(this.typeIdx<0||!this.certNo){uni.showToast({title:'请填写类型和编号',icon:'none'});return}
      this.loading=true
      try{
        const h=authHeader()
        const body={
          certType:this.typeValues[this.typeIdx],certNo:this.certNo,issueDate:this.issueDate,
          expireDate:this.expireDate,certImg:this.imgUrls.join(',')
        }
        const url=config.baseUrl+'/app/worker/certs'+(this.certId?'/'+this.certId:'')
        const method=this.certId?'PUT':'POST'
        const[e,r]=await uni.request({url,method,header:h,data:body})
        if(r&&r.data.code===200){
          this.loading=false;uni.showToast({title:this.certId?'修改已提交':'上传成功'});setTimeout(()=>uni.navigateBack(),500)
        }else{
          this.loading=false;uni.showToast({title:(r&&r.data&&r.data.msg)||'提交失败',icon:'none'})
        }
      }catch(e){this.loading=false;uni.showToast({title:'失败',icon:'none'})}
    },
    deleteCert(){
      uni.showModal({
        title:'确认删除',
        content:'确定删除该资质证书吗？删除后如需使用需要重新上传。',
        success:async res=>{
          if(!res.confirm)return
          this.loading=true
          try{
            const[e,r]=await uni.request({url:config.baseUrl+'/app/worker/certs/'+this.certId,method:'DELETE',header:authHeader()})
            this.loading=false
            if(r&&r.data.code===200){
              uni.showToast({title:'已删除'})
              setTimeout(()=>uni.navigateBack(),500)
            }else{
              uni.showToast({title:(r&&r.data&&r.data.msg)||'删除失败',icon:'none'})
            }
          }catch(e){
            this.loading=false
            uni.showToast({title:'删除失败',icon:'none'})
          }
        }
      })
    }
  }
}
</script>

<style>
.container{padding:20rpx;min-height:100vh;background:#f5f5f5}
.page-title{font-size:34rpx;font-weight:bold;color:#222;margin-bottom:16rpx}
.form{background:#fff;border-radius:12rpx;padding:24rpx}
.input-group{margin-bottom:24rpx}
.label{font-size:28rpx;color:#666;display:block;margin-bottom:8rpx}
.input-group input,.input-group picker{width:100%;box-sizing:border-box;border:1px solid #e5e5e5;border-radius:8rpx;padding:16rpx;font-size:28rpx}
.img-list{display:flex;flex-wrap:wrap;gap:12rpx}
.img-item{position:relative;width:150rpx;height:150rpx}
.preview{width:100%;height:100%;border-radius:8rpx;object-fit:cover}
.del{position:absolute;top:-10rpx;right:-10rpx;width:40rpx;height:40rpx;background:red;color:#fff;border-radius:50%;text-align:center;line-height:40rpx;font-size:24rpx}
.upload-box{width:150rpx;height:150rpx;background:#f0f0f0;border-radius:8rpx;display:flex;align-items:center;justify-content:center}
.placeholder{color:#999;font-size:26rpx}
.btn{background:#007aff;color:#fff;border-radius:8rpx;margin-top:20rpx;font-size:30rpx}
.delete-btn{background:#fff2f0;color:#ff3b30;border-radius:8rpx;margin-top:16rpx;font-size:30rpx}
</style>
