import request from '@/utils/request'

// 查询审核记录列表
export function listAudit(query) {
  return request({
    url: '/worker/audit/list',
    method: 'get',
    params: query
  })
}

// 查询审核记录详细
export function getAudit(id) {
  return request({
    url: '/worker/audit/' + id,
    method: 'get'
  })
}

// 新增审核记录
export function addAudit(data) {
  return request({
    url: '/worker/audit',
    method: 'post',
    data: data
  })
}

// 修改审核记录
export function updateAudit(data) {
  return request({
    url: '/worker/audit',
    method: 'put',
    data: data
  })
}

// 删除审核记录
export function delAudit(id) {
  return request({
    url: '/worker/audit/' + id,
    method: 'delete'
  })
}
