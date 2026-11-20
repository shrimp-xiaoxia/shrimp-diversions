<template>
  <router-view></router-view>
  
  <!-- 登录模态框 -->
  <Teleport to="body">
    <LoginModal v-model:visible="showLoginModal" />
  </Teleport>
</template>

<script setup lang="ts">
import { ref, provide, onMounted, onUnmounted } from 'vue'
import LoginModal from '@/components/auth/LoginModal.vue'
import { useChannelStore } from '@/stores/channel'

const showLoginModal = ref(false)
provide('showLoginModal', showLoginModal)

const channelStore = useChannelStore()
const requestLogin = () => { showLoginModal.value = true }

onMounted(() => {
  window.addEventListener('shrimp:login-required', requestLogin)
  // 加载频道数据
  channelStore.loadChannels()
})
onUnmounted(() => window.removeEventListener('shrimp:login-required', requestLogin))
</script>

<style scoped>
</style>
