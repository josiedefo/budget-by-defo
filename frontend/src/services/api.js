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

export const transactionApi = {
  getTransactions(params = {}) {
    return api.get('/transactions', { params })
  },

  getTransaction(id) {
    return api.get(`/transactions/${id}`)
  },

  create(data) {
    return api.post('/transactions', data)
  },

  update(id, data) {
    return api.put(`/transactions/${id}`, data)
  },

  delete(id) {
    return api.delete(`/transactions/${id}`)
  },

  import(data) {
    return api.post('/transactions/import', data).then(res => res.data)
  }
}

export const planApi = {
  getPlans(year, month) {
    return api.get('/plans', { params: { year, month } })
  },

  getPlan(id) {
    return api.get(`/plans/${id}`)
  },

  getPlanByBudgetItem(budgetItemId, year, month) {
    return api.get('/plans/by-item', { params: { budgetItemId, year, month } })
  },

  create(data) {
    return api.post('/plans', data)
  },

  update(id, data) {
    return api.put(`/plans/${id}`, data)
  },

  delete(id) {
    return api.delete(`/plans/${id}`)
  }
}

export const subscriptionApi = {
  getAll() {
    return api.get('/subscriptions')
  },

  get(id) {
    return api.get(`/subscriptions/${id}`)
  },

  create(data) {
    return api.post('/subscriptions', data)
  },

  update(id, data) {
    return api.put(`/subscriptions/${id}`, data)
  },

  delete(id) {
    return api.delete(`/subscriptions/${id}`)
  }
}

export const salaryApi = {
  getAll() {
    return api.get('/salaries')
  },

  get(id) {
    return api.get(`/salaries/${id}`)
  },

  create(data) {
    return api.post('/salaries', data)
  },

  update(id, data) {
    return api.put(`/salaries/${id}`, data)
  },

  delete(id) {
    return api.delete(`/salaries/${id}`)
  }
}

export const savingsApi = {
  // Accounts
  getAccounts() { return api.get('/savings/accounts') },
  getAccount(id) { return api.get(`/savings/accounts/${id}`) },
  createAccount(data) { return api.post('/savings/accounts', data) },
  updateAccount(id, data) { return api.put(`/savings/accounts/${id}`, data) },
  deleteAccount(id) { return api.delete(`/savings/accounts/${id}`) },
  getPoolBalance() { return api.get('/savings/accounts/pool-balance') },
  getAccountEvents(accountId) { return api.get(`/savings/accounts/${accountId}/events`) },
  depositToAccount(accountId, data) { return api.post(`/savings/accounts/${accountId}/deposit`, data) },
  withdrawFromAccount(accountId, data) { return api.post(`/savings/accounts/${accountId}/withdrawal`, data) },
  updateAccountEvent(eventId, data) { return api.put(`/savings/accounts/events/${eventId}`, data) },
  deleteAccountEvent(eventId) { return api.delete(`/savings/accounts/events/${eventId}`) },

  // Funds
  getFunds() { return api.get('/savings/funds') },
  getFund(id) { return api.get(`/savings/funds/${id}`) },
  createFund(data) { return api.post('/savings/funds', data) },
  updateFund(id, data) { return api.put(`/savings/funds/${id}`, data) },
  deleteFund(id) { return api.delete(`/savings/funds/${id}`) },
  getSummary() { return api.get('/savings/funds/summary') },

  // Events
  getEventsForFund(fundId) { return api.get(`/savings/events/fund/${fundId}`) },
  logDeposit(data) { return api.post('/savings/events/deposit', data) },
  logWithdrawal(data) { return api.post('/savings/events/withdrawal', data) },
  reallocate(data) { return api.post('/savings/events/reallocate', data) },
  processPayout(fundId) { return api.post(`/savings/events/payout/${fundId}`) },
  updateEvent(id, data) { return api.put(`/savings/events/${id}`, data) },
  deleteEvent(id) { return api.delete(`/savings/events/${id}`) }
}

export default api
