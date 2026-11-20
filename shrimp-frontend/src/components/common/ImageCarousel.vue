<template>
  <div 
    class="relative h-full flex items-center justify-center group"
    ref="carouselRef"
    @mouseenter="showControls = true"
    @mouseleave="showControls = false"
    @wheel="handleWheel"
  >
    <!-- 图片容器 -->
    <div class="h-full w-full overflow-hidden">
      <div 
        class="h-full flex transition-transform duration-500 ease-in-out"
        :style="{ transform: `translateX(-${currentIndex * 100}%)` }"
      >
        <div 
          v-for="(image, index) in images" 
          :key="index"
          class="h-full w-full flex-shrink-0 flex items-center justify-center bg-gray-100 border-r border-gray-200"
        >
          <img v-if="!failedImages.has(index)"
            :key="`${image}-${retryCounts[index] ?? 0}`"
            :src="image" 
            class="h-full w-full object-contain cursor-zoom-in"
            alt="笔记图片" role="button" :tabindex="index === currentIndex ? 0 : -1" aria-label="打开图片预览"
            @error="markFailed(index)"
            @click="onImageClick"
            @keydown.enter.prevent="onImageClick" @keydown.space.prevent="onImageClick"
          />
          <div v-else class="image-error" role="status">
            <span>图片暂不可用</span>
            <button type="button" @click.stop="retryImage(index)">重试</button>
          </div>
        </div>
      </div>
    </div>

    <!-- 图片计数器 -->
    <Transition name="fade">
      <div 
        v-if="images.length > 1 && showControls"
        class="absolute top-4 right-4 px-[12px] py-[6px] rounded-full bg-black/40 backdrop-blur-sm text-white text-xs"
      >
        {{ currentIndex + 1 }}/{{ images.length }}
      </div>
    </Transition>

    <!-- 左右切换按钮 -->
    <Transition name="fade">
      <button 
        v-if="images.length > 1 && showControls"
        type="button" aria-label="上一张图片"
        class="carousel-btn left-4" 
        @click="prev"
      >
        <svg class="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor">
          <path d="M15 19l-7-7 7-7" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
        </svg>
      </button>
    </Transition>
    
    <Transition name="fade">
      <button 
        v-if="images.length > 1 && showControls"
        type="button" aria-label="下一张图片"
        class="carousel-btn right-4" 
        @click="next"
      >
        <svg class="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor">
          <path d="M9 5l7 7-7 7" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
        </svg>
      </button>
    </Transition>

    <!-- 底部指示器 -->
    <div 
      v-if="images.length > 1"
      class="absolute bottom-4 left-1/2 -translate-x-1/2 flex items-center gap-2"
    >
      <button 
        v-for="(_, index) in images" 
        :key="index"
        type="button" :aria-label="`查看第 ${index + 1} 张图片`" :aria-current="index === currentIndex ? 'true' : undefined"
        class="w-1.5 h-1.5 rounded-full transition-all duration-200"
        :class="index === currentIndex ? 'bg-white scale-125' : 'bg-white/50 hover:bg-white/80'"
        @click="currentIndex = index"
      />
    </div>

    <!-- 图片预览 -->
    <ImagePreview
      v-model:visible="showPreview"
      :images="images"
      :initial-index="currentIndex"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onBeforeUnmount, onMounted, type PropType } from 'vue'
import ImagePreview from './ImagePreview.vue'

const props = defineProps({
  images: {
    type: Array as PropType<string[]>,
    required: true,
    default: () => []
  }
})

const currentIndex = ref(0)
const carouselRef = ref<HTMLElement | null>(null)
const failedImages = ref<Set<number>>(new Set())
const retryCounts = ref<Record<number, number>>({})
const markFailed = (index: number) => { failedImages.value.add(index) }
const retryImage = (index: number) => {
  retryCounts.value[index] = (retryCounts.value[index] ?? 0) + 1
  failedImages.value.delete(index)
}
const showControls = ref(false)
const showPreview = ref(false)

// 添加滚动节流控制
let wheelTimeout: ReturnType<typeof setTimeout> | null = null
const WHEEL_DELAY = 200 // 200ms 的节流延迟

// 监听 images 变化，重置 currentIndex
watch(() => props.images, (newImages) => {
  failedImages.value.clear()
  retryCounts.value = {}
  if (newImages && newImages.length) {
    currentIndex.value = 0
  }
}, { immediate: true })

// 键盘事件处理
const handleKeydown = (e: KeyboardEvent) => {
  const target = e.target
  if (showPreview.value || !props.images.length || props.images.length <= 1 ||
      (target instanceof HTMLElement && (target.isContentEditable || /^(INPUT|TEXTAREA|SELECT)$/.test(target.tagName)))) return
  const ownerDialog = carouselRef.value?.closest('[role="dialog"]')
  if (ownerDialog && (!(target instanceof Element) || target.closest('[role="dialog"]') !== ownerDialog)) return
  
  switch (e.key) {
    case 'ArrowLeft':
    case 'PageUp':
      e.preventDefault()
      prev()
      break
    case 'ArrowRight':
    case 'PageDown':
      e.preventDefault()
      next()
      break
  }
}

// 组件挂载时添加键盘事件监听
onMounted(() => {
  window.addEventListener('keydown', handleKeydown)
})

// 组件卸载时移除键盘事件监听
onBeforeUnmount(() => {
  window.removeEventListener('keydown', handleKeydown)
  currentIndex.value = 0
  if (wheelTimeout) {
    clearTimeout(wheelTimeout)
  }
})

const next = () => {
  if (!props.images.length) return
  currentIndex.value = (currentIndex.value + 1) % props.images.length
}

const prev = () => {
  if (!props.images.length) return
  currentIndex.value = currentIndex.value === 0 
    ? props.images.length - 1 
    : currentIndex.value - 1
}

// 修改图片点击事件
const onImageClick = () => {
  showPreview.value = true
}

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
</script>

<style scoped>
.image-error {
  display: flex;
  width: 100%;
  height: 100%;
  min-width: 160px;
  min-height: 160px;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  gap: 10px;
  color: #777;
  font-size: 14px;
}
.image-error button { text-decoration: underline; }

.carousel-btn {
  @apply absolute top-1/2;
  transform: translateY(-50%);
  width: 30px;
  height: 30px;
  border-radius: 9999px;
  background-color: rgba(0, 0, 0, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  transition: all 0.2s;
}

.carousel-btn:hover {
  background-color: rgba(0, 0, 0, 0.4);
}

/* 添加计数器的动画效果 */
.carousel-counter-enter-active,
.carousel-counter-leave-active {
  transition: opacity 0.2s ease;
}

.carousel-counter-enter-from,
.carousel-counter-leave-to {
  opacity: 0;
}

/* 添加图片容器过渡效果 */
.carousel-container {
  transition: width 0.3s ease-in-out;
}

/* 添加淡入淡出动画 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style> 
