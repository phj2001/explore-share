<template>
  <div ref="rootRef" class="lazy-mount" :style="placeholderStyle">
    <slot v-if="mounted" />
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'

const props = defineProps({
  rootMargin: {
    type: String,
    default: '200px 0px'
  },
  minHeight: {
    type: String,
    default: '320px'
  },
  once: {
    type: Boolean,
    default: true
  }
})

const rootRef = ref(null)
const mounted = ref(false)
let observer = null

const placeholderStyle = computed(() => {
  return mounted.value ? undefined : { minHeight: props.minHeight }
})

const stopObserving = () => {
  observer?.disconnect()
  observer = null
}

onMounted(() => {
  if (typeof window === 'undefined' || !rootRef.value) {
    mounted.value = true
    return
  }

  if (!('IntersectionObserver' in window)) {
    mounted.value = true
    return
  }

  observer = new IntersectionObserver(
    (entries) => {
      const entry = entries[0]
      if (!entry?.isIntersecting) {
        return
      }

      mounted.value = true

      if (props.once) {
        stopObserving()
      }
    },
    {
      rootMargin: props.rootMargin,
      threshold: 0.01
    }
  )

  observer.observe(rootRef.value)
})

onBeforeUnmount(() => {
  stopObserving()
})
</script>

<style scoped>
.lazy-mount {
  width: 100%;
}
</style>
