const cacheStore = new Map()

const cloneValue = (value) => {
  if (value == null) {
    return value
  }

  if (typeof structuredClone === 'function') {
    return structuredClone(value)
  }

  return JSON.parse(JSON.stringify(value))
}

export const getCachedValue = async (key, fetcher, options = {}) => {
  const {
    ttl = 60_000,
    forceRefresh = false
  } = options

  const now = Date.now()
  const existing = cacheStore.get(key)

  if (!forceRefresh && existing?.data && existing.expiresAt > now) {
    return cloneValue(existing.data)
  }

  if (!forceRefresh && existing?.promise) {
    return cloneValue(await existing.promise)
  }

  const requestPromise = Promise.resolve()
    .then(fetcher)
    .then((data) => {
      cacheStore.set(key, {
        data,
        expiresAt: Date.now() + ttl
      })
      return data
    })
    .catch((error) => {
      const latest = cacheStore.get(key)
      if (latest?.promise === requestPromise) {
        cacheStore.delete(key)
      }
      throw error
    })

  cacheStore.set(key, {
    data: existing?.data,
    expiresAt: existing?.expiresAt ?? 0,
    promise: requestPromise
  })

  return cloneValue(await requestPromise)
}

export const clearCachedValue = (keyPrefix = '') => {
  for (const key of cacheStore.keys()) {
    if (!keyPrefix || key.startsWith(keyPrefix)) {
      cacheStore.delete(key)
    }
  }
}
