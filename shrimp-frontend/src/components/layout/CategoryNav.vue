<template>
  <div class="category-nav flex items-center">
    <div class="flex items-center overflow-x-auto py-2 px-4 whitespace-nowrap">
      <!-- 全部频道 -->
      <button type="button"
        class="channel"
        :aria-pressed="activeChannelId === 0"
        :class="{ 
          'active-channel': activeChannelId === 0,
          '': activeChannelId !== 0
        }"
        @click="handleChannelClick(0)"
      >
        全部
      </button>
      
      <!-- 动态获取的频道列表 -->
      <button type="button"
        v-for="channel in channelStore.channels" 
        :key="channel.id"
        class="channel"
        :aria-pressed="activeChannelId === channel.id"
        :class="{ 
          'active-channel': activeChannelId === channel.id,
          '': activeChannelId !== channel.id
        }"
        @click="handleChannelClick(channel.id)"
      >
        {{ channel.name }}
      </button>
      <span v-if="channelStore.loading" role="status" class="px-4 text-sm text-gray-500">频道加载中…</span>
      <span v-else-if="channelStore.error" role="alert" class="px-4 text-sm text-gray-500">
        {{ channelStore.error }}
        <button type="button" class="ml-2 text-red-500" @click="channelStore.loadChannels()">重试</button>
      </span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useChannelStore } from '@/stores/channel'

const props = defineProps({
  activeChannelId: {
    type: [String, Number],
    default: 0
  }
})

const emit = defineEmits(['channel-change'])

const channelStore = useChannelStore()

// 处理频道点击
const handleChannelClick = (channelId: string | number) => {
  // 触发自定义事件，将频道ID传递给父组件
  emit('channel-change', channelId)
}

// 组件挂载时获取频道列表
onMounted(() => {
  void channelStore.loadChannels()
})
</script>

<style scoped>
.category-nav {
  min-width: 0;
  max-width: 100%;
  /* border-bottom: 1px solid rgba(0, 0, 0, 0.05);
  background-color: #fff; */
  height: 72px;
}

.category-nav > div { min-width: 0; max-width: 100%; }

/* 隐藏滚动条但保留功能 */
.overflow-x-auto {
  scrollbar-width: none; /* Firefox */
  -ms-overflow-style: none; /* IE and Edge */
}

.overflow-x-auto::-webkit-scrollbar {
  display: none; /* Chrome, Safari, Opera */
}

.channel {
    height: 40px;
    display: flex;
    justify-content: center;
    align-items: center;
    padding: 0 16px;
    cursor: pointer;
    -webkit-user-select: none;
    user-select: none;
    color: var(--color-secondary-label)
}

.channel:hover {
    background: var(--color-active-background);
    border-radius: 999px;
    color: var(--color-primary-label);
}

.active-channel {
    background: var(--color-active-background);
    border-radius: 999px;
    color: var(--color-primary-label);
    font-weight: 600;
}
</style> 
