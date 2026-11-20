import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { UserProfile } from '@/types/api'

export const useUserStore = defineStore('user', () => {
  const token = ref('')
  // 主页用户信息
  const profile = ref<UserProfile>({})

  const setProfile = (newProfile: UserProfile) => {
    profile.value = newProfile
  }

  const setToken = (newToken: string) => {
    token.value = newToken
  }

  // 退出登录
  const logout = () => {
    token.value = ''
    // 删除用户信息
    profile.value = {}
  }


  return {
    token,
    profile,
    setProfile,
    setToken,
    logout,
  }
}, 
{
  // 开启持久化
  persist: true,
})