<template>
  <q-card flat bordered style="height: 400px;">
    <q-scroll-area :thumb-style="thumbStyle" :bar-style="barStyle" style="height: 400px;">
      <q-list dense>
        <q-item class="force-wrap" v-for="error in errors" :key="error.id" clickable @click="copyToClipboard(error.error)">
          <q-item-section avatar>
            <q-chip :label="error.id" color="accent" text-color="white"/>
          </q-item-section>
          <q-item-section>
            {{ error.error }}
          </q-item-section>
        </q-item>
      </q-list>
    </q-scroll-area>
  </q-card>
</template>

<script lang="ts">
import {defineComponent} from 'vue';

export default defineComponent({
  name: 'ErrorsCard',
  props: {
    errors: {
      type: Array,
      required: true
    }
  },
  setup() {
    return {
      thumbStyle: {
        right: '4px',
        borderRadius: '5px',
        backgroundColor: '#6A1A78',
        width: '5px',
        opacity: 0.75
      },

      barStyle: {
        right: '2px',
        borderRadius: '9px',
        backgroundColor: '#6A1A78',
        width: '9px',
        opacity: 0.2
      }
    };
  },
  methods: {
    copyToClipboard(text: string) {
      navigator.clipboard.writeText(text);
      this.$q.notify({
        timeout: 5000,
        progress: true,
        icon: 'info',
        color: 'info',
        message: 'Copied to clipboard!'
      });
    }
  }
});
</script>
