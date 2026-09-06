<template>
  <v-container fluid class="pa-2 pa-sm-4">
    <MonthSelector ref="selectorRef" :year="year" :month="month" @change="navigateToMonth" />

    <v-row v-if="loading" class="justify-center mt-8">
      <v-progress-circular indeterminate color="primary" size="64"></v-progress-circular>
    </v-row>

    <v-alert
      v-else-if="error"
      type="warning"
      variant="tonal"
      class="mt-4"
    >
      {{ error }}
    </v-alert>

    <template v-else-if="currentBudget">
      <BudgetDashboard :year="year" :month="month" class="mb-4" />

      <div class="d-flex justify-end mb-3 ga-2">
        <v-btn variant="tonal" size="small" @click="showCopyDialog = true">
          <v-icon start>mdi-content-copy</v-icon>
          <span class="d-none d-sm-inline">Copy from...</span>
        </v-btn>
        <v-btn color="primary" variant="tonal" size="small" @click="showAddSection = true">
          <v-icon start>mdi-plus</v-icon>
          <span class="d-none d-sm-inline">Add Section</span>
        </v-btn>
      </div>

      <!-- Income sections: quick-access cards (income isn't a "spend vs budget" ring) -->
      <div v-if="incomeSections.length" class="income-strip mb-4">
        <button
          v-for="s in incomeSections"
          :key="s.id"
          type="button"
          class="income-card"
          @click="openSectionDrawer(s.id)"
        >
          <div class="flex items-center gap-2">
            <v-avatar color="success" variant="tonal" size="32">
              <v-icon size="18">mdi-arrow-down</v-icon>
            </v-avatar>
            <div class="text-left">
              <div class="text-sm font-medium">{{ s.name }}</div>
              <div class="text-xs text-[color:var(--color-ink-soft)]">
                {{ formatCurrency(s.totalActual) }} of {{ formatCurrency(s.totalPlanned) }}
              </div>
            </div>
          </div>
          <v-icon size="20" class="text-[color:var(--color-ink-soft)]">mdi-chevron-right</v-icon>
        </button>
      </div>

      <MoneyFlow
        :sections="sections"
        @open="openSectionDrawer($event.id)"
        @add-section="showAddSection = true"
      />
    </template>

    <!-- Section detail drawer: reuses BudgetSection for the full item list + editing -->
    <v-navigation-drawer
      v-model="showDrawer"
      location="right"
      temporary
      width="480"
      class="section-drawer"
    >
      <div class="pa-2">
        <div class="d-flex justify-end">
          <v-btn icon variant="text" size="small" @click="showDrawer = false">
            <v-icon>mdi-close</v-icon>
          </v-btn>
        </div>
        <BudgetSection
          v-if="selectedSection"
          :section="selectedSection"
          :total-planned-income="totalPlannedIncome"
          :total-actual-income="totalActualIncome"
          :year="year"
          :month="month"
          :highlight-item-id="highlightItemId"
          @add-item="openAddItemDialog(selectedSection.id)"
          @update-item="handleUpdateItem"
          @delete-item="handleDeleteItem"
          @delete-section="handleDeleteSection"
          @toggle-exclusion="handleToggleExclusion"
        />
      </div>
    </v-navigation-drawer>

    <AddSectionDialog
      v-model="showAddSection"
      @save="handleAddSection"
    />

    <AddItemDialog
      v-model="showAddItem"
      @save="handleAddItem"
    />

    <CopyBudgetDialog
      v-model="showCopyDialog"
      :target-year="year"
      :target-month="month"
      :target-has-data="targetHasData"
      @save="handleCopyBudget"
    />
  </v-container>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { storeToRefs } from 'pinia'
import { useBudgetStore } from '@/stores/budget'
import MonthSelector from '@/components/MonthSelector.vue'
import BudgetDashboard from '@/components/BudgetDashboard.vue'
import MoneyFlow from '@/components/MoneyFlow.vue'
import BudgetSection from '@/components/BudgetSection.vue'
import AddSectionDialog from '@/components/AddSectionDialog.vue'
import AddItemDialog from '@/components/AddItemDialog.vue'
import CopyBudgetDialog from '@/components/CopyBudgetDialog.vue'

const props = defineProps({
  year: { type: Number, required: true },
  month: { type: Number, required: true }
})

const router = useRouter()
const route = useRoute()
const budgetStore = useBudgetStore()
const { currentBudget, sections, loading, error, totalPlannedIncome, totalActualIncome } = storeToRefs(budgetStore)

const incomeSections = computed(() => sections.value.filter(s => s.isIncome))

const selectorRef = ref(null)

const showAddSection = ref(false)
const showAddItem = ref(false)
const showCopyDialog = ref(false)
const selectedSectionId = ref(null)
const highlightItemId = ref(null)

const showDrawer = ref(false)
// Look the section up from the store each render so the drawer stays live as
// the store replaces item arrays on edit/add/delete.
const selectedSection = computed(() => sections.value.find(s => s.id === selectedSectionId.value) || null)

const targetHasData = computed(() => (currentBudget.value?.sections?.length ?? 0) > 0)

function openSectionDrawer(sectionId) {
  selectedSectionId.value = sectionId
  showDrawer.value = true
}

async function loadBudget() {
  await budgetStore.fetchBudget(props.year, props.month)

  // Handle incoming highlight — open the containing section's drawer, set the
  // highlight, then clear after 1.5s (BudgetSection scrolls to + pulses the row).
  const id = route.query.highlightItemId ? Number(route.query.highlightItemId) : null
  if (id) {
    const containing = sections.value.find(s => s.items?.some(i => i.id === id))
    if (containing) {
      selectedSectionId.value = containing.id
      showDrawer.value = true
      highlightItemId.value = id
      router.replace({ query: {} })
      setTimeout(() => { highlightItemId.value = null }, 1500)
    }
  }
}

function navigateToMonth({ year, month }) {
  router.push({ name: 'monthly', params: { year, month } })
}

function openAddItemDialog(sectionId) {
  selectedSectionId.value = sectionId
  showAddItem.value = true
}

async function handleAddSection({ name, isIncome }) {
  await budgetStore.addSection(name, isIncome)
  showAddSection.value = false
}

async function handleAddItem({ name, plannedAmount }) {
  await budgetStore.addItem(selectedSectionId.value, name, plannedAmount)
  showAddItem.value = false
}

async function handleUpdateItem({ sectionId, itemId, data }) {
  await budgetStore.updateItem(sectionId, itemId, data)
}

async function handleDeleteItem({ sectionId, itemId }) {
  await budgetStore.deleteItem(sectionId, itemId)
}

async function handleDeleteSection(sectionId) {
  await budgetStore.deleteSection(sectionId)
  showDrawer.value = false
}

async function handleToggleExclusion({ sectionId, itemId, excluded }) {
  await budgetStore.toggleItemExclusion(sectionId, itemId, excluded)
}

async function handleCopyBudget({ sourceYear, sourceMonth }) {
  const result = await budgetStore.copyBudget(props.year, props.month, sourceYear, sourceMonth)
  if (result.success) showCopyDialog.value = false
  // On failure: dialog stays open; budgetStore.error is shown inside the dialog
}

function formatCurrency(value) {
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD',
    maximumFractionDigits: 0
  }).format(value || 0)
}

onMounted(loadBudget)

watch(() => [props.year, props.month], loadBudget)
</script>

<style scoped>
.income-strip {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 12px;
}
.income-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: rgb(var(--v-theme-surface));
  border-radius: 14px;
  padding: 10px 14px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
  cursor: pointer;
  transition: transform 0.12s ease, box-shadow 0.12s ease;
}
.income-card:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}
.section-drawer :deep(.v-navigation-drawer__content) {
  background-color: rgb(var(--v-theme-background));
}
</style>
