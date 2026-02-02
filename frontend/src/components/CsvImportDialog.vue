<template>
  <v-dialog v-model="dialog" max-width="800" persistent>
    <v-card>
      <v-card-title>Import Transactions from CSV</v-card-title>
      <v-card-text>
        <!-- Step 1: File Upload -->
        <div v-if="step === 1">
          <v-file-input
            v-model="file"
            label="Select CSV file"
            accept=".csv"
            variant="outlined"
            prepend-icon="mdi-file-delimited"
            @update:model-value="parseFile"
          ></v-file-input>

          <v-checkbox
            v-model="skipFirstRow"
            label="First row contains headers"
            class="mt-2"
          ></v-checkbox>

          <v-select
            v-model="dateFormat"
            :items="dateFormatOptions"
            label="Date format in CSV"
            variant="outlined"
            class="mt-2"
          ></v-select>
        </div>

        <!-- Step 2: Column Mapping -->
        <div v-if="step === 2">
          <v-alert type="info" variant="tonal" class="mb-4">
            Map your CSV columns to transaction fields. Required: Date, Amount.
          </v-alert>

          <v-row>
            <v-col cols="6" v-for="field in mappableFields" :key="field.key">
              <v-select
                v-model="columnMapping[field.key]"
                :items="columnOptions"
                :label="field.label + (field.required ? ' *' : '')"
                variant="outlined"
                clearable
                density="compact"
              ></v-select>
            </v-col>
          </v-row>

          <!-- Preview -->
          <v-divider class="my-4"></v-divider>
          <h4 class="mb-2">Preview (first 5 rows)</h4>
          <v-table density="compact" class="preview-table">
            <thead>
              <tr>
                <th v-for="(col, idx) in previewHeaders" :key="idx" class="text-left">
                  {{ col }}
                </th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(row, rowIdx) in previewRows" :key="rowIdx">
                <td v-for="(cell, cellIdx) in row" :key="cellIdx">
                  {{ cell }}
                </td>
              </tr>
            </tbody>
          </v-table>
        </div>

        <!-- Step 3: Import Results -->
        <div v-if="step === 3">
          <v-alert v-if="importError" type="error" variant="tonal" class="mb-4">
            {{ importError }}
          </v-alert>
          <v-alert v-else type="success" variant="tonal" class="mb-4">
            Successfully imported {{ importedCount }} transactions.
          </v-alert>
        </div>

        <!-- Loading -->
        <v-progress-linear v-if="loading" indeterminate color="primary" class="mt-4"></v-progress-linear>
      </v-card-text>

      <v-card-actions>
        <v-spacer></v-spacer>
        <v-btn variant="text" @click="close">{{ step === 3 ? 'Close' : 'Cancel' }}</v-btn>
        <v-btn
          v-if="step === 1"
          color="primary"
          variant="flat"
          :disabled="!parsedRows.length"
          @click="step = 2"
        >
          Next
        </v-btn>
        <v-btn
          v-if="step === 2"
          variant="text"
          @click="step = 1"
        >
          Back
        </v-btn>
        <v-btn
          v-if="step === 2"
          color="primary"
          variant="flat"
          :disabled="!canImport || loading"
          @click="importCsv"
        >
          Import
        </v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<script setup>
import { ref, watch, computed } from 'vue'
import { transactionApi } from '@/services/api'

const props = defineProps({
  modelValue: { type: Boolean, default: false }
})

const emit = defineEmits(['update:modelValue', 'imported'])

const dialog = ref(props.modelValue)
const step = ref(1)
const file = ref(null)
const parsedRows = ref([])
const skipFirstRow = ref(true)
const dateFormat = ref('yyyy-MM-dd')
const columnMapping = ref({})
const loading = ref(false)
const importedCount = ref(0)
const importError = ref('')

const dateFormatOptions = [
  { title: 'YYYY-MM-DD (2026-01-15)', value: 'yyyy-MM-dd' },
  { title: 'MM/DD/YYYY (01/15/2026)', value: 'MM/dd/yyyy' },
  { title: 'DD/MM/YYYY (15/01/2026)', value: 'dd/MM/yyyy' },
  { title: 'M/D/YYYY (1/15/2026)', value: 'M/d/yyyy' }
]

const mappableFields = [
  { key: 'date', label: 'Date', required: true },
  { key: 'type', label: 'Type (Income/Expense)', required: false },
  { key: 'merchant', label: 'Merchant/Description', required: false },
  { key: 'amount', label: 'Amount', required: true },
  { key: 'category', label: 'Section/Category', required: false },
  { key: 'budgetItem', label: 'Budget Item', required: false },
  { key: 'note', label: 'Note', required: false }
]

const columnOptions = computed(() => {
  if (!parsedRows.value.length) return []
  const firstRow = parsedRows.value[0]
  return firstRow.map((col, idx) => ({
    title: skipFirstRow.value ? `${col} (Column ${idx + 1})` : `Column ${idx + 1}`,
    value: idx
  }))
})

const previewHeaders = computed(() => {
  if (!parsedRows.value.length) return []
  if (skipFirstRow.value) {
    return parsedRows.value[0]
  }
  return parsedRows.value[0].map((_, idx) => `Column ${idx + 1}`)
})

const previewRows = computed(() => {
  if (!parsedRows.value.length) return []
  const startIdx = skipFirstRow.value ? 1 : 0
  return parsedRows.value.slice(startIdx, startIdx + 5)
})

const canImport = computed(() => {
  return columnMapping.value.date !== undefined && columnMapping.value.amount !== undefined
})

watch(() => props.modelValue, (val) => {
  dialog.value = val
  if (val) {
    resetDialog()
  }
})

watch(dialog, (val) => {
  emit('update:modelValue', val)
})

function resetDialog() {
  step.value = 1
  file.value = null
  parsedRows.value = []
  skipFirstRow.value = true
  dateFormat.value = 'yyyy-MM-dd'
  columnMapping.value = {}
  importedCount.value = 0
  importError.value = ''
}

function parseFile(newFile) {
  if (!newFile) {
    parsedRows.value = []
    return
  }

  const reader = new FileReader()
  reader.onload = (e) => {
    const text = e.target.result
    parsedRows.value = parseCSV(text)
    autoDetectMapping()
  }
  reader.readAsText(newFile)
}

function parseCSV(text) {
  const lines = text.split(/\r?\n/).filter(line => line.trim())
  return lines.map(line => {
    const result = []
    let current = ''
    let inQuotes = false

    for (let i = 0; i < line.length; i++) {
      const char = line[i]
      if (char === '"') {
        inQuotes = !inQuotes
      } else if (char === ',' && !inQuotes) {
        result.push(current.trim())
        current = ''
      } else {
        current += char
      }
    }
    result.push(current.trim())
    return result
  })
}

function autoDetectMapping() {
  if (!parsedRows.value.length || !skipFirstRow.value) return

  const headers = parsedRows.value[0].map(h => h.toLowerCase().trim())

  headers.forEach((header, idx) => {
    if (header === 'date' || header.includes('transaction date')) {
      columnMapping.value.date = idx
    } else if (header === 'type' || header.includes('transaction type')) {
      columnMapping.value.type = idx
    } else if (header === 'merchant' || header.includes('description') || header.includes('payee')) {
      columnMapping.value.merchant = idx
    } else if (header === 'amount' || header.includes('value')) {
      columnMapping.value.amount = idx
    } else if (header === 'section' || header === 'category') {
      columnMapping.value.category = idx
    } else if (header === 'budget item' || header === 'budgetitem' || header === 'item') {
      columnMapping.value.budgetItem = idx
    } else if (header === 'note' || header.includes('memo') || header.includes('comment')) {
      columnMapping.value.note = idx
    }
  })
}

async function importCsv() {
  loading.value = true
  importError.value = ''

  try {
    const response = await transactionApi.import({
      columnMapping: columnMapping.value,
      rows: parsedRows.value,
      dateFormat: dateFormat.value,
      skipFirstRow: skipFirstRow.value
    })

    importedCount.value = response.length
    step.value = 3
    emit('imported', response)
  } catch (error) {
    importError.value = error.response?.data?.message || 'Failed to import transactions'
    step.value = 3
  } finally {
    loading.value = false
  }
}

function close() {
  dialog.value = false
}
</script>

<style scoped>
.preview-table {
  max-height: 200px;
  overflow-y: auto;
}
</style>
