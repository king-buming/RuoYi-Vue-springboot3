import request from '@/utils/request'

// 查询资质证件列表
export function listCert(query) {
  return request({
    url: '/worker/cert/list',
    method: 'get',
    params: query
  })
}

// 查询资质证件详细
export function getCert(id) {
  return request({
    url: '/worker/cert/' + id,
    method: 'get'
  })
}

// 新增资质证件
export function addCert(data) {
  return request({
    url: '/worker/cert',
    method: 'post',
    data: data
  })
}

// 修改资质证件
export function updateCert(data) {
  return request({
    url: '/worker/cert',
    method: 'put',
    data: data
  })
}

// 删除资质证件
export function delCert(id) {
  return request({
    url: '/worker/cert/' + id,
    method: 'delete'
  })
}
