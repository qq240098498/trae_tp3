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
        <template #default="{ row }">¥{{ row.dailyRent?.toFixed(2) ?? '0.00' }}</template>
      </el-table-column>
      <el-table-column prop="totalRent" label="总租金" min-width="90">
        <template #default="{ row }">¥{{ row.totalRent?.toFixed(2) ?? '0.00' }}</template>
      </el-table-column>
      <el-table-column prop="usedPoints" label="使用积分" min-width="90" align="right">
        <template #default="{ row }">
          <span v-if="row.usedPoints > 0" style="color: #E6A23C">{{ row.usedPoints }}</span>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column prop="pointsDeductAmount" label="积分抵扣" min-width="90" align="right">
        <template #default="{ row }">
          <span v-if="row.pointsDeductAmount > 0" style="color: #67C23A">-¥{{ row.pointsDeductAmount?.toFixed(2) }}</span>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column prop="couponDeductAmount" label="优惠券抵扣" min-width="100" align="right">
        <template #default="{ row }">
          <span v-if="row.couponDeductAmount > 0" style="color: #E91E63; font-weight: 500">-¥{{ row.couponDeductAmount?.toFixed(2) }}</span>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column prop="actualPayAmount" label="实付金额" min-width="100" align="right">
        <template #default="{ row }">
          <span style="color: #F56C6C; font-weight: 600">¥{{ row.actualPayAmount?.toFixed(2) ?? row.totalRent?.toFixed(2) ?? '0.00' }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="earnedPoints" label="获得积分" min-width="90" align="right">
        <template #default="{ row }">
          <span v-if="row.earnedPoints > 0" style="color: #409EFF; font-weight: 600">+{{ row.earnedPoints }}</span>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column prop="depositAmount" label="押金" min-width="90">
        <template #default="{ row }">¥{{ row.depositAmount?.toFixed(2) ?? '0.00' }}</template>
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

    <el-dialog v-model="createDialogVisible" title="新建租赁" width="650px" @close="resetCreateForm">
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="100px">
        <el-form-item label="客户" prop="customerId">
          <el-select v-model="createForm.customerId" placeholder="请选择客户" style="width: 100%" @change="onCustomerChange">
            <el-option
              v-for="c in customerOptions"
              :key="c.id"
              :label="c.name + ' (' + c.phone + ')'"
              :value="c.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item v-if="currentCustomerPoints !== null" label="可用积分">
          <div style="display: flex; align-items: center; gap: 12px">
            <el-tag type="success" effect="dark" size="large">
              <span style="font-size: 16px; font-weight: 600">{{ currentCustomerPoints }}</span> 积分
            </el-tag>
            <span v-if="currentCustomerPoints > 0" style="color: #909399; font-size: 13px">
              可抵扣约 ¥{{ (currentCustomerPoints / deductRate).toFixed(2) }}
            </span>
          </div>
        </el-form-item>
        <el-form-item label="乐器" prop="instrumentId">
          <el-select v-model="createForm.instrumentId" placeholder="请选择乐器" style="width: 100%" @change="onInstrumentChange">
            <el-option
              v-for="i in instrumentOptions"
              :key="i.id"
              :label="i.name + ' (日租¥' + i.dailyRent + ')'"
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
            @change="onDateChange"
          />
        </el-form-item>
        <el-form-item label="使用积分抵扣">
          <el-switch
            v-model="createForm.usePoints"
            :disabled="!canUsePoints"
            active-text="使用积分"
            inactive-text="不使用"
            @change="onUsePointsChange"
          />
          <el-tooltip v-if="!pointsInfo.pointsCompatible && createForm.useCouponId" effect="dark" content="当前优惠券与积分抵扣互斥，请先取消选择优惠券">
            <el-tag type="danger" size="small" effect="plain" style="margin-left: 10px">🔒 互斥券已禁用积分</el-tag>
          </el-tooltip>
        </el-form-item>
        <el-form-item v-if="createForm.usePoints && canUsePoints" label="抵扣积分">
          <div style="width: 100%">
            <el-slider
              v-model="createForm.usePointsAmount"
              :min="minPointsSlider"
              :max="maxPointsSlider"
              :step="pointsSliderStep"
              :marks="sliderMarks"
              show-input
              @input="calculatePointsInfo"
              style="margin-top: 8px"
            />
            <div style="color: #909399; font-size: 12px; margin-top: 6px">
              最高可抵扣 {{ maxPointsSlider }} 积分 = ¥{{ maxDeductAmount.toFixed(2) }}
              （订单金额 {{ maxDeductPercent }}% 上限）
            </div>
          </div>
        </el-form-item>

        <el-form-item label="选择优惠券">
          <div style="width: 100%">
            <el-select
              v-model="createForm.useCouponId"
              placeholder="请选择优惠券（选填）"
              style="width: 100%"
              clearable
              @change="onCouponChange"
            >
              <el-option
                v-for="c in availableCoupons"
                :key="c.id"
                :value="c.id"
              >
                <div style="display: flex; justify-content: space-between; align-items: center; width: 100%">
                  <div>
                    <el-tag :type="c.type === 'FIXED' ? 'success' : 'warning'" size="small" style="margin-right: 8px">
                      {{ c.type === 'FIXED' ? '满减' : '折扣' }}
                    </el-tag>
                    <span style="font-weight: 600">
                      <template v-if="c.type === 'FIXED'">
                        满¥{{ Number(c.minAmount || 0).toFixed(0) }}减¥{{ Number(c.discountValue).toFixed(0) }}
                      </template>
                      <template v-else>
                        {{ Number(c.discountValue) }}折
                      </template>
                    </span>
                    <span style="color: #909399; margin-left: 10px; font-size: 12px">{{ c.couponNo }}</span>
                  </div>
                  <div style="text-align: right">
                    <el-tag v-if="c.pointsCompatible" type="success" size="small" effect="plain">✓ 可组合积分</el-tag>
                    <el-tag v-else type="danger" size="small" effect="plain">✕ 互斥</el-tag>
                    <div style="color: #E6A23C; font-size: 12px; margin-top: 2px">
                      {{ c.validStartDate }}~{{ c.validEndDate }}
                    </div>
                  </div>
                </div>
              </el-option>
            </el-select>
            <div v-if="availableCoupons.length === 0 && createForm.customerId" style="color: #909399; font-size: 12px; margin-top: 6px">
              <el-icon style="vertical-align: -2px"><InfoFilled /></el-icon>
              当前客户暂无满足订单金额的可用优惠券
            </div>
            <el-alert
              v-if="pointsInfo.message"
              :title="pointsInfo.message"
              type="warning"
              :closable="false"
              show-icon
              style="margin-top: 8px"
            />
          </div>
        </el-form-item>

        <el-divider content-position="left">费用明细</el-divider>
        <div class="fee-detail">
          <div class="fee-row">
            <span class="fee-label">总租金</span>
            <span class="fee-value">¥{{ pointsInfo.totalRent?.toFixed(2) || '0.00' }}</span>
          </div>
          <div v-if="pointsInfo.couponDeductAmount > 0" class="fee-row">
            <span class="fee-label">
              🎟️ 优惠券抵扣
              <el-tag v-if="selectedCouponInfo" size="small" effect="plain" type="success" style="margin-left: 6px">
                {{ selectedCouponInfo.type === 'FIXED' ? '满减券' : selectedCouponInfo.discountValue + '折券' }}
              </el-tag>
            </span>
            <span class="fee-value coupon">-¥{{ pointsInfo.couponDeductAmount?.toFixed(2) }}</span>
          </div>
          <div v-if="pointsInfo.deductAmount > 0" class="fee-row">
            <span class="fee-label">积分抵扣（{{ pointsInfo.usePoints }} 积分）</span>
            <span class="fee-value deduct">-¥{{ pointsInfo.deductAmount?.toFixed(2) }}</span>
          </div>
          <div class="fee-row total">
            <span class="fee-label">实付金额</span>
            <span class="fee-value">¥{{ pointsInfo.actualPayAmount?.toFixed(2) || '0.00' }}</span>
          </div>
          <div v-if="pointsInfo.willEarnPoints > 0" class="fee-row earn">
            <span class="fee-label">🎉 本次消费预计获得</span>
            <span class="fee-value"><el-tag type="primary" effect="dark">+{{ pointsInfo.willEarnPoints }} 积分</el-tag></span>
          </div>
          <div v-if="pointsInfo.couponDeductAmount > 0 || pointsInfo.deductAmount > 0" class="fee-row">
            <span class="fee-label" style="color: #67C23A; font-weight: 600">💸 合计已省</span>
            <span class="fee-value" style="color: #67C23A; font-weight: 700; font-size: 15px">
              ¥{{ (Number(pointsInfo.couponDeductAmount || 0) + Number(pointsInfo.deductAmount || 0)).toFixed(2) }}
            </span>
          </div>
        </div>

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
        <el-button type="primary" @click="handleCreateSubmit">确定下单</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="renewDialogVisible" title="续租" width="600px" @close="resetRenewForm">
      <el-form ref="renewFormRef" :model="renewForm" :rules="renewRules" label-width="100px">
        <el-form-item label="订单编号">{{ renewForm.orderNo }}</el-form-item>
        <el-form-item label="乐器">{{ renewForm.instrumentName }}</el-form-item>
        <el-form-item label="当前到期日">{{ renewForm.currentEndDate }}</el-form-item>
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
        <el-form-item v-if="additionalRent > 0" label="续租费用">¥{{ additionalRent.toFixed(2) }}</el-form-item>
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
        <el-form-item label="订单编号">{{ returnForm.orderNo }}</el-form-item>
        <el-form-item label="乐器">{{ returnForm.instrumentName }}</el-form-item>
        <el-form-item v-if="estimatedEarnPoints > 0" label="将获得积分">
          <el-tag type="primary" effect="dark" size="large">
            <span style="font-size: 15px">+{{ estimatedEarnPoints }} 积分</span>
          </el-tag>
          <span style="color: #909399; font-size: 13px; margin-left: 10px">归还订单后自动发放</span>
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
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { InfoFilled } from '@element-plus/icons-vue'
import { getOrderList, createOrder, renewOrder, returnOrder, calculateOrderPoints, calculateOrderCoupon } from '../api/order.js'
import { getAvailableInstruments } from '../api/instrument.js'
import { getAllCustomers } from '../api/customer.js'
import { getCustomerPoints, getDefaultConfigs } from '../api/points.js'
import { getCustomerAvailableCouponsForAmount } from '../api/coupon.js'

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

const deductRate = ref(100)
const earnRate = ref(1)
const maxDeductPercent = ref(30)

const createDialogVisible = ref(false)
const createFormRef = ref(null)
const createForm = reactive({
  customerId: null,
  instrumentId: null,
  dateRange: null,
  payMethod: '',
  remark: '',
  usePoints: false,
  usePointsAmount: 0,
  useCouponId: null
})

const currentCustomerPoints = ref(null)
const availableCoupons = ref([])
const selectedCouponInfo = ref(null)

const createRules = {
  customerId: [{ required: true, message: '请选择客户', trigger: 'change' }],
  instrumentId: [{ required: true, message: '请选择乐器', trigger: 'change' }],
  dateRange: [{ required: true, message: '请选择租赁日期', trigger: 'change' }],
  payMethod: [{ required: true, message: '请选择支付方式', trigger: 'change' }]
}

const selectedInstrumentDailyRent = ref(0)

const pointsInfo = reactive({
  totalRent: 0,
  deductAmount: 0,
  usePoints: 0,
  actualPayAmount: 0,
  willEarnPoints: 0,
  maxDeductAmount: 0,
  couponDeductAmount: 0,
  selectedCoupon: null,
  pointsCompatible: true,
  message: ''
})

const maxDeductAmount = computed(() => pointsInfo.maxDeductAmount || 0)

const canUsePoints = computed(() => {
  if (!pointsInfo.pointsCompatible) return false
  return !!(currentCustomerPoints.value && currentCustomerPoints.value > 0
    && createForm.customerId && createForm.instrumentId
    && createForm.dateRange && createForm.dateRange.length === 2)
})

const minPointsSlider = computed(() => Math.min(deductRate.value, (currentCustomerPoints.value || 0)))
const maxPointsSlider = computed(() => {
  const byPercent = (pointsInfo.maxDeductAmount || 0) * deductRate.value
  return Math.min(Math.floor(byPercent), (currentCustomerPoints.value || 0))
})

const pointsSliderStep = computed(() => Math.max(1, Math.floor(deductRate.value / 2)))

const sliderMarks = computed(() => {
  const max = maxPointsSlider.value
  if (max <= 0) return {}
  const marks = {}
  const step = Math.ceil(max / 4)
  for (let i = 0; i <= 4; i++) {
    const val = Math.round(i * step)
    if (val <= max) marks[val] = val + ''
  }
  return marks
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

const estimatedEarnPoints = ref(0)

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

const loadPointsConfigs = async () => {
  try {
    const { data } = await getDefaultConfigs()
    if (data) {
      earnRate.value = Number(data.earnRate) || 1
      deductRate.value = Number(data.deductRate) || 100
      maxDeductPercent.value = Number(data.maxDeductPercent) || 30
    }
  } catch {}
}

const onInstrumentChange = (val) => {
  const inst = instrumentOptions.value.find(i => i.id === val)
  selectedInstrumentDailyRent.value = inst ? inst.dailyRent || 0 : 0
  calculatePointsInfo()
  loadAvailableCoupons()
}

const onCustomerChange = async (customerId) => {
  currentCustomerPoints.value = null
  availableCoupons.value = []
  createForm.usePoints = false
  createForm.usePointsAmount = 0
  createForm.useCouponId = null
  pointsInfo.usePoints = 0
  pointsInfo.deductAmount = 0
  pointsInfo.selectedCoupon = null
  pointsInfo.couponDeductAmount = 0
  pointsInfo.pointsCompatible = true
  pointsInfo.message = ''
  if (customerId) {
    try {
      const { data } = await getCustomerPoints(customerId)
      currentCustomerPoints.value = data?.availablePoints || 0
    } catch {
      currentCustomerPoints.value = 0
    }
  }
  calculatePointsInfo()
}

const onDateChange = () => {
  calculatePointsInfo()
  loadAvailableCoupons()
}

const onUsePointsChange = (val) => {
  if (val && currentCustomerPoints.value > 0) {
    createForm.usePointsAmount = Math.min(maxPointsSlider.value, currentCustomerPoints.value)
  } else {
    createForm.usePointsAmount = 0
  }
  calculatePointsInfo()
}

const onCouponChange = async (val) => {
  if (val) {
    const coupon = availableCoupons.value.find(c => c.id === val)
    selectedCouponInfo.value = coupon
    if (coupon && !coupon.pointsCompatible) {
      createForm.usePoints = false
      createForm.usePointsAmount = 0
    }
  } else {
    selectedCouponInfo.value = null
  }
  calculatePointsInfo()
}

const loadAvailableCoupons = async () => {
  if (!createForm.customerId || !createForm.instrumentId || !createForm.dateRange || createForm.dateRange.length !== 2) {
    availableCoupons.value = []
    return
  }
  try {
    const [start, end] = createForm.dateRange
    const days = Math.ceil((new Date(end) - new Date(start)) / (1000 * 60 * 60 * 24))
    const total = Math.max(days, 0) * selectedInstrumentDailyRent.value
    if (total > 0) {
      const { data } = await getCustomerAvailableCouponsForAmount(createForm.customerId, total)
      availableCoupons.value = data || []
    }
  } catch {}
}

const calculatePointsInfo = async () => {
  if (!createForm.customerId || !createForm.instrumentId || !createForm.dateRange || createForm.dateRange.length !== 2) {
    pointsInfo.totalRent = 0
    pointsInfo.deductAmount = 0
    pointsInfo.usePoints = 0
    pointsInfo.actualPayAmount = 0
    pointsInfo.willEarnPoints = 0
    pointsInfo.maxDeductAmount = 0
    pointsInfo.couponDeductAmount = 0
    pointsInfo.selectedCoupon = null
    pointsInfo.pointsCompatible = true
    pointsInfo.message = ''
    return
  }
  try {
    const [startDate, endDate] = createForm.dateRange
    const usePoints = createForm.usePoints ? createForm.usePointsAmount : 0
    const { data } = await calculateOrderCoupon({
      customerId: createForm.customerId,
      instrumentId: createForm.instrumentId,
      startDate,
      endDate,
      couponId: createForm.useCouponId || undefined,
      usePoints: usePoints || undefined
    })
    if (data) {
      pointsInfo.totalRent = Number(data.totalRent) || 0
      pointsInfo.deductAmount = Number(data.pointsDeductAmount) || 0
      pointsInfo.usePoints = Number(data.usePoints) || 0
      pointsInfo.actualPayAmount = Number(data.actualPayAmount) || pointsInfo.totalRent
      pointsInfo.willEarnPoints = Number(data.willEarnPoints) || 0
      pointsInfo.maxDeductAmount = Number(data.maxDeductAmount) || 0
      pointsInfo.couponDeductAmount = Number(data.couponDeductAmount) || 0
      pointsInfo.selectedCoupon = data.selectedCoupon || null
      pointsInfo.pointsCompatible = data.pointsCompatible !== false
      pointsInfo.message = data.message || ''
    }
  } catch (e) {
    // fallback to local calculation
    const [start, end] = createForm.dateRange
    const days = Math.ceil((new Date(end) - new Date(start)) / (1000 * 60 * 60 * 24))
    const total = Math.max(days, 0) * selectedInstrumentDailyRent.value
    pointsInfo.totalRent = total
    const max = total * maxDeductPercent.value / 100
    pointsInfo.maxDeductAmount = max
    let useAmt = 0, usePts = 0
    if (createForm.usePoints && currentCustomerPoints.value > 0 && pointsInfo.pointsCompatible) {
      const pts = Math.min(createForm.usePointsAmount, currentCustomerPoints.value)
      const tempAmt = pts / deductRate.value
      if (tempAmt > max) {
        useAmt = max
        usePts = Math.floor(max * deductRate.value)
      } else {
        useAmt = tempAmt
        usePts = pts
      }
    }
    pointsInfo.usePoints = usePts
    pointsInfo.deductAmount = Number(useAmt.toFixed(2))
    let couponDiscount = 0
    if (createForm.useCouponId && availableCoupons.value.length > 0) {
      const c = availableCoupons.value.find(x => x.id === createForm.useCouponId)
      if (c) {
        if (c.type === 'FIXED' && total >= (c.minAmount || 0)) {
          couponDiscount = Number(c.discountValue || 0)
        } else if (c.type === 'PERCENT' && total >= (c.minAmount || 0)) {
          const pct = Number(c.discountValue || 10) / 10
          couponDiscount = total * (1 - pct)
          if (c.maxDiscountAmount && couponDiscount > Number(c.maxDiscountAmount)) {
            couponDiscount = Number(c.maxDiscountAmount)
          }
        }
        couponDiscount = Number(couponDiscount.toFixed(2))
      }
    }
    pointsInfo.couponDeductAmount = couponDiscount
    pointsInfo.actualPayAmount = Number((total - couponDiscount - useAmt).toFixed(2))
    pointsInfo.willEarnPoints = Math.round(total * earnRate.value)
  }
}

watch([() => createForm.usePoints, () => createForm.usePointsAmount, () => createForm.useCouponId], () => {
  calculatePointsInfo()
})

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
    remark: '',
    usePoints: false,
    usePointsAmount: 0,
    useCouponId: null
  })
  selectedInstrumentDailyRent.value = 0
  currentCustomerPoints.value = null
  availableCoupons.value = []
  selectedCouponInfo.value = null
  Object.assign(pointsInfo, {
    totalRent: 0, deductAmount: 0, usePoints: 0,
    actualPayAmount: 0, willEarnPoints: 0, maxDeductAmount: 0,
    couponDeductAmount: 0, selectedCoupon: null, pointsCompatible: true, message: ''
  })
}

const handleCreateSubmit = async () => {
  const valid = await createFormRef.value.validate().catch(() => false)
  if (!valid) return
  const [startDate, endDate] = createForm.dateRange
  try {
    await createOrder({
      customerId: createForm.customerId,
      instrumentId: createForm.instrumentId,
      startDate,
      endDate,
      payMethod: createForm.payMethod,
      remark: createForm.remark,
      usePoints: createForm.usePoints,
      usePointsAmount: createForm.usePoints ? pointsInfo.usePoints : 0,
      useCouponId: createForm.useCouponId
    })
    let msg = '新建租赁成功'
    let savings = []
    if (pointsInfo.couponDeductAmount > 0) {
      savings.push(`优惠券省 ¥${pointsInfo.couponDeductAmount.toFixed(2)}`)
    }
    if (pointsInfo.usePoints > 0) {
      savings.push(`${pointsInfo.usePoints} 积分省 ¥${pointsInfo.deductAmount.toFixed(2)}`)
    }
    if (savings.length > 0) {
      msg += `（${savings.join('，')}）`
    }
    if (pointsInfo.willEarnPoints > 0) {
      msg += `，归还后将获得 ${pointsInfo.willEarnPoints} 积分`
    }
    ElMessage.success(msg)
    createDialogVisible.value = false
    fetchList()
  } catch (e) {
    ElMessage.error(e?.response?.data?.msg || '新建租赁失败')
  }
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
  estimatedEarnPoints.value = Math.round((Number(row.totalRent || 0) + Number(row.overdueFee || 0)) * earnRate.value)
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
  estimatedEarnPoints.value = 0
}

const handleReturnSubmit = async () => {
  const valid = await returnFormRef.value.validate().catch(() => false)
  if (!valid) return
  try {
    await returnOrder({
      orderId: returnForm.orderId,
      instrumentCondition: returnForm.instrumentCondition,
      deductAmount: returnForm.deductAmount,
      refundMethod: returnForm.refundMethod,
      remark: returnForm.remark
    })
    let msg = '退租成功'
    if (estimatedEarnPoints.value > 0) {
      msg += `，已获得 ${estimatedEarnPoints.value} 积分 🎉`
    }
    ElMessage.success(msg)
    returnDialogVisible.value = false
    fetchList()
  } catch (e) {
    ElMessage.error(e?.response?.data?.msg || '退租失败')
  }
}

onMounted(() => {
  fetchList()
  loadPointsConfigs()
})
</script>

<style scoped>
.order { padding: 20px; }
.toolbar { display: flex; align-items: center; gap: 12px; margin-bottom: 16px; flex-wrap: wrap; }

.fee-detail {
  background: #f5f7fa;
  border-radius: 8px;
  padding: 16px 20px;
  margin-bottom: 20px;
}
.fee-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
  font-size: 14px;
}
.fee-label { color: #606266; }
.fee-value { color: #303133; font-weight: 500; }
.fee-value.deduct { color: #67C23A; font-weight: 600; }
.fee-value.coupon { color: #E91E63; font-weight: 600; }
.fee-row.total {
  border-top: 1px dashed #dcdfe6;
  margin-top: 8px;
  padding-top: 16px;
}
.fee-row.total .fee-label { font-size: 15px; font-weight: 600; color: #303133; }
.fee-row.total .fee-value { font-size: 20px; font-weight: 700; color: #F56C6C; }
.fee-row.earn { padding-top: 12px; }
</style>
