import request from '../utils/request'

export const getRepairList = (params) => request.get('/repair/list', { params })
export const getRepair = (id) => request.get(`/repair/${id}`)
export const addRepair = (data) => request.post('/repair', data)
export const updateRepair = (data) => request.put('/repair', data)
export const completeRepair = (data) => request.post('/repair/complete', data)
