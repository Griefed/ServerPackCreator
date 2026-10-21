<template>
  <div class="background text-white text-center q-pa-md flex flex-center">
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
        text-color="blue"
        unelevated
        to="/"
        label="Go Home"
        no-caps
      />
    </div>
  </div>
</template>

<script>
import { defineComponent } from 'vue'

export default defineComponent({
  name: 'PageError404'
})
</script>

<style>
.background {
  background-image: url("~assets/background.webp");
  background-repeat: repeat;
  background-attachment: fixed;
}

</style>
