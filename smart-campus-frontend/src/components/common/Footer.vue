<template>
  <footer class="site-footer">
    <div class="front-shell footer-shell">
      <section class="footer-hero front-panel">
        <div class="footer-brand">
          <span class="front-kicker">地点探索与分享</span>
          <h2 class="front-title">把地点、体验和路线组织成可以持续分享的发现系统</h2>
          <p class="front-description">
            从地图上的一个点开始，延展到分享、活动、推荐路线和公共公告，让探索变成一套完整的地点内容体验。
          </p>
        </div>

        <div class="footer-actions">
          <router-link to="/" class="footer-link-pill">回到探索首页</router-link>
          <router-link v-if="userStore.isLoggedIn" to="/settings" class="footer-link-pill">进入个人中心</router-link>
          <router-link v-if="userStore.isSuperAdmin" to="/admin/overview" class="footer-link-pill">进入运营后台</router-link>
        </div>
      </section>

      <div class="footer-grid">
        <section class="footer-column">
          <h3>探索能力</h3>
          <p>地点浏览、分类筛选、路线串联、地图定位与内容发现。</p>
        </section>

        <section class="footer-column">
          <h3>分享互动</h3>
          <p>地点打卡、图片分享、推荐内容、活动信息与社区互动。</p>
        </section>

        <section class="footer-column">
          <h3>项目标识</h3>
          <p>清忧@凡辰</p>
        </section>
      </div>
    </div>
  </footer>
</template>

<script setup>
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
</script>

<style scoped>
.site-footer {
  padding: 24px 0 30px;
}

.footer-shell {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.footer-hero {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 18px;
  padding: 24px 26px;
  border-radius: var(--front-radius-xl);
}

.footer-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.footer-link-pill {
  min-height: 38px;
  padding: 0 14px;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  color: var(--front-accent-strong);
  text-decoration: none;
  font-size: 13px;
  font-weight: 700;
  background: var(--front-accent-soft);
}

.footer-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.footer-column {
  padding: 18px 20px;
  border-radius: 22px;
  border: 1px solid var(--front-border);
  background: rgba(255, 255, 255, 0.7);
}

.footer-column h3 {
  margin: 0 0 10px;
  color: var(--front-text);
  font-size: 15px;
}

.footer-column p {
  margin: 0;
  color: var(--front-text-soft);
  font-size: 13px;
  line-height: 1.75;
}

@media (max-width: 960px) {
  .footer-hero {
    flex-direction: column;
    align-items: stretch;
  }

  .footer-actions {
    justify-content: flex-start;
  }

  .footer-grid {
    grid-template-columns: 1fr;
  }
}
</style>
