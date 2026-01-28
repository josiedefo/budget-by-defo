import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { budgetApi, sectionApi, itemApi } from '@/services/api'

const DEFAULT_SECTIONS = [
  { id: 'default-1', name: 'Income', displayOrder: 1, isIncome: true, items: [], totalPlanned: 0, totalActual: 0 },
  { id: 'default-2', name: 'Housing', displayOrder: 2, isIncome: false, items: [], totalPlanned: 0, totalActual: 0 },
  { id: 'default-3', name: 'Transportation', displayOrder: 3, isIncome: false, items: [], totalPlanned: 0, totalActual: 0 },
  { id: 'default-4', name: 'Food', displayOrder: 4, isIncome: false, items: [], totalPlanned: 0, totalActual: 0 },
  { id: 'default-5', name: 'Utilities', displayOrder: 5, isIncome: false, items: [], totalPlanned: 0, totalActual: 0 },
  { id: 'default-6', name: 'Healthcare', displayOrder: 6, isIncome: false, items: [], totalPlanned: 0, totalActual: 0 },
  { id: 'default-7', name: 'Entertainment', displayOrder: 7, isIncome: false, items: [], totalPlanned: 0, totalActual: 0 },
  { id: 'default-8', name: 'Savings', displayOrder: 8, isIncome: false, items: [], totalPlanned: 0, totalActual: 0 }
]

function createOfflineBudget(year, month) {
  return {
    id: null,
    year,
    month,
    sections: DEFAULT_SECTIONS.map(s => ({ ...s, items: [] })),
    totalPlannedIncome: 0,
    totalIncome: 0,
    totalPlannedExpenses: 0,
    totalExpenses: 0,
    isOffline: true
  }
}

export const useBudgetStore = defineStore('budget', () => {
  const currentBudget = ref(null)
  const yearlySummary = ref(null)
  const loading = ref(false)
  const error = ref(null)
  const isOffline = computed(() => currentBudget.value?.isOffline || false)

  const sections = computed(() => currentBudget.value?.sections || [])

  const totalPlannedIncome = computed(() => currentBudget.value?.totalPlannedIncome || 0)
  const totalActualIncome = computed(() => currentBudget.value?.totalIncome || 0)
  const totalPlannedExpenses = computed(() => currentBudget.value?.totalPlannedExpenses || 0)
  const totalActualExpenses = computed(() => currentBudget.value?.totalExpenses || 0)

  const plannedBalance = computed(() => totalPlannedIncome.value - totalPlannedExpenses.value)
  const actualBalance = computed(() => totalActualIncome.value - totalActualExpenses.value)

  async function fetchBudget(year, month) {
    loading.value = true
    error.value = null
    try {
      const response = await budgetApi.getBudget(year, month)
      currentBudget.value = response.data
    } catch (e) {
      error.value = e.message
      // Fallback to offline budget with default sections
      currentBudget.value = createOfflineBudget(year, month)
    } finally {
      loading.value = false
    }
  }

  async function fetchYearlySummary(year) {
    loading.value = true
    error.value = null
    try {
      const response = await budgetApi.getYearlySummary(year)
      yearlySummary.value = response.data
    } catch (e) {
      error.value = e.message
      // Fallback to empty yearly summary
      yearlySummary.value = {
        year,
        months: [],
        totalPlannedIncome: 0,
        totalActualIncome: 0,
        totalPlannedExpenses: 0,
        totalActualExpenses: 0,
        totalPlannedSavings: 0,
        totalActualSavings: 0,
        isOffline: true
      }
    } finally {
      loading.value = false
    }
  }

  async function addSection(name, isIncome = false) {
    if (!currentBudget.value) return
    try {
      const response = await sectionApi.create(currentBudget.value.id, name, isIncome)
      currentBudget.value.sections.push(response.data)
    } catch (e) {
      error.value = e.message
    }
  }

  async function updateSection(sectionId, data) {
    try {
      const response = await sectionApi.update(sectionId, data)
      const index = currentBudget.value.sections.findIndex(s => s.id === sectionId)
      if (index !== -1) {
        currentBudget.value.sections[index] = response.data
      }
    } catch (e) {
      error.value = e.message
    }
  }

  async function deleteSection(sectionId) {
    try {
      await sectionApi.delete(sectionId)
      currentBudget.value.sections = currentBudget.value.sections.filter(s => s.id !== sectionId)
    } catch (e) {
      error.value = e.message
    }
  }

  async function addItem(sectionId, name, plannedAmount = 0) {
    try {
      const response = await itemApi.create(sectionId, name, plannedAmount, 0)
      const section = currentBudget.value.sections.find(s => s.id === sectionId)
      if (section) {
        section.items.push(response.data)
        recalculateTotals()
      }
    } catch (e) {
      error.value = e.message
    }
  }

  async function updateItem(sectionId, itemId, data) {
    try {
      const response = await itemApi.update(itemId, data)
      const section = currentBudget.value.sections.find(s => s.id === sectionId)
      if (section) {
        const index = section.items.findIndex(i => i.id === itemId)
        if (index !== -1) {
          section.items[index] = response.data
          recalculateTotals()
        }
      }
    } catch (e) {
      error.value = e.message
    }
  }

  async function deleteItem(sectionId, itemId) {
    try {
      await itemApi.delete(itemId)
      const section = currentBudget.value.sections.find(s => s.id === sectionId)
      if (section) {
        section.items = section.items.filter(i => i.id !== itemId)
        recalculateTotals()
      }
    } catch (e) {
      error.value = e.message
    }
  }

  function recalculateTotals() {
    if (!currentBudget.value) return

    let plannedIncome = 0
    let actualIncome = 0
    let plannedExpenses = 0
    let actualExpenses = 0

    for (const section of currentBudget.value.sections) {
      let sectionPlanned = 0
      let sectionActual = 0

      for (const item of section.items) {
        sectionPlanned += parseFloat(item.plannedAmount) || 0
        sectionActual += parseFloat(item.actualAmount) || 0
      }

      section.totalPlanned = sectionPlanned
      section.totalActual = sectionActual

      if (section.isIncome) {
        plannedIncome += sectionPlanned
        actualIncome += sectionActual
      } else {
        plannedExpenses += sectionPlanned
        actualExpenses += sectionActual
      }
    }

    currentBudget.value.totalPlannedIncome = plannedIncome
    currentBudget.value.totalIncome = actualIncome
    currentBudget.value.totalPlannedExpenses = plannedExpenses
    currentBudget.value.totalExpenses = actualExpenses
  }

  return {
    currentBudget,
    yearlySummary,
    loading,
    error,
    isOffline,
    sections,
    totalPlannedIncome,
    totalActualIncome,
    totalPlannedExpenses,
    totalActualExpenses,
    plannedBalance,
    actualBalance,
    fetchBudget,
    fetchYearlySummary,
    addSection,
    updateSection,
    deleteSection,
    addItem,
    updateItem,
    deleteItem
  }
})
