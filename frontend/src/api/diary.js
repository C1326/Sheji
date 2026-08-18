import request from '@/utils/request'

// 新增日记
export function addDiary(data) {
  return request({
    url: '/diary/add',
    method: 'post',
    data
  })
}

// 修改日记
export function updateDiary(data) {
  return request({
    url: '/diary/update',
    method: 'put',
    data
  })
}

// 删除日记
export function deleteDiary(diaryId) {
  return request({
    url: `/diary/delete/${diaryId}`,
    method: 'delete'
  })
}

// 获取我的日记列表
export function getMyDiaryList(params) {
  return request({
    url: '/diary/my/list',
    method: 'get',
    params
  })
}

// 获取公开日记列表
export function getPublicDiaryList(params) {
  return request({
    url: '/diary/public/list',
    method: 'get',
    params
  })
}

// 获取日记详情
export function getDiaryDetail(diaryId) {
  return request({
    url: `/diary/${diaryId}`,
    method: 'get'
  })
}
