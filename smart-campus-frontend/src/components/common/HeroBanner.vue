<template>
  <div class="hero-banner front-shell front-panel">
    <p class="banner-tagline">探索校园地点 · 发现精彩活动 · 分享你的路线</p>
    <nav class="banner-nav">
      <button class="nav-chip" @click="scrollTo('map-section')">浏览地点</button>
      <button class="nav-chip" @click="scrollTo('recommended-share-section')">精选分享</button>
      <button class="nav-chip" @click="scrollTo('activity-section')">近期活动</button>
      <button class="nav-chip" @click="scrollTo('recommended-route-section')">路线探索</button>
      <button class="nav-chip" @click="scrollTo('user-route-section')">社区路线</button>
      <router-link :to="isLoggedIn ? '/settings' : '/login'" class="nav-chip nav-chip--accent">
        {{ isLoggedIn ? '我的空间' : '去登录' }}
      </router-link>
    </nav>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const isLoggedIn = computed(() => userStore.isLoggedIn)

const scrollTo = (id) => {
  const el = document.getElementById(id)
  if (el) el.scrollIntoView({ behavior: 'smooth', block: 'start' })
}
</script>

<style scoped lang="scss">
/* ── HeroBanner 新设计系统 ── */

/* 等高线背景装饰（SVG data URI，浅色翠绿等高线） */
.hero-banner {
  position: relative;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 24px;
  padding: 48px 40px 44px;
  border-radius: 18px;
  border: 1px solid var(--front-border);
  background-color: var(--paper-50);
  background-image:
    url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='600' height='260'%3E%3Cellipse cx='480' cy='130' rx='280' ry='90' fill='none' stroke='%231f8c69' stroke-width='1' opacity='0.08'/%3E%3Cellipse cx='480' cy='130' rx='220' ry='65' fill='none' stroke='%231f8c69' stroke-width='1' opacity='0.07'/%3E%3Cellipse cx='480' cy='130' rx='160' ry='44' fill='none' stroke='%231f8c69' stroke-width='1' opacity='0.06'/%3E%3Cellipse cx='480' cy='130' rx='100' ry='26' fill='none' stroke='%231f8c69' stroke-width='1' opacity='0.05'/%3E%3Cellipse cx='60' cy='200' rx='120' ry='50' fill='none' stroke='%231f8c69' stroke-width='1' opacity='0.05'/%3E%3Cellipse cx='60' cy='200' rx='80' ry='34' fill='none' stroke='%231f8c69' stroke-width='1' opacity='0.04'/%3E%3C/svg%3E"),
    linear-gradient(150deg, var(--paper-50) 0%, var(--forest-50) 100%);
  background-position: right center, left top;
  background-repeat: no-repeat;
  background-size: 60% auto, 100% 100%;
  box-shadow: var(--front-shadow);
}

/* 坐标小字 */
.hero-banner::before {
  content: "30°17′N  120°09′E";
  position: absolute;
  top: 18px;
  right: 24px;
  font-family: var(--font-mono);
  font-size: 10.5px;
  letter-spacing: 0.16em;
  color: var(--ink-400);
  opacity: 0.8;
}

/* 主标语 */
.banner-tagline {
  margin: 0;
  font-family: var(--font-serif);
  font-size: clamp(26px, 3.5vw, 48px);
  font-weight: 700;
  color: var(--ink-900);
  letter-spacing: -0.025em;
  line-height: 1.18;
  max-width: 580px;
}

/* 珊瑚橘高亮 em */
.banner-tagline :deep(em),
.banner-tagline em {
  font-style: normal;
  color: var(--clay-600);
}

/* 导航 chip 组 */
.banner-nav {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.nav-chip {
  display: inline-flex;
  align-items: center;
  padding: 7px 16px;
  border-radius: 999px;
  border: 1px solid var(--front-border);
  background: rgba(255, 255, 255, 0.88);
  color: var(--ink-600);
  font-family: var(--font-sans);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  text-decoration: none;
  transition: background 0.2s, color 0.2s, border-color 0.2s, box-shadow 0.2s;
  white-space: nowrap;
  backdrop-filter: blur(4px);
}

.nav-chip:hover {
  background: var(--paper-100);
  color: var(--forest-700);
  border-color: var(--forest-600);
  box-shadow: 0 2px 8px rgba(20, 80, 55, 0.10);
}

/* 翠玉绿主按钮 chip */
.nav-chip--accent {
  background: var(--forest-700);
  color: #fff;
  border-color: var(--forest-700);
  font-weight: 600;
  box-shadow: 0 2px 10px rgba(31, 140, 105, 0.22);
}

.nav-chip--accent:hover {
  background: var(--forest-800);
  border-color: var(--forest-800);
  box-shadow: 0 4px 16px rgba(31, 140, 105, 0.30);
}

/* 响应式 */
// 触屏热区：chip 撑到 40px（coarse 指针才生效）
@include coarse-pointer {
  .nav-chip {
    min-height: 40px;
  }
}

@include respond-to(md) {
  .hero-banner {
    padding: 36px 24px 32px;
    border-radius: 14px;
    background-size: 80% auto, 100% 100%;
    gap: 20px;
  }

  .hero-banner::before {
    top: 14px;
    right: 16px;
  }

  .banner-nav {
    gap: 6px;
  }

  .nav-chip {
    font-size: 12px;
    padding: 6px 13px;
  }
}

@include respond-to(xs) {
  .hero-banner {
    padding: 28px 18px 26px;
    background-image: linear-gradient(150deg, var(--paper-50) 0%, var(--forest-50) 100%);
    background-size: 100% 100%;
  }

  .hero-banner::before {
    display: none;
  }
}
</style>
