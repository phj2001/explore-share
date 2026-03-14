import { defineStore } from 'pinia'
import { ref } from 'vue'
import {
  getAllPOIs,
  getPOIById,
  searchPOIByName,
  getPOIsByCategory,
  advancedSearchPOI,
  getPOIsInBounds,
  getCategories,
  createPOI,
  updatePOI,
  deletePOI
} from '@/api/poi.js'

export const usePOIStore = defineStore('poi', () => {
  // 状态
  const poiList = ref([])
  const currentPOI = ref(null)
  const categories = ref([])
  const isLoading = ref(false)
  const error = ref(null)

  /**
   * 获取所有 POI
   */
  const fetchAllPOIs = async () => {
    isLoading.value = true
    error.value = null
    try {
      const data = await getAllPOIs()
      poiList.value = data || []
      return data
    } catch (err) {
      error.value = err.message
      throw err
    } finally {
      isLoading.value = false
    }
  }

  /**
   * 根据 ID 获取 POI
   */
  const fetchPOIById = async (id) => {
    isLoading.value = true
    error.value = null
    try {
      const data = await getPOIById(id)
      currentPOI.value = data
      return data
    } catch (err) {
      error.value = err.message
      throw err
    } finally {
      isLoading.value = false
    }
  }

  /**
   * 搜索 POI（按名称）
   */
  const searchByName = async (name) => {
    isLoading.value = true
    error.value = null
    try {
      const data = await searchPOIByName(name)
      poiList.value = data || []
      return data
    } catch (err) {
      error.value = err.message
      throw err
    } finally {
      isLoading.value = false
    }
  }

  /**
   * 按分类获取 POI
   */
  const fetchByCategory = async (category) => {
    isLoading.value = true
    error.value = null
    try {
      const data = await getPOIsByCategory(category)
      poiList.value = data || []
      return data
    } catch (err) {
      error.value = err.message
      throw err
    } finally {
      isLoading.value = false
    }
  }

  /**
   * 高级搜索 POI
   */
  const advancedSearch = async (params) => {
    isLoading.value = true
    error.value = null
    try {
      const data = await advancedSearchPOI(params)
      poiList.value = data || []
      return data
    } catch (err) {
      error.value = err.message
      throw err
    } finally {
      isLoading.value = false
    }
  }

  /**
   * 获取边界范围内的 POI
   */
  const fetchInBounds = async (minLat, maxLat, minLng, maxLng) => {
    isLoading.value = true
    error.value = null
    try {
      const data = await getPOIsInBounds(minLat, maxLat, minLng, maxLng)
      poiList.value = data || []
      return data
    } catch (err) {
      error.value = err.message
      throw err
    } finally {
      isLoading.value = false
    }
  }

  /**
   * 获取所有分类
   */
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

  /**
   * 创建 POI
   */
  const create = async (poiData) => {
    isLoading.value = true
    error.value = null
    try {
      const data = await createPOI(poiData)
      poiList.value.push(data)
      return data
    } catch (err) {
      error.value = err.message
      throw err
    } finally {
      isLoading.value = false
    }
  }

  /**
   * 更新 POI
   */
  const update = async (id, poiData) => {
    isLoading.value = true
    error.value = null
    try {
      const data = await updatePOI(id, poiData)
      const index = poiList.value.findIndex((p) => p.id === id)
      if (index !== -1) {
        poiList.value[index] = data
      }
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

  /**
   * 删除 POI
   */
  const remove = async (id) => {
    isLoading.value = true
    error.value = null
    try {
      await deletePOI(id)
      poiList.value = poiList.value.filter((p) => p.id !== id)
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

  /**
   * 清空列表
   */
  const clearPOIList = () => {
    poiList.value = []
  }

  /**
   * 清空当前 POI
   */
  const clearCurrentPOI = () => {
    currentPOI.value = null
  }

  return {
    // 状态
    poiList,
    currentPOI,
    categories,
    isLoading,
    error,
    // 方法
    fetchAllPOIs,
    fetchPOIById,
    searchByName,
    fetchByCategory,
    advancedSearch,
    fetchInBounds,
    fetchCategories,
    create,
    update,
    remove,
    clearPOIList,
    clearCurrentPOI
  }
})
