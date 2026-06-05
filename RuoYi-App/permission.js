import { getToken } from '@/utils/auth'

// 工人端登录页
const workerLoginPage = "/pages/worker/login"
// 系统端登录页（原有）
const loginPage = "/pages/login"

// 白名单：无需登录即可访问
const whiteList = [
  '/pages/worker/login', '/pages/login', '/pages/register', '/pages/common/webview/index',
  '/pages/worker/checkin', '/pages/worker/records', '/pages/worker/workbench', '/pages/worker/notification', '/pages/worker/mine',
  '/pages/worker/idcard', '/pages/worker/upload', '/pages/worker/face',
  '/pages/worker/plan/list', '/pages/worker/plan/detail', '/pages/worker/plan/create',
  '/pages/worker/review/list', '/pages/worker/review/detail'
]

function needAuth(url) {
  const path = url.split('?')[0]
  return !whiteList.includes(path)
}

// 页面跳转验证拦截器
let list = ["navigateTo", "redirectTo", "reLaunch", "switchTab"]
list.forEach(item => {
  uni.addInterceptor(item, {
    invoke(to) {
      const hasSysToken = !!getToken()           // 系统登录 token
      const hasAppToken = !!uni.getStorageSync('appToken')  // 工人端 token

      // 如果已登录但还往登录页跳，直接跳到首页
      if (to.url === loginPage && hasSysToken) {
        uni.reLaunch({ url: "/" }); return false
      }
      if (to.url === workerLoginPage && hasAppToken) {
        uni.reLaunch({ url: "/pages/worker/checkin" }); return false
      }

      // 需要鉴权的页面
      if (needAuth(to.url)) {
        if (to.url.startsWith('/pages/worker/')) {
          // 工人端页面走 appToken
          if (!hasAppToken) {
            uni.reLaunch({ url: workerLoginPage }); return false
          }
        } else {
          // 系统页面走原有 token
          if (!hasSysToken) {
            uni.reLaunch({ url: loginPage }); return false
          }
        }
      }
      return true
    },
    fail(err) { console.log(err) }
  })
})
