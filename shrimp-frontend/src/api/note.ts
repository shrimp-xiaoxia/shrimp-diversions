import type { EntityId, PublishNoteRequest, NoteCardData, InteractionState, NoteDetailData } from '@/types/api'
import { post, postPage } from '@/axios'

// 接口前缀
const API_PREFIX = '/note'

// 发布笔记
export function publishNote(note: PublishNoteRequest) {
    return post(`${API_PREFIX}/note/publish`, note)
}

// 获取发现页笔记数据
export function getDiscoverNotePageList(channelId: EntityId | 0, pageNo: number) {
    return postPage<NoteCardData>(`${API_PREFIX}/discover/note/list`, {channelId, pageNo})
}

// 获取笔记详情
export function getNoteDetail(id: EntityId) {
    return post<NoteDetailData>(`${API_PREFIX}/note/detail`, {id})
}

export function deleteNote(id: EntityId) {
    return post(`${API_PREFIX}/note/delete`, {id})
}

// 点赞笔记
export function likeNote(noteId: EntityId) {
    return post(`${API_PREFIX}/note/like`, {id: noteId})
}

// 取消点赞笔记
export function unlikeNote(noteId: EntityId) {
    return post(`${API_PREFIX}/note/unlike`, {id: noteId})
}

// 收藏笔记
export function collectNote(noteId: EntityId) {
    return post(`${API_PREFIX}/note/collect`, {id: noteId})
}

// 取消收藏笔记
export function uncollectNote(noteId: EntityId) {
    return post(`${API_PREFIX}/note/uncollect`, {id: noteId})
}

// 是否点赞、收藏
export function isLikedAndCollectedData(noteId: EntityId) {
    return post<InteractionState>(`${API_PREFIX}/note/interaction-state`, {noteId})
}

// 获取个人主页笔记数据
export function getProfileNotePageList(type: number, userId: EntityId, pageNo: number) {
    return postPage<NoteCardData>(`${API_PREFIX}/profile/note/list`, {type, userId, pageNo})
}

