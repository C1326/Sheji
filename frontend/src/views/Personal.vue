<template>
  <div class="personal-container">
    <!-- 顶部导航 -->
    <header class="header">
      <div class="header-content">
        <div class="logo" @click="goHome">
          <el-icon :size="28"><Notebook /></el-icon>
          <span>佘记</span>
        </div>
        
        <div class="nav-actions">
          <el-button type="primary" @click="goWrite">
            <el-icon><Edit /></el-icon>
            写日记
          </el-button>
          <el-dropdown @command="handleCommand">
            <div class="user-info">
              <el-avatar :size="36" :src="userStore.avatar">
                {{ userStore.nickname.charAt(0) }}
              </el-avatar>
              <span class="nickname">{{ userStore.nickname }}</span>
              <el-icon><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="home">
                  <el-icon><HomeFilled /></el-icon>
                  公开广场
                </el-dropdown-item>
                <el-dropdown-item command="logout" divided>
                  <el-icon><SwitchButton /></el-icon>
                  退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
    </header>

    <!-- 主要内容 -->
    <main class="main-content">
      <div class="container">
        <!-- 用户信息卡片 -->
        <div class="user-card">
          <div class="user-avatar">
            <el-avatar :size="80" :src="userStore.avatar">
              {{ userStore.nickname.charAt(0) }}
            </el-avatar>
            <div class="user-info-text">
              <h2>{{ userStore.nickname }}</h2>
              <p class="username">@{{ userStore.userInfo.username }}</p>
              <p class="join-time">加入于 {{ formatDate(userStore.userInfo.createTime) }}</p>
            </div>
          </div>
          <div class="user-stats">
            <div class="stat-item">
              <span class="stat-value">{{ diaryList.length }}</span>
              <span class="stat-label">日记数</span>
            </div>
            <div class="stat-item">
              <span class="stat-value">{{ publicCount }}</span>
              <span class="stat-label">公开日记</span>
            </div>
            <div class="stat-item">
              <span class="stat-value">{{ privateCount }}</span>
              <span class="stat-label">私有日记</span>
            </div>
          </div>
        </div>

        <!-- 我的日记列表 -->
        <div class="diary-section">
          <div class="section-header">
            <h3>我的日记</h3>
            <el-button type="primary" @click="goWrite">
              <el-icon><Plus /></el-icon>
              写日记
            </el-button>
          </div>

          <!-- 日记列表 -->
          <div v-if="diaryList.length > 0" class="diary-list">
            <div
              v-for="diary in diaryList"
              :key="diary.diaryId || diary.id"
              class="diary-item"
            >
              <div class="diary-info" @click="goDetail(diary.diaryId || diary.id)">
                <div class="diary-header">
                  <h4 class="diary-title">{{ diary.title }}</h4>
                  <el-tag :type="diary.permission === 1 ? 'success' : 'info'" size="small">
                    {{ diary.permission === 1 ? '公开' : '私有' }}
                  </el-tag>
                </div>
                <p class="diary-content">{{ stripHtml(diary.content) }}</p>
                <div class="diary-footer">
                  <span class="diary-time">{{ formatDate(diary.createTime) }}</span>
                  <span v-if="diary.updateTime && diary.updateTime !== diary.createTime" class="diary-update">
                    · 已编辑 {{ formatDate(diary.updateTime) }}
                  </span>
                </div>
              </div>
              <div class="diary-actions">
                <el-button text @click="handleEdit(diary.diaryId || diary.id)">
                  <el-icon><Edit /></el-icon>
                  编辑
                </el-button>
                <el-button text type="danger" @click="handleDelete(diary.diaryId || diary.id)">
                  <el-icon><Delete /></el-icon>
                  删除
                </el-button>
              </div>
            </div>
          </div>

          <!-- 空状态 -->
          <el-empty v-else-if="!loading" description="还没有写过日记">
            <el-button type="primary" @click="goWrite">写第一篇日记</el-button>
          </el-empty>

          <!-- 加载状态 -->
          <div v-if="loading" class="loading-container">
            <el-icon class="is-loading" :size="40"><Loading /></el-icon>
            <p>加载中...</p>
          </div>

          <!-- 分页 -->
          <div v-if="total > pageSize" class="pagination-container">
            <el-pagination
              v-model:current-page="currentPage"
              :page-size="pageSize"
              :total="total"
              layout="prev, pager, next"
              background
              @current-change="handlePageChange"
            />
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { getMyDiaryList, deleteDiary } from '@/api/diary'

const router = useRouter()
const userStore = useUserStore()

const diaryList = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 计算公开和私有日记数量
const publicCount = computed(() => {
  return diaryList.value.filter(d => d.permission === 1).length
})

const privateCount = computed(() => {
  return diaryList.value.filter(d => d.permission === 0).length
})

// 获取我的日记列表
const fetchDiaryList = async () => {
  loading.value = true
  try {
    const data = await getMyDiaryList({
      page: currentPage.value,
      pageSize: pageSize.value
    })
    diaryList.value = data.records || data.list || []
    total.value = data.total || 0
  } catch (error) {
    console.error('获取日记列表失败:', error)
  } finally {
    loading.value = false
  }
}

// 去除HTML标签
const stripHtml = (html) => {
  if (!html) return ''
  const text = html.replace(/<[^>]+>/g, '').replace(/&nbsp;/g, ' ')
  return text.length > 150 ? text.substring(0, 150) + '...' : text
}

// 格式化日期
const formatDate = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

// 页码改变
const handlePageChange = (page) => {
  currentPage.value = page
  fetchDiaryList()
}

// 编辑日记
const handleEdit = (id) => {
  router.push(`/diary/edit/${id}`)
}

// 删除日记
const handleDelete = async (id) => {
  try {
    await ElMessageBox.confirm('确定要删除这篇日记吗？删除后无法恢复', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    await deleteDiary(id)
    ElMessage.success('删除成功')
    fetchDiaryList()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
    }
  }
}

// 下拉菜单命令
const handleCommand = (command) => {
  if (command === 'home') {
    router.push('/home')
  } else if (command === 'logout') {
    userStore.logout()
    ElMessage.success('已退出登录')
    router.push('/home')
  }
}

// 跳转方法
const goHome = () => router.push('/home')
const goWrite = () => router.push('/diary/write')
const goDetail = (id) => router.push(`/diary/detail/${id}`)

onMounted(() => {
  fetchDiaryList()
})
</script>

<style scoped>
.personal-container {
  min-height: 100vh;
  background-color: #f5f7fa;
}

.header {
  background: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  position: sticky;
  top: 0;
  z-index: 100;
}

.header-content {
  max-width: 1200px;
  margin: 0 auto;
  padding: 15px 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.logo {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  font-size: 24px;
  font-weight: 600;
  color: #667eea;
}

.nav-actions {
  display: flex;
  align-items: center;
  gap: 15px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 5px 10px;
  border-radius: 20px;
  transition: background-color 0.3s;
}

.user-info:hover {
  background-color: #f5f7fa;
}

.nickname {
  font-size: 14px;
  color: #333;
}

.main-content {
  padding: 30px 0;
}

.container {
  max-width: 900px;
  margin: 0 auto;
  padding: 0 20px;
}

.user-card {
  background: #fff;
  border-radius: 8px;
  padding: 30px;
  margin-bottom: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.user-avatar {
  display: flex;
  align-items: center;
  gap: 20px;
}

.user-info-text h2 {
  font-size: 24px;
  color: #333;
  margin-bottom: 5px;
}

.username {
  color: #999;
  font-size: 14px;
  margin-bottom: 5px;
}

.join-time {
  color: #999;
  font-size: 12px;
}

.user-stats {
  display: flex;
  gap: 40px;
}

.stat-item {
  text-align: center;
}

.stat-value {
  display: block;
  font-size: 24px;
  font-weight: 600;
  color: #667eea;
}

.stat-label {
  display: block;
  font-size: 14px;
  color: #999;
  margin-top: 5px;
}

.diary-section {
  background: #fff;
  border-radius: 8px;
  padding: 30px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 1px solid #f0f0f0;
}

.section-header h3 {
  font-size: 18px;
  color: #333;
}

.diary-list {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.diary-item {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  padding: 15px;
  border-radius: 8px;
  border: 1px solid #f0f0f0;
  transition: all 0.3s;
}

.diary-item:hover {
  border-color: #667eea;
  box-shadow: 0 2px 8px rgba(102, 126, 234, 0.1);
}

.diary-info {
  flex: 1;
  cursor: pointer;
}

.diary-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}

.diary-title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.diary-content {
  font-size: 14px;
  color: #666;
  line-height: 1.6;
  margin-bottom: 10px;
}

.diary-footer {
  font-size: 12px;
  color: #999;
}

.diary-update {
  color: #999;
}

.diary-actions {
  display: flex;
  gap: 5px;
  margin-left: 15px;
}

.loading-container {
  text-align: center;
  padding: 60px 0;
  color: #999;
}

.pagination-container {
  display: flex;
  justify-content: center;
  margin-top: 30px;
}

@media (max-width: 768px) {
  .user-card {
    flex-direction: column;
    align-items: flex-start;
    gap: 20px;
  }
  
  .user-stats {
    width: 100%;
    justify-content: space-around;
  }
  
  .diary-item {
    flex-direction: column;
  }
  
  .diary-actions {
    margin-left: 0;
    margin-top: 10px;
    width: 100%;
    justify-content: flex-end;
  }
  
  .nickname {
    display: none;
  }
}
</style>
