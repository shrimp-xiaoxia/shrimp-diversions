import { post } from '@/axios'
import type { NamedItem } from '@/types/api'

// 接口前缀
const API_PREFIX = '/note/topic'

// 获取所有频道
export function getTopicList(keyword: string) {
    return post<NamedItem[]>(`${API_PREFIX}/list`, {keyword})
}

export function createTopic(name: string) {
    return post(`${API_PREFIX}/create`, {name})
}

