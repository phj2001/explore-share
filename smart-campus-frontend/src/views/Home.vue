<template>
  <div class="home-container">
    <Header />

    <div class="main-content">
      <MapContainer />
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
</script>

<style scoped>
.home-container {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
}

.main-content {
  flex: 1;
}
</style>
