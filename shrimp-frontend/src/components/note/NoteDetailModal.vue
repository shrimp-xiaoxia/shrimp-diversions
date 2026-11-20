<template>
  <Teleport to="body">
    <!-- 遮罩层 -->
    <div v-if="visible" class="fixed inset-0 bg-gray-800/25 z-[100]" @click="onClose"></div>
    
    <!-- 笔记详情 -->
    <Transition 
      name="zoom"
      appear
      @before-enter="onBeforeEnter"
      @enter="onEnter"
      @leave="onLeave"
    >
      <div v-if="visible" class="fixed inset-0 z-[101] pointer-events-none">
        <div 
          class="note-dialog absolute left-1/2 top-1/2 -translate-x-1/2 -translate-y-1/2 bg-white h-[90vh] 
          max-w-[90%] md:max-w-[85%] lg:max-w-[80%] xl:max-w-[70%] 2xl:max-w-[60%] w-full rounded-lg flex overflow-hidden pointer-events-auto"
          ref="modalRef"
          role="dialog"
          aria-modal="true"
          aria-label="笔记详情"
        >
          <button ref="closeButtonRef" class="detail-close" type="button" aria-label="关闭笔记详情" @click="onClose">×</button>
          <div v-if="detailLoading" class="flex min-w-[280px] flex-1 items-center justify-center p-8 text-gray-500" role="status">笔记加载中…</div>
          <div v-else-if="detailError" class="flex min-w-[280px] flex-1 flex-col items-center justify-center gap-3 p-8 text-gray-500" role="alert">
            <span>笔记详情加载失败</span>
            <button type="button" class="underline" @click="loadNoteDetail">重试</button>
          </div>
          <template v-else>
          <!-- 左侧图片区域 -->
          <div class="note-media h-full flex-1 flex items-center justify-center overflow-hidden">
            <div class="h-full w-full flex items-center justify-center">
              <ImageCarousel v-if="currNote.type === 0" :images="currNote.imgUris || []" class="h-full w-full" />
              <VideoPlayer v-else-if="currNote.type === 1"
                      :video-url="currNote.videoUri || ''" 
                      :autoplay="true"
                    ></VideoPlayer>
            </div>
          </div>

          <!-- 右侧内容区域 -->
          <div class="note-body w-[480px] min-w-[480px] flex flex-col bg-white">

            <!-- 作者信息 -->
            <div 
              class="p-[24px] flex items-center sticky top-0 bg-white"
              :class="{'border-b border-gray-100': isScrolled}"
              ref="authorInfoRef"
            >
              <router-link v-if="currNote.creatorId" :to="`/user/profile/${currNote.creatorId}`">
                      <img 
                      :src="currNote.avatar || defaultAvatar" @error="useDefaultAvatar" alt="作者头像"
                      class="w-[40px]! h-[40px]! rounded-full"
                    />
              </router-link>

              <router-link v-if="currNote.creatorId" :to="`/user/profile/${currNote.creatorId}`" class="ml-[12px] flex-1">
                      <div class="font-medium text-[16px] text-gray-600 hover:text-gray-800">{{ currNote.creatorName }}</div>
              </router-link>
              <div v-else class="ml-[12px] flex-1 text-gray-500">{{ currNote.creatorName || '作者信息暂不可用' }}</div>
              <div v-if="userStore.token && currNote.creatorId && String(currNote.creatorId) === String(userStore.profile.userId)" class="relative">
                <button type="button" class="rounded-full px-3 py-2 text-gray-600 hover:bg-gray-100" aria-label="笔记更多操作" :aria-expanded="showNoteMenu" @click="showNoteMenu = !showNoteMenu">···</button>
                <div v-if="showNoteMenu" class="absolute right-0 top-full z-10 rounded-lg border border-gray-100 bg-white shadow-lg">
                  <button type="button" class="whitespace-nowrap px-4 py-3 text-sm text-red-500 hover:bg-gray-50 disabled:opacity-50" :disabled="deletingNote" @click="handleDeleteNote">删除笔记</button>
                </div>
              </div>
              

              <button 
              v-if="currNote.creatorId && (!userStore.token || userStore.profile.userId !== currNote.creatorId)"
              @click="handleFollow"
              :disabled="followBusy || (isLoggedIn && followState === null)"
              class="bg-[#ff2442] text-white px-6 py-2 rounded-full font-bold hover:opacity-90 w-[96px] h-[40px] cursor-pointer">
                {{ isLoggedIn && followState === null ? '状态不可用' : followState ? '已关注' : '关注' }}
              </button>
            </div>

            <!-- 评论区域容器 -->
            <div 
              class="overflow-y-auto flex-1" 
              @scroll="handleScroll"
              ref="scrollContainerRef"
            >
              <!-- 笔记正文 -->
              <div 
                class="text-[#333] px-[24px] pb-[24px] flex-1"
                ref="contentRef"
              >
                <h1 class="title">{{ currNote.title }}</h1>
                <div class="note-conten whitespace-pre-wrap">{{ currNote.content }}</div>
                <ul v-if="currNote.topics && currNote.topics.length > 0" class="text-[#13386c] flex flex-wrap gap-2">
                  <li v-for="(topic, index) in currNote.topics" :key="index" class="cursor-pointer">#{{topic.name}}</li>
                </ul>
                <div class="text-gray-500 text-[14px] mt-[12px]">
                  编辑于 {{ currNote.updateTime }}
                </div>
              </div>

              <!-- 分割线 -->
              <div class="h-[1px] border-b border-gray-100 mx-[24px]"></div>

              <!-- 评论区 -->
              <div class="flex justify-end px-6 pt-3">
                <button type="button" class="text-sm text-gray-500 underline hover:text-gray-800 disabled:opacity-50" :disabled="commentsLoading" @click="refreshComments">刷新评论</button>
              </div>
              <div v-if="commentsLoading" class="p-6 text-sm text-gray-500" role="status">评论加载中…</div>
              <div v-else-if="commentsError" class="p-6 text-sm text-gray-500" role="alert">
                评论加载失败 <button class="underline" type="button" @click="loadInitialComments">重试</button>
              </div>
              <CommentList
                v-else
                :comments="comments"
                :total="commentTotal ?? '—'"
                :has-more="hasMoreComments"
                :loading-more="isLoadingMoreComments"
                :more-error="moreCommentsError"
                :more-count="moreCommentsCount"
                @load-more="loadMoreComments"
                @reply="onReplyClick"
                @click-comment="focusComment"
                @expand-replies="handleExpandReplies"
                @like="handleCommentLike"
                @delete="handleDeleteComment"
              />
              <div v-if="moreCommentsError && !commentsLoading && !commentsError" role="alert" class="px-6 pb-4 text-sm text-gray-500">
                更多评论加载失败 <button type="button" class="underline" @click="loadMoreComments(true)">重试</button>
              </div>
            </div>
            

            <!-- 底部互动区 -->
            <div class="border-t border-gray-100 p-[16px]">
              <div class="flex flex-col text-gray-500 text-[15px]">
                <!-- 评论输入区域 -->
                <div class="flex items-center gap-2">
                  <!-- 登录提示/评论输入框 -->
                  <div 
                    v-if="!isLoggedIn"
                    class="content-input grow cursor-pointer"
                    @click="focusComment"
                  >
                    <svg class="w-4 h-4 text-gray-400" viewBox="0 0 24 24" fill="none" stroke="currentColor">
                      <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2M12 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8z" stroke-width="2"/>
                    </svg>
                    <span class="text-gray-500 text-sm ml-2">登录后评论</span>
                  </div>
                  
                  <!-- 已登录状态显示评论输入框 -->
                  <div 
                    v-else
                    class="flex flex-col"
                    :class="{ 'w-full': isInputFocused }"
                  >
                    <!-- 回复提示 - 仅在回复时显示 -->
                    <div 
                      v-if="replyTo"
                      class="flex flex-col px-3 py-2 text-[14px] w-full"
                    >
                      <div class="flex items-center reply">
                        回复
                        <span class="text-[#333] mx-1">{{ replyTo.nickname }}</span>
                      </div>
                      <div class="reply-content line-clamp-1">
                        {{ replyTo.content }} <span v-if="replyTo.imageUrl">[图片]</span>
                      </div>
                    </div>

                    <!-- 评论输入框 -->
                    <div 
                      class="gap-2 rounded-full flex items-center content-input"
                      :class="{
                        'w-full px-[16px]!': isInputFocused,
                        'w-[200px]': !isInputFocused
                      }"
                      @click="focusComment"
                    >
                      <!-- 头像只在未聚焦时显示 -->
                      <img 
                        v-if="!isInputFocused"
                        :src="userStore.profile.avatar || defaultAvatar" @error="useDefaultAvatar" alt="我的头像"
                        class="w-[24px] h-[24px] rounded-full shrink-0"
                      />
                      
                      <!-- 未聚焦时显示默认文本 -->
                      <div 
                        v-if="!isInputFocused && !commentContent" 
                        class="text-gray-500 text-sm ml-2 whitespace-nowrap overflow-hidden text-ellipsis"
                      >
                        说点什么...
                      </div>
                      
                      <!-- 输入框 -->
                      <input 
                        type="text" 
                        placeholder="说点什么..." 
                        v-model="commentContent"
                        :disabled="isPublishingComment"
                        class="flex-1 bg-transparent focus:outline-none min-w-0 text-[#333]"
                        :class="{
                          'ml-2 text-sm': !isInputFocused,
                          'text-[16px]': isInputFocused
                        }"
                        @blur="onInputBlur"
                        ref="commentInput"
                      />
                      
                    </div>
                    
                    <div class="mt-[8px]" v-if="commentImage">
                      <img :src="commentImage" 
                      class="w-[60px]! rounded-lg object-cover cursor-zoom-in hover:brightness-80 ml-2">
                    </div>
                  </div>

                  <!-- 互动数据 - 仅在输入框未聚焦时显示 -->
                  <div v-if="!isInputFocused" class="flex items-center gap-4 ml-auto text-[#333]">
                    <button type="button" class="flex items-center gap-1 cursor-pointer hover:text-gray-800 disabled:opacity-50" :aria-label="isNoteLiked ? '取消点赞笔记' : '点赞笔记'" :aria-pressed="isNoteLiked" :disabled="isLoggedIn && (!interactionAvailable || interactionBusy)" :title="isLoggedIn && !interactionAvailable ? '互动状态暂不可用' : undefined" @click="handleNoteLike">
                      <svg 
                        class="w-[20px] h-[20px] transition-all duration-200"
                        :class="[isNoteLiked ? 'animate-like' : 'animate-unlike']"
                        viewBox="0 0 24 24" 
                        :fill="isNoteLiked ? '#ff2442' : 'none'" 
                        :stroke="isNoteLiked ? '#ff2442' : 'currentColor'"
                      >
                        <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z" stroke-width="2"/>
                      </svg>
                      <span>{{ currNote.likeTotalAvailable === false || currNote.likeTotal == null ? '—' : currNote.likeTotal }}</span>
                    </button>
                    <button type="button" class="flex items-center gap-1 cursor-pointer hover:text-gray-800 disabled:opacity-50" :aria-label="isNoteCollected ? '取消收藏笔记' : '收藏笔记'" :aria-pressed="isNoteCollected" :disabled="isLoggedIn && (!interactionAvailable || interactionBusy)" :title="isLoggedIn && !interactionAvailable ? '互动状态暂不可用' : undefined" @click="handleNoteCollect">
                      <svg 
                        class="w-[20px] h-[20px] transition-all duration-200"
                        :class="[isNoteCollected ? 'animate-like' : 'animate-unlike']"
                        viewBox="0 0 24 24" 
                        :fill="isNoteCollected ? '#FF8C00' : 'none'" 
                        :stroke="isNoteCollected ? '#FF8C00' : 'currentColor'"
                      >
                        <path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                      </svg>
                      <span>{{ currNote.collectTotalAvailable === false || currNote.collectTotal == null ? '—' : currNote.collectTotal }}</span>
                    </button>
                    <button type="button" aria-label="发表评论"
                      class="flex items-center gap-1 cursor-pointer hover:text-gray-800"
                      @click="focusComment"
                    >
                      <svg class="w-[20px] h-[20px]" viewBox="0 0 24 24" fill="none" stroke="currentColor">
                        <path d="M21 11.5a8.38 8.38 0 0 1-.9 3.8 8.5 8.5 0 0 1-7.6 4.7 8.38 8.38 0 0 1-3.8-.9L3 21l1.9-5.7a8.38 8.38 0 0 1-.9-3.8 8.5 8.5 0 0 1 4.7-7.6 8.38 8.38 0 0 1 3.8-.9h.5a8.48 8.48 0 0 1 8 8v.5z" stroke-width="2"/>
                      </svg>
                      <span class="text-sm">{{ currNote.commentTotalAvailable === false || currNote.commentTotal == null ? '—' : currNote.commentTotal }}</span>
                    </button>
                  </div>
                </div>

                <!-- 输入框聚焦时显示的底部工具栏 -->
                <div v-if="isInputFocused" class="flex items-center justify-between mt-3">
                  <div class="flex items-center gap-1">
                    <div class="relative">
                      <button 
                        class="p-[10px] hover:text-[#333] hover:bg-gray-100 rounded-full"
                        @click="toggleEmojiPicker"
                        ref="emojiButtonRef"
                      >
                        <svg class="w-5 h-5" viewBox="0 0 24 24" fill="none">
                          <path d="M12 22C17.5228 22 22 17.5228 22 12C22 6.47715 17.5228 2 12 2C6.47715 2 2 6.47715 2 12C2 17.5228 6.47715 22 12 22Z" stroke="currentColor" stroke-width="2"/>
                          <path d="M8.5 15C8.5 15 9.8125 17 12 17C14.1875 17 15.5 15 15.5 15" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
                          <path d="M9 10H9.01" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
                          <path d="M15 10H15.01" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
                        </svg>
                      </button>
                      
                      <!-- 表情选择弹出框 -->
                      <div 
                        v-if="showEmojiPicker" 
                        class="emoji-picker border border-gray-200"
                        ref="emojiPickerRef"
                      >
                        <div class="emoji-grid">
                          <button 
                            v-for="emoji in emojiList" 
                            :key="emoji"
                            class="emoji-item"
                            @click="insertEmoji(emoji)"
                          >
                            {{ emoji }}
                          </button>
                        </div>
                      </div>
                    </div>
                    
                    <!-- 上传图片按钮 -->
                    <div class="relative">
                      <button 
                        class="p-[10px] hover:text-[#333] hover:bg-gray-100 rounded-full"
                        @click="triggerFileUpload"
                      >
                        <svg class="w-5 h-5" viewBox="0 0 24 24" fill="none" stroke="currentColor">
                          <path d="M19 3H5C3.89543 3 3 3.89543 3 5V19C3 20.1046 3.89543 21 5 21H19C20.1046 21 21 20.1046 21 19V5C21 3.89543 20.1046 3 19 3Z" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                          <path d="M8.5 10C9.32843 10 10 9.32843 10 8.5C10 7.67157 9.32843 7 8.5 7C7.67157 7 7 7.67157 7 8.5C7 9.32843 7.67157 10 8.5 10Z" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                          <path d="M21 15L16 10L5 21" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                        </svg>
                      </button>
                      <input 
                        type="file" 
                        ref="fileInputRef" 
                        class="hidden" 
                        accept="image/*"
                        @change="handleFileChange"
                      />
                    </div>
                  </div>
                  <div class="flex items-center gap-2">
                    <!-- 发送按钮 -->
                    <button 
                      type="button"
                      class="w-[64px] h-[40px] text-[16px] text-white bg-[#ff2442] 
                      rounded-full font-bold cursor-pointer"
                      :disabled="isPublishingComment || isUploadingCommentImage || (!commentContent.trim() && !commentImage)"
                      :class="{'opacity-50': isPublishingComment || isUploadingCommentImage || (!commentContent.trim() && !commentImage)}"
                      @click="handlePublishComment"
                    >
                      {{ isPublishingComment ? '发送中…' : '发送' }}
                    </button>
                    <button type="button" :disabled="isPublishingComment" class="border border-gray-200 w-[64px] h-[40px] text-[16px] 
                    font-bold text-gray-600 hover:text-gray-800 hover:bg-gray-100 
                    rounded-full cursor-pointer" @click="onCancel">
                      取消
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </div>
          </template>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import { defaultAvatar, useDefaultAvatar } from '@/utils/avatar'
import { ref, computed, nextTick, watch, onBeforeUnmount, inject, onMounted, type Ref, type PropType } from 'vue'
import type { NoteCardData, NoteDetailData, CommentItemData } from '@/types/api'
import gsap from 'gsap'
import CommentList from './CommentList.vue'
import ImageCarousel from '@/components/common/ImageCarousel.vue'
import VideoPlayer from '@/components/common/VideoPlayer.vue'
import { getNoteDetail, deleteNote, likeNote, unlikeNote, collectNote, uncollectNote, isLikedAndCollectedData } from '@/api/note' // 获取笔记详情的API
import { getCommentList, publishComment, getChildCommentList, deleteComment, likeComment, unlikeComment } from '@/api/comment'
import { findFollowState, followUser, unfollowUser } from '@/api/relation'
import { useUserStore } from '@/stores/user'
import { message } from '@/utils/message'
import { uploadFile } from '@/api/file' // 导入文件上传API

const userStore = useUserStore()

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  note: {
    type: Object as PropType<NoteCardData>,
    default: () => ({ id: '' })
  }
})

const currNote = ref<Partial<NoteDetailData>>({})
const currNoteId = ref('')
const detailLoading = ref(false)
const detailError = ref(false)
let detailSequence = 0
let followSequence = 0
let interactionSequence = 0
let commentSequence = 0
const interactionAvailable = ref(false)
const followState = ref<boolean | null>(null)
const followBusy = ref(false)
const interactionBusy = ref(false)
const commentBusy = new Set<string>()

const emit = defineEmits(['update:visible', 'deleted'])
const showNoteMenu = ref(false)
const deletingNote = ref(false)
const deletingComments = new Set<string>()

const handleDeleteNote = async () => {
  const id = currNoteId.value
  if (!id || deletingNote.value || !window.confirm('确定删除这篇笔记吗？删除后将无法查看。')) return
  deletingNote.value = true
  try {
    const result = await deleteNote(id)
    if (!result.success) { message.show(result.message || '删除笔记失败'); return }
    showNoteMenu.value = false
    emit('deleted', id)
    onClose()
    message.show('笔记已删除')
  } catch { message.show('删除笔记失败，请重试') }
  finally { deletingNote.value = false }
}

const handleDeleteComment = async (comment: CommentItemData) => {
  const id = comment.commentId
  if (!id || deletingComments.has(id) || !window.confirm('确定删除这条评论或回复吗？关联的回复也可能被删除。')) return
  const noteId = currNoteId.value
  deletingComments.add(id)
  try {
    const result = await deleteComment(id)
    if (!result.success) { message.show(result.message || '删除失败'); return }
    if (noteId !== currNoteId.value || !props.visible) return
    commentSequence++
    commentsLoading.value = false
    isLoadingMoreComments.value = false
    loadingChildComments.clear()
    comments.value = comments.value.filter(item => item.commentId !== id)
    for (const item of comments.value) {
      if (item.childComments?.some(child => child.commentId === id)) {
        item.childComments = item.childComments.filter(child => child.commentId !== id)
        item.childCommentTotal = Math.max(0, (item.childCommentTotal ?? 1) - 1)
        item.hasMoreChildComments = true
      }
    }
    if (replyTo.value?.commentId === id) replyTo.value = null
    commentTotal.value = null
    currNote.value.commentTotalAvailable = false
    message.show('请刷新查看回复')
  } catch { message.show('删除失败，请重试') }
  finally { deletingComments.delete(id) }
}

// DOM 引用
const modalRef = ref<HTMLElement | null>(null)
const closeButtonRef = ref<HTMLButtonElement | null>(null)
let previousFocus: HTMLElement | null = null
const authorInfoRef = ref<HTMLElement | null>(null)
const contentRef = ref<HTMLElement | null>(null)
const scrollContainerRef = ref<HTMLElement | null>(null)

// 动画相关状态
let animation: gsap.core.Tween | null = null

// 在组件卸载前清除所有动画
onBeforeUnmount(() => {
  detailSequence++
  followSequence++
  interactionSequence++
  commentSequence++
  commentImageGeneration++
  if (animation) {
    animation.kill()
    animation = null
  }
})

// 关闭模态框
const onClose = () => {
  showNoteMenu.value = false
  emit('update:visible', false)
}

// 动画相关方法
const onBeforeEnter = (_el: Element) => {
  if (modalRef.value) {
    gsap.set(modalRef.value, {
      scale: 0.8,
      opacity: 0
    })
  }
}

const onEnter = (_el: Element, done: () => void) => {
  nextTick(() => {
    if (modalRef.value) {
      animation = gsap.to(modalRef.value, {
        scale: 1,
        opacity: 1,
        duration: 0.3,
        ease: 'back.out(1.7)',
        onComplete: done
      })
    } else {
      done()
    }
  })
}

const onLeave = (_el: Element, done: () => void) => {
  if (modalRef.value) {
    animation = gsap.to(modalRef.value, {
      scale: 0.8,
      opacity: 0,
      duration: 0.2,
      ease: 'power1.in',
      onComplete: done
    })
  } else {
    done()
  }
}

const isScrolled = ref(false)

const handleScroll = (e: Event) => {
  // 设置 isScrolled 状态
  const container = e.target as HTMLElement
  isScrolled.value = container.scrollTop > 0
  
  const scrollTop = container.scrollTop
  const scrollHeight = container.scrollHeight
  const clientHeight = container.clientHeight

  // 当滚动到距离底部 50px 时触发加载
  if (scrollHeight - scrollTop - clientHeight < 50) {
    loadMoreComments()
  }
}

const currCommentPageNo = ref(1)
const totalCommentPage = ref(0)
const comments = ref<CommentItemData[]>([])
const commentsLoading = ref(false)
const commentsError = ref(false)
const moreCommentsError = ref(false)


const commentTotal = ref<number | null>(0)
const hasMoreComments = computed(() => currCommentPageNo.value < totalCommentPage.value)
const moreCommentsCount = ref(7)

// 一级评论包含第一条回复，后续分页从第二条开始。
const normalizeComment = (comment: CommentItemData): CommentItemData => ({
  ...comment,
  childComments: comment.firstReplyComment ? [comment.firstReplyComment] : [],
})

// 加载更多评论
const isLoadingMoreComments = ref(false)

const loadMoreComments = (retry = false) => {
  if (currCommentPageNo.value >= totalCommentPage.value || isLoadingMoreComments.value || (moreCommentsError.value && !retry)) return
  
  isLoadingMoreComments.value = true
  moreCommentsError.value = false
  const nextPage = currCommentPageNo.value + 1
  
  const requestId = commentSequence
  getCommentList(currNoteId.value, nextPage).then(res => {
    if (requestId !== commentSequence) return
    if (res.success) {
      // 过滤掉可能重复的评论
      const existingCommentIds = new Set(comments.value.map(c => c.commentId))
      const newComments = (res.data || []).filter(c => {
        if (existingCommentIds.has(c.commentId)) return false
        existingCommentIds.add(c.commentId)
        return true
      }).map(normalizeComment)
      
      // 将新评论追加到现有列表
      comments.value = [...comments.value, ...newComments]
      currCommentPageNo.value = res.pageNo
      commentTotal.value = res.totalCount
      totalCommentPage.value = res.totalPage
    } else {
      moreCommentsError.value = true
    }
  }).catch(error => {
    if (requestId === commentSequence) moreCommentsError.value = true
    console.error('加载评论失败:')
  }).finally(() => {
    if (requestId === commentSequence) isLoadingMoreComments.value = false
  })
}

// 登录状态控制
const isLoggedIn = computed(() => !!userStore.token)

// 评论输入框状态
const isInputFocused = ref(false)
const commentInput = ref<HTMLInputElement | null>(null)
const commentContent = ref('')

// 添加回复对象状态
const replyTo = ref<CommentItemData | null>(null)

const showLoginModal = inject<Ref<boolean>>('showLoginModal', ref(false))

// 聚焦评论输入框
const focusComment = () => {
  if (!isLoggedIn.value) {
    showLoginModal.value = true
    return
  }
  isInputFocused.value = true
  nextTick(() => {
    if (commentInput.value) {
      commentInput.value.focus()
    }
  })
}

// 修改回复点击处理
const onReplyClick = (comment: CommentItemData) => {
  if (!isLoggedIn.value) { showLoginModal.value = true; return }
  if (isPublishingComment.value) return
  replyTo.value = comment
  isInputFocused.value = true
  nextTick(() => commentInput.value?.focus())
}

// 清除回复
const clearReply = () => {
  replyTo.value = null
}

// 输入框失焦处理
const onInputBlur = (e: FocusEvent) => {
  // 只有点击取消按钮时才会关闭输入框
  if (e.relatedTarget instanceof HTMLElement && e.relatedTarget.textContent === '取消') {
    isInputFocused.value = false
  }
}

// 监听评论内容
const isCommentEmpty = computed(() => !commentContent.value.trim())

// 评论图片相关
const commentImage = ref('')
const isUploadingCommentImage = ref(false)
let commentImageGeneration = 0
const fileInputRef = ref<HTMLInputElement | null>(null)

// 修改取消评论处理
const onCancel = () => {
  if (isPublishingComment.value) return
  commentImageGeneration++
  isUploadingCommentImage.value = false
  isInputFocused.value = false
  commentContent.value = ''
  commentImage.value = '' // 清除已上传的图片
  replyTo.value = null  // 清除回复对象
  if (commentInput.value) {
    commentInput.value.blur()
  }
}

const refreshInteractionState = async () => {
  const id = currNoteId.value
  const requestId = ++interactionSequence
  interactionAvailable.value = false
  if (!id) return
  try {
    const res = await isLikedAndCollectedData(id)
    if (requestId !== interactionSequence) return
    if (res.success && res.data) {
      isNoteLiked.value = res.data.isLiked
      isNoteCollected.value = res.data.isCollected
      interactionAvailable.value = true
    }
  } catch (error) {
    if (requestId === interactionSequence) console.error('加载互动状态失败:')
  }
}
watch(() => userStore.token, () => {
  if (props.visible) {
    refreshInteractionState()
    commentImageGeneration++
    isUploadingCommentImage.value = false
    commentImage.value = ''
    commentSequence++
    comments.value = []
    commentsLoading.value = false
    isLoadingMoreComments.value = false
    currCommentPageNo.value = 1
    totalCommentPage.value = 0
    replyTo.value = null
    void loadInitialComments()
  }
})

const loadInitialComments = async () => {
  const noteId = currNoteId.value
  const requestId = commentSequence
  if (!noteId || commentsLoading.value) return
  commentsLoading.value = true
  commentsError.value = false
  moreCommentsError.value = false
  try {
    const res = await getCommentList(noteId, 1)
    if (requestId !== commentSequence) return
    if (!res.success) { commentsError.value = true; return }
    const seen = new Set<string>()
    comments.value = (res.data || []).filter(comment => {
      if (seen.has(comment.commentId)) return false
      seen.add(comment.commentId)
      return true
    }).map(normalizeComment)
    commentTotal.value = res.totalCount
    currCommentPageNo.value = res.pageNo
    totalCommentPage.value = res.totalPage
  } catch (error) {
    if (requestId === commentSequence) {
      commentsError.value = true
      console.error('加载评论失败:')
    }
  } finally {
    if (requestId === commentSequence) commentsLoading.value = false
  }
}

const refreshComments = () => {
  if (commentsLoading.value) return
  commentSequence++
  isLoadingMoreComments.value = false
  loadingChildComments.clear()
  void loadInitialComments()
}

const loadNoteDetail = async () => {
  const noteId = currNoteId.value
  const requestId = detailSequence
  if (!noteId || detailLoading.value) return
  detailLoading.value = true
  detailError.value = false
  try {
    const response = await getNoteDetail(noteId)
    if (requestId !== detailSequence) return
    if (!response.success || !response.data) {
      detailError.value = true
      return
    }
    currNote.value = {
      ...response.data,
      topics: response.data.topicName ? [{ name: response.data.topicName }] : []
    }
  } catch (error) {
    if (requestId === detailSequence) {
      detailError.value = true
      console.error('加载详情失败:')
    }
  } finally {
    if (requestId === detailSequence) detailLoading.value = false
  }
}

// 监听模态框可见性变化
watch(() => props.visible, (newVisible) => {
  ++detailSequence
  ++commentSequence
  interactionAvailable.value = false
  followState.value = null
  isLoadingMoreComments.value = false
  if (newVisible && props.note && props.note.id) {
    previousFocus = document.activeElement instanceof HTMLElement ? document.activeElement : null
    nextTick(() => closeButtonRef.value?.focus())
    // 模态框打开时加载数据
    currNoteId.value = props.note.id
    currNote.value = { id: props.note.id, type: props.note.type ?? undefined, title: props.note.title ?? undefined, videoUri: props.note.videoUri, creatorId: props.note.creatorId, avatar: props.note.avatar }
    
    // 加载笔记详情
    void loadNoteDetail()
    
    // 加载评论列表第一页
    loadInitialComments()

    refreshInteractionState()

  } else {
    // 模态框关闭时重置数据
    if (previousFocus?.isConnected) previousFocus.focus()
    previousFocus = null
    followSequence++
    interactionSequence++
    detailLoading.value = false
    detailError.value = false
    commentImageGeneration++
    isUploadingCommentImage.value = false
    currNote.value = {}
    currNoteId.value = ''
    comments.value = []
    commentsLoading.value = false
    commentsError.value = false
    moreCommentsError.value = false
    commentTotal.value = 0
    currCommentPageNo.value = 1
    totalCommentPage.value = 1
    commentContent.value = ''
    commentImage.value = ''
    replyTo.value = null
    isInputFocused.value = false
    isScrolled.value = false
    isNoteLiked.value = false
    isNoteCollected.value = false
  }
})

// 表情选择相关
const showEmojiPicker = ref(false)
const emojiButtonRef = ref<HTMLElement | null>(null)
const emojiPickerRef = ref<HTMLElement | null>(null)

// 常用表情列表
const emojiList = [
  '😀', '😃', '😄', '😁', '😆', '😅', '😂', '🤣', '😊', '😇',
  '🙂', '🙃', '😉', '😌', '😍', '🥰', '😘', '😗', '😙', '😚',
  '😋', '😛', '😝', '😜', '🤪', '🤨', '🧐', '🤓', '😎', '🤩',
  '🥳', '😏', '😒', '😞', '😔', '😟', '😕', '🙁', '☹️', '😣',
  '😖', '😫', '😩', '🥺', '😢', '😭', '😤', '😠', '😡', '🤬',
  '🤯', '😳', '🥵', '🥶', '😱', '😨', '😰', '😥', '😓', '🤗',
  '🤔', '🤭', '🤫', '🤥', '😶', '😐', '😑', '😬', '🙄', '😯',
  '😦', '😧', '😮', '😲', '🥱', '😴', '🤤', '😪', '😵', '🤐',
  '🥴', '🤢', '🤮', '🤧', '😷', '🤒', '🤕', '🤑', '🤠', '😈',
  '👿', '👹', '👺', '🤡', '💩', '👻', '💀', '☠️', '👽', '👾'
]

// 切换表情选择器显示状态
const toggleEmojiPicker = () => {
  showEmojiPicker.value = !showEmojiPicker.value
}

// 插入表情到评论内容
const insertEmoji = (emoji: string) => {
  if (isPublishingComment.value) return
  commentContent.value += emoji
  // 选中表情后隐藏表情选择器
  showEmojiPicker.value = false
}

// 点击外部关闭表情选择器
const handleClickOutside = (event: MouseEvent) => {
  if (!(event.target instanceof Node)) return
  if (
    showEmojiPicker.value && 
    emojiPickerRef.value && 
    !emojiPickerRef.value.contains(event.target) &&
    !emojiButtonRef.value?.contains(event.target)
  ) {
    showEmojiPicker.value = false
  }
}

// 组件挂载时添加点击外部关闭事件
const handleDialogKeydown = (event: KeyboardEvent) => {
  if (!props.visible || showLoginModal.value) return
  if (document.querySelector('.fullscreen-glass')) return
  if (event.key === 'Tab' && modalRef.value) {
    const focusable = Array.from(modalRef.value.querySelectorAll<HTMLElement>(
      'a[href], button:not([disabled]), input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])'
    )).filter(element => element.getClientRects().length > 0)
    if (!focusable.length) return
    const first = focusable[0]!
    const last = focusable[focusable.length - 1]!
    const current = document.activeElement
    if (!modalRef.value.contains(current) || (event.shiftKey && current === first) || (!event.shiftKey && current === last)) {
      event.preventDefault()
      ;(event.shiftKey ? last : first).focus()
    }
    return
  }
  if (event.key !== 'Escape') return
  if (showEmojiPicker.value) {
    event.preventDefault()
    showEmojiPicker.value = false
    return
  }
  event.preventDefault()
  onClose()
}
onMounted(() => {
  document.addEventListener('click', handleClickOutside)
  document.addEventListener('keydown', handleDialogKeydown)
})

// 组件卸载时移除事件监听
onBeforeUnmount(() => {
  document.removeEventListener('click', handleClickOutside)
  document.removeEventListener('keydown', handleDialogKeydown)
})



// 触发文件选择
const triggerFileUpload = () => {
  if (isUploadingCommentImage.value || isPublishingComment.value) return
  fileInputRef.value?.click()
}

// 处理文件选择
const handleFileChange = async (event: Event) => {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file || isUploadingCommentImage.value || isPublishingComment.value) return
  if (!file.type.startsWith('image/')) { message.show('请选择图片文件'); return }
  if (file.size > 5 * 1024 * 1024) { message.show('图片大小不能超过5MB'); return }
  commentImage.value = ''
  const formData = new FormData()
  formData.append('file', file)
  const requestId = detailSequence
  const generation = ++commentImageGeneration
  isUploadingCommentImage.value = true
  message.show('图片上传中...')
  try {
    const res = await uploadFile(formData)
    if (requestId !== detailSequence || generation !== commentImageGeneration) return
    if (res.success && res.data && res.data.length > 108) message.show('图片地址过长，暂时无法用于评论')
    else if (res.success && res.data) commentImage.value = res.data
    else message.show(res.message || '图片上传失败')
  } catch (error) {
    if (requestId === detailSequence && generation === commentImageGeneration) {
      console.error('图片上传错误:')
    }
  } finally {
    if (generation === commentImageGeneration) isUploadingCommentImage.value = false
  }
}

// 移除已上传的图片
const removeImage = () => {
  if (isPublishingComment.value) return
  commentImageGeneration++
  isUploadingCommentImage.value = false
  commentImage.value = ''
}

// 评论发布是异步受理，不把尚未落库的评论显示为已保存。
const isPublishingComment = ref(false)
const handlePublishComment = async () => {
  if (!isLoggedIn.value) { showLoginModal.value = true; return }
  if (isPublishingComment.value || !currNoteId.value) return
  if (isUploadingCommentImage.value) { message.show('图片上传中，请稍候'); return }
  if (!commentContent.value.trim() && !commentImage.value) {
    message.show('请输入评论内容或上传图片')
    return
  }
  const requestId = detailSequence
  const requestSession = userStore.token
  isPublishingComment.value = true
  try {
    const res = await publishComment({
      noteId: currNoteId.value,
      content: commentContent.value,
      replyCommentId: replyTo.value?.commentId,
      imageUrl: commentImage.value
    })
    if (requestId !== detailSequence || requestSession !== userStore.token) return
    if (!res.success) { message.show(res.message || '评论提交失败'); return }
    message.show('请刷新查看回复')
    commentContent.value = ''
    commentImage.value = ''
    replyTo.value = null
    isInputFocused.value = false
  } catch (error) {
    console.error('评论提交失败:')
  } finally {
    isPublishingComment.value = false
  }
}

// 加载二级评论
const loadingChildComments = new Set<string>()
const loadChildComments = (parentComment: CommentItemData) => {
  if (!parentComment || !parentComment.commentId || loadingChildComments.has(parentComment.commentId)) return
  loadingChildComments.add(parentComment.commentId)
  parentComment.childCommentsLoading = true
  parentComment.childCommentsError = false
  
  // 确保父评论有 childComments 数组和分页信息
  if (!parentComment.childComments) {
    parentComment.childComments = []
  }
  if (!parentComment.currChildCommentPage) {
    parentComment.currChildCommentPage = 0
  }
  
  const requestId = commentSequence
  getChildCommentList(parentComment.commentId, parentComment.currChildCommentPage + 1, !parentComment.firstReplyComment).then(res => {
    if (requestId !== commentSequence) return
    if (res.success) {
      parentComment.childComments ??= []
      const existingIds = new Set(parentComment.childComments.map(item => item.commentId))
      parentComment.childComments.push(...(res.data || []).filter(item => {
        if (existingIds.has(item.commentId)) return false
        existingIds.add(item.commentId)
        return true
      }))
      parentComment.childCommentTotal = res.totalCount
      parentComment.currChildCommentPage = res.pageNo
      parentComment.totalChildCommentPage = res.totalPage
      parentComment.hasMoreChildComments = res.pageNo < res.totalPage
    } else {
      parentComment.childCommentsError = true
    }
  }).catch(error => {
    if (requestId === commentSequence) parentComment.childCommentsError = true
    console.error('加载回复失败:')
  }).finally(() => {
    if (requestId === commentSequence) {
      parentComment.childCommentsLoading = false
      loadingChildComments.delete(parentComment.commentId)
    }
  })
}

// 处理展开回复事件
const handleExpandReplies = (comment: CommentItemData) => {
  loadChildComments(comment)
}

const isNoteLiked = ref(false)
const isNoteCollected = ref(false)
const handleNoteLike = async () => {
  if (!isLoggedIn.value) { showLoginModal.value = true; return }
  if (!interactionAvailable.value || interactionBusy.value || !currNoteId.value) return
  const requestId = detailSequence
  const requestSession = userStore.token
  const requestInteraction = interactionSequence
  interactionBusy.value = true
  try {
    const res = await (isNoteLiked.value ? unlikeNote(currNoteId.value) : likeNote(currNoteId.value))
    if (requestId !== detailSequence || requestSession !== userStore.token || requestInteraction !== interactionSequence) return
    if (!res.success) { message.show(res.message || '操作失败，请重试'); return }
    isNoteLiked.value = !isNoteLiked.value
    if (currNote.value.likeTotal != null) currNote.value.likeTotal = Math.max(0, currNote.value.likeTotal + (isNoteLiked.value ? 1 : -1))
    window.dispatchEvent(new CustomEvent('shrimp:detail-like-changed', {
      detail: { noteId: currNoteId.value, liked: isNoteLiked.value }
    }))
  } catch (error) { console.error('笔记点赞失败:') }
  finally { interactionBusy.value = false }
}
const handleNoteCollect = async () => {
  if (!isLoggedIn.value) { showLoginModal.value = true; return }
  if (!interactionAvailable.value || interactionBusy.value || !currNoteId.value) return
  const requestId = detailSequence
  const requestSession = userStore.token
  const requestInteraction = interactionSequence
  interactionBusy.value = true
  try {
    const res = await (isNoteCollected.value ? uncollectNote(currNoteId.value) : collectNote(currNoteId.value))
    if (requestId !== detailSequence || requestSession !== userStore.token || requestInteraction !== interactionSequence) return
    if (!res.success) { message.show(res.message || '操作失败，请重试'); return }
    isNoteCollected.value = !isNoteCollected.value
    if (currNote.value.collectTotal != null) currNote.value.collectTotal = Math.max(0, currNote.value.collectTotal + (isNoteCollected.value ? 1 : -1))
    window.dispatchEvent(new CustomEvent('shrimp:detail-collect-changed', {
      detail: { noteId: currNoteId.value, collected: isNoteCollected.value }
    }))
  } catch (error) { console.error('笔记收藏失败:') }
  finally { interactionBusy.value = false }
}
const handleCommentLike = async ({ comment, liked }: { comment: CommentItemData; liked: boolean }) => {
  if (!isLoggedIn.value) { showLoginModal.value = true; return }
  if (comment.likeStateAvailable === false || comment.isLiked == null || commentBusy.has(comment.commentId)) return
  commentBusy.add(comment.commentId)
  const requestId = commentSequence
  const requestSession = userStore.token
  try {
    const res = await (liked ? likeComment(comment.commentId) : unlikeComment(comment.commentId))
    if (requestId !== commentSequence || requestSession !== userStore.token) return
    if (!res.success) { message.show(res.message || '操作失败，请重试'); return }
    comment.isLiked = liked
    if (comment.likeTotal != null) comment.likeTotal = Math.max(0, comment.likeTotal + (liked ? 1 : -1))
  } catch (error) { console.error('评论点赞失败:') }
  finally { commentBusy.delete(comment.commentId) }
}
const loadFollowState = async () => {
  const creatorId = currNote.value.creatorId
  const requestId = ++followSequence
  followState.value = null
  if (!isLoggedIn.value || !creatorId || creatorId === userStore.profile.userId) return
  try {
    const res = await findFollowState(creatorId)
    if (requestId !== followSequence) return
    if (res.success && res.data?.available && typeof res.data.followed === 'boolean') followState.value = res.data.followed
  } catch (error) { console.error('获取关注状态失败:') }
}
watch(() => [props.visible, currNote.value.creatorId, userStore.token, userStore.profile.userId], () => {
  if (props.visible) loadFollowState()
})
const handleFollow = async () => {
  if (!isLoggedIn.value) { showLoginModal.value = true; return }
  if (followBusy.value || followState.value === null || !currNote.value.creatorId) return
  const requestId = detailSequence
  const requestSession = userStore.token
  const requestFollow = followSequence
  followBusy.value = true
  try {
    const res = await (followState.value ? unfollowUser(currNote.value.creatorId) : followUser(currNote.value.creatorId))
    if (requestId !== detailSequence || requestSession !== userStore.token || requestFollow !== followSequence) return
    if (!res.success) { message.show(res.message || '操作失败，请重试'); return }
    followState.value = !followState.value
    message.show(followState.value ? '关注成功' : '已取消关注')
  } catch (error) { console.error('关注操作失败:') }
  finally { followBusy.value = false }
}
</script>

<style scoped>
.detail-close {
  position: absolute;
  z-index: 10;
  top: 8px;
  right: 8px;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: rgba(0, 0, 0, .55);
  color: #fff;
  font-size: 24px;
  line-height: 1;
}
@media (max-width: 900px) {
  .note-dialog { width: calc(100vw - 24px); max-width: none; height: min(92vh, 820px); flex-direction: column; }
  .note-media { flex: 0 0 36%; height: 36%; min-height: 0; }
  .note-body { width: 100%; min-width: 0; min-height: 0; flex: 1; }
}

.zoom-move {
  transition: transform 0.3s ease-out;
}

/* 防止图片溢出容器 */
img {
  max-height: 90vh;
  width: auto;
}

.title {
    margin-bottom: 8px;
    font-weight: 600;
    font-size: 18px;
    line-height: 140%;
}

.content-input {
  caret-color: #ff2442;
  margin: 0px;
  height: 40px;  /* 固定高度 */
  background-color: rgba(0, 0, 0, 0.03);
  border: none;
  padding: 0 10px;  /* 调整内边距 */
  border-radius: 20px;
  outline: none;
  display: flex;
  align-items: center;
  white-space: nowrap;  /* 防止换行 */
}

/* 未聚焦状态下隐藏滚动条 */
.content-input:not(:focus-within) {
  overflow: hidden;
}

/* 限制回复内容显示一行 */
.line-clamp-1 {
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
}

.reply {
    color: rgba(51, 51, 51, 0.6);
    font-size: 14px;
}

.reply-content {
    color: rgba(51, 51, 51, 0.8);
    font-size: 14px;
    width: 100%;
    margin-top: 4px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

/* 隐藏输入框的默认 placeholder */
input::placeholder {
    color: transparent;
}

/* 聚焦时显示 placeholder */
input:focus::placeholder {
    color: rgba(51, 51, 51, 0.6);
}

/* 修改图片样式，确保图片适应容器 */
:deep(.carousel-container) {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

:deep(.carousel-image) {
  max-width: 100%;
  max-height: 90vh;
  object-fit: contain;
  object-position: center;
}

/* 表情选择器样式 */
.emoji-picker {
  position: absolute;
  bottom: 45px;
  left: 0;
  max-height: 280px;
  background-color: white;
  border-radius: 8px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  padding: 10px;
  z-index: 100;
  overflow-y: auto;
}

.emoji-grid {
  display: grid;
  grid-template-columns: repeat(10, 1fr);
  gap: 5px;
}

.emoji-item {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 38px;
  height: 38px;
  font-size: 28px;
  cursor: pointer;
  border-radius: 4px;
  transition: background-color 0.2s;
}

.emoji-item:hover {
  background-color: #f5f5f5;
}

/* 点赞动画效果 */
@keyframes like {
  0% {
    transform: scale(1);
  }
  25% {
    transform: scale(0.8);
  }
  50% {
    transform: scale(1.2);
  }
  75% {
    transform: scale(0.95);
  }
  100% {
    transform: scale(1);
  }
}

@keyframes unlike {
  0% {
    transform: scale(1);
  }
  25% {
    transform: scale(0.9);
  }
  50% {
    transform: scale(1.1);
  }
  75% {
    transform: scale(0.95);
  }
  100% {
    transform: scale(1);
  }
}

.animate-like {
  animation: like 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.275);
  transform-origin: center;
}

.animate-unlike {
  animation: unlike 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.275);
  transform-origin: center;
}

/* 防止动画重复播放 */
.animate-like, .animate-unlike {
  animation-fill-mode: forwards;
}

/* 防止动画过程中文字抖动 */
.interactions span {
  min-width: 1.5em;
  display: inline-block;
  text-align: left;
}
</style> 
