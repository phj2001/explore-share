<template>
  <section class="front-shell leaderboard-section">
    <div class="section-head">
      <div class="head-left">
        <span class="section-kicker">探索排行</span>
        <h2>活跃用户排行榜</h2>
      </div>
    </div>

    <div class="leaderboard-tabs">
      <div class="tab-group">
        <button
          v-for="t in typeTabs"
          :key="t.value"
          :class="['tab-btn', { active: activeType === t.value }]"
          @click="switchType(t.value)"
        >
          {{ t.label }}
        </button>
      </div>
      <div class="tab-group">
        <button
          v-for="p in periodTabs"
          :key="p.value"
          :class="['period-btn', { active: activePeriod === p.value }]"
          @click="switchPeriod(p.value)"
        >
          {{ p.label }}
        </button>
      </div>
    </div>

    <el-skeleton v-if="loading && !items.length" :rows="5" animated />

    <div v-else-if="items.length" class="leaderboard-list">
      <router-link
        v-for="item in items"
        :key="item.userId"
        :to="'/user/' + item.userId"
        class="leaderboard-item"
      >
        <span :class="['rank-badge', { gold: item.rank === 1, silver: item.rank === 2, bronze: item.rank === 3 }]">
          {{ item.rank }}
        </span>
        <el-avatar :size="38" :src="item.avatarUrl || undefined" class="item-avatar">
          {{ (item.displayName || 'U').slice(0, 1).toUpperCase() }}
        </el-avatar>
        <div class="item-info">
          <strong>{{ item.displayName }}</strong>
          <span class="item-count">{{ item.count }} {{ countLabel }}</span>
        </div>
      </router-link>
    </div>

    <div v-else class="empty-hint">暂无排行数据</div>
  </section>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { getLeaderboard } from '@/api/leaderboard'

const typeTabs = [
  { value: 'checkin', label: '打卡达人' },
  { value: 'share', label: '分享达人' },
  { value: 'likes', label: '获赞达人' }
]

const periodTabs = [
  { value: 'total', label: '总榜' },
  { value: 'week', label: '周榜' }
]

const countLabels = { checkin: '次打卡', share: '次分享', likes: '个赞' }

const activeType = ref('checkin')
const activePeriod = ref('total')
const loading = ref(false)
const items = ref([])

const countLabel = computed(() => countLabels[activeType.value] || '')

const loadData = async () => {
  loading.value = true
  try {
    const data = await getLeaderboard({ type: activeType.value, period: activePeriod.value, limit: 10 })
    items.value = data || []
  } catch {
    items.value = []
  } finally {
    loading.value = false
  }
}

const switchType = (type) => { activeType.value = type }
const switchPeriod = (period) => { activePeriod.value = period }

watch([activeType, activePeriod], () => loadData())
onMounted(() => loadData())
</script>

<style scoped>
.leaderboard-section {
  padding: 24px 0;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.head-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.section-kicker {
  display: inline-flex;
  padding: 4px 10px;
  border-radius: 999px;
  background: #ecfdf5;
  color: #059669;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.06em;
  flex-shrink: 0;
}

.section-head h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  color: #0f172a;
}

.leaderboard-tabs {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  border-bottom: 2px solid #e2e8f0;
}

.tab-group {
  display: flex;
  gap: 2px;
}

.tab-btn,
.period-btn {
  padding: 7px 16px;
  font-size: 13px;
  font-weight: 600;
  color: #64748b;
  background: none;
  border: none;
  border-bottom: 2px solid transparent;
  margin-bottom: -2px;
  cursor: pointer;
  transition: color 0.2s, border-color 0.2s;
}

.tab-btn:hover,
.period-btn:hover {
  color: #0f172a;
}

.tab-btn.active {
  color: #0ea5e9;
  border-bottom-color: #0ea5e9;
}

.period-btn.active {
  color: #8b5cf6;
  border-bottom-color: #8b5cf6;
}

.leaderboard-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.leaderboard-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border-radius: 16px;
  background: #fff;
  box-shadow: 0 4px 14px rgba(15, 23, 42, 0.05);
  text-decoration: none;
  color: inherit;
  transition: transform 0.15s, box-shadow 0.15s;
}

.leaderboard-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.09);
}

.rank-badge {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 13px;
  color: #64748b;
  background: #f1f5f9;
  flex-shrink: 0;
}

.rank-badge.gold {
  background: linear-gradient(135deg, #fbbf24, #f59e0b);
  color: #fff;
}

.rank-badge.silver {
  background: linear-gradient(135deg, #cbd5e1, #94a3b8);
  color: #fff;
}

.rank-badge.bronze {
  background: linear-gradient(135deg, #f97316, #ea580c);
  color: #fff;
}

.item-avatar {
  background: linear-gradient(135deg, #38bdf8, #2563eb);
  color: #fff;
  font-weight: 700;
  flex-shrink: 0;
}

.item-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.item-info strong {
  color: #0f172a;
  font-size: 14px;
}

.item-count {
  color: #64748b;
  font-size: 12px;
}

.empty-hint {
  text-align: center;
  padding: 20px;
  color: var(--front-text-muted);
  font-size: 13px;
}

@media (max-width: 640px) {
  .leaderboard-section {
    padding: 20px 0;
  }

  .leaderboard-tabs {
    flex-direction: column;
    align-items: stretch;
  }

  .tab-group {
    overflow-x: auto;
  }

  .section-head h2 {
    font-size: 17px;
  }
}
</style>
