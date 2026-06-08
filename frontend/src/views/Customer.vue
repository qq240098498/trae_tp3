<template>
  <div class="customer">
    <div class="toolbar">
      <el-input
        v-model="searchParams.keyword"
        placeholder="搜索姓名/电话/身份证号"
        clearable
        style="width: 240px"
        @keyup.enter="handleSearch"
      />
      <el-button type="primary" @click="handleSearch">搜索</el-button>
      <el-button @click="handleReset">重置</el-button>
      <el-button type="success" @click="handleAdd">新增客户</el-button>
    </div>

    <el-table :data="tableData" border stripe style="width: 100%">
      <el-table-column prop="name" label="客户姓名" min-width="100" />
      <el-table-column prop="phone" label="联系电话" min-width="120" />
      <el-table-column prop="idCard" label="身份证号" min-width="180" />
      <el-table-column prop="address" label="地址" min-width="150" />
      <el-table-column prop="email" label="邮箱" min-width="150" />
      <el-table-column prop="remark" label="备注" min-width="120" />
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
      :title="isEdit ? '编辑客户' : '新增客户'"
      width="600px"
      @close="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="客户姓名" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="联系电话" prop="phone">
          <el-input v-model="form.phone" />
        </el-form-item>
        <el-form-item label="身份证号" prop="idCard">
          <el-input v-model="form.idCard" />
        </el-form-item>
        <el-form-item label="地址" prop="address">
          <el-input v-model="form.address" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" />
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
import { getCustomerList, addCustomer, updateCustomer, deleteCustomer } from '../api/customer.js'

const searchParams = reactive({
  keyword: ''
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
  phone: '',
  idCard: '',
  address: '',
  email: '',
  remark: ''
})

const rules = {
  name: [{ required: true, message: '请输入客户姓名', trigger: 'blur' }],
  phone: [{ required: true, message: '请输入联系电话', trigger: 'blur' }]
}

const fetchList = async () => {
  const { data } = await getCustomerList({
    pageNum: pagination.pageNum,
    pageSize: pagination.pageSize,
    keyword: searchParams.keyword || undefined
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
  ElMessageBox.confirm('确定删除该客户吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    await deleteCustomer(row.id)
    ElMessage.success('删除成功')
    fetchList()
  }).catch(() => {})
}

const handleSubmit = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  if (isEdit.value) {
    await updateCustomer({ ...form })
    ElMessage.success('更新成功')
  } else {
    await addCustomer({ ...form })
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
    phone: '',
    idCard: '',
    address: '',
    email: '',
    remark: ''
  })
}

onMounted(() => {
  fetchList()
})
</script>

<style scoped>
.customer {
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
