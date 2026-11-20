import { post } from '@/axios'
import type { NamedItem } from '@/types/api'

// 接口前缀
const API_PREFIX = '/note/channel'

// 获取所有频道
export function getAllChannel() {
    return post<NamedItem[]>(`${API_PREFIX}/list`)
}

