<template>
  <div class="home-container front-page">
    <Header />

    <div class="main-content">
      <section id="map-section">
        <MapContainer />
      </section>

      <section id="recommended-share-section">
        <LazyMount min-height="520px">
          <RecommendedShareSection />
        </LazyMount>
      </section>

      <section id="activity-section">
        <LazyMount min-height="520px">
          <ActivitySection />
        </LazyMount>
      </section>

      <section id="recommended-route-section">
        <LazyMount min-height="520px">
          <RecommendedRouteSection />
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
  gap: 22px;
  padding: 18px 0 0;
}

@media (max-width: 768px) {
  .main-content {
    gap: 16px;
    padding-top: 12px;
  }
}

@media (max-width: 480px) {
  .main-content {
    gap: 14px;
    padding-top: 10px;
  }
}
</style>
