<template>
  <q-page class="row items-center justify-evenly">
    <q-card bordered style='max-width: 80%'>
      <q-card-section>
        <div class="text-h6">Welcome to ServerPackCreator!</div>
      </q-card-section>

      <q-card-section class="q-pt-none">
        Welcome to the ServerPackCreator-Webservice, where you can generate server packs from your modpacks,
        without having to install any software.<br>
        <br>
        The web-version of ServerPackCreator is a convenient way of generating a server pack with minimal effort.
        Said minimal effort comes at a price, though! Some customization-options are not available in the web-version of
        ServerPackCreator. For maximum customization of your server pack, make sure to download and install it to your
        machine.<br>
        Some options not available in the web-version are:
        <ul>
          <li>Custom script templates</li>
          <li>Custom server icon</li>
          <li>custom server properties</li>
          <li>Custom server pack suffix</li>
          <li>Custom script variables and values</li>
          <li>Custom inclusions of files and / or folders</li>
          <li>etc.</li>
        </ul>
        <br>
        If you're fine with the limited options the web-version offers, feel free to proceed to the ZIP-page and upload
        your modpack!
      </q-card-section>

      <q-separator inset />

      <q-card-section>
        The menu on the left lets you browse to the different areas of the web-version of ServerPackCreator.<br>
        The list below gives you a small overview of what's what and what's available.<br>
        <br>
        I suggest checking out the <strong>Submissions</strong>-page first!
      </q-card-section>

      <q-list>
        <IndexItem
          v-for="item in indexItems"
          :key="item.title"
          v-bind="item"
        />
      </q-list>
    </q-card>
  </q-page>
</template>

<script lang="ts">
import { defineComponent } from 'vue';
import IndexItem from 'components/IndexItem.vue';
const indexItems = [
  {
    title: 'Submissions',
    caption: 'This is where you upload a zipped modpack, or request a regeneration of an already uploaded modpack, in order to have ServerPackCreator generate a server pack from it.',
    icon: 'upload',
    link: 'submissions'
  },
  {
    title: 'Downloads',
    caption: 'Here you can browse available modpacks and their available server packs, and download any modpack and server pack you want.',
    icon: 'download',
    link: 'downloads'
  },
  {
    title: 'Event Log / History',
    caption: 'See the latest history for submitted tasks and find out whether your server pack was generated, or whether it is still waiting in the queue.',
    icon: 'history',
    link: 'history'
  },
  {
    title: 'About ServerPackCreator',
    caption: 'General information about this instance of the ServerPackCreator webservice, like the version, useful links, etc...',
    icon: 'info',
    link: 'about'
  }
]
export default defineComponent({
  name: 'IndexPage',
  components: { IndexItem },
  setup () {
    return {
      indexItems: indexItems
    };
  }
});
</script>
