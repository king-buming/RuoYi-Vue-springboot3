import request from '@/utils/request'

// 查询人脸注册列表（含tb_worker联表数据）
export function listFaceRegister(query) {
  return request({ url: '/ai/face/list', method: 'get', params: query })
}

// 查询单条注册记录
export function getFaceRegister(registerId) {
  return request({ url: '/ai/face/' + registerId, method: 'get' })
}

// 单个人脸录入
export function registerFace(workerId, modelCode) {
  return request({
    url: '/ai/face/register/' + workerId,
    method: 'post',
    params: { modelCode: modelCode || 'arcface_r100_001' }
  })
}

// 批量人脸录入
export function batchRegister(data) {
  return request({ url: '/ai/face/batchRegister', method: 'post', data: data })
}

// 取消注册
export function cancelRegister(registerId) {
  return request({ url: '/ai/face/cancel/' + registerId, method: 'put' })
}

// 删除注册记录
export function delFaceRegister(registerIds) {
  return request({ url: '/ai/face/' + registerIds, method: 'delete' })
}
