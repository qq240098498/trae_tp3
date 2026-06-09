import request from '../utils/request'

export const getCouponTemplates = () => request.get('/coupons/templates')
export const getActiveCouponTemplates = () => request.get('/coupons/templates/active')
export const getCouponTemplate = (id) => request.get(`/coupons/templates/${id}`)
export const createCouponTemplate = (data) => request.post('/coupons/templates', data)
export const updateCouponTemplate = (data) => request.put('/coupons/templates', data)
export const deleteCouponTemplate = (id) => request.delete(`/coupons/templates/${id}`)

export const issueCoupon = (data) => request.post('/coupons/issue', data)
export const issueCouponsBatch = (data) => request.post('/coupons/issue/batch', data)

export const getCustomerCoupons = (customerId) => request.get(`/coupons/customer/${customerId}`)
export const getCustomerAvailableCoupons = (customerId) => request.get(`/coupons/customer/${customerId}/available`)
export const getCustomerAvailableCouponsForAmount = (customerId, amount) => request.get(`/coupons/customer/${customerId}/available-for-amount`, { params: { amount } })

export const getCouponById = (id) => request.get(`/coupons/${id}`)
export const getCouponByNo = (couponNo) => request.get(`/coupons/no/${couponNo}`)

export const revokeCoupon = (id, data) => request.post(`/coupons/${id}/revoke`, data)
export const checkAndExpireCoupons = () => request.post('/coupons/check-expire')

export const getCouponRecordsByCouponId = (couponId) => request.get(`/coupons/records/coupon/${couponId}`)
export const getCouponRecordsByCustomerId = (customerId) => request.get(`/coupons/records/customer/${customerId}`)
export const getCouponRecordsByOrderId = (orderId) => request.get(`/coupons/records/order/${orderId}`)
export const getCouponRecordsByTemplateId = (templateId) => request.get(`/coupons/records/template/${templateId}`)

export const calculateCouponDiscount = (couponId, orderAmount) => request.get('/coupons/discount/calculate', { params: { couponId, orderAmount } })
