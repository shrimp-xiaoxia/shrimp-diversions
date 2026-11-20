import type { EntityId, RelationUser } from '@/types/api'
import { post, postPage } from '@/axios'

// 接口前缀
const API_PREFIX = '/relation/relation'

// 关注用户
export function followUser(followUserId: EntityId) {
    return post(`${API_PREFIX}/follow`, {followUserId})
}

export function unfollowUser(unfollowUserId: EntityId) {
    return post(`${API_PREFIX}/unfollow`, {unfollowUserId})
}

// 获取关注列表
export function getFollowingList(userId: EntityId, pageNo: number) {
    return postPage<RelationUser>(`${API_PREFIX}/following/list`, {userId, pageNo})
}

// 获取粉丝列表
export function getFansList(userId: EntityId, pageNo: number) {
    return postPage<RelationUser>(`${API_PREFIX}/fans/list`, {userId, pageNo})
}



// 状态来自关系表；异步消息落库期间可能短暂滞后。
export function findFollowState(userId: EntityId) {
  return post<{ followed: boolean | null; available: boolean }>(`${API_PREFIX}/find-follow-state`, { userId })
}
