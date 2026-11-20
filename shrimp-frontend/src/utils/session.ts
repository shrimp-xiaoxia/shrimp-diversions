/** 仅让发起时使用当前会话的请求清除当前会话。 */
export function isCurrentSessionRequest(requestAuthorization: unknown, currentToken: string): boolean {
  const requested = requestAuthorization || undefined
  const current = currentToken ? `Bearer ${currentToken}` : undefined
  return requested === current
}
