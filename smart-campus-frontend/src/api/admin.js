import request from '@/utils/request.js'

export const getAdminOverview = (days = 7) => {
  return request.get('/admin/dashboard/overview', {
    params: { days }
  })
}
