<template>
  <v-dialog v-model="dialog" max-width="400">
    <v-card>
      <v-card-title>Add Budget Item</v-card-title>
      <v-card-text>
        <v-text-field
          v-model="name"
          label="Item Name"
          variant="outlined"
          autofocus
          :rules="[v => !!v || 'Name is required']"
          class="mb-2"
        ></v-text-field>
        <v-text-field
          v-model.number="plannedAmount"
          label="Planned Amount"
          variant="outlined"
          type="number"
          prefix="$"
          :rules="[v => v >= 0 || 'Must be positive']"
        ></v-text-field>
      </v-card-text>
      <v-card-actions>
        <v-spacer></v-spacer>
        <v-btn variant="text" @click="close">Cancel</v-btn>
        <v-btn color="primary" variant="flat" :disabled="!name" @click="save">Add</v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<script setup>
import { ref, watch } from 'vue'

const props = defineProps({
  modelValue: { type: Boolean, default: false }
})

const emit = defineEmits(['update:modelValue', 'save'])

const dialog = ref(props.modelValue)
const name = ref('')
const plannedAmount = ref(0)

watch(() => props.modelValue, (val) => {
  dialog.value = val
  if (val) {
    name.value = ''
    plannedAmount.value = 0
  }
})

watch(dialog, (val) => {
  emit('update:modelValue', val)
})

function close() {
  dialog.value = false
}

function save() {
  emit('save', { name: name.value, plannedAmount: plannedAmount.value })
}
</script>
