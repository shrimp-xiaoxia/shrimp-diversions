<template>
  <div class="note-card">
    <button type="button"
      class="relative block w-full text-left rounded-lg overflow-hidden"
      :aria-label="note.title ? `查看笔记：${note.title}` : '查看笔记详情'"
      @click="$emit('click', note)"
    >
      <video v-if="note.type === 1 && note.videoUri && !videoFailed"
          :src="note.videoUri"
          class="w-full object-cover border border-gray-200 rounded-2xl hover:brightness-80 cursor-pointer"
          preload="metadata"
          muted
          @error="videoFailed = true"
        ></video>
      <img v-else-if="note.cover && !coverFailed"
        :src="note.cover"
        :alt="note.title ? `${note.title}的封面` : '笔记封面'"
        class="w-full object-cover border border-gray-200 rounded-2xl hover:brightness-80 cursor-pointer"
        @error="coverFailed = true"
      />
      <div v-else class="media-placeholder" role="img" aria-label="笔记封面暂不可用">
        <svg viewBox="0 0 24 24" width="30" height="30" fill="none" stroke="currentColor" aria-hidden="true">
          <rect x="3" y="4" width="18" height="16" rx="2" stroke-width="1.5"/>
          <path d="m4 17 5-5 4 4 3-3 4 4M8 9h.01" stroke-width="1.5" stroke-linecap="round"/>
        </svg>
        <span>封面暂不可用</span>
      </div>

      <div v-if="note.type === 1" class="absolute right-2 top-2 play-icon">
        <svg class="w-3 h-3" viewBox="0 0 24 24" fill="currentColor">
          <path d="M8 5v14l11-7z"/>
        </svg>
      </div>
    </button>

    <div class="p-[12px]">
      <h3 class="note-title">{{ note.title }}</h3>
      <div class="flex items-center">
        <router-link v-if="note.creatorId" :to="`/user/profile/${note.creatorId}`">
          <img
          v-if="note.avatar && !avatarFailed"
          :src="note.avatar"
          @error="avatarFailed = true"
          class="w-[20px] h-[20px] mr-[6px] rounded-full border border-gray-200"
        />
          <div 
            v-else 
            class="w-[20px] h-[20px] mr-[6px] rounded-full border border-gray-200
             flex items-center justify-center bg-gray-100 text-gray-400"
          >
            <svg class="w-3 h-3" viewBox="0 0 24 24" fill="none" stroke="currentColor">
              <path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2" stroke-width="2" stroke-linecap="round"/>
              <circle cx="12" cy="7" r="4" stroke-width="2" stroke-linecap="round"/>
            </svg>
          </div>
        </router-link>
        <span class="text-[12px] text-gray-600 hover:text-gray-800 flex-1 truncate">
          <router-link v-if="note.creatorId" :to="`/user/profile/${note.creatorId}`">
            {{ note.nickname }}
          </router-link>
          <span v-else>{{ note.nickname }}</span>
        </span>
        <button 
          class="flex items-center transition-colors group"
          @click.stop="toggleLike"
          :disabled="isUpdating"
          :aria-label="isLoggedIn && !stateKnown ? '操作点赞（先查询状态）' : isLiked ? '取消点赞' : '点赞'"
          :aria-pressed="stateKnown ? isLiked : undefined"
          :title="isLoggedIn && !stateKnown ? '点击后先查询点赞状态' : undefined"
        >
          <svg 
            class="w-[16px] h-[16px] transition-all duration-300" 
            :class="{'scale-animation': isLiked}"
            viewBox="0 0 24 24" 
            :style="{
              fill: isLiked ? '#fe2c55' : 'none',
              stroke: isLiked ? '#fe2c55' : 'currentColor'
            }"
            stroke-width="2"
          >
            <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/>
          </svg>
          <span 
            class="ml-1 text-[12px] text-gray-600 transition-all duration-300"
            :class="{'scale-animation': isLiked}"
          >
            {{ note.likeTotalAvailable === false || likeCount == null ? '—' : likeCount }}
          </span>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, inject, watch, onMounted, onBeforeUnmount, type PropType, type Ref } from 'vue'
import { isLikedAndCollectedData, likeNote, unlikeNote } from '@/api/note'
import type { NoteCardData } from '@/types/api'
import { message } from '@/utils/message'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const props = defineProps({
  note: { type: Object as PropType<NoteCardData>, required: true }
})
defineEmits<{ click: [note: NoteCardData] }>()

// 未返回当前用户状态时，首次操作前读取。
const coverFailed = ref(false)
const videoFailed = ref(false)
const avatarFailed = ref(false)
watch(() => [props.note.id, props.note.cover, props.note.videoUri, props.note.avatar], () => {
  coverFailed.value = false
  videoFailed.value = false
  avatarFailed.value = false
})
const isLiked = ref(props.note.isLiked === true && !!userStore.token)
const stateKnown = ref(typeof props.note.isLiked === 'boolean' && !!userStore.token)
const isUpdating = ref(false)
const likeCount = ref<number | string | null>(props.note.likeTotal ?? null)
const isLoggedIn = computed(() => !!userStore.token)
const showLoginModal = inject<Ref<boolean>>('showLoginModal', ref(false))
watch(() => [props.note.id, userStore.token], () => {
  isLiked.value = false
  stateKnown.value = false
  likeCount.value = props.note.likeTotal ?? null
})
watch(() => props.note.isLiked, value => {
  isLiked.value = value === true && !!userStore.token
  stateKnown.value = typeof value === 'boolean' && !!userStore.token
})
const onDetailLikeChanged = (event: Event) => {
  const detail = (event as CustomEvent<{ noteId: string; liked: boolean }>).detail
  if (!detail || detail.noteId !== props.note.id) return
  isLiked.value = detail.liked
  stateKnown.value = true
  const currentCount = Number(likeCount.value)
  if (likeCount.value !== null && Number.isFinite(currentCount)) {
    likeCount.value = Math.max(0, currentCount + (detail.liked ? 1 : -1))
  }
}
onMounted(() => window.addEventListener('shrimp:detail-like-changed', onDetailLikeChanged))
onBeforeUnmount(() => window.removeEventListener('shrimp:detail-like-changed', onDetailLikeChanged))

const toggleLike = async () => {
  if (!isLoggedIn.value) {
    showLoginModal.value = true
    return
  }
  if (isUpdating.value) return
  isUpdating.value = true
  const targetId = props.note.id
  const token = userStore.token
  try {
    if (!stateKnown.value) {
      const state = await isLikedAndCollectedData(targetId)
      if (!state.success || !state.data) {
        message.show(state.message || '无法获取点赞状态，请稍后重试')
        return
      }
      if (props.note.id !== targetId || userStore.token !== token) return
      isLiked.value = state.data.isLiked
      stateKnown.value = true
    }
    const res = await (isLiked.value ? unlikeNote(targetId) : likeNote(targetId))
    if (props.note.id !== targetId || userStore.token !== token) return
    if (!res.success) {
      message.show(res.message || '操作失败，请重试')
      return
    }
    const nextLiked = !isLiked.value
    const currentCount = Number(likeCount.value)
    if (likeCount.value !== null && Number.isFinite(currentCount)) {
      likeCount.value = Math.max(0, currentCount + (nextLiked ? 1 : -1))
    }
    isLiked.value = nextLiked
    window.dispatchEvent(new CustomEvent('shrimp:card-like-changed', {
      detail: { noteId: targetId, liked: nextLiked }
    }))
  } catch (error) {
    console.error('笔记点赞操作失败:')
  } finally {
    isUpdating.value = false
  }
}
</script>

<style scoped>
.media-placeholder {
  width: 100%;
  aspect-ratio: 4 / 5;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  border-radius: 16px;
  background: #f4f4f4;
  color: #777;
  font-size: 12px;
  cursor: pointer;
}

.note-title {
    margin-bottom: 8px;
    word-break: break-all;
    display: -webkit-box;
    -webkit-box-orient: vertical;
    -webkit-line-clamp: 2;
    overflow: hidden;
    font-weight: 500;
    font-size: 14px;
    line-height: 140%;
    color: #333;
}

/* 点赞动画 */
@keyframes scale {
  0% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.2);
  }
  100% {
    transform: scale(1);
  }
}

.scale-animation {
  animation: scale 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

/* 悬浮效果 */
button:hover svg {
  transform: scale(1.1);
}

.play-icon {
  display: flex
;
    align-items: center;
    justify-content: center;
    position: absolute;
    right: 14px;
    top: 14px;
    width: 20px;
    height: 20px;
    color: #fff;
    background: rgba(64,64,64,0.25);
    backdrop-filter: saturate(150%) blur(10px);
    border-radius: 20px;
}
</style>
