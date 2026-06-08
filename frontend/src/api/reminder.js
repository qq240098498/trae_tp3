import request from '../utils/request'

export const getReminderList = (params) => request.get('/reminder/list', { params })
export const checkReminders = () => request.post('/reminder/check')
export const updateReminderStatus = (id, status) => request.put(`/reminder/${id}/status`, null, { params: { status } })
export const sendNotify = (id) => request.post(`/reminder/notify/${id}`)
export const sendAllPending = () => request.post('/reminder/notify-all')
