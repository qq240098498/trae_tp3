<template>
  <div class="deposit-container">
    <div class="search-toolbar">
      <el-input
        v-model="searchForm.orderId"
        placeholder="请输入订单ID"
        clearable
        style="width: 200px"
      />
      <el-select
        v-model="searchForm.type"
        placeholder="类型筛选"
        clearable
        style="width: 150px"
      >
        <el-option label="收取" value="COLLECT" />
        <el-option label="退还" value="REFUND" />
        <el-option label="扣除" value="DEDUCT" />
      </el-select>
      <el-select
        v-model="searchForm.status"
        placeholder="状态筛选"
        clearable
        style="width: 150px"
      >
        <el-option label="待处理" value="PENDING" />
        <el-option label="已完成" value="PAID" />
        <el-option label="已取消" value="CANCELLED" />
      </el-select>
      <el-button type="primary" @click="handleSearch">搜索</el-button>
      <el-button @click="handleReset">重置</el-button>
      <el-button type="success" @click="openCollectDialog">收取押金</el-button>
      <el-button type="warning" @click="openRefundDialog">退还押金</el-button>
      <el-button type="danger" @click="openDeductDialog">扣除押金</el-button>
    </div>

    <el-row :gutter="20" style="margin-bottom: 16px">
      <el-col :span="8">
        <el-card shadow="hover">
          <div class="stat-content">
            <div class="stat-info">
              <div class="stat-label">押金收取总额</div>
              <div class="stat-number" style="color: #67C23A">¥{{ summary.totalCollected }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover">
          <div class="stat-content">
            <div class="stat-info">
              <div class="stat-label">押金退还总额</div>
              <div class="stat-number" style="color: #E6A23C">¥{{ summary.totalRefunded }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover">
          <div class="stat-content">
            <div class="stat-info">
              <div class="stat-label">押金扣除总额</div>
              <div class="stat-number" style="color: #F56C6C">¥{{ summary.totalDeducted }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-table :data="tableData" border stripe style="width: 100%">
      <el-table-column prop="orderId" label="订单ID" min-width="80" />
      <el-table-column prop="type" label="类型" min-width="80">
        <template #default="{ row }">
          <el-tag v-if="row.type === 'COLLECT'" type="success">收取</el-tag>
          <el-tag v-else-if="row.type === 'REFUND'" type="warning">退还</el-tag>
          <el-tag v-else-if="row.type === 'DEDUCT'" type="danger">扣除</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="amount" label="金额" min-width="100">
        <template #default="{ row }">¥{{ row.amount?.toFixed(2) ?? '0.00' }}</template>
      </el-table-column>
      <el-table-column prop="payMethod" label="支付方式" min-width="100">
        <template #default="{ row }">
          {{ payMethodMap[row.payMethod] || row.payMethod || '-' }}
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" min-width="90">
        <template #default="{ row }">
          <el-tag v-if="row.status === 'PENDING'" type="warning">待处理</el-tag>
          <el-tag v-else-if="row.status === 'PAID'" type="success">已完成</el-tag>
          <el-tag v-else-if="row.status === 'COMPLETED'" type="success">已完成</el-tag>
          <el-tag v-else-if="row.status === 'CANCELLED'" type="info">已取消</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="remark" label="备注" min-width="180" show-overflow-tooltip />
      <el-table-column prop="createTime" label="创建时间" min-width="160" />
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

    <el-dialog v-model="collectDialogVisible" title="收取押金" width="500px" @close="resetCollectForm">
      <el-form ref="collectFormRef" :model="collectForm" :rules="collectRules" label-width="90px">
        <el-form-item label="订单" prop="orderId">
          <el-select v-model="collectForm.orderId" placeholder="请选择订单" filterable style="width: 100%">
            <el-option
              v-for="item in orderOptions"
              :key="item.id"
              :label="item.orderNo + ' - ' + item.instrumentName"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="金额" prop="amount">
          <el-input-number v-model="collectForm.amount" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="支付方式" prop="payMethod">
          <el-select v-model="collectForm.payMethod" placeholder="请选择支付方式" style="width: 100%">
            <el-option label="现金" value="CASH" />
            <el-option label="微信" value="WECHAT" />
            <el-option label="支付宝" value="ALIPAY" />
            <el-option label="银行转账" value="BANK" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="collectForm.remark" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="collectDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleCollectSubmit">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="refundDialogVisible" title="退还押金" width="500px" @close="resetRefundForm">
      <el-form ref="refundFormRef" :model="refundForm" :rules="refundRules" label-width="90px">
        <el-form-item label="订单" prop="orderId">
          <el-select v-model="refundForm.orderId" placeholder="请选择订单" filterable style="width: 100%" @change="onRefundOrderChange">
            <el-option
              v-for="item in returnedOrderOptions"
              :key="item.id"
              :label="item.orderNo + ' - ' + item.instrumentName"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="可退金额">
          <span style="font-size: 18px; font-weight: 600; color: #E6A23C">¥{{ refundForm.availableAmount }}</span>
        </el-form-item>
        <el-form-item label="退还金额" prop="amount">
          <el-input-number v-model="refundForm.amount" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="退款方式" prop="payMethod">
          <el-select v-model="refundForm.payMethod" placeholder="请选择退款方式" style="width: 100%">
            <el-option label="现金" value="CASH" />
            <el-option label="微信" value="WECHAT" />
            <el-option label="支付宝" value="ALIPAY" />
            <el-option label="银行转账" value="BANK" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="refundForm.remark" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="refundDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleRefundSubmit">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="deductDialogVisible" title="扣除押金" width="500px" @close="resetDeductForm">
      <el-form ref="deductFormRef" :model="deductForm" :rules="deductRules" label-width="90px">
        <el-form-item label="订单" prop="orderId">
          <el-select v-model="deductForm.orderId" placeholder="请选择订单" filterable style="width: 100%" @change="onDeductOrderChange">
            <el-option
              v-for="item in activeOrderOptions"
              :key="item.id"
              :label="item.orderNo + ' - ' + item.instrumentName"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="可扣余额">
          <span style="font-size: 18px; font-weight: 600; color: #F56C6C">¥{{ deductForm.availableAmount }}</span>
        </el-form-item>
        <el-form-item label="扣除金额" prop="amount">
          <el-input-number v-model="deductForm.amount" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="deductForm.remark" type="textarea" :rows="3" placeholder="请说明扣除原因（如：损坏赔偿、维修费用等）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="deductDialogVisible = false">取消</el-button>
        <el-button type="danger" @click="handleDeductSubmit">确定扣除</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getDepositList, addDeposit } from '../api/deposit.js'
import { getOrderList } from '../api/order.js'

const searchForm = reactive({
  orderId: '',
  type: '',
  status: ''
})

const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

const tableData = ref([])
const allDepositRecords = ref([])

const summary = reactive({
  totalCollected: '0.00',
  totalRefunded: '0.00',
  totalDeducted: '0.00'
})

const payMethodMap = {
  CASH: '现金',
  WECHAT: '微信',
  ALIPAY: '支付宝',
  BANK: '银行'
}

const orderOptions = ref([])
const activeOrderOptions = ref([])
const returnedOrderOptions = ref([])

const collectDialogVisible = ref(false)
const collectFormRef = ref(null)
const collectForm = reactive({
  orderId: null,
  amount: 0,
  payMethod: '',
  remark: ''
})
const collectRules = {
  orderId: [{ required: true, message: '请选择订单', trigger: 'change' }],
  amount: [{ required: true, message: '请输入金额', trigger: 'blur' }],
  payMethod: [{ required: true, message: '请选择支付方式', trigger: 'change' }]
}

const refundDialogVisible = ref(false)
const refundFormRef = ref(null)
const refundForm = reactive({
  orderId: null,
  amount: 0,
  availableAmount: 0,
  payMethod: '',
  remark: ''
})
const refundRules = {
  orderId: [{ required: true, message: '请选择订单', trigger: 'change' }],
  amount: [{ required: true, message: '请输入退还金额', trigger: 'blur' }],
  payMethod: [{ required: true, message: '请选择退款方式', trigger: 'change' }]
}

const deductDialogVisible = ref(false)
const deductFormRef = ref(null)
const deductForm = reactive({
  orderId: null,
  amount: 0,
  availableAmount: 0,
  remark: ''
})
const deductRules = {
  orderId: [{ required: true, message: '请选择订单', trigger: 'change' }],
  amount: [{ required: true, message: '请输入扣除金额', trigger: 'blur' }],
  remark: [{ required: true, message: '请说明扣除原因', trigger: 'blur' }]
}

const calculateSummary = () => {
  let collected = 0
  let refunded = 0
  let deducted = 0
  allDepositRecords.value.forEach(r => {
    if (r.status === 'PAID' || r.status === 'COMPLETED') {
      if (r.type === 'COLLECT') collected += Number(r.amount) || 0
      else if (r.type === 'REFUND') refunded += Number(r.amount) || 0
      else if (r.type === 'DEDUCT') deducted += Number(r.amount) || 0
    }
  })
  summary.totalCollected = collected.toFixed(2)
  summary.totalRefunded = refunded.toFixed(2)
  summary.totalDeducted = deducted.toFixed(2)
}

const getAvailableDeposit = (orderId) => {
  let collected = 0
  let refunded = 0
  let deducted = 0
  allDepositRecords.value.forEach(r => {
    if (r.orderId === orderId && (r.status === 'PAID' || r.status === 'COMPLETED')) {
      if (r.type === 'COLLECT') collected += Number(r.amount) || 0
      else if (r.type === 'REFUND') refunded += Number(r.amount) || 0
      else if (r.type === 'DEDUCT') deducted += Number(r.amount) || 0
    }
  })
  return collected - refunded - deducted
}

const fetchList = async () => {
  try {
    const { data } = await getDepositList({
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
      orderId: searchForm.orderId || undefined,
      type: searchForm.type || undefined,
      status: searchForm.status || undefined
    })
    tableData.value = data.records
    pagination.total = data.total
  } catch {
    ElMessage.error('获取押金列表失败')
  }
}

const fetchAllDeposits = async () => {
  try {
    const { data } = await getDepositList({ pageNum: 1, pageSize: 99999 })
    allDepositRecords.value = data.records
    calculateSummary()
  } catch {}
}

const fetchOrders = async () => {
  const { data } = await getOrderList({ pageNum: 1, pageSize: 9999 })
  orderOptions.value = data.records
  activeOrderOptions.value = data.records.filter(o => o.status === 'ACTIVE' || o.status === 'OVERDUE')
  returnedOrderOptions.value = data.records.filter(o => o.status === 'RETURNED')
}

const handleSearch = () => {
  pagination.pageNum = 1
  fetchList()
}

const handleReset = () => {
  searchForm.orderId = ''
  searchForm.type = ''
  searchForm.status = ''
  pagination.pageNum = 1
  fetchList()
}

const openCollectDialog = () => {
  fetchOrders()
  collectForm.orderId = null
  collectForm.amount = 0
  collectForm.payMethod = ''
  collectForm.remark = ''
  collectDialogVisible.value = true
}

const resetCollectForm = () => {
  collectFormRef.value?.resetFields()
}

const handleCollectSubmit = async () => {
  const valid = await collectFormRef.value.validate().catch(() => false)
  if (!valid) return
  await addDeposit({
    orderId: collectForm.orderId,
    type: 'COLLECT',
    amount: collectForm.amount,
    payMethod: collectForm.payMethod,
    status: 'PAID',
    remark: collectForm.remark
  })
  ElMessage.success('押金收取成功')
  collectDialogVisible.value = false
  fetchList()
  fetchAllDeposits()
}

const openRefundDialog = () => {
  fetchOrders()
  fetchAllDeposits()
  refundForm.orderId = null
  refundForm.amount = 0
  refundForm.availableAmount = 0
  refundForm.payMethod = ''
  refundForm.remark = ''
  refundDialogVisible.value = true
}

const resetRefundForm = () => {
  refundFormRef.value?.resetFields()
}

const onRefundOrderChange = (val) => {
  refundForm.availableAmount = getAvailableDeposit(val)
  refundForm.amount = refundForm.availableAmount
}

const handleRefundSubmit = async () => {
  const valid = await refundFormRef.value.validate().catch(() => false)
  if (!valid) return
  if (refundForm.amount > refundForm.availableAmount) {
    ElMessage.warning('退还金额不能超过可退金额')
    return
  }
  await addDeposit({
    orderId: refundForm.orderId,
    type: 'REFUND',
    amount: refundForm.amount,
    payMethod: refundForm.payMethod,
    status: 'PAID',
    remark: refundForm.remark
  })
  ElMessage.success('押金退还成功')
  refundDialogVisible.value = false
  fetchList()
  fetchAllDeposits()
}

const openDeductDialog = () => {
  fetchOrders()
  fetchAllDeposits()
  deductForm.orderId = null
  deductForm.amount = 0
  deductForm.availableAmount = 0
  deductForm.remark = ''
  deductDialogVisible.value = true
}

const resetDeductForm = () => {
  deductFormRef.value?.resetFields()
}

const onDeductOrderChange = (val) => {
  deductForm.availableAmount = getAvailableDeposit(val)
}

const handleDeductSubmit = async () => {
  const valid = await deductFormRef.value.validate().catch(() => false)
  if (!valid) return
  if (deductForm.amount > deductForm.availableAmount) {
    ElMessage.warning('扣除金额不能超过可扣余额')
    return
  }
  await addDeposit({
    orderId: deductForm.orderId,
    type: 'DEDUCT',
    amount: deductForm.amount,
    status: 'PAID',
    remark: deductForm.remark
  })
  ElMessage.success('押金扣除成功')
  deductDialogVisible.value = false
  fetchList()
  fetchAllDeposits()
}

onMounted(() => {
  fetchList()
  fetchAllDeposits()
})
</script>

<style scoped>
.deposit-container {
  padding: 20px;
}

.search-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.stat-content {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 10px 0;
}

.stat-info {
  display: flex;
  flex-direction: column;
}

.stat-label {
  font-size: 14px;
  color: #909399;
  margin-bottom: 4px;
}

.stat-number {
  font-size: 24px;
  font-weight: 700;
  line-height: 1.2;
}
</style>
