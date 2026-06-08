<template>
  <div class="damage">
    <div class="toolbar">
      <el-input
        v-model="searchParams.keyword"
        placeholder="搜索订单编号"
        clearable
        style="width: 200px"
        @keyup.enter="handleSearch"
      />
      <el-select
        v-model="searchParams.severity"
        placeholder="严重程度"
        clearable
        style="width: 140px"
      >
        <el-option label="轻微" value="MINOR" />
        <el-option label="中等" value="MODERATE" />
        <el-option label="严重" value="SEVERE" />
      </el-select>
      <el-select
        v-model="searchParams.status"
        placeholder="状态筛选"
        clearable
        style="width: 140px"
      >
        <el-option label="已上报" value="REPORTED" />
        <el-option label="已创建工单" value="REPAIR_CREATED" />
        <el-option label="已修复" value="REPAIRED" />
      </el-select>
      <el-button type="primary" @click="handleSearch">搜索</el-button>
      <el-button @click="handleReset">重置</el-button>
      <el-button type="danger" @click="handleAdd">损坏登记</el-button>
    </div>

    <el-table :data="tableData" border stripe style="width: 100%">
      <el-table-column prop="orderNo" label="订单编号" min-width="140" />
      <el-table-column prop="instrumentName" label="乐器" min-width="120" />
      <el-table-column prop="customerName" label="客户" min-width="100" />
      <el-table-column prop="damageType" label="损坏类型" min-width="100">
        <template #default="{ row }">
          <el-tag>{{ row.damageType }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="severity" label="严重程度" min-width="100">
        <template #default="{ row }">
          <el-tag :type="severityTagType(row.severity)">{{ severityLabel(row.severity) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="description" label="描述" min-width="180" show-overflow-tooltip />
      <el-table-column prop="estimatedCost" label="预估费用" min-width="100">
        <template #default="{ row }">
          ¥{{ row.estimatedCost?.toFixed(2) ?? '0.00' }}
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" min-width="110">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.status)">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="remark" label="备注" min-width="140" show-overflow-tooltip />
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
          <el-button
            v-if="row.status === 'REPORTED'"
            type="warning"
            link
            @click="handleCreateRepair(row)"
          >创建工单</el-button>
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

    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑损坏' : '损坏登记'"
      width="600px"
      @close="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="租赁订单" prop="orderId">
          <el-select
            v-model="form.orderId"
            placeholder="请选择租赁订单"
            :disabled="isEdit"
            filterable
            style="width: 100%"
            @change="onOrderChange"
          >
            <el-option
              v-for="item in orderOptions"
              :key="item.id"
              :label="item.orderNo + ' - ' + item.instrumentName"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="乐器" prop="instrumentId">
          <el-select
            v-model="form.instrumentId"
            placeholder="请选择乐器"
            :disabled="isEdit"
            style="width: 100%"
          >
            <el-option
              v-for="item in instrumentOptions"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="损坏类型" prop="damageType">
          <el-select
            v-model="form.damageType"
            placeholder="请选择损坏类型"
            :disabled="isEdit"
            style="width: 100%"
          >
            <el-option label="外观损坏" value="外观损坏" />
            <el-option label="功能故障" value="功能故障" />
            <el-option label="配件丢失" value="配件丢失" />
            <el-option label="音质异常" value="音质异常" />
            <el-option label="其他" value="其他" />
          </el-select>
        </el-form-item>
        <el-form-item label="严重程度" prop="severity">
          <el-select
            v-model="form.severity"
            placeholder="请选择严重程度"
            :disabled="isEdit"
            style="width: 100%"
          >
            <el-option label="轻微" value="MINOR" />
            <el-option label="中等" value="MODERATE" />
            <el-option label="严重" value="SEVERE" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="预估费用" prop="estimatedCost">
          <el-input-number v-model="form.estimatedCost" :min="0" :precision="2" :disabled="isEdit" style="width: 100%" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="repairDialogVisible"
      title="创建维修工单"
      width="600px"
      @close="resetRepairForm"
    >
      <el-form ref="repairFormRef" :model="repairForm" :rules="repairRules" label-width="100px">
        <el-form-item label="损坏记录ID">
          {{ repairForm.damageId }}
        </el-form-item>
        <el-form-item label="乐器">
          {{ repairForm.instrumentName }}
        </el-form-item>
        <el-form-item label="损坏描述">
          {{ repairForm.damageDescription }}
        </el-form-item>
        <el-form-item label="维修类型" prop="repairType">
          <el-select v-model="repairForm.repairType" placeholder="请选择维修类型" style="width: 100%">
            <el-option label="外观修复" value="外观修复" />
            <el-option label="功能维修" value="功能维修" />
            <el-option label="配件更换" value="配件更换" />
            <el-option label="调音校正" value="调音校正" />
            <el-option label="其他" value="其他" />
          </el-select>
        </el-form-item>
        <el-form-item label="维修描述" prop="description">
          <el-input v-model="repairForm.description" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="预估费用" prop="estimatedCost">
          <el-input-number v-model="repairForm.estimatedCost" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="负责人" prop="assignee">
          <el-input v-model="repairForm.assignee" placeholder="请输入负责人" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="repairForm.remark" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="repairDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleRepairSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { getDamageList, addDamage, updateDamage } from '../api/damage.js'
import { addRepair } from '../api/repair.js'
import { getInstrumentList } from '../api/instrument.js'
import { getOrderList } from '../api/order.js'

const router = useRouter()

const searchParams = reactive({
  keyword: '',
  severity: '',
  status: ''
})

const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

const tableData = ref([])
const instrumentOptions = ref([])
const orderOptions = ref([])

const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref(null)

const form = reactive({
  id: null,
  orderId: null,
  instrumentId: null,
  customerId: null,
  damageType: '',
  description: '',
  severity: '',
  estimatedCost: 0,
  status: '',
  remark: ''
})

const rules = {
  orderId: [{ required: true, message: '请选择租赁订单', trigger: 'change' }],
  instrumentId: [{ required: true, message: '请选择乐器', trigger: 'change' }],
  damageType: [{ required: true, message: '请选择损坏类型', trigger: 'change' }],
  severity: [{ required: true, message: '请选择严重程度', trigger: 'change' }],
  description: [{ required: true, message: '请输入损坏描述', trigger: 'blur' }]
}

const repairDialogVisible = ref(false)
const repairFormRef = ref(null)
const repairForm = reactive({
  damageId: null,
  instrumentId: null,
  instrumentName: '',
  damageDescription: '',
  repairType: '',
  description: '',
  estimatedCost: 0,
  assignee: '',
  remark: ''
})

const repairRules = {
  repairType: [{ required: true, message: '请选择维修类型', trigger: 'change' }],
  description: [{ required: true, message: '请输入维修描述', trigger: 'blur' }]
}

const severityTagType = (severity) => {
  const map = { MINOR: 'info', MODERATE: 'warning', SEVERE: 'danger' }
  return map[severity] || 'info'
}

const severityLabel = (severity) => {
  const map = { MINOR: '轻微', MODERATE: '中等', SEVERE: '严重' }
  return map[severity] || severity
}

const statusTagType = (status) => {
  const map = { REPORTED: 'danger', REPAIR_CREATED: 'warning', REPAIRED: 'success' }
  return map[status] || 'info'
}

const statusLabel = (status) => {
  const map = { REPORTED: '已上报', REPAIR_CREATED: '已创建工单', REPAIRED: '已修复' }
  return map[status] || status
}

const fetchList = async () => {
  const { data } = await getDamageList({
    pageNum: pagination.pageNum,
    pageSize: pagination.pageSize,
    severity: searchParams.severity || undefined,
    status: searchParams.status || undefined
  })
  tableData.value = data.records
  pagination.total = data.total
}

const fetchInstruments = async () => {
  const { data } = await getInstrumentList({ pageNum: 1, pageSize: 9999 })
  instrumentOptions.value = data.records
}

const fetchOrders = async () => {
  const { data } = await getOrderList({ pageNum: 1, pageSize: 9999, status: 'ACTIVE' })
  orderOptions.value = data.records
}

const onOrderChange = (val) => {
  const order = orderOptions.value.find(o => o.id === val)
  if (order) {
    form.instrumentId = order.instrumentId
    form.customerId = order.customerId
  }
}

const handleSearch = () => {
  pagination.pageNum = 1
  fetchList()
}

const handleReset = () => {
  searchParams.keyword = ''
  searchParams.severity = ''
  searchParams.status = ''
  pagination.pageNum = 1
  fetchList()
}

const handleAdd = () => {
  isEdit.value = false
  fetchOrders()
  fetchInstruments()
  dialogVisible.value = true
}

const handleEdit = (row) => {
  isEdit.value = true
  Object.keys(form).forEach((key) => {
    form[key] = row[key] ?? null
  })
  dialogVisible.value = true
}

const handleSubmit = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  if (isEdit.value) {
    await updateDamage({ ...form })
    ElMessage.success('更新成功')
  } else {
    await addDamage({ ...form })
    ElMessage.success('登记成功')
  }
  dialogVisible.value = false
  fetchList()
}

const resetForm = () => {
  formRef.value?.resetFields()
  Object.assign(form, {
    id: null,
    orderId: null,
    instrumentId: null,
    customerId: null,
    damageType: '',
    description: '',
    severity: '',
    estimatedCost: 0,
    status: '',
    remark: ''
  })
}

const handleCreateRepair = (row) => {
  repairForm.damageId = row.id
  repairForm.instrumentId = row.instrumentId
  repairForm.instrumentName = row.instrumentName
  repairForm.damageDescription = row.description
  repairForm.estimatedCost = row.estimatedCost || 0
  repairForm.repairType = ''
  repairForm.description = ''
  repairForm.assignee = ''
  repairForm.remark = ''
  repairDialogVisible.value = true
}

const handleRepairSubmit = async () => {
  const valid = await repairFormRef.value.validate().catch(() => false)
  if (!valid) return
  await addRepair({
    damageId: repairForm.damageId,
    instrumentId: repairForm.instrumentId,
    repairType: repairForm.repairType,
    description: repairForm.description,
    estimatedCost: repairForm.estimatedCost,
    assignee: repairForm.assignee,
    remark: repairForm.remark
  })
  ElMessage.success('维修工单创建成功')
  repairDialogVisible.value = false
  fetchList()
  router.push('/repair')
}

const resetRepairForm = () => {
  repairFormRef.value?.resetFields()
  Object.assign(repairForm, {
    damageId: null,
    instrumentId: null,
    instrumentName: '',
    damageDescription: '',
    repairType: '',
    description: '',
    estimatedCost: 0,
    assignee: '',
    remark: ''
  })
}

onMounted(() => {
  fetchList()
})
</script>

<style scoped>
.damage {
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
