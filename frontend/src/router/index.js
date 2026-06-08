import { createRouter, createWebHistory } from 'vue-router'
import Layout from '../components/Layout.vue'

const routes = [
  {
    path: '/',
    component: Layout,
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', name: 'Dashboard', component: () => import('../views/Dashboard.vue'), meta: { title: '工作台' } },
      { path: 'instrument', name: 'Instrument', component: () => import('../views/Instrument.vue'), meta: { title: '乐器档案' } },
      { path: 'customer', name: 'Customer', component: () => import('../views/Customer.vue'), meta: { title: '客户档案' } },
      { path: 'order', name: 'Order', component: () => import('../views/Order.vue'), meta: { title: '租赁管理' } },
      { path: 'deposit', name: 'Deposit', component: () => import('../views/Deposit.vue'), meta: { title: '押金管理' } },
      { path: 'maintenance', name: 'Maintenance', component: () => import('../views/Maintenance.vue'), meta: { title: '维保管理' } },
      { path: 'reminder', name: 'Reminder', component: () => import('../views/Reminder.vue'), meta: { title: '到期提醒' } },
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
