<template>
  <div class="reminder">
    <div class="toolbar">
      <el-select
        v-model="searchStatus"
        placeholder="状态筛选"
        clearable
        style="width: 130px"
      >
        <el-option label="待通知" value="PENDING" />
        <el-option label="已通知" value="SENT" />
        <el-option label="已忽略" value="IGNORED" />
      </el-select>
      <el-button type="primary" @click="handleSearch">搜索</el-button>
      <el-button @click="handleReset">重置</el-button>
      <el-button type="warning" @click="handleCheckReminders">检查到期</el-button>
    </div>

    <el-table :data="tableData" border stripe style="width: 100%">
      <el-table-column prop="orderId" label="订单ID" min-width="100" />
      <el-table-column prop="customerId" label="客户ID" min-width="100" />
      <el-table-column prop="expireDate" label="到期日期" min-width="120" />
      <el-table-column prop="daysBeforeExpire" label="提前天数" min-width="100" />
      <el-table-column prop="status" label="状态" min-width="100">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.status)">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="notifyMethod" label="通知方式" min-width="100">
        <template #default="{ row }">
          {{ notifyMethodLabel(row.notifyMethod) }}
        </template>
      </el-table-column>
      <el-table-column prop="notifyTime" label="通知时间" min-width="160" />
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button
            v-if="row.status === 'PENDING'"
            type="success"
            link
            @click="handleUpdateStatus(row.id, 'SENT')"
          >SENT</el-button>
          <el-button
            v-if="row.status === 'PENDING' || row.status === 'SENT'"
            type="info"
            link
            @click="handleUpdateStatus(row.id, 'IGNORED')"
          >IGNORE</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="pagination.pageNum"
      v-model:page-size="pagination.pageSize"
      :total="pagination.total"
      :page-sizes="[10, 20, 50, 100]"
      layout="total, sizes, prev, pager, next, jumper"
      style="margin-top: 16px; justify-content: flex-end"
      @size-change="fetchList"
      @current-change="fetchList"
    />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getReminderList, checkReminders, updateReminderStatus } from '../api/reminder.js'

const searchStatus = ref('')
const tableData = ref([])

const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

const statusTagType = (status) => {
  const map = { PENDING: 'warning', SENT: 'success', IGNORED: 'info' }
  return map[status] || 'info'
}

const statusLabel = (status) => {
  const map = { PENDING: '待通知', SENT: '已通知', IGNORED: '已忽略' }
  return map[status] || status
}

const notifyMethodLabel = (method) => {
  const map = { SYSTEM: '系统', SMS: '短信', PHONE: '电话' }
  return map[method] || method
}

const fetchList = async () => {
  const { data } = await getReminderList({
    pageNum: pagination.pageNum,
    pageSize: pagination.pageSize,
    status: searchStatus.value || undefined
  })
  tableData.value = data.records
  pagination.total = data.total
}

const handleSearch = () => {
  pagination.pageNum = 1
  fetchList()
}

const handleReset = () => {
  searchStatus.value = ''
  pagination.pageNum = 1
  fetchList()
}

const handleCheckReminders = async () => {
  await checkReminders()
  ElMessage.success('检查完成')
  fetchList()
}

const handleUpdateStatus = async (id, status) => {
  await updateReminderStatus(id, status)
  ElMessage.success('操作成功')
  fetchList()
}

onMounted(() => {
  fetchList()
})
</script>

<style scoped>
.reminder {
  padding: 20px;
}

.toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}
</style>
