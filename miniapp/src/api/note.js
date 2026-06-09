import request from './request'

export const noteApi = {
  async uploadText(data) {
    return request({ url: '/note/upload-text', method: 'POST', data })
  },

  async list(params) {
    return request({ url: '/note/list', data: params })
  },

  async detail(id) {
    return request({ url: `/note/${id}` })
  },

  async delete(id) {
    return request({ url: `/note/${id}`, method: 'DELETE' })
  },

  async reparse(id) {
    return request({ url: `/note/${id}/parse`, method: 'POST' })
  }
}
