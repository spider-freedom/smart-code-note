import request from './request'

export const knowledgeApi = {
  async list(params) {
    return request({ url: '/knowledge/list', data: params })
  },

  async detail(id) {
    return request({ url: `/knowledge/${id}` })
  },

  async update(id, data) {
    return request({ url: `/knowledge/${id}`, method: 'PUT', data })
  },

  async delete(id) {
    return request({ url: `/knowledge/${id}`, method: 'DELETE' })
  },

  async generate(data) {
    return request({ url: '/knowledge/generate', method: 'POST', data })
  }
}
