<template>
  <v-dialog v-model="dialog" max-width="400">
    <v-card>
      <v-card-title>Add Section</v-card-title>
      <v-card-text>
        <v-text-field
          v-model="name"
          label="Section Name"
          variant="outlined"
          autofocus
          :rules="[v => !!v || 'Name is required']"
        ></v-text-field>
        <v-checkbox
          v-model="isIncome"
          label="This is an income section"
          color="success"
          hide-details
        ></v-checkbox>
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
const isIncome = ref(false)

watch(() => props.modelValue, (val) => {
  dialog.value = val
  if (val) {
    name.value = ''
    isIncome.value = false
  }
})

watch(dialog, (val) => {
  emit('update:modelValue', val)
})

function close() {
  dialog.value = false
}

function save() {
  emit('save', { name: name.value, isIncome: isIncome.value })
}
</script>
