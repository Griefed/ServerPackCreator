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
      <q-item clickable @click="copyToClipboard(id.toString())">
        <q-item-section side>
          <q-icon color="accent" name="token"/>
        </q-item-section>
        <q-item-section>
          <q-item-label>ModPack ID</q-item-label>
          <q-item-label caption>{{ id }}</q-item-label>
        </q-item-section>
      </q-item>

      <q-item clickable @click="copyToClipboard(projectID.length === 1 ? projectID : 'N/A')">
        <q-item-section side>
          <q-icon color="accent" name="badge"/>
        </q-item-section>
        <q-item-section>
          <q-item-label>Modrinth Project ID</q-item-label>
          <q-item-label caption>{{ projectID.length === 1 ? projectID : 'N/A' }}</q-item-label>
        </q-item-section>
      </q-item>

      <q-item clickable @click="copyToClipboard(versionID.length === 1 ? versionID : 'N/A')">
        <q-item-section side>
          <q-icon color="accent" name="badge"/>
        </q-item-section>
        <q-item-section>
          <q-item-label>Modrinth Version ID</q-item-label>
          <q-item-label caption>{{ versionID.length === 1 ? versionID : 'N/A' }}</q-item-label>
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

      <q-item clickable @click="copyToClipboard(name)">
        <q-item-section side>
          <q-icon color="accent" name="abc"/>
        </q-item-section>
        <q-item-section>
          <q-item-label>Name</q-item-label>
          <q-item-label caption class="force-wrap">{{ name }}</q-item-label>
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

      <q-item clickable @click="copyToClipboard(status)">
        <q-item-section side>
          <q-icon color="accent" name="pending_actions"/>
        </q-item-section>
        <q-item-section>
          <q-item-label>Current Status</q-item-label>
          <q-item-label caption>{{ status }}</q-item-label>
        </q-item-section>
      </q-item>

      <q-item clickable @click="copyToClipboard(source)">
        <q-item-section side>
          <q-icon color="accent" name="move_to_inbox"/>
        </q-item-section>
        <q-item-section>
          <q-item-label>ModPack Source</q-item-label>
          <q-item-label caption>{{ source }}</q-item-label>
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

      <q-item clickable @click="copyToClipboard(serverPacks.length.toString())">
        <q-item-section side>
          <q-icon color="accent" name="dns"/>
        </q-item-section>
        <q-item-section>
          <q-item-label>ServerPacks</q-item-label>
          <q-item-label caption>{{ serverPacks.length }}</q-item-label>
        </q-item-section>
      </q-item>
    </q-list>
  </q-card>
</template>

<script >
import {defineComponent, ref} from 'vue';
import {modpacks} from 'boot/axios';
import {date} from 'quasar';

export default defineComponent({
  name: 'ModPackCard',
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
      dateCreated: ref(0),
      name: ref(''),
      projectID: ref(''),
      serverPacks: ref([]),
      sha256: ref(''),
      size: ref(0),
      source: ref(''),
      status: ref(''),
      versionID: ref('')
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
    modpacks.get(this.id.toString()).then(response => {
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
    }).catch(error => {
      this.visible = false;
      this.showSimulatedReturnData = true;
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
