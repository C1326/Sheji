import request from '@/utils/request'

// 上传媒体文件
export function uploadMedia(file, onUploadProgress) {
  const formData = new FormData()
  formData.append('file', file)
  return request({
    url: '/media/upload',
    method: 'post',
    headers: {
      'Content-Type': 'multipart/form-data'
    },
    data: formData,
    onUploadProgress
  })
}

// 下载媒体文件
export function downloadMedia(mediaId) {
  return request({
    url: `/media/download/${mediaId}`,
    method: 'get',
    responseType: 'blob'
  })
}

// 获取下载URL（用于直接下载）
export function getDownloadUrl(mediaId) {
  return `/api/media/download/${mediaId}`
}
