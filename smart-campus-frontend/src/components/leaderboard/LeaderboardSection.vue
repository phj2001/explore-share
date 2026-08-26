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

<style scoped lang="scss">
/* ── LeaderboardSection 新设计系统 ── */
.leaderboard-section {
  padding: 48px 0;
  display: flex;
  flex-direction: column;
  gap: 0;
}

.section-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  padding-bottom: 24px;
  border-bottom: 1px solid var(--front-border);
  margin-bottom: 24px;
}

.head-left {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.section-kicker {
  display: inline-flex;
  padding: 3px 10px;
  border-radius: 999px;
  background: rgba(31, 140, 105, 0.10);
  color: var(--forest-700);
  font-family: var(--font-mono);
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  width: fit-content;
}

.section-head h2 {
  margin: 0;
  font-family: var(--font-serif);
  font-size: 24px;
  font-weight: 700;
  color: var(--ink-900);
  letter-spacing: -0.02em;
  line-height: 1.25;
}

/* Tab 区 */
.leaderboard-tabs {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 24px;
}

.tab-group {
  display: flex;
  gap: 6px;
}

.tab-btn,
.period-btn {
  padding: 7px 16px;
  border-radius: 999px;
  border: 1px solid var(--front-border);
  background: transparent;
  font-family: var(--font-sans);
  font-size: 12.5px;
  font-weight: 500;
  color: var(--ink-600);
  cursor: pointer;
  transition: background 0.2s, color 0.2s, border-color 0.2s;
  white-space: nowrap;
}

.tab-btn:hover,
.period-btn:hover {
  background: var(--paper-100);
  color: var(--ink-900);
}

.tab-btn.active,
.period-btn.active {
  background: var(--forest-700);
  color: #fff;
  border-color: var(--forest-700);
}

/* 排行列表 */
.leaderboard-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.leaderboard-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 18px;
  border: 1px solid var(--front-border);
  border-radius: 12px;
  background: #fff;
  text-decoration: none;
  color: inherit;
  transition: border-color 0.2s, box-shadow 0.2s, transform 0.2s;
}

.leaderboard-item:hover {
  border-color: var(--forest-500);
  box-shadow: 0 4px 16px rgba(20, 80, 55, 0.10);
  transform: translateY(-1px);
}

/* 排名徽章 */
.rank-badge {
  font-family: var(--font-mono);
  font-size: 14px;
  font-weight: 700;
  width: 28px;
  text-align: center;
  color: var(--ink-400);
  flex-shrink: 0;
}

.rank-badge.gold   { color: #d5b53c; }
.rank-badge.silver { color: #97ad9f; }
.rank-badge.bronze { color: #d9923e; }

/* 头像 */
.item-avatar {
  background: linear-gradient(135deg, var(--forest-500), var(--forest-700));
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
  font-family: var(--font-sans);
  font-size: 14px;
  font-weight: 600;
  color: var(--ink-900);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.item-count {
  font-family: var(--font-mono);
  font-size: 11.5px;
  color: var(--ink-400);
  display: block;
  margin-top: 2px;
}

/* 空状态 */
.empty-hint {
  font-family: var(--font-mono);
  font-size: 12px;
  color: var(--ink-400);
  text-align: center;
  padding: 48px 0;
  letter-spacing: 0.08em;
}

/* 响应式 */
@include respond-to(md) {
  .leaderboard-list {
    grid-template-columns: 1fr;
  }
}

@include respond-to(sm) {
  .leaderboard-section {
    padding: 32px 0;
  }

  .leaderboard-tabs {
    flex-wrap: wrap;
    gap: 8px;
  }

  .tab-group {
    overflow-x: auto;
  }

  .section-head h2 {
    font-size: 20px;
  }
}

/* 触屏：tab 切换与排行项撑足热区 */
@include coarse-pointer {
  .tab-btn,
  .period-btn {
    min-height: 40px;
  }

  .leaderboard-item {
    min-height: 48px;
  }
}
</style>
