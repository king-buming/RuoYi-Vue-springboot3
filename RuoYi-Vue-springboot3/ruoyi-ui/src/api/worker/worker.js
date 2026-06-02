import request from '@/utils/request'

// 查询人员档案列表
export function listWorker(query) {
  return request({
    url: '/worker/worker/list',
    method: 'get',
    params: query
  })
}

// 查询人员档案详细
export function getWorker(id) {
  return request({
    url: '/worker/worker/' + id,
    method: 'get'
  })
}

// 新增人员档案
export function addWorker(data) {
  return request({
    url: '/worker/worker',
    method: 'post',
    data: data
  })
}

// 修改人员档案
export function updateWorker(data) {
  return request({
    url: '/worker/worker',
    method: 'put',
    data: data
  })
}

// 删除人员档案
export function delWorker(id) {
  return request({
    url: '/worker/worker/' + id,
    method: 'delete'
  })
}

// 查询人员已分配角色 ID 列表
export function getWorkerRoles(workerId) {
  return request({
    url: '/worker/worker/' + workerId + '/roles',
    method: 'get'
  })
}

// 查询人员下拉选项（含已归档，供历史展示/查询用）
export function listWorkerOptions() {
  return request({ url: '/worker/worker/options', method: 'get' })
}
// 查询有效人员下拉选项（仅 del_flag='0'，供新增业务数据用）
export function listActiveWorkerOptions() {
  return request({ url: '/worker/worker/options/active', method: 'get' })
}

// 查询所有人员角色映射（列表展示用）
export function getAllRoleNames() {
  return request({ url: '/worker/worker/allRoleNames', method: 'get' })
}

// 查询人员关联数据
export function getWorkerCerts(workerId) {
  return request({ url: '/worker/worker/' + workerId + '/certs', method: 'get' })
}
export function getWorkerFaces(workerId) {
  return request({ url: '/worker/worker/' + workerId + '/faces', method: 'get' })
}
export function getWorkerAudits(workerId) {
  return request({ url: '/worker/worker/' + workerId + '/audits', method: 'get' })
}

// 保存人员角色关联
export function saveWorkerRoles(workerId, roleIds) {
  return request({
    url: '/worker/worker/' + workerId + '/roles',
    method: 'put',
    data: { roleIds }
  })
}
