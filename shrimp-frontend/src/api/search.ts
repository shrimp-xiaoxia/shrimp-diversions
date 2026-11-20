import { postPage } from '@/axios'
import type { EntityId, PageResponse } from '@/types/api'
import { mapSearchNotes, type SearchNoteResult, type SearchNoteCard } from '@/utils/search-mapping'
export type { SearchNoteResult, SearchNoteCard } from '@/utils/search-mapping'

export interface SearchUserResult {
  userId: EntityId
  nickname: string
  avatar?: string | null
  isLiked?: boolean | null
  followStateAvailable?: boolean
  xiaohashuId?: string | null
  fansTotal?: number | null
  noteTotal?: number | null
  introduction?: string | null
}

export async function searchNote(keyword: string, type: number | null, sort: number | null, publishTimeRange: number | null, pageNo: number): Promise<PageResponse<SearchNoteCard>> {
  const response: PageResponse<SearchNoteResult> = await postPage<SearchNoteResult>('/search/search/note', { keyword, type, sort, publishTimeRange, pageNo })
  return mapSearchNotes(response)
}

export function searchUser(keyword: string, pageNo: number): Promise<PageResponse<SearchUserResult>> {
  return postPage<SearchUserResult>('/search/search/user', { keyword, pageNo })
}


