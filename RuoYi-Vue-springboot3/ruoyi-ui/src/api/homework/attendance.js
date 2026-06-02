import request from '@/utils/request'

// 查询打卡记录列表
export function listAttendance(query) {
  return request({
    url: '/homework/attendance/list',
    method: 'get',
    params: query
  })
}

// 查询打卡记录详细
export function getAttendance(id) {
  return request({
    url: '/homework/attendance/' + id,
    method: 'get'
  })
}

// 进场打卡
export function checkIn(data) {
  return request({
    url: '/homework/attendance/checkIn',
    method: 'post',
    data: data
  })
}

// 离场打卡
export function checkOut(data) {
  return request({
    url: '/homework/attendance/checkOut',
    method: 'post',
    data: data
  })
}

// 删除打卡记录
export function delAttendance(ids) {
  return request({
    url: '/homework/attendance/' + ids,
    method: 'delete'
  })
}
