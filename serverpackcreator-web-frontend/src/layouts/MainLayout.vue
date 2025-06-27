<template>
  <q-layout view="hHh Lpr lff">
    <q-header elevated reveal bordered class="tile-x">
      <q-toolbar>
        <q-btn flat dense round icon="menu" aria-label="Menu" @click="drawerClick" />
        <q-separator inset />
        <q-toolbar-title>
          <q-img :ratio="323/54" width="256px" alt="ServerPackCreator" fit="contain"
                 src="~assets/serverpackcreator.webp" />
        </q-toolbar-title>
      </q-toolbar>
    </q-header>

    <q-drawer
      v-model="drawer"
      :mini="miniState"
      :width="270"
      :breakpoint="500"
      mini-to-overlay
      bordered
      show-if-above
      class="tile2-y"
    >
      <q-list>
        <DrawerLink
          v-for="link in linksList"
          :key="link.title"
          v-bind="link"
        />
      </q-list>
    </q-drawer>

    <q-page-container>
      <router-view />
    </q-page-container>
  </q-layout>
</template>

<script >
import { defineComponent, ref } from 'vue';
import DrawerLink from 'components/DrawerLink.vue';

const linksList = [
  {
    title: 'Home',
    caption: 'Back to the start-page',
    icon: 'cottage',
    link: '/'
  },
  {
    title: 'Submissions',
    caption: 'Generate from zipped modpack',
    icon: 'upload',
    link: '/submissions'
  },
  {
    title: 'Downloads',
    caption: 'Browser modpacks and server packs for downloads',
    icon: 'download',
    link: '/downloads'
  },
  {
    title: 'History',
    caption: 'Event history',
    icon: 'history',
    link: '/history'
  },
  {
    title: 'About',
    caption: 'About ServerPackCreator',
    icon: 'info',
    link: '/about'
  }
];

export default defineComponent({
  name: 'MainLayout',

  components: {
    DrawerLink: DrawerLink
  },

  setup() {
    const drawer = ref(true);
    const miniState = ref(true);
    return {
      linksList: linksList,
      drawer,
      miniState,
      drawerClick(e) {
        miniState.value = !miniState.value;
        e.stopPropagation();
      }
    };
  }
});
</script>

<style>
</style>
