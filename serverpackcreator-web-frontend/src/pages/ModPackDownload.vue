<template>
  <q-page class="row items-center justify-evenly">
    <q-card flat bordered class="relative-position download-modpack" v-if="visible">
      <q-card-section>
        <transition appear enter-active-class="animated fadeIn" leave-active-class="animated fadeOut" />
      </q-card-section>
      <q-inner-loading :showing="visible">
        <q-spinner-gears size="50px" color="accent" />
      </q-inner-loading>
    </q-card>
    <q-card flat bordered class="relative-position download-modpack" v-else-if="!visible && !available">
      <q-card-section class="flex flex-center absolute-center">
        Modpack with ID {{ $route.params.id }} not found...
      </q-card-section>
    </q-card>
    <q-card class="relative-position download-modpack" v-else>
      <q-card-section horizontal>
        <q-card-section class="q-pt-xs">
          <div class="row">
            <div class="col text-overline">ModPack {{ $route.params.id }}</div>
            <div class="col text-overline">Source {{ source }}</div>
            <div v-if="projectID.length > 0" class="col text-overline">Project ID {{ projectID }}</div>
            <div v-if="versionID.length > 0" class="col text-overline">Version ID {{ versionID }}</div>
          </div>
          <div class="text-h5 q-mt-sm q-mb-xs">{{ name }}</div>
        </q-card-section>
      </q-card-section>
      <q-card-section horizontal>
        <q-list dense>
          <q-item clickable @click="copyToClipboard(size.toString())">
            <q-item-section side>
              <q-icon color="accent" name="scale" />
            </q-item-section>
            <q-item-section>
              <q-item-label>Size</q-item-label>
              <q-item-label caption>{{ size }} MB</q-item-label>
            </q-item-section>
          </q-item>

          <q-item clickable @click="copyToClipboard(status)">
            <q-item-section side>
              <q-icon color="accent" name="pending_actions" />
            </q-item-section>
            <q-item-section>
              <q-item-label>Current Status</q-item-label>
              <q-item-label caption>{{ status }}</q-item-label>
            </q-item-section>
          </q-item>

          <q-item clickable @click="copyToClipboard(source)">
            <q-item-section side>
              <q-icon color="accent" name="move_to_inbox" />
            </q-item-section>
            <q-item-section>
              <q-item-label>ModPack Source</q-item-label>
              <q-item-label caption>{{ source }}</q-item-label>
            </q-item-section>
          </q-item>

          <q-item clickable @click="copyToClipboard(sha256)">
            <q-item-section side>
              <q-icon color="accent" name="tag" />
            </q-item-section>
            <q-item-section>
              <q-item-label>ModPack SHA256 Hash</q-item-label>
              <q-item-label lines="1" class="force-wrap" caption>{{ sha256 }}</q-item-label>
            </q-item-section>
          </q-item>

          <q-item clickable @click="copyToClipboard(serverPacks.length.toString())">
            <q-item-section side>
              <q-icon color="accent" name="dns" />
            </q-item-section>
            <q-item-section>
              <q-item-label>ServerPacks</q-item-label>
              <q-item-label caption>{{ serverPacks.length }}</q-item-label>
            </q-item-section>
          </q-item>

          <q-item clickable @click="copyToClipboard(date.formatDate(dateCreated, 'YYYY-MM-DD : HH:mm'))">
            <q-item-section side>
              <q-icon color="accent" name="event" />
            </q-item-section>
            <q-item-section>
              <q-item-label>Creation Date and Time</q-item-label>
              <q-item-label caption>{{ date.formatDate(dateCreated, 'YYYY-MM-DD : HH:mm') }}</q-item-label>
            </q-item-section>
          </q-item>
        </q-list>
        <q-separator vertical />
        <q-card-actions vertical class="justify-around">
          <q-btn size="xl" flat round color="positive" icon="download" :loading="loading"
                 @click="downloadWithAxios($route.params.id);this.count = false; this.canceled = true;">
            <template v-slot:loading>
              <q-spinner-hourglass/>
            </template>
          </q-btn>
          <q-btn size="xl" flat round color="info" icon="share" @click="copyToClipboard(current())" />
        </q-card-actions>
      </q-card-section>
      <q-separator />
      <q-card-section>
        <q-item>
          <q-item-section>
            <q-item-label overline v-if="canceled">
              Download aborted...
            </q-item-label>
            <q-item-label overline v-else>
              {{ counter === 0 ? 'Downloading...' : 'Download starting in ' + counter }}
            </q-item-label>
            <q-item-label>
              <q-linear-progress :value="counter / 100 * 20" rounded class="q-mt-md" animation-speed="200" />
            </q-item-label>
          </q-item-section>
          <q-item-section side>
            <q-btn v-if="this.count" round color="negative" icon="cancel" @click="this.count = false; this.canceled = true;"/>
          </q-item-section>
        </q-item>
      </q-card-section>
    </q-card>
  </q-page>
</template>

<script >
import { defineComponent, ref } from 'vue';
import { modpacks } from 'boot/axios';
import { date } from 'quasar';

export default defineComponent({
  name: 'ModPackDownload',
  computed: {
    date() {
      return date;
    }
  },
  setup() {
    const count = ref(true);
    const counter = ref(5);
    const visible = ref(true);
    const available = ref(true);
    const showSimulatedReturnData = ref(false);
    return {
      visible,
      showSimulatedReturnData,
      available,
      showTextLoading() {
        visible.value = true;
        showSimulatedReturnData.value = false;

        setTimeout(() => {
          visible.value = false;
          showSimulatedReturnData.value = true;
        }, 3000);
      },
      dateCreated: ref(0),
      name: ref(''),
      projectID: ref(''),
      serverPacks: ref([]),
      sha256: ref(''),
      size: ref(0),
      source: ref(''),
      status: ref(''),
      versionID: ref(''),
      canceled: ref(false),
      loading: ref(false),
      count,
      counter
    };
  },
  methods: {
    current() {
      const route = this.$router.resolve({});
      console.log(this.$route);
      return new URL(route.href, window.location.origin).href;
    },
    copyToClipboard(text) {
      navigator.clipboard.writeText(text);
      this.$q.notify({
        timeout: 5000,
        progress: true,
        icon: 'info',
        color: 'info',
        message: 'Copied to clipboard!'
      });
    },
    countDownTimer() {
      if (this.counter > 0 && this.count) {
        setTimeout(() => {
          this.counter -= 1;
          this.countDownTimer();
        }, 1000);
      } else if (this.counter === 0 && this.count && !this.canceled) {
        this.count = false;
        this.downloadWithAxios(this.$route.params.id)
      }
    },
    downloadWithAxios(id) {
      this.loading = true
      modpacks.get('download/' + id, {
        responseType: 'arraybuffer'
      }).then(response => {
        const url = window.URL.createObjectURL(new Blob([response.data]))
        const link = document.createElement('a')
        link.href = url
        link.setAttribute('download', this.name)
        document.body.appendChild(link)
        link.click()
        this.loading = false
      }).catch(error => {
        this.$q.notify({
          timeout: 5000,
          progress: true,
          icon: 'error',
          color: 'negative',
          message: 'Could not retrieve modpack: ' + error
        });
        this.loading = false
      });
    }
  },
  mounted() {
    this.showTextLoading();
    modpacks.get(this.$route.params.id.toString()).then(response => {
      this.dateCreated = response.data.dateCreated;
      this.name = response.data.name.replaceAll(' ', '_');
      this.projectID = response.data.projectID;
      this.serverPacks = response.data.serverPacks;
      this.sha256 = response.data.sha256;
      this.size = response.data.size;
      this.source = response.data.source;
      this.status = response.data.status;
      this.versionID = response.data.versionID;
      this.visible = false;
      this.showSimulatedReturnData = true;
      this.count = true;
      this.countDownTimer();
    }).catch(error => {
      this.visible = false;
      this.showSimulatedReturnData = true;
      this.available = false;
      this.$q.notify({
        timeout: 5000,
        progress: true,
        icon: 'error',
        color: 'negative',
        message: 'Could not retrieve modpack: ' + error
      });
    });
  }
});
</script>

<style>
.download-modpack {
  height: 450px;
  width: 600px;
}
</style>
