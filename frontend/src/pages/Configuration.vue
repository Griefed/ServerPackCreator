<template>
  <div class="row no-wrap q-pa-md absolute-center">
    <q-intersection
      class="intersection"
      once
      transition="scale">
      <q-card :style="this.$q.platform.is.mobile ? 'max-width: 300px;width:300px' : 'max-width: 600px;width:600px'">
        <q-card-section>
          <div class="column">
            <div class="text-h6 q-mb-md text-center">Enter your CurseForge projectID and fileID</div>
            <q-form @submit="submit(store.state.projectID, store.state.fileID)" @reset="reset()" class="q-gutter-md">
              <div :class="this.$q.platform.is.mobile ? 'row' : 'row no-wrap'">
                <q-input
                  ref="projectRef"
                  color="black"
                  filled
                  v-model.number="store.state.projectID"
                  label="CurseForge projectID"
                  type="number"
                  maxlength="10"
                  class="full-width"
                  :rules="projectRules">
                  <template v-slot:append>
                    <q-icon name="info" @click="projectInfo = true" class="cursor-pointer" />
                  </template>
                  <q-tooltip :disable="this.$q.platform.is.mobile">
                    The projectID can be found in the top right of a modpacks CurseForge project page.
                  </q-tooltip>
                </q-input>
                <q-separator inset/>
                <q-input
                  ref="fileRef"
                  color="black"
                  filled
                  v-model.number="store.state.fileID"
                  label="CurseForge fileID"
                  type="number"
                  maxlength="10"
                  class="full-width"
                  :rules="fileRules">
                  <template v-slot:append>
                    <q-icon name="info" @click="fileInfo = true" class="cursor-pointer" />
                  </template>
                  <q-tooltip :disable="this.$q.platform.is.mobile">
                    The fileID can be found in the URL-bar after selecting a file of a project.
                  </q-tooltip>
                </q-input>
              </div>
              <div :class="this.$q.platform.is.mobile ? 'row flex-center' : 'row no-wrap flex-center'">
                <div>
                  <q-btn label="Submit" :disable="disable" :loading="loading" type="submit" color="primary" />
                  <q-btn label="Reset" type="reset" color="warning" class="q-ml-sm" />
                </div>
              </div>
            </q-form>
          </div>
        </q-card-section>
      </q-card>
    </q-intersection>
  </div>

  <q-dialog v-model="projectInfo">
    <q-card style="max-width: 1000px;width:750px">
      <q-card-section>
        <div class="text-h6 text-center">How to find the projectID</div>
      </q-card-section>
      <q-card-section class="q-pt-none text-center">
        Browse to the project which you want a server pack of.<br>
        On the project page on the right hand side, in the <b>About Project</b>-section<br>
        Check the line starting with <b>Project ID</b>-section.<br>
        <br>
        The number on the right of that text is the projectID you are looking for.
      </q-card-section>
      <q-card-section align="center">
        <q-img alt="Where to find the projectID of a CurseForge project." src="~assets/projectID.webp"/>
      </q-card-section>
      <q-card-actions align="right">
        <q-btn flat label="OK" color="primary" v-close-popup />
      </q-card-actions>
    </q-card>
  </q-dialog>

  <q-dialog v-model="fileInfo">
    <q-card style="max-width: 1000px;width:750px">
      <q-card-section>
        <div class="text-h6 text-center">How to find the file ID</div>
      </q-card-section>
      <q-card-section class="q-pt-none text-center">
        Browse to the project which you want a server pack of.<br>
        On the project page, browse to the <b>Files</b>-tab of the project.<br>
        Select the file you want a server pack of.<br>
        In the URL-field of your browser, check <b>the end</b> of the <b>URL</b>.<br>
        <br>
        The digits at the end of the URL is the fileID.
      </q-card-section>
      <q-card-section align="center">
        <q-img alt="Where to find the projectID of a CurseForge project." src="~assets/fileID.webp"/>
      </q-card-section>
      <q-card-actions align="right">
        <q-btn flat label="OK" color="primary" v-close-popup />
      </q-card-actions>
    </q-card>
  </q-dialog>

</template>

<script lang="js">
import { defineComponent, inject, ref } from 'vue';
import { useQuasar, Cookies } from 'quasar';
import { api } from "boot/axios";

export default defineComponent({
  name: "Configuration",
  setup() {

    const store = inject('store');
    const projectRef = ref(false);
    const fileRef = ref(false);
    const regenerationActivated = ref(false);

    return {
      store,
      projectRef,
      fileRef,
      regenerationActivated,
      disable: ref(false),
      loading: ref(false),
      projectInfo: ref(false),
      fileInfo: ref(false),
      projectRules: [
        projectID  => store.state.projectID >= 10 || 'ProjectID should not be smaller than 10!',
        projectID  => ( store.state.projectID !== null && store.state.projectID !== '' ) || 'Please enter a projectID!'
      ],
      fileRules: [
        fileID  => store.state.fileID >= 60018 || 'FileID should not be smaller than 60018!',
        projectID  => ( store.state.fileID !== null && store.state.fileID !== '' ) || 'Please enter a fileID!'
      ]
    }
  },
  methods: {
    submit(project, file) {
      this.loading = true;
      this.disable = true;
      this.$q.loadingBar.start();
      console.log(project + "," + file);
      //api.get("/curse?modpack=" + project + "," + file)
      api.get("/curse/task?modpack=" + project + "," + file)
          .then(response => {
            this.notify(response.data.status, response.data.timeout, response.data.icon, response.data.colour, response.data.message, project, file);
            this.enableButtons();
            if (response.data.status === 0 || response.data.status === 1) {
              this.setProjectCookies(project, file);
            }
          })
          .catch(error =>{
            console.log("An error occurred trying to contact the backend with the requested project.");
            console.log(error);
            this.notify(2,5000, 'error', 'negative', "An error occurred trying to contact the backend with the requested project: " + error);
            this.enableButtons();
      });
    },
    notify(status, timeout, icon, color, message, project, file) {
      if (status === 0 && this.regenerationActivated) {
        this.$q.notify({
          timeout: timeout,
          progress: true,
          multiLine: true,
          icon: icon,
          color: color,
          message: message,
          actions: [ { label: 'Regenerate', color: 'white', handler: () => { this.regenerate(project, file)} } ]
        })
      } else {
        this.$q.notify({
          timeout: timeout,
          progress: true,
          multiLine: true,
          icon: icon,
          color: color,
          message: message,
        })
      }
      this.$q.loadingBar.stop();
    },
    enableButtons() {
      this.loading = false;
      this.disable = false;
    },
    regenerate(project, file) {
      this.$q.loadingBar.start();
      api.get("/curse/regenerate?modpack=" + project + "," + file)
        .then(response => {
          this.notify(response.data.status, response.data.timeout, response.data.icon, response.data.colour, response.data.message, project, file);
        })
        .catch(error =>{
          console.log("An error occurred trying to contact the backend with the requested project.");
          console.log(error);
          this.$q.notify({
            timeout: 5000,
            progress: true,
            icon: 'error',
            color: 'negative',
            message: 'An error occurred trying to contact the backend with the requested project: ' + error
          })
          this.$q.loadingBar.stop();
        });
    },
    setProjectCookies(project, file) {
      this.$q.cookies.set('projectID', project);
      this.$q.cookies.set('fileID', file);
    },
    reset() {
      this.store.state.projectID = 10;
      this.store.state.fileID = 60018;
    }
  },
  mounted() {
    api.get("/curse/regenerate/active")
      .then(response => {this.regenerationActivated = response.data.regenerationActivated});
    if (this.$q.cookies.has('projectID')) {
      this.store.state.projectID = this.$q.cookies.get('projectID');
    }
    if (this.$q.cookies.has('fileID')) {
      this.store.state.fileID = this.$q.cookies.get('fileID');
    }
  }
})
</script>

<style>

.intersection {
  height: 100%;
  width: 100%;
}
</style>
