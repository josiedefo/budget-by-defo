import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'

const pageResponse = (content = []) => ({
  data: {
    content,
    number: 0,
    size: 20,
    totalElements: content.length,
    totalPages: 1
  }
})

vi.mock('@/services/api', () => ({
  transactionApi: {
    getTransactions: vi.fn(() => Promise.resolve(pageResponse())),
    getMatchingIds: vi.fn(() => Promise.resolve({ data: [1, 2] })),
    create: vi.fn(),
    update: vi.fn(),
    delete: vi.fn(() => Promise.resolve()),
    bulkDelete: vi.fn(() => Promise.resolve())
  }
}))

import { transactionApi } from '@/services/api'
import { useTransactionStore } from '@/stores/transaction'

describe('transaction store filters', () => {
  let store

  beforeEach(() => {
    setActivePinia(createPinia())
    store = useTransactionStore()
    vi.clearAllMocks()
    transactionApi.getTransactions.mockResolvedValue(pageResponse())
  })

  it('setFilters merges into existing filters', async () => {
    store.setFilters({ merchant: 'costco' })
    store.setFilters({ type: 'EXPENSE' })

    expect(store.filters.merchant).toBe('costco')
    expect(store.filters.type).toBe('EXPENSE')
  })

  it('replaceFilters resets everything before applying — no stale bleed-through', async () => {
    store.setFilters({ merchant: 'costco', startDate: '2026-01-01', uncategorized: true })

    store.replaceFilters({ transactionId: 42 })

    expect(store.filters.transactionId).toBe(42)
    expect(store.filters.merchant).toBe('')
    expect(store.filters.startDate).toBeNull()
    expect(store.filters.uncategorized).toBe(false)
  })

  it('fetchTransactions drops null/empty params but always sends uncategorized', async () => {
    store.setFilters({ merchant: 'target' })
    await vi.waitFor(() => expect(transactionApi.getTransactions).toHaveBeenCalled())

    const params = transactionApi.getTransactions.mock.calls.at(-1)[0]
    expect(params.merchant).toBe('target')
    expect(params.uncategorized).toBe(false)
    expect(params).not.toHaveProperty('startDate')
    expect(params).not.toHaveProperty('transactionId')
  })

  it('setFilters supports searching by exact amount', async () => {
    store.setFilters({ amount: 45.5 })
    await vi.waitFor(() => expect(transactionApi.getTransactions).toHaveBeenCalled())

    const params = transactionApi.getTransactions.mock.calls.at(-1)[0]
    expect(params.amount).toBe(45.5)
  })

  it('clearFilters resets amount back to null', async () => {
    store.setFilters({ amount: 45.5 })
    store.clearFilters()

    expect(store.filters.amount).toBeNull()
  })

  it('getMatchingIds sends only active filters plus the uncategorized flag', async () => {
    store.filters.merchant = 'costco'
    store.filters.type = 'EXPENSE'

    const ids = await store.getMatchingIds()

    expect(ids).toEqual([1, 2])
    const params = transactionApi.getMatchingIds.mock.calls[0][0]
    expect(params).toEqual({ merchant: 'costco', type: 'EXPENSE', uncategorized: false })
  })

  it('getMatchingIds includes amount when searching by amount', async () => {
    store.filters.amount = 45.5

    await store.getMatchingIds()

    const params = transactionApi.getMatchingIds.mock.calls[0][0]
    expect(params).toEqual({ amount: 45.5, uncategorized: false })
  })

  it('updateLinkedSavings mutates the transaction in place so open dialogs stay live', () => {
    const tx = { id: 5, merchant: 'Vanguard', linkedSavingsAccountEventId: null }
    store.transactions.push(tx)

    store.updateLinkedSavings(5, {
      linkedSavingsAccountEventId: 99,
      linkedSavingsAccountName: 'Ally'
    })

    // Same object reference, updated fields
    expect(tx.linkedSavingsAccountEventId).toBe(99)
    expect(tx.linkedSavingsAccountName).toBe('Ally')
  })

  it('deleteTransaction removes the row and decrements the total', async () => {
    store.transactions.push({ id: 5 }, { id: 6 })
    store.pagination.totalElements = 2

    await store.deleteTransaction(5)

    expect(store.transactions.map(t => t.id)).toEqual([6])
    expect(store.pagination.totalElements).toBe(1)
  })
})
