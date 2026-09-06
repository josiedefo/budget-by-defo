import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createVuetify } from 'vuetify'
import * as components from 'vuetify/components'
import * as directives from 'vuetify/directives'
import BudgetSection from '@/components/BudgetSection.vue'
import EditBudgetItemDialog from '@/components/EditBudgetItemDialog.vue'

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
      // Plan-linked: its planned amount comes from a plan and must not be edited directly.
      { id: 102, name: 'Restaurants', plannedAmount: 0, actualAmount: 0, planId: 99,
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

// Editing moved out of the row (Direction B): tapping a row opens EditBudgetItemDialog,
// which emits granular events that BudgetSection translates into its own emits.
describe('BudgetSection edit-dialog wiring', () => {
  it('opens the edit dialog for the clicked item', async () => {
    const wrapper = mountSection()
    const rows = wrapper.findAll('.v-list-item')
    await rows[0].trigger('click')

    const dialog = wrapper.findComponent(EditBudgetItemDialog)
    expect(dialog.props('item').id).toBe(101)
  })

  it('propagates a dialog save as update-item with name + plannedAmount', async () => {
    const wrapper = mountSection()
    await wrapper.findAll('.v-list-item')[0].trigger('click')

    wrapper.findComponent(EditBudgetItemDialog).vm.$emit('save', { name: 'Groceries', plannedAmount: 75 })

    const emitted = wrapper.emitted('update-item') ?? []
    expect(emitted).toHaveLength(1)
    expect(emitted[0][0]).toEqual({
      sectionId: 1,
      itemId: 101,
      data: { name: 'Groceries', plannedAmount: 75 }
    })
  })

  it('omits plannedAmount when saving a plan-linked item', async () => {
    const wrapper = mountSection()
    await wrapper.findAll('.v-list-item')[1].trigger('click')  // item 102 (planId 99)

    wrapper.findComponent(EditBudgetItemDialog).vm.$emit('save', { name: 'Restaurants', plannedAmount: 999 })

    const emitted = wrapper.emitted('update-item') ?? []
    expect(emitted).toHaveLength(1)
    expect(emitted[0][0]).toEqual({
      sectionId: 1,
      itemId: 102,
      data: { name: 'Restaurants' }
    })
  })

  it('toggles the key-item flag from the dialog', async () => {
    const wrapper = mountSection()
    await wrapper.findAll('.v-list-item')[0].trigger('click')

    wrapper.findComponent(EditBudgetItemDialog).vm.$emit('toggle-key')

    const emitted = wrapper.emitted('update-item') ?? []
    expect(emitted).toHaveLength(1)
    expect(emitted[0][0]).toEqual({
      sectionId: 1,
      itemId: 101,
      data: { isKeyItem: true }
    })
  })

  it('propagates delete and exclusion events from the dialog', async () => {
    const wrapper = mountSection()
    await wrapper.findAll('.v-list-item')[0].trigger('click')
    const dialog = wrapper.findComponent(EditBudgetItemDialog)

    dialog.vm.$emit('toggle-exclusion', true)
    dialog.vm.$emit('delete')

    expect(wrapper.emitted('toggle-exclusion')[0][0]).toEqual({ sectionId: 1, itemId: 101, excluded: true })
    expect(wrapper.emitted('delete-item')[0][0]).toEqual({ sectionId: 1, itemId: 101 })
  })
})
