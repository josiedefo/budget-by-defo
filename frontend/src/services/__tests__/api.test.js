import { describe, it, expect, vi, beforeEach } from 'vitest'

// Mock axios so we can inspect exactly what the api layer sends.
vi.mock('axios', () => {
  const instance = {
    get: vi.fn(() => Promise.resolve({ data: {} })),
    post: vi.fn(() => Promise.resolve({ data: {} })),
    put: vi.fn(() => Promise.resolve({ data: {} })),
    delete: vi.fn(() => Promise.resolve({ data: {} }))
  }
  return { default: { create: vi.fn(() => instance) } }
})

import api, { savingsApi, transactionApi } from '@/services/api'

describe('savingsApi.getBudgetItemLinkStatuses', () => {
  beforeEach(() => {
    api.get.mockClear()
  })

  it('sends ids as a single comma-separated string, not an array', async () => {
    await savingsApi.getBudgetItemLinkStatuses([12, 34, 56], '2026-07-01', '2026-07-31')

    expect(api.get).toHaveBeenCalledWith('/savings/link-status/budget-items', {
      params: { ids: '12,34,56', startDate: '2026-07-01', endDate: '2026-07-31' }
    })
    // Regression guard: an array here would serialize as "ids[]=12&ids[]=34",
    // which only binds via a Spring 6.1+ fallback.
    const params = api.get.mock.calls[0][1].params
    expect(Array.isArray(params.ids)).toBe(false)
    expect(typeof params.ids).toBe('string')
  })

  it('handles a single id', async () => {
    await savingsApi.getBudgetItemLinkStatuses([7], '2026-07-01', '2026-07-31')

    expect(api.get.mock.calls[0][1].params.ids).toBe('7')
  })
})

describe('transactionApi', () => {
  beforeEach(() => {
    api.delete.mockClear()
  })

  it('bulkDelete sends ids in the request body', async () => {
    await transactionApi.bulkDelete([1, 2, 3])

    expect(api.delete).toHaveBeenCalledWith('/transactions/bulk', { data: { ids: [1, 2, 3] } })
  })
})
