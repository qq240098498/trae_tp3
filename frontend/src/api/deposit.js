import request from '../utils/request'

export const getDepositList = (params) => request.get('/deposit/list', { params })
export const addDeposit = (data) => request.post('/deposit', data)
