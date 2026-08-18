<template>
  <div class="home-container">
    <!-- 顶部导航 -->
    <header class="header">
      <div class="header-content">
        <div class="logo" @click="goHome">
          <el-icon :size="28"><Notebook /></el-icon>
          <span>佘记</span>
        </div>
        
        <div class="nav-actions">
          <el-button v-if="!userStore.isLoggedIn" type="primary" @click="goLogin">
            登录
          </el-button>
          <template v-else>
            <el-button @click="goWrite">
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
                  <el-dropdown-item command="personal">
                    <el-icon><User /></el-icon>
                    个人中心
                  </el-dropdown-item>
                  <el-dropdown-item command="logout" divided>
                    <el-icon><SwitchButton /></el-icon>
                    退出登录
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
        </div>
      </div>
    </header>

    <!-- 主要内容 -->
    <main class="main-content">
      <div class="container">
        <div class="page-title">
          <h2>公开日记广场</h2>
          <p>发现精彩日记，记录美好生活</p>
        </div>

        <!-- 日记列表 -->
        <div v-if="diaryList.length > 0" class="diary-grid">
          <div
            v-for="diary in diaryList"
            :key="diary.diaryId || diary.id"
            class="diary-card card-hover"
            @click="goDetail(diary.diaryId || diary.id)"
          >
            <div class="card-image">
              <img
                v-if="diary.coverImage || getFirstImage(diary.mediaList)"
                :src="diary.coverImage || getFirstImage(diary.mediaList)"
                alt="封面"
              />
              <div v-else class="no-image">
                <el-icon :size="48"><Picture /></el-icon>
              </div>
            </div>
            <div class="card-content">
              <h3 class="card-title">{{ diary.title }}</h3>
              <p class="card-desc">{{ stripHtml(diary.content) }}</p>
              <div class="card-footer">
                <div class="author">
                  <el-avatar :size="24" :src="resolveUrl(diary.avatar)">
                    {{ diary.nickname?.charAt(0) || '佚' }}
                  </el-avatar>
                  <span>{{ diary.nickname || '佚名' }}</span>
                </div>
                <span class="time">{{ formatDate(diary.createTime) }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 空状态 -->
        <el-empty v-else-if="!loading" description="暂无公开日记">
          <el-button type="primary" @click="userStore.isLoggedIn ? goWrite() : goLogin()">
            {{ userStore.isLoggedIn ? '写第一篇日记' : '去登录发布日记' }}
          </el-button>
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
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { getPublicDiaryList } from '@/api/diary'
import { resolveUrl, mediaViewUrl } from '@/utils/url'

const router = useRouter()
const userStore = useUserStore()

const diaryList = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(12)
const total = ref(0)

// 获取公开日记列表
const fetchDiaryList = async () => {
  loading.value = true
  try {
    const data = await getPublicDiaryList({
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

// 获取第一张图片（走带权限校验的 view 接口）
const getFirstImage = (mediaList) => {
  if (!mediaList || mediaList.length === 0) return null
  const image = mediaList.find(m => m.mediaType === 1)
  return image ? mediaViewUrl(image.mediaId) : null
}

// 去除HTML标签
const stripHtml = (html) => {
  if (!html) return ''
  const text = html.replace(/<[^>]+>/g, '').replace(/&nbsp;/g, ' ')
  return text.length > 100 ? text.substring(0, 100) + '...' : text
}

// 格式化日期
const formatDate = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  const now = new Date()
  const diff = now - date
  const days = Math.floor(diff / (1000 * 60 * 60 * 24))
  
  if (days === 0) {
    const hours = Math.floor(diff / (1000 * 60 * 60))
    if (hours === 0) {
      const minutes = Math.floor(diff / (1000 * 60))
      return minutes <= 1 ? '刚刚' : `${minutes}分钟前`
    }
    return `${hours}小时前`
  } else if (days === 1) {
    return '昨天'
  } else if (days < 7) {
    return `${days}天前`
  } else {
    return dateStr.substring(0, 10)
  }
}

// 页码改变
const handlePageChange = (page) => {
  currentPage.value = page
  fetchDiaryList()
}

// 下拉菜单命令
const handleCommand = (command) => {
  if (command === 'personal') {
    router.push('/personal')
  } else if (command === 'logout') {
    userStore.logout()
    ElMessage.success('已退出登录')
    router.push('/home')
  }
}

// 跳转方法
const goHome = () => router.push('/home')
const goLogin = () => router.push('/login')
const goWrite = () => router.push('/diary/write')
const goDetail = (id) => router.push(`/diary/detail/${id}`)

onMounted(() => {
  fetchDiaryList()
})
</script>

<style scoped>
.home-container {
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
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
}

.page-title {
  text-align: center;
  margin-bottom: 30px;
}

.page-title h2 {
  font-size: 28px;
  color: #333;
  margin-bottom: 10px;
}

.page-title p {
  color: #999;
  font-size: 14px;
}

.diary-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
}

.diary-card {
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.card-image {
  width: 100%;
  height: 180px;
  overflow: hidden;
  background-color: #f5f7fa;
}

.card-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s;
}

.diary-card:hover .card-image img {
  transform: scale(1.05);
}

.no-image {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #ccc;
}

.card-content {
  padding: 15px;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin-bottom: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-desc {
  font-size: 14px;
  color: #666;
  line-height: 1.5;
  height: 42px;
  overflow: hidden;
  margin-bottom: 12px;
}

.card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 10px;
  border-top: 1px solid #f0f0f0;
}

.author {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #666;
}

.time {
  font-size: 12px;
  color: #999;
}

.loading-container {
  text-align: center;
  padding: 60px 0;
  color: #999;
}

.pagination-container {
  display: flex;
  justify-content: center;
  margin-top: 40px;
}

@media (max-width: 768px) {
  .diary-grid {
    grid-template-columns: 1fr;
  }
  
  .header-content {
    padding: 10px 15px;
  }
  
  .nickname {
    display: none;
  }
}
</style>
