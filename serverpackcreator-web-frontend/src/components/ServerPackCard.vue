<template>
  <q-card flat bordered style="height: 385px;" class="relative-position" v-if="visible">
    <q-card-section>
      <transition appear enter-active-class="animated fadeIn" leave-active-class="animated fadeOut"/>
    </q-card-section>
    <q-inner-loading :showing="visible">
      <q-spinner-gears size="50px" color="accent"/>
    </q-inner-loading>
  </q-card>
  <q-card flat bordered style="height: 400px;" v-else>
    <q-list dense>
      <q-item clickable @click="copyToClipboard(id)">
        <q-item-section side>
          <q-icon color="accent" name="token"/>
        </q-item-section>
        <q-item-section>
          <q-item-label>ServerPack ID</q-item-label>
          <q-item-label caption>{{ id }}</q-item-label>
        </q-item-section>
      </q-item>

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
          <q-item-label>ServerPack SHA256 Hash</q-item-label>
          <q-item-label lines="1" caption class="force-wrap">{{ sha256 }}</q-item-label>
        </q-item-section>
      </q-item>
    </q-list>
  </q-card>
</template>

<script >
import {defineComponent, ref} from 'vue';
import {serverpacks} from 'boot/axios';
import {date} from 'quasar';

export default defineComponent({
  name: 'ServerPackCard',
  computed: {
    date() {
      return date;
    }
  },
  props: {
    id: {
      type: String,
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
      dateCreated: ref(0),
      sha256: ref(''),
      size: ref(0),
      confirmedWorking: ref(0),
      downloads: ref(0)
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
    serverpacks.get(this.id).then(response => {
      this.dateCreated = response.data.dateCreated;
      this.sha256 = response.data.sha256;
      this.size = response.data.size;
      this.downloads = response.data.downloads;
      this.confirmedWorking = response.data.confirmedWorking;
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
