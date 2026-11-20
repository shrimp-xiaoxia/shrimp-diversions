<template>
  <Teleport to="body">
    <!-- 遮罩层 -->
    <Transition name="fade">
      <div 
        v-if="visible" 
        class="fixed inset-0 z-[200] fullscreen-glass"
        ref="previewRef" role="dialog" aria-modal="true" aria-label="图片预览"
        @click="onClose"
        @wheel="handleWheel"
        @mouseenter="showControls = true"
        @mouseleave="showControls = false"
      >
        <!-- 关闭按钮 -->
        <button 
          ref="closeButtonRef" type="button" aria-label="关闭图片预览"
          class="absolute top-6 right-6 text-white/90 hover:text-white z-10 w-8 h-8 rounded-full bg-black/20 flex items-center justify-center backdrop-blur-sm"
          @click="onClose"
        >
          <svg class="w-5 h-5" viewBox="0 0 24 24" fill="none" stroke="currentColor">
            <path d="M18 6L6 18M6 6l12 12" stroke-width="2" stroke-linecap="round"/>
          </svg>
        </button>

        <!-- 图片计数器 - 只在显示控件时可见 -->
        <Transition name="fade">
          <div
            v-if="images.length > 1 && showControls"
            class="absolute top-6 left-6 px-[12px] py-[6px] rounded-full bg-black/20 backdrop-blur-sm text-white/90 text-xs"
          >
            {{ currentIndex + 1 }}/{{ images.length }}
          </div>
        </Transition>

        <!-- 图片展示区域 -->
        <div class="h-full overflow-hidden">
          <div
            class="h-full flex transition-transform duration-500 ease-in-out"
            :style="{ transform: `translateX(-${currentIndex * 100}%)` }"
          >
            <div 
              v-for="(image, index) in images" 
              :key="index"
              class="h-full w-full flex-shrink-0 flex items-center justify-center cursor-zoom-out"
              @click="onClose"
            >
              <img 
                :src="image" 
                class="h-full w-auto object-contain"
                @click.stop="onClose"
              />
            </div>
          </div>
        </div>

        <!-- 左右切换按钮 - 只在显示控件时可见 -->
        <Transition name="fade">
          <button type="button" aria-label="上一张预览图片"
            v-if="images.length > 1 && showControls"
            class="absolute left-6 top-1/2 -translate-y-1/2 text-white/90 hover:text-white cursor-pointer 
            w-8 h-8 rounded-full bg-black/20 backdrop-blur-sm flex items-center justify-center"
            @click.stop="prev"
          >
            <svg class="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor">
              <path d="M15 19l-7-7 7-7" stroke-width="2" stroke-linecap="round"/>
            </svg>
          </button>
        </Transition>

        <Transition name="fade">
          <button type="button" aria-label="下一张预览图片"
            v-if="images.length > 1 && showControls"
            class="absolute right-6 top-1/2 -translate-y-1/2 text-white/90 hover:text-white cursor-pointer 
            w-8 h-8 rounded-full bg-black/20 backdrop-blur-sm flex items-center justify-center"
            @click.stop="next"
          >
            <svg class="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor">
              <path d="M9 5l7 7-7 7" stroke-width="2" stroke-linecap="round"/>
            </svg>
          </button>
        </Transition>

        <!-- 底部指示器 - 始终显示 -->
        <div 
          v-if="images.length > 1"
          class="absolute bottom-6 left-1/2 -translate-x-1/2 flex items-center gap-2"
        >
          <button 
            v-for="(_, index) in images" 
            :key="index"
            type="button" :aria-label="`预览第 ${index + 1} 张图片`" :aria-current="index === currentIndex ? 'true' : undefined"
            class="w-1.5 h-1.5 rounded-full transition-all duration-200 bg-black/20 backdrop-blur-sm"
            :class="[
              index === currentIndex ? 'w-3 bg-white' : 'hover:bg-white/60'
            ]"
            @click.stop="currentIndex = index"
          />
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import { ref, watch, nextTick, onMounted, onBeforeUnmount, type PropType } from 'vue'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  images: {
    type: Array as PropType<string[]>,
    default: () => []
  },
  initialIndex: {
    type: Number,
    default: 0
  }
})

const emit = defineEmits(['update:visible'])
const currentIndex = ref(props.initialIndex)
const previewRef = ref<HTMLElement | null>(null)
const closeButtonRef = ref<HTMLButtonElement | null>(null)
let previousFocus: HTMLElement | null = null


// 监听 visible 变化，重置 currentIndex
watch(() => props.visible, async (newVisible) => {
  if (newVisible) {
    previousFocus = document.activeElement instanceof HTMLElement ? document.activeElement : null
    currentIndex.value = props.initialIndex
    await nextTick()
    if (props.visible) closeButtonRef.value?.focus()
  } else {
    if (previousFocus?.isConnected) previousFocus.focus()
    previousFocus = null
  }
})

// 关闭预览
const onClose = () => {
  emit('update:visible', false)
}

// 上一张
const prev = () => {
  if (props.images.length <= 1) return
  currentIndex.value = currentIndex.value === 0 
    ? props.images.length - 1 
    : currentIndex.value - 1
}

// 下一张
const next = () => {
  if (props.images.length <= 1) return
  currentIndex.value = (currentIndex.value + 1) % props.images.length
}

// 键盘事件处理
const handleKeydown = (e: KeyboardEvent) => {
  if (!props.visible) return

  if (e.key === 'Tab' && previewRef.value) {
    const focusable = Array.from(previewRef.value.querySelectorAll<HTMLElement>('button:not([disabled]), [tabindex]:not([tabindex="-1"])'))
      .filter(element => element.getClientRects().length > 0)
    if (!focusable.length) return
    const first = focusable[0]!
    const last = focusable[focusable.length - 1]!
    const current = document.activeElement
    if (!previewRef.value.contains(current) || (e.shiftKey && current === first) || (!e.shiftKey && current === last)) {
      e.preventDefault()
      ;(e.shiftKey ? last : first).focus()
    }
    return
  }
  
  switch (e.key) {
    case 'Escape':
      e.preventDefault()
      onClose()
      break
    case 'ArrowLeft':
      e.preventDefault()
      prev()
      break
    case 'ArrowRight':
      e.preventDefault()
      next()
      break
  }
}

// 添加键盘事件监听
onMounted(() => {
  window.addEventListener('keydown', handleKeydown)
})

// 移除键盘事件监听
onBeforeUnmount(() => {
  window.removeEventListener('keydown', handleKeydown)
})

// 添加滚动节流控制
let wheelTimeout: ReturnType<typeof setTimeout> | null = null
const WHEEL_DELAY = 200 // 200ms 的节流延迟

// 处理鼠标滚轮事件
const handleWheel = (e: WheelEvent) => {
  e.preventDefault() // 阻止页面滚动
  
  // 如果已经有待执行的滚动，则忽略新的滚动
  if (wheelTimeout) return
  
  // 执行滚动并设置节流
  if (e.deltaY > 0) {
    next()
  } else {
    prev()
  }
  
  // 设置节流定时器
  wheelTimeout = setTimeout(() => {
    wheelTimeout = null
  }, WHEEL_DELAY)
}

// 组件卸载时清理定时器
onBeforeUnmount(() => {
  if (wheelTimeout) {
    clearTimeout(wheelTimeout)
  }
})

// 控件显示状态
const showControls = ref(false)
</script>

<style scoped>
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/* 添加图片切换动画 */
.slide-enter-active,
.slide-leave-active {
  transition: transform 0.5s ease-in-out;
}

.slide-enter-from {
  transform: translateX(100%);
}

.slide-leave-to {
  transform: translateX(-100%);
}

.fullscreen-glass {
    backdrop-filter: blur(20px);
    background: rgba(0, 0, 0, 0.25);
}

/* 添加控件悬停效果 */
.fullscreen-glass button:hover,
.fullscreen-glass div[class*="rounded-full"]:hover {
  background-color: rgba(0, 0, 0, 0.3);
  transition: background-color 0.2s ease;
}

/* 添加指示器动画 */
button {
  transition: all 0.2s ease;
}
</style> 
