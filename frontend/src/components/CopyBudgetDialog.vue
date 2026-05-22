<template>
  <v-dialog :model-value="modelValue" max-width="480" persistent @update:model-value="$emit('update:modelValue', $event)">
    <v-card>
      <v-card-title class="text-h6 pt-4 px-4">Copy Budget From...</v-card-title>

      <v-card-text class="px-4">
        <p class="text-body-2 text-medium-emphasis mb-4">
          Sections and planned amounts will be copied. Actual amounts are not copied.
        </p>

        <v-row dense>
          <v-col cols="8">
            <v-select
              v-model="sourceMonth"
              :items="monthOptions"
              item-title="label"
              item-value="value"
              label="Month"
              density="comfortable"
              variant="outlined"
            />
          </v-col>
          <v-col cols="4">
            <v-select
              v-model="sourceYear"
              :items="yearOptions"
              label="Year"
              density="comfortable"
              variant="outlined"
            />
          </v-col>
        </v-row>

        <p v-if="isSameMonth" class="text-body-2 text-error mt-1 mb-2">
          Source cannot be the same month as the target.
        </p>

        <v-alert
          v-if="targetHasData && !isSameMonth"
          type="warning"
          variant="tonal"
          density="compact"
          class="mt-2"
        >
          This will replace all sections and items in <strong>{{ targetMonthLabel }}</strong>.
        </v-alert>

        <v-alert
          v-if="budgetError"
          type="error"
          variant="tonal"
          density="compact"
          class="mt-2"
        >
          {{ budgetError }}
        </v-alert>
      </v-card-text>

      <v-card-actions class="px-4 pb-4">
        <v-spacer />
        <v-btn variant="text" @click="cancel">Cancel</v-btn>
        <v-btn
          color="primary"
          variant="tonal"
          :disabled="isSameMonth || loading"
          :loading="loading"
          @click="save"
        >
          Copy
        </v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useBudgetStore } from '@/stores/budget'
import { storeToRefs } from 'pinia'

const props = defineProps({
  modelValue: { type: Boolean, required: true },
  targetYear: { type: Number, required: true },
  targetMonth: { type: Number, required: true },
  targetHasData: { type: Boolean, default: false }
})

const emit = defineEmits(['update:modelValue', 'save'])

const budgetStore = useBudgetStore()
const { loading, error: budgetError } = storeToRefs(budgetStore)

// Source month/year state — default to the previous month relative to target
const sourceMonth = ref(1)
const sourceYear = ref(2026)

function initDefaults() {
  if (props.targetMonth === 1) {
    sourceMonth.value = 12
    sourceYear.value = props.targetYear - 1
  } else {
    sourceMonth.value = props.targetMonth - 1
    sourceYear.value = props.targetYear
  }
  budgetStore.error = null
}

// Re-initialize when dialog opens
watch(() => props.modelValue, (val) => {
  if (val) initDefaults()
})

const monthOptions = [
  { label: 'January',   value: 1  },
  { label: 'February',  value: 2  },
  { label: 'March',     value: 3  },
  { label: 'April',     value: 4  },
  { label: 'May',       value: 5  },
  { label: 'June',      value: 6  },
  { label: 'July',      value: 7  },
  { label: 'August',    value: 8  },
  { label: 'September', value: 9  },
  { label: 'October',   value: 10 },
  { label: 'November',  value: 11 },
  { label: 'December',  value: 12 }
]

const currentYear = new Date().getFullYear()
const yearOptions = Array.from({ length: 11 }, (_, i) => currentYear - 5 + i)

const isSameMonth = computed(() =>
  sourceYear.value === props.targetYear && sourceMonth.value === props.targetMonth
)

const targetMonthLabel = computed(() =>
  new Date(props.targetYear, props.targetMonth - 1)
    .toLocaleDateString('en-US', { month: 'long', year: 'numeric' })
)

function cancel() {
  budgetStore.error = null
  emit('update:modelValue', false)
}

function save() {
  emit('save', { sourceYear: sourceYear.value, sourceMonth: sourceMonth.value })
}
</script>
