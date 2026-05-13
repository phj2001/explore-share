<template>
  <div class="home-container front-page">
    <Header />

    <div class="main-content">
      <section id="map-section">
        <MapContainer />
      </section>

      <section id="recommended-share-section" class="section-band">
        <LazyMount min-height="320px">
          <RecommendedShareSection />
        </LazyMount>
      </section>

      <section id="activity-section" class="section-band section-band--warm">
        <LazyMount min-height="320px">
          <ActivitySection />
        </LazyMount>
      </section>

      <section id="recommended-route-section" class="section-band section-band--teal">
        <LazyMount min-height="320px">
          <RecommendedRouteSection />
        </LazyMount>
      </section>

      <section id="leaderboard-section" class="section-band section-band--gray">
        <LazyMount min-height="360px">
          <LeaderboardSection />
        </LazyMount>
      </section>

      <section id="feed-section" class="section-band">
        <LazyMount min-height="300px">
          <FeedSection />
        </LazyMount>
      </section>

      <section id="user-route-section" class="section-band section-band--amber">
        <LazyMount min-height="300px">
          <UserRouteSection />
        </LazyMount>
      </section>
    </div>

    <section id="announcement-section">
      <LazyMount min-height="0" root-margin="0px">
        <AnnouncementSection />
      </LazyMount>
    </section>

    <Footer />
  </div>
</template>

<script setup>
import { defineAsyncComponent } from 'vue'
import LazyMount from '@/components/common/LazyMount.vue'
import Header from '@/components/common/Header.vue'
import Footer from '@/components/common/Footer.vue'
import MapContainer from '@/components/map/MapContainer.vue'

const AnnouncementSection = defineAsyncComponent(() => import('@/components/announcement/AnnouncementSection.vue'))
const RecommendedShareSection = defineAsyncComponent(() => import('@/components/recommendation/RecommendedShareSection.vue'))
const ActivitySection = defineAsyncComponent(() => import('@/components/activity/ActivitySection.vue'))
const RecommendedRouteSection = defineAsyncComponent(() => import('@/components/route/RecommendedRouteSection.vue'))
const LeaderboardSection = defineAsyncComponent(() => import('@/components/leaderboard/LeaderboardSection.vue'))
const FeedSection = defineAsyncComponent(() => import('@/components/feed/FeedSection.vue'))
const UserRouteSection = defineAsyncComponent(() => import('@/components/route/UserRouteSection.vue'))
</script>

<style scoped>
.home-container {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
}

.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.section-band {
  padding: 4px 0;
}

.section-band--warm  { background: rgba(245, 158, 11, 0.04); }
.section-band--teal  { background: rgba(20, 184, 166, 0.04); }
.section-band--gray  { background: rgba(15, 23, 42, 0.025); }
.section-band--amber { background: rgba(251, 191, 36, 0.06); }
</style>
