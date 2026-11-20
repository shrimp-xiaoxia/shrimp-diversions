<template>
  <div v-if="visible" ref="dialogRoot" class="fixed inset-0 flex items-center justify-center" style="z-index: 9999;" @keydown.esc.stop.prevent="closeTopDialog" @keydown.tab="trapFocus">
    <!-- 遮罩层 -->
    <div class="absolute inset-0 bg-gray-800/25" style="z-index: 9998;" @click="onClose"></div>
    
    <!-- 登录框 -->
    <Transition 
      name="zoom"
      appear
      @before-enter="onBeforeEnter"
      @enter="onEnter"
      @leave="onLeave"
    >
      <div class="login-dialog relative bg-white rounded-lg w-[480px]" style="z-index: 9999;" ref="modalRef" role="dialog" aria-modal="true" aria-labelledby="login-title">
        <!-- 关闭按钮 -->
        <button 
          class="absolute right-6 top-6 text-gray-400 hover:text-gray-600 hover:bg-gray-100 rounded-full p-2" 
          type="button" aria-label="关闭登录" @click="onClose"
        >
          <svg class="w-5 h-5" viewBox="0 0 24 24" fill="none" stroke="currentColor">
            <path d="M6 18L18 6M6 6l12 12" stroke-width="2" stroke-linecap="round"/>
          </svg>
        </button>

        <!-- 登录内容 -->
        <div class="login-content text-center px-[72px] py-10">
          <h2 id="login-title" class="text-[18px] font-bold mb-12">手机号登录</h2>
          
          <!-- 手机号输入 -->
          <div class="relative mb-4">
            <div class="flex items-center bg-[#f4f4f4] rounded-3xl h-[48px] px-4">
              <div class="flex items-center text-[15px] text-gray-800">
                <span>+86</span>
                <div class="mx-2 w-[1px] h-[14px] bg-[#ddd]"></div>
              </div>
              <input 
                type="text" 
                placeholder="输入手机号" 
                autocomplete="off"
                ref="phoneInput"
                class="min-w-0 flex-1 outline-none text-[15px] ml-1 bg-transparent caret-[#ff2442]"
                v-model="formattedPhone"
                :disabled="isLoggingIn"
                maxlength="13"
                @input="formatPhoneNumber"
              >
            </div>
          </div>

          <!-- 验证码输入 -->
          <div class="relative mb-8">
            <div class="flex items-center bg-[#f4f4f4] rounded-3xl h-[48px] px-4">
              <input 
                type="text" 
                placeholder="输入验证码" 
                autocomplete="off"
                class="min-w-0 flex-1 outline-none text-[15px] bg-transparent caret-[#ff2442]"
                v-model="code"
                :disabled="isLoggingIn"
                maxlength="6"
                @input="formatCode"
              >
              <button 
                class="shrink-0 whitespace-nowrap text-[15px] font-medium ml-4 cursor-pointer transition-colors"
                :class="buttonClass"
                @click="getCode"
                :disabled="!isPhoneValid || countdown > 0 || isSending || isLoggingIn"
              >
                {{ countdown > 0 ? `重新发送(${countdown}s)` : '获取验证码' }}
              </button>
            </div>
          </div>

          <!-- 登录按钮 -->
          <button 
            class="w-full bg-[#ff2442] text-white rounded-full h-[48px] text-[16px] cursor-pointer
            font-bold hover:bg-opacity-90 mt-5"
            @click="handleLogin"
            :disabled="isLoggingIn"
          >
            登录
          </button>

          <!-- 协议勾选 -->
          <div class="mt-5 text-xs text-gray-700">
            <div class="flex justify-center gap-1">
              <input type="checkbox" class="w-4 h-4" v-model="agreeTerms" :disabled="isLoggingIn">
              <div>
                <span>我已阅读并同意</span>
                <a href="#" class="text-[#13386c] cursor-pointer">《用户协议》</a>
                <a href="#" class="text-[#13386c] cursor-pointer">《隐私政策》</a>
                <a href="#" class="text-[#13386c] cursor-pointer">《儿童/青少年个人信息保护规则》</a>
              </div>
            </div>
          </div>

          <!-- 新用户提示 -->
          <div class="mt-8 text-[14px] text-gray-500">
            新用户可直接登录
          </div>
        </div>
      </div>
    </Transition>

    <!-- 添加协议确认模态框 -->
    <TermsConfirmModal
      v-model:visible="showTermsConfirm"
      @confirm="handleConfirmTerms"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, nextTick, onUnmounted, watch } from 'vue'
import gsap from 'gsap'
import { message } from '@/utils/message'
import TermsConfirmModal from './TermsConfirmModal.vue'
import { login, getVerificationCode } from '@/api/auth'
import { getUserProfile } from '@/api/user'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:visible'])

const phone = ref('')
const formattedPhone = ref('')
const code = ref('')
const agreeTerms = ref(false)

const modalRef = ref(null)
const dialogRoot = ref<HTMLElement | null>(null)
const phoneInput = ref<HTMLInputElement | null>(null)
let previousFocus: HTMLElement | null = null
let beforeTermsFocus: HTMLElement | null = null

const showTermsConfirm = ref(false)

// 倒计时
const countdown = ref(0)
let timer: ReturnType<typeof setInterval> | null = null
const isSending = ref(false)
const isLoggingIn = ref(false)
let loginAttempt = 0
let pendingLoginToken: string | null = null

const invalidateLoginAttempt = () => {
  loginAttempt++
  isLoggingIn.value = false
  if (pendingLoginToken && userStore.token === pendingLoginToken) userStore.logout()
  pendingLoginToken = null
}

const onClose = () => {
  showTermsConfirm.value = false
  invalidateLoginAttempt()
  emit('update:visible', false)
}
const closeTopDialog = () => {
  if (showTermsConfirm.value) showTermsConfirm.value = false
  else onClose()
}
const trapFocus = (event: KeyboardEvent) => {
  const dialogs = dialogRoot.value?.querySelectorAll<HTMLElement>('[role="dialog"]')
  const activeDialog = dialogs?.[showTermsConfirm.value ? dialogs.length - 1 : 0]
  if (!activeDialog) return
  const focusable = Array.from(activeDialog.querySelectorAll<HTMLElement>(
    'a[href], button:not([disabled]), input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])'
  )).filter(element => element.getClientRects().length > 0)
  if (!focusable.length) return
  const first = focusable[0]!
  const last = focusable[focusable.length - 1]!
  const current = document.activeElement
  if (!activeDialog.contains(current) || (event.shiftKey && current === first) || (!event.shiftKey && current === last)) {
    event.preventDefault()
    ;(event.shiftKey ? last : first).focus()
  }
}
watch(showTermsConfirm, async visible => {
  if (visible) {
    beforeTermsFocus = document.activeElement instanceof HTMLElement ? document.activeElement : null
    await nextTick()
    if (showTermsConfirm.value) dialogRoot.value?.querySelector<HTMLElement>('[role="dialog"] [data-terms-confirm]')?.focus()
  } else {
    if (props.visible && beforeTermsFocus?.isConnected) beforeTermsFocus.focus()
    beforeTermsFocus = null
  }
})
watch(() => props.visible, async visible => {
  if (visible) {
    previousFocus = document.activeElement instanceof HTMLElement ? document.activeElement : null
    await nextTick()
    if (props.visible) phoneInput.value?.focus()
  } else {
    showTermsConfirm.value = false
    invalidateLoginAttempt()
    phone.value = ''
    formattedPhone.value = ''
    code.value = ''
    previousFocus?.focus()
    previousFocus = null
  }
})

// 验证手机号格式
const isPhoneValid = computed(() => {
  return phone.value.length === 11 && /^1[3-9]\d{9}$/.test(phone.value)
})

// 按钮样式计算属性
const buttonClass = computed(() => {
  if (countdown.value > 0) {
    return 'text-[#fe2c55]/40 cursor-not-allowed'
  }
  return isPhoneValid.value 
    ? 'text-[#fe2c55] hover:text-[#f4294f]' 
    : 'text-[#fe2c55]/60 cursor-not-allowed'
})

// 获取验证码
const getCode = async () => {
  if (!isPhoneValid.value || countdown.value > 0 || isSending.value) return
  isSending.value = true
  try {
    const res = await getVerificationCode(phone.value)
    if (!res.success) {
      message.show(res.message || '验证码获取失败')
      return
    }
    message.show('验证码请求成功')
    countdown.value = 180
    timer = setInterval(() => {
      countdown.value--
      if (countdown.value <= 0 && timer) {
        clearInterval(timer)
        timer = null
      }
    }, 1000)
  } catch (error) {
    console.error('获取验证码失败:')
  } finally {
    isSending.value = false
  }
}

// 组件卸载时清除定时器
onUnmounted(() => {
  if (timer) {
    clearInterval(timer)
  }
})

// 处理登录
const handleLogin = () => {
  if (!agreeTerms.value) {
    showTermsConfirm.value = true
    return
  }
  
  doLogin()
}

// 确认同意协议
const handleConfirmTerms = () => {
  agreeTerms.value = true
  doLogin()
}

// 执行登录
const doLogin = async () => {
  if (isLoggingIn.value) return
  if (!isPhoneValid.value) {
    message.show('请输入正确的手机号')
    return
  }
  if (!/^\d{6}$/.test(code.value)) {
    message.show('请输入正确的验证码')
    return
  }
  isLoggingIn.value = true
  const attempt = ++loginAttempt
  const previousToken = userStore.token
  try {
    const res = await login({ phone: phone.value, code: code.value, type: 1 })
    if (attempt !== loginAttempt || !props.visible || userStore.token !== previousToken) return
    if (!res.success || !res.data) {
      message.show(res.message || '登录失败，请重试')
      return
    }
    const loginToken = res.data
    pendingLoginToken = loginToken
    userStore.setProfile({})
    userStore.setToken(loginToken)
    try {
      const profile = await getUserProfile()
      if (attempt !== loginAttempt || !props.visible || userStore.token !== loginToken) return
      if (profile.success && profile.data?.userId) {
        userStore.setProfile(profile.data)
        message.show('登录成功')
      } else {
        message.show('登录成功，但个人资料加载失败，请刷新重试')
      }
    } catch (error) {
      if (attempt !== loginAttempt || !props.visible) return
      console.error('获取个人资料失败:')
      if (userStore.token && userStore.token !== loginToken) return
      if (!userStore.token) {
        message.show('登录状态已失效，请重新登录')
        return
      }
      message.show('登录成功，但个人资料加载失败，请刷新重试')
    }
    if (userStore.token === loginToken) {
      pendingLoginToken = null
      onClose()
    }
  } catch (error) {
    console.error('登录请求失败:')
  } finally {
    if (attempt === loginAttempt) isLoggingIn.value = false
  }
}

const onBeforeEnter = (el: Element) => {
  gsap.set(el, {
    opacity: 0,
    scale: 0.5,
    y: 40
  })
}

const onEnter = (el: Element) => {
  gsap.to(el, {
    opacity: 1,
    scale: 1,
    y: 0,
    duration: 0.3,
    ease: 'back.out(1.7)'
  })
}

const onLeave = (el: Element) => {
  gsap.to(el, {
    opacity: 0,
    scale: 0.5,
    duration: 0.2,
    ease: 'power2.in'
  })
}

// 格式化手机号
const formatPhoneNumber = (event: Event) => {
  // 移除所有非数字字符
  let value = (event.target as HTMLInputElement).value.replace(/\D/g, '')
  
  // 限制长度为11位数字
  if (value.length > 11) {
    value = value.slice(0, 11)
  }
  
  // 添加空格: xxx xxxx xxxx
  if (value.length > 7) {
    formattedPhone.value = `${value.slice(0, 3)} ${value.slice(3, 7)} ${value.slice(7)}`
  } else if (value.length > 3) {
    formattedPhone.value = `${value.slice(0, 3)} ${value.slice(3)}`
  } else {
    formattedPhone.value = value
  }
  
  // 更新实际的手机号(不带空格)
  phone.value = value
}

// 格式化验证码 - 只允许输入数字
const formatCode = (event: Event) => {
  // 移除所有非数字字符
  let value = (event.target as HTMLInputElement).value.replace(/\D/g, '')
  
  // 限制长度为6位数字
  if (value.length > 6) {
    value = value.slice(0, 6)
  }
  
  code.value = value
}
</script>

<style scoped>
@media (max-width: 520px) {
  .login-dialog { width: calc(100vw - 32px); }
  .login-content { padding-left: 24px; padding-right: 24px; }
}
.zoom-move {
  transition: transform 0.3s ease-out;
}

/* 可选：禁用 Chrome 浏览器的自动填充背景色 */
input:-webkit-autofill,
input:-webkit-autofill:hover,
input:-webkit-autofill:focus {
  -webkit-box-shadow: 0 0 0 30px white inset !important;
}
</style>
