// ============================================================
// POI 分类符号系统
// 分类名（后端动态聚合、管理员可重命名，不可枚举）→ {分类色 + 图标} 的映射
// 采用「关键词匹配 + 默认兜底」策略
// 色值与 src/assets/styles/public-theme.scss 的 --cat-* / --forest-* 保持同步
// ============================================================

// --- 内联线性 SVG 图标库（24x24 viewBox，stroke="currentColor" 风格，与 Header/Footer 一致）---
// 每项为 SVG 内部 markup（不含 <svg> 外壳），由调用方按需包裹
export const POI_ICONS = {
  // 山峰 —— 观景
  view: '<path d="M2 20 9 8l4 6 3-4 6 10z" fill="none" stroke="currentColor" stroke-width="2" stroke-linejoin="round"/>',
  // 碗 + 热气 —— 美食
  food: '<path d="M4 11h16a8 8 0 0 1-16 0z" fill="none" stroke="currentColor" stroke-width="2" stroke-linejoin="round"/><path d="M8 7c0-2 0-3 1-4M12 7c0-2 0-3 1-4" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round"/>',
  // 帐篷 —— 营地
  camp: '<path d="M3 20 12 5l9 15z" fill="none" stroke="currentColor" stroke-width="2" stroke-linejoin="round"/><path d="M12 5v15" stroke="currentColor" stroke-width="1.6"/>',
  // 床 —— 住宿
  stay: '<path d="M3 8h14v8H3z" fill="none" stroke="currentColor" stroke-width="2" stroke-linejoin="round"/><path d="M17 11h3a1 1 0 0 1 1 1v4" fill="none" stroke="currentColor" stroke-width="2" stroke-linejoin="round"/><path d="M3 16v2M20 16v2" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>',
  // 蜿蜒步道 —— 徒步
  trail: '<path d="M4 20c4 0 5-4 8-4s4 4 8 4" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"/><path d="M4 14c4 0 5-4 8-4s4 4 8 4" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" opacity="0.55"/>',
  // 书本 —— 文化
  culture: '<path d="M5 4h12v16H7a2 2 0 0 1-2-2z" fill="none" stroke="currentColor" stroke-width="2" stroke-linejoin="round"/><path d="M9 8h5M9 12h5" stroke="currentColor" stroke-width="1.6" stroke-linecap="round"/>',
  // 五角星 —— 特色
  special: '<path d="M12 3l2.6 5.6 6.1.7-4.5 4.2 1.2 6L12 16.8 6.6 19.5l1.2-6L3.3 9.3l6.1-.7z" fill="none" stroke="currentColor" stroke-width="2" stroke-linejoin="round"/>',
  // 定位针 —— 默认兜底（与 Header 品牌 mark 一致）
  default: '<path d="M12 22s7-7 7-12a7 7 0 1 0-14 0c0 5 7 12 7 12z" fill="none" stroke="currentColor" stroke-width="2" stroke-linejoin="round"/><circle cx="12" cy="10" r="2.5" fill="none" stroke="currentColor" stroke-width="2"/>'
}

// --- 分类映射表（顺序即匹配优先级）---
// keywords 经过收窄以降低误匹配：去除"路/楼/吃/住"等过宽单字
export const POI_SYMBOL_MAP = [
  {
    key: 'view', label: '观景',
    hex: '#1aa67e', cssVar: 'var(--cat-view)', iconKey: 'view',
    keywords: ['观景', '景观', '风景', '瞭望', '视野', '观景台', '景']
  },
  {
    key: 'food', label: '美食',
    hex: '#ec5a36', cssVar: 'var(--cat-food)', iconKey: 'food',
    keywords: ['餐', '食', '饭', '味', '咖啡', '咖', '茶', '饮', '厨', '烤', '美食', '吧', '厅']
  },
  {
    key: 'camp', label: '营地',
    hex: '#d9923e', cssVar: 'var(--cat-camp)', iconKey: 'camp',
    keywords: ['营', '帐篷', '扎营', '露营', 'camp']
  },
  {
    key: 'stay', label: '住宿',
    hex: '#3a9bd2', cssVar: 'var(--cat-stay)', iconKey: 'stay',
    keywords: ['住宿', '宿', '宾馆', '酒店', '旅店', '旅', '民宿', '客栈', '旅馆', '舍', '寓', 'hotel', 'hostel']
  },
  {
    key: 'trail', label: '步道',
    hex: '#9b7adb', cssVar: 'var(--cat-trail)', iconKey: 'trail',
    keywords: ['步道', '径', '徒步', '栈道', '绿道', '小径', 'trail']
  },
  {
    key: 'culture', label: '文化',
    hex: '#d4537e', cssVar: 'var(--cat-culture)', iconKey: 'culture',
    keywords: ['文', '书', '史', '博', '纪念', '学', '堂', '院', '艺术', '展', '寺', '庙', '教堂']
  },
  {
    key: 'special', label: '特色',
    hex: '#d5b53c', cssVar: 'var(--cat-special)', iconKey: 'special',
    keywords: ['特色', '打卡', '网红', '地标', '标志', '特殊', 'special']
  }
]

// 默认兜底符号
export const DEFAULT_POI_SYMBOL = {
  key: 'default', label: '地点',
  hex: '#1f8c69', cssVar: 'var(--forest-700)', iconKey: 'default',
  iconSvg: POI_ICONS.default
}

// --- 工具：hex → rgba（badge 浅底色用，避免依赖 color-mix 兼容性）---
function hexToRgba(hex, alpha) {
  const h = String(hex).replace('#', '')
  const r = parseInt(h.slice(0, 2), 16)
  const g = parseInt(h.slice(2, 4), 16)
  const b = parseInt(h.slice(4, 6), 16)
  return `rgba(${r}, ${g}, ${b}, ${alpha})`
}

// --- 核心：根据分类名解析符号 ---
// 返回 { key, label, hex, cssVar, iconKey, iconSvg }
export function resolvePoiSymbol(category) {
  const name = String(category || '').trim().toLowerCase()
  if (!name) return { ...DEFAULT_POI_SYMBOL }

  for (const item of POI_SYMBOL_MAP) {
    const hit = item.keywords.some((kw) => name.includes(String(kw).toLowerCase()))
    if (hit) {
      return { ...item, iconSvg: POI_ICONS[item.iconKey] }
    }
  }
  return { ...DEFAULT_POI_SYMBOL }
}

// --- 渲染完整 <svg> 字符串（供 marker content / badge v-html 使用）---
export function renderPoiIconSvg(symbol, { size = 14, color = 'currentColor', strokeWidth = null } = {}) {
  const strokeStyle = strokeWidth ? ` stroke-width="${strokeWidth}"` : ''
  return `<svg xmlns="http://www.w3.org/2000/svg" width="${size}" height="${size}" viewBox="0 0 24 24" style="color:${color}"${strokeStyle}>${symbol.iconSvg}</svg>`
}

// --- badge 浅底色（分类色 + 透明度）---
export function getPoiBadgeSoftBg(symbol, alpha = 0.14) {
  return hexToRgba(symbol.hex, alpha)
}
