<template>
  <v-card :class="section.isIncome ? 'border-success' : ''">
    <v-card-title class="d-flex align-center">
      <v-avatar
        :color="section.isIncome ? 'success' : 'error'"
        variant="tonal"
        size="36"
        class="mr-3"
      >
        <v-icon>{{ section.isIncome ? 'mdi-arrow-down' : 'mdi-arrow-up' }}</v-icon>
      </v-avatar>
      <span class="text-truncate">{{ section.name }}</span>
      <v-chip size="small" class="ml-2" :color="section.isIncome ? 'success' : 'error'" variant="tonal">
        {{ formatCurrency(section.totalActual) }} / {{ formatCurrency(section.totalPlanned) }}
      </v-chip>
      <v-spacer></v-spacer>
      <v-btn icon size="small" variant="text" title="Add item" @click="emit('add-item')">
        <v-icon>mdi-plus</v-icon>
      </v-btn>
      <v-btn icon size="small" variant="text" color="error" title="Delete section" @click="confirmDelete">
        <v-icon>mdi-delete</v-icon>
      </v-btn>
    </v-card-title>

    <v-divider></v-divider>

    <v-list density="comfortable" class="py-0">
      <template v-for="item in section.items" :key="item.id">
        <v-list-item
          :ref="el => setItemRef(el, item.id)"
          :class="{
            'excluded-item': item.isExcludedFromBudget,
            'highlight-pulse': item.id === highlightItemId
          }"
          @click="openEdit(item)"
        >
          <template #prepend>
            <v-icon
              v-if="item.isKeyItem"
              color="amber"
              size="small"
              class="mr-2"
              title="Key item"
            >mdi-bookmark</v-icon>
          </template>

          <v-list-item-title class="d-flex align-center">
            {{ item.name }}
            <v-icon v-if="item.planId" size="x-small" class="ml-1 text-medium-emphasis">mdi-link</v-icon>
          </v-list-item-title>
          <v-list-item-subtitle>
            {{ formatCurrency(item.actualAmount) }}
            <span class="text-disabled"> of {{ formatCurrency(item.plannedAmount) }}</span>
          </v-list-item-subtitle>

          <v-progress-linear
            :model-value="meterValue(item)"
            :color="meterColor(item)"
            height="4"
            rounded
            class="mt-1"
            bg-opacity="0.15"
          ></v-progress-linear>

          <template #append>
            <div class="d-flex align-center ga-1">
              <v-chip
                size="small"
                variant="tonal"
                :color="getItemDiff(item) >= 0 ? 'success' : 'error'"
              >
                {{ formatCurrency(getItemDiff(item)) }}
              </v-chip>
              <v-icon
                v-if="item.actualAmount > 0"
                size="small"
                :color="isFullyLinked(item) ? 'teal' : 'grey-lighten-1'"
                :title="isFullyLinked(item) ? 'All transactions linked to savings' : 'Not fully linked to savings'"
              >mdi-bank-transfer</v-icon>
              <v-icon size="small" class="text-medium-emphasis">mdi-chevron-right</v-icon>
            </div>
          </template>
        </v-list-item>
        <v-divider></v-divider>
      </template>

      <v-list-item v-if="section.items.length === 0">
        <v-list-item-title class="text-center text-medium-emphasis py-2">
          No items yet. Click + to add one.
        </v-list-item-title>
      </v-list-item>

      <v-list-item class="total-row">
        <v-list-item-title class="font-weight-bold">Total</v-list-item-title>
        <template #append>
          <div class="text-right">
            <div class="font-weight-bold">
              {{ formatCurrency(section.totalActual) }}
              <span class="text-medium-emphasis percent-badge"> / {{ formatCurrency(section.totalPlanned) }}</span>
            </div>
            <div class="text-caption">
              <span class="font-weight-bold" :class="getTotalDiffClass">{{ formatCurrency(totalDifference) }}</span>
              <span v-if="actualPercentOfIncome" class="text-medium-emphasis percent-badge">
                · {{ actualPercentOfIncome }}% of income
              </span>
            </div>
          </div>
        </template>
      </v-list-item>
    </v-list>
  </v-card>

  <EditBudgetItemDialog
    v-model="showEditDialog"
    :item="editItem"
    :fully-linked="editItem ? isFullyLinked(editItem) : false"
    @save="handleEditSave"
    @delete="handleEditDelete"
    @toggle-exclusion="handleEditToggleExclusion"
    @toggle-key="handleEditToggleKey"
    @view-transactions="editItem && viewTransactions(editItem)"
    @view-plan="editItem && viewPlan(editItem)"
    @link-savings="handleEditLinkSavings"
  />

  <BulkSavingsLinkDialog
    v-model="showBulkSavingsLink"
    :budget-item="bulkLinkItem"
    :year="year"
    :month="month"
    @status-changed="handleStatusChanged"
  />
</template>

<script setup>
import { computed, watch, nextTick, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import BulkSavingsLinkDialog from '@/components/BulkSavingsLinkDialog.vue'
import EditBudgetItemDialog from '@/components/EditBudgetItemDialog.vue'
import { savingsApi } from '@/services/api'

const router = useRouter()

const props = defineProps({
  section: { type: Object, required: true },
  totalPlannedIncome: { type: Number, default: 0 },
  totalActualIncome: { type: Number, default: 0 },
  year: { type: Number, required: true },
  month: { type: Number, required: true },
  highlightItemId: { type: Number, default: null }
})

const showBulkSavingsLink = ref(false)
const bulkLinkItem = ref(null)
// Map of budgetItemId → BudgetItemLinkStatus (fetched from API)
const linkStatusMap = ref({})

const showEditDialog = ref(false)
const editItem = ref(null)

function openEdit(item) {
  editItem.value = item
  showEditDialog.value = true
}

function openBulkSavingsLink(item) {
  bulkLinkItem.value = item
  showBulkSavingsLink.value = true
}

function handleStatusChanged(itemId, status) {
  linkStatusMap.value = { ...linkStatusMap.value, [itemId]: status }
}

function isFullyLinked(item) {
  const status = linkStatusMap.value[item.id]
  return status?.allLinkedToAccount || status?.allLinkedToFund
}

// --- Edit dialog handlers: translate dialog events into the section's existing emits ---
function handleEditSave({ name, plannedAmount }) {
  const item = editItem.value
  if (!item) return
  const data = { name }
  // Plan-linked items don't accept a directly-edited planned amount.
  if (!item.planId) data.plannedAmount = plannedAmount
  emit('update-item', { sectionId: props.section.id, itemId: item.id, data })
}

function handleEditDelete() {
  if (!editItem.value) return
  deleteItem(editItem.value.id)
  showEditDialog.value = false
}

function handleEditToggleExclusion(excluded) {
  if (!editItem.value) return
  toggleExclusion(editItem.value.id, excluded)
}

function handleEditToggleKey() {
  if (editItem.value) toggleKeyItem(editItem.value)
}

function handleEditLinkSavings() {
  if (!editItem.value) return
  openBulkSavingsLink(editItem.value)
  showEditDialog.value = false
}

async function fetchLinkStatuses() {
  const itemsWithActual = props.section.items.filter(i => (i.actualAmount || 0) > 0)
  if (itemsWithActual.length === 0) return
  const y = props.year
  const m = props.month
  const startDate = `${y}-${String(m).padStart(2, '0')}-01`
  const lastDay = new Date(y, m, 0).getDate()
  const endDate = `${y}-${String(m).padStart(2, '0')}-${String(lastDay).padStart(2, '0')}`
  try {
    const ids = itemsWithActual.map(i => i.id)
    const response = await savingsApi.getBudgetItemLinkStatuses(ids, startDate, endDate)
    linkStatusMap.value = { ...linkStatusMap.value, ...response.data }
  } catch { /* silently ignore — icon just stays grey */ }
}

onMounted(() => {
  fetchLinkStatuses()
})

// Map item id → element ref so we can scroll to it
const itemRefs = ref({})
function setItemRef(el, itemId) {
  if (el) itemRefs.value[itemId] = el
}

watch(() => props.highlightItemId, async (id) => {
  if (!id) return
  const itemInSection = props.section.items.some(i => i.id === id)
  if (!itemInSection) return
  await nextTick()
  const el = itemRefs.value[id]
  const node = el?.$el || el
  if (node?.scrollIntoView) node.scrollIntoView({ behavior: 'smooth', block: 'center' })
})

const emit = defineEmits(['add-item', 'update-item', 'delete-item', 'delete-section', 'toggle-exclusion'])

const totalDifference = computed(() => {
  const planned = props.section.totalPlanned || 0
  const actual = props.section.totalActual || 0
  // For income: actual - planned (positive = earned more than expected)
  // For expenses: planned - actual (positive = spent less than expected)
  return props.section.isIncome ? actual - planned : planned - actual
})

const getTotalDiffClass = computed(() => {
  // Positive diff is always good (earned more or spent less), negative is bad
  return totalDifference.value >= 0 ? 'text-success' : 'text-error'
})

const actualPercentOfIncome = computed(() => {
  if (props.section.isIncome || !props.totalActualIncome) return null
  return ((props.section.totalActual || 0) / props.totalActualIncome * 100).toFixed(1)
})

function formatCurrency(value) {
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD'
  }).format(value || 0)
}

// Progress meter: how much of planned has been used/earned (0–100).
function meterValue(item) {
  const planned = parseFloat(item.plannedAmount) || 0
  const actual = parseFloat(item.actualAmount) || 0
  if (planned <= 0) return actual > 0 ? 100 : 0
  return Math.min((actual / planned) * 100, 100)
}

// Green when on track, warning/error when off. For expenses, over-spend is bad;
// for income, under-earning is the warning state.
function meterColor(item) {
  const planned = parseFloat(item.plannedAmount) || 0
  const actual = parseFloat(item.actualAmount) || 0
  if (props.section.isIncome) {
    return actual >= planned ? 'success' : 'warning'
  }
  if (planned > 0 && actual > planned) return 'error'
  return 'success'
}

function getItemDiff(item) {
  const diff = parseFloat(item.difference) || 0
  // For income: invert the diff (backend gives planned - actual, we want actual - planned)
  return props.section.isIncome ? -diff : diff
}

function deleteItem(itemId) {
  emit('delete-item', { sectionId: props.section.id, itemId })
}

function toggleExclusion(itemId, excluded) {
  emit('toggle-exclusion', { sectionId: props.section.id, itemId, excluded })
}

function toggleKeyItem(item) {
  emit('update-item', {
    sectionId: props.section.id,
    itemId: item.id,
    data: { isKeyItem: !item.isKeyItem }
  })
}

function confirmDelete() {
  if (confirm(`Delete section "${props.section.name}" and all its items?`)) {
    emit('delete-section', props.section.id)
  }
}

function viewTransactions(item) {
  // Calculate start and end dates for the month
  const startDate = `${props.year}-${String(props.month).padStart(2, '0')}-01`
  const lastDay = new Date(props.year, props.month, 0).getDate()
  const endDate = `${props.year}-${String(props.month).padStart(2, '0')}-${String(lastDay).padStart(2, '0')}`

  router.push({
    path: '/transactions',
    query: {
      sectionName: props.section.name,
      budgetItemName: item.name,
      startDate,
      endDate
    }
  })
}

function viewPlan(item) {
  router.push({
    path: '/planner',
    query: {
      year: props.year,
      month: props.month,
      planId: item.planId
    }
  })
}
</script>

<style scoped>
.total-row {
  background-color: color-mix(in srgb, rgb(var(--v-theme-surface-variant)) 40%, transparent);
  border-top: 2px solid rgba(var(--v-border-color), 0.4);
}

.percent-badge {
  font-size: 0.85em;
  font-weight: normal;
}

.excluded-item :deep(.v-list-item-title),
.excluded-item :deep(.v-list-item-subtitle) {
  opacity: 0.5;
  text-decoration: line-through;
}

@keyframes highlight-pulse {
  0%   { background-color: transparent; }
  20%  { background-color: color-mix(in srgb, rgb(var(--v-theme-primary)) 22%, transparent); }
  60%  { background-color: color-mix(in srgb, rgb(var(--v-theme-primary)) 22%, transparent); }
  100% { background-color: transparent; }
}

.highlight-pulse {
  animation: highlight-pulse 1.5s ease-in-out forwards;
}
</style>
