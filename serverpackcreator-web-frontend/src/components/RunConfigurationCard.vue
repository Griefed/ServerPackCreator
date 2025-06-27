<template>
  <q-card flat bordered style="height: 385px; max-width: 100vw;" class="relative-position" v-if="visible">
    <q-card-section>
      <transition appear enter-active-class="animated fadeIn" leave-active-class="animated fadeOut"/>
    </q-card-section>
    <q-inner-loading :showing="visible">
      <q-spinner-gears size="50px" color="accent"/>
    </q-inner-loading>
  </q-card>
  <q-card flat bordered v-else>
    <q-scroll-area :thumb-style="thumbStyle" :bar-style="barStyle" style="height: 400px;">
      <q-list dense>
        <q-item clickable @click="copyToClipboard(id.toString())">
          <q-item-section side>
            <q-icon color="accent" name="token"/>
          </q-item-section>
          <q-item-section>
            <q-item-label>RunConfiguration ID</q-item-label>
            <q-item-label caption>{{ id }}</q-item-label>
          </q-item-section>
        </q-item>

        <q-item clickable @click="copyToClipboard(minecraftVersion)">
          <q-item-section side>
            <q-icon color="accent" name="pin"/>
          </q-item-section>
          <q-item-section>
            <q-item-label>Minecraft Version</q-item-label>
            <q-item-label caption>{{ minecraftVersion }}</q-item-label>
          </q-item-section>
        </q-item>

        <q-item clickable @click="copyToClipboard(modloader)">
          <q-item-section side>
            <q-icon color="accent" name="pin"/>
          </q-item-section>
          <q-item-section>
            <q-item-label>Modloader</q-item-label>
            <q-item-label caption>{{ modloader }}</q-item-label>
          </q-item-section>
        </q-item>

        <q-item clickable @click="copyToClipboard(modloaderVersion)">
          <q-item-section side>
            <q-icon color="accent" name="pin"/>
          </q-item-section>
          <q-item-section>
            <q-item-label>Modloader Version</q-item-label>
            <q-item-label caption>{{ modloaderVersion }}</q-item-label>
          </q-item-section>
        </q-item>

        <q-item clickable @click="copyToClipboard(startArgs.join(' '))">
          <q-item-section side>
            <q-icon color="accent" name="not_started"/>
          </q-item-section>
          <q-item-section>
            <q-item-label>Start Arguments</q-item-label>
            <q-item-label lines="1" caption class="force-wrap">{{ startArgs.join(' ') }}</q-item-label>
          </q-item-section>
        </q-item>

        <q-item clickable @click="copyToClipboard(clientMods.join(', '))">
          <q-item-section side>
            <q-icon color="accent" name="format_list_numbered"/>
          </q-item-section>
          <q-item-section>
            <q-item-label>Client Mods</q-item-label>
            <q-item-label lines="1" caption class="force-wrap">{{ clientMods.join(', ') }}</q-item-label>
          </q-item-section>
        </q-item>

        <q-item clickable @click="copyToClipboard(whitelistedMods.join(', '))">
          <q-item-section side>
            <q-icon color="accent" name="format_list_numbered"/>
          </q-item-section>
          <q-item-section>
            <q-item-label>Whitelisted Mods</q-item-label>
            <q-item-label caption class="force-wrap">{{ whitelistedMods.join(', ') }}</q-item-label>
          </q-item-section>
        </q-item>
      </q-list>
    </q-scroll-area>
  </q-card>
</template>

<script >
import {defineComponent, ref} from 'vue';
import {serverpacks} from 'boot/axios';
import {date} from 'quasar';

export default defineComponent({
  name: 'RunConfigurationCard',
  computed: {
    date() {
      return date;
    }
  },
  props: {
    id: {
      type: Number,
      required: true
    }
  },
  setup() {
    const visible = ref(true);
    const showSimulatedReturnData = ref(false);
    return {
      visible,
      showSimulatedReturnData,
      showTextLoading() {
        visible.value = true;
        showSimulatedReturnData.value = false;

        setTimeout(() => {
          visible.value = false;
          showSimulatedReturnData.value = true;
        }, 3000);
      },
      minecraftVersion: ref(''),
      modloader: ref(''),
      modloaderVersion: ref(''),
      startArgs: ref([]),
      clientMods: ref([]),
      whitelistedMods: ref([]),
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
    copyToClipboard(text) {
      navigator.clipboard.writeText(text);
      this.$q.notify({
        timeout: 5000,
        progress: true,
        icon: 'info',
        color: 'info',
        message: 'Copied to clipboard!'
      });
    }
  },
  mounted() {
    this.showTextLoading();
    serverpacks.get(this.id.toString()).then(response => {
      let runConfig = response.data.runConfiguration
      this.minecraftVersion = runConfig.minecraftVersion
      this.modloader = runConfig.modloader
      this.modloaderVersion = runConfig.modloaderVersion
      this.startArgs = runConfig.startArgs.map(entry => entry.argument)
      this.clientMods = runConfig.clientMods.map(entry => entry.mod)
      this.whitelistedMods = runConfig.whitelistedMods.map(entry => entry.mod)
      this.visible = false;
      this.showSimulatedReturnData = true;
    }).catch(error => {
      this.visible = false;
      this.showSimulatedReturnData = true;
      this.$q.notify({
        timeout: 5000,
        progress: true,
        icon: 'error',
        color: 'negative',
        message: 'Could not retrieve serverpack: ' + error
      });
    });
  }
});
</script>
