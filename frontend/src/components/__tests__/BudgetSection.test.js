import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createVuetify } from 'vuetify'
import * as components from 'vuetify/components'
import * as directives from 'vuetify/directives'
import BudgetSection from '@/components/BudgetSection.vue'

vi.mock('@/services/api', () => ({
  savingsApi: {
    getBudgetItemLinkStatuses: vi.fn(() => Promise.resolve({ data: {} }))
  }
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: vi.fn() })
}))

const vuetify = createVuetify({ components, directives })

function makeSection() {
  return {
    id: 1,
    name: 'Food',
    isIncome: false,
    totalPlanned: 0,
    totalActual: 0,
    items: [
      { id: 101, name: 'Groceries', plannedAmount: 0, actualAmount: 0, planId: null,
        isExcludedFromBudget: false, isKeyItem: false },
      { id: 102, name: 'Restaurants', plannedAmount: 0, actualAmount: 0, planId: null,
        isExcludedFromBudget: false, isKeyItem: false }
    ]
  }
}

function mountSection() {
  return mount(BudgetSection, {
    props: {
      section: makeSection(),
      totalPlannedIncome: 0,
      totalActualIncome: 0,
      year: 2026,
      month: 7
    },
    global: {
      plugins: [vuetify],
      stubs: { BulkSavingsLinkDialog: true }
    }
  })
}

describe('BudgetSection planned-amount editing', () => {
  beforeEach(() => {
    vi.useFakeTimers()
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('debounces rapid edits to the SAME item into one update with the last value', async () => {
    const wrapper = mountSection()
    const input = wrapper.findAll('input[type="number"]')[0]

    await input.setValue('50')
    vi.advanceTimersByTime(200)
    await input.setValue('75')
    vi.advanceTimersByTime(500)

    const emitted = wrapper.emitted('update-item') ?? []
    expect(emitted).toHaveLength(1)
    expect(emitted[0][0]).toEqual({
      sectionId: 1,
      itemId: 101,
      data: { plannedAmount: 75 }
    })
  })

  it('does NOT drop item A\'s edit when item B is edited within the debounce window', async () => {
    // Regression test: a single shared timer used to cancel the first item's
    // pending save, silently losing the edit.
    const wrapper = mountSection()
    const inputs = wrapper.findAll('input[type="number"]')

    await inputs[0].setValue('100')   // item 101
    vi.advanceTimersByTime(200)       // still inside item 101's 500ms window
    await inputs[1].setValue('200')   // item 102
    vi.advanceTimersByTime(500)       // both windows elapse

    const emitted = wrapper.emitted('update-item') ?? []
    expect(emitted).toHaveLength(2)

    const byItem = Object.fromEntries(emitted.map(([payload]) => [payload.itemId, payload]))
    expect(byItem[101].data).toEqual({ plannedAmount: 100 })
    expect(byItem[102].data).toEqual({ plannedAmount: 200 })
  })

  it('edits to different fields of the same item are debounced independently', async () => {
    const wrapper = mountSection()
    const input = wrapper.findAll('input[type="number"]')[0]

    await input.setValue('100')
    vi.advanceTimersByTime(200)

    // Simulate a second field on the same item via the exclusion toggle path — the
    // key-item toggle emits immediately and must not be affected by pending timers.
    const keyItemButton = wrapper.findAll('button').find(b =>
      b.attributes('title')?.includes('Tag as key item'))
    await keyItemButton.trigger('click')
    vi.advanceTimersByTime(500)

    const updates = wrapper.emitted('update-item') ?? []
    expect(updates).toHaveLength(2)
    expect(updates.some(([p]) => p.data.isKeyItem === true)).toBe(true)
    expect(updates.some(([p]) => p.data.plannedAmount === 100)).toBe(true)
  })
})
