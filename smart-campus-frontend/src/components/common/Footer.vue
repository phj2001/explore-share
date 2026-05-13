<template>
  <footer class="site-footer">
    <div class="front-shell footer-line">
      <span class="footer-credit">清忧@凡辰 · 地点探索与分享</span>
      <nav class="footer-nav">
        <router-link to="/">首页</router-link>
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

<style scoped>
.site-footer {
  padding: 18px 0;
  border-top: 1px solid var(--front-border);
  margin-top: 12px;
}

.footer-line {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}

.footer-credit {
  font-size: 13px;
  color: var(--front-text-muted);
}

.footer-nav {
  display: flex;
  align-items: center;
  gap: 16px;
}

.footer-nav a {
  font-size: 13px;
  color: var(--front-text-muted);
  text-decoration: none;
  transition: color 0.15s;
}

.footer-nav a:hover {
  color: var(--front-accent-strong);
}

@media (max-width: 480px) {
  .footer-line {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }
}
</style>
