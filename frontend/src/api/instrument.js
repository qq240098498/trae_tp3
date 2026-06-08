import request from '../utils/request'

export const getInstrumentList = (params) => request.get('/instrument/list', { params })
export const getInstrument = (id) => request.get(`/instrument/${id}`)
export const addInstrument = (data) => request.post('/instrument', data)
export const updateInstrument = (data) => request.put('/instrument', data)
export const deleteInstrument = (id) => request.delete(`/instrument/${id}`)
export const getAvailableInstruments = () => request.get('/instrument/available')
