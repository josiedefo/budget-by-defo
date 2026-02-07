<template>
  <v-container fluid class="pa-4">
    <MonthSelector :year="year" :month="month" @change="navigateToMonth" />

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
      <div class="sticky-summary">
        <BudgetSummary />
      </div>

      <v-row>
        <v-col cols="12">
          <div class="d-flex justify-end mb-2">
            <v-btn color="primary" variant="tonal" @click="showAddSection = true">
              <v-icon start>mdi-plus</v-icon>
              Add Section
            </v-btn>
          </div>
        </v-col>
      </v-row>

      <v-row>
        <v-col v-for="section in sections" :key="section.id" cols="12" md="6">
          <BudgetSection
            :section="section"
            :total-planned-income="totalPlannedIncome"
            :total-actual-income="totalActualIncome"
            :year="year"
            :month="month"
            @add-item="openAddItemDialog(section.id)"
            @update-item="handleUpdateItem"
            @delete-item="handleDeleteItem"
            @delete-section="handleDeleteSection"
            @toggle-exclusion="handleToggleExclusion"
          />
        </v-col>
      </v-row>
    </template>

    <AddSectionDialog
      v-model="showAddSection"
      @save="handleAddSection"
    />

    <AddItemDialog
      v-model="showAddItem"
      @save="handleAddItem"
    />
  </v-container>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { storeToRefs } from 'pinia'
import { useBudgetStore } from '@/stores/budget'
import MonthSelector from '@/components/MonthSelector.vue'
import BudgetSummary from '@/components/BudgetSummary.vue'
import BudgetSection from '@/components/BudgetSection.vue'
import AddSectionDialog from '@/components/AddSectionDialog.vue'
import AddItemDialog from '@/components/AddItemDialog.vue'

const props = defineProps({
  year: { type: Number, required: true },
  month: { type: Number, required: true }
})

const router = useRouter()
const budgetStore = useBudgetStore()
const { currentBudget, sections, loading, error, totalPlannedIncome, totalActualIncome } = storeToRefs(budgetStore)

const showAddSection = ref(false)
const showAddItem = ref(false)
const selectedSectionId = ref(null)

async function loadBudget() {
  await budgetStore.fetchBudget(props.year, props.month)
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
}

async function handleToggleExclusion({ sectionId, itemId, excluded }) {
  await budgetStore.toggleItemExclusion(sectionId, itemId, excluded)
}

onMounted(loadBudget)

watch(() => [props.year, props.month], loadBudget)
</script>

<style scoped>
.sticky-summary {
  position: sticky;
  top: 48px;
  z-index: 10;
  background-color: rgb(var(--v-theme-background));
  padding-top: 8px;
  padding-bottom: 16px;
  margin-left: -16px;
  margin-right: -16px;
  padding-left: 16px;
  padding-right: 16px;
}
</style>
