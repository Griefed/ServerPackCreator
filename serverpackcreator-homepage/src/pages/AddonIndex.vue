<template>
  <div class="addons">
    <div class="row q-pa-md flex-center window-width">
      <q-card :style="$q.dark.isActive ?
                'border-bottom: #C0FFEE 3px solid; max-width: 90%;' :
                'border-bottom: #325358 3px solid; max-width: 90%;'"
      >
        <q-list bordered class="rounded-borders">
          <q-item-label
            header
            :class="$q.platform.is.mobile ? 'text-weight-bolder text-h6' : 'text-weight-bolder text-h5'"
            :style="$q.platform.is.mobile ? 'font-size: 18px;' : ''"
          >
            Curated list of plugins for ServerPackCreator:

            <q-btn
              icon="info"
              class="q-mr-xs right"
              dense
              flat
              @click="viewInfo()">
            </q-btn>
          </q-item-label>

          <q-separator />

          <q-item v-for="addon in addons" v-bind:key="addon.name" :class=
            "$q.dark.isActive ?
              (addon.index % 2 ? 'item-dark' : 'item-dark-alternative') :
              (addon.index % 2 ? 'item' : 'item-alternative')"
          >
            <q-item-section top>

              <q-item-label lines="1">
                <span class="text-weight-bolder">[{{ addon.owner }}/{{ addon.name }}]</span>
                <span class="text-grey-8"> - {{ addon.type }}</span>
                <span class="text-grey-8"> - {{ addon.branch }}</span>
              </q-item-label>

              <q-item-label caption lines="10">
                {{ addon.description }}
              </q-item-label>

            </q-item-section>

            <q-item-section top side class="flex-center">
              <div class="q-gutter-xs">
                <q-btn size="lg" type="a" :href="addon.releasesLink" target="_blank" flat dense round icon="save" />
                <q-btn size="lg" type="a" :href="addon.repositoryLink" target="_blank" flat dense round
                       icon="open_in_new" />
              </div>
            </q-item-section>

            <q-separator />

          </q-item>

        </q-list>

      </q-card>
    </div>
  </div>
</template>

<script>
import { defineComponent } from "vue";

export default defineComponent({
  name: "AddonIndex",
  setup() {
    return {
      addons: {
        /*
        objectName: {
          index: last one +1,
          name: "Name of the plugin, usually repo-name",
          owner: "Repo-Author on GitHub",
          type: "GitHub,
          branch: "main or master",
          description: "Usually repo description, can be custom",
          releasesLink: "Link to GitHub-repo/releases/latest",
          repositoryLink: "Link To GitHub Repo"
        }
         */
        grinder: {
          index: 1,
          name: "serverpackcreator-plugin-grinder",
          owner: "Griefed",
          type: "GitHub",
          branch: "main",
          description: "Bridges ServerPackCreator to a grinder daemon: browse its clientside-mod verdicts in a GUI tab, tick the mods you want excluded, and have those ticks folded into the clientside-mod exclusion list of every server pack you generate. Experimental \u2014 it ships with each ServerPackCreator release as an _experimental asset, so its behaviour and configuration may still change between releases.",
          releasesLink: "https://github.com/Griefed/ServerPackCreator/releases/latest",
          repositoryLink: "https://github.com/Griefed/ServerPackCreator/tree/main/serverpackcreator-plugin-grinder"
        },
        selfextract: {
          index: 2,
          name: "serverpackcreator-plugin-selfextract",
          owner: "Griefed",
          type: "GitHub",
          branch: "main",
          description: "Wraps every generated server pack in two self-extracting archives \u2014 a .bsx for Linux and macOS, a .cmd for Windows 10 1803 and newer \u2014 so a finished pack can be handed over and unpacked as a single file. Experimental \u2014 it ships with each ServerPackCreator release as an _experimental asset, so its behaviour and configuration may still change between releases.",
          releasesLink: "https://github.com/Griefed/ServerPackCreator/releases/latest",
          repositoryLink: "https://github.com/Griefed/ServerPackCreator/tree/main/serverpackcreator-plugin-selfextract"
        },
        example: {
          index: 3,
          name: "serverpackcreator-plugin-example",
          owner: "Griefed",
          type: "GitHub",
          branch: "main",
          description: "Demonstrates every extension point ServerPackCreator offers to pf4j plugins \u2014 configuration checks, pre- and post-generation hooks, and GUI tabs and panels. The place to start when writing your own plugin.",
          releasesLink: "https://github.com/Griefed/ServerPackCreator/releases/latest",
          repositoryLink: "https://github.com/Griefed/ServerPackCreator/tree/main/serverpackcreator-plugin-example"
        },
        servertest: {
          index: 4,
          name: "serverpackcreator-plugin-servertest",
          owner: "Griefed",
          type: "GitHub",
          branch: "main",
          description: "Launches a generated server pack from inside ServerPackCreator, through the pack\u2019s own start scripts, and hands you that server\u2019s console together with a line into its standard input. Experimental \u2014 it ships with each ServerPackCreator release as an _experimental asset, so its behaviour and configuration may still change between releases.",
          releasesLink: "https://github.com/Griefed/ServerPackCreator/releases/latest",
          repositoryLink: "https://github.com/Griefed/ServerPackCreator/tree/main/serverpackcreator-plugin-servertest"
        }
      }
    };
  },
  methods: {
    viewInfo() {
      this.$q.notify({
        timeout: 0,
        icon: "info",
        color: "primary",
        position: "center",
        multiLine: true,
        html: true,
        message:
          "<span style=\"font-size: 20px;\"><b>Why:</b></span>" +
          "<br><br>" +
          "<p>" +
          "There are things which people want to do with their server packs which could most certainly be automated. Some of those" +
          "things so special, or maybe out of place, that they would not warrant a separate feature for ServerPackCreator itself." +
          "It may also be that it is such a niche feature, that I either don't have the time to code it in, or simply don't want to." +
          "Maybe it doesn't fit into the overall design of ServerPackCreator, too. Who knows, it could be any of those reasons or another." +
          "</p>" +

          "<p>" +
          "<b>Hence, the plugin functionality!</b>" +
          "</p>" +

          "<p>" +
          "This allows people to write their own plugins to expand the functionality of ServerPackCreator with their own features as" +
          "they see fit or want." +
          "</p>" +

          "<p>For documentation about Pf4j, visit the [Pf4j documentation](https://pf4j.org/)</p>" +

          "<span style=\"font-size: 20px;\"><b>Adding your own plugin:</b></span>" +
          "<br><br>" +
          "If you have written your own plugin for ServerPackCreator and you would like to see it added here, please open an issue over at ServerPackCreatoron GitHub, using the Documentation template." +
          "<br>" +
          "For a plugin to be accepted, you must at least provide:<br>" +
          "<ul>" +
          "<li>The name of the repository, and therefore the plugin.</li>" +
          "<li>The owner of the repository, and therefore the plugin.</li>" +
          "<li>The branch of the repository where the main code resides in.</li>" +
          "<li>A description of the plugin.</li>" +
          "</ul>" +

          "Some examples for potential plugins can be found <b><a style=\"color: #c0ffee\" href=\"https://github.com/Griefed/ServerPackCreator/discussions/201\">in this discussion thread</a></b>." +
          "<br>" +
          "Some excerpts:" +
          "<ul>" +
          "<li>1. Changelog generator, by @TheButterbrotMan at [Feature request]: Changelog generator #198</li>" +
          "<ul><li>A changelog generator that checks the differences to the previous version and generates a changelog.</li></ul>" +
          "<li>2. Bundle Adoptium Java with server packs, by @kreezxil at [Feature request]: Bundle Adoptium Java #199</li>" +
          "<ul><li>Because modpacks need one of the either Java 8, 16, or 17, it would be nice to have the corresponding <b><a style=\"color: #c0ffee\" href=\"https://adoptium.net\">Adoptium JDK/JRE</a></b> prebundled with the server pack.</li></ul>" +
          "<li>3. Automatic setup of a server pack for [BlueMap](https://www.curseforge.com/minecraft/mc-mods/bluemap)</li>" +
          "<ul><li>Check all mods in the specified modpacks mods-directory for textures, and if any are found, add the mod to" +
          "BlueMap's resourcepack folder `config/bluemap/resourcepacks`, install BlueMap for the specified Minecraft and Forge/Fabric" +
          "version and voilà!</li></ul>" +
          "</ul>",
        actions: [
          { label: "OK", color: "white" }
        ]
      });
    }
  }
});
</script>

<style>
.right {
  position: absolute;
  right: 0px;
  padding-right: 10px;
}

.item {
  background-color: whitesmoke;
}

.item-alternative {
  background-color: white;
}

.item-dark {
  background-color: black;
}

.item-dark-alternative {
  background-color: #1D1D1D;
}
</style>
