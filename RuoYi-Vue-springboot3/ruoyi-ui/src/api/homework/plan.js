import request from '@/utils/request'

// 查询作业计划列表
export function listPlan(query) {
  return request({ url: '/homework/plan/list', method: 'get', params: query })
}

// 查询作业计划详细
export function getPlan(planId) {
  return request({ url: '/homework/plan/' + planId, method: 'get' })
}

// 新增作业计划
export function addPlan(data) {
  return request({ url: '/homework/plan', method: 'post', data: data })
}

// 修改作业计划
export function updatePlan(data) {
  return request({ url: '/homework/plan', method: 'put', data: data })
}

// 删除作业计划
export function delPlan(planIds) {
  return request({ url: '/homework/plan/' + planIds, method: 'delete' })
}

// 变更作业计划状态
export function changePlanStatus(data) {
  return request({ url: '/homework/plan/changeStatus', method: 'put', data: data })
}

// 获取计划的关联人员
export function getPlanWorkers(planId) {
  return request({ url: '/homework/plan/' + planId + '/workers', method: 'get' })
}

// 保存计划的关联人员
export function savePlanWorkers(planId, workerList) {
  return request({ url: '/homework/plan/' + planId + '/workers', method: 'post', data: workerList })
}
