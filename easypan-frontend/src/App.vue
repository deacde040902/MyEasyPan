<template>
  <div class="app-container">
    <router-view />
  </div>
</template>

<script setup lang="ts">
// App.vue 作为根组件
</script>

<style>
/* 重置样式 */
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

/* 全局样式 */
html, body {
  height: 100%;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC',
    'Hiragino Sans GB', 'Microsoft YaHei', 'Helvetica Neue',
    Helvetica, Arial, sans-serif;
  font-size: 14px;
  line-height: 1.5;
  color: #333;
}

.app-container {
  height: 100vh;
}

/* Element Plus 样式覆盖 */
.el-card {
  border: none;
  border-radius: 8px;
}

.el-button {
  border-radius: 6px;
}

.el-input {
  border-radius: 6px;
}

.el-input__prefix {
  margin-right: 8px;
}

.el-checkbox__label {
  color: #666;
}

.el-form-item__content {
  align-items: center;
}
</style>