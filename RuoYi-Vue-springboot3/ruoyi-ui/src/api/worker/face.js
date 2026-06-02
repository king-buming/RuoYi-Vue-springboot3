import request from '@/utils/request'

// 查询人脸信息列表
export function listFace(query) {
  return request({
    url: '/worker/face/list',
    method: 'get',
    params: query
  })
}

// 查询人脸信息详细
export function getFace(id) {
  return request({
    url: '/worker/face/' + id,
    method: 'get'
  })
}

// 新增人脸信息
export function addFace(data) {
  return request({
    url: '/worker/face',
    method: 'post',
    data: data
  })
}

// 修改人脸信息
export function updateFace(data) {
  return request({
    url: '/worker/face',
    method: 'put',
    data: data
  })
}

// 删除人脸信息
export function delFace(id) {
  return request({
    url: '/worker/face/' + id,
    method: 'delete'
  })
}
