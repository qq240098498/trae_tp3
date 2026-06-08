import request from '../utils/request'

export const getDamageList = (params) => request.get('/damage/list', { params })
export const getDamage = (id) => request.get(`/damage/${id}`)
export const addDamage = (data) => request.post('/damage', data)
export const updateDamage = (data) => request.put('/damage', data)
