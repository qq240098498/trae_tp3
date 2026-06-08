<template>
  <div class="deposit-container">
    <div class="search-toolbar">
      <el-input
        v-model="searchForm.orderId"
        placeholder="请输入订单ID"
        clearable
        style="width: 200px; margin-right: 12px"
      />
      <el-select
        v-model="searchForm.type"
        placeholder="类型筛选"
        clearable
        style="width: 150px; margin-right: 12px"
      >
        <el-option label="收取" value="COLLECT" />
        <el-option label="退还" value="REFUND" />
        <el-option label="扣除" value="DEDUCT" />
      </el-select>
      <el-button type="primary" @click="handleSearch">搜索</el-button>
      <el-button @click="handleReset">重置</el-button>
    </div>

    <el-table :data="tableData" border stripe style="width: 100%">
      <el-table-column prop="orderId" label="订单ID" />
      <el-table-column prop="type" label="类型">
        <template #default="{ row }">
          <el-tag v-if="row.type === 'COLLECT'" type="success">收取</el-tag>
          <el-tag v-else-if="row.type === 'REFUND'" type="warning">退还</el-tag>
          <el-tag v-else-if="row.type === 'DEDUCT'" type="danger">扣除</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="amount" label="金额">
        <template #default="{ row }">¥{{ row.amount }}</template>
      </el-table-column>
      <el-table-column prop="payMethod" label="支付方式">
        <template #default="{ row }">
          {{ payMethodMap[row.payMethod] || row.payMethod }}
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态">
        <template #default="{ row }">
          <el-tag v-if="row.status === 'PENDING'" type="warning">待处理</el-tag>
          <el-tag v-else-if="row.status === 'COMPLETED'" type="success">已完成</el-tag>
          <el-tag v-else-if="row.status === 'CANCELLED'" type="info">已取消</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="remark" label="备注" />
      <el-table-column prop="createTime" label="创建时间" />
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
import { getDepositList, addDeposit } from '../api/deposit.js'

const searchForm = reactive({
  orderId: '',
  type: ''
})

const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

const tableData = ref([])

const payMethodMap = {
  CASH: '现金',
  WECHAT: '微信',
  ALIPAY: '支付宝',
  BANK: '银行'
}

const fetchList = async () => {
  try {
    const { data } = await getDepositList({
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
      orderId: searchForm.orderId,
      type: searchForm.type
    })
    tableData.value = data.records
    pagination.total = data.total
  } catch {
    ElMessage.error('获取押金列表失败')
  }
}

const handleSearch = () => {
  pagination.pageNum = 1
  fetchList()
}

const handleReset = () => {
  searchForm.orderId = ''
  searchForm.type = ''
  pagination.pageNum = 1
  fetchList()
}

onMounted(() => {
  fetchList()
})
</script>

<style scoped>
.deposit-container {
  padding: 20px;
}

.search-toolbar {
  display: flex;
  align-items: center;
  margin-bottom: 16px;
}
</style>
