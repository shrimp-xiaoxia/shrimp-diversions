import type { EntityId, PublishCommentRequest, CommentItemData } from '@/types/api'
import { post, postPage } from '@/axios'

// 接口前缀
const API_PREFIX = '/comment/comment'

// 获取笔记一级评论分页数据
export function getCommentList(noteId: EntityId, pageNo: number) {
    return postPage<CommentItemData>(`${API_PREFIX}/list`, {noteId, pageNo})
}

// 发布评论
export function publishComment(comment: PublishCommentRequest) {
    return post(`${API_PREFIX}/publish`, comment)
}

export function deleteComment(commentId: EntityId) {
    return post(`${API_PREFIX}/delete`, {commentId})
}

// 获取笔记二级评论分页数据
export function getChildCommentList(parentCommentId: EntityId, pageNo: number, includeFirst = false) {
    return postPage<CommentItemData>(`${API_PREFIX}/child/list`, {parentCommentId, pageNo, includeFirst})
}

// 点赞评论
export function likeComment(commentId: EntityId) {
    return post(`${API_PREFIX}/like`, {commentId})
}

// 取消点赞评论
export function unlikeComment(commentId: EntityId) {
    return post(`${API_PREFIX}/unlike`, {commentId})
}

