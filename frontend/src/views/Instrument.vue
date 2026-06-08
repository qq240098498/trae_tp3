<template>
  <div class="instrument">
    <div class="toolbar">
      <el-input
        v-model="searchParams.keyword"
        placeholder="搜索名称/品牌/序列号"
        clearable
        style="width: 220px"
        @keyup.enter="handleSearch"
      />
      <el-select
        v-model="searchParams.category"
        placeholder="分类筛选"
        clearable
        style="width: 130px"
      >
        <el-option label="键盘" value="键盘" />
        <el-option label="弦乐" value="弦乐" />
        <el-option label="管乐" value="管乐" />
        <el-option label="打击" value="打击" />
        <el-option label="其他" value="其他" />
      </el-select>
      <el-select
        v-model="searchParams.status"
        placeholder="状态筛选"
        clearable
        style="width: 130px"
      >
        <el-option label="可租" value="AVAILABLE" />
        <el-option label="已租" value="RENTED" />
        <el-option label="维保中" value="MAINTENANCE" />
        <el-option label="已退役" value="RETIRED" />
      </el-select>
      <el-button type="primary" @click="handleSearch">搜索</el-button>
      <el-button @click="handleReset">重置</el-button>
      <el-button type="success" @click="handleAdd">新增乐器</el-button>
    </div>

    <el-table :data="tableData" border stripe style="width: 100%">
      <el-table-column prop="name" label="乐器名称" min-width="120" />
      <el-table-column prop="brand" label="品牌" min-width="100" />
      <el-table-column prop="category" label="分类" min-width="80">
        <template #default="{ row }">
          <el-tag>{{ row.category }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="model" label="型号" min-width="100" />
      <el-table-column prop="serialNo" label="序列号" min-width="120" />
      <el-table-column prop="purchasePrice" label="采购价格" min-width="100">
        <template #default="{ row }">
          ¥{{ row.purchasePrice?.toFixed(2) ?? '0.00' }}
        </template>
      </el-table-column>
      <el-table-column prop="dailyRent" label="日租金" min-width="100">
        <template #default="{ row }">
          ¥{{ row.dailyRent?.toFixed(2) ?? '0.00' }}
        </template>
      </el-table-column>
      <el-table-column prop="depositAmount" label="押金" min-width="100">
        <template #default="{ row }">
          ¥{{ row.depositAmount?.toFixed(2) ?? '0.00' }}
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" min-width="80">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.status)">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="cond" label="成色" min-width="80">
        <template #default="{ row }">
          {{ conditionLabel(row.cond) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
          <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
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
      :title="isEdit ? '编辑乐器' : '新增乐器'"
      width="600px"
      @close="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="乐器名称" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="品牌" prop="brand">
          <el-input v-model="form.brand" />
        </el-form-item>
        <el-form-item label="分类" prop="category">
          <el-select v-model="form.category" placeholder="请选择分类" style="width: 100%">
            <el-option label="键盘" value="键盘" />
            <el-option label="弦乐" value="弦乐" />
            <el-option label="管乐" value="管乐" />
            <el-option label="打击" value="打击" />
            <el-option label="其他" value="其他" />
          </el-select>
        </el-form-item>
        <el-form-item label="型号" prop="model">
          <el-input v-model="form.model" />
        </el-form-item>
        <el-form-item label="序列号" prop="serialNo">
          <el-input v-model="form.serialNo" />
        </el-form-item>
        <el-form-item label="采购价格" prop="purchasePrice">
          <el-input-number v-model="form.purchasePrice" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="日租金" prop="dailyRent">
          <el-input-number v-model="form.dailyRent" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="押金" prop="depositAmount">
          <el-input-number v-model="form.depositAmount" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="成色" prop="cond">
          <el-select v-model="form.cond" placeholder="请选择成色" style="width: 100%">
            <el-option label="全新" value="NEW" />
            <el-option label="良好" value="GOOD" />
            <el-option label="一般" value="FAIR" />
            <el-option label="较差" value="POOR" />
          </el-select>
        </el-form-item>
        <el-form-item label="图片URL" prop="imageUrl">
          <el-input v-model="form.imageUrl" />
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
import { getInstrumentList, addInstrument, updateInstrument, deleteInstrument } from '../api/instrument.js'

const searchParams = reactive({
  keyword: '',
  category: '',
  status: ''
})

const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

const tableData = ref([])

const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref(null)

const form = reactive({
  id: null,
  name: '',
  brand: '',
  category: '',
  model: '',
  serialNo: '',
  purchasePrice: null,
  dailyRent: null,
  depositAmount: null,
  cond: '',
  imageUrl: '',
  remark: ''
})

const rules = {
  name: [{ required: true, message: '请输入乐器名称', trigger: 'blur' }],
  dailyRent: [{ required: true, message: '请输入日租金', trigger: 'blur' }],
  depositAmount: [{ required: true, message: '请输入押金', trigger: 'blur' }]
}

const statusTagType = (status) => {
  const map = { AVAILABLE: 'success', RENTED: 'warning', MAINTENANCE: 'danger', RETIRED: 'info' }
  return map[status] || 'info'
}

const statusLabel = (status) => {
  const map = { AVAILABLE: '可租', RENTED: '已租', MAINTENANCE: '维保中', RETIRED: '已退役' }
  return map[status] || status
}

const conditionLabel = (condition) => {
  const map = { NEW: '全新', GOOD: '良好', FAIR: '一般', POOR: '较差' }
  return map[condition] || condition
}

const fetchList = async () => {
  const { data } = await getInstrumentList({
    pageNum: pagination.pageNum,
    pageSize: pagination.pageSize,
    keyword: searchParams.keyword || undefined,
    category: searchParams.category || undefined,
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
  searchParams.category = ''
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
    form[key] = row[key] ?? (key === 'id' ? row.id : null)
  })
  dialogVisible.value = true
}

const handleDelete = (row) => {
  ElMessageBox.confirm('确定删除该乐器吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    await deleteInstrument(row.id)
    ElMessage.success('删除成功')
    fetchList()
  }).catch(() => {})
}

const handleSubmit = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  if (isEdit.value) {
    await updateInstrument({ ...form })
    ElMessage.success('更新成功')
  } else {
    await addInstrument({ ...form })
    ElMessage.success('新增成功')
  }
  dialogVisible.value = false
  fetchList()
}

const resetForm = () => {
  formRef.value?.resetFields()
  Object.assign(form, {
    id: null,
    name: '',
    brand: '',
    category: '',
    model: '',
    serialNo: '',
    purchasePrice: null,
    dailyRent: null,
    depositAmount: null,
    cond: '',
    imageUrl: '',
    remark: ''
  })
}

onMounted(() => {
  fetchList()
})
</script>

<style scoped>
.instrument {
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
