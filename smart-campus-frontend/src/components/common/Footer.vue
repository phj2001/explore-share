<template>
  <footer class="footer">
    <div class="footer-content">
      <div class="footer-section">
        <h3>智慧校园</h3>
        <p>基于地图的校园信息服务平台</p>
      </div>

      <div class="footer-section">
        <h4>快速链接</h4>
        <ul>
          <li><router-link to="/">首页</router-link></li>
          <li><router-link to="/admin/poi">管理后台</router-link></li>
        </ul>
      </div>

      <div class="footer-section">
        <h4>关于我们</h4>
        <p>提供校园地图导航、POI 查询等服务</p>
      </div>
    </div>

    <div class="footer-bottom">
      <p>&copy; 2024 智慧校园. All rights reserved.</p>
    </div>
  </footer>
</template>

<script setup>
// Footer 组件无需脚本逻辑
</script>

<style scoped>
.footer {
  background: #2c3e50;
  color: #ecf0f1;
  padding: 40px 0 20px;
}

.footer-content {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
  display: flex;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 40px;
}

.footer-section {
  flex: 1;
  min-width: 200px;
}

.footer-section h3 {
  margin: 0 0 15px;
  font-size: 20px;
  color: #409eff;
}

.footer-section h4 {
  margin: 0 0 15px;
  font-size: 16px;
}

.footer-section p {
  margin: 0;
  color: #bdc3c7;
  line-height: 1.6;
}

.footer-section ul {
  list-style: none;
  padding: 0;
  margin: 0;
}

.footer-section li {
  margin-bottom: 8px;
}

.footer-section a {
  color: #bdc3c7;
  text-decoration: none;
  transition: color 0.3s;
}

.footer-section a:hover {
  color: #409eff;
}

.footer-bottom {
  max-width: 1200px;
  margin: 40px auto 0;
  padding: 20px;
  text-align: center;
  border-top: 1px solid #34495e;
}

.footer-bottom p {
  margin: 0;
  color: #7f8c8d;
  font-size: 14px;
}
</style>
