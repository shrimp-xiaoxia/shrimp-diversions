import assert from 'node:assert/strict'
import { test } from 'node:test'
import { mapSearchNotes } from '../src/utils/search-mapping.ts'

test('search mapping retains string IDs, available fields and page metadata', () => {
  const id = '9007199254740993'
  const response = { success: true, data: [{ noteId: id, creatorId: id, title: '笔记', likeCount: '12' }], pageNo: 2, pageSize: 10, totalCount: 11, totalPage: 2 }
  const result = mapSearchNotes(response)
  assert.equal(result.data?.[0].id, id)
  assert.equal(result.data?.[0].creatorId, id)
  assert.equal(result.data?.[0].likeTotal, '12')
  assert.equal(result.pageNo, 2)
  assert.equal(result.totalPage, 2)
})

test('search mapping preserves empty and failed responses without inventing data', () => {
  const base = { pageNo: 1, pageSize: 10, totalCount: 0, totalPage: 0 }
  assert.deepEqual(mapSearchNotes({ ...base, success: true, data: [] }).data, [])
  const failure = mapSearchNotes({ ...base, success: false, errorCode: 'SEARCH-ERROR', data: null })
  assert.equal(failure.success, false)
  assert.equal(failure.errorCode, 'SEARCH-ERROR')
  assert.equal(failure.data, null)
})
