import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  headers: {
    'Content-Type': 'application/json'
  }
})

export const budgetApi = {
  getBudget(year, month, createIfMissing = true) {
    return api.get(`/budgets/${year}/${month}`, {
      params: { createIfMissing }
    })
  },

  getYearlySummary(year) {
    return api.get(`/budgets/${year}`)
  },

  createBudget(year, month) {
    return api.post('/budgets', null, {
      params: { year, month }
    })
  }
}

export const sectionApi = {
  create(budgetId, name, isIncome = false) {
    return api.post('/sections', { budgetId, name, isIncome })
  },

  update(id, data) {
    return api.put(`/sections/${id}`, data)
  },

  delete(id) {
    return api.delete(`/sections/${id}`)
  }
}

export const itemApi = {
  create(sectionId, name, plannedAmount = 0, actualAmount = 0) {
    return api.post('/items', { sectionId, name, plannedAmount, actualAmount })
  },

  update(id, data) {
    return api.put(`/items/${id}`, data)
  },

  delete(id) {
    return api.delete(`/items/${id}`)
  }
}

export default api
