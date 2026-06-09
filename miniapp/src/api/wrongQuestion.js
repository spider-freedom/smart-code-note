import request from './request'

export const wrongQuestionApi = {
  async list(params) {
    return request({ url: '/wrong-questions/list', data: params })
  },

  async retry(id) {
    return request({ url: `/wrong-questions/${id}/retry`, method: 'POST' })
  },

  async markMastered(id) {
    return request({ url: `/wrong-questions/${id}/mastered`, method: 'PUT' })
  }
}
