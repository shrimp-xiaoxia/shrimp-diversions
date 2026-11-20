<template>
  <div class="relative">
    <!-- 话题输入框 -->
    <div class="flex items-center border-b border-gray-100 py-3">
      <span class="text-[#ff2442] text-[14px] font-medium mr-2">#</span>
      <input
        ref="inputRef"
        v-model="keyword"
        type="text"
        placeholder="搜索添加话题"
        class="flex-1 text-[14px] focus:outline-none"
        @input="onInput"
        @keydown.enter="onEnter"
        @keydown.backspace="onBackspace"
      />
    </div>

    <!-- 已选话题展示 -->
    <div class="flex flex-wrap gap-2 mt-3">
      <div
        v-for="topic in selectedTopics"
        :key="topic.id"
        class="px-3 py-1.5 bg-gray-50 rounded-full text-[14px] text-gray-800 flex items-center gap-1"
      >
        <span class="text-[#ff2442]">#</span>
        {{ topic.name }}
        <button 
          class="ml-1 p-0.5 hover:bg-gray-200 rounded-full"
          @click="removeTopic(topic)"
        >
          <svg class="w-3 h-3" viewBox="0 0 24 24" fill="none" stroke="currentColor">
            <path d="M18 6L6 18M6 6l12 12" stroke-width="2" stroke-linecap="round"/>
          </svg>
        </button>
      </div>
    </div>

    <!-- 搜索结果下拉框 -->
    <div 
      v-if="showDropdown && suggestions.length" 
      class="absolute left-0 right-0 top-[100%] bg-white rounded-lg shadow-lg mt-1 py-2 max-h-[300px] overflow-auto z-10"
    >
      <div
        v-for="topic in suggestions"
        :key="topic.id"
        class="px-4 py-2 hover:bg-gray-50 cursor-pointer flex items-center"
        @click="selectTopic(topic)"
      >
        <span class="text-[#ff2442] mr-1">#</span>
        <div>
          <div class="text-[14px] text-gray-800">{{ topic.name }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onBeforeUnmount, type PropType } from 'vue'
import { getTopicList } from '@/api/topic'
import type { NamedItem } from '@/types/api'
import { message } from '@/utils/message'

const props = defineProps({
  modelValue: { type: Array as PropType<NamedItem[]>, default: () => [] }
})
const emit = defineEmits<{ 'update:modelValue': [value: NamedItem[]] }>()
const keyword = ref('')
const suggestions = ref<NamedItem[]>([])
const showDropdown = ref(false)
const inputRef = ref<HTMLInputElement | null>(null)
const selectedTopics = ref<NamedItem[]>(props.modelValue)
let timer: ReturnType<typeof setTimeout> | null = null
let searchSequence = 0

watch(() => props.modelValue, value => { selectedTopics.value = value })

const onInput = () => {
  if (timer) clearTimeout(timer)
  const searchId = ++searchSequence
  const text = keyword.value.trim()
  if (!text) {
    suggestions.value = []
    showDropdown.value = false
    return
  }
  timer = setTimeout(async () => {
    try {
      const res = await getTopicList(text)
      if (searchId !== searchSequence) return
      if (!res.success) {
        suggestions.value = []
        showDropdown.value = false
        message.show(res.message || '话题搜索失败')
        return
      }
      suggestions.value = res.data || []
      showDropdown.value = true
    } catch (error) {
      if (searchId === searchSequence) showDropdown.value = false
      console.error('话题搜索失败:')
    }
  }, 300)
}

const selectTopic = (topic: NamedItem) => {
  // 后端发布接口只支持一个话题。
  selectedTopics.value = [topic]
  emit('update:modelValue', selectedTopics.value)
  keyword.value = ''
  showDropdown.value = false
  searchSequence++
  inputRef.value?.focus()
}
const removeTopic = (topic: NamedItem) => {
  selectedTopics.value = selectedTopics.value.filter(item => item.id !== topic.id)
  emit('update:modelValue', selectedTopics.value)
}
const onEnter = () => {
  const first = suggestions.value[0]
  if (showDropdown.value && first) selectTopic(first)
}
const onBackspace = (event: KeyboardEvent) => {
  if (!keyword.value && selectedTopics.value.length) {
    event.preventDefault()
    selectedTopics.value = []
    emit('update:modelValue', [])
  }
}
onBeforeUnmount(() => {
  searchSequence++
  if (timer) clearTimeout(timer)
})
</script> 