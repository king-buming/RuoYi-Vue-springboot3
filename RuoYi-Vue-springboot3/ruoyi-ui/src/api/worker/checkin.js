import request from '@/utils/request'

export function listCheckin(query) {
  return request({ url: '/worker/checkin/list', method: 'get', params: query })
}
export function getCheckin(id) {
  return request({ url: '/worker/checkin/' + id, method: 'get' })
}
export function addCheckin(data) {
  return request({ url: '/worker/checkin', method: 'post', data })
}
export function updateCheckin(data) {
  return request({ url: '/worker/checkin', method: 'put', data })
}
export function delCheckin(id) {
  return request({ url: '/worker/checkin/' + id, method: 'delete' })
}
