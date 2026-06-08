import request from '../utils/request'

export const getOrderList = (params) => request.get('/order/list', { params })
export const getOrder = (id) => request.get(`/order/${id}`)
export const createOrder = (data) => request.post('/order', data)
export const renewOrder = (data) => request.post('/order/renew', data)
export const returnOrder = (data) => request.post('/order/return', data)
