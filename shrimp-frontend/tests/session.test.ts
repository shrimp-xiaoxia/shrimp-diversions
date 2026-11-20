import assert from 'node:assert/strict'
import { test } from 'node:test'
import { isCurrentSessionRequest } from '../src/utils/session.ts'

test('current session and anonymous requests can be invalidated', () => {
  assert.equal(isCurrentSessionRequest('Bearer current', 'current'), true)
  assert.equal(isCurrentSessionRequest(undefined, ''), true)
})

test('late responses from older or anonymous sessions cannot invalidate a new session', () => {
  assert.equal(isCurrentSessionRequest('Bearer old', 'new'), false)
  assert.equal(isCurrentSessionRequest(undefined, 'new'), false)
  assert.equal(isCurrentSessionRequest('Bearer old', ''), false)
})
