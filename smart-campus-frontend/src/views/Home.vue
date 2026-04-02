<template>
  <div class="home-container front-page">
    <Header />

    <div class="main-content">
      <MapContainer />
      <RecommendedShareSection />
      <ActivitySection />
      <RecommendedRouteSection />
    </div>

    <AnnouncementSection />

    <Footer />
  </div>
</template>

<script setup>
import Header from '@/components/common/Header.vue'
import Footer from '@/components/common/Footer.vue'
import MapContainer from '@/components/map/MapContainer.vue'
import AnnouncementSection from '@/components/announcement/AnnouncementSection.vue'
import RecommendedShareSection from '@/components/recommendation/RecommendedShareSection.vue'
import ActivitySection from '@/components/activity/ActivitySection.vue'
import RecommendedRouteSection from '@/components/route/RecommendedRouteSection.vue'
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
</style>
