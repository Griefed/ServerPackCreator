<template>
  <div class="row no-wrap q-pa-md absolute-center">
    <q-intersection
        class="intersection"
        once
        transition="scale">
      <q-card
          :style="this.$q.platform.is.mobile ? 'max-width: 300px;width:300px' : 'max-width: 600px;width:600px'">

        <q-tabs
            v-model="tab"
            active-bg-color="primary"
            active-color="secondary"
            align="justify"
            indicator-color="accent"
            narrow-indicator
        >
          <q-tab label="zip" name="zip">
            <q-tooltip :disable="this.$q.platform.is.mobile">Create a server pack from a zipped up
              modpack.
            </q-tooltip>
          </q-tab>
        </q-tabs>

        <q-separator/>
        <q-tab-panels v-model="tab" animated>
          <!--

          UPLOAD AND CREATE FROM ZIP

          -->
          <q-tab-panel name="zip">
            <div class="column">
              <div class="text-h6 q-mb-md text-center">Upload a modpack ZIP-archive</div>
              <q-form class="q-gutter-xs" @reset="resetForm" @submit="submitZip">
                <q-uploader
                    ref="zipUploader"
                    v-model="zipName"
                    accept=".zip,application/zip"
                    auto-upload
                    class="full-width"
                    field-name="file"
                    label="Only ZIP-archives (max 500Mb)"
                    max-file-size="512000000"
                    method="POST"
                    url="/api/v1/zip/upload"
                    @added="currentlyUploading = true"
                    @failed="uploadZipFailed"
                    @uploaded="uploadZipSuccess"
                >
                  <template v-slot:header="scope">
                    <div class="row no-wrap items-center q-pa-sm q-gutter-xs">

                      <q-btn v-if="scope.queuedFiles.length > 0" dense
                             flat icon="clear_all" round @click="scope.removeQueuedFiles">
                        <q-tooltip>Clear All</q-tooltip>
                      </q-btn>

                      <q-btn v-if="scope.uploadedFiles.length > 0" dense
                             flat icon="done_all" round @click="scope.removeUploadedFiles">
                        <q-tooltip>Remove Uploaded Files</q-tooltip>
                      </q-btn>

                      <q-spinner v-if="scope.isUploading" class="q-uploader__spinner"/>

                      <div class="col">
                        <div class="q-uploader__title">Only ZIP-archives (max 500Mb)</div>
                      </div>

                      <q-btn v-if="scope.uploadedFiles.length < 1" :disable="currentlyUploading"
                             dense flat icon="add_box" round type="a" @click="scope.pickFiles">
                        <q-uploader-add-trigger/>
                        <q-tooltip :disable="currentlyUploading || this.$q.platform.is.mobile">Pick
                          ZIP-archive
                        </q-tooltip>
                      </q-btn>

                      <q-btn dense flat icon="help_outline" round @click="zipInfo = true"/>
                    </div>
                  </template>
                </q-uploader>
                <div class="text-center flex-center">
                  ZIP-archives must be full modpacks, with mods, configs etc. No single-folder-
                  archives with only 'overrides' or the like.
                </div>

                <q-separator color="accent" inset spaced/>
                <!-- CLIENTSIDE MODS -->
                <div class="row flex-center">
                  <q-input
                      ref="clientMods"
                      v-model="clientsideMods"
                      class="full-width"
                      color="black"
                      filled
                      label="Clientside-only Mods"
                      type="textarea"
                  >
                    <q-tooltip :disable="this.$q.platform.is.mobile">
                      Comma separated. Clientside-only mods which must not be in the server
                      pack.<br>
                      If left blank, default values will be used.
                    </q-tooltip>
                  </q-input>
                </div>

                <!-- MINECRAFT VERSION -->
                <div class="row flex-center">

                  <div class="column flex-center">
                    <div class="row flex-center">
                      <b>Minecraft version:</b>
                    </div>
                    <div class="row flex-center">
                      <q-btn-dropdown
                          :label="minecraftVersion"
                          align="center"
                          no-caps
                          size="100%">
                        <q-list>
                          <q-item v-for="version in minecraftVersions" v-bind:key="version"
                                  v-close-popup clickable
                                  @click="minecraftVersionSelected(version)">
                            <q-item-section>
                              {{ version }}
                            </q-item-section>
                          </q-item>
                        </q-list>
                      </q-btn-dropdown>
                    </div>
                  </div>

                  <q-separator color="transparent" size="15px" vertical/>

                  <!-- MODLOADER -->
                  <div class="column flex-center">
                    <div class="row flex-center">
                      <b>Modloader:</b>
                    </div>
                    <div class="row flex-center">
                      <q-btn-dropdown
                          :label="modLoader"
                          align="center"
                          no-caps
                          size="100%">
                        <q-list>
                          <q-item v-for="loader in modloaders" v-bind:key="loader" v-close-popup
                                  clickable @click="modLoaderSelected(loader)">
                            <q-item-section>
                              {{ loader }}
                            </q-item-section>
                          </q-item>
                        </q-list>
                      </q-btn-dropdown>
                    </div>
                  </div>

                  <q-separator color="transparent" size="15px" vertical/>

                  <!-- MODLOADER VERSION -->
                  <div class="column flex-center">
                    <div class="row flex-center">
                      <b>Modloader version:</b>
                    </div>
                    <div class="row flex-center">
                      <q-btn-dropdown
                          :label="modloaderVersion"
                          align="center"
                          no-caps
                          size="100%">
                        <q-list v-if="modLoader === 'Forge'">
                          <q-item v-for="version in forgeVersions" v-bind:key="version"
                                  v-close-popup
                                  clickable @click="modloaderVersion = version">
                            <q-item-section>
                              {{ version }}
                            </q-item-section>
                          </q-item>
                        </q-list>
                        <q-list v-else-if="modLoader === 'Fabric'">
                          <q-item v-for="version in fabricVersions" v-bind:key="version"
                                  v-close-popup
                                  clickable @click="modloaderVersion = version">
                            <q-item-section>
                              {{ version }}
                            </q-item-section>
                          </q-item>
                        </q-list>
                        <q-list v-else-if="modLoader === 'Quilt'">
                          <q-item v-for="version in quiltVersions" v-bind:key="version"
                                  v-close-popup
                                  clickable @click="modloaderVersion = version">
                            <q-item-section>
                              {{ version }}
                            </q-item-section>
                          </q-item>
                        </q-list>
                      </q-btn-dropdown>
                    </div>
                  </div>
                </div>

                <div
                    :class="this.$q.platform.is.mobile ? 'row flex-center' : 'row no-wrap flex-center'"
                    style="margin-top: 20px;">
                  <div>
                    <q-btn :disable="disableZip" :loading="loading" color="primary" label="Submit"
                           type="submit"/>
                    <q-btn class="q-ml-sm" color="warning" label="Reset" type="reset"/>
                  </div>
                </div>

              </q-form>
            </div>
          </q-tab-panel>
        </q-tab-panels>
      </q-card>
    </q-intersection>
  </div>
  <!--

   ZIP HELP DIALOGS

  -->
  <q-dialog v-model="zipInfo" style="max-width: 1000px;width:750px">
    <q-card class="full-width">
      <q-card-section>
        <div class="text-h6">ZIP requirements in detail:</div>
      </q-card-section>

      <q-card-section class="q-pt-none text-center flex-center">
        <q-carousel
            v-model="zipSlide"
            :arrows="autoplay"
            :autoplay="autoplay"
            :navigation="autoplay"
            animated
            control-color="accent"
            infinite
            swipeable
            transition-next="slide-left"
            transition-prev="slide-right"
            @mouseenter="autoplay = false"
            @mouseleave="autoplay = true"
        >
          <q-carousel-slide :name="1" img-src="~assets/invalid_minecraft.webp"/>
          <q-carousel-slide :name="2" img-src="~assets/invalid_overrides.webp"/>
          <q-carousel-slide :name="3" img-src="~assets/valid1.webp"/>
          <q-carousel-slide :name="4" img-src="~assets/valid2.webp"/>
        </q-carousel>
        <q-chip outline>
          {{ zipSlideInfo[zipSlide - 1] }}
        </q-chip>

      </q-card-section>

      <q-card-section>
        <div>
          All modpack contents must be in the root of the ZIP-archive. Having either of the
          following in your modpack will
          help ServerPackCreator determine some configurations:
          <ul>
            <li>manifest.json (Overwolf's CurseForge or GDLauncher export)</li>
            <li>minecraftinstance.json (Overwolf's CurseForge)</li>
            <li>config.json (GDLauncher)</li>
            <li>mmc-pack.json & instance.cfg (MultiMC)</li>
          </ul>
          Configurations acquired from said files are:
          <ul>
            <li>Minecraft version</li>
            <li>Modloader</li>
            <li>Modlaoder version</li>
            <li>Modpack/Instance name if available</li>
          </ul>
        </div>
      </q-card-section>

      <q-card-actions align="right">
        <q-btn v-close-popup color="primary" flat label="OK"/>
      </q-card-actions>
    </q-card>
  </q-dialog>

</template>

<script lang="js">
import {defineComponent, inject, ref} from 'vue';
import {api} from 'boot/axios';

export default defineComponent({
  name: 'SubmitRequest',
  setup() {

    const store = inject('store');

    return {
      /*
       * GENERAL
       */
      store,
      loading: ref(false),
      tab: ref('zip'),
      /*
       * ZIP
       */
      minecraftVersion: ref("1.18.1"),
      minecraftVersions: ref([]),
      modLoader: ref("Forge"),
      modloaders: ref([]),
      clientsideMods: ref([]),
      clientsideModsDefault: ref([]),
      forgeVersions: ref([]),
      fabricVersions: ref([]),
      quiltVersions: ref([]),
      modloaderVersion: ref(""),
      zipName: ref(""),
      disableZip: ref(true),
      zipSlide: ref(1),
      autoplay: ref(true),
      currentlyUploading: ref(false),
      zipSlideInfo: [
        "Not accepted: Only minecraft",
        "Not accepted: Only overrides",
        "Accepted.",
        "Accepted."
      ],
      zipInfo: ref(false)
    }
  },
  methods: {
    /*
     *
     *  GENERAL
     *
     */

    /**
     * Reset the submission form.
     * @author Griefed
     */
    resetForm() {
      this.clientsideMods = this.clientsideModsDefault;
      this.minecraftVersion = this.minecraftVersions[0];
      this.modLoader = 'Forge';
      this.modloaderVersion = this.forgeVersions[0];
      this.$refs.zipUploader.abort();
      this.$refs.zipUploader.reset();
    },
    /**
     * Display a generic error notification with the error message passed.
     * @author Griefed
     * @param error
     */
    errorNotification(error) {
      this.$q.notify({
        timeout: 5000,
        progress: true,
        icon: 'error',
        color: 'negative',
        message: 'Communication with the backend failed. ' + error
      })
    },
    /*
     *
     *  ZIP
     *
     */
    submitZip() {
      console.log(this.zipName);
      api.get("/zip/" +
          [
            this.zipName,
            this.clientsideMods,
            this.minecraftVersion,
            this.modLoader,
            this.modloaderVersion
          ].join('&')
      )
      .then(response => {
        if (response.data.success) {
          this.$q.notify({
            timeout: response.data.timeout,
            progress: true,
            icon: response.data.icon,
            color: response.data.color,
            message: response.data.message
          })
        } else {
          this.$q.notify({
            timeout: response.data.timeout,
            progress: true,
            icon: response.data.icon,
            color: response.data.color,
            message: response.data.message
          })
        }
      })
      .catch(error => {
        this.errorNotification(error);
      });
    },
    /**
     * Set Forge and Fabric versions depending on which Minecraft version and modloader is selected.
     * @author Griefed
     * @param version The Minecraft version selected.
     */
    minecraftVersionSelected(version) {
      this.minecraftVersion = version;

      switch (this.modLoader) {
        case 'Forge':

          this.getForgeVersions(this.minecraftVersion);
          break;

        case 'Fabric':

          this.modloaderVersion = this.fabricVersions[0];
          break;

        case 'Quilt':

          this.modloaderVersion = this.quiltVersions[0];
          break;
      }

      this.disableZip = this.modloaderVersion === 'None' || this.zipName === "";
    },
    /**
     * Get the available Forge versions for the given Minecraft version.
     * @author Griefed
     * @param minecraftVersion
     */
    getForgeVersions(minecraftVersion) {

      api.get("/versions/forge/" + minecraftVersion)
      .then(response => {
        this.forgeVersions = response.data.forge;

        if (this.forgeVersions.length === 0) {
          console.log('Zero')
          this.modloaderVersion = 'None';
        } else {
          this.modloaderVersion = this.forgeVersions[0];
        }

      })
      .catch(error => {
        console.log(error);
        this.errorNotification(error);
      });

    },
    /**
     * Set selected modloader and modloader versions.
     * @author Griefed
     * @param loader The selected modloader.
     */
    modLoaderSelected(loader) {

      switch (loader) {
        case "Forge":

          this.modLoader = 'Forge';
          this.getForgeVersions(this.minecraftVersion);
          break;

        case "Fabric":

          this.modLoader = 'Fabric';
          this.modloaderVersion = this.fabricVersions[0];
          break;

        case "Quilt":

          this.modLoader = 'Quilt';
          this.modloaderVersion = this.quiltVersions[0];
          break;

      }
      this.disableZip = this.modloaderVersion === 'None' || this.zipName === "";
    },
    /**
     * If the upload of a given ZIP-file fails, display a notification to tell the user about said failure, reset the
     * ZIP-form, stop the loading-bar and disable the submit button.
     * @author Griefed
     * @param info XHR-Response from the backend.
     */
    uploadZipFailed(info) {

      this.$q.notify({
        timeout: JSON.parse(info.xhr.response).timeout,
        progress: true,
        icon: JSON.parse(info.xhr.response).icon,
        color: JSON.parse(info.xhr.response).color,
        message: JSON.parse(info.xhr.response).message
      })

      this.$refs.zipUploader.reset();
      this.$q.loadingBar.stop();

      this.zipName = "";

      this.disableZip = false;
    },
    /**
     * If the upload of a ZIP-file succeeds and the backend tells us that the ZIP-file is valid, we notify the user,
     * enable the submit button, save the filename we recieved from the backend and stop the loading-bar.
     * @author Griefed
     * @param info XHR-Response from the backend.
     */
    uploadZipSuccess(info) {

      this.$q.notify({
        timeout: JSON.parse(info.xhr.response).timeout,
        progress: true,
        icon: JSON.parse(info.xhr.response).icon,
        color: JSON.parse(info.xhr.response).color,
        message: JSON.parse(info.xhr.response).message
      })

      this.$q.loadingBar.stop();
      this.zipName = JSON.parse(info.xhr.response).file;

      if (JSON.parse(info.xhr.response).success) {

        this.zipName = JSON.parse(info.xhr.response).file;

        this.disableZip = this.modloaderVersion === 'None' || this.zipName === "";

      }
    }
  },
  mounted() {
    /*
     * Acquire some settings from our backend.
     *  - Acquire the configured list of clientside-only mods.
     *  - Acquire the fallback list of clientside-only mods.
     */
    api.get("/settings")
    .then(response => {
      this.clientsideMods = response.data.listFallbackMods;
      this.clientsideModsDefault = response.data.listFallbackMods;
      this.modloaders = response.data.supportedModloaders;
    })
    .catch(error => {
      console.log(error);
      this.errorNotification(error);
    });
    /*
     * Acquire a list of available Minecraft versions from our backend.
     * We do not get this list from Mojang directly because we need to make sure we only submit request to the backend
     * with versions that are available to the backend, too.
     */
    api.get("/versions/minecraft")
    .then(mcResponse => {
      this.minecraftVersions = mcResponse.data.minecraft;
      this.minecraftVersion = this.minecraftVersions[0];
      /*
       * Acquire a list of available Forge versions for a given Minecraft version from our backend.
       * We do not get this list from Forge directly because we need to make sure we only submit request to the backend
       * with versions that are available to the backend, too.
       */
      api.get("/versions/forge/" + this.minecraftVersion)
      .then(forgeResponse => {
        this.forgeVersions = forgeResponse.data.forge;
        this.modloaderVersion = this.forgeVersions[0];
      })
      .catch(error => {
        console.log(error);
        this.errorNotification(error);
      });

    })
    .catch(error => {
      console.log(error);
      this.errorNotification(error);
    });

    /*
     * Acquire a list of available Fabric versions from our backend.
     * We do not get this list from Fabric directly because we need to make sure we only submit request to the backend
     * with versions that are available to the backend, too.
     */
    api.get("/versions/fabric")
    .then(response => {
      this.fabricVersions = response.data.fabric;
    })
    .catch(error => {
      console.log(error);
      this.errorNotification(error);
    });

    /*
     * Acquire a list of available Quilt versions from our backend.
     * We do not get this list from Quilt directly because we need to make sure we only submit request to the backend
     * with versions that are available to the backend, too.
     */
    api.get("/versions/quilt")
    .then(response => {
      this.quiltVersions = response.data.quilt;
    })
    .catch(error => {
      console.log(error);
      this.errorNotification(error);
    });
  }
})
</script>

<style>

.intersection {
  height: 100%;
  width: 100%;
}
</style>
