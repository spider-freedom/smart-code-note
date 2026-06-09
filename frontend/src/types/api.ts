export interface ApiResult<T> {
  code: number
  message: string
  data: T
}

export interface PageQuery {
  pageNum: number
  pageSize: number
}

export interface PageResponse<T> {
  records: T[]
  total: number
  pageNum: number
  pageSize: number
}
