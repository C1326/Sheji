import { defineStore } from 'pinia'
import { login, register, getUserInfo } from '@/api/user'
import { resolveUrl } from '@/utils/url'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    userInfo: JSON.parse(localStorage.getItem('userInfo') || '{}')
  }),

  getters: {
    isLoggedIn: (state) => !!state.token,
    userId: (state) => state.userInfo.userId || state.userInfo.id,
    nickname: (state) => state.userInfo.nickname || '未登录',
    avatar: (state) => (state.userInfo.avatar ? resolveUrl(state.userInfo.avatar) : '')
  },

  actions: {
    // 登录（后端 login 直接返回 token 字符串）
    async loginAction(loginData) {
      try {
        const data = await login(loginData)
        this.token = data
        localStorage.setItem('token', data)

        // 登录成功后获取用户信息
        await this.getUserInfoAction()

        return data
      } catch (error) {
        throw error
      }
    },

    // 注册
    async registerAction(registerData) {
      try {
        const data = await register(registerData)
        return data
      } catch (error) {
        throw error
      }
    },

    // 获取用户信息
    async getUserInfoAction() {
      try {
        const data = await getUserInfo()
        this.userInfo = data
        localStorage.setItem('userInfo', JSON.stringify(data))
        return data
      } catch (error) {
        throw error
      }
    },

    // 登出（跳转由调用方决定）
    logout() {
      this.token = ''
      this.userInfo = {}
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
    },

    // 更新用户信息
    updateUserInfo(userInfo) {
      this.userInfo = { ...this.userInfo, ...userInfo }
      localStorage.setItem('userInfo', JSON.stringify(this.userInfo))
    }
  }
})
