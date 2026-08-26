import { computed, ref } from 'vue'

/**
 * 视口状态（唯一事实来源），替换各组件手写的 resize + innerWidth 判断。
 *
 * isMobile 阈值与 _mixins.scss 的 $bp-md(768px) 对齐：< 768 视为移动端。
 * 用法：const { isMobile, isTablet, isDesktop, width, height } = useViewport()
 *
 * 实现为模块级共享单例：多处调用共享同一组监听器（应用生命周期内存活，开销可忽略），
 * 组件卸载无需手动清理。
 */

// 与 _mixins.scss 断点对齐（max-width: 768px 在小数视口下会误命中，取 767.98px）
const MQ_MOBILE = '(max-width: 767.98px)'
const MQ_TABLET = '(min-width: 768px) and (max-width: 1023.98px)'

const isMobile = ref(false)
const isTablet = ref(false)
const width = ref(0)
const height = ref(0)

let initialized = false

function init() {
  if (initialized || typeof window === 'undefined') {
    return
  }
  initialized = true

  const sync = () => {
    width.value = window.innerWidth
    height.value = window.innerHeight
  }

  const mqlMobile = window.matchMedia(MQ_MOBILE)
  const mqlTablet = window.matchMedia(MQ_TABLET)

  const onChange = () => {
    isMobile.value = mqlMobile.matches
    isTablet.value = mqlTablet.matches
    sync()
  }

  onChange()
  sync()

  // 老版本 Safari（< 14）无 addEventListener，回退 addListener
  for (const mql of [mqlMobile, mqlTablet]) {
    if (typeof mql.addEventListener === 'function') {
      mql.addEventListener('change', onChange)
    } else if (typeof mql.addListener === 'function') {
      mql.addListener(onChange)
    }
  }
  // matchMedia 只在跨断点时触发；同断点内尺寸变化靠 resize 兜底（供地图等按像素布局的场景）
  window.addEventListener('resize', onChange)
}

export function useViewport() {
  init()

  const isDesktop = computed(() => !isMobile.value && !isTablet.value)

  return {
    isMobile,
    isTablet,
    isDesktop,
    width,
    height
  }
}
