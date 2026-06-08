<template>
  <div class="maintenance">
    <div class="toolbar">
      <el-select
        v-model="searchParams.instrumentId"
        placeholder="乐器筛选"
        clearable
        style="width: 180px"
      >
        <el-option
          v-for="item in instrumentOptions"
          :key="item.id"
          :label="item.name"
          :value="item.id"
        />
      </el-select>
      <el-select
        v-model="searchParams.type"
        placeholder="维保类型"
        clearable
        style="width: 140px"
      >
        <el-option label="日常保养" value="日常保养" />
        <el-option label="维修" value="维修" />
        <el-option label="调音" value="调音" />
        <el-option label="更换配件" value="更换配件" />
      </el-select>
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
      <el-button type="success" @click="handleAdd">新增维保</el-button>
    </div>

    <el-table :data="tableData" border stripe style="width: 100%">
      <el-table-column prop="instrumentId" label="乐器ID" min-width="80" />
      <el-table-column prop="type" label="维保类型" min-width="100">
        <template #default="{ row }">
          <el-tag>{{ row.type }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="description" label="描述" min-width="160" show-overflow-tooltip />
      <el-table-column prop="cost" label="费用" min-width="100">
        <template #default="{ row }">
          ¥{{ row.cost || '0.00' }}
        </template>
      </el-table-column>
      <el-table-column prop="maintenanceDate" label="维保日期" min-width="120" />
      <el-table-column prop="status" label="状态" min-width="90">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.status)">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="remark" label="备注" min-width="140" show-overflow-tooltip />
      <el-table-column label="操作" width="100" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
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
      :title="isEdit ? '编辑维保' : '新增维保'"
      width="600px"
      @close="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
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
        <el-form-item label="维保类型" prop="type">
          <el-select
            v-model="form.type"
            placeholder="请选择维保类型"
            :disabled="isEdit"
            style="width: 100%"
          >
            <el-option label="日常保养" value="日常保养" />
            <el-option label="维修" value="维修" />
            <el-option label="调音" value="调音" />
            <el-option label="更换配件" value="更换配件" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="费用" prop="cost">
          <el-input v-model="form.cost" placeholder="请输入费用" :disabled="isEdit" style="width: 100%" />
        </el-form-item>
        <el-form-item label="维保日期" prop="maintenanceDate">
          <el-date-picker
            v-model="form.maintenanceDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="请选择日期"
            :disabled="isEdit"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="状态" prop="status" v-if="isEdit">
          <el-select v-model="form.status" placeholder="请选择状态" style="width: 100%">
            <el-option label="待处理" value="PENDING" />
            <el-option label="进行中" value="IN_PROGRESS" />
            <el-option label="已完成" value="COMPLETED" />
          </el-select>
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
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getMaintenanceList, addMaintenance, updateMaintenance } from '../api/maintenance.js'
import { getInstrumentList } from '../api/instrument.js'

const searchParams = reactive({
  instrumentId: '',
  type: '',
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
const isEdit = ref(false)
const formRef = ref(null)

const form = reactive({
  id: null,
  instrumentId: null,
  type: '',
  description: '',
  cost: '',
  maintenanceDate: '',
  status: 'PENDING',
  remark: ''
})

const rules = {
  instrumentId: [{ required: true, message: '请选择乐器', trigger: 'change' }],
  type: [{ required: true, message: '请选择维保类型', trigger: 'change' }],
  maintenanceDate: [{ required: true, message: '请选择维保日期', trigger: 'change' }]
}

const statusTagType = (status) => {
  const map = { PENDING: 'warning', IN_PROGRESS: 'primary', COMPLETED: 'success' }
  return map[status] || 'info'
}

const statusLabel = (status) => {
  const map = { PENDING: '待处理', IN_PROGRESS: '进行中', COMPLETED: '已完成' }
  return map[status] || status
}

const fetchList = async () => {
  const { data } = await getMaintenanceList({
    pageNum: pagination.pageNum,
    pageSize: pagination.pageSize,
    instrumentId: searchParams.instrumentId || undefined,
    type: searchParams.type || undefined,
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
  searchParams.instrumentId = ''
  searchParams.type = ''
  searchParams.status = ''
  pagination.pageNum = 1
  fetchList()
}

const handleAdd = () => {
  isEdit.value = false
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
    await updateMaintenance({ ...form })
    ElMessage.success('更新成功')
  } else {
    await addMaintenance({ ...form })
    ElMessage.success('新增成功')
  }
  dialogVisible.value = false
  fetchList()
}

const resetForm = () => {
  formRef.value?.resetFields()
  Object.assign(form, {
    id: null,
    instrumentId: null,
    type: '',
    description: '',
    cost: '',
    maintenanceDate: '',
    status: 'PENDING',
    remark: ''
  })
}

onMounted(() => {
  fetchList()
  fetchInstruments()
})
</script>

<style scoped>
.maintenance {
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
