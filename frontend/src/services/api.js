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
  },

  copyBudget(targetYear, targetMonth, sourceYear, sourceMonth) {
    return api.post(`/budgets/${targetYear}/${targetMonth}/copy`, { sourceYear, sourceMonth })
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

  getMatchingIds(params = {}) {
    return api.get('/transactions/matching-ids', { params })
  },

  bulkDelete(ids) {
    return api.delete('/transactions/bulk', { data: { ids } })
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
  linkTransaction(accountId, data) { return api.post(`/savings/accounts/${accountId}/link-transaction`, data) },
  bulkLinkBudgetItem(accountId, data) { return api.post(`/savings/accounts/${accountId}/bulk-link-budget-item`, data) },
  bulkLinkTransactions(accountId, data) { return api.post(`/savings/accounts/${accountId}/bulk-link-transactions`, data) },

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
  deleteEvent(id) { return api.delete(`/savings/events/${id}`) },
  linkTransactionToFund(data) { return api.post('/savings/events/link-transaction', data) },
  bulkLinkBudgetItemToFund(data) { return api.post('/savings/events/bulk-link-budget-item', data) },
  bulkLinkTransactionsToFund(data) { return api.post('/savings/events/bulk-link-transactions', data) },
  getBudgetItemLinkStatuses(ids, startDate, endDate) {
    // Send ids as an explicit comma-separated string instead of relying on axios's
    // "ids[]=1&ids[]=2" array form, which only binds thanks to a Spring 6.1+ fallback.
    return api.get('/savings/link-status/budget-items', {
      params: { ids: ids.join(','), startDate, endDate }
    })
  }
}

export default api
