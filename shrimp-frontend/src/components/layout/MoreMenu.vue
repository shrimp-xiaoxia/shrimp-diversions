<template>
  <Transition name="dropdown">
    <div 
      v-if="visible" 
      class="absolute bottom-[55px] left-4 bg-white rounded-xl w-[18rem] shadow-lg 
      overflow-hidden border border-gray-100"
      style="z-index: 100;"
    >
      <!-- 主要菜单项 -->
      <div class="p-1.5">
        <div class="px-3 py-2 hover:bg-gray-100 text-gray-600 hover:text-gray-800 rounded-lg cursor-not-allowed flex items-center justify-between">
          <span>关于虾趣社区</span>
          <svg class="w-4 h-4 text-gray-400" viewBox="0 0 24 24" fill="none" stroke="currentColor">
            <path d="M9 5l7 7-7 7" stroke-width="2" stroke-linecap="round"/>
          </svg>
        </div>
        <div class="px-3 py-2 hover:bg-gray-100 text-gray-600 hover:text-gray-800 rounded-lg cursor-not-allowed flex items-center justify-between">
          <span>隐私、协议</span>
          <svg class="w-4 h-4 text-gray-400" viewBox="0 0 24 24" fill="none" stroke="currentColor">
            <path d="M9 5l7 7-7 7" stroke-width="2" stroke-linecap="round"/>
          </svg>
        </div>
        <div class="px-3 py-2 hover:bg-gray-100 text-gray-600 hover:text-gray-800 rounded-lg cursor-not-allowed">
          帮助与客服
        </div>
      </div>

      <!-- 分隔线 -->
      <div class="h-[1px] bg-gray-100"></div>

      <!-- 次要菜单项 -->
      <div class="p-1.5">
        <div class="px-3 py-2 text-gray-400 text-xs">
          访问方式
        </div>
        <div class="px-3 py-2 hover:bg-gray-100 text-gray-600 hover:text-gray-800 rounded-lg cursor-not-allowed">
          键盘快捷键
        </div>
        <div class="px-3 py-2 hover:bg-gray-100 text-gray-600 hover:text-gray-800 rounded-lg cursor-not-allowed">
          添加虾趣社区到桌面
        </div>
        <div class="px-3 py-2 hover:bg-gray-100 text-gray-600 hover:text-gray-800 rounded-lg cursor-not-allowed">
          打开小窗模式
        </div>
      </div>

      <!-- 分隔线 -->
      <div v-if="isLoggedIn" class="h-[1px] bg-gray-100"></div>

      <!-- 设置相关 -->
      <div v-if="isLoggedIn" class="p-1.5">
        <div class="px-3 py-2 text-gray-400 text-xs">
          设置
        </div>
        <!-- <div 
          class="px-3 py-2 hover:bg-gray-50 text-gray-600 hover:text-gray-800 rounded-lg cursor-pointer"
        >
          修改密码
        </div> -->
        <button type="button"
          class="w-full px-3 py-2 text-left hover:bg-gray-50 text-gray-600 hover:text-gray-800 rounded-lg cursor-pointer disabled:opacity-50"
          :disabled="isLoggingOut"
          @click="handleLogout"
        >
          {{ isLoggingOut ? '退出中…' : '退出登录' }}
        </button>
      </div>
    </div>
  </Transition>

  <!-- 点击外部关闭的遮罩层 -->
  <div v-if="visible" class="fixed inset-0" style="z-index: 99;" @click="onClose"></div>
</template>

<script setup lang="ts">
import { useUserStore } from '@/stores/user'
import { logout } from '@/api/auth'
import { message } from '@/utils/message'
import { useRouter } from 'vue-router'
import { computed, ref } from 'vue'

const router = useRouter()
const userStore = useUserStore()

// 使用 store 中的计算属性判断登录状态
const isLoggedIn = computed(() => !!userStore.token)
const isLoggingOut = ref(false)

defineProps({
  visible: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:visible'])

const onClose = () => {
  emit('update:visible', false)
}

const handleLogout = async () => {
  if (isLoggingOut.value) return
  isLoggingOut.value = true
  const token = userStore.token
  try {
    const res = await logout()
    if (userStore.token !== token) return
    if (!res.success) {
      message.show(res.message || '退出失败，请重试')
      return
    }
    userStore.logout()
    message.show('退出登录成功')
    onClose()
    await router.push('/')
  } catch (error) {
    console.error('退出登录失败:')
  } finally {
    isLoggingOut.value = false
  }
}
</script>

<style scoped>
.dropdown-enter-active,
.dropdown-leave-active {
  transition: all 0.15s ease-out;
  transform-origin: top center;
}

.dropdown-enter-from,
.dropdown-leave-to {
  opacity: 0;
  transform: translateY(8px);
}
</style>
