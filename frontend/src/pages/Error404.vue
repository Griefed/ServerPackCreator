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
          label="Go Home"
          no-caps
          text-color="blue"
          to="/"
          unelevated
      />
    </div>
  </div>
</template>

<script lang="js">
export default {
  name: 'Error404',
  data() {
    return {}
  }
}
</script>

<style>
.background {
  background-image: url("~assets/background.webp");
  background-repeat: repeat;
  background-attachment: fixed;
}

</style>
