import { defineStore } from 'pinia'
import { computed, ref, shallowRef } from 'vue'
import {
  advancedSearchPOI,
  createPOI,
  deletePOI,
  getAllPOIs,
  getCategories,
  getPOIById,
  getPOIsByCategory,
  getPOIsInBounds,
  searchPOIByName,
  updatePOI
} from '@/api/poi.js'

export const usePOIStore = defineStore('poi', () => {
  const mapPoiList = shallowRef([])
  const searchPoiList = shallowRef([])
  const activeSource = ref('bounds')
  const currentPOI = ref(null)
  const categories = ref([])
  const boundsSummary = ref({
    total: 0,
    limit: 0,
    truncated: false
  })
  const searchSummary = ref({
    total: 0,
    limit: 0,
    truncated: false
  })
  const isLoading = ref(false)
  const error = ref(null)
  const poiEntityCache = new Map()

  const visiblePoiList = computed(() =>
    activeSource.value === 'search' ? searchPoiList.value : mapPoiList.value
  )
  const poiList = visiblePoiList

  const cachePoiRecords = (records) => {
    for (const poi of records || []) {
      if (poi?.id != null) {
        poiEntityCache.set(poi.id, poi)
      }
    }
  }

  const setMapPoiList = (records) => {
    const nextRecords = records || []
    mapPoiList.value = nextRecords
    cachePoiRecords(nextRecords)
    activeSource.value = 'bounds'
  }

  const setSearchPoiList = (records) => {
    const nextRecords = records || []
    searchPoiList.value = nextRecords
    cachePoiRecords(nextRecords)
    activeSource.value = 'search'
  }

  const resetBoundsSummary = (total = 0, limit = 0, truncated = false) => {
    boundsSummary.value = { total, limit, truncated }
  }

  const resetSearchSummary = (total = 0, limit = 0, truncated = false) => {
    searchSummary.value = { total, limit, truncated }
  }

  const showBoundsResults = () => {
    activeSource.value = 'bounds'
  }

  const fetchAllPOIs = async () => {
    isLoading.value = true
    error.value = null
    try {
      const data = await getAllPOIs()
      setSearchPoiList(data?.records || [])
      resetBoundsSummary(searchPoiList.value.length, searchPoiList.value.length, false)
      resetSearchSummary(
        Number(data?.total || 0),
        Number(data?.limit || 0),
        Boolean(data?.truncated)
      )
      return data
    } catch (err) {
      error.value = err.message
      throw err
    } finally {
      isLoading.value = false
    }
  }

  const fetchPOIById = async (id) => {
    isLoading.value = true
    error.value = null
    try {
      const data = await getPOIById(id)
      currentPOI.value = data
      cachePoiRecords([data])
      return data
    } catch (err) {
      error.value = err.message
      throw err
    } finally {
      isLoading.value = false
    }
  }

  const searchByName = async (name) => {
    isLoading.value = true
    error.value = null
    try {
      const data = await searchPOIByName(name)
      setSearchPoiList(data?.records || [])
      resetBoundsSummary(
        Number(data?.total || 0),
        Number(data?.limit || 0),
        Boolean(data?.truncated)
      )
      resetSearchSummary(
        Number(data?.total || 0),
        Number(data?.limit || 0),
        Boolean(data?.truncated)
      )
      return data
    } catch (err) {
      error.value = err.message
      throw err
    } finally {
      isLoading.value = false
    }
  }

  const fetchByCategory = async (category) => {
    isLoading.value = true
    error.value = null
    try {
      const data = await getPOIsByCategory(category)
      setSearchPoiList(data?.records || [])
      resetBoundsSummary(
        Number(data?.total || 0),
        Number(data?.limit || 0),
        Boolean(data?.truncated)
      )
      resetSearchSummary(
        Number(data?.total || 0),
        Number(data?.limit || 0),
        Boolean(data?.truncated)
      )
      return data
    } catch (err) {
      error.value = err.message
      throw err
    } finally {
      isLoading.value = false
    }
  }

  const advancedSearch = async (params) => {
    const hasKeyword = typeof params?.name === 'string' && params.name.trim()
    const hasCategory = typeof params?.category === 'string' && params.category.trim()

    if (!hasKeyword && !hasCategory) {
      searchPoiList.value = []
      resetBoundsSummary()
      resetSearchSummary()
      return { records: [], total: 0, limit: 0, truncated: false }
    }

    isLoading.value = true
    error.value = null
    try {
      const data = await advancedSearchPOI(params)
      setSearchPoiList(data?.records || [])
      resetBoundsSummary(
        Number(data?.total || 0),
        Number(data?.limit || 0),
        Boolean(data?.truncated)
      )
      resetSearchSummary(
        Number(data?.total || 0),
        Number(data?.limit || 0),
        Boolean(data?.truncated)
      )
      return data
    } catch (err) {
      error.value = err.message
      throw err
    } finally {
      isLoading.value = false
    }
  }

  const fetchInBounds = async (minLat, maxLat, minLng, maxLng, limit = 1200) => {
    isLoading.value = true
    error.value = null
    try {
      const data = await getPOIsInBounds(minLat, maxLat, minLng, maxLng, limit)
      setMapPoiList(data?.records || [])
      resetBoundsSummary(
        Number(data?.total || 0),
        Number(data?.limit || limit || 0),
        Boolean(data?.truncated)
      )
      return data
    } catch (err) {
      error.value = err.message
      throw err
    } finally {
      isLoading.value = false
    }
  }

  const fetchCategories = async () => {
    try {
      const data = await getCategories()
      categories.value = data || []
      return data
    } catch (err) {
      error.value = err.message
      throw err
    }
  }

  const create = async (poiData) => {
    isLoading.value = true
    error.value = null
    try {
      const data = await createPOI(poiData)
      setSearchPoiList([...searchPoiList.value, data])
      resetBoundsSummary(searchPoiList.value.length, searchPoiList.value.length, false)
      resetSearchSummary(searchPoiList.value.length, searchPoiList.value.length, false)
      return data
    } catch (err) {
      error.value = err.message
      throw err
    } finally {
      isLoading.value = false
    }
  }

  const update = async (id, poiData) => {
    isLoading.value = true
    error.value = null
    try {
      const data = await updatePOI(id, poiData)
      cachePoiRecords([data])
      mapPoiList.value = mapPoiList.value.map((item) => (item.id === id ? data : item))
      searchPoiList.value = searchPoiList.value.map((item) => (item.id === id ? data : item))
      if (currentPOI.value?.id === id) {
        currentPOI.value = data
      }
      return data
    } catch (err) {
      error.value = err.message
      throw err
    } finally {
      isLoading.value = false
    }
  }

  const remove = async (id) => {
    isLoading.value = true
    error.value = null
    try {
      await deletePOI(id)
      poiEntityCache.delete(id)
      mapPoiList.value = mapPoiList.value.filter((item) => item.id !== id)
      searchPoiList.value = searchPoiList.value.filter((item) => item.id !== id)
      resetBoundsSummary(
        Math.max(boundsSummary.value.total - 1, 0),
        boundsSummary.value.limit,
        boundsSummary.value.truncated && visiblePoiList.value.length >= boundsSummary.value.limit
      )
      if (currentPOI.value?.id === id) {
        currentPOI.value = null
      }
    } catch (err) {
      error.value = err.message
      throw err
    } finally {
      isLoading.value = false
    }
  }

  const clearPOIList = () => {
    mapPoiList.value = []
    searchPoiList.value = []
    resetBoundsSummary()
    resetSearchSummary()
  }

  const clearSearchPoiList = () => {
    searchPoiList.value = []
    resetSearchSummary()
  }

  const getCachedPOIById = (id) => {
    if (id == null) {
      return null
    }

    return poiEntityCache.get(id)
      || mapPoiList.value.find((item) => item.id === id)
      || searchPoiList.value.find((item) => item.id === id)
      || null
  }

  const clearCurrentPOI = () => {
    currentPOI.value = null
  }

  return {
    poiList,
    visiblePoiList,
    mapPoiList,
    searchPoiList,
    activeSource,
    currentPOI,
    categories,
    boundsSummary,
    searchSummary,
    isLoading,
    error,
    fetchAllPOIs,
    fetchPOIById,
    searchByName,
    fetchByCategory,
    advancedSearch,
    fetchInBounds,
    fetchCategories,
    showBoundsResults,
    create,
    update,
    remove,
    clearPOIList,
    clearSearchPoiList,
    getCachedPOIById,
    clearCurrentPOI
  }
})
