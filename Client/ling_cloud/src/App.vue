<script setup lang="ts">
// App根组件
</script>

<template>
  <div id="app">
    <router-view />
  </div>
</template>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  background-color: #f8fafc;
}

#app {
  min-height: 100vh;
}

/* 全局样式覆盖 */
.el-button {
  border-radius: 6px;
  font-weight: 500;
}

.el-button--primary {
  background: linear-gradient(135deg, #3b82f6, #1e40af);
  border: none;
}

.el-button--primary:hover {
  background: linear-gradient(135deg, #2563eb, #1d4ed8);
}

.el-input__wrapper {
  border-radius: 6px;
}

.el-card {
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.el-table {
  border-radius: 8px;
}

.el-message {
  border-radius: 8px;
}

.el-dialog {
  border-radius: 12px;
}
</style>
