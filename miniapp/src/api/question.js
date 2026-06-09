import request from './request'

export const questionApi = {
  async list(params) {
    return request({ url: '/questions/list', data: params })
  },

  async detail(id) {
    return request({ url: `/questions/${id}` })
  },

  async delete(id) {
    return request({ url: `/questions/${id}`, method: 'DELETE' })
  },

  async generate(data) {
    return request({ url: '/questions/generate', method: 'POST', data })
  }
}
