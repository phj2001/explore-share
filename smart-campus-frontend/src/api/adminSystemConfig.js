import request from '@/utils/request.js'

export const getAdminSystemConfigList = () => {
  return request.get('/admin/system-configs')
}

export const updateAdminSystemConfig = (configKey, value) => {
  return request.put(`/admin/system-configs/${encodeURIComponent(configKey)}`, { value })
}
