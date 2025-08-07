<template>
  <q-field filled :label="title" square stack-label>
    <template v-slot:control>
      <div class="self-center full-width no-outline" tabindex="0">
        {{ link }}
      </div>
    </template>
    <template v-slot:default>
      <q-btn
        :href="link"
        :icon="icon"
        round
        style="margin-bottom: 18px; background:radial-gradient(circle, #325358 0%, #45a395 100%);color: floralwhite;"
        target="_blank"
        type="a"/>
    </template>
  </q-field>
</template>

<script >
import { defineComponent } from 'vue';

export default defineComponent({
  name: 'AboutItem',
  props: {
    title: {
      type: String,
      required: true
    },

    link: {
      type: String,
      required: true
    },

    icon: {
      type: String,
      required: true
    }
  }
});
</script>
