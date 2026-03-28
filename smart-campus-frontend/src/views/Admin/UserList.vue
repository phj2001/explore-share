<template>
  <div class="user-page">
    <section class="page-hero">
      <div>
        <span class="hero-kicker">用户管理</span>
        <h1>统一查看账号、角色与账号状态</h1>
        <p>支持按用户名、展示名、角色和状态筛选，并可在后台直接查看用户资料和互动统计。</p>
      </div>
    </section>

    <section class="filter-panel">
      <el-input
        v-model="keyword"
        clearable
        placeholder="搜索用户名或展示名"
        class="filter-input"
        @keyup.enter="handleSearch"
        @clear="handleSearch"
      >
        <template #append>
          <el-button :icon="Search" @click="handleSearch" />
        </template>
      </el-input>

      <el-select v-model="roleFilter" clearable placeholder="筛选角色" class="filter-select" @change="handleSearch">
        <el-option :value="USER_ROLE" label="普通用户" />
        <el-option :value="SUPER_ADMIN_ROLE" label="超级管理员" />
      </el-select>

      <el-select v-model="statusFilter" clearable placeholder="筛选状态" class="filter-select" @change="handleSearch">
        <el-option :value="ACTIVE_STATUS" label="正常" />
        <el-option :value="DISABLED_STATUS" label="禁用" />
      </el-select>

      <el-button @click="resetFilters">重置</el-button>
    </section>

    <section class="table-panel">
      <el-table :data="users" v-loading="loading" stripe>
        <el-table-column label="头像" width="88">
          <template #default="{ row }">
            <el-avatar :size="38" :src="resolveAvatar(row.avatarUrl) || undefined" class="user-avatar">
              {{ getDisplayName(row).slice(0, 1).toUpperCase() }}
            </el-avatar>
          </template>
        </el-table-column>

        <el-table-column prop="id" label="ID" width="88" />
        <el-table-column prop="username" label="用户名" min-width="140" />
        <el-table-column label="展示名" min-width="150">
          <template #default="{ row }">
            {{ getDisplayName(row) }}
          </template>
        </el-table-column>
        <el-table-column label="个性签名" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.bio || '未设置签名' }}
          </template>
        </el-table-column>
        <el-table-column label="角色" width="120">
          <template #default="{ row }">
            <el-tag :type="row.role === SUPER_ADMIN_ROLE ? 'danger' : 'info'" effect="plain">
              {{ getRoleLabel(row.role) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="row.status === ACTIVE_STATUS ? 'success' : 'warning'" effect="plain">
              {{ getStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="注册时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="290" fixed="right">
          <template #default="{ row }">
            <div class="action-group">
              <el-button size="small" @click="openDetail(row)">查看</el-button>
              <el-button
                size="small"
                :type="row.role === SUPER_ADMIN_ROLE ? 'warning' : 'primary'"
                :disabled="isCurrentUser(row)"
                :loading="roleUpdatingId === row.id"
                @click="toggleRole(row)"
              >
                {{ row.role === SUPER_ADMIN_ROLE ? '降为普通用户' : '设为管理员' }}
              </el-button>
              <el-button
                size="small"
                :type="row.status === ACTIVE_STATUS ? 'danger' : 'success'"
                :disabled="isCurrentUser(row)"
                :loading="statusUpdatingId === row.id"
                @click="toggleStatus(row)"
              >
                {{ row.status === ACTIVE_STATUS ? '禁用' : '启用' }}
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-bar">
        <span>共 {{ total }} 位用户</span>
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50]"
          :total="total"
          layout="sizes, prev, pager, next"
          @current-change="loadUsers"
          @size-change="handlePageSizeChange"
        />
      </div>
    </section>

    <el-drawer v-model="drawerVisible" size="480px" :with-header="false" destroy-on-close>
      <div class="drawer-body" v-loading="detailLoading">
        <template v-if="selectedUser">
          <div class="drawer-hero">
            <div class="drawer-profile">
              <el-avatar :size="68" :src="resolveAvatar(selectedUser.avatarUrl) || undefined" class="detail-avatar">
                {{ getDisplayName(selectedUser).slice(0, 1).toUpperCase() }}
              </el-avatar>
              <div>
                <h2>{{ getDisplayName(selectedUser) }}</h2>
                <p>@{{ selectedUser.username }}</p>
              </div>
            </div>
            <div class="drawer-tags">
              <el-tag :type="selectedUser.role === SUPER_ADMIN_ROLE ? 'danger' : 'info'" effect="plain">
                {{ getRoleLabel(selectedUser.role) }}
              </el-tag>
              <el-tag :type="selectedUser.status === ACTIVE_STATUS ? 'success' : 'warning'" effect="plain">
                {{ getStatusLabel(selectedUser.status) }}
              </el-tag>
            </div>
          </div>

          <div class="drawer-section">
            <h3>基础资料</h3>
            <div class="info-grid">
              <div class="info-item">
                <span>用户 ID</span>
                <strong>{{ selectedUser.id }}</strong>
              </div>
              <div class="info-item">
                <span>注册时间</span>
                <strong>{{ formatDate(selectedUser.createdAt) }}</strong>
              </div>
              <div class="info-item">
                <span>最近更新时间</span>
                <strong>{{ formatDate(selectedUser.updatedAt) }}</strong>
              </div>
              <div class="info-item wide">
                <span>个性签名</span>
                <strong>{{ selectedUser.bio || '未设置签名' }}</strong>
              </div>
            </div>
          </div>

          <div class="drawer-section">
            <h3>互动统计</h3>
            <div class="stats-grid">
              <div class="stat-card">
                <span>发布分享</span>
                <strong>{{ selectedUser.shareCount || 0 }}</strong>
              </div>
              <div class="stat-card">
                <span>发布回复</span>
                <strong>{{ selectedUser.replyCount || 0 }}</strong>
              </div>
              <div class="stat-card">
                <span>累计点赞</span>
                <strong>{{ selectedUser.likeCount || 0 }}</strong>
              </div>
            </div>
          </div>

          <div class="drawer-section">
            <h3>管理说明</h3>
            <ul class="tips-list">
              <li>用户禁用后将无法登录，已登录状态会在后续请求中失效。</li>
              <li>当前登录管理员不能禁用自己，也不能将自己降级为普通用户。</li>
              <li>第一版不提供删除用户功能，避免破坏现有分享与互动数据。</li>
            </ul>
          </div>
        </template>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import {
  getAdminUserDetail,
  getAdminUserPage,
  updateAdminUserRole,
  updateAdminUserStatus
} from '@/api/adminUser'
import { useUserStore } from '@/stores/user'
import { SUPER_ADMIN_ROLE } from '@/constants/auth'
import { API_ORIGIN } from '@/utils/request'

const USER_ROLE = 1
const ACTIVE_STATUS = 1
const DISABLED_STATUS = 0

const userStore = useUserStore()
const route = useRoute()

const keyword = ref('')
const roleFilter = ref()
const statusFilter = ref()
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const users = ref([])
const loading = ref(false)
const drawerVisible = ref(false)
const detailLoading = ref(false)
const selectedUser = ref(null)
const roleUpdatingId = ref(null)
const statusUpdatingId = ref(null)

const currentUserId = computed(() => userStore.userInfo?.id ?? null)

const loadUsers = async () => {
  loading.value = true
  try {
    const data = await getAdminUserPage({
      keyword: keyword.value.trim() || undefined,
      role: roleFilter.value,
      status: statusFilter.value,
      page: currentPage.value - 1,
      size: pageSize.value
    })
    users.value = data.records || []
    total.value = data.total || 0
    currentPage.value = (data.page || 0) + 1
  } catch (error) {
    ElMessage.error(error.message || '加载用户列表失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = async () => {
  currentPage.value = 1
  await loadUsers()
}

const handlePageSizeChange = async () => {
  currentPage.value = 1
  await loadUsers()
}

const resetFilters = async () => {
  keyword.value = ''
  roleFilter.value = undefined
  statusFilter.value = undefined
  currentPage.value = 1
  await loadUsers()
}

const openDetail = async (row) => {
  drawerVisible.value = true
  detailLoading.value = true
  try {
    selectedUser.value = await getAdminUserDetail(row.id)
  } catch (error) {
    ElMessage.error(error.message || '加载用户详情失败')
  } finally {
    detailLoading.value = false
  }
}

const refreshDetailIfNeeded = async (userId) => {
  if (!drawerVisible.value || selectedUser.value?.id !== userId) {
    return
  }
  selectedUser.value = await getAdminUserDetail(userId)
}

const toggleRole = async (row) => {
  if (isCurrentUser(row)) {
    return
  }

  const nextRole = row.role === SUPER_ADMIN_ROLE ? USER_ROLE : SUPER_ADMIN_ROLE
  const nextLabel = getRoleLabel(nextRole)

  try {
    await ElMessageBox.confirm(`确定将该用户角色设置为“${nextLabel}”吗？`, '修改角色', {
      type: 'warning',
      confirmButtonText: '确认',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }

  roleUpdatingId.value = row.id
  try {
    const detail = await updateAdminUserRole(row.id, nextRole)
    row.role = detail.role
    await refreshDetailIfNeeded(row.id)
    ElMessage.success('用户角色已更新')
  } catch (error) {
    ElMessage.error(error.message || '更新用户角色失败')
  } finally {
    roleUpdatingId.value = null
  }
}

const toggleStatus = async (row) => {
  if (isCurrentUser(row)) {
    return
  }

  const nextStatus = row.status === ACTIVE_STATUS ? DISABLED_STATUS : ACTIVE_STATUS
  const actionText = nextStatus === ACTIVE_STATUS ? '启用' : '禁用'

  try {
    await ElMessageBox.confirm(`确定要${actionText}该用户吗？`, `${actionText}用户`, {
      type: 'warning',
      confirmButtonText: '确认',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }

  statusUpdatingId.value = row.id
  try {
    const detail = await updateAdminUserStatus(row.id, nextStatus)
    row.status = detail.status
    await refreshDetailIfNeeded(row.id)
    ElMessage.success(`用户已${actionText}`)
  } catch (error) {
    ElMessage.error(error.message || `${actionText}用户失败`)
  } finally {
    statusUpdatingId.value = null
  }
}

const getDisplayName = (row) => row.displayName || row.username || '未命名用户'
const getRoleLabel = (role) => (role === SUPER_ADMIN_ROLE ? '超级管理员' : '普通用户')
const getStatusLabel = (status) => (status === ACTIVE_STATUS ? '正常' : '禁用')
const isCurrentUser = (row) => row.id === currentUserId.value

const resolveAvatar = (value) => {
  if (!value) {
    return ''
  }

  if (/^https?:\/\//i.test(value)) {
    return value
  }

  return `${API_ORIGIN}${value.startsWith('/') ? value : `/${value}`}`
}

const formatDate = (value) => {
  if (!value) {
    return '暂无'
  }

  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  }).format(new Date(value))
}

onMounted(async () => {
  await loadUsers()
  if (route.query.openUserId) {
    await openDetail({ id: Number(route.query.openUserId) })
  }
})
</script>

<style scoped>
.user-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.page-hero,
.filter-panel,
.table-panel {
  border: 1px solid rgba(148, 163, 184, 0.18);
  background: rgba(255, 255, 255, 0.88);
  backdrop-filter: blur(18px);
  box-shadow: 0 18px 42px rgba(15, 23, 42, 0.06);
}

.page-hero {
  border-radius: 28px;
  padding: 26px 28px;
  background:
    radial-gradient(circle at top right, rgba(14, 165, 233, 0.16), transparent 30%),
    linear-gradient(135deg, rgba(255, 255, 255, 0.96), rgba(244, 248, 255, 0.9));
}

.hero-kicker {
  display: inline-flex;
  align-items: center;
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(37, 99, 235, 0.08);
  color: #2563eb;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.page-hero h1 {
  margin: 14px 0 10px;
  font-size: clamp(26px, 4vw, 38px);
  line-height: 1.1;
  color: #0f172a;
}

.page-hero p {
  margin: 0;
  color: #64748b;
  line-height: 1.75;
}

.filter-panel {
  border-radius: 24px;
  padding: 18px;
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.filter-input {
  width: 320px;
}

.filter-select {
  width: 180px;
}

.table-panel {
  border-radius: 28px;
  padding: 18px 18px 8px;
}

.user-avatar,
.detail-avatar {
  background: linear-gradient(135deg, #38bdf8, #2563eb);
  color: #fff;
  font-weight: 700;
}

.action-group {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.pagination-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 18px 6px 8px;
  color: #64748b;
}

.drawer-body {
  height: 100%;
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 22px;
}

.drawer-hero {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
}

.drawer-profile {
  display: flex;
  align-items: center;
  gap: 16px;
}

.drawer-profile h2 {
  margin: 0;
  color: #0f172a;
  font-size: 28px;
}

.drawer-profile p {
  margin: 6px 0 0;
  color: #64748b;
}

.drawer-tags {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.drawer-section h3 {
  margin: 0 0 14px;
  color: #0f172a;
  font-size: 18px;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.info-item,
.stat-card {
  border-radius: 18px;
  padding: 16px;
  background: linear-gradient(180deg, rgba(247, 250, 255, 0.96), rgba(255, 255, 255, 0.96));
  border: 1px solid rgba(226, 232, 240, 0.9);
}

.info-item span,
.stat-card span {
  display: block;
  color: #64748b;
  font-size: 13px;
}

.info-item strong,
.stat-card strong {
  display: block;
  margin-top: 10px;
  color: #0f172a;
  line-height: 1.6;
}

.info-item.wide {
  grid-column: 1 / -1;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.stat-card strong {
  font-size: 30px;
  line-height: 1;
}

.tips-list {
  margin: 0;
  padding-left: 18px;
  color: #475569;
  line-height: 1.8;
}

@media (max-width: 1100px) {
  .filter-input {
    width: 100%;
  }
}

@media (max-width: 760px) {
  .pagination-bar,
  .drawer-hero {
    flex-direction: column;
    align-items: stretch;
  }

  .info-grid,
  .stats-grid {
    grid-template-columns: 1fr;
  }
}
</style>
