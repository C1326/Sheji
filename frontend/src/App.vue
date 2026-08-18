<template>
  <div id="app">
    <router-view />
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

onMounted(() => {
  // 已登录时静默刷新用户信息（头像、昵称等可能已更新）
  if (userStore.isLoggedIn) {
    userStore.getUserInfoAction().catch(() => {})
  }
})
</script>

<style>
#app {
  min-height: 100vh;
  background-color: #f5f7fa;
}
</style>
