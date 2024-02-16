<template>
  <q-page class="row items-center justify-evenly">
    <q-card flat bordered class="relative-position download-serverpack" v-if="visible">
      <q-card-section>
        <transition appear enter-active-class="animated fadeIn" leave-active-class="animated fadeOut"/>
      </q-card-section>
      <q-inner-loading :showing="visible">
        <q-spinner-gears size="50px" color="accent"/>
      </q-inner-loading>
    </q-card>
    <q-card flat bordered class="relative-position download-serverpack" v-else-if="!visible && !available">
      <q-card-section class="flex flex-center absolute-center">
        Server Pack with ID {{ $route.params.id }} not found...
      </q-card-section>
    </q-card>
    <q-card class="relative-position download-serverpack" v-else>
      <q-card-section horizontal>
        <q-card-section class="q-pt-xs">
          <div class="text-overline">Server Pack {{ $route.params.id }}</div>
        </q-card-section>
      </q-card-section>
      <q-card-section horizontal>
        <q-list dense>
          <q-item clickable @click="copyToClipboard(size.toString())">
            <q-item-section side>
              <q-icon color="accent" name="scale"/>
            </q-item-section>
            <q-item-section>
              <q-item-label>Size</q-item-label>
              <q-item-label caption>{{ size }} MB</q-item-label>
            </q-item-section>
          </q-item>

          <q-item clickable @click="copyToClipboard(downloads.toString())">
            <q-item-section side>
              <q-icon color="accent" name="download"/>
            </q-item-section>
            <q-item-section>
              <q-item-label>Downloads</q-item-label>
              <q-item-label caption>{{ downloads }}</q-item-label>
            </q-item-section>
          </q-item>

          <q-item clickable @click="copyToClipboard(confirmedWorking.toString())">
            <q-item-section side>
              <q-icon color="accent" name="thumbs_up_down"/>
            </q-item-section>
            <q-item-section>
              <q-item-label>Confirmed Working</q-item-label>
              <q-item-label caption>{{ confirmedWorking }}</q-item-label>
            </q-item-section>
          </q-item>

          <q-item clickable @click="copyToClipboard(date.formatDate(dateCreated, 'YYYY-MM-DD : HH:mm'))">
            <q-item-section side>
              <q-icon color="accent" name="event"/>
            </q-item-section>
            <q-item-section>
              <q-item-label>Creation Date and Time</q-item-label>
              <q-item-label caption>{{ date.formatDate(dateCreated, 'YYYY-MM-DD : HH:mm') }}</q-item-label>
            </q-item-section>
          </q-item>

          <q-item clickable @click="copyToClipboard(sha256)">
            <q-item-section side>
              <q-icon color="accent" name="tag"/>
            </q-item-section>
            <q-item-section>
              <q-item-label>ModPack SHA256 Hash</q-item-label>
              <q-item-label lines="1" class="force-wrap" caption>{{ sha256 }}</q-item-label>
            </q-item-section>
          </q-item>
        </q-list>
        <q-separator vertical/>
        <q-card-actions vertical class="justify-around">
          <q-btn size="xl" flat round color="positive" icon="download"
                 :href="buildDownloadUrl($route.params.id)" type="a"/>
          <q-btn size="xl" flat round color="info" icon="share" @click="copyToClipboard(location())"/>
        </q-card-actions>
      </q-card-section>
      <q-separator/>
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
              <q-linear-progress :value="counter / 100 * 20" rounded class="q-mt-md" animation-speed="200"/>
            </q-item-label>
          </q-item-section>
          <q-item-section side>
            <q-btn round color="negative" icon="cancel" @click="this.count = false; this.canceled = true;"/>
          </q-item-section>
        </q-item>
      </q-card-section>
    </q-card>
  </q-page>
</template>

<script lang="ts">
import {defineComponent, ref} from 'vue';
import {serverpacks} from 'boot/axios';
import {date} from 'quasar';

export default defineComponent({
  name: 'ServerPackDownload',
  computed: {
    date() {
      return date;
    }
  },
  setup() {
    const count = ref(true)
    const counter = ref(5)
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
      size: ref(0),
      downloads: ref(0),
      confirmedWorking: ref(0),
      dateCreated: ref(0),
      sha256: ref(''),
      canceled: ref(false),
      count,
      counter
    };
  },
  methods: {
    location(): string {
      const route = this.$router.resolve({});
      return new URL(route.href, window.location.origin).href;
    },
    buildDownloadUrl(id: number): string {
      return '/api/v2/serverpacks/download/' + id
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
          this.counter -= 1;
          this.countDownTimer()
        }, 1000)
      } else if (this.counter === 0 && this.count && !this.canceled) {
        this.count = false;
        window.open(window.location.origin + this.buildDownloadUrl(this.$route.params.id.toString()));
      }
    }
  },
  mounted() {
    this.showTextLoading();
    serverpacks.get(this.$route.params.id.toString()).then(response => {
      this.size = response.data.size;
      this.downloads = response.data.downloads;
      this.confirmedWorking = response.data.confirmedWorking;
      this.dateCreated = response.data.dateCreated;
      this.sha256 = response.data.sha256;
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
        message: 'Could not retrieve server pack: ' + error
      });
    });
  }
});
</script>

<style>
.download-serverpack {
  height: 350px;
  width: 600px;
}
</style>
