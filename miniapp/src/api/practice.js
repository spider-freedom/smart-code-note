import request from './request'

export const practiceApi = {
  async start(params) {
    return request({ url: '/practice/start', data: params })
  },

  async submit(data) {
    return request({ url: '/practice/submit', method: 'POST', data })
  }
}
