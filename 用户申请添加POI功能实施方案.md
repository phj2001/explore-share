# 用户申请添加 POI 功能实施方案

> 编制时间：2026-04-29
> 核心目标：开放 POI 创建权，让普通用户参与共建地图，通过审核机制保障质量
> 设计原则：复用现有审核流程模式（ContentReport）、通知系统、运营后台框架

---

## 一、方案总览

```
普通用户 → 地图上点击"申请添加地点" → 填写信息 + 地图选点 → 提交申请
                                                              ↓
                                                     进入审核队列（PENDING）
                                                              ↓
                                          管理员后台审核 → 通过（APPROVED）→ 自动创建 POI + 通知用户
                                                         → 驳回（REJECTED）→ 通知用户原因
```

**涉及模块：**

| 层级 | 新增/修改 | 文件 |
|------|----------|------|
| 数据库 | 新增 1 张表 | `poi_applications` |
| Entity | 新增 1 个 | `POIApplication.java` |
| Repository | 新增 1 个 | `POIApplicationRepository.java` |
| Service | 新增 1 对 | `POIApplicationService.java` / `Impl` |
| Controller | 新增 2 个 | `POIApplicationController.java`（用户端）、`AdminPOIApplicationController.java`（管理端） |
| DTO | 新增 4 个 | Request × 2 + Response × 2 |
| 前端 API | 新增 1 个 | `src/api/poiApplication.js` |
| 前端组件 | 新增 1 个 | `src/components/map/POIApplicationDialog.vue` |
| 前端页面 | 新增 1 个 | `src/views/admin/POIApplicationList.vue` |
| 前端修改 | 2 个文件 | `MapContainer.vue`（加按钮）、`router/index.js`（加路由） |
| 后端修改 | 1 个文件 | `SecurityConfig.java`（加权限规则） |

---

## 二、数据库设计

### 2.1 迁移 SQL

```sql
-- POI 申请表
CREATE TABLE IF NOT EXISTS poi_applications (
    id              BIGSERIAL PRIMARY KEY,
    applicant_id    BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name            VARCHAR(100) NOT NULL,
    category        VARCHAR(50) NOT NULL,
    description     TEXT,
    latitude        DECIMAL(10,7) NOT NULL,
    longitude       DECIMAL(10,7) NOT NULL,
    address         VARCHAR(255),
    photo_urls      TEXT,
    status          SMALLINT NOT NULL DEFAULT 1,
    reviewed_by     BIGINT REFERENCES users(id) ON DELETE SET NULL,
    reviewed_at     TIMESTAMP,
    review_note     VARCHAR(500),
    created_poi_id  BIGINT REFERENCES pois(id) ON DELETE SET NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_poi_applications_applicant ON poi_applications(applicant_id);
CREATE INDEX idx_poi_applications_status ON poi_applications(status);
CREATE INDEX idx_poi_applications_created ON poi_applications(created_at DESC);

COMMENT ON TABLE poi_applications IS 'POI 新增申请';
COMMENT ON COLUMN poi_applications.status IS '1=待审核 2=已通过 3=已驳回';
COMMENT ON COLUMN poi_applications.photo_urls IS 'JSON数组，用户上传的现场照片URL';
COMMENT ON COLUMN poi_applications.created_poi_id IS '审核通过后自动创建的POI ID';
```

### 2.2 状态流转

```
用户提交 → PENDING(1) → APPROVED(2) → 自动创建POI，记录created_poi_id
                      → REJECTED(3) → 记录review_note
```

终态不可逆转，审核后不可再次修改状态。

---

## 三、后端设计

### 3.1 Entity

文件：`src/main/java/com/smartcampus/entity/POIApplication.java`

```java
@Data @NoArgsConstructor @AllArgsConstructor
@Entity @Table(name = "poi_applications")
public class POIApplication {

    public static final short STATUS_PENDING  = 1;
    public static final short STATUS_APPROVED = 2;
    public static final short STATUS_REJECTED = 3;

    @Id @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "applicant_id", nullable = false)
    private User applicant;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 50)
    private String category;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(length = 255)
    private String address;

    @Column(columnDefinition = "TEXT")
    private String photoUrls;          // JSON 数组

    @Column(nullable = false)
    private Short status = STATUS_PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "review_note", length = 500)
    private String reviewNote;

    @Column(name = "created_poi_id")
    private Long createdPoiId;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
}
```

### 3.2 Repository

文件：`src/main/java/com/smartcampus/repository/POIApplicationRepository.java`

```java
@Repository
public interface POIApplicationRepository extends JpaRepository<POIApplication, Long> {

    Page<POIApplication> findByApplicantIdOrderByCreatedAtDesc(Long applicantId, Pageable pageable);

    Page<POIApplication> findByStatusOrderByCreatedAtDesc(Short status, Pageable pageable);

    Page<POIApplication> findAllByOrderByCreatedAtDesc(Pageable pageable);

    long countByStatus(Short status);

    long countByApplicantId(Long applicantId);

    boolean existsByApplicantIdAndNameAndStatus(Long applicantId, String name, Short status);

    // 管理端：按状态 + 关键词筛选
    @Query("SELECT a FROM POIApplication a WHERE " +
           "(:status IS NULL OR a.status = :status) AND " +
           "(:keyword IS NULL OR LOWER(a.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(a.category) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<POIApplication> searchByKeywordAndStatus(
            @Param("keyword") String keyword,
            @Param("status") Short status,
            Pageable pageable);
}
```

### 3.3 DTO

**CreatePOIApplicationRequest.java** — 用户提交申请

```java
@Data
public class CreatePOIApplicationRequest {
    @NotBlank(message = "地点名称不能为空")
    @Size(max = 100)
    private String name;

    @NotBlank(message = "分类不能为空")
    @Size(max = 50)
    private String category;

    @Size(max = 1000)
    private String description;

    @NotNull(message = "纬度不能为空")
    private BigDecimal latitude;

    @NotNull(message = "经度不能为空")
    private BigDecimal longitude;

    @Size(max = 255)
    private String address;

    private List<String> photoUrls;  // 最多 3 张
}
```

**ReviewPOIApplicationRequest.java** — 管理员审核

```java
@Data
public class ReviewPOIApplicationRequest {
    @NotNull(message = "审核状态不能为空")
    private Short status;  // 2=通过, 3=驳回

    @Size(max = 500)
    private String reviewNote;
}
```

**POIApplicationResponse.java** — 用户端查看自己的申请

```java
public record POIApplicationResponse(
    Long id,
    String name, String category, String description,
    BigDecimal latitude, BigDecimal longitude,
    String address, String photoUrls,
    Short status, String reviewNote,
    Long createdPoiId,
    LocalDateTime createdAt, LocalDateTime reviewedAt
) {}
```

**AdminPOIApplicationListItemResponse.java** — 管理端列表

```java
public record AdminPOIApplicationListItemResponse(
    Long id,
    Long applicantId, String applicantName, String applicantAvatarUrl,
    String name, String category,
    BigDecimal latitude, BigDecimal longitude,
    String address, String description,
    Short status,
    String reviewedByName,
    String reviewNote,
    Long createdPoiId,
    LocalDateTime createdAt, LocalDateTime reviewedAt
) {}
```

### 3.4 Service

文件：`src/main/java/com/smartcampus/service/POIApplicationService.java`

```java
public interface POIApplicationService {
    POIApplicationResponse submitApplication(Long userId, CreatePOIApplicationRequest request);
    PageResponse<POIApplicationResponse> getMyApplications(Long userId, Integer page, Integer size);
    PageResponse<AdminPOIApplicationListItemResponse> getAdminList(String keyword, Short status, Integer page, Integer size);
    AdminPOIApplicationListItemResponse getAdminDetail(Long id);
    void reviewApplication(Long id, Long reviewerId, ReviewPOIApplicationRequest request);
}
```

文件：`src/main/java/com/smartcampus/service/impl/POIApplicationServiceImpl.java`

**核心逻辑：**

```
submitApplication:
  1. 校验 name 长度、category 有效性（从现有分类列表中校验）
  2. photoUrls 最多 3 张，转 JSON 字符串存储
  3. 同一用户不能对同名地点有 PENDING 状态的重复申请
  4. 保存并返回

reviewApplication:
  1. 查询申请，校验存在且 status == PENDING
  2. 校验 request.status 为 APPROVED 或 REJECTED
  3. 更新 status、reviewedBy、reviewedAt、reviewNote
  4. 如果 APPROVED：
     a. 创建 POI（name/category/description/latitude/longitude）
     b. 回填 createdPoiId
     c. 通知申请人："您申请添加的地点「{name}」已通过审核"
  5. 如果 REJECTED：
     a. 通知申请人："您申请添加的地点「{name}」未通过审核{reviewNote}"
```

### 3.5 Controller

**POIApplicationController.java** — 用户端

| 方法 | 路径 | 权限 |
|------|------|------|
| POST | `/api/poi-applications` | `authenticated()` |
| GET | `/api/poi-applications/my` | `authenticated()` |

**AdminPOIApplicationController.java** — 管理端

| 方法 | 路径 | 权限 |
|------|------|------|
| GET | `/api/admin/poi-applications` | `@PreAuthorize SUPER_ADMIN, ADMIN` |
| GET | `/api/admin/poi-applications/{id}` | `@PreAuthorize SUPER_ADMIN, ADMIN` |
| PUT | `/api/admin/poi-applications/{id}/review` | `@PreAuthorize SUPER_ADMIN, ADMIN` |

### 3.6 SecurityConfig 新增规则

```java
// 用户提交 POI 申请
.requestMatchers(HttpMethod.POST, "/api/poi-applications").authenticated()
.requestMatchers(HttpMethod.GET, "/api/poi-applications/my").authenticated()
```

管理端 `/api/admin/**` 走已有的方法级 `@PreAuthorize`，无需额外配置。

---

## 四、前端设计

### 4.1 API 文件

文件：`src/api/poiApplication.js`

```javascript
import request from '@/utils/request.js'

export const submitPOIApplication = (data) => {
  return request.post('/poi-applications', data)
}

export const getMyApplications = (params = {}) => {
  return request.get('/poi-applications/my', { params })
}

export const getAdminPOIApplications = (params = {}) => {
  return request.get('/admin/poi-applications', { params })
}

export const getAdminPOIApplicationDetail = (id) => {
  return request.get(`/admin/poi-applications/${id}`)
}

export const reviewPOIApplication = (id, data) => {
  return request.put(`/admin/poi-applications/${id}/review`, data)
}
```

### 4.2 用户端 — 申请弹窗组件

文件：`src/components/map/POIApplicationDialog.vue`

**交互流程：**

1. 用户点击地图工具栏"申请添加地点"按钮
2. 弹出 Dialog，内含：
   - 地图选点区域（复用高德地图，点击选点，显示标记）
   - 表单：地点名称（必填）、分类（下拉选择，复用现有 categories）、描述（选填）、地址（选填）
   - 经纬度根据选点自动填充，只读展示
3. 提交后显示成功提示，关闭弹窗

**关键实现点：**
- Dialog 内嵌一个独立的高德地图实例用于选点
- 分类选项复用 `poiStore.categories`
- 提交时校验：名称非空、分类非空、经纬度非空
- 使用 `defineAsyncComponent` 按需加载，不影响首屏

### 4.3 用户端 — 我的申请记录

在 `Settings.vue` 中新增"我的地点申请"区域，展示申请历史和审核状态：

```
┌─────────────────────────────────────────┐
│ 我的地点申请                              │
│ ┌──────────────────────────────────────┐ │
│ │ 图书馆B座  │ 探索  │ 待审核           │ │
│ │ 学生食堂三楼 │ 餐饮  │ 已通过 ✓        │ │
│ │ 新建实验楼  │ 教学  │ 已驳回 ✗        │ │
│ └──────────────────────────────────────┘ │
└─────────────────────────────────────────┘
```

每条记录显示：名称、分类、状态标签（颜色区分）、审核意见、提交时间。
已通过的可点击跳转到对应 POI 详情。

### 4.4 管理端 — 审核列表页

文件：`src/views/admin/POIApplicationList.vue`

**参考现有 `ReportList.vue` 的设计模式：**

- 顶部筛选栏：状态筛选（全部/待审核/已通过/已驳回）+ 关键词搜索
- 数据表格列：ID、申请人、地点名称、分类、地址、经纬度、状态、提交时间、操作
- 操作按钮：
  - 待审核 → 显示"通过"和"驳回"按钮
  - 已通过/已驳回 → 显示"查看"按钮
- 审核操作弹窗：显示申请详情（含地图位置标注），通过/驳回选择 + 审核意见输入框
- 已通过的申请显示"查看POI"链接，跳转到 POI 管理页

**路由配置：**

```javascript
{
  path: '/admin/poi-applications',
  name: 'AdminPOIApplications',
  component: () => import('@/views/admin/POIApplicationList.vue'),
  meta: { requiresAuth: true, requiresAdmin: true }
}
```

在运营后台侧边栏导航中增加"地点审核"入口。

### 4.5 MapContainer.vue 修改

在 `map-toolbar` 区域增加按钮：

```html
<el-button class="toolbar-btn" @click="showApplyDialog = true" aria-label="申请添加地点">
  <el-icon><Location /></el-icon>
  <span>申请添加地点</span>
</el-button>
```

异步引入组件：

```javascript
const POIApplicationDialog = defineAsyncComponent(
  () => import('@/components/map/POIApplicationDialog.vue')
)
```

```html
<POIApplicationDialog v-if="showApplyDialog" @close="showApplyDialog = false" />
```

---

## 五、通知集成

审核完成后，通过现有 `NotificationService` 发送通知：

| 场景 | type | title | targetType | targetId |
|------|------|-------|------------|----------|
| 审核通过 | `POI_APPROVED` | "您申请的地点「{name}」已通过审核" | `POI_APPLICATION` | applicationId |
| 审核驳回 | `POI_REJECTED` | "您申请的地点「{name}」未通过审核" | `POI_APPLICATION` | applicationId |

通知铃铛组件的 `typeIcon` 映射中增加：

```javascript
POI_APPROVED: Location,
POI_REJECTED: Warning,
```

点击通知跳转到 Settings 页面的"我的地点申请"区域。

---

## 六、成就系统集成（可选）

新增一个成就，鼓励用户参与共建：

| id | name | description | category | sort_order |
|----|------|-------------|----------|------------|
| `poi_apply_1` | 地点发现者 | 成功申请并通过第一个地点审核 | 探索 | 11 |

在 `AchievementServiceImpl.checkAndUnlock` 中增加条件：

```java
long approvedCount = poiApplicationRepository.countByApplicantIdAndStatus(userId, POIApplication.STATUS_APPROVED);
```

条件映射中增加：`"poi_apply_1", approvedCount >= 1`

---

## 七、边界条件与安全考虑

| 场景 | 处理方式 |
|------|----------|
| 同名重复申请 | 同一用户对同名地点有 PENDING 申请时拒绝提交 |
| 坐标超出范围 | 后端校验 latitude ∈ [-90, 90]、longitude ∈ [-180, 180] |
| 分类不合法 | 后端校验 category 必须在系统分类列表中 |
| photoUrls 过多 | 限制最多 3 张，单张 URL 长度 ≤ 255 |
| 驳回后重新申请 | 允许，不做限制（每次提交都是新记录） |
| 审核终态不可变 | 已通过/已驳回的申请不允许再次审核 |
| 权限隔离 | 用户只能看自己的申请，管理员看所有 |
| XSS 防护 | name/description/address 做 HTML 转义 |
| 操作日志 | 管理员审核操作记录到操作日志（复用现有机制） |

---

## 八、任务清单

### 后端

- [x] 新建 `POIApplication` Entity
- [x] 新建 `POIApplicationRepository`
- [x] 新建 `CreatePOIApplicationRequest` / `ReviewPOIApplicationRequest`
- [x] 新建 `POIApplicationResponse` / `AdminPOIApplicationListItemResponse`
- [x] 新建 `POIApplicationService` / `POIApplicationServiceImpl`
- [x] 新建 `POIApplicationController`（用户端：提交申请、我的申请列表）
- [x] 新建 `AdminPOIApplicationController`（管理端：列表、详情、审核）
- [x] 修改 `SecurityConfig` 新增 POST/GET `/api/poi-applications` 权限规则
- [ ] 修改 `AchievementServiceImpl` 新增 `poi_apply_1` 成就条件（可选）
- [x] 修改 `NotificationBell.vue` typeIcon 映射增加 `POI_APPROVED` / `POI_REJECTED`

### 前端

- [x] 新建 `src/api/poiApplication.js`
- [x] 新建 `src/components/map/POIApplicationDialog.vue`（申请弹窗 + 地图选点）
- [x] 修改 `src/components/map/MapContainer.vue`（工具栏加按钮 + 引入弹窗）
- [x] 修改 `src/views/Settings.vue`（新增"我的地点申请"区域）
- [x] 新建 `src/views/admin/POIApplicationList.vue`（管理端审核列表页）
- [x] 修改 `src/router/index.js`（新增管理端路由）
- [x] 修改运营后台侧边栏导航（增加"地点审核"入口）

### 数据库

- [ ] 执行 `poi_applications` 建表 SQL（Hibernate 自动建表或手动执行）
- [ ] 执行 `achievement_definitions` 新增 `poi_apply_1` 数据（可选）

### 测试

- [ ] 用户提交申请 → 列表可见 → 状态为待审核
- [ ] 管理员审核通过 → POI 自动创建 → 用户收到通知
- [ ] 管理员审核驳回 → 用户收到通知含审核意见
- [ ] 重复提交同名 PENDING 申请被拒绝
- [ ] 已审核申请不可再次操作
- [ ] 移动端弹窗/列表正常展示

---

## 九、工作量评估

| 模块 | 预计时间 |
|------|----------|
| 后端 Entity + Repository + DTO | 0.5 天 |
| 后端 Service + Controller | 1 天 |
| 前端申请弹窗（含地图选点） | 0.5 天 |
| 前端管理端审核页面 | 0.5 天 |
| 前端 Settings 我的申请区域 | 0.5 天 |
| 联调 + 测试 + 修 bug | 0.5 天 |
| **合计** | **约 3 天** |
