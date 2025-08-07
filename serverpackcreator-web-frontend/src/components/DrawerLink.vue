<template>
  <q-item clickable :to="link">
    <q-item-section avatar>
      <q-icon :name="icon" colour="accent" left/>
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
  name: 'DrawerLink',
  props: {
    title: {
      type: String,
      required: true
    },

    caption: {
      type: String,
      default: ''
    },

    link: {
      type: String,
      default: '#'
    },

    icon: {
      type: String,
      default: ''
    }
  }
});
</script>
