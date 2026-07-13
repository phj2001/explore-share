<template>
  <span class="poi-cat-badge" :class="[`poi-cat-badge--${size}`]" :style="badgeStyle">
    <span class="poi-cat-badge__icon" v-html="iconSvg" />
    <span class="poi-cat-badge__text">{{ category || '未分类' }}</span>
  </span>
</template>

<script setup>
import { computed } from 'vue'
import { resolvePoiSymbol, renderPoiIconSvg, getPoiBadgeSoftBg } from '@/utils/poiSymbol'

const props = defineProps({
  category: { type: String, default: '' },
  size: { type: String, default: 'default' } // 'default' | 'small'
})

const symbol = computed(() => resolvePoiSymbol(props.category))

const iconSvg = computed(() => {
  const iconSize = props.size === 'small' ? 11 : 13
  return renderPoiIconSvg(symbol.value, { size: iconSize, color: symbol.value.hex })
})

const badgeStyle = computed(() => ({
  backgroundColor: getPoiBadgeSoftBg(symbol.value, 0.14),
  color: symbol.value.hex
}))
</script>

<style scoped>
.poi-cat-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  border-radius: 999px;
  font-weight: 600;
  letter-spacing: 0.02em;
  white-space: nowrap;
  line-height: 1;
  vertical-align: middle;
}

.poi-cat-badge--default {
  padding: 5px 10px;
  font-size: 12px;
}

.poi-cat-badge--small {
  padding: 2px 7px;
  font-size: 10.5px;
}

.poi-cat-badge__icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.poi-cat-badge__icon :deep(svg) {
  display: block;
}

.poi-cat-badge__text {
  font-family: var(--font-sans, inherit);
}
</style>
