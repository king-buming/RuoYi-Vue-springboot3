import request from '@/utils/request'

// 查询人员列表
export function listWorker(query) {
  return request({
    url: '/homework/worker/list',
    method: 'get',
    params: query
  })
}

// 查询人员详细
export function getWorker(id) {
  return request({
    url: '/homework/worker/' + id,
    method: 'get'
  })
}

// 新增人员
export function addWorker(data) {
  return request({
    url: '/homework/worker',
    method: 'post',
    data: data
  })
}

// 修改人员
export function updateWorker(data) {
  return request({
    url: '/homework/worker',
    method: 'put',
    data: data
  })
}

// 删除人员
export function delWorker(ids) {
  return request({
    url: '/homework/worker/' + ids,
    method: 'delete'
  })
}
