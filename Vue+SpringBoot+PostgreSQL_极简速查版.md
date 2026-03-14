# Vue + Spring Boot + PostgreSQL 极简速查版

## 一句话总原则
先设计数据，再设计接口，再写后端，再写前端，最后联调验证。

## 标准开发顺序
1. 列业务对象
2. 设计数据库表
3. 画表关系
4. 列接口清单
5. 写后端 entity
6. 写 repository
7. 写 service
8. 写 controller
9. 做登录鉴权
10. 写前端 api
11. 写 store
12. 写页面和组件
13. 联调
14. 测试
15. 优化 UI

## 后端目录口诀
- entity：表长什么样
- repository：怎么查库
- service：业务规则
- controller：接口入口
- config：鉴权、跨域、异常
- common：统一返回

## 前端目录口诀
- api：请求接口
- store：全局状态
- router：页面路由
- views：整页
- components：复用组件
- utils/request.js：axios 封装

## 每个业务对象的最小接口 5 件套
- GET /xxx
- GET /xxx/{id}
- POST /xxx
- PUT /xxx/{id}
- DELETE /xxx/{id}

## 页面固定结构
- 搜索栏
- 操作按钮
- 数据表格
- 分页器
- 新增/编辑弹窗
- 删除确认框

## 登录固定链路
登录页 -> login API -> 后端校验 -> 生成 JWT -> 前端保存 token -> 后续请求自动带 token -> 后端校验 token

## 联调检查清单
- 地址对不对
- 参数对不对
- token 带没带
- 后端是否收到请求
- 数据库是否真的写入
- 返回结构是否统一
- 页面是否正确展示
- 错误提示是否正常

## 最常见的坑
- 页面先写了，接口没定
- controller 写满业务逻辑
- 前端页面里直接写请求细节
- 前端限制了页面，后端却没限制接口
- 返回结构不统一
- 表关系一开始没设计清楚
