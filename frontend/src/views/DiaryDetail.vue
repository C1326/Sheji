<template>
  <div class="detail-container">
    <!-- 顶部导航 -->
    <header class="header">
      <div class="header-content">
        <div class="logo" @click="goHome">
          <el-icon :size="28"><Notebook /></el-icon>
          <span>佘记</span>
        </div>

        <div class="nav-actions">
          <el-button @click="goBack">
            <el-icon><Back /></el-icon>
            返回
          </el-button>
          <el-button v-if="userStore.isLoggedIn" @click="goWrite">
            <el-icon><Edit /></el-icon>
            写日记
          </el-button>
        </div>
      </div>
    </header>

    <!-- 主要内容 -->
    <main class="main-content">
      <div class="container">
        <!-- 加载状态 -->
        <div v-if="loading" class="loading-container">
          <el-icon class="is-loading" :size="40"><Loading /></el-icon>
          <p>加载中...</p>
        </div>

        <!-- 日记内容 -->
        <article v-else-if="diary" class="diary-article">
          <h1 class="diary-title">{{ diary.title }}</h1>

          <div class="diary-meta">
            <div class="author">
              <el-avatar :size="40" :src="resolveUrl(diary.avatar)">
                {{ (diary.nickname || '佚').charAt(0) }}
              </el-avatar>
              <div class="author-info">
                <span class="nickname">{{ diary.nickname || '佚名' }}</span>
                <span class="time">{{ formatDate(diary.createTime) }}</span>
              </div>
            </div>
            <el-tag :type="diary.permission === 1 ? 'success' : 'info'" size="small">
              {{ diary.permission === 1 ? '公开' : '私有' }}
            </el-tag>
          </div>

          <!-- 封面图 -->
          <div v-if="diary.coverImage || getFirstImage(diary.mediaList)" class="cover-image">
            <img
              :src="diary.coverImage || getFirstImage(diary.mediaList)"
              alt="封面"
            />
          </div>

          <!-- 内容 -->
          <div v-if="diary.content" class="diary-content" v-html="sanitizedContent"></div>

          <!-- 媒体列表 -->
          <div v-if="hasMedia" class="media-list">
            <div
              v-for="media in diary.mediaList"
              :key="media.mediaId"
              class="media-item"
            >
              <img
                v-if="media.mediaType === 1"
                :src="mediaViewUrl(media.mediaId)"
                :alt="media.fileName"
                @click="previewImage(mediaViewUrl(media.mediaId))"
              />
              <video
                v-else-if="media.mediaType === 2"
                :src="mediaViewUrl(media.mediaId)"
                controls
                preload="metadata"
              ></video>
              <div class="media-footer">
                <div class="media-info">
                  <span class="media-name" :title="media.fileName">{{ media.fileName }}</span>
                  <span class="media-size">{{ formatFileSize(media.fileSize) }}</span>
                </div>
                <el-button
                  v-if="canDownload"
                  size="small"
                  type="primary"
                  link
                  :loading="downloadingId === media.mediaId"
                  @click="handleDownload(media)"
                >
                  <el-icon><Download /></el-icon>
                  下载
                </el-button>
              </div>
            </div>
          </div>

          <!-- 操作按钮（作者可见） -->
          <div v-if="isOwner" class="diary-actions">
            <el-button type="primary" @click="goEdit">
              <el-icon><Edit /></el-icon>
              编辑
            </el-button>
            <el-button type="danger" @click="handleDelete">
              <el-icon><Delete /></el-icon>
              删除
            </el-button>
          </div>
        </article>

        <!-- 空状态 -->
        <el-empty v-else-if="!loading" description="日记不存在或已被删除">
          <el-button type="primary" @click="goHome">返回首页</el-button>
        </el-empty>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import DOMPurify from 'dompurify'
import { useUserStore } from '@/stores/user'
import { getDiaryDetail, deleteDiary } from '@/api/diary'
import { downloadMedia } from '@/api/media'
import { resolveUrl, mediaViewUrl } from '@/utils/url'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const diary = ref(null)
const loading = ref(false)
const downloadingId = ref(null)

// 渲染前对富文本内容消毒，防止存储型 XSS
const sanitizedContent = computed(() => {
  return diary.value && diary.value.content ? DOMPurify.sanitize(diary.value.content) : ''
})

// 是否包含媒体文件
const hasMedia = computed(() => {
  return diary.value && diary.value.mediaList && diary.value.mediaList.length > 0
})

// 是否为作者本人
const isOwner = computed(() => {
  if (!diary.value || !userStore.isLoggedIn) return false
  const diaryUserId = diary.value.userId || diary.value.userIdValue
  const currentUserId = userStore.userId || (userStore.userInfo && userStore.userInfo.userId)
  return currentUserId && currentUserId === diaryUserId
})

// 下载权限：登录用户 +（公开日记 或 本人日记）
const canDownload = computed(() => {
  if (!userStore.isLoggedIn || !diary.value) return false
  return diary.value.permission === 1 || isOwner.value
})

// 获取第一张图片（走带权限校验的 view 接口）
const getFirstImage = (mediaList) => {
  if (!mediaList || mediaList.length === 0) return null
  const image = mediaList.find(m => m.mediaType === 1)
  return image ? mediaViewUrl(image.mediaId) : null
}

// 格式化文件大小
const formatFileSize = (size) => {
  if (size === null || size === undefined || size === '') return ''
  const num = Number(size)
  if (isNaN(num) || num <= 0) return ''
  if (num < 1024) return `${num} B`
  if (num < 1024 * 1024) return `${(num / 1024).toFixed(1)} KB`
  if (num < 1024 * 1024 * 1024) return `${(num / 1024 / 1024).toFixed(2)} MB`
  return `${(num / 1024 / 1024 / 1024).toFixed(2)} GB`
}

// 格式化日期
const formatDate = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hour = String(date.getHours()).padStart(2, '0')
  const minute = String(date.getMinutes()).padStart(2, '0')
  return `${year}-${month}-${day} ${hour}:${minute}`
}

// 预览图片
const previewImage = (url) => {
  window.open(url, '_blank')
}

// 下载媒体资源（携带 token，支持私有日记本人下载）
const handleDownload = async (media) => {
  if (downloadingId.value) return
  downloadingId.value = media.mediaId
  try {
    const blob = await downloadMedia(media.mediaId)
    const url = window.URL.createObjectURL(new Blob([blob]))
    const link = document.createElement('a')
    link.href = url
    link.download = media.fileName || `media-${media.mediaId}`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
    ElMessage.success('下载成功')
  } catch (error) {
    console.error('下载失败:', error)
    ElMessage.error('下载失败，请稍后重试')
  } finally {
    downloadingId.value = null
  }
}

// 加载日记详情
const loadDiaryDetail = async () => {
  const id = route.params.id
  if (!id) return

  loading.value = true
  try {
    const data = await getDiaryDetail(id)
    diary.value = data
    document.title = `${data.title} - 佘记`
  } catch (error) {
    console.error('获取日记详情失败:', error)
  } finally {
    loading.value = false
  }
}

// 删除日记
const handleDelete = async () => {
  try {
    await ElMessageBox.confirm('确定要删除这篇日记吗？删除后无法恢复', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await deleteDiary(diary.value.diaryId || diary.value.id)
    ElMessage.success('删除成功')
    router.push('/personal')
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
    }
  }
}

// 跳转方法
const goHome = () => router.push('/home')
const goBack = () => router.back()
const goWrite = () => router.push('/diary/write')
const goEdit = () => router.push(`/diary/edit/${diary.value.diaryId || diary.value.id}`)

onMounted(() => {
  loadDiaryDetail()
})
</script>

<style scoped>
.detail-container {
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
  max-width: 900px;
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
  gap: 10px;
}

.main-content {
  padding: 30px 0;
}

.container {
  max-width: 900px;
  margin: 0 auto;
  padding: 0 20px;
}

.loading-container {
  text-align: center;
  padding: 80px 0;
  color: #999;
}

.diary-article {
  background: #fff;
  border-radius: 8px;
  padding: 40px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.diary-title {
  font-size: 28px;
  color: #333;
  margin-bottom: 20px;
  line-height: 1.4;
}

.diary-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 20px;
  margin-bottom: 25px;
  border-bottom: 1px solid #f0f0f0;
}

.author {
  display: flex;
  align-items: center;
  gap: 12px;
}

.author-info {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.nickname {
  font-size: 15px;
  font-weight: 500;
  color: #333;
}

.time {
  font-size: 12px;
  color: #999;
}

.cover-image {
  width: 100%;
  max-height: 400px;
  overflow: hidden;
  border-radius: 8px;
  margin-bottom: 25px;
}

.cover-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.diary-content {
  font-size: 16px;
  color: #333;
  line-height: 1.8;
  word-break: break-word;
}

.diary-content :deep(img) {
  max-width: 100%;
  height: auto;
  border-radius: 4px;
  margin: 10px 0;
}

.diary-content :deep(video) {
  max-width: 100%;
  border-radius: 4px;
  margin: 10px 0;
}

.media-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 15px;
  margin-top: 30px;
}

.media-item {
  border-radius: 8px;
  overflow: hidden;
  background-color: #f5f7fa;
}

.media-item img {
  width: 100%;
  height: 180px;
  object-fit: cover;
  cursor: zoom-in;
  transition: transform 0.3s;
  display: block;
}

.media-item img:hover {
  transform: scale(1.02);
}

.media-item video {
  width: 100%;
  max-height: 300px;
  display: block;
}

.media-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 8px 12px;
  background: #fff;
}

.media-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.media-name {
  font-size: 13px;
  color: #333;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 160px;
}

.media-size {
  font-size: 12px;
  color: #999;
}

.diary-actions {
  display: flex;
  gap: 10px;
  margin-top: 30px;
  padding-top: 20px;
  border-top: 1px solid #f0f0f0;
}

@media (max-width: 768px) {
  .diary-article {
    padding: 20px;
  }

  .diary-title {
    font-size: 22px;
  }

  .header-content {
    padding: 10px 15px;
  }
}
</style>
