import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { createSSEStream } from './sse'

// Helper: create a ReadableStream from chunks
function createMockStream(chunks: string[]) {
  const encoder = new TextEncoder()
  return new ReadableStream({
    async start(controller) {
      for (const chunk of chunks) {
        controller.enqueue(encoder.encode(chunk))
      }
      controller.close()
    },
  })
}

describe('createSSEStream', () => {
  beforeEach(() => {
    vi.stubGlobal('fetch', vi.fn())
    localStorage.setItem('smart-code-note-token', 'test-token')
  })

  afterEach(() => {
    vi.restoreAllMocks()
    localStorage.clear()
  })

  it('delivers chunk events to onChunk callback', async () => {
    const stream = createMockStream([
      'data: Hello\n\n',
      'data: World\n\n',
    ])
    vi.mocked(fetch).mockResolvedValue({
      ok: true,
      body: stream,
    } as Response)

    const chunks: string[] = []
    await new Promise<void>((resolve) => {
      createSSEStream('/test', {}, (text) => {
        chunks.push(text)
        if (chunks.length === 2) resolve()
      }, vi.fn(), vi.fn())
    })

    expect(chunks).toEqual(['Hello', 'World'])
    expect(fetch).toHaveBeenCalledWith(
      expect.stringContaining('/test?token=test-token'),
      expect.objectContaining({ signal: expect.any(AbortSignal) }),
    )
  })

  it('parses result event as JSON and calls onResult', async () => {
    const stream = createMockStream([
      'event: result\ndata: {"items":[1,2,3]}\n\n',
    ])
    vi.mocked(fetch).mockResolvedValue({ ok: true, body: stream } as Response)

    const result = await new Promise<unknown>((resolve) => {
      createSSEStream('/test', {}, vi.fn(), resolve, vi.fn())
    })

    expect(result).toEqual({ items: [1, 2, 3] })
  })

  it('calls onError when server responds with error event', async () => {
    const stream = createMockStream([
      'event: error\ndata: something went wrong\n\n',
    ])
    vi.mocked(fetch).mockResolvedValue({ ok: true, body: stream } as Response)

    const error = await new Promise<string>((resolve) => {
      createSSEStream('/test', {}, vi.fn(), vi.fn(), resolve)
    })

    expect(error).toBe('something went wrong')
  })

  it('calls onError when HTTP status is not ok', async () => {
    vi.mocked(fetch).mockResolvedValue({
      ok: false,
      status: 500,
      body: null,
    } as Response)

    const error = await new Promise<string>((resolve) => {
      createSSEStream('/test', {}, vi.fn(), vi.fn(), resolve)
    })

    expect(error).toContain('500')
  })

  it('calls onError when result JSON is malformed', async () => {
    const stream = createMockStream([
      'event: result\ndata: not-valid-json\n\n',
    ])
    vi.mocked(fetch).mockResolvedValue({ ok: true, body: stream } as Response)

    const error = await new Promise<string>((resolve) => {
      createSSEStream('/test', {}, vi.fn(), vi.fn(), resolve)
    })

    expect(error).toContain('解析失败')
  })

  it('handles network error', async () => {
    vi.mocked(fetch).mockRejectedValue(new Error('Network error'))

    const error = await new Promise<string>((resolve) => {
      createSSEStream('/test', {}, vi.fn(), vi.fn(), resolve)
    })

    expect(error).toBe('Network error')
  })

  it('does not call onError when request is aborted', async () => {
    const stream = createMockStream(['data: hello\n\n'])
    vi.mocked(fetch).mockResolvedValue({ ok: true, body: stream } as Response)

    const onError = vi.fn()
    const controller = createSSEStream('/test', {}, vi.fn(), vi.fn(), onError)
    controller.abort()

    // Wait for the AbortError to be silently caught
    await new Promise((resolve) => setTimeout(resolve, 50))

    expect(onError).not.toHaveBeenCalled()
  })

  it('appends request params as query string', async () => {
    const stream = createMockStream(['data: ok\n\n'])
    vi.mocked(fetch).mockResolvedValue({ ok: true, body: stream } as Response)

    createSSEStream('/test', { name: 'foo', id: 42 }, vi.fn(), vi.fn(), vi.fn())

    expect(fetch).toHaveBeenCalledWith(
      expect.stringContaining('name=foo'),
      expect.any(Object),
    )
    expect(fetch).toHaveBeenCalledWith(
      expect.stringContaining('id=42'),
      expect.any(Object),
    )
  })
})
