import request from '../utils/request'

export const getMaintenanceList = (params) => request.get('/maintenance/list', { params })
export const getMaintenance = (id) => request.get(`/maintenance/${id}`)
export const addMaintenance = (data) => request.post('/maintenance', data)
export const updateMaintenance = (data) => request.put('/maintenance', data)
