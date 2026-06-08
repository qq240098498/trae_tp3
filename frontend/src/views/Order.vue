<template>
  <div class="order">
    <div class="toolbar">
      <el-input
        v-model="searchParams.keyword"
        placeholder="搜索订单编号"
        clearable
        style="width: 220px"
        @keyup.enter="handleSearch"
      />
      <el-select
        v-model="searchParams.status"
        placeholder="状态筛选"
        clearable
        style="width: 130px"
      >
        <el-option label="租赁中" value="ACTIVE" />
        <el-option label="已归还" value="RETURNED" />
        <el-option label="逾期" value="OVERDUE" />
        <el-option label="已取消" value="CANCELLED" />
      </el-select>
      <el-button type="primary" @click="handleSearch">搜索</el-button>
      <el-button @click="handleReset">重置</el-button>
      <el-button type="success" @click="openCreateDialog">新建租赁</el-button>
      <el-button type="warning" @click="openRenewDialog">续租</el-button>
      <el-button type="danger" @click="openReturnDialog">退租</el-button>
    </div>

    <el-table
      :data="tableData"
      border
      stripe
      style="width: 100%"
      highlight-current-row
      @current-change="handleCurrentChange"
    >
      <el-table-column prop="orderNo" label="订单编号" min-width="150" />
      <el-table-column prop="customerName" label="客户" min-width="100" />
      <el-table-column prop="instrumentName" label="乐器" min-width="120" />
      <el-table-column prop="startDate" label="开始日期" min-width="110" />
      <el-table-column prop="endDate" label="结束日期" min-width="110" />
      <el-table-column prop="dailyRent" label="日租金" min-width="90">
        <template #default="{ row }">
          ¥{{ row.dailyRent?.toFixed(2) ?? '0.00' }}
        </template>
      </el-table-column>
      <el-table-column prop="totalRent" label="总租金" min-width="90">
        <template #default="{ row }">
          ¥{{ row.totalRent?.toFixed(2) ?? '0.00' }}
        </template>
      </el-table-column>
      <el-table-column prop="depositAmount" label="押金" min-width="90">
        <template #default="{ row }">
          ¥{{ row.depositAmount?.toFixed(2) ?? '0.00' }}
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" min-width="80">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.status)">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="overdueFee" label="逾期费" min-width="90">
        <template #default="{ row }">
          <span :style="{ color: row.overdueFee > 0 ? 'red' : '' }">
            ¥{{ row.overdueFee?.toFixed(2) ?? '0.00' }}
          </span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button
            v-if="row.status === 'ACTIVE'"
            type="primary"
            link
            @click="openRenewForRow(row)"
          >续租</el-button>
          <el-button
            v-if="row.status === 'ACTIVE' || row.status === 'OVERDUE'"
            type="danger"
            link
            @click="openReturnForRow(row)"
          >退租</el-button>
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

    <el-dialog v-model="createDialogVisible" title="新建租赁" width="600px" @close="resetCreateForm">
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="100px">
        <el-form-item label="客户" prop="customerId">
          <el-select v-model="createForm.customerId" placeholder="请选择客户" style="width: 100%">
            <el-option
              v-for="c in customerOptions"
              :key="c.id"
              :label="c.name"
              :value="c.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="乐器" prop="instrumentId">
          <el-select v-model="createForm.instrumentId" placeholder="请选择乐器" style="width: 100%" @change="onInstrumentChange">
            <el-option
              v-for="i in instrumentOptions"
              :key="i.id"
              :label="i.name"
              :value="i.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="租赁日期" prop="dateRange">
          <el-date-picker
            v-model="createForm.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item v-if="calculatedTotalRent > 0" label="总租金">
          ¥{{ calculatedTotalRent.toFixed(2) }}
        </el-form-item>
        <el-form-item label="支付方式" prop="payMethod">
          <el-select v-model="createForm.payMethod" placeholder="请选择支付方式" style="width: 100%">
            <el-option label="现金" value="CASH" />
            <el-option label="微信" value="WECHAT" />
            <el-option label="支付宝" value="ALIPAY" />
            <el-option label="银行转账" value="BANK" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="createForm.remark" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleCreateSubmit">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="renewDialogVisible" title="续租" width="600px" @close="resetRenewForm">
      <el-form ref="renewFormRef" :model="renewForm" :rules="renewRules" label-width="100px">
        <el-form-item label="订单编号">
          {{ renewForm.orderNo }}
        </el-form-item>
        <el-form-item label="乐器">
          {{ renewForm.instrumentName }}
        </el-form-item>
        <el-form-item label="当前到期日">
          {{ renewForm.currentEndDate }}
        </el-form-item>
        <el-form-item label="新到期日" prop="newEndDate">
          <el-date-picker
            v-model="renewForm.newEndDate"
            type="date"
            placeholder="请选择新到期日"
            value-format="YYYY-MM-DD"
            style="width: 100%"
            :disabled-date="renewDisabledDate"
          />
        </el-form-item>
        <el-form-item v-if="additionalRent > 0" label="续租费用">
          ¥{{ additionalRent.toFixed(2) }}
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="renewForm.remark" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="renewDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleRenewSubmit">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="returnDialogVisible" title="退租" width="600px" @close="resetReturnForm">
      <el-form ref="returnFormRef" :model="returnForm" :rules="returnRules" label-width="100px">
        <el-form-item label="订单编号">
          {{ returnForm.orderNo }}
        </el-form-item>
        <el-form-item label="乐器">
          {{ returnForm.instrumentName }}
        </el-form-item>
        <el-form-item label="乐器成色" prop="instrumentCondition">
          <el-select v-model="returnForm.instrumentCondition" placeholder="请选择成色" style="width: 100%">
            <el-option label="全新" value="NEW" />
            <el-option label="良好" value="GOOD" />
            <el-option label="一般" value="FAIR" />
            <el-option label="较差" value="POOR" />
          </el-select>
        </el-form-item>
        <el-form-item label="扣款金额" prop="deductAmount">
          <el-input-number v-model="returnForm.deductAmount" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="退款方式" prop="refundMethod">
          <el-select v-model="returnForm.refundMethod" placeholder="请选择退款方式" style="width: 100%">
            <el-option label="现金" value="CASH" />
            <el-option label="微信" value="WECHAT" />
            <el-option label="支付宝" value="ALIPAY" />
            <el-option label="银行转账" value="BANK" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="returnForm.remark" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="returnDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleReturnSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getOrderList, createOrder, renewOrder, returnOrder } from '../api/order.js'
import { getAvailableInstruments } from '../api/instrument.js'
import { getAllCustomers } from '../api/customer.js'

const searchParams = reactive({
  keyword: '',
  status: ''
})

const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

const tableData = ref([])
const selectedRow = ref(null)

const customerOptions = ref([])
const instrumentOptions = ref([])

const createDialogVisible = ref(false)
const createFormRef = ref(null)
const createForm = reactive({
  customerId: null,
  instrumentId: null,
  dateRange: null,
  payMethod: '',
  remark: ''
})

const createRules = {
  customerId: [{ required: true, message: '请选择客户', trigger: 'change' }],
  instrumentId: [{ required: true, message: '请选择乐器', trigger: 'change' }],
  dateRange: [{ required: true, message: '请选择租赁日期', trigger: 'change' }],
  payMethod: [{ required: true, message: '请选择支付方式', trigger: 'change' }]
}

const selectedInstrumentDailyRent = ref(0)

const calculatedTotalRent = computed(() => {
  if (!createForm.dateRange || !createForm.instrumentId) return 0
  const [start, end] = createForm.dateRange
  if (!start || !end || selectedInstrumentDailyRent.value <= 0) return 0
  const days = Math.ceil((new Date(end) - new Date(start)) / (1000 * 60 * 60 * 24))
  return Math.max(days, 0) * selectedInstrumentDailyRent.value
})

const renewDialogVisible = ref(false)
const renewFormRef = ref(null)
const renewForm = reactive({
  orderId: null,
  orderNo: '',
  instrumentName: '',
  currentEndDate: '',
  dailyRent: 0,
  newEndDate: '',
  remark: ''
})

const renewRules = {
  newEndDate: [{ required: true, message: '请选择新到期日', trigger: 'change' }]
}

const renewDisabledDate = (date) => {
  if (!renewForm.currentEndDate) return false
  return date <= new Date(renewForm.currentEndDate)
}

const additionalRent = computed(() => {
  if (!renewForm.newEndDate || !renewForm.currentEndDate || renewForm.dailyRent <= 0) return 0
  const days = Math.ceil((new Date(renewForm.newEndDate) - new Date(renewForm.currentEndDate)) / (1000 * 60 * 60 * 24))
  return Math.max(days, 0) * renewForm.dailyRent
})

const returnDialogVisible = ref(false)
const returnFormRef = ref(null)
const returnForm = reactive({
  orderId: null,
  orderNo: '',
  instrumentName: '',
  instrumentCondition: '',
  deductAmount: 0,
  refundMethod: '',
  remark: ''
})

const returnRules = {
  instrumentCondition: [{ required: true, message: '请选择乐器成色', trigger: 'change' }],
  refundMethod: [{ required: true, message: '请选择退款方式', trigger: 'change' }]
}

const statusTagType = (status) => {
  const map = { ACTIVE: 'success', RETURNED: 'info', OVERDUE: 'danger', CANCELLED: 'warning' }
  return map[status] || 'info'
}

const statusLabel = (status) => {
  const map = { ACTIVE: '租赁中', RETURNED: '已归还', OVERDUE: '逾期', CANCELLED: '已取消' }
  return map[status] || status
}

const fetchList = async () => {
  const { data } = await getOrderList({
    pageNum: pagination.pageNum,
    pageSize: pagination.pageSize,
    keyword: searchParams.keyword || undefined,
    status: searchParams.status || undefined
  })
  tableData.value = data.records
  pagination.total = data.total
}

const handleSearch = () => {
  pagination.pageNum = 1
  fetchList()
}

const handleReset = () => {
  searchParams.keyword = ''
  searchParams.status = ''
  pagination.pageNum = 1
  fetchList()
}

const handleCurrentChange = (row) => {
  selectedRow.value = row
}

const loadCustomers = async () => {
  const { data } = await getAllCustomers()
  customerOptions.value = data
}

const loadInstruments = async () => {
  const { data } = await getAvailableInstruments()
  instrumentOptions.value = data
}

const onInstrumentChange = (val) => {
  const inst = instrumentOptions.value.find(i => i.id === val)
  selectedInstrumentDailyRent.value = inst ? inst.dailyRent || 0 : 0
}

const openCreateDialog = () => {
  loadCustomers()
  loadInstruments()
  createDialogVisible.value = true
}

const resetCreateForm = () => {
  createFormRef.value?.resetFields()
  Object.assign(createForm, {
    customerId: null,
    instrumentId: null,
    dateRange: null,
    payMethod: '',
    remark: ''
  })
  selectedInstrumentDailyRent.value = 0
}

const handleCreateSubmit = async () => {
  const valid = await createFormRef.value.validate().catch(() => false)
  if (!valid) return
  const [startDate, endDate] = createForm.dateRange
  await createOrder({
    customerId: createForm.customerId,
    instrumentId: createForm.instrumentId,
    startDate,
    endDate,
    payMethod: createForm.payMethod,
    remark: createForm.remark
  })
  ElMessage.success('新建租赁成功')
  createDialogVisible.value = false
  fetchList()
}

const openRenewDialog = () => {
  if (!selectedRow.value) {
    ElMessage.warning('请先选择一条订单')
    return
  }
  if (selectedRow.value.status !== 'ACTIVE') {
    ElMessage.warning('只有租赁中的订单才能续租')
    return
  }
  fillRenewForm(selectedRow.value)
}

const openRenewForRow = (row) => {
  selectedRow.value = row
  fillRenewForm(row)
}

const fillRenewForm = (row) => {
  renewForm.orderId = row.id
  renewForm.orderNo = row.orderNo
  renewForm.instrumentName = row.instrumentName
  renewForm.currentEndDate = row.endDate
  renewForm.dailyRent = row.dailyRent || 0
  renewForm.newEndDate = ''
  renewForm.remark = ''
  renewDialogVisible.value = true
}

const resetRenewForm = () => {
  renewFormRef.value?.resetFields()
  Object.assign(renewForm, {
    orderId: null,
    orderNo: '',
    instrumentName: '',
    currentEndDate: '',
    dailyRent: 0,
    newEndDate: '',
    remark: ''
  })
}

const handleRenewSubmit = async () => {
  const valid = await renewFormRef.value.validate().catch(() => false)
  if (!valid) return
  await renewOrder({
    orderId: renewForm.orderId,
    newEndDate: renewForm.newEndDate,
    remark: renewForm.remark
  })
  ElMessage.success('续租成功')
  renewDialogVisible.value = false
  fetchList()
}

const openReturnDialog = () => {
  if (!selectedRow.value) {
    ElMessage.warning('请先选择一条订单')
    return
  }
  if (selectedRow.value.status !== 'ACTIVE' && selectedRow.value.status !== 'OVERDUE') {
    ElMessage.warning('只有租赁中或逾期的订单才能退租')
    return
  }
  fillReturnForm(selectedRow.value)
}

const openReturnForRow = (row) => {
  selectedRow.value = row
  fillReturnForm(row)
}

const fillReturnForm = (row) => {
  returnForm.orderId = row.id
  returnForm.orderNo = row.orderNo
  returnForm.instrumentName = row.instrumentName
  returnForm.instrumentCondition = ''
  returnForm.deductAmount = 0
  returnForm.refundMethod = ''
  returnForm.remark = ''
  returnDialogVisible.value = true
}

const resetReturnForm = () => {
  returnFormRef.value?.resetFields()
  Object.assign(returnForm, {
    orderId: null,
    orderNo: '',
    instrumentName: '',
    instrumentCondition: '',
    deductAmount: 0,
    refundMethod: '',
    remark: ''
  })
}

const handleReturnSubmit = async () => {
  const valid = await returnFormRef.value.validate().catch(() => false)
  if (!valid) return
  await returnOrder({
    orderId: returnForm.orderId,
    instrumentCondition: returnForm.instrumentCondition,
    deductAmount: returnForm.deductAmount,
    refundMethod: returnForm.refundMethod,
    remark: returnForm.remark
  })
  ElMessage.success('退租成功')
  returnDialogVisible.value = false
  fetchList()
}

onMounted(() => {
  fetchList()
})
</script>

<style scoped>
.order {
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
