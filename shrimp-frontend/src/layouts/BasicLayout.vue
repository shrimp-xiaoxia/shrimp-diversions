<template>
  <div class="h-screen max-w-screen-3xl mx-auto">
    <div>
      <!-- 顶部导航栏 -->
      <AppHeader />

      <div class="layout-body flex h-[calc(100vh-72px)] px-5 2xl:px-24 pt-[76px]">
        <!-- 左侧侧边栏 -->
        <AppSidebar />

        <!-- 右侧内容区域 -->
        <main class="layout-main px-5 grow ml-[320px]">

          
          <router-view></router-view>
        </main>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import AppHeader from '@/components/layout/AppHeader.vue'
import AppSidebar from '@/components/layout/AppSidebar.vue'

</script>

<style scoped>
.layout-main { min-width: 0; }
@media (max-width: 767px) {
  .layout-body { padding: 72px 12px 80px; height: auto; }
  .layout-main { margin-left: 0; padding: 0; width: 100%; }
}
</style> 