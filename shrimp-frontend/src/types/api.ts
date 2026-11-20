export type EntityId = string

export interface ApiResponse<T> {
  success: boolean
  message?: string | null
  errorCode?: string | null
  data: T | null
}

export interface PageResponse<T> extends ApiResponse<T[]> {
  pageNo: number
  pageSize: number
  totalCount: number
  totalPage: number
}

export interface InteractionState {
  isLiked: boolean
  isCollected: boolean
}

// 根据现有页面入参整理；后端校验规则以接口实现为准。
export interface PublishCommentRequest {
  noteId: EntityId
  content: string
  replyCommentId?: EntityId | null
  imageUrl?: string | null
}
export interface PublishNoteRequest {
  type: number
  title: string
  content: string
  channelId: EntityId
  topicId?: EntityId | null
  imgUris?: string[]
  videoUri?: string
}
export interface UpdateUserProfileRequest {
  avatar?: File | string | null
  nickname?: string
  xiaohashuId?: string
  sex?: string | number | null
  birthday?: string
  introduction?: string
}

export interface NamedItem {
  id: EntityId
  name: string
}

// 用户资料在登录前为空；字段依据 FindProfileRspVO。
export interface UserProfile {
  userId?: EntityId | null
  nickname?: string | null
  avatar?: string | null
  xiaohashuId?: string | null
  sex?: number | null
  birthday?: string | null
  age?: number | null
  introduction?: string | null
  fansTotal?: number | null
  followingTotal?: number | null
  noteTotal?: number | null
  likeAndCollectTotal?: number | null
  fansTotalAvailable?: boolean
  followingTotalAvailable?: boolean
  noteTotalAvailable?: boolean
  likeAndCollectTotalAvailable?: boolean
}

// 发现页返回完整卡片；搜索结果可能缺少媒体类型和作者 ID。
export interface NoteCardData {
  isLiked?: boolean | null
  id: EntityId
  type?: number | null
  title?: string | null
  cover?: string | null
  videoUri?: string | null
  creatorId?: EntityId | null
  nickname?: string | null
  avatar?: string | null
  likeTotal?: number | string | null
  likeTotalAvailable?: boolean
}

// 评论接口字段及页面维护的回复分页状态。
export interface CommentItemData {
  commentId: EntityId
  userId?: EntityId | null
  avatar?: string | null
  nickname?: string | null
  content?: string | null
  replyUserName?: string | null
  imageUrl?: string | null
  createTime?: string | null
  likeTotal?: number | null
  isLiked?: boolean | null
  likeStateAvailable?: boolean
  childCommentTotal?: number | null
  childComments?: CommentItemData[]
  firstReplyComment?: CommentItemData | null
  currChildCommentPage?: number
  totalChildCommentPage?: number
  hasMoreChildComments?: boolean
  childCommentsLoading?: boolean
  childCommentsError?: boolean
  isNewComment?: boolean
}

export interface RelationUser {
  userId: EntityId
  isLiked?: boolean | null
  followStateAvailable?: boolean
  avatar?: string | null
  nickname?: string | null
  nickName?: string | null
  introduction?: string | null
  fansTotal?: number | null
  noteTotal?: number | null
  fansTotalAvailable?: boolean
  noteTotalAvailable?: boolean
}

export interface NoteDetailData {
  id: EntityId
  type: number
  title: string
  content: string
  imgUris?: string[] | null
  videoUri?: string | null
  topicName?: string | null
  topics?: { name: string }[]
  creatorId?: EntityId | null
  creatorName?: string | null
  avatar?: string | null
  updateTime?: string | null
  likeTotal?: number | null
  collectTotal?: number | null
  commentTotal?: number | null
  likeTotalAvailable?: boolean
  collectTotalAvailable?: boolean
  commentTotalAvailable?: boolean
}
