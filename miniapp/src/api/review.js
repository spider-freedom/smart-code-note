import request from './request'

export const reviewApi = {
  async getTodayTasks() {
    return request({ url: '/reviews/today' })
  },

  async submit(data) {
    return request({ url: '/reviews/submit', method: 'POST', data })
  }
}
