<template>
  <svg class="leaf-logo" viewBox="0 0 48 48" fill="none" xmlns="http://www.w3.org/2000/svg" aria-label="楠树叶">
    <!-- 叶片（披针形，楠树叶特征：先端渐尖） -->
    <path d="M24 4 C35 13 35 28 24 39 C13 28 13 13 24 4 Z"
          fill="currentColor" fill-opacity="0.18"
          stroke="currentColor" stroke-width="2.4" stroke-linejoin="round" />
    <!-- 叶柄 -->
    <path d="M24 39 L24 45" stroke="currentColor" stroke-width="2.6" stroke-linecap="round" />
    <!-- 中脉 -->
    <path d="M24 9 L24 37" stroke="currentColor" stroke-width="2" stroke-linecap="round" />
    <!-- 侧脉 -->
    <path d="M24 15 L31 11 M24 15 L17 11 M24 23 L32 19 M24 23 L16 19 M24 30 L29 27 M24 30 L19 27"
          stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-opacity="0.7" />
  </svg>
</template>

<script setup>
// 纯展示组件：颜色继承父级 currentColor，尺寸填满父容器
</script>

<style scoped>
.leaf-logo { width: 100%; height: 100%; display: block; }
</style>
