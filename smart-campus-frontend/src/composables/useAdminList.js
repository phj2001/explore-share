import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'

/**
 * Admin 分页列表通用逻辑。
 *
 * 约定后端分页接口：入参 { page(0 基), size, ...业务筛选 }，返回 { records, total, page }。
 * 业务筛选字段由调用方在 fetchPage 闭包内组装，这里只负责分页状态、加载态与错误提示。
 *
 * @param {Object} options
 * @param {(params: { page: number, size: number }) => Promise<{records: Array, total: number, page?: number}>} options.fetchPage 分页请求函数
 * @param {number} [options.defaultPageSize=10] 初始每页条数
 * @param {string} [options.errorMessage] 加载失败提示文案
 * @param {boolean} [options.syncPageFromResponse=true] 是否用响应中的 page 回写当前页码（后端钳制页码时生效）
 * @param {boolean} [options.immediate=true] 是否在挂载时自动加载一次
 * @param {(data: {records: Array, total: number, page?: number}) => void} [options.onLoaded] 数据加载完成后的钩子
 */
export function useAdminList(options) {
  const {
    fetchPage,
    defaultPageSize = 10,
    errorMessage = '加载列表失败',
    syncPageFromResponse = true,
    immediate = true,
    onLoaded
  } = options

  const list = ref([])
  const loading = ref(false)
  const currentPage = ref(1)
  const pageSize = ref(defaultPageSize)
  const total = ref(0)

  const load = async () => {
    loading.value = true
    try {
      const data = await fetchPage({
        page: currentPage.value - 1,
        size: pageSize.value
      })
      list.value = data.records || []
      total.value = data.total || 0
      if (syncPageFromResponse) {
        currentPage.value = (data.page || 0) + 1
      }
      onLoaded?.(data)
    } catch (error) {
      ElMessage.error(error.message || errorMessage)
    } finally {
      loading.value = false
    }
  }

  // 搜索 / 切换每页条数：回到第一页后重新加载
  const search = async () => {
    currentPage.value = 1
    await load()
  }

  // 重置：调用方先清空筛选字段，再调用本方法
  const reset = async () => {
    currentPage.value = 1
    await load()
  }

  const handlePageSizeChange = async () => {
    currentPage.value = 1
    await load()
  }

  // 删除最后一行且不在第一页时回退一页，避免刷新后落在空页（在删除成功后、load 之前调用）
  const shrinkPageIfLastRow = () => {
    if (list.value.length <= 1 && currentPage.value > 1) {
      currentPage.value -= 1
    }
  }

  if (immediate) {
    onMounted(load)
  }

  return {
    list,
    loading,
    currentPage,
    pageSize,
    total,
    load,
    search,
    reset,
    handlePageSizeChange,
    shrinkPageIfLastRow
  }
}
