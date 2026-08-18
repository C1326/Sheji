import { defineStore } from 'pinia'
import { getMyDiaryList, getPublicDiaryList, getDiaryDetail } from '@/api/diary'

export const useDiaryStore = defineStore('diary', {
  state: () => ({
    myDiaryList: [],
    publicDiaryList: [],
    currentDiary: null,
    pagination: {
      page: 1,
      pageSize: 10,
      total: 0
    }
  }),

  actions: {
    // 获取我的日记列表
    async getMyDiaryListAction(params = {}) {
      try {
        const data = await getMyDiaryList({
          page: this.pagination.page,
          pageSize: this.pagination.pageSize,
          ...params
        })
        this.myDiaryList = data.records || data.list || []
        this.pagination.total = data.total || 0
        return data
      } catch (error) {
        throw error
      }
    },

    // 获取公开日记列表
    async getPublicDiaryListAction(params = {}) {
      try {
        const data = await getPublicDiaryList({
          page: this.pagination.page,
          pageSize: this.pagination.pageSize,
          ...params
        })
        this.publicDiaryList = data.records || data.list || []
        this.pagination.total = data.total || 0
        return data
      } catch (error) {
        throw error
      }
    },

    // 获取日记详情
    async getDiaryDetailAction(diaryId) {
      try {
        const data = await getDiaryDetail(diaryId)
        this.currentDiary = data
        return data
      } catch (error) {
        throw error
      }
    },

    // 重置分页
    resetPagination() {
      this.pagination = {
        page: 1,
        pageSize: 10,
        total: 0
      }
    },

    // 设置分页
    setPagination(page, pageSize) {
      this.pagination.page = page
      this.pagination.pageSize = pageSize
    }
  }
})
