import request from '../utils/request'

export const getCustomerList = (params) => request.get('/customer/list', { params })
export const getCustomer = (id) => request.get(`/customer/${id}`)
export const addCustomer = (data) => request.post('/customer', data)
export const updateCustomer = (data) => request.put('/customer', data)
export const deleteCustomer = (id) => request.delete(`/customer/${id}`)
export const getAllCustomers = () => request.get('/customer/all')
