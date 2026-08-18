<template>
  <div class="write-container">
    <!-- 顶部导航 -->
    <header class="header">
      <div class="header-content">
        <div class="logo" @click="goHome">
          <el-icon :size="28"><Notebook /></el-icon>
          <span>佘记</span>
        </div>
        
        <div class="nav-actions">
          <el-button @click="goHome">返回首页</el-button>
          <el-button type="primary" :loading="submitting" :disabled="uploading" @click="handleSubmit">
            {{ uploading ? '文件上传中...' : (isEdit ? '保存修改' : '发布日记') }}
          </el-button>
        </div>
      </div>
    </header>

    <!-- 主要内容 -->
    <main class="main-content">
      <div class="container">
        <div class="write-form">
          <!-- 标题 -->
          <el-input
            v-model="diaryForm.title"
            class="title-input"
            placeholder="请输入日记标题"
            maxlength="100"
            show-word-limit
          />

          <!-- 富文本编辑器 -->
          <div class="editor-container">
            <Toolbar
              class="toolbar"
              :editor="editorRef"
              :defaultConfig="toolbarConfig"
              mode="default"
            />
            <Editor
              v-model="diaryForm.content"
              class="editor"
              :defaultConfig="editorConfig"
              mode="default"
              @onCreated="handleEditorCreated"
            />
          </div>

          <!-- 媒体上传 -->
          <div class="media-section">
            <div class="section-title">
              <el-icon><Picture /></el-icon>
              <span>上传图片</span>
            </div>
            <el-upload
              v-model:file-list="imageList"
              action="#"
              list-type="picture-card"
              accept="image/jpeg,image/jpg,image/png,image/gif,image/webp"
              :auto-upload="false"
              :on-change="handleImageChange"
              :on-remove="handleImageRemove"
              :before-upload="beforeImageUpload"
            >
              <el-icon><Plus /></el-icon>
            </el-upload>
            <p class="upload-tip">支持 jpg、png、gif、webp 格式，单张不超过 5MB</p>
          </div>

          <div class="media-section">
            <div class="section-title">
              <el-icon><VideoCamera /></el-icon>
              <span>上传视频</span>
            </div>
            <el-upload
              v-model:file-list="videoList"
              action="#"
              accept="video/mp4"
              :auto-upload="false"
              :limit="1"
              :on-change="handleVideoChange"
              :on-remove="handleVideoRemove"
              :before-upload="beforeVideoUpload"
            >
              <el-button type="primary">
                <el-icon><Upload /></el-icon>
                选择视频
              </el-button>
              <template #tip>
                <p class="upload-tip">支持 mp4 格式，不超过 50MB</p>
              </template>
            </el-upload>
          </div>

          <!-- 权限设置 -->
          <div class="permission-section">
            <div class="section-title">
              <el-icon><Lock /></el-icon>
              <span>权限设置</span>
            </div>
            <el-radio-group v-model="diaryForm.permission">
              <el-radio :value="0">
                <div class="radio-label">
                  <span>私有</span>
                  <span class="radio-desc">仅自己可见</span>
                </div>
              </el-radio>
              <el-radio :value="1">
                <div class="radio-label">
                  <span>公开</span>
                  <span class="radio-desc">所有人可见</span>
                </div>
              </el-radio>
            </el-radio-group>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount, shallowRef } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Editor, Toolbar } from '@wangeditor/editor-for-vue'
import '@wangeditor/editor/dist/css/style.css'
import DOMPurify from 'dompurify'
import { addDiary, updateDiary, getDiaryDetail } from '@/api/diary'
import { uploadMedia } from '@/api/media'
import { resolveUrl, mediaViewUrl } from '@/utils/url'

const router = useRouter()
const route = useRoute()

const editorRef = shallowRef()
const submitting = ref(false)
const isEdit = ref(false)
const diaryId = ref(null)
const uploadCount = ref(0)

// 是否正在上传文件
const uploading = computed(() => uploadCount.value > 0)

const diaryForm = reactive({
  title: '',
  content: '',
  permission: 0
})

const imageList = ref([])
const videoList = ref([])
// 已上传文件的相对路径列表（/uploads/xxx），提交日记时放入 mediaUrls
const uploadedMediaUrls = ref([])

// 上传计数管理（防止上传未完成时提交导致 mediaUrls 缺失）
const beginUpload = () => {
  uploadCount.value++
}
const finishUpload = () => {
  uploadCount.value = Math.max(0, uploadCount.value - 1)
}

// 编辑器配置
const toolbarConfig = {}
const editorConfig = {
  placeholder: '开始写日记吧...',
  MENU_CONF: {
    uploadImage: {
      async customUpload(file, insertFn) {
        beginUpload()
        try {
          const data = await uploadMedia(file)
          const url = resolveUrl(data.filePath)
          // wangeditor 的插入函数：insertFn(url, alt, href)
          insertFn(url, data.fileName, url)
          uploadedMediaUrls.value.push(data.filePath)
        } catch (error) {
          ElMessage.error('图片上传失败')
        } finally {
          finishUpload()
        }
      }
    },
    uploadVideo: {
      async customUpload(file, insertFn) {
        beginUpload()
        try {
          const data = await uploadMedia(file)
          const url = resolveUrl(data.filePath)
          insertFn(url, data.fileName, url)
          uploadedMediaUrls.value.push(data.filePath)
        } catch (error) {
          ElMessage.error('视频上传失败')
        } finally {
          finishUpload()
        }
      }
    }
  }
}

// 编辑器创建完成
const handleEditorCreated = (editor) => {
  editorRef.value = editor
}

// 图片上传前校验
const beforeImageUpload = (file) => {
  const isImage = ['image/jpeg', 'image/jpg', 'image/png', 'image/gif', 'image/webp'].includes(file.type)
  const isLt5M = file.size / 1024 / 1024 < 5

  if (!isImage) {
    ElMessage.error('只能上传 JPG/PNG/GIF/WEBP 格式的图片')
    return false
  }
  if (!isLt5M) {
    ElMessage.error('图片大小不能超过 5MB')
    return false
  }
  return true
}

// 图片选择变化：自动上传到服务器
const handleImageChange = async (file, fileList) => {
  if (file.status === 'ready') {
    beginUpload()
    try {
      const data = await uploadMedia(file.raw)
      file.url = resolveUrl(data.filePath) // 本地预览
      file.filePath = data.filePath
      uploadedMediaUrls.value.push(data.filePath)
    } catch (error) {
      ElMessage.error('图片上传失败')
      const index = fileList.indexOf(file)
      if (index > -1) {
        fileList.splice(index, 1)
      }
    } finally {
      finishUpload()
    }
  }
}

// 图片移除
const handleImageRemove = (file) => {
  if (file.filePath) {
    const index = uploadedMediaUrls.value.indexOf(file.filePath)
    if (index > -1) {
      uploadedMediaUrls.value.splice(index, 1)
    }
  }
}

// 视频上传前校验
const beforeVideoUpload = (file) => {
  const isVideo = file.type === 'video/mp4'
  const isLt50M = file.size / 1024 / 1024 < 50

  if (!isVideo) {
    ElMessage.error('只能上传 MP4 格式的视频')
    return false
  }
  if (!isLt50M) {
    ElMessage.error('视频大小不能超过 50MB')
    return false
  }
  return true
}

// 视频选择变化：自动上传到服务器
const handleVideoChange = async (file, fileList) => {
  if (file.status === 'ready') {
    beginUpload()
    try {
      const data = await uploadMedia(file.raw)
      file.url = resolveUrl(data.filePath)
      file.filePath = data.filePath
      uploadedMediaUrls.value.push(data.filePath)
    } catch (error) {
      ElMessage.error('视频上传失败')
      const index = fileList.indexOf(file)
      if (index > -1) {
        fileList.splice(index, 1)
      }
    } finally {
      finishUpload()
    }
  }
}

// 视频移除
const handleVideoRemove = (file) => {
  if (file.filePath) {
    const index = uploadedMediaUrls.value.indexOf(file.filePath)
    if (index > -1) {
      uploadedMediaUrls.value.splice(index, 1)
    }
  }
}

// 提交日记
const handleSubmit = async () => {
  if (!diaryForm.title.trim()) {
    ElMessage.warning('请输入日记标题')
    return
  }
  if (!diaryForm.content.trim()) {
    ElMessage.warning('请输入日记内容')
    return
  }

  if (uploading.value) {
    ElMessage.warning('图片/视频正在上传，请稍候再提交')
    return
  }

  submitting.value = true
  try {
    const payload = {
      title: diaryForm.title,
      // 提交前再次消毒，防止富文本注入脚本
      content: DOMPurify.sanitize(diaryForm.content),
      permission: diaryForm.permission,
      mediaUrls: uploadedMediaUrls.value
    }

    if (isEdit.value) {
      payload.diaryId = diaryId.value
      await updateDiary(payload)
      ElMessage.success('修改成功')
    } else {
      await addDiary(payload)
      ElMessage.success('发布成功')
    }

    router.push('/personal')
  } catch (error) {
    console.error('提交失败:', error)
  } finally {
    submitting.value = false
  }
}

// 加载日记详情（编辑模式）
const loadDiaryDetail = async () => {
  try {
    const data = await getDiaryDetail(diaryId.value)
    diaryForm.title = data.title
    diaryForm.content = data.content
    diaryForm.permission = data.permission
    uploadedMediaUrls.value = []

    // 加载媒体文件（预览走带权限校验的 view 接口）
    if (data.mediaList && data.mediaList.length > 0) {
      data.mediaList.forEach(media => {
        uploadedMediaUrls.value.push(media.url)
        const previewUrl = mediaViewUrl(media.mediaId)
        if (media.mediaType === 1) {
          imageList.value.push({
            name: media.fileName,
            url: previewUrl,
            filePath: media.url
          })
        } else if (media.mediaType === 2) {
          videoList.value.push({
            name: media.fileName,
            url: previewUrl,
            filePath: media.url
          })
        }
      })
    }
  } catch (error) {
    ElMessage.error('加载日记失败')
    router.push('/personal')
  }
}

// 跳转方法
const goHome = () => router.push('/home')

onMounted(() => {
  // 判断是否为编辑模式
  if (route.params.id) {
    isEdit.value = true
    diaryId.value = route.params.id
    loadDiaryDetail()
  }
})

onBeforeUnmount(() => {
  const editor = editorRef.value
  if (editor) {
    editor.destroy()
  }
})
</script>

<style scoped>
.write-container {
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

.write-form {
  background: #fff;
  border-radius: 8px;
  padding: 30px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.title-input {
  margin-bottom: 20px;
}

.title-input :deep(.el-input__inner) {
  font-size: 24px;
  font-weight: 600;
  border: none;
  padding: 10px 0;
  border-bottom: 1px solid #e0e0e0;
  border-radius: 0;
}

.title-input :deep(.el-input__inner:focus) {
  border-bottom-color: #667eea;
}

.editor-container {
  border: 1px solid #e0e0e0;
  border-radius: 4px;
  margin-bottom: 30px;
}

.toolbar {
  border-bottom: 1px solid #e0e0e0;
}

.editor {
  height: 400px;
  overflow-y: auto;
}

.media-section {
  margin-bottom: 30px;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin-bottom: 15px;
}

.upload-tip {
  font-size: 12px;
  color: #999;
  margin-top: 8px;
}

.permission-section {
  padding-top: 20px;
  border-top: 1px solid #f0f0f0;
}

.radio-label {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
}

.radio-desc {
  font-size: 12px;
  color: #999;
  margin-top: 2px;
}

:deep(.el-radio-group) {
  display: flex;
  gap: 30px;
}

:deep(.el-upload--picture-card) {
  width: 120px;
  height: 120px;
}

:deep(.el-upload-list--picture-card .el-upload-list__item) {
  width: 120px;
  height: 120px;
}
</style>
