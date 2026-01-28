import { createRouter, createWebHistory } from 'vue-router'
import MonthlyBudgetView from '@/views/MonthlyBudgetView.vue'
import YearlyBudgetView from '@/views/YearlyBudgetView.vue'

const routes = [
  {
    path: '/',
    redirect: () => {
      const now = new Date()
      return `/budget/${now.getFullYear()}/${now.getMonth() + 1}`
    }
  },
  {
    path: '/budget/:year/:month',
    name: 'monthly',
    component: MonthlyBudgetView,
    props: route => ({
      year: parseInt(route.params.year),
      month: parseInt(route.params.month)
    })
  },
  {
    path: '/budget/:year',
    name: 'yearly',
    component: YearlyBudgetView,
    props: route => ({
      year: parseInt(route.params.year)
    })
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
