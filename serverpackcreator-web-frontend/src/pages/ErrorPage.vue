<template>
  <div class="fullscreen text-white text-center q-pa-md flex flex-center">
    <div>
      <div style="font-size: 30vh">
        404
      </div>

      <div class="text-h2" style="opacity:.4">
        You shouldn't be here...
      </div>

      <q-btn
          class="q-mt-xl"
          color="white"
          label="Go Home"
          no-caps
          text-color="blue"
          to="/"
          unelevated
      />
    </div>
  </div>
</template>

<script >
import { defineComponent } from 'vue';

export default defineComponent({
  name: 'ErrorPage',
  setup() {
    return {}
  }
})
</script>
