// 后端返回的相对路径（如 /uploads/xxx.jpg）在 dev 下需要走 /api 代理才能访问
export function resolveUrl(path) {
  if (!path) return ''
  if (/^https?:\/\//i.test(path) || path.startsWith('data:')) return path
  if (path.startsWith('/api/')) return path
  return `/api${path.startsWith('/') ? path : '/' + path}`
}

// 媒体展示统一走带权限校验的 view 接口（公开可看、私有仅本人）
export function mediaViewUrl(mediaId) {
  if (mediaId === null || mediaId === undefined) return ''
  return `/api/media/view/${mediaId}`
}
