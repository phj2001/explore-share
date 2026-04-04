import request from '@/utils/request.js'
import { getCachedValue } from '@/utils/requestCache'

const PUBLIC_SYSTEM_CONFIGS_CACHE_KEY = 'system-config:public'

export const getPublicSystemConfigs = (options = {}) => {
  return getCachedValue(
    PUBLIC_SYSTEM_CONFIGS_CACHE_KEY,
    () => request.get('/system-configs/public'),
    {
      ttl: 5 * 60_000,
      forceRefresh: options.forceRefresh
    }
  )
}
