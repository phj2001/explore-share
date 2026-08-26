<template>
  <footer class="site-footer">
    <div class="front-shell footer-inner">
      <div class="footer-brand">
        <span class="footer-mark">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M12 22s-8-7.5-8-13a8 8 0 1 1 16 0c0 5.5-8 13-8 13Z"/>
            <circle cx="12" cy="9" r="2.5"/>
          </svg>
        </span>
        <span class="footer-credit">地点探索与分享</span>
        <span class="footer-dot">·</span>
        <span class="footer-coord">清忧@凡辰</span>
      </div>

      <nav class="footer-nav">
        <router-link to="/">探索首页</router-link>
        <router-link v-if="userStore.isLoggedIn" to="/settings">个人中心</router-link>
        <router-link v-if="userStore.isSuperAdmin" to="/admin/overview">运营后台</router-link>
      </nav>
    </div>
  </footer>
</template>

<script setup>
import { useUserStore } from '@/stores/user'
const userStore = useUserStore()
</script>

<style scoped lang="scss">
.site-footer {
  padding: 16px 0;
  border-top: 1px solid var(--front-border);
  margin-top: 8px;
  background: rgba(247,250,247,0.7);
}

.footer-inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}

.footer-brand {
  display: flex;
  align-items: center;
  gap: 8px;
}

.footer-mark {
  width: 26px;
  height: 26px;
  border-radius: 7px;
  background: var(--forest-700);
  color: #fff;
  display: grid;
  place-items: center;
  flex-shrink: 0;
}

.footer-credit {
  font-size: 13px;
  font-weight: 500;
  color: var(--ink-700);
}

.footer-dot {
  color: var(--ink-300);
  font-size: 12px;
}

.footer-coord {
  font-family: var(--font-mono);
  font-size: 10.5px;
  letter-spacing: 0.08em;
  color: var(--ink-400);
}

.footer-nav {
  display: flex;
  align-items: center;
  gap: 4px;
}

.footer-nav a {
  padding: 5px 10px;
  border-radius: 6px;
  font-size: 12.5px;
  color: var(--ink-500);
  text-decoration: none;
  transition: background 0.15s, color 0.15s;
}

.footer-nav a:hover {
  background: var(--forest-50);
  color: var(--forest-700);
}

// 触屏热区：页脚链接撑到 40px（coarse 指针才生效，桌面鼠标不受影响）
@include coarse-pointer {
  .footer-nav a {
    display: inline-flex;
    align-items: center;
    min-height: 40px;
  }
}

@include respond-to(xs) {
  .footer-inner {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }
  .footer-coord { display: none; }
}
</style>
