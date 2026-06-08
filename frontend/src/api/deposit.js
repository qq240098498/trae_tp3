import request from '../utils/request'

export const getDepositList = (params) => request.get('/deposit/list', { params })
export const getAvailableDeposit = (orderId) => request.get('/deposit/available', { params: { orderId } })
export const collectDeposit = (data) => request.post('/deposit/collect', data)
export const refundDeposit = (data) => request.post('/deposit/refund', data)
export const deductDeposit = (data) => request.post('/deposit/deduct', data)
