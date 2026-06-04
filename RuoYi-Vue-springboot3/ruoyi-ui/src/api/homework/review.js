import request from '@/utils/request'

// 查询作业审核列表
export function listReview(query) {
  return request({ url: '/homework/review/list', method: 'get', params: query })
}

// 查询作业审核详细
export function getReview(reviewId) {
  return request({ url: '/homework/review/' + reviewId, method: 'get' })
}

// 审核通过
export function approveReview(data) {
  return request({ url: '/homework/review/approve', method: 'put', data: data })
}

// 审核驳回
export function rejectReview(data) {
  return request({ url: '/homework/review/reject', method: 'put', data: data })
}

// 删除审核记录
export function delReview(reviewIds) {
  return request({ url: '/homework/review/' + reviewIds, method: 'delete' })
}
