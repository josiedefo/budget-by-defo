<template>
  <v-dialog v-model="dialog" max-width="700" persistent>
    <v-card v-if="plan">
      <v-card-title class="d-flex align-center">
        <v-icon class="mr-2" color="primary">mdi-calendar-text</v-icon>
        {{ plan.budgetItemName }}
      </v-card-title>
      <v-card-subtitle>{{ plan.sectionName }} - {{ monthName }} {{ plan.year }}</v-card-subtitle>

      <v-card-text>
        <v-table density="compact">
          <thead>
            <tr>
              <th>Description</th>
              <th class="text-right" style="width: 150px">Amount</th>
              <th style="width: 50px"></th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(item, index) in items" :key="index">
              <td>
                <div class="d-flex align-center">
                  <v-btn
                    v-if="item.fromSubscription"
                    icon
                    size="x-small"
                    variant="text"
                    color="secondary"
                    class="mr-1"
                    title="Edit recurring payment"
                    @click="openSubscriptionEdit(item)"
                  >
                    <v-icon size="small">mdi-repeat</v-icon>
                  </v-btn>
                  <v-btn
                    v-if="item.fromSalary"
                    icon
                    size="x-small"
                    variant="text"
                    color="success"
                    class="mr-1"
                    title="Edit salary"
                    @click="openSalaryEdit(item)"
                  >
                    <v-icon size="small">mdi-currency-usd</v-icon>
                  </v-btn>
                  <v-text-field
                    v-model="item.name"
                    density="compact"
                    hide-details
                    variant="plain"
                    placeholder="Description"
                  ></v-text-field>
                </div>
              </td>
              <td>
                <v-text-field
                  v-model.number="item.amount"
                  type="number"
                  density="compact"
                  hide-details
                  variant="plain"
                  class="text-right"
                  prefix="$"
                ></v-text-field>
              </td>
              <td>
                <v-btn icon size="x-small" variant="text" color="error" @click="removeItem(index)">
                  <v-icon size="small">mdi-delete</v-icon>
                </v-btn>
              </td>
            </tr>
            <tr v-if="items.length === 0">
              <td colspan="3" class="text-center text-medium-emphasis py-4">
                No items yet. Click "Add Item" to add one.
              </td>
            </tr>
          </tbody>
          <tfoot>
            <tr class="total-row">
              <td class="font-weight-bold">Total</td>
              <td class="text-right font-weight-bold">{{ formatCurrency(total) }}</td>
              <td></td>
            </tr>
          </tfoot>
        </v-table>

        <div class="d-flex gap-2 mt-4 flex-wrap">
          <v-btn variant="tonal" @click="addItem">
            <v-icon start>mdi-plus</v-icon>
            Add Item
          </v-btn>
          <v-btn variant="tonal" color="secondary" @click="showSubscriptionPicker = true">
            <v-icon start>mdi-repeat</v-icon>
            Add from Recurring Payments
          </v-btn>
          <v-btn variant="tonal" color="success" @click="showSalaryPicker = true">
            <v-icon start>mdi-currency-usd</v-icon>
            Add from Salaries
          </v-btn>
        </div>
      </v-card-text>

      <v-card-actions>
        <v-btn color="error" variant="text" @click="confirmDelete">
          <v-icon start>mdi-delete</v-icon>
          Delete Plan
        </v-btn>
        <v-spacer></v-spacer>
        <v-btn variant="text" @click="close">Cancel</v-btn>
        <v-btn color="primary" variant="flat" :loading="saving" @click="save">
          Save
        </v-btn>
      </v-card-actions>
    </v-card>

    <AddSubscriptionsToPlanDialog
      v-model="showSubscriptionPicker"
      @add="addFromSubscriptions"
      @manage-subscriptions="openManageSubscriptions"
    />

    <SubscriptionsDialog
      v-model="showSubscriptionsDialog"
      :edit-id="editingSubscriptionId"
      @updated="handleSourceUpdated"
    />

    <AddSalaryToPlanDialog
      v-model="showSalaryPicker"
      @add="addFromSalaries"
      @manage-salaries="openManageSalaries"
    />

    <SalariesDialog
      v-model="showSalariesDialog"
      :edit-id="editingSalaryId"
      @updated="handleSourceUpdated"
    />
  </v-dialog>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { usePlanStore } from '@/stores/plan'
import { useSubscriptionStore } from '@/stores/subscription'
import { useSalaryStore } from '@/stores/salary'
import AddSubscriptionsToPlanDialog from '@/components/AddSubscriptionsToPlanDialog.vue'
import SubscriptionsDialog from '@/components/SubscriptionsDialog.vue'
import AddSalaryToPlanDialog from '@/components/AddSalaryToPlanDialog.vue'
import SalariesDialog from '@/components/SalariesDialog.vue'

const props = defineProps({
  modelValue: Boolean,
  plan: Object
})

const emit = defineEmits(['update:modelValue', 'saved', 'deleted'])

const planStore = usePlanStore()
const subscriptionStore = useSubscriptionStore()
const salaryStore = useSalaryStore()

const dialog = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v)
})

const items = ref([])
const saving = ref(false)
const showSubscriptionPicker = ref(false)
const showSubscriptionsDialog = ref(false)
const showSalaryPicker = ref(false)
const showSalariesDialog = ref(false)
// Track which subscription/salary to open in edit mode when the manage dialog opens
const editingSubscriptionId = ref(null)
const editingSalaryId = ref(null)

const monthName = computed(() => {
  if (!props.plan) return ''
  return new Date(props.plan.year, props.plan.month - 1).toLocaleString('default', { month: 'long' })
})

const total = computed(() => {
  return items.value.reduce((sum, item) => sum + (parseFloat(item.amount) || 0), 0)
})

function formatCurrency(value) {
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD'
  }).format(value || 0)
}

function addItem() {
  items.value.push({ name: '', amount: 0 })
}

function removeItem(index) {
  items.value.splice(index, 1)
}

function addFromSubscriptions(subscriptionItems) {
  for (const sub of subscriptionItems) {
    items.value.push({
      name: sub.name,
      amount: sub.amount,
      fromSubscription: true,
      subscriptionId: sub.subscriptionId || null
    })
  }
}

function openManageSubscriptions() {
  editingSubscriptionId.value = null
  showSubscriptionsDialog.value = true
}

async function openSubscriptionEdit(item) {
  // Use the stored FK id if available; otherwise fall back to name-matching in the store
  if (item.subscriptionId) {
    editingSubscriptionId.value = item.subscriptionId
  } else {
    await subscriptionStore.fetchSubscriptions()
    const match = subscriptionStore.subscriptions.find(s => s.name === item.name)
    editingSubscriptionId.value = match ? match.id : null
  }
  showSubscriptionsDialog.value = true
}

function addFromSalaries(salaryItems) {
  for (const sal of salaryItems) {
    items.value.push({
      name: sal.name,
      amount: sal.amount,
      fromSalary: true,
      salaryId: sal.salaryId || null
    })
  }
}

function openManageSalaries() {
  editingSalaryId.value = null
  showSalariesDialog.value = true
}

async function openSalaryEdit(item) {
  // Use the stored FK id if available; otherwise fall back to name-matching in the store
  if (item.salaryId) {
    editingSalaryId.value = item.salaryId
  } else {
    await salaryStore.fetchSalaries()
    const match = salaryStore.salaries.find(s => s.name === item.name)
    editingSalaryId.value = match ? match.id : null
  }
  showSalariesDialog.value = true
}

/**
 * Called when a subscription or salary is updated from within PlanDialog.
 * The backend has already cascaded the change to plan items and budget item.
 * Reload the plan from the server so the dialog reflects the updated names/amounts.
 */
async function handleSourceUpdated() {
  if (!props.plan) return
  const refreshed = await planStore.fetchPlan(props.plan.id)
  if (refreshed) {
    items.value = refreshed.items.map(i => ({
      name: i.name,
      amount: i.amount,
      fromSubscription: i.fromSubscription || false,
      fromSalary: i.fromSalary || false,
      subscriptionId: i.subscriptionId || null,
      salaryId: i.salaryId || null
    }))
  }
}

async function save() {
  saving.value = true
  try {
    await planStore.updatePlan(props.plan.id, items.value.map(i => ({
      name: i.name,
      amount: i.amount,
      fromSubscription: i.fromSubscription || false,
      fromSalary: i.fromSalary || false,
      subscriptionId: i.subscriptionId || null,
      salaryId: i.salaryId || null
    })))
    emit('saved')
    close()
  } catch (e) {
    console.error('Failed to save plan:', e)
  } finally {
    saving.value = false
  }
}

function confirmDelete() {
  if (confirm(`Delete this plan? The planned amount will be reset to $0.`)) {
    planStore.deletePlan(props.plan.id).then(() => {
      emit('deleted')
    })
  }
}

function close() {
  dialog.value = false
}

watch(() => props.plan, (plan) => {
  if (plan) {
    items.value = plan.items.map(i => ({
      name: i.name,
      amount: i.amount,
      fromSubscription: i.fromSubscription || false,
      fromSalary: i.fromSalary || false,
      subscriptionId: i.subscriptionId || null,
      salaryId: i.salaryId || null
    }))
  } else {
    items.value = []
  }
}, { immediate: true })
</script>

<style scoped>
.total-row {
  background-color: color-mix(in srgb, rgb(var(--v-theme-surface-variant)) 40%, transparent);
  border-top: 2px solid rgba(var(--v-border-color), 0.4);
}

.text-right :deep(input) {
  text-align: right;
}
</style>
