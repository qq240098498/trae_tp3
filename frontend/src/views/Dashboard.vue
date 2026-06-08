<template>
  <div class="dashboard">
    <el-row :gutter="20" class="stat-row">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <el-icon class="stat-icon" :size="40" color="#409EFF"><Headset /></el-icon>
            <div class="stat-info">
              <div class="stat-number">{{ stats.totalInstruments }}</div>
              <div class="stat-label">乐器总数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <el-icon class="stat-icon" :size="40" color="#67C23A"><CircleCheck /></el-icon>
            <div class="stat-info">
              <div class="stat-number">{{ stats.availableInstruments }}</div>
              <div class="stat-label">可租乐器</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <el-icon class="stat-icon" :size="40" color="#E6A23C"><Clock /></el-icon>
            <div class="stat-info">
              <div class="stat-number">{{ stats.rentedInstruments }}</div>
              <div class="stat-label">在租乐器</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <el-icon class="stat-icon" :size="40" color="#909399"><User /></el-icon>
            <div class="stat-info">
              <div class="stat-number">{{ stats.totalCustomers }}</div>
              <div class="stat-label">客户总数</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="stat-row">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <el-icon class="stat-icon" :size="40" color="#409EFF"><Document /></el-icon>
            <div class="stat-info">
              <div class="stat-number">{{ stats.activeOrders }}</div>
              <div class="stat-label">活跃订单</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card" :class="{ 'stat-card--danger': stats.overdueOrders > 0 }">
          <div class="stat-content">
            <el-icon class="stat-icon" :size="40" :color="stats.overdueOrders > 0 ? '#F56C6C' : '#909399'"><Warning /></el-icon>
            <div class="stat-info">
              <div class="stat-number" :class="{ 'text-danger': stats.overdueOrders > 0 }">{{ stats.overdueOrders }}</div>
              <div class="stat-label">逾期订单</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card" :class="{ 'stat-card--danger': stats.pendingDamages > 0 }">
          <div class="stat-content">
            <el-icon class="stat-icon" :size="40" :color="stats.pendingDamages > 0 ? '#F56C6C' : '#909399'"><Warning /></el-icon>
            <div class="stat-info">
              <div class="stat-number" :class="{ 'text-danger': stats.pendingDamages > 0 }">{{ stats.pendingDamages }}</div>
              <div class="stat-label">待处理损坏</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card" :class="{ 'stat-card--warning': stats.pendingRepairs > 0 }">
          <div class="stat-content">
            <el-icon class="stat-icon" :size="40" :color="stats.pendingRepairs > 0 ? '#E6A23C' : '#909399'"><SetUp /></el-icon>
            <div class="stat-info">
              <div class="stat-number" :class="{ 'text-warning': stats.pendingRepairs > 0 }">{{ stats.pendingRepairs }}</div>
              <div class="stat-label">进行中维修</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="stat-row">
      <el-col :span="8">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <el-icon class="stat-icon" :size="40" color="#F56C6C"><SetUp /></el-icon>
            <div class="stat-info">
              <div class="stat-number" style="color: #F56C6C">{{ stats.maintenanceInstruments }}</div>
              <div class="stat-label">维保中乐器</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <el-icon class="stat-icon" :size="40" color="#E6A23C"><Bell /></el-icon>
            <div class="stat-info">
              <div class="stat-number">{{ stats.pendingReminders }}</div>
              <div class="stat-label">待处理提醒</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <el-icon class="stat-icon" :size="40" color="#409EFF"><Connection /></el-icon>
            <div class="stat-info">
              <div class="stat-number" style="color: #409EFF">{{ flowStats }}</div>
              <div class="stat-label">待闭环流程</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 10px">
      <el-col :span="24">
        <el-card shadow="hover">
          <template #header>
            <span style="font-weight: 600">流程闭环说明</span>
          </template>
          <el-steps :active="4" align-center>
            <el-step title="损坏登记" description="记录乐器损坏情况" />
            <el-step title="维修工单" description="创建维修工单安排维修" />
            <el-step title="维保记录" description="完成维修生成维保记录" />
            <el-step title="押金扣除" description="损坏赔偿扣除押金" />
          </el-steps>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { Headset, CircleCheck, Clock, User, Document, Warning, Bell, SetUp, Connection } from '@element-plus/icons-vue'
import { getDashboardStats } from '../api/dashboard.js'

const stats = ref({
  totalInstruments: 0,
  availableInstruments: 0,
  rentedInstruments: 0,
  totalCustomers: 0,
  activeOrders: 0,
  overdueOrders: 0,
  pendingReminders: 0,
  pendingDamages: 0,
  pendingRepairs: 0,
  maintenanceInstruments: 0
})

const flowStats = computed(() => {
  return stats.value.pendingDamages + stats.value.pendingRepairs
})

onMounted(async () => {
  const { data } = await getDashboardStats()
  stats.value = data
})
</script>

<style scoped>
.dashboard {
  padding: 20px;
}

.stat-row {
  margin-bottom: 20px;
}

.stat-card {
  border-radius: 8px;
  transition: transform 0.3s;
}

.stat-card:hover {
  transform: translateY(-4px);
}

.stat-card--danger {
  border: 1px solid #F56C6C;
}

.stat-card--warning {
  border: 1px solid #E6A23C;
}

.stat-content {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 10px 0;
}

.stat-icon {
  flex-shrink: 0;
}

.stat-info {
  display: flex;
  flex-direction: column;
}

.stat-number {
  font-size: 28px;
  font-weight: 700;
  color: #303133;
  line-height: 1.2;
}

.stat-label {
  font-size: 14px;
  color: #909399;
  margin-top: 4px;
}

.text-danger {
  color: #F56C6C;
}

.text-warning {
  color: #E6A23C;
}
</style>
