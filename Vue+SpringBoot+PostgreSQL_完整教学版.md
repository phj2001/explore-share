# Vue + Spring Boot + PostgreSQL 完整教学版

## 1. 核心思维
把项目想成盖房子：
- 数据库是地基
- 后端是承重结构
- 接口是水电管线
- 前端是门窗和装修

所以正确顺序一定是：
先定数据，再定接口，再写后端，再写前端，最后联调。

## 2. 通用目录模板

### 后端
```text
backend/
├─ src/main/java/com/example/project/
│  ├─ Application.java
│  ├─ config/
│  ├─ controller/
│  ├─ service/
│  ├─ service/impl/
│  ├─ repository/
│  ├─ entity/
│  ├─ dto/
│  ├─ vo/
│  ├─ common/
│  ├─ exception/
│  └─ utils/
└─ src/main/resources/
   └─ application.properties
```

### 前端
```text
frontend/
├─ src/
│  ├─ main.js
│  ├─ App.vue
│  ├─ router/
│  ├─ stores/
│  ├─ api/
│  ├─ utils/
│  ├─ layouts/
│  ├─ views/
│  ├─ components/
│  └─ assets/
└─ vite.config.js
```

## 3. 各层职责

### 后端
- entity：数据库表映射
- repository：增删改查
- service：业务规则
- controller：接收请求、返回响应
- config：安全、JWT、跨域、Jackson
- common：统一响应结构
- exception：统一异常处理
- utils：工具类

### 前端
- api：接口函数
- stores：全局状态
- router：路由控制
- views：整页
- components：可复用组件
- layouts：页面骨架
- utils/request.js：统一请求封装

## 4. 标准开发顺序

### 第一步：梳理业务对象
例：用户、学生、课程、订单、商品、地图点位。

### 第二步：设计数据库表
每张表都先写清楚：
- 表名
- 字段名
- 类型
- 主键
- 是否必填
- 是否唯一
- 是否索引
- 是否外键
- create_time
- update_time

### 第三步：设计表关系
- 一对多：子表放外键
- 多对多：建中间表
- 一对一：子表放唯一外键

### 第四步：设计接口
每个对象先做最小 5 件套：
- GET /xxx
- GET /xxx/{id}
- POST /xxx
- PUT /xxx/{id}
- DELETE /xxx/{id}

列表接口建议默认支持：
- page
- size
- keyword 或业务字段搜索
- sort

### 第五步：写后端
固定顺序：
1. entity
2. repository
3. service
4. service/impl
5. controller
6. config / exception / common

### 第六步：写前端
固定顺序：
1. api
2. store
3. router
4. view
5. component
6. 联调交互

### 第七步：联调和测试
必须检查：
- 请求地址
- 请求参数
- token
- 权限
- 返回结构
- 页面渲染
- 异常提示
- 数据回显
- 数据库落库

## 5. 后端标准模板

### 固定文件
```text
entity/Student.java
repository/StudentRepository.java
service/StudentService.java
service/impl/StudentServiceImpl.java
controller/StudentController.java
```

### 统一返回结构
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {}
}
```

### 登录鉴权最少文件
```text
controller/AuthController.java
service/AuthService.java
utils/JwtUtil.java
config/SecurityConfig.java
config/JwtAuthenticationFilter.java
```

原则：
- 前端权限控制只是体验
- 真正权限控制必须在后端
- 写接口和敏感接口必须做鉴权

## 6. 前端标准模板

### 固定文件
```text
api/student.js
stores/student.js
views/student/StudentList.vue
components/student/StudentDialog.vue
```

### 页面固定结构
- 搜索栏
- 操作按钮区
- 表格
- 分页器
- 新增/编辑弹窗
- 删除确认框

### 页面开发顺序
1. 先做列表
2. 再做搜索
3. 再做分页
4. 再做新增
5. 再做编辑
6. 再做删除
7. 最后做细节优化

## 7. 每开发一个新模块的执行模板
以“课程管理”为例：
1. 明确课程字段
2. 设计 course 表
3. 设计接口
4. 写 Course 实体
5. 写 Repository
6. 写 Service
7. 写 Controller
8. 写前端 api/course.js
9. 写 CourseList 页面
10. 做新增/修改/删除
11. 联调
12. 测试异常情况

## 8. 最常见的坑
- 页面先写，接口后补
- Controller 写满业务逻辑
- 前端页面里直接写请求
- 返回结构一会儿数组一会儿对象
- token 只在前端判断，后端没拦
- 表关系设计太晚
- 一个模块没有统一模板，导致每页写法都不一样

## 9. 这套模板适用范围
非常适合：
- 管理系统
- 后台系统
- 学生课程系统
- 商品订单系统
- 地图点位系统
- 内容管理系统
- 库存系统

不适合直接原样套用：
- 即时聊天
- 在线协作编辑
- 实时行情
- 大型微服务平台
- 大数据和流处理平台
- 大量异步任务平台

原因：这些系统往往还需要 WebSocket、MQ、缓存、任务调度、搜索引擎、微服务拆分。

最准确的结论：
这套不是“只适合简单项目”，而是“绝大多数标准业务型项目的通用骨架”。

## 10. 最后记住这句
```text
先设计数据，再设计接口，再写后端，再写前端，最后联调验证。
```
