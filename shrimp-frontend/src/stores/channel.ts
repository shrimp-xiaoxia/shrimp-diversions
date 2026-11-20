import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getAllChannel } from '@/api/channel'
import type { EntityId, NamedItem } from '@/types/api'

export const useChannelStore = defineStore('channel', () => {
  // 存储所有频道数据
  const channels = ref<NamedItem[]>([])
  const loading = ref(false)
  const loaded = ref(false)
  const error = ref('')
  let pending: Promise<void> | null = null
  
  // 加载所有频道数据
  const loadChannels = (): Promise<void> => {
    if (pending) return pending
    if (loaded.value) return Promise.resolve()
    loading.value = true
    error.value = ''
    pending = (async () => {
      try {
        const res = await getAllChannel()
        if (!res.success) {
          error.value = res.message || '频道加载失败，请重试'
          return
        }
        channels.value = res.data ?? []
        loaded.value = true
      } catch (cause) {
        console.error('加载频道数据失败:')
        error.value = '频道加载失败，请重试'
      } finally {
        loading.value = false
      }
    })().finally(() => { pending = null })
    return pending
  }
  
  // 根据ID获取频道名称
  const getChannelNameById = (id: EntityId) => {
    const channel = channels.value.find(item => item.id === id)
    return channel ? channel.name : ''
  }
  
  return {
    channels,
    loading,
    loaded,
    error,
    loadChannels,
    getChannelNameById
  }
}) 
