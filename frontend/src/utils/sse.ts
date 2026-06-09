import { TOKEN_KEY } from '@/constants'

export function createSSEStream<T>(
  url: string,
  params: Record<string, unknown>,
  onChunk: (text: string) => void,
  onResult: (data: T) => void,
  onError: (message: string) => void,
): AbortController {
  const searchParams = new URLSearchParams()
  Object.entries(params).forEach(([key, value]) => {
    if (value != null) searchParams.append(key, String(value))
  })

  const token = localStorage.getItem(TOKEN_KEY)
  if (token) searchParams.append('token', token)

  const controller = new AbortController()

  fetch(`${url}?${searchParams.toString()}`, { signal: controller.signal })
    .then(async (response) => {
      if (!response.ok) {
        onError(`请求失败 (${response.status})`)
        return
      }
      const reader = response.body?.getReader()
      if (!reader) return

      const decoder = new TextDecoder()
      let buffer = ''
      let currentEvent = ''

      while (true) {
        const { done, value } = await reader.read()
        if (done) break

        buffer += decoder.decode(value, { stream: true })
        const lines = buffer.split('\n')
        buffer = lines.pop() || ''

        for (const line of lines) {
          if (line.startsWith('event:')) {
            currentEvent = line.substring(6).trim()
          } else if (line.startsWith('data:')) {
            const content = line.substring(5).trim()
            if (currentEvent === 'error') {
              onError(content)
            } else if (currentEvent === 'result') {
              try {
                onResult(JSON.parse(content))
              } catch {
                onError('AI 响应解析失败')
              }
            } else {
              onChunk(content)
            }
          }
          if (line.trim() === '') {
            currentEvent = ''
          }
        }
      }
    })
    .catch((err) => {
      if (err.name !== 'AbortError') {
        onError(err.message || '网络请求失败')
      }
    })

  return controller
}
