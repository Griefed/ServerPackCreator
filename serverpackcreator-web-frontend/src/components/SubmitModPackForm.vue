<template>
  <q-tabs v-model="tab" dense active-color="primary" indicator-color="secondary" narrow-indicator>
    <q-tab name="upload" label="Upload"/>
    <q-tab name="regeneration" label="Regeneration"/>
  </q-tabs>

  <q-separator/>

  <q-tab-panels v-model="tab">
    <q-tab-panel name="upload">
      <q-form @submit="onSubmit" ref="upload">
        <q-item>
          <q-item-section style="margin-bottom: 20px;">
            <q-btn-dropdown label="Minecraft Version">
              <q-list>
                <q-item v-for="version in minecraftVersions" v-bind:key="version" v-close-popup clickable
                        @click="selectedMinecraft(version)">
                  <q-item-section>
                    {{ version }}
                  </q-item-section>
                </q-item>
              </q-list>
            </q-btn-dropdown>
          </q-item-section>

          <q-item-section>
            <q-input
              label-color="accent"
              name="minecraftVersion"
              style="margin-right: 10px;"
              dense rounded outlined readonly
              v-model="minecraftVersion"
              label="Minecraft Version"
              hint="The Minecraft version your modpack uses."
            >
              <template v-slot:append>
                <q-icon name="pin" color="accent"/>
              </template>
            </q-input>
          </q-item-section>
        </q-item>

        <q-item>
          <q-item-section style="margin-bottom: 20px;">
            <q-btn-dropdown label="Modloader">
              <q-list>
                <q-item v-for="loader in modloaders" v-bind:key="loader" v-close-popup clickable
                        @click="modloaderSelected(loader)">
                  <q-item-section>
                    {{ loader }}
                  </q-item-section>
                </q-item>
              </q-list>
            </q-btn-dropdown>
          </q-item-section>
          <q-item-section>
            <q-input
              label-color="accent"
              name="modloader"
              style="margin-right: 10px;"
              dense rounded outlined readonly
              v-model="modloader"
              label="Modloader"
              hint="The modlaoder your modpack uses."
            >
              <template v-slot:append>
                <q-icon name="pin" color="accent"/>
              </template>
            </q-input>
          </q-item-section>
        </q-item>

        <q-item>
          <q-item-section style="margin-bottom: 20px;">
            <q-btn-dropdown label="Modloader Version">
              <q-list>
                <q-item v-for="version in modloaderVersions" v-bind:key="version" v-close-popup clickable
                        @click="setModloaderVersion(version)">
                  <q-item-section>
                    {{ version }}
                  </q-item-section>
                </q-item>
              </q-list>
            </q-btn-dropdown>
          </q-item-section>
          <q-item-section>
            <q-input
              label-color="accent"
              name="modloaderVersion"
              style="margin-right: 10px;"
              dense rounded outlined readonly
              v-model="modloaderVersion"
              label="Modloader Version"
              hint="The modloader version your modpack uses."
            >
              <template v-slot:append>
                <q-icon name="pin" color="accent"/>
              </template>
            </q-input>
          </q-item-section>
        </q-item>

        <q-separator inset spaced/>

        <q-expansion-item expand-separator label="Customizations" icon="tune">
          <q-item>
            <q-item-section>
              <q-input
                label-color="accent"
                name="startArgs"
                style="margin-right: 10px;"
                filled dense
                type="textarea"
                v-model="startArgs"
                label="Start Arguments"
                hint="JVM flags and start arguments for your server."
              />
            </q-item-section>
          </q-item>

          <q-separator inset spaced/>

          <q-item>
            <q-item-section>
              <q-input
                label-color="accent"
                name="clientMods"
                style="margin-right: 10px;"
                filled dense
                type="textarea"
                v-model="clientMods"
                label="Clientside Mods"
                hint="Clientside mods to exclude from your server pack."
              />
            </q-item-section>
          </q-item>

          <q-separator inset spaced/>

          <q-item>
            <q-item-section>
              <q-input
                label-color="accent"
                name="whiteListMods"
                style="margin-right: 10px;"
                filled dense
                type="textarea"
                v-model="whiteListMods"
                label="Whitelisted Mods"
                hint="Mods to ensure to NOT exclude from your server pack."
              />
            </q-item-section>
          </q-item>
        </q-expansion-item>

        <q-separator inset spaced/>

        <q-item>
          <q-item-section>
            <q-file label-color="accent" filled bottom-slots v-model="file" label="Modpack ZIP" counter
                    @rejected="onRejected" accept=".zip"
                    style="margin-right: 10px;" name="file">
              <template v-slot:file="{ file }">
                <q-chip
                  class="full-width q-my-xs"
                  square
                >
                  <q-linear-progress v-if="progress > 0" :stripe="progress === 1" class="absolute-full full-height" :value="progress"
                                     color="positive" track-color="primary" instant-feedback>
                    <div class="absolute-full flex-center" style="display: flex; justify-content: flex-end; margin-right: 10px;">
                      <q-badge v-if="progress < 1" color="white" text-color="accent" :label="'Uploaded: ' + Math.round(progress * 100) + '%'"/>
                      <q-badge v-else color="white" text-color="accent" label="Upload done. Checking..."/>
                    </div>
                  </q-linear-progress>

                  <div class="ellipsis relative-position">
                    {{ file.name }}
                  </div>

                  <q-tooltip>
                    {{ file.name }}
                  </q-tooltip>
                </q-chip>
              </template>
              <template v-slot:prepend>
                <q-icon name="folder_zip" @click.stop.prevent/>
              </template>
              <template v-slot:append>
                <q-icon v-if="!uploading" name="close" @click.stop.prevent="file = null" class="cursor-pointer"/>
                <q-btn dense flat icon="help_outline" round @click="zipInfo = true"/>
              </template>

              <template v-slot:hint>
                ZIP-archive of your modpack.
              </template>
            </q-file>
          </q-item-section>
        </q-item>

        <q-item>
          <q-item-section>
            <q-btn label="Upload & Submit" type="submit" color="primary" :disable="uploading" :loading="uploading"/>
          </q-item-section>
        </q-item>
      </q-form>
    </q-tab-panel>

    <q-tab-panel name="regeneration">
      <q-form @submit="onSubmitRegeneration" ref="regeneration">
        <q-item>
          <q-item-section style="margin-bottom: 20px;">
            <q-btn-dropdown label="ModPack ID" @before-show="refreshModPackIDs">
              <q-list>
                <q-item v-for="id in modPackIDs" v-bind:key="id" v-close-popup clickable
                        @click="selectedModPack(id)">
                  <q-item-section>
                    {{ id }}
                    <q-tooltip anchor="bottom right" self="top middle">
                      <div class="row" v-if="this.modPacks[id].projectID.length > 0">
                        Project ID: {{ this.modPacks[id].projectID }}
                      </div>
                      <div class="row" v-if="this.modPacks[id].versionID.length > 0">
                        Version ID: {{ this.modPacks[id].versionID }}
                      </div>
                      <div class="row">
                        Date Created: {{ date.formatDate(this.modPacks[id].dateCreated, 'YYYY-MM-DD : HH:mm') }}
                      </div>
                      <div class="row">
                        Name: {{ this.modPacks[id].name }}
                      </div>
                      <div class="row">
                        Size: {{ this.modPacks[id].size }}
                      </div>
                      <div class="row">
                        Status: {{ this.modPacks[id].status }}
                      </div>
                      <div class="row">
                        Source: {{ this.modPacks[id].source }}
                      </div>
                      <div class="row">
                        SHA256 Hash: {{ this.modPacks[id].sha256 }}
                      </div>
                      <div class="row">
                        Server Packs: {{ this.modPacks[id].serverPacks.length }}
                      </div>
                    </q-tooltip>
                  </q-item-section>
                </q-item>
              </q-list>
            </q-btn-dropdown>
          </q-item-section>

          <q-item-section>
            <q-input
              label-color="accent"
              name="modPackID"
              style="margin-right: 10px;"
              dense rounded outlined readonly
              v-model="modPackID"
              label="ModPack ID"
              hint="ID of an already available ModPack"
            >
              <template v-slot:append>
                <q-icon name="token" color="accent"/>
              </template>
            </q-input>
          </q-item-section>
        </q-item>

        <q-item>
          <q-item-section style="margin-bottom: 20px;">
            <q-btn-dropdown label="RunConfiguration ID" @before-show="refreshRunConfigurationIDs">
              <q-list>
                <q-item v-for="id in runConfigurationIDs" v-bind:key="id" v-close-popup clickable
                        @click="selectedRunConfiguration(id)">
                  <q-item-section>
                    {{ id }}
                    <q-tooltip anchor="bottom right" self="top middle">
                      <div class="row">
                        Minecraft Version: {{ this.runConfigurations[id].minecraftVersion }}
                      </div>
                      <div class="row">
                        Modloader: {{ this.runConfigurations[id].modloader }}
                      </div>
                      <div class="row">
                        Modloader Version: {{ this.runConfigurations[id].modloaderVersion }}
                      </div>
                      <div class="row">
                        Start Args: {{ this.runConfigurations[id].startArgs.map((arg) => arg.argument).join(', ') }}
                      </div>
                      <div class="row">
                        Client Mods: {{ this.runConfigurations[id].clientMods.map((mod) => mod.mod).join(', ') }}
                      </div>
                      <div class="row">
                        Whitelisted Mods: {{
                          this.runConfigurations[id].whitelistedMods.map((mod) => mod.mod).join(', ')
                        }}
                      </div>
                    </q-tooltip>
                  </q-item-section>
                </q-item>
              </q-list>
            </q-btn-dropdown>
          </q-item-section>

          <q-item-section>
            <q-input
              label-color="accent"
              style="margin-right: 10px;"
              dense rounded outlined readonly
              v-model="runConfigID"
              label="RunConfiguration ID"
              hint="ID of an already available RunConfiguration"
            >
              <template v-slot:append>
                <q-icon name="token" color="accent"/>
              </template>
            </q-input>
          </q-item-section>
        </q-item>

        <q-item>
          <q-item-section style="margin-bottom: 20px;">
            <q-btn-dropdown label="Minecraft Version">
              <q-list>
                <q-item v-for="version in minecraftVersions" v-bind:key="version" v-close-popup clickable
                        @click="selectedMinecraft(version)">
                  <q-item-section>
                    {{ version }}
                  </q-item-section>
                </q-item>
              </q-list>
            </q-btn-dropdown>
          </q-item-section>

          <q-item-section>
            <q-input
              label-color="accent"
              name="minecraftVersion"
              style="margin-right: 10px;"
              dense rounded outlined readonly
              v-model="minecraftVersion"
              label="Minecraft Version"
              hint="The Minecraft version your modpack uses."
            >
              <template v-slot:append>
                <q-icon name="pin" color="accent"/>
              </template>
            </q-input>
          </q-item-section>
        </q-item>

        <q-item>
          <q-item-section style="margin-bottom: 20px;">
            <q-btn-dropdown label="Modloader">
              <q-list>
                <q-item v-for="loader in modloaders" v-bind:key="loader" v-close-popup clickable
                        @click="modloaderSelected(loader)">
                  <q-item-section>
                    {{ loader }}
                  </q-item-section>
                </q-item>
              </q-list>
            </q-btn-dropdown>
          </q-item-section>
          <q-item-section>
            <q-input
              label-color="accent"
              name="modloader"
              style="margin-right: 10px;"
              dense rounded outlined readonly
              v-model="modloader"
              label="Modloader"
              hint="The modlaoder your modpack uses."
            >
              <template v-slot:append>
                <q-icon name="pin" color="accent"/>
              </template>
            </q-input>
          </q-item-section>
        </q-item>

        <q-item>
          <q-item-section style="margin-bottom: 20px;">
            <q-btn-dropdown label="Modloader Version">
              <q-list>
                <q-item v-for="version in modloaderVersions" v-bind:key="version" v-close-popup clickable
                        @click="setModloaderVersion(version)">
                  <q-item-section>
                    {{ version }}
                  </q-item-section>
                </q-item>
              </q-list>
            </q-btn-dropdown>
          </q-item-section>
          <q-item-section>
            <q-input
              label-color="accent"
              name="modloaderVersion"
              style="margin-right: 10px;"
              dense rounded outlined readonly
              v-model="modloaderVersion"
              label="Modloader Version"
              hint="The modloader version your modpack uses."
            >
              <template v-slot:append>
                <q-icon name="pin" color="accent"/>
              </template>
            </q-input>
          </q-item-section>
        </q-item>

        <q-separator inset spaced/>

        <q-expansion-item expand-separator label="Customizations" icon="tune">
          <q-item>
            <q-item-section>
              <q-input
                label-color="accent"
                name="startArgs"
                style="margin-right: 10px;"
                filled dense
                type="textarea"
                v-model="startArgs"
                label="Start Arguments"
                hint="JVM flags and start arguments for your server."
              />
            </q-item-section>
          </q-item>

          <q-separator inset spaced/>

          <q-item>
            <q-item-section>
              <q-input
                label-color="accent"
                name="clientMods"
                style="margin-right: 10px;"
                filled dense
                type="textarea"
                v-model="clientMods"
                label="Clientside Mods"
                hint="Clientside mods to exclude from your server pack."
              />
            </q-item-section>
          </q-item>

          <q-separator inset spaced/>

          <q-item>
            <q-item-section>
              <q-input
                label-color="accent"
                name="whiteListMods"
                style="margin-right: 10px;"
                filled dense
                type="textarea"
                v-model="whiteListMods"
                label="Whitelisted Mods"
                hint="Mods to ensure to NOT exclude from your server pack."
              />
            </q-item-section>
          </q-item>
        </q-expansion-item>

        <q-separator inset spaced/>

        <q-item>
          <q-item-section>
            <q-btn label="Submit" type="submit" color="primary"/>
          </q-item-section>
        </q-item>
      </q-form>
    </q-tab-panel>
  </q-tab-panels>

  <q-dialog v-model="zipInfo">
    <q-card flat bordered style="width: 50vw; max-width: 50vw;">
      <q-item>
        <q-item-section>
          <q-item-label>ZIP requirements in detail:</q-item-label>
          <q-item-label caption>
            Short overview on the general structure of accepted, zipped, modpacks.
          </q-item-label>
        </q-item-section>
      </q-item>

      <q-separator/>

      <q-card-section horizontal>
        <q-card-section class="col">
          <q-carousel
            v-model="zipSlide"
            :arrows="zipAutoplay"
            :autoplay="zipAutoplay"
            :navigation="zipAutoplay"
            animated infinite swipeable padding
            control-color="accent"
            transition-next="slide-left"
            transition-prev="slide-right"
            @mouseenter="zipAutoplay = false"
            @mouseleave="zipAutoplay = true"

          >
            <q-carousel-slide :name="1" class="rounded-borders" img-src="~assets/invalid_minecraft.webp"/>
            <q-carousel-slide :name="2" class="rounded-borders" img-src="~assets/invalid_overrides.webp"/>
            <q-carousel-slide :name="3" class="rounded-borders" img-src="~assets/valid1.webp"/>
            <q-carousel-slide :name="4" class="rounded-borders" img-src="~assets/valid2.webp"/>
          </q-carousel>
          <q-chip outline>
            {{ zipSlideInfo[zipSlide - 1] }}
          </q-chip>
        </q-card-section>

        <q-separator vertical/>

        <q-card-section class="col-5">
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
      </q-card-section>

      <q-card-actions align="right">
        <q-btn v-close-popup color="primary" flat label="OK"/>
      </q-card-actions>
    </q-card>

  </q-dialog>
</template>

<script >
import {defineComponent, ref} from 'vue';
import {modpacks, runConfigs, versions} from 'boot/axios';
import {settingsStore} from 'stores/setting-store';
import {date} from 'quasar';

export default defineComponent({
  name: 'SubmitModPackForm',
  computed: {
    date() {
      return date
    }
  },
  setup() {
    const store = settingsStore();
    store.refresh();
    const modloaders = ref([]);
    const modloader = ref('');
    const file = ref(null);
    const minecraftVersion = ref('');
    const modloaderVersion = ref('');
    const startArgs = ref('');
    const clientMods = ref('');
    const whiteListMods = ref('');
    const minecraftVersions = ref([]);
    const fabricVersions = ref([]);
    const legacyFabricVersions = ref([]);
    const quiltVersions = ref([]);
    const forgeVersions = ref(new Map);
    const neoForgeVersions = ref(new Map);
    const modloaderVersions = ref([]);
    const submitted = ref(false);
    const submitEmpty = ref(false);
    const submitResult = ref([]);
    const progress = ref(0);
    const uploading = ref(false);
    const modPackID = ref(0);
    const runConfigID = ref(0);
    const tab = ref('upload');
    const modPackIDs = ref([]);
    const runConfigurationIDs = ref([]);
    const modPacks = ref(new Map);
    const runConfigurations = ref(new Map);
    const zipInfo = ref(false);
    const zipSlide = ref(1);
    const zipAutoplay = ref(true);
    const zipSlideInfo = ['Not accepted: Only minecraft', 'Not accepted: Only overrides', 'Accepted.', 'Accepted.'];
    return {
      store,
      file,
      minecraftVersion,
      modloader,
      modloaderVersion,
      startArgs,
      clientMods,
      whiteListMods,
      minecraftVersions,
      modloaders,
      fabricVersions,
      legacyFabricVersions,
      quiltVersions,
      forgeVersions,
      neoForgeVersions,
      modloaderVersions,
      submitted,
      submitEmpty,
      submitResult,
      progress,
      uploading,
      modPackID,
      runConfigID,
      tab,
      modPackIDs,
      runConfigurationIDs,
      modPacks,
      runConfigurations,
      zipInfo,
      zipSlide,
      zipAutoplay,
      zipSlideInfo
    };
  },
  methods: {
    refreshModPackIDs() {
      modpacks.get('all').then(response => {
        this.modPackIDs = response.data.map((modpack) => modpack.id);
        response.data.forEach(modpack => this.modPacks[modpack.id] = modpack);
      }).catch(error => {
        this.$q.notify({
          timeout: 5000,
          progress: true,
          icon: 'error',
          color: 'negative',
          message: 'Request failed: ' + error
        });
      })
    },
    selectedModPack(id) {
      this.modPackID = id;
    },
    refreshRunConfigurationIDs() {
      runConfigs.get('all').then(response => {
        this.runConfigurationIDs = response.data.map(runconfig => runconfig.id);
        response.data.forEach(runConfig => this.runConfigurations[runConfig.id] = runConfig);
      }).catch(error => {
        this.$q.notify({
          timeout: 5000,
          progress: true,
          icon: 'error',
          color: 'negative',
          message: 'Request failed: ' + error
        });
      })
    },
    selectedRunConfiguration(id) {
      this.runConfigID = id;
      let config = this.runConfigurations[id];
      this.minecraftVersion = config.minecraftVersion;
      this.modloader = config.modloader;
      this.modloaderVersion = config.modloaderVersion;
      this.startArgs = config.startArgs.map(arg => arg.argument).join(', ');
      this.whiteListMods = config.whitelistedMods.map(mod => mod.mod).join(', ');
      this.clientMods = config.clientMods.map(mod => mod.mod).join(', ');
    },
    onSubmitRegeneration(evt) {
      const formData = new FormData(evt.target);
      this.uploading = true;

      modpacks.postForm('generate', formData)
        .then(response => {
          this.$q.notify({
            timeout: 5000,
            progress: true,
            position: 'center',
            icon: 'check',
            color: 'positive',
            message: 'Generation queued for ModPack ID: ' + response.data.modPackId + '. RunConfiguration ID: ' + response.data.runConfigId
          });
          this.modPackID = response.data.modPackID;
          this.runConfigID = response.data.runConfigID;
          this.resetForm();
        }).catch(error => {
        this.$q.notify({
          timeout: 5000,
          progress: true,
          position: 'center',
          icon: 'error',
          color: 'negative',
          message: 'Request failed: ' + error
        });
        this.modPackID = error.data.modPackID;
        this.runConfigID = error.data.runConfigID;
        this.resetForm();
      })
    },
    onSubmit(evt) {
      const formData = new FormData(evt.target);
      this.uploading = true;

      modpacks.postForm('upload', formData, {
        onUploadProgress: progressEvent => {
          this.progress = progressEvent.loaded / (progressEvent.total ? progressEvent.total : progressEvent.loaded)
        }
      }).then(response => {
        this.$q.notify({
          timeout: 5000,
          progress: true,
          position: 'center',
          icon: 'check',
          color: 'positive',
          message: response.data.message + '  ModPack ID: ' + response.data.modPackId + '. RunConfiguration ID: ' + response.data.runConfigId
        });
        this.progress = 0;
        this.resetForm();
        this.delayedRegenPrep(response.data.modPackId, response.data.runConfigId);
      }).catch(error => {
        let message = (error.response.data.message === undefined ||
            error.response.data.message === null) ? error.message : error.response.data.message;
        let suffix = (error.response.data.modPackId === undefined ||
            error.response.data.modPackId === null) ? '' : ' See Modpack ID: ' + error.response.data.modPackId;
        this.$q.notify({
          timeout: 10000,
          progress: true,
          position: 'center',
          icon: 'error',
          color: 'negative',
          message: 'Upload failed: ' + message + suffix
        });
        this.progress = 0;
        this.resetForm();
        if (error.response.data.modPackId !== undefined &&
            error.response.data.runConfigId !== undefined &&
            error.response.data.modPackId !== null &&
            error.response.data.runConfigId !== null) {
          this.delayedRegenPrep(error.response.data.modPackId, error.response.data.runConfigId);
          this.tab = 'regeneration';
        }
      })
    },
    delayedRegenPrep(modPackId, runConfigId) {
      this.refreshModPackIDs();
      this.refreshRunConfigurationIDs();
      this.sleep(2000).then(() => {
        this.selectedModPack(modPackId);
        this.selectedRunConfiguration(runConfigId);
      })
    },
    resetForm() {
      this.file = null;
      this.uploading = false;
    },
    onRejected(rejectedEntry) {
      this.$q.notify({
        type: 'negative',
        position: 'center',
        message: `${rejectedEntry.name} is not a ZIP-file`
      });
    },
    setModloaderVersion(version) {
      this.modloaderVersion = version;
    },
    selectedMinecraft(version) {
      this.minecraftVersion = version;
      this.updateForgeVersions();
      this.updateNeoForgeVersions();
    },
    updateForgeVersions() {
      if (this.modloader === 'Forge') {
        if (this.forgeVersions[this.minecraftVersion] === undefined) {
          this.modloaderVersions = [];
          this.modloaderVersion = 'N/A';
        } else {
          this.modloaderVersions = this.forgeVersions[this.minecraftVersion];
          this.modloaderVersion = this.modloaderVersions[0];
        }
      }
    },
    updateNeoForgeVersions() {
      if (this.modloader === 'NeoForge') {
        if (this.neoForgeVersions[this.minecraftVersion] === undefined) {
          this.modloaderVersions = [];
          this.modloaderVersion = 'N/A';
        } else {
          this.modloaderVersions = this.neoForgeVersions[this.minecraftVersion];
          this.modloaderVersion = this.modloaderVersions[0];
        }
      }
    },
    modloaderSelected(loader) {
      this.modloader = loader;
      switch (this.modloader) {
        case 'Forge':
          this.updateForgeVersions();
          break;

        case 'Fabric':
          this.modloaderVersions = this.fabricVersions;
          this.modloaderVersion = this.modloaderVersions[0];
          break;

        case 'Quilt':
          this.modloaderVersions = this.quiltVersions;
          this.modloaderVersion = this.modloaderVersions[0];
          break;

        case 'LegacyFabric':
          this.modloaderVersions = this.legacyFabricVersions;
          this.modloaderVersion = this.modloaderVersions[0];
          break;

        case 'NeoForge':
          this.updateNeoForgeVersions();
          break;
      }
    },
    sleep(ms) {
      return new Promise(resolve => setTimeout(resolve, ms));
    }
  },
  mounted() {
    this.refreshModPackIDs()
    this.refreshRunConfigurationIDs()
    versions.get('all').then(response => {
      this.minecraftVersions = response.data.minecraft;
      this.fabricVersions = response.data.fabric;
      this.legacyFabricVersions = response.data.legacyFabric;
      this.quiltVersions = response.data.quilt;
      this.forgeVersions = response.data.forge;
      this.neoForgeVersions = response.data.neoForge;
      this.modloaders = this.store.supportedModloaders;
      this.minecraftVersion = this.minecraftVersions[0];
      this.modloader = this.store.supportedModloaders[0];
      this.clientMods = this.store.clientsideMods.join(', ');
      this.whiteListMods = this.store.whitelistMods.join(', ');
      this.startArgs = this.store.aikarsFlags;
      this.modloaderSelected(this.modloader);
    }).catch(error => {
      this.$q.notify({
        timeout: 5000,
        progress: true,
        position: 'center',
        icon: 'error',
        color: 'negative',
        message: 'Could not retrieve versions: ' + error
      });
    });
  }
});
</script>

<style>
</style>
