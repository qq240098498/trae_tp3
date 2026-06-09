import request from '../utils/request'

export const getAllPointsConfig = () => request.get('/points/config')
export const getPointsConfig = (key) => request.get(`/points/config/${key}`)
export const updatePointsConfig = (data) => request.post('/points/config', data)
export const getDefaultConfigs = () => request.get('/points/config/defaults')
export const getCustomerPoints = (customerId) => request.get(`/points/customer/${customerId}`)
export const getCustomerPointsRecords = (customerId) => request.get(`/points/customer/${customerId}/records`)
export const calculatePoints = (params) => request.get('/points/calculate', { params })
export const adjustPoints = (data) => request.post('/points/adjust', data)
