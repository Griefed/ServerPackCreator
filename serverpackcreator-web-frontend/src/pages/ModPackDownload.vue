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
            <div class="col text-overline">ModPack {{ name }}</div>
            <div class="col text-overline">Source {{ source }}</div>
            <div v-if="projectID.length > 0" class="col text-overline">Project ID<br>{{ projectID }}</div>
            <div v-if="versionID.length > 0" class="col text-overline">Version ID<br>{{ versionID }}</div>
          </div>
          <div class="text-h5 q-mt-sm q-mb-xs">ID {{ $route.params.id }}</div>
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
          <q-btn ref="downloadButton" size="xl" flat round color="positive" icon="download"
                 type="a" :href="downloadHref" download
                 @click="onLinkClicked" />
          <q-btn size="xl" flat round color="info" icon="share" @click="copyToClipboard(current())" />
        </q-card-actions>
      </q-card-section>
      <q-separator />
      <q-card-section>
        <q-item>
          <q-item-section>
            <q-item-label overline v-if="started">
              Downloading &mdash; your browser is saving the file.
            </q-item-label>
            <q-item-label overline v-else-if="failed">
              Download failed &mdash; the server could not provide the file.
            </q-item-label>
            <q-item-label overline v-else-if="canceled">
              Download aborted...
            </q-item-label>
            <q-item-label overline v-else>
              Download starting in {{ counter }}
            </q-item-label>
            <q-item-label>
              <q-linear-progress :value="counter / 100 * 20" rounded class="q-mt-md" animation-speed="200" />
            </q-item-label>
          </q-item-section>
          <q-item-section side>
            <q-btn v-if="count" round color="negative" icon="cancel" @click="count = false; canceled = true;"/>
          </q-item-section>
        </q-item>
      </q-card-section>
    </q-card>
  </q-page>
</template>

<script lang="ts">
import { defineComponent, ref } from 'vue';
import { modpacks } from '@/boot/axios';
import { date } from 'quasar';

export default defineComponent({
  name: 'ModPackDownload',
  computed: {
    date() {
      return date;
    },
    /**
     * The archive's own URL, built from the boot module's `baseURL` so the API path stays written in
     * exactly one place. The browser fetches this itself; nothing about the archive passes through
     * JavaScript.
     */
    downloadHref(): string {
      return `${modpacks.defaults.baseURL ?? ''}${this.downloadPath}`;
    },
    /** The download route relative to the boot module's `baseURL`, for the link and for the availability check. */
    downloadPath(): string {
      return `download/${encodeURIComponent(String(this.$route.params.id))}`;
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
      started: ref(false),
      failed: ref(false),
      count,
      counter
    };
  },
  methods: {
    current() {
      const route = this.$router.resolve({});
      return new URL(route.href, window.location.origin).href;
    },
    copyToClipboard(text: string) {
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
          if (!this.count) {
            return;
          }
          this.counter -= 1;
          this.countDownTimer();
        }, 1000);
      } else if (this.counter === 0 && this.count && !this.canceled) {
        this.count = false;
        this.startDownload()
      }
    },
    /**
     * Fires the download link the countdown was counting towards. Clicks the real anchor rather than
     * fetching the archive: the browser then streams it straight to disk, with its own progress, its
     * own resume on an interrupted transfer, and no copy of a multi-gigabyte pack in the tab. The browser
     * reports a failed download only in its own download list, so the route is asked with `HEAD` first and
     * the page claims a download only once the archive is known to be there.
     */
    startDownload() {
      const link = (this.$refs.downloadButton as { $el?: unknown } | undefined)?.$el;
      if (!(link instanceof HTMLAnchorElement)) {
        return;
      }
      modpacks.head(this.downloadPath).then(() => {
        this.started = true;
        link.click();
      }).catch(error => this.reportUnavailable(error));
    },
    /**
     * A click on the link itself, by the user or by `startDownload`. The browser is already fetching, so
     * the availability check runs alongside and takes the "Downloading" claim back if it fails.
     */
    onLinkClicked() {
      this.count = false;
      if (this.started) {
        return;
      }
      this.started = true;
      this.failed = false;
      modpacks.head(this.downloadPath).catch(error => this.reportUnavailable(error));
    },
    reportUnavailable(error: unknown) {
      this.started = false;
      this.failed = true;
      this.$q.notify({
        timeout: 5000,
        progress: true,
        icon: 'error',
        color: 'negative',
        message: 'Could not download the modpack: ' + String(error)
      });
    }
  },
  /** Stops the countdown, so a page the user has left never fires its download. */
  beforeUnmount() {
    this.count = false;
  },
  mounted() {
    this.showTextLoading();
    modpacks.get(String(this.$route.params.id)).then(response => {
      this.dateCreated = response.data.dateCreated;
      this.name = response.data.name;
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
