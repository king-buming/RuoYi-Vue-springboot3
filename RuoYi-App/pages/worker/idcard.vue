<template>
  <view class="container">
    <view class="page-title">{{ editMode ? '修改身份证' : '上传身份证' }}</view>
    <view class="form">
      <view class="input-group">
        <text class="label">身份证号</text>
        <input v-model="idNo" placeholder="请输入身份证号" maxlength="18"/>
      </view>
      <view class="input-group">
        <text class="label">到期日期</text>
        <picker mode="date" :value="expireDate" @change="onDate">{{ expireDate||'请选择' }}</picker>
      </view>
      <view class="input-group">
        <text class="label">人像面</text>
        <view class="upload-box" @click="pickImg('front')">
          <image v-if="frontUrl" :src="frontUrl" class="preview" @click.stop="previewSide('front')"/>
          <text v-else class="placeholder">点击拍照/选择图片</text>
        </view>
      </view>
      <view class="input-group">
        <text class="label">国徽面</text>
        <view class="upload-box" @click="pickImg('back')">
          <image v-if="backUrl" :src="backUrl" class="preview" @click.stop="previewSide('back')"/>
          <text v-else class="placeholder">点击拍照/选择图片</text>
        </view>
      </view>
      <button class="btn" @click="submit" :disabled="loading">{{loading?'提交中...':(editMode?'提交修改':'提交')}}</button>
    </view>
  </view>
</template>

<script>
import config from '@/config.js'
import upload from '@/utils/upload'
function authHeader(){return{'Authorization':'Bearer '+(uni.getStorageSync('appToken')||'')}}
export default {
  data(){return{editMode:false,frontId:null,backId:null,idNo:'',expireDate:'',frontUrl:'',backUrl:'',loading:false}},
  onLoad(options){
    this.editMode=options.mode==='edit'||options.edit==='1'
    if(this.editMode)this.loadExisting()
  },
  methods:{
    async loadExisting(){
      try{
        const[e,r]=await uni.request({url:config.baseUrl+'/app/worker/certs',header:authHeader()})
        if(r&&r.data.code===200){
          const list=(r.data.data||[]).filter(c=>c.certType==='id_card')
          const front=list.find(c=>(c.remark||'').indexOf('人像')>=0)||list[0]
          const back=list.find(c=>(c.remark||'').indexOf('国徽')>=0)||list.find(c=>front&&c.id!==front.id)
          if(front){
            this.frontId=front.id
            this.idNo=front.certNo||''
            this.expireDate=front.expireDate||''
            this.frontUrl=front.certImg||''
          }
          if(back){
            this.backId=back.id
            if(!this.idNo)this.idNo=back.certNo||''
            if(!this.expireDate)this.expireDate=back.expireDate||''
            this.backUrl=back.certImg||''
          }
        }
      }catch(e){uni.showToast({title:'加载失败',icon:'none'})}
    },
    onDate(e){this.expireDate=e.detail.value},
    previewSide(side){
      const urls=[this.frontUrl,this.backUrl].filter(Boolean)
      const current=side==='front'?this.frontUrl:this.backUrl
      if(urls.length)uni.previewImage({urls,current})
    },
    pickImg(side){
      uni.chooseImage({count:1,sourceType:['camera','album'],success:async res=>{
        uni.showLoading({title:'上传中...'})
        try{const r=await upload({url:'/common/upload',filePath:res.tempFilePaths[0]})
          const url=r.url||r.data||r.fileName
          if(side==='front')this.frontUrl=url;else this.backUrl=url
          uni.hideLoading()
        }catch(e){uni.hideLoading();uni.showToast({title:'上传失败',icon:'none'})}
      }})
    },
    async submit(){
      if(!this.idNo||!this.frontUrl||!this.backUrl){uni.showToast({title:'请填写完整',icon:'none'});return}
      this.loading=true
      try{
        const h=authHeader()
        const b1={certType:'id_card',certNo:this.idNo,certImg:this.frontUrl,expireDate:this.expireDate,remark:'人像面'}
        const b2={certType:'id_card',certNo:this.idNo,certImg:this.backUrl,expireDate:this.expireDate,remark:'国徽面'}
        const r1=await this.saveCert(this.frontId,b1,h)
        const r2=await this.saveCert(this.backId,b2,h)
        if(r1&&r2){
          this.loading=false;uni.showToast({title:this.editMode?'修改已提交':'上传成功'});setTimeout(()=>uni.navigateBack(),500)
        }else{
          this.loading=false;uni.showToast({title:'提交失败',icon:'none'})
        }
      }catch(e){this.loading=false;uni.showToast({title:'失败',icon:'none'})}
    },
    async saveCert(id,body,header){
      const url=config.baseUrl+'/app/worker/certs'+(id?'/'+id:'')
      const method=id?'PUT':'POST'
      const[e,r]=await uni.request({url,method,header,data:body})
      if(r&&r.data.code===200)return true
      uni.showToast({title:(r&&r.data&&r.data.msg)||'提交失败',icon:'none'})
      return false
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
.input-group input{width:100%;box-sizing:border-box;border:1px solid #e5e5e5;border-radius:8rpx;padding:16rpx;font-size:28rpx}
.upload-box{width:100%;height:200rpx;background:#f0f0f0;border-radius:8rpx;display:flex;align-items:center;justify-content:center}
.preview{width:100%;height:100%;object-fit:cover;border-radius:8rpx}
.placeholder{color:#999;font-size:28rpx}
.btn{background:#007aff;color:#fff;border-radius:8rpx;margin-top:20rpx;font-size:30rpx}
</style>
