<template>
  <div class="coupon-container">
    <el-tabs v-model="activeTab" type="card">
      <el-tab-pane label="优惠券模板" name="template">
        <div class="toolbar">
          <el-input
            v-model="templateSearch"
            placeholder="搜索模板名称"
            clearable
            style="width: 220px"
          />
          <el-select v-model="templateStatusFilter" placeholder="状态筛选" clearable style="width: 130px">
            <el-option label="启用中" value="ACTIVE" />
            <el-option label="已停用" value="INACTIVE" />
            <el-option label="已过期" value="EXPIRED" />
          </el-select>
          <el-button type="primary" @click="openCreateTemplateDialog"><el-icon><Plus /></el-icon>新建模板</el-button>
          <el-button @click="loadTemplates">刷新</el-button>
        </div>

        <el-table :data="filteredTemplates" border stripe style="width: 100%">
          <el-table-column prop="id" label="ID" width="70" />
          <el-table-column prop="name" label="模板名称" min-width="160" />
          <el-table-column label="类型" width="100">
            <template #default="{ row }">
              <el-tag :type="row.type === 'FIXED' ? 'success' : 'warning'">
                {{ row.type === 'FIXED' ? '满减券' : '折扣券' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="优惠值" width="110" align="right">
            <template #default="{ row }">
              <span v-if="row.type === 'FIXED'">¥{{ Number(row.discountValue).toFixed(2) }}</span>
              <span v-else>{{ Number(row.discountValue) }} 折</span>
            </template>
          </el-table-column>
          <el-table-column label="最低消费" width="100" align="right">
            <template #default="{ row }">¥{{ Number(row.minAmount || 0).toFixed(2) }}</template>
          </el-table-column>
          <el-table-column label="有效期" min-width="180">
            <template #default="{ row }">
              <span v-if="row.validDays">发放后 {{ row.validDays }} 天</span>
              <span v-else-if="row.validStartDate && row.validEndDate">
                {{ row.validStartDate }} ~ {{ row.validEndDate }}
              </span>
              <span v-else>永久有效</span>
            </template>
          </el-table-column>
          <el-table-column label="积分" width="90">
            <template #default="{ row }">
              <el-tag :type="row.pointsCompatible ? 'success' : 'danger'" size="small">
                {{ row.pointsCompatible ? '可组合' : '互斥' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="发放/已用" width="110" align="center">
            <template #default="{ row }">
              <span style="color: #409EFF">{{ row.issuedCount || 0 }}</span>
              <span style="color: #909399"> / </span>
              <span style="color: #E6A23C">{{ row.usedCount || 0 }}</span>
              <span v-if="row.totalCount > 0" style="color: #909399">
                <br/>(限 {{ row.totalCount }} 张)
              </span>
              <span v-else style="color: #67C23A; font-size: 12px">不限</span>
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="90">
            <template #default="{ row }">
              <el-tag :type="statusTagType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="260" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" link size="small" @click="openIssueDialog(row)">发放</el-button>
              <el-button type="warning" link size="small" @click="openEditTemplateDialog(row)">编辑</el-button>
              <el-button type="info" link size="small" @click="viewTemplateRecords(row)">流水</el-button>
              <el-popconfirm title="确定删除此模板?" @confirm="handleDeleteTemplate(row)">
                <template #reference>
                  <el-button type="danger" link size="small">删除</el-button>
                </template>
              </el-popconfirm>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="发放优惠券" name="issue">
        <el-card shadow="hover" style="margin-bottom: 16px">
          <template #header>
            <div style="display: flex; justify-content: space-between; align-items: center">
              <span style="font-weight: 600">🎫 发放新优惠券</span>
            </div>
          </template>
          <el-form :model="issueForm" label-width="100px" style="max-width: 600px">
            <el-form-item label="选择模板" required>
              <el-select v-model="issueForm.templateId" placeholder="请选择优惠券模板" style="width: 100%" filterable>
                <el-option
                  v-for="t in templateList"
                  :key="t.id"
                  :label="t.name + (t.type === 'FIXED' ? ' [满' + (t.minAmount||0) + '减' + t.discountValue + ']' : ' [' + t.discountValue + '折]')"
                  :value="t.id"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="发放方式">
              <el-radio-group v-model="issueForm.mode">
                <el-radio-button label="single">单客户发放</el-radio-button>
                <el-radio-button label="batch">批量发放</el-radio-button>
              </el-radio-group>
            </el-form-item>
            <el-form-item v-if="issueForm.mode === 'single'" label="选择客户" required>
              <el-select v-model="issueForm.customerId" placeholder="请选择客户" style="width: 100%" filterable>
                <el-option
                  v-for="c in customerList"
                  :key="c.id"
                  :label="c.name + ' - ' + c.phone"
                  :value="c.id"
                />
              </el-select>
            </el-form-item>
            <el-form-item v-else label="目标客户" required>
              <el-select
                v-model="issueForm.customerIds"
                multiple
                collapse-tags
                collapse-tags-tooltip
                placeholder="请选择要发放的客户（可多选）"
                style="width: 100%"
                filterable
              >
                <el-option
                  v-for="c in customerList"
                  :key="c.id"
                  :label="c.name + ' - ' + c.phone"
                  :value="c.id"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="操作人">
              <el-input v-model="issueForm.operator" placeholder="如 admin" />
            </el-form-item>
            <el-form-item label="备注">
              <el-input v-model="issueForm.remark" type="textarea" :rows="2" placeholder="发放备注信息" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleIssueSubmit">确认发放</el-button>
              <el-button @click="resetIssueForm">重置</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="用户优惠券" name="userCoupons">
        <div class="toolbar">
          <el-select
            v-model="couponCustomerId"
            placeholder="请选择客户"
            filterable
            style="width: 240px"
            @change="loadCustomerCoupons"
          >
            <el-option
              v-for="c in customerList"
              :key="c.id"
              :label="c.name + ' - ' + c.phone"
              :value="c.id"
            />
          </el-select>
          <el-select v-model="couponStatusFilter" placeholder="状态筛选" clearable style="width: 130px">
            <el-option label="可用" value="AVAILABLE" />
            <el-option label="已使用" value="USED" />
            <el-option label="已过期" value="EXPIRED" />
          </el-select>
          <el-button type="primary" @click="loadCustomerCoupons" :disabled="!couponCustomerId">查询</el-button>
          <el-button type="warning" @click="triggerExpireCheck">
            <el-icon><Refresh /></el-icon>检查过期
          </el-button>
        </div>

        <el-table :data="filteredCustomerCoupons" border stripe style="width: 100%">
          <el-table-column prop="couponNo" label="优惠券编号" min-width="200" />
          <el-table-column label="模板名称" min-width="140">
            <template #default="{ row }">{{ templateMap[row.templateId]?.name || '-' }}</template>
          </el-table-column>
          <el-table-column label="类型" width="90">
            <template #default="{ row }">
              <el-tag :type="row.type === 'FIXED' ? 'success' : 'warning'" size="small">
                {{ row.type === 'FIXED' ? '满减' : '折扣' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="优惠值" width="100" align="right">
            <template #default="{ row }">
              <span v-if="row.type === 'FIXED'">¥{{ Number(row.discountValue).toFixed(2) }}</span>
              <span v-else>{{ Number(row.discountValue) }} 折</span>
            </template>
          </el-table-column>
          <el-table-column label="有效期" min-width="200">
            <template #default="{ row }">
              <div style="font-size: 13px">{{ row.validStartDate }} ~ {{ row.validEndDate }}</div>
              <el-tag v-if="isNearExpire(row)" type="danger" size="small" effect="plain" style="margin-top: 4px">
                {{ daysLeft(row) }}天后过期
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="积分组合" width="90">
            <template #default="{ row }">
              <el-tag :type="row.pointsCompatible ? 'success' : 'danger'" size="small">
                {{ row.pointsCompatible ? '可组合' : '互斥' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="使用订单" width="100">
            <template #default="{ row }">
              <span v-if="row.orderId">Order#{{ row.orderId }}</span>
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="90">
            <template #default="{ row }">
              <el-tag :type="couponStatusTag(row.status)">{{ couponStatusLabel(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="160" fixed="right">
            <template #default="{ row }">
              <el-button type="info" link size="small" @click="viewCouponRecords(row)">流水</el-button>
              <el-popconfirm
                v-if="row.status === 'AVAILABLE'"
                title="确定撤回此优惠券?"
                @confirm="handleRevokeCoupon(row)"
              >
                <template #reference>
                  <el-button type="danger" link size="small">撤回</el-button>
                </template>
              </el-popconfirm>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="流水记录" name="records">
        <div class="toolbar">
          <el-radio-group v-model="recordQueryMode">
            <el-radio-button label="customer">按客户查询</el-radio-button>
            <el-radio-button label="template">按模板查询</el-radio-button>
            <el-radio-button label="order">按订单查询</el-radio-button>
            <el-radio-button label="coupon">按优惠券查询</el-radio-button>
          </el-radio-group>
          <el-select
            v-if="recordQueryMode === 'customer'"
            v-model="recordCustomerId"
            placeholder="选择客户"
            filterable
            style="width: 220px"
            @change="loadRecords"
          >
            <el-option v-for="c in customerList" :key="c.id" :label="c.name + ' - ' + c.phone" :value="c.id" />
          </el-select>
          <el-select
            v-if="recordQueryMode === 'template'"
            v-model="recordTemplateId"
            placeholder="选择模板"
            filterable
            style="width: 220px"
            @change="loadRecords"
          >
            <el-option v-for="t in templateList" :key="t.id" :label="t.name" :value="t.id" />
          </el-select>
          <el-input
            v-if="recordQueryMode === 'order'"
            v-model="recordOrderId"
            placeholder="输入订单ID"
            style="width: 180px"
            @keyup.enter="loadRecords"
          />
          <el-input
            v-if="recordQueryMode === 'coupon'"
            v-model="recordCouponId"
            placeholder="输入优惠券ID"
            style="width: 180px"
            @keyup.enter="loadRecords"
          />
          <el-button type="primary" @click="loadRecords">查询</el-button>
          <el-button @click="clearRecords">清空</el-button>
        </div>

        <el-table :data="recordList" border stripe style="width: 100%">
          <el-table-column prop="id" label="ID" width="70" />
          <el-table-column prop="createTime" label="时间" min-width="170">
            <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
          </el-table-column>
          <el-table-column label="操作类型" width="100">
            <template #default="{ row }">
              <el-tag :type="recordTypeTag(row.type)">{{ recordTypeLabel(row.type) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="couponId" label="券ID" width="80" />
          <el-table-column label="客户" min-width="120">
            <template #default="{ row }">{{ customerMap[row.customerId]?.name || ('#' + row.customerId) }}</template>
          </el-table-column>
          <el-table-column prop="orderId" label="关联订单" width="110">
            <template #default="{ row }">
              <span v-if="row.orderId">#{{ row.orderId }}</span>
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column prop="discountAmount" label="优惠金额" width="100" align="right">
            <template #default="{ row }">
              <span v-if="row.discountAmount" style="color: #67C23A; font-weight: 600">-¥{{ Number(row.discountAmount).toFixed(2) }}</span>
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column prop="operator" label="操作人" width="100">
            <template #default="{ row }">
              <span v-if="row.operator">{{ row.operator }}</span>
              <span v-else style="color: #909399">系统</span>
            </template>
          </el-table-column>
          <el-table-column prop="remark" label="备注" min-width="200" show-overflow-tooltip />
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="templateDialogVisible" :title="editingTemplate?.id ? '编辑模板' : '新建模板'" width="600px">
      <el-form ref="templateFormRef" :model="editingTemplate" :rules="templateRules" label-width="120px">
        <el-form-item label="模板名称" prop="name">
          <el-input v-model="editingTemplate.name" placeholder="如：新人满100减20券" />
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-radio-group v-model="editingTemplate.type">
            <el-radio-button label="FIXED">满减券</el-radio-button>
            <el-radio-button label="PERCENT">折扣券</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="优惠值" prop="discountValue">
          <el-input-number v-model="editingTemplate.discountValue" :precision="2" :min="0" style="width: 200px" />
          <span style="margin-left: 8px; color: #909399; font-size: 13px">
            {{ editingTemplate.type === 'FIXED' ? '满减金额（元）' : '折扣值（如 9 表示9折）' }}
          </span>
        </el-form-item>
        <el-form-item label="最低使用金额" prop="minAmount">
          <el-input-number v-model="editingTemplate.minAmount" :precision="2" :min="0" style="width: 200px" />
          <span style="margin-left: 8px; color: #909399; font-size: 13px">0 表示无门槛</span>
        </el-form-item>
        <el-form-item v-if="editingTemplate.type === 'PERCENT'" label="最大优惠额">
          <el-input-number v-model="editingTemplate.maxDiscountAmount" :precision="2" :min="0" style="width: 200px" />
          <span style="margin-left: 8px; color: #909399; font-size: 13px">折扣券最高可抵扣金额，空表示不限制</span>
        </el-form-item>
        <el-form-item label="积分组合" prop="pointsCompatible">
          <el-radio-group v-model="editingTemplate.pointsCompatible">
            <el-radio-button :label="true">可组合使用</el-radio-button>
            <el-radio-button :label="false">互斥使用</el-radio-button>
          </el-radio-group>
          <div style="color: #909399; font-size: 12px; margin-top: 4px">
            互斥：使用该优惠券时不能同时使用积分抵扣
          </div>
        </el-form-item>
        <el-form-item label="有效期设置">
          <el-radio-group v-model="validityMode" @change="onValidityModeChange">
            <el-radio-button label="days">发放后 N 天有效</el-radio-button>
            <el-radio-button label="fixed">固定起止日期</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="validityMode === 'days'" label="有效天数">
          <el-input-number v-model="editingTemplate.validDays" :min="1" style="width: 200px" />
        </el-form-item>
        <el-form-item v-else label="有效期范围">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始"
            end-placeholder="结束"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="发放总量">
          <el-input-number v-model="editingTemplate.totalCount" :min="-1" style="width: 200px" />
          <span style="margin-left: 8px; color: #909399; font-size: 13px">-1 表示不限量</span>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="editingTemplate.status">
            <el-radio-button label="ACTIVE">启用</el-radio-button>
            <el-radio-button label="INACTIVE">停用</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="editingTemplate.description" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="templateDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleTemplateSubmit">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="issueDialogVisible" title="快速发放优惠券" width="500px">
      <el-form label-width="100px">
        <el-form-item label="模板名称">
          <el-tag type="success" size="large">{{ quickIssueTemplate?.name }}</el-tag>
        </el-form-item>
        <el-form-item label="目标客户">
          <el-select v-model="quickIssueCustomerId" placeholder="请选择客户" filterable style="width: 100%">
            <el-option v-for="c in customerList" :key="c.id" :label="c.name + ' - ' + c.phone" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="quickIssueRemark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="issueDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleQuickIssue">确认发放</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Refresh } from '@element-plus/icons-vue'
import {
  getCouponTemplates, createCouponTemplate, updateCouponTemplate, deleteCouponTemplate,
  issueCoupon, issueCouponsBatch,
  getCustomerCoupons, revokeCoupon, checkAndExpireCoupons,
  getCouponRecordsByCustomerId, getCouponRecordsByTemplateId, getCouponRecordsByOrderId, getCouponRecordsByCouponId
} from '../api/coupon.js'
import { getAllCustomers } from '../api/customer.js'

const activeTab = ref('template')

const templateList = ref([])
const templateSearch = ref('')
const templateStatusFilter = ref('')
const templateMap = computed(() => {
  const m = {}
  templateList.value.forEach(t => (m[t.id] = t))
  return m
})
const filteredTemplates = computed(() => {
  return templateList.value.filter(t => {
    if (templateSearch.value && !t.name.includes(templateSearch.value)) return false
    if (templateStatusFilter.value && t.status !== templateStatusFilter.value) return false
    return true
  })
})

const customerList = ref([])
const customerMap = computed(() => {
  const m = {}
  customerList.value.forEach(c => (m[c.id] = c))
  return m
})

const templateDialogVisible = ref(false)
const templateFormRef = ref(null)
const editingTemplate = reactive({
  id: null,
  name: '',
  type: 'FIXED',
  discountValue: 10,
  minAmount: 0,
  maxDiscountAmount: null,
  validDays: 30,
  validStartDate: null,
  validEndDate: null,
  pointsCompatible: true,
  totalCount: -1,
  status: 'ACTIVE',
  description: ''
})
const validityMode = ref('days')
const dateRange = ref(null)

const templateRules = {
  name: [{ required: true, message: '请输入模板名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择类型', trigger: 'change' }],
  discountValue: [{ required: true, message: '请输入优惠值', trigger: 'blur' }],
  minAmount: [{ required: true, message: '请输入最低使用金额', trigger: 'blur' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
}

const issueForm = reactive({
  mode: 'single',
  templateId: null,
  customerId: null,
  customerIds: [],
  operator: 'admin',
  remark: ''
})

const issueDialogVisible = ref(false)
const quickIssueTemplate = ref(null)
const quickIssueCustomerId = ref(null)
const quickIssueRemark = ref('')

const couponCustomerId = ref(null)
const couponStatusFilter = ref('')
const customerCoupons = ref([])
const filteredCustomerCoupons = computed(() => {
  return customerCoupons.value.filter(c => {
    if (couponStatusFilter.value && c.status !== couponStatusFilter.value) return false
    return true
  })
})

const recordQueryMode = ref('customer')
const recordCustomerId = ref(null)
const recordTemplateId = ref(null)
const recordOrderId = ref('')
const recordCouponId = ref('')
const recordList = ref([])

const formatTime = (t) => {
  if (!t) return '-'
  const d = new Date(t)
  return d.getFullYear() + '-' + String(d.getMonth()+1).padStart(2,'0') + '-' + String(d.getDate()).padStart(2,'0')
    + ' ' + String(d.getHours()).padStart(2,'0') + ':' + String(d.getMinutes()).padStart(2,'0') + ':' + String(d.getSeconds()).padStart(2,'0')
}

const statusTagType = (s) => ({ ACTIVE: 'success', INACTIVE: 'info', EXPIRED: 'danger' }[s] || 'info')
const statusLabel = (s) => ({ ACTIVE: '启用', INACTIVE: '停用', EXPIRED: '过期' }[s] || s)
const couponStatusTag = (s) => ({ AVAILABLE: 'success', USED: 'primary', EXPIRED: 'danger' }[s] || 'info')
const couponStatusLabel = (s) => ({ AVAILABLE: '可用', USED: '已使用', EXPIRED: '已过期' }[s] || s)
const recordTypeTag = (t) => ({ ISSUE: 'success', USE: 'primary', EXPIRE: 'danger', REVOKE: 'warning' }[t] || 'info')
const recordTypeLabel = (t) => ({ ISSUE: '发放', USE: '使用', EXPIRE: '过期', REVOKE: '撤回' }[t] || t)

const isNearExpire = (row) => {
  if (row.status !== 'AVAILABLE') return false
  const days = daysLeft(row)
  return days >= 0 && days <= 7
}
const daysLeft = (row) => {
  if (!row.validEndDate) return -1
  return Math.ceil((new Date(row.validEndDate) - new Date()) / (1000 * 60 * 60 * 24))
}

const loadTemplates = async () => {
  const { data } = await getCouponTemplates()
  templateList.value = data || []
}

const loadCustomers = async () => {
  const { data } = await getAllCustomers()
  customerList.value = data || []
}

const openCreateTemplateDialog = () => {
  Object.assign(editingTemplate, {
    id: null, name: '', type: 'FIXED', discountValue: 10, minAmount: 0,
    maxDiscountAmount: null, validDays: 30, validStartDate: null, validEndDate: null,
    pointsCompatible: true, totalCount: -1, status: 'ACTIVE', description: ''
  })
  validityMode.value = 'days'
  dateRange.value = null
  templateDialogVisible.value = true
}

const openEditTemplateDialog = (row) => {
  Object.assign(editingTemplate, { ...row })
  if (row.validDays && row.validDays > 0) {
    validityMode.value = 'days'
  } else {
    validityMode.value = 'fixed'
    dateRange.value = [row.validStartDate, row.validEndDate]
  }
  templateDialogVisible.value = true
}

const onValidityModeChange = () => {
  if (validityMode.value === 'days') {
    editingTemplate.validStartDate = null
    editingTemplate.validEndDate = null
  } else {
    editingTemplate.validDays = null
  }
}

const handleTemplateSubmit = async () => {
  const valid = await templateFormRef.value.validate().catch(() => false)
  if (!valid) return

  if (validityMode.value === 'fixed' && dateRange.value) {
    editingTemplate.validStartDate = dateRange.value[0]
    editingTemplate.validEndDate = dateRange.value[1]
  }

  if (editingTemplate.id) {
    await updateCouponTemplate(editingTemplate)
    ElMessage.success('模板更新成功')
  } else {
    await createCouponTemplate(editingTemplate)
    ElMessage.success('模板创建成功')
  }
  templateDialogVisible.value = false
  loadTemplates()
}

const handleDeleteTemplate = async (row) => {
  await deleteCouponTemplate(row.id)
  ElMessage.success('删除成功')
  loadTemplates()
}

const openIssueDialog = (row) => {
  quickIssueTemplate.value = row
  quickIssueCustomerId.value = null
  quickIssueRemark.value = ''
  issueDialogVisible.value = true
}

const handleQuickIssue = async () => {
  if (!quickIssueCustomerId.value) {
    ElMessage.warning('请选择客户')
    return
  }
  try {
    await issueCoupon({
      templateId: quickIssueTemplate.value.id,
      customerId: quickIssueCustomerId.value,
      operator: 'admin',
      remark: quickIssueRemark.value || '快速发放'
    })
    ElMessage.success('发放成功')
    issueDialogVisible.value = false
    loadTemplates()
  } catch (e) {
    ElMessage.error(e?.response?.data?.msg || '发放失败')
  }
}

const handleIssueSubmit = async () => {
  if (!issueForm.templateId) {
    ElMessage.warning('请选择模板')
    return
  }
  try {
    if (issueForm.mode === 'single') {
      if (!issueForm.customerId) {
        ElMessage.warning('请选择客户')
        return
      }
      await issueCoupon({
        templateId: issueForm.templateId,
        customerId: issueForm.customerId,
        operator: issueForm.operator,
        remark: issueForm.remark
      })
      ElMessage.success('发放成功')
    } else {
      if (!issueForm.customerIds || issueForm.customerIds.length === 0) {
        ElMessage.warning('请选择客户')
        return
      }
      const { data } = await issueCouponsBatch({
        templateId: issueForm.templateId,
        customerIds: issueForm.customerIds,
        operator: issueForm.operator,
        remark: issueForm.remark
      })
      ElMessage.success(`批量发放成功，共 ${data.count} 张`)
    }
    resetIssueForm()
    loadTemplates()
  } catch (e) {
    ElMessage.error(e?.response?.data?.msg || '发放失败')
  }
}

const resetIssueForm = () => {
  Object.assign(issueForm, {
    mode: 'single', templateId: null, customerId: null, customerIds: [],
    operator: 'admin', remark: ''
  })
}

const loadCustomerCoupons = async () => {
  if (!couponCustomerId.value) return
  const { data } = await getCustomerCoupons(couponCustomerId.value)
  customerCoupons.value = data || []
}

const triggerExpireCheck = async () => {
  await checkAndExpireCoupons()
  ElMessage.success('已检查过期优惠券')
  if (couponCustomerId.value) loadCustomerCoupons()
}

const handleRevokeCoupon = async (row) => {
  try {
    await revokeCoupon(row.id, { operator: 'admin', remark: '管理员撤回' })
    ElMessage.success('已撤回')
    loadCustomerCoupons()
  } catch (e) {
    ElMessage.error(e?.response?.data?.msg || '撤回失败')
  }
}

const loadRecords = async () => {
  try {
    let res
    if (recordQueryMode.value === 'customer' && recordCustomerId.value) {
      res = await getCouponRecordsByCustomerId(recordCustomerId.value)
    } else if (recordQueryMode.value === 'template' && recordTemplateId.value) {
      res = await getCouponRecordsByTemplateId(recordTemplateId.value)
    } else if (recordQueryMode.value === 'order' && recordOrderId.value) {
      res = await getCouponRecordsByOrderId(Number(recordOrderId.value))
    } else if (recordQueryMode.value === 'coupon' && recordCouponId.value) {
      res = await getCouponRecordsByCouponId(Number(recordCouponId.value))
    } else {
      ElMessage.warning('请先选择查询条件')
      return
    }
    recordList.value = res?.data || []
  } catch (e) {
    ElMessage.error('查询失败')
  }
}

const clearRecords = () => {
  recordCustomerId.value = null
  recordTemplateId.value = null
  recordOrderId.value = ''
  recordCouponId.value = ''
  recordList.value = []
}

const viewCouponRecords = (row) => {
  activeTab.value = 'records'
  recordQueryMode.value = 'coupon'
  recordCouponId.value = String(row.id)
  loadRecords()
}

const viewTemplateRecords = (row) => {
  activeTab.value = 'records'
  recordQueryMode.value = 'template'
  recordTemplateId.value = row.id
  loadRecords()
}

onMounted(() => {
  loadTemplates()
  loadCustomers()
})
</script>

<style scoped>
.coupon-container { padding: 20px; }
.toolbar { display: flex; align-items: center; gap: 12px; margin-bottom: 16px; flex-wrap: wrap; }
</style>
