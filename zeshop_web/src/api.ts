/** 本地演示默认使用已启动的 Java 服务；生产或 Python 模式可通过 VITE_API_BASE_URL 覆盖。 */
export const API_BASE = (import.meta.env.VITE_API_BASE_URL as string | undefined) || 'http://127.0.0.1:8083'

const TOKEN_KEY = 'zeshop-token'

export function readToken(): string {
  return localStorage.getItem(TOKEN_KEY) || ''
}

export function writeToken(value: string): void {
  if (value) localStorage.setItem(TOKEN_KEY, value)
  else localStorage.removeItem(TOKEN_KEY)
}

/** 后端错误体固定为 { error: { code, message } }，这里只把可读文案抛给页面。 */
export async function api<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers: Record<string, string> = { 'Content-Type': 'application/json', ...(init.headers as Record<string, string> | undefined) }
  const token = readToken()
  if (token) headers.Authorization = 'Bearer ' + token
  const response = await fetch(API_BASE + path, { ...init, headers })
  if (!response.ok) {
    let message = '请求失败（' + response.status + '）'
    try {
      const body = await response.json()
      message = body?.error?.message || message
    } catch { /* 非 JSON 错误体保留状态码文案 */ }
    throw new Error(message)
  }
  if (response.status === 204) return undefined as T
  return (await response.json()) as T
}
