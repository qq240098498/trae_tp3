<template>
  <el-container style="height: 100vh">
    <el-aside width="220px" style="background: #304156">
      <div style="height: 60px; display: flex; align-items: center; justify-content: center; color: #fff; font-size: 16px; font-weight: bold; border-bottom: 1px solid #3d4d5e">
        🎵 乐器租赁管理
      </div>
      <el-menu :default-active="$route.path" router background-color="#304156" text-color="#bfcbd9" active-text-color="#409EFF">
        <el-menu-item index="/dashboard"><el-icon><DataAnalysis /></el-icon><span>工作台</span></el-menu-item>
        <el-menu-item index="/instrument"><el-icon><Collection /></el-icon><span>乐器档案</span></el-menu-item>
        <el-menu-item index="/customer"><el-icon><User /></el-icon><span>客户档案</span></el-menu-item>
        <el-menu-item index="/order"><el-icon><Document /></el-icon><span>租赁管理</span></el-menu-item>
        <el-menu-item index="/damage"><el-icon><Warning /></el-icon><span>损坏登记</span></el-menu-item>
        <el-menu-item index="/repair"><el-icon><SetUp /></el-icon><span>维修工单</span></el-menu-item>
        <el-menu-item index="/deposit"><el-icon><Wallet /></el-icon><span>押金管理</span></el-menu-item>
        <el-menu-item index="/maintenance"><el-icon><SetUp /></el-icon><span>维保记录</span></el-menu-item>
        <el-menu-item index="/reminder"><el-icon><Bell /></el-icon><span>到期提醒</span></el-menu-item>
        <el-menu-item index="/points"><el-icon><CreditCard /></el-icon><span>会员积分</span></el-menu-item>
        <el-menu-item index="/coupon"><el-icon><Present /></el-icon><span>优惠券管理</span></el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header style="background: #fff; border-bottom: 1px solid #e6e6e6; display: flex; align-items: center; justify-content: space-between; padding: 0 20px">
        <span style="font-size: 18px; font-weight: 600">{{ $route.meta.title }}</span>
        <span style="color: #999">乐器租赁维保后台管理系统 v1.0</span>
      </el-header>
      <el-main style="background: #f0f2f5; padding: 20px">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
</script>
