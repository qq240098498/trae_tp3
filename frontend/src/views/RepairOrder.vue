<template>
  <div class="repair">
    <div class="toolbar">
      <el-input
        v-model="searchParams.keyword"
        placeholder="搜索工单编号"
        clearable
        style="width: 200px"
        @keyup.enter="handleSearch"
      />
      <el-select
        v-model="searchParams.status"
        placeholder="状态筛选"
        clearable
        style="width: 140px"
      >
        <el-option label="待处理" value="PENDING" />
        <el-option label="进行中" value="IN_PROGRESS" />
        <el-option label="已完成" value="COMPLETED" />
      </el-select>
      <el-button type="primary" @click="handleSearch">搜索</el-button>
      <el-button @click="handleReset">重置</el-button>
      <el-button type="success" @click="handleAdd">新建工单</el-button>
    </div>

    <el-table :data="tableData" border stripe style="width: 100%">
      <el-table-column prop="orderNo" label="工单编号" min-width="140" />
      <el-table-column prop="instrumentName" label="乐器" min-width="120" />
      <el-table-column prop="repairType" label="维修类型" min-width="100">
        <template #default="{ row }">
          <el-tag>{{ row.repairType }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="damageDescription" label="损坏描述" min-width="160" show-overflow-tooltip />
      <el-table-column prop="description" label="维修描述" min-width="160" show-overflow-tooltip />
      <el-table-column prop="estimatedCost" label="预估费用" min-width="100">
        <template #default="{ row }">
          ¥{{ row.estimatedCost?.toFixed(2) ?? '0.00' }}
        </template>
      </el-table-column>
      <el-table-column prop="actualCost" label="实际费用" min-width="100">
        <template #default="{ row }">
          <span v-if="row.actualCost">¥{{ row.actualCost?.toFixed(2) }}</span>
          <span v-else style="color: #999">-</span>
        </template>
      </el-table-column>
      <el-table-column prop="assignee" label="负责人" min-width="90" />
      <el-table-column prop="status" label="状态" min-width="100">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.status)">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="remark" label="备注" min-width="120" show-overflow-tooltip />
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button
            v-if="row.status === 'PENDING'"
            type="primary"
            link
            @click="handleStartRepair(row)"
          >开始维修</el-button>
          <el-button
            v-if="row.status === 'IN_PROGRESS'"
            type="success"
            link
            @click="handleComplete(row)"
          >完成</el-button>
          <el-button
            v-if="row.status === 'COMPLETED'"
            type="info"
            link
            @click="handleViewDetail(row)"
          >查看</el-button>
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
      title="新建维修工单"
      width="600px"
      @close="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="乐器" prop="instrumentId">
          <el-select
            v-model="form.instrumentId"
            placeholder="请选择乐器"
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
        <el-form-item label="维修类型" prop="repairType">
          <el-select v-model="form.repairType" placeholder="请选择维修类型" style="width: 100%">
            <el-option label="外观修复" value="外观修复" />
            <el-option label="功能维修" value="功能维修" />
            <el-option label="配件更换" value="配件更换" />
            <el-option label="调音校正" value="调音校正" />
            <el-option label="其他" value="其他" />
          </el-select>
        </el-form-item>
        <el-form-item label="维修描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="预估费用" prop="estimatedCost">
          <el-input-number v-model="form.estimatedCost" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="负责人" prop="assignee">
          <el-input v-model="form.assignee" placeholder="请输入负责人" />
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
      v-model="completeDialogVisible"
      title="完成维修"
      width="600px"
      @close="resetCompleteForm"
    >
      <el-form ref="completeFormRef" :model="completeForm" :rules="completeRules" label-width="100px">
        <el-form-item label="工单编号">
          {{ completeForm.orderNo }}
        </el-form-item>
        <el-form-item label="乐器">
          {{ completeForm.instrumentName }}
        </el-form-item>
        <el-form-item label="实际费用" prop="actualCost">
          <el-input-number v-model="completeForm.actualCost" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="押金扣除" prop="deductDeposit">
          <el-switch v-model="completeForm.deductDeposit" />
          <span style="margin-left: 12px; color: #999; font-size: 13px">开启后将自动从押金中扣除维修费用</span>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="completeForm.remark" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="completeDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleCompleteSubmit">确定完成</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="detailDialogVisible"
      title="工单详情"
      width="600px"
    >
      <el-descriptions :column="2" border>
        <el-descriptions-item label="工单编号">{{ detailData.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="乐器">{{ detailData.instrumentName }}</el-descriptions-item>
        <el-descriptions-item label="维修类型">{{ detailData.repairType }}</el-descriptions-item>
        <el-descriptions-item label="负责人">{{ detailData.assignee || '-' }}</el-descriptions-item>
        <el-descriptions-item label="预估费用">¥{{ detailData.estimatedCost?.toFixed(2) ?? '0.00' }}</el-descriptions-item>
        <el-descriptions-item label="实际费用">¥{{ detailData.actualCost?.toFixed(2) ?? '0.00' }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ statusLabel(detailData.status) }}</el-descriptions-item>
        <el-descriptions-item label="关联维保记录">{{ detailData.maintenanceRecordId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="损坏描述" :span="2">{{ detailData.damageDescription || '-' }}</el-descriptions-item>
        <el-descriptions-item label="维修描述" :span="2">{{ detailData.description || '-' }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ detailData.remark || '-' }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button type="primary" @click="detailDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getRepairList, addRepair, updateRepair, completeRepair } from '../api/repair.js'
import { getInstrumentList } from '../api/instrument.js'

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
const instrumentOptions = ref([])

const dialogVisible = ref(false)
const formRef = ref(null)

const form = reactive({
  instrumentId: null,
  damageId: null,
  repairType: '',
  description: '',
  estimatedCost: 0,
  assignee: '',
  remark: ''
})

const rules = {
  instrumentId: [{ required: true, message: '请选择乐器', trigger: 'change' }],
  repairType: [{ required: true, message: '请选择维修类型', trigger: 'change' }],
  description: [{ required: true, message: '请输入维修描述', trigger: 'blur' }]
}

const completeDialogVisible = ref(false)
const completeFormRef = ref(null)
const completeForm = reactive({
  id: null,
  orderNo: '',
  instrumentName: '',
  actualCost: 0,
  deductDeposit: false,
  remark: ''
})

const completeRules = {
  actualCost: [{ required: true, message: '请输入实际费用', trigger: 'blur' }]
}

const detailDialogVisible = ref(false)
const detailData = ref({})

const statusTagType = (status) => {
  const map = { PENDING: 'warning', IN_PROGRESS: 'primary', COMPLETED: 'success' }
  return map[status] || 'info'
}

const statusLabel = (status) => {
  const map = { PENDING: '待处理', IN_PROGRESS: '进行中', COMPLETED: '已完成' }
  return map[status] || status
}

const fetchList = async () => {
  const { data } = await getRepairList({
    pageNum: pagination.pageNum,
    pageSize: pagination.pageSize,
    keyword: searchParams.keyword || undefined,
    status: searchParams.status || undefined
  })
  tableData.value = data.records
  pagination.total = data.total
}

const fetchInstruments = async () => {
  const { data } = await getInstrumentList({ pageNum: 1, pageSize: 9999 })
  instrumentOptions.value = data.records
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

const handleAdd = () => {
  fetchInstruments()
  dialogVisible.value = true
}

const handleSubmit = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  await addRepair({ ...form })
  ElMessage.success('工单创建成功')
  dialogVisible.value = false
  fetchList()
}

const resetForm = () => {
  formRef.value?.resetFields()
  Object.assign(form, {
    instrumentId: null,
    damageId: null,
    repairType: '',
    description: '',
    estimatedCost: 0,
    assignee: '',
    remark: ''
  })
}

const handleStartRepair = async (row) => {
  await updateRepair({ id: row.id, status: 'IN_PROGRESS' })
  ElMessage.success('维修已开始')
  fetchList()
}

const handleComplete = (row) => {
  completeForm.id = row.id
  completeForm.orderNo = row.orderNo
  completeForm.instrumentName = row.instrumentName
  completeForm.actualCost = row.estimatedCost || 0
  completeForm.deductDeposit = false
  completeForm.remark = ''
  completeDialogVisible.value = true
}

const handleCompleteSubmit = async () => {
  const valid = await completeFormRef.value.validate().catch(() => false)
  if (!valid) return
  await completeRepair({
    id: completeForm.id,
    actualCost: completeForm.actualCost,
    deductDeposit: completeForm.deductDeposit,
    remark: completeForm.remark
  })
  ElMessage.success('维修完成，已生成维保记录')
  completeDialogVisible.value = false
  fetchList()
}

const resetCompleteForm = () => {
  completeFormRef.value?.resetFields()
  Object.assign(completeForm, {
    id: null,
    orderNo: '',
    instrumentName: '',
    actualCost: 0,
    deductDeposit: false,
    remark: ''
  })
}

const handleViewDetail = (row) => {
  detailData.value = row
  detailDialogVisible.value = true
}

onMounted(() => {
  fetchList()
})
</script>

<style scoped>
.repair {
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
