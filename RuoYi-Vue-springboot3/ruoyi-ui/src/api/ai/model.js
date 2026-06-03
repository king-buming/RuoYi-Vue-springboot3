import request from '@/utils/request'

// 查询AI模型列表
export function listModel(query) {
  return request({ url: '/ai/model/list', method: 'get', params: query })
}

// 查询AI模型详细
export function getModel(modelId) {
  return request({ url: '/ai/model/' + modelId, method: 'get' })
}

// 按编码查询AI模型
export function getModelByCode(modelCode) {
  return request({ url: '/ai/model/code/' + modelCode, method: 'get' })
}

// 新增AI模型
export function addModel(data) {
  return request({ url: '/ai/model', method: 'post', data: data })
}

// 修改AI模型
export function updateModel(data) {
  return request({ url: '/ai/model', method: 'put', data: data })
}

// 部署AI模型
export function deployModel(modelId) {
  return request({ url: '/ai/model/deploy/' + modelId, method: 'put' })
}

// 删除AI模型
export function delModel(modelIds) {
  return request({ url: '/ai/model/' + modelIds, method: 'delete' })
}
