<template>
  <div class="points-container">
    <el-tabs v-model="activeTab" type="card">
      <el-tab-pane label="客户积分" name="customer">
        <div class="toolbar">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索客户姓名/手机号"
            clearable
            style="width: 240px"
            @keyup.enter="searchCustomers"
          />
          <el-button type="primary" @click="searchCustomers">搜索</el-button>
          <el-button @click="loadAllCustomers">刷新</el-button>
        </div>

        <el-table :data="customerPointsList" border stripe style="width: 100%">
          <el-table-column prop="customerId" label="客户ID" width="90" />
          <el-table-column label="客户姓名" min-width="120">
            <template #default="{ row }">
              {{ customerMap[row.customerId]?.name || '-' }}
            </template>
          </el-table-column>
          <el-table-column label="联系电话" min-width="120">
            <template #default="{ row }">
              {{ customerMap[row.customerId]?.phone || '-' }}
            </template>
          </el-table-column>
          <el-table-column prop="totalPoints" label="累计积分" width="100" align="right">
            <template #default="{ row }">
              <span style="color: #67C23A; font-weight: 600">{{ row.totalPoints || 0 }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="availablePoints" label="可用积分" width="100" align="right">
            <template #default="{ row }">
              <span style="color: #409EFF; font-weight: 600">{{ row.availablePoints || 0 }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="usedPoints" label="已用积分" width="100" align="right">
            <template #default="{ row }">
              <span style="color: #E6A23C">{{ row.usedPoints || 0 }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="180" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" link @click="viewRecords(row.customerId)">查看流水</el-button>
              <el-button type="warning" link @click="openAdjustDialog(row)">调整积分</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="积分配置" name="config">
        <el-row :gutter="20">
          <el-col :span="12" v-for="config in configList" :key="config.configKey">
            <el-card shadow="hover" style="margin-bottom: 16px">
              <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px">
                <div>
                  <div style="font-size: 16px; font-weight: 600">{{ config.configName }}</div>
                  <div style="color: #909399; font-size: 13px; margin-top: 4px">{{ config.description || '暂无描述' }}</div>
                </div>
                <el-button type="primary" link @click="openEditConfig(config)">修改</el-button>
              </div>
              <div style="font-size: 28px; font-weight: 700; color: #409EFF">
                {{ formatConfigValue(config) }}
              </div>
              <div style="color: #909399; font-size: 12px; margin-top: 4px">{{ configTip(config.configKey) }}</div>
            </el-card>
          </el-col>
        </el-row>

        <el-divider content-position="left">配置说明</el-divider>
        <el-alert type="info" :closable="false">
          <div style="line-height: 1.8">
            <div>• <b>消费积分获得比例</b>：每消费 1 元可获得的积分数（默认 1，即 1 元 = 1 积分）</div>
            <div>• <b>积分抵扣现金比例</b>：多少积分可抵扣 1 元（默认 100，即 100 积分 = 1 元）</div>
            <div>• <b>积分抵扣最高百分比</b>：积分抵扣金额最多占订单总金额的百分比（默认 30，即最多抵扣 30%）</div>
          </div>
        </el-alert>
      </el-tab-pane>

      <el-tab-pane label="积分流水" name="record">
        <div class="toolbar">
          <el-select
            v-model="recordCustomerId"
            placeholder="请选择客户"
            filterable
            style="width: 220px"
            @change="loadRecords"
          >
            <el-option
              v-for="c in allCustomers"
              :key="c.id"
              :label="c.name + ' - ' + c.phone"
              :value="c.id"
            />
          </el-select>
          <el-button type="primary" @click="loadRecords" :disabled="!recordCustomerId">查询</el-button>
          <el-button @click="clearRecord">清空</el-button>
        </div>

        <el-table :data="recordList" border stripe style="width: 100%">
          <el-table-column prop="createTime" label="时间" width="170" />
          <el-table-column prop="type" label="类型" width="110">
            <template #default="{ row }">
              <el-tag :type="typeTagType(row.type)">{{ typeLabel(row.type) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="points" label="积分变动" width="100" align="right">
            <template #default="{ row }">
              <span :style="{ color: row.points > 0 ? '#67C23A' : '#F56C6C', fontWeight: 600 }">
                {{ row.points > 0 ? '+' : '' }}{{ row.points }}
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="relatedAmount" label="关联金额" width="110" align="right">
            <template #default="{ row }">
              ¥{{ row.relatedAmount?.toFixed(2) ?? '-' }}
            </template>
          </el-table-column>
          <el-table-column prop="orderId" label="关联订单" width="100" />
          <el-table-column prop="remark" label="备注" min-width="200" show-overflow-tooltip />
        </el-table>
        <el-empty v-if="recordList.length === 0 && !loadingRecords" description="请选择客户查看积分流水" style="margin-top: 40px" />
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="adjustDialogVisible" title="调整客户积分" width="500px">
      <el-form label-width="100px">
        <el-form-item label="客户">
          {{ customerMap[adjustForm.customerId]?.name || '-' }}
        </el-form-item>
        <el-form-item label="当前可用">
          <span style="color: #409EFF; font-weight: 600; font-size: 16px">{{ adjustForm.currentAvailable || 0 }}</span>
        </el-form-item>
        <el-form-item label="调整方式">
          <el-radio-group v-model="adjustForm.type">
            <el-radio label="add">增加积分</el-radio>
            <el-radio label="deduct">扣减积分</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="积分数量">
          <el-input-number v-model="adjustForm.points" :min="1" :precision="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="adjustForm.remark" type="textarea" :rows="2" placeholder="请输入调整原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="adjustDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitAdjust">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="editConfigDialogVisible" title="修改积分配置" width="500px">
      <el-form label-width="120px">
        <el-form-item label="配置名称">
          {{ editingConfig?.configName }}
        </el-form-item>
        <el-form-item label="配置说明">
          <span style="color: #909399">{{ editingConfig?.description }}</span>
        </el-form-item>
        <el-form-item label="配置值">
          <el-input-number
            v-model="editingConfig.configValue"
            :min="0"
            :precision="2"
            :step="configStep"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="调整说明">
          <div style="color: #E6A23C; font-size: 13px; line-height: 1.6">{{ configTip(editingConfig?.configKey) }}</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editConfigDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitEditConfig">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { getAllCustomers } from '../api/customer.js'
import {
  getAllPointsConfig, updatePointsConfig, getDefaultConfigs,
  getCustomerPoints, getCustomerPointsRecords, adjustPoints
} from '../api/points.js'

const activeTab = ref('customer')
const searchKeyword = ref('')
const allCustomers = ref([])
const customerPointsList = ref([])
const customerMap = ref({})
const configList = ref([])

const loadingRecords = ref(false)
const recordCustomerId = ref(null)
const recordList = ref([])

const adjustDialogVisible = ref(false)
const adjustForm = reactive({
  customerId: null,
  currentAvailable: 0,
  type: 'add',
  points: 100,
  remark: ''
})

const editConfigDialogVisible = ref(false)
const editingConfig = ref(null)
const configStep = computed(() => {
  if (editingConfig.value?.configKey === 'MAX_DEDUCT_PERCENT') return 5
  if (editingConfig.value?.configKey === 'DEDUCT_RATE') return 10
  return 0.1
})

const searchCustomers = async () => {
  if (!searchKeyword.value.trim()) {
    customerPointsList.value = allCustomers.value.map(c => buildPointsData(c))
    return
  }
  const kw = searchKeyword.value.trim().toLowerCase()
  const matched = allCustomers.value.filter(c =>
    c.name?.toLowerCase().includes(kw) || c.phone?.includes(kw)
  )
  customerPointsList.value = matched.map(c => buildPointsData(c))
  for (const c of matched) {
    try {
      const { data } = await getCustomerPoints(c.id)
      if (data) updateCustomerPointsData(c.id, data)
    } catch {}
  }
}

const loadAllCustomers = async () => {
  try {
    const { data } = await getAllCustomers()
    allCustomers.value = data || []
    customerMap.value = {}
    data.forEach(c => { customerMap.value[c.id] = c })
    customerPointsList.value = data.map(c => buildPointsData(c))
    for (const c of data) {
      try {
        const res = await getCustomerPoints(c.id)
        if (res.data) updateCustomerPointsData(c.id, res.data)
      } catch {}
    }
  } catch (e) {
    ElMessage.error('加载客户列表失败')
  }
}

const buildPointsData = (customer) => ({
  id: null,
  customerId: customer.id,
  totalPoints: 0,
  availablePoints: 0,
  usedPoints: 0,
  expiredPoints: 0
})

const updateCustomerPointsData = (customerId, pointsData) => {
  const idx = customerPointsList.value.findIndex(p => p.customerId === customerId)
  if (idx >= 0) {
    customerPointsList.value[idx] = { ...pointsData }
  }
}

const loadConfigs = async () => {
  try {
    const { data } = await getAllPointsConfig()
    configList.value = data || []
  } catch (e) {
    ElMessage.error('加载配置失败')
  }
}

const viewRecords = (customerId) => {
  recordCustomerId.value = customerId
  activeTab.value = 'record'
  loadRecords()
}

const loadRecords = async () => {
  if (!recordCustomerId.value) return
  loadingRecords.value = true
  try {
    const { data } = await getCustomerPointsRecords(recordCustomerId.value)
    recordList.value = data || []
  } catch (e) {
    ElMessage.error('加载积分流水失败')
  } finally {
    loadingRecords.value = false
  }
}

const clearRecord = () => {
  recordCustomerId.value = null
  recordList.value = []
}

const openAdjustDialog = (row) => {
  adjustForm.customerId = row.customerId
  adjustForm.currentAvailable = row.availablePoints || 0
  adjustForm.type = 'add'
  adjustForm.points = 100
  adjustForm.remark = ''
  adjustDialogVisible.value = true
}

const submitAdjust = async () => {
  if (!adjustForm.points || adjustForm.points <= 0) {
    ElMessage.warning('请输入积分数量')
    return
  }
  const points = adjustForm.type === 'add' ? adjustForm.points : -adjustForm.points
  try {
    await adjustPoints({
      customerId: adjustForm.customerId,
      points,
      remark: adjustForm.remark
    })
    ElMessage.success('积分调整成功')
    adjustDialogVisible.value = false
    const { data } = await getCustomerPoints(adjustForm.customerId)
    if (data) updateCustomerPointsData(adjustForm.customerId, data)
  } catch (e) {
    ElMessage.error(e?.response?.data?.msg || '积分调整失败')
  }
}

const openEditConfig = (config) => {
  editingConfig.value = { ...config }
  editConfigDialogVisible.value = true
}

const submitEditConfig = async () => {
  try {
    await updatePointsConfig({
      configKey: editingConfig.value.configKey,
      configValue: editingConfig.value.configValue,
      description: editingConfig.value.description
    })
    ElMessage.success('配置修改成功')
    editConfigDialogVisible.value = false
    loadConfigs()
  } catch (e) {
    ElMessage.error('配置修改失败')
  }
}

const typeLabel = (type) => {
  const map = {
    EARN: '获得积分', DEDUCT: '积分抵扣',
    MANUAL_ADD: '手动增加', MANUAL_DEDUCT: '手动扣减', EXPIRE: '积分过期'
  }
  return map[type] || type
}

const typeTagType = (type) => {
  if (type === 'EARN' || type === 'MANUAL_ADD') return 'success'
  if (type === 'DEDUCT' || type === 'MANUAL_DEDUCT') return 'danger'
  if (type === 'EXPIRE') return 'info'
  return 'warning'
}

const formatConfigValue = (config) => {
  if (!config) return '-'
  if (config.configKey === 'EARN_RATE') return `${config.configValue} 积分 / 元`
  if (config.configKey === 'DEDUCT_RATE') return `${config.configValue} 积分 = 1 元`
  if (config.configKey === 'MAX_DEDUCT_PERCENT') return `${config.configValue} %`
  return config.configValue
}

const configTip = (key) => {
  if (key === 'EARN_RATE') return '例如：消费 100 元，可获得 100 × 1 = 100 积分'
  if (key === 'DEDUCT_RATE') return '例如：使用 200 积分，可抵扣 200 ÷ 100 = 2 元'
  if (key === 'MAX_DEDUCT_PERCENT') return '例如：订单 100 元，最多抵扣 100 × 30% = 30 元'
  return ''
}

onMounted(async () => {
  await loadAllCustomers()
  await loadConfigs()
})
</script>

<style scoped>
.points-container { padding: 20px; }
.toolbar { display: flex; align-items: center; gap: 12px; margin-bottom: 16px; flex-wrap: wrap; }
</style>
