import request from '@/utils/request'

// 查询角色规则列表
export function listRole(query) {
  return request({
    url: '/worker/role/list',
    method: 'get',
    params: query
  })
}

// 查询角色规则详细
export function getRole(id) {
  return request({
    url: '/worker/role/' + id,
    method: 'get'
  })
}

// 新增角色规则
export function addRole(data) {
  return request({
    url: '/worker/role',
    method: 'post',
    data: data
  })
}

// 修改角色规则
export function updateRole(data) {
  return request({
    url: '/worker/role',
    method: 'put',
    data: data
  })
}

// 删除角色规则
export function delRole(id) {
  return request({
    url: '/worker/role/' + id,
    method: 'delete'
  })
}
