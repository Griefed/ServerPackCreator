<template>
  <div class="row no-wrap q-pa-md absolute-center">
    <q-intersection
      class="intersection"
      once
      transition="scale">
      <q-card :style="this.$q.platform.is.mobile ? 'max-width: 300px;width:300px' : 'max-width: 600px;width:600px'">
        <q-card-section>
          <div class="column">
            <div class="text-h6 q-mb-md text-center">Upload your modpack ZIP-archive</div>

          </div>
        </q-card-section>
      </q-card>
    </q-intersection>
  </div>

</template>

<script lang="js">
import { defineComponent, inject, ref } from 'vue';
import { useQuasar, Cookies, openURL  } from 'quasar';
import { api } from "boot/axios";

export default defineComponent({
  name: "ArchivePage",
  setup() {

    const store = inject('store');

    return {
      store,
      disable: ref(false),
      loading: ref(false),
    }
  },
  methods: {
    /**
     * Submit a CurseForge project and fileID combination for generation. If the combination is invalid, a notification is
     * shown to the user, telling them what went wrong. If the requested combination already exists, a notification is shown
     * prompting the user to either regenerate the server pack (if regeneration is enabled on the affected instance), or
     * download the server pack.
     * @author Griefed
     * @param project The CurseForge projectID
     * @param file The CurseForge fileID
     */
    submit(project, file) {
      this.loading = true;
      this.disable = true;
      this.$q.loadingBar.start();

    },
    /**
     * Shows a notification using the input for formatting and information shown.
     * @author Griefed
     * @param status
     * @param timeout
     * @param icon
     * @param color
     * @param message
     * @param project
     * @param file
     */
    notify(status, timeout, icon, color, message, project, file) {

      this.$q.notify({
        timeout: timeout,
        progress: true,
        multiLine: true,
        icon: icon,
        color: color,
        message: message,
      })

      this.$q.loadingBar.stop();
    },
    /**
     * Disable buttons for submitting a new request.
     * @author Griefed
     */
    enableButtons() {
      this.loading = false;
      this.disable = false;
    },
    /**
     * Reset the submission form.
     * @author Griefed
     */
    reset() {

    }
  },
  /**
   * Check whether regeneration is active on our instance.
   * Check and get CurseForge project and fileID cookies, if they exist.
   * @author Griefed
   */
  mounted() {

  }
})
</script>

<style>

.intersection {
  height: 100%;
  width: 100%;
}
</style>
