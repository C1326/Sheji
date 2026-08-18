import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import router from '@/router'

// 创建axios实例
const request = axios.create({
  baseURL: '/api',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器
request.interceptors.request.use(
  config => {
    const userStore = useUserStore()
    if (userStore.token) {
      config.headers.Authorization = `Bearer ${userStore.token}`
    }
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

// 响应拦截器
request.interceptors.response.use(
  response => {
    // blob 响应（文件下载）直接返回，不参与 code 判断
    if (response.config.responseType === 'blob') {
      const blob = response.data
      // 后端业务错误返回的是 HTTP 200 + JSON（如下载无权限），此时 blob 实际是 JSON 错误信息
      if (blob && blob.type && blob.type.includes('application/json')) {
        return blob.text().then(text => {
          let msg = '操作失败'
          try {
            msg = JSON.parse(text).message || msg
          } catch (e) { /* 忽略解析失败 */ }
          ElMessage.error(msg)
          return Promise.reject(new Error(msg))
        })
      }
      return blob
    }
    const res = response.data
    // 根据后端返回的code判断
    if (res.code === 200 || res.code === 0) {
      return res.data
    } else {
      ElMessage.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message || '请求失败'))
    }
  },
  error => {
    if (error.response) {
      switch (error.response.status) {
        case 401:
          ElMessage.error('登录已过期，请重新登录')
          const userStore = useUserStore()
          userStore.logout()
          router.push('/login')
          break
        case 403:
          ElMessage.error('没有权限访问')
          break
        case 404:
          ElMessage.error('请求资源不存在')
          break
        case 500:
          ElMessage.error('服务器错误')
          break
        default:
          ElMessage.error(error.response.data.message || '请求失败')
      }
    } else {
      ElMessage.error('网络连接失败')
    }
    return Promise.reject(error)
  }
)

export default request
