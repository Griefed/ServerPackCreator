<template>
  <q-item filled square stack-label>
    <q-item-section avatar>
      <q-btn
        :to="link"
        :icon="icon"
        round
        style="margin-bottom: 18px; background:radial-gradient(circle, #325358 0%, #45a395 100%);color: floralwhite;" />
    </q-item-section>

    <q-item-section>
      <q-item-label>{{ title }}</q-item-label>
      <q-item-label caption>{{ caption }}</q-item-label>
    </q-item-section>
  </q-item>
</template>

<script >
import { defineComponent } from 'vue';

export default defineComponent({
  name: 'IndexItem',
  props: {
    title: {
      type: String,
      required: true
    },

    caption: {
      type: String,
      required: true
    },

    icon: {
      type: String,
      required: true
    },

    link: {
      type: String,
      required: true
    }
  }
});
</script>
