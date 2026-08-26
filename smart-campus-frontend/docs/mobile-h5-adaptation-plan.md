# 移动端 H5 适配实施计划

> 定位：**C 端用户可达界面 = 移动优先打磨；Admin 管理后台 = 兜底级（能用不破版）**
> 创建：2026-08-22　｜　状态：**全部 7 阶段已完成（2026-08-24）**　｜　原则：不影响既有桌面端与其他模块，每阶段构建验证
> 待办：部署后在 https://map.jieai.shop 做带数据的线上回归（C 端逐页 + 抽样截图对比基线）

---

## 1. 背景

本项目是社区分享型平台（地点探索/分享/路线），C 端主要访问场景是手机（游玩途中拍照、分享、评论、看路线）。
现状为"桌面优先 + 各组件零散 max-width 降级"，存在断点碎片化（19 种断点值）、JS 移动检测三处自建阈值不一、
弹窗固定 px 宽度溢出、100vh 无 dvh 回退、小字号（73 处 <12px）、触控热区偏小（48 处紧凑按钮）等问题。

## 2. 最终目标与验收标准

**最终目标**：用户用手机（375~430px 宽）访问所有 C 端页面时，界面自适应、文字可读、按钮好点、要素不重叠不溢出、
移动特有交互（软键盘、相机上传、地图手势）正常。

**验收标准（C 端逐页检查）**：
- [ ] 375px / 390px / 768px 三档宽度下无横向滚动破版、无要素重叠
- [ ] 正文/按钮字号 ≥ 12px（装饰性 mono 小字允许 10.5px 但需保证对比度）
- [ ] 可点击元素热区 ≥ 40×40px（核心操作 ≥ 44px）
- [ ] 所有弹窗宽度自适应：`min(92vw, 原宽度)`，不超出视口
- [ ] 全屏/近全屏容器（地图、AI 助手）高度用 dvh 回退，iOS 地址栏收放不裁切内容
- [ ] 刘海屏安全区：viewport-fit=cover + 底部固定元素用 safe-area-inset
- [ ] 软键盘弹出不遮挡输入框、不顶飞固定底栏
- [ ] 分享/头像上传在手机上可调用相机，大图前端压缩
- [ ] 构建通过，桌面端（≥1280px）视觉与现状一致

**Admin 验收（兜底级）**：分页条不溢出、页面容器不横向破版即可，不做热区/字号精修。

## 3. 范围

### C 端（移动优先，9 处）
| 页面/区域 | 关键文件 |
|---|---|
| Header 导航 + 移动入口恢复 | `components/common/Header.vue` |
| 首页地图区 | `components/map/MapContainer.vue`、`RoutePolyline.vue` |
| 地点详情弹窗 | `components/map/PoiDetailDialog.vue` |
| 分享发布 | `components/map/PoiSharePanel.vue` |
| 动态流（分享卡片/评论） | `components/feed/FeedSection.vue`、`FeedItem.vue` |
| 路线创建 | `views/RouteCreate.vue` |
| 路线详情 | `views/UserRouteDetail.vue`、`components/route/*` |
| 个人主页 / 设置 / 头像裁剪 | `views/UserProfile.vue`、`views/Settings.vue`、`components/user/AvatarCropperDialog.vue` |
| 登录注册 / AI 助手 / 通知 / 公告 / 活动 / 页脚 | `views/Login.vue`、`components/assistant/AssistantChat.vue`、`common/NotificationBell.vue`、`announcement/AnnouncementSection.vue`、`activity/ActivitySection.vue`、`common/Footer.vue`、`common/HeroBanner.vue` |

### Admin（仅兜底 2 项）
- 分页条窄屏换行/简化 layout（13 个列表页统一）
- 页面容器防横向破版

### 明确不做（防蔓延）
- 不做 Admin 表格卡片化、不做 Admin 触控/字号精修
- 不重构现有 desktop-first 写法为 mobile-first（保留现有 max-width 模式，只收敛断点值）
- 不引入 UI 新框架/大依赖；PWA 为可选独立项

## 4. 技术规范（唯一事实来源）

### 4.1 断点 token（5 档收敛）
| token | 值 | 语义 | 替代的旧值 |
|---|---|---|---|
| `$bp-xs` | 480px | 小手机 | 480/520 |
| `$bp-sm` | 640px | 大手机 | 560/640 |
| `$bp-md` | 768px | 平板竖屏 / 移动态切换线 | **760/720/860/900/960**（就近归并） |
| `$bp-lg` | 1024px | 平板横屏 | 1024/1080/1100/1120 |
| `$bp-xl` | 1280px | 小桌面 | 1180/1200/1240/1280/1440 |

- 定义于 `src/assets/styles/_mixins.scss`，经 vite `css.additionalData` 全局注入，组件内直接 `@include respond-to(md)`。
- 旧断点随各组件改造**就近归并**，不要求一次性全量替换。

### 4.2 useViewport composable（`src/composables/useViewport.js`）
```js
const { isMobile, isTablet, isDesktop, width, height } = useViewport()
// isMobile: width < 768（与 $bp-md 一致）；matchMedia 实现，组件卸载自动清理
```
替换 AnnouncementSection(900)/MapContainer(768)/RoutePolyline(768) 三处手写 resize 监听。

### 4.3 弹窗宽度规范
所有 C 端 `el-dialog`：`width="min(92vw, 原px)"`；全局兜底 `.el-dialog { max-width: calc(100vw - 24px) }`。

### 4.4 视口高度 / 安全区
- 近全屏高度：`height: calc(100vh - X)` 后跟一行 `height: calc(100dvh - X)` 回退。
- `viewport` meta 补 `viewport-fit=cover`；底部固定元素统一 `padding-bottom: max(现值, env(safe-area-inset-bottom))`。

### 4.5 热区与字号
- `@media (pointer: coarse)` 下核心交互热区 ≥ 44px；次要 ≥ 40px。
- C 端字号底线 12px；`10~11px` 仅保留装饰用途并核对对比度。

### 4.6 验证方式
- 每阶段：`npm run build` 通过 + chunk 无异常膨胀。
- 每页改造后：dev server + 浏览器 375/390/768 截图自检（溢出/重叠/热区）。

## 5. 阶段任务分解

### 阶段 0 · 基础设施（不改业务组件行为）
1. `index.html`：viewport 加 `viewport-fit=cover`；Google Fonts 加 `&display=swap` + preconnect（国内访问止血）
2. 新建 `src/assets/styles/_mixins.scss`（断点 token + respond-to mixin）
3. `vite.config.js`：`css.preprocessorOptions.scss.additionalData` 全局注入 mixins
4. 新建 `src/composables/useViewport.js`
5. `index.scss`：全局 `.el-dialog` / `.el-message-box` max-width 兜底
- 完成标准：build 通过；现有页面渲染无变化。

### 阶段 1 · Header + 首页骨架
- Header：移动端恢复导航入口（"更多"抽屉：首页/个人中心/后台管理/退出），热区 ≥44px，断点归并（1100/860/640/480 → lg/md/sm/xs）
- Footer / HeroBanner 断点归并 + 字号收敛
- 完成标准：375px 下导航全部入口可达、无溢出。

### 阶段 2 · 首页地图区（核心）
- MapContainer / RoutePolyline：接入 useViewport；100vh→dvh 回退；移动端面板/浮层不重叠；地图手势与页面滚动不冲突（touch-action 实测调优）
- PoiDetailDialog：宽度 min() 化、内容区移动端可滚动
- 完成标准：375px 下地图可用、面板层叠有序、地址栏收放不裁切。

### 阶段 3 · 分享发布 + 动态流/评论
- PoiSharePanel：弹窗宽度 min() 化；上传支持调用相机（`accept="image/*"`）+ 前端压缩；软键盘弹出处理
- FeedSection / FeedItem：卡片自适应、图片懒加载、操作按钮热区
- 完成标准：手机上完成"拍照→发布"全流程无障碍。

### 阶段 4 · 路线创建 / 详情
- RouteCreate：窄屏分步/单列布局、表单控件 100% 宽
- UserRouteDetail / RecommendedRouteSection(760px 弹窗) / UserRouteCard：弹窗 min() 化、卡片自适应
- 完成标准：375px 下可完整创建/浏览路线。

### 阶段 5 · 个人中心 / 设置 / 登录 / 其余 C 端
- Settings / UserProfile / Login / AvatarCropperDialog(560px) / NotificationBell / AnnouncementSection(900→768 统一) / ActivitySection(760px 弹窗) / AssistantChat(dvh+安全区复核)
- 完成标准：C 端 9 处全部过验收标准。

### 阶段 6 · Admin 兜底 + 性能 + 可选 PWA
- Admin：分页条 wrap（13 页统一小改动）；容器防破版
- 性能：字体加载策略复核（swap 已加，视效果决定是否本地子集化）；分享图片懒加载补全
- 可选：PWA manifest + 图标 + 主题色（独立小项，可裁）
- 完成标准：Admin 两项兜底落地；全量构建 + 抽样截图回归。

## 6. 进度跟踪

| 阶段 | 状态 | 备注 |
|---|---|---|
| 0 基础设施 | ✅ 完成（2026-08-22） | viewport-fit=cover 已加；字体 display=swap+preconnect 原本已存在；_mixins.scss + additionalData 注入无模块循环；useViewport 单例就绪；el-dialog/.el-message-box max-width 兜底已加。build 10.88s 通过，本地 dev 桌面 1280 与线上基线渲染一致 |
| 1 Header+骨架 | ✅ 完成（2026-08-22） | Header：汉堡菜单+el-drawer 抽屉（用户信息/探索首页/个人中心/后台管理/退出或登录）、热区（菜单44/普通40/菜单项48）、断点 1100/860/640/480→lg/md/sm/xs。375px 布局无溢出重叠（截图+DOM 双核对）、抽屉打开态入口齐全、桌面 1280 视觉不变（mobile-actions 隐藏、无溢出）。Footer：scss 化+respond-to(xs)+coarse-pointer 链接热区 40px，375 验证 column 布局生效。HeroBanner：断点归并+坐标字 10→10.5px+chip 热区 40px（注：该组件当前未被任何页面引用，改造编译通过、无可见影响）。顺路修复 _mixins.scss map-get→map.get，构建弃用警告清零。build 7.10s 通过 |
| 2 首页地图区 | ✅ 完成（2026-08-22） | MapContainer：接入 useViewport（fitView padding/移动路线面板状态改响应式，删手写 resize 监听）、切桌面自动收起移动面板、断点 768/480→md/xs、地图高度 svh+vh 双写（375=58svh、768=62svh 实测吻合）、map-toolbar 窄屏左右锚定+wrap 防溢出。RoutePolyline：同样接入 useViewport（删手写监听整块）、route-panel 高度 dvh+vh 双写、移动面板 60/72svh+vh 双写、768→md、功能性 10px 字号→11px（7 处）/坐标 mono→10.5px、route-actions 热区 34→40px、circle 30→40px、coarse-pointer 头部/选点横幅按钮撑 40px。PoiDetailDialog：scss 化、640/768/480→sm/md/xs、全屏高度与 body max-height 补 vh 回退、coarse-pointer 评分星撑 40px（视觉尺寸不变）。验证：375/768/1280 DOM 断言（无横向溢出、热区 44/40 达标、桌面 calc(100vh-96px) 不变）、编译 CSS 断言（md/sm/xs/coarse 规则全部正确展开）、build 7.45s 无警告。带数据回归待部署后线上进行 |
| 3 分享+动态流 | ✅ 完成（2026-08-22） | PoiSharePanel：上传 accept 放宽 image/*（手机弹相机/相册）、新建 `utils/imageCompress.js` 前端压缩管线（最长边 1920/JPEG 0.85/PNG 保型/512KB 跳过/异常原样返回），compressionTasks Set+`__compressed` 幂等标记+发布前 allSettled 等待（防竞态：压缩中被删图/重复重编码）、压缩后 >5MB 移除提示（对齐后端）、软键盘 focus 后 300ms scrollIntoView 居中、图片预览/举报弹窗 min(92vw)化、900/640→md/sm、coarse-pointer 热区 40px+输入框 16px 防 iOS 缩放。FeedSection：scss 化+sm+coarse 热区。FeedItem：el-image lazy（页面级滚动）、xs 断点、feed-poi 热区 40px。验证：375 无横向溢出（369/369）、login-hint column 生效、768/1280 无溢出且恢复桌面布局、PoiSharePanel 异步组件 CSS 直接从 dev server 拉编译产物断言（md/sm/coarse/40px/16px/旧 900px 清除 全过）、build 7.15s 无警告（PoiSharePanel chunk 20.2KB 正常）。拍照→发布全流程线上回归待部署 |
| 4 路线创建/详情 | ✅ 完成（2026-08-22） | RouteCreate：scss 化+100dvh 双写、md 单列（地图 50vh/50svh+320px 兜底、地图 static、hint-desktop/hint-mobile 文案切换、waypoints-list 放开滚动）、sm（panel 18px、地图 42vh/42svh）、coarse（提交 44px/其余 40px、输入 16px 防 iOS 缩放）；UserRouteDetail：scss 化+100dvh、sm 归并（hero 纵排+h1 22px+按钮 flex:1+地图 280px）、coarse（赞/收藏 44px、wp-link 40px inline-flex）；RecommendedRouteSection：dialog min(92vw,760px)、xl 两列/md 单列+timeline 按钮掉行修正（grid-column:2）/sm 紧凑、coarse 热区；UserRouteSection：md 两列/sm 单列+coarse；UserRouteCard：scss 化（整卡 router-link 大热区，无额外断点需求）。验证：/route/create 390/768/1280 三档 DOM 断言全过（单双栏切换/svh/dvh/sticky/文案切换）；/route/1 无溢出+dvh+640 归并+coarse ✓；三组件编译 CSS 断言（md/sm/xl/coarse 存在、旧 900/760/560 清零）✓（UserRouteSection/Card 因 LazyMount 异步未注入 styleSheets，改用 dev server 产物直取法）；build 7.27s 无警告。线上回归待部署 |
| 5 个人中心等 | ✅ 完成（2026-08-24） | 11 文件改造+编译级 CSS 断言全绿：Settings（断点 1080/720/560→lg/md/sm、**修复 tab-grid sm 未单列化真破版**（384px 下两卡各 153px→326px 满宽）、coarse 44/40/16px+tabs 手势横滑替代箭头）；UserProfile（md/xs、stats-bar xs 2 列、hero 纵排，**补 coarse**：关注 44/tab/统计/加载更多 40）；Login（960/520→md/xs、coarse 44+16px，384px 实测无横向滚动、按钮 44px、卡片满宽）；AvatarCropperDialog（720→md、coarse 44）；ActivitySection（1200/760/560→xl/md/sm、coarse 40+44）；AnnouncementSection（900/560→md/sm、coarse 触发器/footer 44）；LeaderboardSection（900→md/sm、coarse tab 40）；RecommendedShareSection（1100/720/560→lg/md/sm、coarse 刷新 40）；POIApplicationDialog（sm+地图 200px、coarse 16px）；AssistantChat（dvh+安全区+fab 44+16px 复核全过）；NotificationBell（xs+dvh60+铃铛 40）。DOM 实测：/login 384px 与 /settings 384px 无溢出（settings 修复前后对比 [153,153]→[326,326]）；/user/1 空态（本地无后端）无溢出，数据态由编译断言覆盖。build 9.09s 无警告。线上回归待部署 |
| 6 Admin兜底+性能 | ✅ 完成（2026-08-24） | **Admin 兜底**（admin-theme.scss 768px 块内全局一处生效于 19 页）：el-pagination flex-wrap+row-gap+justify-end+按钮 30px（防 sizes+多页码溢出，DOM 实测 flexWrap=wrap/btn 30px）、.el-dialog/.el-drawer max-width: calc(100vw-24px)（820/1080px 固定宽弹窗自动收缩）。实测 17 个 Admin 路由（16 列表/概览页+poi/create）369px 全部无横向溢出、无未裁剪溢出元素（nav-item 溢出属 overflow-x:auto 横滑容器内预期）；Admin 侧边栏 1180px 变顶部横滑导航、720px top-bar 纵排为既有能力确认有效。**性能**：C 端图片懒加载补全 4 处（PoiDetailDialog 图集/PoiSharePanel 分享图/UserProfile 分享图/UserRouteCard 封面，均 v-for 列表图+preview 组合安全）；字体 preconnect+display=swap 已达标，本地子集化评估后不做（Google Fonts 按 unicode-range 分片+swap 不阻塞渲染，收益/风险比不划算）；PWA 按计划"可裁"裁掉。**回归**：build 7.97s 无警告；抽样截图 phase6-home-375 / phase6-admin-users-375 / phase6-admin-users-desktop-1280 / phase6-home-desktop-1280（桌面与基线一致）。线上回归待部署 |
| 6 Admin兜底+性能 | ⬜ 未开始 | |

验证基线（改造前）：`baseline-desktop-1280.png` / `baseline-mobile-375.png`（线上部署版截图，2026-08-22）。
移动端本地验证说明：本机无后端，dev server 接口报错属预期，布局骨架仍可验证；带数据的页面到部署环境回归。

## 7. 风险与原则
- **不动桌面端**：所有改动在媒体查询分支或 min()/dvh 渐进增强内，≥1280px 视觉不变；
- **不破其他模块**：全局注入(mixins)与全局样式(dialog max-width)改动后必须 build + 首页回归；
- **就近归并断点**：只改本阶段触及的组件，不搞全量替换；
- Element Plus 全局兜底样式同时作用于 Admin 属预期（"不破版"符合 Admin 兜底定位）。
