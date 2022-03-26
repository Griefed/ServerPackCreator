<template>
  <div class="row no-wrap q-pa-md absolute-center">
    <q-intersection
      class="intersection"
      once
      transition="scale">
      <q-card :style="this.$q.platform.is.mobile ? 'max-width: 300px;width:300px' : 'max-width: 600px;width:600px'">

        <q-tabs
          v-model="tab"
          active-bg-color="primary"
          active-color="secondary"
          indicator-color="accent"
          align="justify"
          narrow-indicator
        >
          <q-tab :disable="curse" name="curseforge" label="CurseForge">
            <q-tooltip :disable="this.$q.platform.is.mobile">The CurseForge-module in ServerPackCreator is currently disabled.</q-tooltip>
          </q-tab>
          <q-tab name="zip" label="zip">
            <q-tooltip :disable="this.$q.platform.is.mobile">Create a server pack from a zipped up modpack.</q-tooltip>
          </q-tab>
          <q-tab :disable="modrinth" name="modrinth" label="Modrinth">
            <q-tooltip :disable="this.$q.platform.is.mobile">The Modrinth-module is not yet implemented.</q-tooltip>
          </q-tab>
        </q-tabs>

        <q-separator/>
<!--

CREATE FROM CURSEFORGE PROJECTID,FILEID

-->
        <q-tab-panels v-model="tab" animated>
          <q-tab-panel name="curseforge">
            <q-tooltip :disable="this.$q.platform.is.mobile">The CurseForge-module in ServerPackCreator is currently disabled.</q-tooltip>
            <div class="column">
              <div class="text-h6 q-mb-md text-center">Enter your CurseForge projectID and fileID</div>
              <q-form :disable="curse" @submit="submitCurse(store.state.projectID, store.state.fileID)" @reset="resetForm" class="q-gutter-xs">
                <div class="row flex-center">
                  <q-input
                    :disable="curse"
                    ref="projectRef"
                    color="black"
                    filled
                    v-model.number="store.state.projectID"
                    label="CurseForge projectID"
                    type="number"
                    maxlength="10"
                    :rules="projectRules">
                    <template v-slot:append>
                      <q-icon name="info" @click="projectInfo = true" class="cursor-pointer" />
                    </template>
                    <q-tooltip :disable="this.$q.platform.is.mobile">
                      The projectID can be found in the top right of a modpacks CurseForge project page.
                    </q-tooltip>
                  </q-input>
                </div>

                <div class="row flex-center">
                  <q-input
                    :disable="curse"
                    ref="fileRef"
                    color="black"
                    filled
                    v-model.number="store.state.fileID"
                    label="CurseForge fileID"
                    type="number"
                    maxlength="10"
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
                    <q-btn label="Submit" :disable="disableCurse || curse" :loading="loading" type="submit" color="primary" />
                    <q-btn label="Reset"  :disable="curse"                                type="reset" color="warning" class="q-ml-sm" />
                  </div>
                </div>
              </q-form>
            </div>
          </q-tab-panel>
<!--

UPLOAD AND CREATE FROM ZIP

-->
          <q-tab-panel name="zip">
            <div class="column">
              <div class="text-h6 q-mb-md text-center">Upload a modpack ZIP-archive</div>
              <q-form @submit="submitZip" @reset="resetForm" class="q-gutter-xs">
                <q-uploader
                  class="full-width"
                  url="/api/v1/zip/upload"
                  method="POST"
                  field-name="file"
                  auto-upload
                  label="Only ZIP-archives (max 500Mb)"
                  accept=".zip,application/zip"
                  max-file-size="512000000"
                  ref="zipUploader"
                  @uploaded="uploadZipSuccess"
                  @failed="uploadZipFailed"
                  @added="currentlyUploading = true"
                  v-model="zipName"
                >
                  <template v-slot:header="scope">
                    <div class="row no-wrap items-center q-pa-sm q-gutter-xs">

                      <q-btn v-if="scope.queuedFiles.length > 0" icon="clear_all" @click="scope.removeQueuedFiles" round dense flat >
                        <q-tooltip>Clear All</q-tooltip>
                      </q-btn>

                      <q-btn v-if="scope.uploadedFiles.length > 0" icon="done_all" @click="scope.removeUploadedFiles" round dense flat >
                        <q-tooltip>Remove Uploaded Files</q-tooltip>
                      </q-btn>

                      <q-spinner v-if="scope.isUploading" class="q-uploader__spinner" />

                      <div class="col">
                        <div class="q-uploader__title">Only ZIP-archives (max 500Mb)</div>
                      </div>

                      <q-btn v-if="scope.uploadedFiles.length < 1" :disable="currentlyUploading" type="a" icon="add_box" @click="scope.pickFiles" round dense flat>
                        <q-uploader-add-trigger/>
                        <q-tooltip :disable="currentlyUploading || this.$q.platform.is.mobile" >Pick ZIP-archive</q-tooltip>
                      </q-btn>

                      <q-btn icon="help_outline" @click="zipInfo = true" round dense flat/>
                    </div>
                  </template>
                </q-uploader>
                <div class="text-center flex-center">
                  ZIP-archives must be full modpacks, with mods, configs etc. No single-folder-
                  archives with only 'overrides' or the like.
                </div>

                <q-separator
                color="transparent"/>
                <!-- CLIENTSIDE MODS -->
                <div class="row flex-center">
                  <q-input
                    class="full-width"
                    ref="clientMods"
                    color="black"
                    type="textarea"
                    filled
                    v-model="clientsideMods"
                    label="Clientside-only Mods"
                  >
                    <q-tooltip :disable="this.$q.platform.is.mobile">
                      Comma separated. Clientside-only mods which must not be in the server pack.<br>
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
                        align="center"
                        :label="minecraftVersion"
                        no-caps
                        size="100%">
                        <q-list>
                          <q-item v-for="version in minecraftVersions" v-bind:key="version" clickable v-close-popup @click="minecraftVersionSelected(version)">
                            <q-item-section>
                              {{version}}
                            </q-item-section>
                          </q-item>
                        </q-list>
                      </q-btn-dropdown>
                    </div>
                  </div>

                  <q-separator vertical size="15px" color="transparent"/>

                  <!-- MODLOADER -->
                  <div class="column flex-center">
                    <div class="row flex-center">
                      <b>Modloader:</b>
                    </div>
                    <div class="row flex-center">
                      <q-btn-dropdown
                        align="center"
                        :label="modLoader"
                        no-caps
                        size="100%">
                        <q-list>
                          <q-item v-for="loader in modloaders" v-bind:key="loader" clickable v-close-popup @click="modLoaderSelected(loader)">
                            <q-item-section>
                              {{loader}}
                            </q-item-section>
                          </q-item>
                        </q-list>
                      </q-btn-dropdown>
                    </div>
                  </div>

                  <q-separator vertical size="15px" color="transparent"/>

                  <!-- MODLOADER VERSION -->
                  <div class="column flex-center">
                    <div class="row flex-center">
                      <b>Modloader version:</b>
                    </div>
                    <div class="row flex-center">
                      <q-btn-dropdown
                        align="center"
                        :label="modloaderVersion"
                        no-caps
                        size="100%">
                        <q-list v-if="modLoader === modloaders[0]">
                          <q-item v-for="version in forgeVersions" v-bind:key="version" clickable v-close-popup @click="modloaderVersion = version">
                            <q-item-section>
                              {{version}}
                            </q-item-section>
                          </q-item>
                        </q-list>
                        <q-list v-else>
                          <q-item v-for="version in fabricVersions" v-bind:key="version" clickable v-close-popup @click="modloaderVersion = version">
                            <q-item-section>
                              {{version}}
                            </q-item-section>
                          </q-item>
                        </q-list>
                      </q-btn-dropdown>
                    </div>
                  </div>

                  <div class="column flex-center">
                    <q-toggle
                      v-model="installServer"
                      checked-icon="check"
                      unchecked-icon="clear"
                      :color="installServer ? 'positive' : 'negative'"
                      label="Install modloader server?"
                    />
                  </div>

                </div>

                <div :class="this.$q.platform.is.mobile ? 'row flex-center' : 'row no-wrap flex-center'">
                  <div>
                    <q-btn label="Submit" :disable="disableZip" :loading="loading" type="submit" color="primary" />
                    <q-btn label="Reset"                                        type="reset" color="warning" class="q-ml-sm" />
                  </div>
                </div>

              </q-form>
            </div>
          </q-tab-panel>
<!--

CREATE FROM MODRINTH MODPACK

-->
          <q-tab-panel name="modrinth">
            <div class="column">
              <div class="text-h6 q-mb-md text-center">Enter your CurseForge projectID and fileID</div>
              <q-form :disable="modrinth" @submit="submitModrinth()" @reset="resetForm" class="q-gutter-xs">

                <div class="row flex-center">
                  <q-input
                    :disable="modrinth"
                    ref="projectRef"
                    color="black"
                    filled
                    v-model.number="store.state.projectID"
                    label="CurseForge projectID"
                    type="number"
                    maxlength="10"
                    :rules="projectRules">
                    <template v-slot:append>
                      <q-icon name="info" @click="projectInfo = true" class="cursor-pointer" />
                    </template>
                    <q-tooltip :disable="this.$q.platform.is.mobile">
                      The projectID can be found in the top right of a modpacks CurseForge project page.
                    </q-tooltip>
                  </q-input>
                </div>

                <div class="row flex-center">
                  <q-input
                    :disable="modrinth"
                    ref="fileRef"
                    color="black"
                    filled
                    v-model.number="store.state.fileID"
                    label="CurseForge fileID"
                    type="number"
                    maxlength="10"
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
                    <q-btn label="Submit" :disable="disable || modrinth" :loading="loading" type="submit" color="primary" />
                    <q-btn label="Reset"                                                    type="reset" color="warning" class="q-ml-sm" />
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
  <q-dialog style="max-width: 1000px;width:750px" v-model="zipInfo">
    <q-card class="full-width">
      <q-card-section>
        <div class="text-h6">ZIP requirements in detail:</div>
      </q-card-section>

      <q-card-section class="q-pt-none text-center flex-center">
          <q-carousel
            animated
            v-model="zipSlide"
            :navigation="autoplay"
            infinite
            swipeable
            :autoplay="autoplay"
            :arrows="autoplay"
            transition-prev="slide-right"
            transition-next="slide-left"
            @mouseenter="autoplay = false"
            @mouseleave="autoplay = true"
            control-color="accent"
          >
            <q-carousel-slide :name="1" img-src="~assets/invalid_minecraft.webp"/>
            <q-carousel-slide :name="2" img-src="~assets/invalid_overrides.webp"/>
            <q-carousel-slide :name="3" img-src="~assets/valid1.webp"/>
            <q-carousel-slide :name="4" img-src="~assets/valid2.webp"/>
          </q-carousel>
        <q-chip outline>
          {{zipSlideInfo[zipSlide-1]}}
        </q-chip>

      </q-card-section>

      <q-card-section>
        <div>
          All modpack contents must be in the root of the ZIP-archive. Having either of the following in your modpack will
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
        <q-btn flat label="OK" color="primary" v-close-popup />
      </q-card-actions>
    </q-card>
  </q-dialog>
<!--

 CURSEFORGE HELP DIALOGS

-->
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
      <q-card-section class="flex-center">
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
      <q-card-section class="flex-center">
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
import { useQuasar, Cookies, openURL  } from 'quasar';
import { api } from "boot/axios";

export default defineComponent({
  name: "SubmitRequest",
  setup() {

    const store = inject('store');
    const projectRef = ref(false);
    const fileRef = ref(false);
    const regenerationActivated = ref(false);

    const onRejectedCurse = function(rejectedEntries) {
      $q.notify({
        type: 'negative',
        message: `${rejectedEntries[0].file.name}-file did not pass validation constraints. The file is either too big or not a valid ZIP-archive.`
      });
    };

    return {
      /*
       * GENERAL
       */
      store,
      loading: ref(false),
      tab: ref('zip'),
      installServer: ref(true),
      /*
       * CURSE
       */
      projectRef,
      fileRef,
      regenerationActivated,
      disableCurse: ref(false),
      curse: ref(true),
      projectInfo: ref(false),
      fileInfo: ref(false),
      projectRules: [
        projectID  => store.state.projectID >= 10 || 'ProjectID should not be smaller than 10!',
        projectID  => ( store.state.projectID !== null && store.state.projectID !== '' ) || 'Please enter a projectID!'
      ],
      fileRules: [
        fileID  => store.state.fileID >= 60018 || 'FileID should not be smaller than 60018!',
        projectID  => ( store.state.fileID !== null && store.state.fileID !== '' ) || 'Please enter a fileID!'
      ],
      onRejectedCurse,
      /*
       * ZIP
       */
      minecraftVersion: ref("1.18.1"),
      minecraftVersions: ref([]),
      modLoader: ref("Forge"),
      modloaders: [
        "Forge",
        "Fabric"
      ],
      clientsideMods: ref([]),
      clientsideModsDefault: ref([]),
      forgeVersions: ref([]),
      fabricVersions: ref([]),
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
      /*
       * MODRINTH
       */
      modrinth: ref(true),
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
     * Downloads a server pack with a given CurseForge project and fileID combination.
     * @author Griefed
     * @param project The CurseForge projectID
     * @param file The CurseForge fileID
     */
    download(project, file) {
      api.get("/packs/specific/" + project + ',' + file)
        .then(response => {
          openURL(
            '/api/packs/download/' + response.data.id,
            null,
            {
              windowName: window.name,
              noopener: true,
              menubar: false,
              toolbar: false,
              noreferrer: true,
            }
          )
        })
        .catch(error => {

          console.log('An error occurred trying to download the server pack for: ' + project + ".");
          console.log(error);

          this.$q.notify({
            timeout: 5000,
            progress: true,
            icon: 'error',
            color: 'negative',
            message: 'An error occurred trying to download the server pack for: ' + project + ". " + error
          })

        })
    },
    /**
     * Sets cookies for a CurseForge project and fileID if the submitted request was valid.
     * @author Griefed
     * @param project The CurseForge projectID
     * @param file The CurseForge fileID
     */
    setProjectCookies(project, file) {
      this.$q.cookies.set('projectID', project);
      this.$q.cookies.set('fileID', file);
    },
    /**
     * Reset the submission form.
     * @author Griefed
     */
    resetForm() {
      this.store.state.projectID = 10;
      this.store.state.fileID = 60018;
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
 *  CURSEFORGE
 *
 */
    /**
     * Submit a CurseForge project and fileID combination for generation. If the combination is invalid, a notification is
     * shown to the user, telling them what went wrong. If the requested combination already exists, a notification is shown
     * prompting the user to either regenerate the server pack (if regeneration is enabled on the affected instance), or
     * download the server pack.
     * @author Griefed
     * @param project The CurseForge projectID
     * @param file The CurseForge fileID
     */
    submitCurse(project, file) {
      this.loading = true;
      this.disable = true;
      this.$q.loadingBar.start();
      console.log(project + "," + file);
      api.get("/curse/task?modpack=" + project + "," + file)
          .then(response => {
            this.curseNotify(response.data.status, response.data.timeout, response.data.icon, response.data.colour, response.data.message, project, file);
            this.enableCurseButtons();
            if (response.data.status === 0 || response.data.status === 1) {
              this.setProjectCookies(project, file);
            }
          })
          .catch(error =>{
            console.log("An error occurred trying to contact the backend with the requested project.");
            console.log(error);
            this.errorNotification(error);
            this.enableCurseButtons();
      });
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
    curseNotify(status, timeout, icon, color, message, project, file) {
      if (status === 0 && this.regenerationActivated) {

        this.$q.notify({
          position: "center",
          timeout: timeout,
          progress: true,
          multiLine: true,
          icon: icon,
          color: color,
          message: message,
          actions: [
            {
              label: 'Regenerate', color: 'warning', handler: () => {
                this.regenerate(project, file);
              }
            },
            {
              label: 'Download', color: 'dark', handler: () => {
                this.download(project, file);
              }
            },
            {
              label: 'OK', color: 'dark'
            }
          ]
        })

      } else if (status === 0 && !this.regenerationActivated) {

        this.$q.notify({
          position: "center",
          timeout: timeout,
          progress: true,
          multiLine: true,
          icon: icon,
          color: color,
          message: message,
          actions: [
            {
              label: 'Download', color: 'white', handler: () => {
                this.download(project, file);
              }
            },
            {
              label: 'OK', color: 'dark'
            }
          ]
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
    /**
     * Disable buttons for submitting a new request.
     * @author Griefed
     */
    enableCurseButtons() {
      this.loading = false;
      this.disableCurse = false;
    },
    /**
     * Submit a request for regenerating a given CurseForge project and fileID combination.
     * @author Griefed
     * @param project The CurseForge projectID
     * @param file The CurseForge fileID
     */
    regenerate(project, file) {
      this.$q.loadingBar.start();
      api.get("/curse/regenerate?modpack=" + project + "," + file)
        .then(response => {
          this.curseNotify(response.data.status, response.data.timeout, response.data.icon, response.data.colour, response.data.message, project, file);
        })
        .catch(error =>{

          console.log("An error occurred trying to contact the backend with the requested project.");
          console.log(error);

          this.errorNotification(error);

          this.$q.loadingBar.stop();

        });
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
          this.modloaderVersion,
          this.installServer
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

      api.get("/versions/forge/" + version)
        .then(response => {
          this.forgeVersions = response.data.forge;

          if (this.modLoader === this.modloaders[0]) {

            if (this.forgeVersions.length === 0) {
              console.log('Zero')
              this.modloaderVersion = 'None';
            } else {
              this.modloaderVersion = this.forgeVersions[0];
            }

          } else {
            this.modloaderVersion = this.fabricVersions[0];
          }

          this.disableZip = this.modloaderVersion === 'None' || this.zipName === "";

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
      //Forge
      if (loader === this.modloaders[0]) {

        this.modLoader = 'Forge';

        api.get("/versions/forge/" + this.minecraftVersion)
          .then(response => {

            this.forgeVersions = response.data.forge;

            if (this.forgeVersions.length === 0) {
              console.log('Zero')
              this.modloaderVersion = 'None';
            } else {
              this.modloaderVersion = this.forgeVersions[0];
            }

            this.disableZip = this.modloaderVersion === 'None' || this.zipName === "";

          })
          .catch(error => {
            console.log(error);
            this.errorNotification(error);
          });

      // Fabric
      } else {
        this.modLoader = 'Fabric';
        this.modloaderVersion = this.fabricVersions[0];

        if (this.modloaderVersion !== 'None' && this.zipName !== "") {
          this.disableZip = false;
        }
      }
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
    },
/*
 *
 *  MODRINTH
 *
 */
    submitModrinth() {

    }
  },
  mounted() {
    /*
     * Acquire some settings from our backend.
     *  - Is CurseForge regeneration of already available server packs enabled?
     *  - Acquire the configured list of clientside-only mods.
     *  - Acquire the fallback list of clientside-only mods.
     */
    api.get("/settings")
      .then(response => {
        this.regenerationActivated = response.data.curseControllerRegenerationEnabled;
        this.clientsideMods = response.data.listFallbackMods;
        this.clientsideModsDefault = response.data.listFallbackMods;
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
      .then(response => {
        this.minecraftVersions = response.data.minecraft;
        this.minecraftVersion = this.minecraftVersions[0];
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
     * Acquire a list of available Forge versions for a given Minecraft version from our backend.
     * We do not get this list from Forge directly because we need to make sure we only submit request to the backend
     * with versions that are available to the backend, too.
     */
    api.get("/versions/forge/" + this.minecraftVersion)
      .then(response => {
        this.forgeVersions = response.data.forge;
        this.modloaderVersion = this.forgeVersions[0];
      })
      .catch(error => {
        console.log(error);
        this.errorNotification(error);
      });
    /*
     * Check cookies whether the user has previously generated a server pack from a CurseForge projectID,fileID and set
     * the GUI accordingly.
     */
    if (this.$q.cookies.has('projectID')) this.store.state.projectID = this.$q.cookies.get('projectID');
    if (this.$q.cookies.has('fileID')) this.store.state.fileID = this.$q.cookies.get('fileID');
  }
})
</script>

<style>

.intersection {
  height: 100%;
  width: 100%;
}
</style>
