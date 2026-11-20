import type { EntityId, UpdateUserProfileRequest, UserProfile } from '@/types/api'
import { post } from '@/axios'

// 接口前缀
const API_PREFIX = '/user/user'

/**
 * 获取用户个人资料
 */
export function getUserProfile(userId?: EntityId | null) {
  return post<UserProfile>(`${API_PREFIX}/profile`, { userId: userId === 'undefined' ? null : userId })
}

/**
 * 更新用户资料
 */
export function updateUserProfile(data: UpdateUserProfileRequest) {
  // 创建 FormData 对象用于文件上传
  const formData = new FormData()
  
  // 如果有头像文件，添加到表单
  if (data.avatar && data.avatar instanceof File) {
    formData.append('avatar', data.avatar)
  }
  
  // 添加其他字段
  if (data.nickname) formData.append('nickName', data.nickname)
  if (data.xiaohashuId) formData.append('xiaohashuId', data.xiaohashuId)
  if (data.sex !== undefined && data.sex !== null) formData.append('sex', String(data.sex))
  if (data.birthday) formData.append('birthday', data.birthday)
  if (data.introduction !== undefined && data.introduction !== null) formData.append('introduction', data.introduction)
  
  return post(`${API_PREFIX}/update`, formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}


