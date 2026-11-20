import type { EntityId, PageResponse } from '@/types/api'

export interface SearchNoteResult {
  noteId: EntityId
  cover?: string | null
  title: string
  avatar?: string | null
  nickname?: string | null
  likeCount?: string | null
  type?: number | null
  creatorId?: EntityId | null
  videoUri?: string | null
}

export type SearchNoteCard = SearchNoteResult & { id: EntityId; likeTotal?: string | null }

export function mapSearchNotes(response: PageResponse<SearchNoteResult>): PageResponse<SearchNoteCard> {
  return {
    ...response,
    data: response.data?.map(note => ({ ...note, id: note.noteId, likeTotal: note.likeCount })) ?? null
  }
}
