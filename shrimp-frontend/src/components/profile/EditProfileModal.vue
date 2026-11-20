<template>
  <BaseModal
    :visible="visible"
    @update:visible="$emit('update:visible', $event)"
    title="编辑资料"
    width="640px"
    :busy="isSaving"
    @confirm="handleConfirm"
  >
    <div class="p-6">
      <!-- 头像 -->
      <div class="flex items-center gap-4 mb-6">
        <img
          v-if="form.avatar" 
          :src="form.avatar" 
          alt="头像预览"
          @error="useDefaultAvatar"
          class="w-20 h-20 rounded-full object-cover border-1 border-gray-100"
        />
        <span v-else class="w-20 h-20 bg-gray-100 rounded-full object-cover border-1 border-gray-100 flex items-center justify-center">
            <svg class="w-10 h-10 text-gray-300" viewBox="0 0 24 24" fill="none" stroke="currentColor">
              <path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2" stroke-width="2" stroke-linecap="round"/>
              <circle cx="12" cy="7" r="4" stroke-width="2" stroke-linecap="round"/>
            </svg>
        </span>
        <div>
          <input 
            type="file" 
            ref="avatarInput"
            accept="image/*"
            class="hidden"
            @change="handleAvatarChange"
          />
          <button 
            class="text-sm px-4 h-9 border border-gray-200 rounded-full text-gray-600 hover:bg-gray-50"
            @click="avatarInput?.click()"
          >
            更换头像
          </button>
        </div>
      </div>

      <!-- 表单 -->
      <div class="space-y-6">
        <!-- 昵称 -->
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-2">昵称</label>
          <div class="relative">
            <input
              v-model="form.nickname"
              type="text"
              class="w-full h-10 px-3 border border-gray-200 rounded-lg focus:outline-none focus:border-[#ff2442]"
              placeholder="请输入昵称"
              maxlength="24"
              @input="updateNicknameCount"
            />
            <span class="absolute right-3 top-1/2 transform -translate-y-1/2 text-xs text-gray-400">
              {{ nicknameCount }}/24
            </span>
          </div>
          <p class="mt-1 text-xs text-gray-500">
            请设置 2-24 个字符，不包括 @<>/ 等无效字符哦
          </p>
        </div>

        <!-- 虾趣号 -->
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-2">虾趣号</label>
          <div class="relative">
            <input
              v-model="form.xiaohashuId"
              type="text"
              class="w-full h-10 px-3 border border-gray-200 rounded-lg focus:outline-none focus:border-[#ff2442]"
              placeholder="请输入虾趣号"
              maxlength="15"
              @input="updateXiaohashuIdCount"
            />
            <span class="absolute right-3 top-1/2 transform -translate-y-1/2 text-xs text-gray-400">
              {{ xiaohashuIdCount }}/15
            </span>
          </div>
          <p class="mt-1 text-xs text-gray-500">
            6-15 个字符，仅可使用英文（必须）、数字、下划线
          </p>
        </div>

        <!-- 生日 -->
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-2">生日</label>
          <div class="flex gap-3 text-sm">
            <select 
              v-model="form.birthYear"
              class="h-10 px-3 border border-gray-200 rounded-lg focus:outline-none focus:border-[#ff2442] bg-white"
            >
              <option value="">年</option>
              <option 
                v-for="year in birthYears" 
                :key="year" 
                :value="year"
              >{{ year }}</option>
            </select>
            <select 
              v-model="form.birthMonth"
              class="h-10 px-3 border border-gray-200 rounded-lg focus:outline-none focus:border-[#ff2442] bg-white"
            >
              <option value="">月</option>
              <option 
                v-for="month in months" 
                :key="month.value" 
                :value="month.value"
              >{{ month.label }}</option>
            </select>
            <select 
              v-model="form.birthDay"
              class="h-10 px-3 border border-gray-200 rounded-lg focus:outline-none focus:border-[#ff2442] bg-white"
            >
              <option value="">日</option>
              <option 
                v-for="day in getDaysInMonth(form.birthYear, form.birthMonth)" 
                :key="day" 
                :value="day.toString().padStart(2, '0')"
              >{{ day }}日</option>
            </select>
          </div>
        </div>

        <!-- 简介 -->
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-2">简介</label>
          <div class="relative">
            <textarea
              v-model="form.introduction"
              rows="3"
              class="w-full p-3 border border-gray-200 rounded-lg focus:outline-none focus:border-[#ff2442] resize-vertical min-h-[80px] max-h-[200px] pr-16"
              placeholder="介绍一下自己吧"
              maxlength="100"
              @input="updateIntroductionCount"
            ></textarea>
            <span class="absolute right-3 bottom-3 text-xs text-gray-400">
              {{ introductionCount }}/100
            </span>
          </div>
        </div>

        <!-- 性别 -->
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-2">性别</label>
          <div class="flex gap-4">
            <label class="flex items-center cursor-pointer">
              <input
                v-model="form.sex"
                type="radio"
                :value="0"
                class="w-4 h-4 text-[#ff2442]"
              />
              <span class="ml-2">女</span>
            </label>
            <label class="flex items-center cursor-pointer">
              <input
                v-model="form.sex"
                type="radio"
                :value="1"
                class="w-4 h-4 text-[#ff2442]"
              />
              <span class="ml-2">男</span>
            </label>
          </div>
        </div>
      </div>
    </div>
  </BaseModal>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import BaseModal from '@/components/common/BaseModal.vue'
import { useUserStore } from '@/stores/user'
import { message } from '@/utils/message'
import { useDefaultAvatar } from '@/utils/avatar'
import { updateUserProfile, getUserProfile } from '@/api/user'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:visible', 'update-success'])

const userStore = useUserStore()
const avatarInput = ref<HTMLInputElement | null>(null)
const isSaving = ref(false)

// 表单数据
interface ProfileForm {
  avatar: string
  avatarFile: File | null
  nickname: string
  xiaohashuId: string
  birthYear: string
  birthMonth: string
  birthDay: string
  introduction: string
  sex: number | null
}
const form = ref<ProfileForm>({
  avatar: '',
  avatarFile: null,
  nickname: '',
  xiaohashuId: '',
  birthYear: '',
  birthMonth: '',
  birthDay: '',
  introduction: '',
  sex: null
})

// 昵称字数计数
const nicknameCount = ref(0)
// 虾趣号字数计数
const xiaohashuIdCount = ref(0)
// 简介字数计数
const introductionCount = ref(0)

// 更新昵称字数计数
const updateNicknameCount = () => {
  nicknameCount.value = form.value.nickname.length
}

// 更新虾趣号字数计数
const updateXiaohashuIdCount = () => {
  xiaohashuIdCount.value = form.value.xiaohashuId.length
}

// 更新简介字数计数
const updateIntroductionCount = () => {
  introductionCount.value = form.value.introduction.length
}

// 监听 visible 变化，当打开模态框时初始化表单数据
let formToken = ''
let avatarReadSequence = 0
watch(() => props.visible, (newValue) => {
  avatarReadSequence++
  if (newValue) {
    formToken = userStore.token
    initFormData()
  }
})
watch(() => userStore.token, token => {
  if (props.visible && token !== formToken) emit('update:visible', false)
})

// 在组件挂载时初始化表单数据
onMounted(() => {
  if (props.visible) {
    formToken = userStore.token
    initFormData()
  }
})

// 初始化表单数据
const initFormData = () => {
  if (userStore.profile) {
    // 从 userStore 获取数据并填充表单
    const profile = userStore.profile
    
    // 处理生日格式 (从 "YYYY-MM-DD" 格式拆分为年、月、日)
    let birthYear = ''
    let birthMonth = ''
    let birthDay = ''
    
    if (profile.birthday) {
      const birthdayParts = profile.birthday.split('-')
      if (birthdayParts.length === 3) {
        birthYear = birthdayParts[0]
        birthMonth = birthdayParts[1]
        birthDay = birthdayParts[2]
      }
    }
    
    form.value = {
      avatar: profile.avatar || '',
      avatarFile: null,
      nickname: profile.nickname || '',
      xiaohashuId: profile.xiaohashuId || '',
      birthYear,
      birthMonth,
      birthDay,
      introduction: profile.introduction || '',
      sex: profile.sex ?? null
    }
    
    // 初始化昵称字数计数
    nicknameCount.value = form.value.nickname.length
    // 初始化虾趣号字数计数
    xiaohashuIdCount.value = form.value.xiaohashuId.length
    // 初始化简介字数计数
    introductionCount.value = form.value.introduction.length
  }
}

// 生成年份选项（从当前年份开始，往前推算100年）
const birthYears = computed(() => {
  const currentYear = new Date().getFullYear()
  const years = []
  // 从当前年份开始，往前推算100年
  for (let i = currentYear; i >= currentYear - 100; i--) {
    years.push(i.toString())
  }
  return years
})

// 获取指定年月的天数
const getDaysInMonth = (year: string, month: string) => {
  if (!year || !month) return 31
  return new Date(Number(year), Number(month), 0).getDate()
}

// 月份选项
const months = [
  { value: '01', label: '1月' },
  { value: '02', label: '2月' },
  { value: '03', label: '3月' },
  { value: '04', label: '4月' },
  { value: '05', label: '5月' },
  { value: '06', label: '6月' },
  { value: '07', label: '7月' },
  { value: '08', label: '8月' },
  { value: '09', label: '9月' },
  { value: '10', label: '10月' },
  { value: '11', label: '11月' },
  { value: '12', label: '12月' }
]

// 表单验证和提交
const handleConfirm = async () => {
  if (isSaving.value) return
  if (!formToken || userStore.token !== formToken) {
    message.show('登录状态已变化，请重新打开资料编辑')
    return
  }
  // 验证表单
  if (!form.value.nickname.trim()) {
    message.show('请输入昵称')
    return
  }
  
  // 昵称长度验证
  const nickname = form.value.nickname.trim()
  if (nickname.length < 2 || nickname.length > 24) {
    message.show('昵称长度应为 2-24 个字符')
    return
  }
  
  // 昵称特殊字符验证
  const invalidCharsRegex = /[@<>/\\:*?"'|]/
  if (invalidCharsRegex.test(nickname)) {
    message.show('昵称不能包含 @<>/ 等特殊字符')
    return
  }

  // 未修改历史账号标识时不重新校验或提交。
  const xiaohashuId = form.value.xiaohashuId.trim()
  const idChanged = xiaohashuId !== (userStore.profile.xiaohashuId || '')
  if (idChanged && !/^(?=.*[a-zA-Z])[a-zA-Z0-9_]{6,15}$/.test(xiaohashuId)) {
    message.show('虾趣号需为 6-15 位，包含英文字母且只能使用字母、数字和下划线')
    return
  }

  const { birthYear, birthMonth, birthDay } = form.value
  const hasBirthday = Boolean(birthYear || birthMonth || birthDay)
  if (hasBirthday && (!birthYear || !birthMonth || !birthDay)) {
    message.show('请选择完整的生日信息')
    return
  }
  if (hasBirthday && Number(birthDay) > getDaysInMonth(birthYear, birthMonth)) {
    message.show('请选择有效的生日日期')
    return
  }
  const birthday = hasBirthday ? `${birthYear}-${birthMonth}-${birthDay}` : undefined
  if (birthday && birthday > new Date().toLocaleDateString('sv-SE')) {
    message.show('生日不能是未来日期')
    return
  }
  const profileData = {
    avatar: form.value.avatarFile,
    nickname,
    xiaohashuId: idChanged ? xiaohashuId : undefined,
    birthday,
    introduction: form.value.introduction,
    sex: form.value.sex
  }
  const requestToken = formToken
  isSaving.value = true
  try {
    const res = await updateUserProfile(profileData)
    if (userStore.token !== requestToken) return
    if (!res.success) {
      message.show(res.message || '更新失败，请重试')
      return
    }
    try {
      const profile = await getUserProfile()
      if (userStore.token !== requestToken) return
      if (profile.success && profile.data) userStore.setProfile(profile.data)
      else message.show('资料已保存，但刷新失败')
    } catch (error) {
      if (userStore.token !== requestToken) return
      console.error('刷新资料失败:')
      message.show('资料已保存，但刷新失败')
    }
    emit('update-success')
    emit('update:visible', false)
  } catch (error) {
    console.error('更新资料出错:')
  } finally {
    isSaving.value = false
  }
}

// 处理头像上传
const handleAvatarChange = (event: Event) => {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return
  
  // 验证文件类型
  if (!file.type.startsWith('image/')) {
    message.show('请上传图片文件')
    return
  }
  
  // 验证文件大小（限制为 5MB）
  if (file.size > 5 * 1024 * 1024) {
    message.show('图片大小不能超过 5MB')
    return
  }
  
  // 存储文件对象
  form.value.avatarFile = file
  const sequence = ++avatarReadSequence
  
  // 创建临时 URL 用于预览
  const reader = new FileReader()
  reader.onload = () => {
    if (sequence === avatarReadSequence && props.visible && typeof reader.result === 'string') {
      form.value.avatar = reader.result
    }
  }
  reader.onerror = () => {
    if (sequence !== avatarReadSequence || !props.visible) return
    form.value.avatarFile = null
    form.value.avatar = userStore.profile.avatar || ''
    message.show('头像预览失败，请重试')
  }
  reader.readAsDataURL(file)
}
</script>

<style scoped>
/* 自定义下拉框样式 */
select {
  appearance: none;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' fill='none' viewBox='0 0 24 24' stroke='%23666666'%3E%3Cpath stroke-linecap='round' stroke-linejoin='round' stroke-width='2' d='M19 9l-7 7-7-7'%3E%3C/path%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 8px center;
  background-size: 16px;
  padding-right: 32px;
  min-width: 80px;
}

select:focus {
  outline: none;
  border-color: #ff2442;
}

/* 禁用状态下的输入框样式 */
input:disabled {
  background-color: #f5f5f5;
  cursor: not-allowed;
}
</style> 
