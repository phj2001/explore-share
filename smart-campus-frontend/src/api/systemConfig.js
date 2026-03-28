import request from '@/utils/request.js'

export const getPublicSystemConfigs = () => {
  return request.get('/system-configs/public')
}
