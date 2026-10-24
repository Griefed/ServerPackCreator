<template>
  <div class="index">
    <q-carousel
      animated
      v-model="slide"
      arrows
      swipeable
      navigation
      infinite
      autoplay
      control-color="white"
      transition-prev="jump-right"
      transition-next="jump-left"
      height="600px"
      style="background-color: #000000 !important;">
      <q-carousel-slide class="q-pa-none" style="word-wrap: break-word;display: block;"
                        :name="1" img-src="~@/assets/serverpackcreator/powershell.webp">
        <div class="full-height flex flex-center" style="background-color: rgba(0, 0, 0, 0.85) !important;">
          <div class="custom-caption">
            <div class="text-h2 main_line animation_2">
              SERVER PACKS <span class="text-orange-9">MADE EASY</span>
            </div>
            <br><br><br>
            <div class="text-h4 animation_1">
              Quick and easy generation of server packs<br>of your favourite Minecraft modpacks.
            </div>
          </div>
        </div>
      </q-carousel-slide>
      <q-carousel-slide class="q-pa-none" style="word-break: break-all;word-wrap: break-word;display: block;"
                        :name="2" img-src="~@/assets/serverpackcreator/linux.webp">
        <div class="full-height flex flex-center" style="background-color: rgba(0, 0, 0, 0.85) !important;">
          <div class="custom-caption">
            <div class="text-h2 main_line animation_2" style="font-size:65px;">
              POPULAR <span class="text-orange-9">MODLOADERS</span> SUPPORTED
            </div>
            <br><br><br>
            <div class="text-h4 animation_1">
              Forge, Fabric, Quilt and LegacyFabric<br>modloaders are supported.
            </div>
          </div>
        </div>
      </q-carousel-slide>
      <q-carousel-slide class="q-pa-none" style="word-break: break-all;word-wrap: break-word;display: block;"
                        :name="3" img-src="~@/assets/serverpackcreator/macos.webp">
        <div class="full-height flex flex-center" style="background-color: rgba(0, 0, 0, 0.85) !important;">
          <div class="custom-caption">
            <div class="text-h2 main_line animation_2" style="font-size:65px;">
              HIGHLY <span class="text-orange-9">CUSTOMIZABLE</span>
            </div>
            <br><br><br>
            <div class="text-h4 animation_1">
              Configure the most important aspects of a server pack<br>for your users convenience.
            </div>
          </div>
        </div>
      </q-carousel-slide>
    </q-carousel>
    <div class="row" style="background: #f7f7f7">
      <div class="col-md-3 col-lg-3 col-xs-12 col-sm-12">
        <q-card class="q-pa-sm flex flex-center text-center box-shadow" style="margin: 40px;">
          <q-card-section>
            <q-icon size="110px" name="ballot" :style="'color:'+primaryColour"/>
            <div class="text-h6">GUI</div>
          </q-card-section>

          <q-card-section class="q-pt-none">
            Use the graphical user interface for quick and easy configuration of your server pack.
          </q-card-section>
        </q-card>
      </div>
      <div class="col-md-3 col-lg-3 col-xs-12 col-sm-12">
        <q-card class="q-pa-sm flex flex-center text-center box-shadow" style="margin: 40px;">
          <q-card-section>
            <q-icon size="110px" name="terminal" :style="'color:'+primaryColour"/>
            <div class="text-h6">CLI</div>
          </q-card-section>

          <q-card-section class="q-pt-none">
            Generate server packs right on your headless system using the CLI mode.
          </q-card-section>
        </q-card>
      </div>
      <div class="col-md-3 col-lg-3 col-xs-12 col-sm-12">
        <q-card class="q-pa-sm flex flex-center text-center box-shadow" style="margin: 40px;">
          <q-card-section>
            <q-icon size="110px" name="web" :style="'color:'+primaryColour"/>
            <div class="text-h6">WEB</div>
          </q-card-section>

          <q-card-section class="q-pt-none">
            Upload your modpack and download an automatically generated server pack.
          </q-card-section>
        </q-card>
      </div>

      <div class="col-md-3 col-lg-3 col-xs-12 col-sm-12">
        <q-card class="q-pa-sm flex flex-center text-center box-shadow" style="margin: 40px;">
          <q-card-section>
            <q-icon size="110px" name="mdi-puzzle" :style="'color:'+primaryColour"/>
            <div class="text-h6">PLUGINS</div>
          </q-card-section>

          <q-card-section class="q-pt-none">
            Add features and functionality with third-party plugins, or write your own.
          </q-card-section>
        </q-card>
      </div>
    </div>
    <div class="q-pt-xl full-height flex flex-center" style="background-color: #00000088 !important;">
      <h3 class="text-white text-center"><strong>
        Working on an update to your modpack?<br>
        Want to play your favourite modpack with your friends?<br><br>
        Run <span class="text-orange-9">ServerPackCreator</span> and BAM!<br>
        You've got a server pack for your modpack.
        <br>
        <br>
        <q-btn type="a" href="https://github.com/Griefed/ServerPackCreator/releases/latest" size="lg"
               :style="'background:'+ primaryColour +'; color: white'" label="Get The Latest Version"/>
      </strong></h3>
    </div>
    <div style="background: #f7f7f7">
      <div class="row">
        <div class="col-md-12 col-lg-12 col-xs-12 col-sm-12">
          <h5 class="text-center text-grey-10"><strong>Features Excerpt</strong>
            <br>
            <span
              class="text-center text-blue-grey-10 text-h6">The most important features in a quick overview</span>
          </h5>
        </div>
      </div>
      <div class="row q-col-gutter-sm q-px-xl">
        <div class="col-lg-3 col-md-3 col-xs-12 col-sm-6 q-px-lg">
          <q-card @mouseover="hoverOver(3)" @mouseout="hoverOutTimeout(3)"
                  class="my-other-card text-center box-shadow" flat bordered>
            <q-card-section class="text-white" style="background: #ec982f">
              <div :class="headingAnimation[3]"
                   class="text-h6 text-center">
                FILES
              </div>
            </q-card-section>
            <q-card-section class="q-pa-md">
              <q-list dense padding style="border-bottom: 1px solid lightgrey;">
                <q-item>
                  Exclude clientside-only mods
                </q-item>
                <q-item>
                  Add any files and/or directories
                </q-item>
                <q-item>
                  Custom, automatically scaled, server icon
                </q-item>
                <q-item>
                  Custom server.properties
                </q-item>
              </q-list>
            </q-card-section>
            <q-card-section>
              <div class="text-center q-mt-xs">
                Fully customize which files and directories get added to your server pack.
                Automatically, or manually, exclude mods which could break the server. Let ServerPackCreator scale
                the server icon for your server. The GUI provides a preview of the server icon for you.
              </div>
            </q-card-section>
          </q-card>
        </div>
        <div class="col-lg-3 col-md-3 col-xs-12 col-sm-6 q-px-lg">
          <q-card @mouseover="hoverOver(4)" @mouseout="hoverOutTimeout(4)"
                  class="my-other-card text-center box-shadow" flat bordered>
            <q-card-section class="text-white" style="background: #f4655f">
              <div :class="headingAnimation[4]"
                   class="text-h6 text-center">
                MODLOADERS
              </div>
            </q-card-section>
            <q-card-section class="q-pa-md">
              <q-list dense padding style="border-bottom: 1px solid lightgrey;">
                <q-item>
                  Forge
                </q-item>
                <q-item>
                  Fabric
                </q-item>
                <q-item>
                  Quilt
                </q-item>
                <q-item>
                  Legacy Fabric
                </q-item>
              </q-list>
            </q-card-section>
            <q-card-section>
              <div class="text-center q-mt-xs">
                The most commonly used modloaders for Minecraft are supported. Start scripts shipped with every
                generated
                server pack take care of the modloader server installation. Let ServerPackCreator install the
                modloader
                server for immediate testing.
              </div>
            </q-card-section>
          </q-card>
        </div>
        <div class="col-lg-3 col-md-3 col-xs-12 col-sm-6 q-px-lg">
          <q-card @mouseover="hoverOver(5)" @mouseout="hoverOutTimeout(5)"
                  class="my-other-card text-center box-shadow" flat bordered>
            <q-card-section class="text-white q-pa-md" style="background: #8b3bbd">
              <div :class="headingAnimation[5]"
                   class="text-h6 text-center">
                ADVANCED
              </div>
            </q-card-section>
            <q-card-section class="q-pa-md">
              <q-list dense padding style="border-bottom: 1px solid lightgrey;">
                <q-item>
                  Java Arguments / JVM flags
                </q-item>
                <q-item>
                  Start script templating
                </q-item>
                <q-item>
                  Minecraft Pre-releases and Snapshots
                </q-item>
                <q-item>
                  Aikar's Flags
                </q-item>
              </q-list>
            </q-card-section>
            <q-card-section>
              <div class="text-center q-mt-xs">
                Customize the JVM flags, or use Aikar's flags, to run your server with. Create your own start
                script
                templates and use your own placeholders for script generation for maximum customization. Use
                Minecraft
                snapshots or pre-releases if you like living on the edge.
              </div>
            </q-card-section>
          </q-card>
        </div>
        <div class="col-lg-3 col-md-3 col-xs-12 col-sm-6 q-px-lg">
          <q-card @mouseover="hoverOver(6)" @mouseout="hoverOutTimeout(6)"
                  class="my-other-card text-center box-shadow" flat bordered>
            <q-card-section class="text-white" style="background: #3b97d1">
              <div :class="headingAnimation[6]"
                   class="text-h6 text-center">
                PLUGINS
              </div>
            </q-card-section>
            <q-card-section class="q-pa-md">
              <q-list dense padding style="border-bottom: 1px solid lightgrey;">
                <q-item>
                  Configuration checks
                </q-item>
                <q-item>
                  Server pack generation tasks
                </q-item>
                <q-item>
                  Per server pack configurations and UIs
                </q-item>
                <q-item>
                  Program wide configurations and UIs
                </q-item>
              </q-list>
            </q-card-section>
            <q-card-section>
              <div class="text-center q-mt-xs">
                Use plugins to add new features in various places. Custom actions to run before, during and after
                server pack generation. Custom configuration checks and UI elements on a per server pack basis,
                and ServerPackCreator wide settings and UI elements.
              </div>
            </q-card-section>
          </q-card>
        </div>
      </div>
      <br>
      <br>
      <br>
    </div>
    <div class="row" style="background-color: #00000088 !important;">
      <div class="col-md-12 col-lg-12 col-xs-12 col-sm-12">
        <h5 class="text-center text-white">
          <a href="https://github.com/Griefed/ServerPackCreator" rel="noopener" target="_blank">
            <q-img src="~@/assets/serverpackcreator.webp" alt=""/>
          </a>
          <br><br>
          <span class="text-center text-grey-4 text-h6">
            <p>
                ServerPackCreator is an open-source passion-project on which I am working in my spare time. It is also the main
                reason for why I got into programming in the first place.
            </p>
            <p>
                I started getting into Java programming in february 2021, when I was working on an update for one of my
                Minecraft modpacks,
                Survive Create Prosper 4, and got tired and bored with manually creating a server pack everytime I
                wanted to update and
                test something, or just release a new server pack for the new modpack version.
            </p>
            <p>
                The thought <br>
              <strong><code><i>
                There must be a way to automate this, or at least make it less tedious...
              </i></code></strong><br>
              was the spark which sent me off into the world of programming.
            </p>
            <p>
                And here we are.
            </p>
            <span>
              <q-chip color="secondary" size="xl" text-color="white">
                <q-avatar color="primary" icon="mdi-download"/>
                <b>Downloads:&nbsp;</b>
                <b>{{ downloads }}</b>
              </q-chip>
            </span>
            <span class="row flex flex-center" style="margin-top: 45px;">
              <img alt="GitHub Repo stars"
                   src="https://img.shields.io/github/stars/griefed/serverpackcreator?color=c0ffee&label=GitHub%20Stars&labelColor=325358&logo=GitHub&style=for-the-badge">
              <q-separator inset vertical color="accent" spaced/>
              <img alt="Docker Pulls"
                   src="https://img.shields.io/docker/pulls/griefed/serverpackcreator?color=c0ffee&label=Docker%20Pulls&labelColor=325358&logo=Docker&style=for-the-badge">
            </span>

            <span class="row flex flex-center" style="margin-top: 20px;">
              ServerPackCreator is free and open source software. It contains no ads whatsoever and does not gather any
              data about you or anything else.

              If you find ServerPackCreator behind a paywall or otherwise monetized, then do not pay any money!
            </span>

          </span>
        </h5>
      </div>
    </div>

    <div class="row text-center flex flex-center" style="background-color: #00000088 !important;">
      <div class="col-md-12 col-lg-12 col-xs-12 col-sm-12">
        <h5 class="text-center text-grey-4">
          ServerPackCreator would not be where or what it is today without these wonderful companies and their products:
        </h5>
      </div>
      <div class="col-md-12 col-lg-12 col-sx-12 col-sm-12 q-gutter-md flex flex-center">
        <div class="col-md-4 col-lg-4 col-sx-12 col-sm-12">
          <q-flashcard :no-hover="hover" class="my-card text-center box-shadow" flat bordered>
            <q-flashcard-section transition="nudge-in" :active="active">
              <!--suppress CheckImageSize -->
              <img src="~@/assets/serverpackcreator/tile_blurred.webp" width=400 height=700 alt="">
            </q-flashcard-section>
            <q-card @mouseover="hoverOver(1)" @mouseout="hoverOutTimeout(1)" style="border: none;"
                    class="my-card text-center box-shadow" flat bordered>
              <a href="https://www.ej-technologies.com/products/install4j/overview.html" rel="noopener"
                 target="_blank">
                <q-img src="~@/assets/install4j_icon.webp" alt="multi-platform installer builder"/>
                <q-img src="~@/assets/install4j.webp" alt="multi-platform installer builder"/>
              </a>

              <q-card-section>
                <div class="text-overline text-orange-9">EJ TECHNOLOGIES</div>
                <div :class="headingAnimation[1]" :style="headingTint(1)"
                     class="about_heading_1 text-h6 q-mt-sm q-mb-xs">INSTALL4J
                </div>
                <div :class="headingAnimation[1]" :style="headingTint(1)"
                     class="about_heading_1 q-mt-sm q-mb-xs">
                  <code><strong>
                    install4j is a powerful multi-platform Java installer builder that generates native installers
                    and application launchers for Java applications.
                  </strong></code>
                </div>
                <div class="text-caption text-grey-6">
                  © Copyright 2001-{{ year }} ej-technologies GmbH<br>
                  install4j and ej-technologies logos property of ej-technologies GmbH
                </div>
              </q-card-section>
            </q-card>
          </q-flashcard>
        </div>
        <div class="col-md-4 col-lg-4 col-sx-12 col-sm-12">
          <q-flashcard :no-hover="hover" class="my-card text-center box-shadow" flat bordered>
            <q-flashcard-section transition="nudge-in" :active="active">
              <!--suppress CheckImageSize -->
              <img src="~@/assets/serverpackcreator/tile_blurred.webp" width=400 height=700 alt="">
            </q-flashcard-section>
            <q-card @mouseover="hoverOver(2)" @mouseout="hoverOutTimeout(2)"
                    :style="headingTint(2)" style="border: none;"
                    class="my-card text-center box-shadow" flat bordered>
              <a href="https://www.jetbrains.com/idea/" rel="noopener" target="_blank">
                <q-img src="~@/assets/idea.png" alt=""/>
              </a>

              <q-card-section>
                <div class="text-overline text-orange-9">JETBRAINS</div>
                <div :class="headingAnimation[2]" :style="headingTint(2)"
                     class="text-h6 q-mt-sm q-mb-xs">INTELLIJ IDEA
                </div>
                <div :class="headingAnimation[2]" :style="headingTint(2)"
                     class="about_heading_1 q-mt-sm q-mb-xs">
                  <code><strong>
                    IntelliJ IDEA is used to develop the backend and Swing-GUI of ServerPackCreator. It's an
                    incredibly capable and ergonomic IDE for JVM.
                  </strong></code>
                </div>
                <div class="text-caption text-grey-6">
                  Copyright © {{ year }} JetBrains s.r.o.<br>IntelliJ IDEA and the IntelliJ IDEA logo are registered trademarks of JetBrains s.r.o.
                </div>
              </q-card-section>
            </q-card>
          </q-flashcard>
        </div>
      </div>
    </div>
    <div class="row text-center flex flex-center" style="background-color: #00000088 !important;">
      <br>
      <br>
    </div>
    <div style="background: #f7f7f7">
      <div class="row">
        <div class="col-md-12 col-lg-12 col-xs-12 col-sm-12">
          <h5 class="text-center"><strong>Platform Support</strong>
            <br>
            <span class="text-center text-grey-10 text-h6">
              Windows, Ubuntu and MacOS are officially supported via installers.<br>
              For other platforms, try the JAR-file!
            </span>
          </h5>
        </div>
      </div>
      <br>
      <div class="row text-center" style="padding-bottom: 40px">
        <div class="col-md-12 col-sm-12 col-xs-12 col-lg-12">
          <span v-for="index in 3" :key="index"  class="text-bold">
            <q-flashcard :no-hover="hover" :style="styleTop">
              <q-flashcard-section transition="nudge-in" :active="active">
                <img :src="cardImage(index)" width=340 height=263 alt="">
              </q-flashcard-section>
              <q-flashcard-section transition="fade-in" class="fit" :active="active">
                <div class="fit" :style="background_style"/>
                <q-flashcard-section transition="drop-down" class="text-center my-header"
                                     :active="active">
                  CLI Mode
                </q-flashcard-section>
                <q-flashcard-section transition="slide-up-in" class="my-text"
                                     :active="active">
                  Generating server packs right from the commandline.
                </q-flashcard-section>
              </q-flashcard-section>
            </q-flashcard>
            <br v-if="index===3"/>
            </span>
        </div>
      </div>
      <div class="row text-center" style="padding-bottom: 40px">
        <div class="col-md-12 col-sm-12 col-xs-12 col-lg-12">
          <span v-for="index in 3" :key="index">
            <q-flashcard :no-hover="hover" :style="styleBottom">
              <q-flashcard-section transition="nudge-in" :active="active">
                <img :src="cardImage(index + 3)" width=340 height=263 alt="">
              </q-flashcard-section>
              <q-flashcard-section transition="fade-in" class="fit" :active="active">
                <div class="fit" :style="background_style"></div>
                <q-flashcard-section transition="slide-up-in" class="text-center my-header" :active="active">
                  GUI Mode
                </q-flashcard-section>
                <q-flashcard-section transition="slide-up-in" class="my-text" :active="active">
                  Convenient, extensive and quick to configure and generate.
                </q-flashcard-section>
              </q-flashcard-section>
            </q-flashcard>
            </span>
        </div>
      </div>
      <div class="row text-center" style="padding-bottom: 40px">
        <div class="col-md-12 col-sm-12 col-xs-12 col-lg-12">
          <span v-for="index in 3" :key="index">
            <q-flashcard :no-hover="hover" :style="styleBottom">
              <q-flashcard-section transition="nudge-in" :active="active">
                <img :src="cardImage(index + 6)" width=340 height=263 alt="">
              </q-flashcard-section>
              <q-flashcard-section transition="fade-in" class="fit" :active="active">
                <div class="fit" :style="background_style"></div>
                <q-flashcard-section transition="slide-up-in" class="text-center my-header" :active="active">
                  Webservice
                </q-flashcard-section>
                <q-flashcard-section transition="slide-up-in" class="my-text" :active="active">
                  Generate a server pack from the comfort of your browser!
                </q-flashcard-section>
              </q-flashcard-section>
            </q-flashcard>
            </span>
        </div>
      </div>
    </div>
    <div style="background-color: #00000088 !important;">
      <q-carousel
        animated
        v-model="reviewSlide"
        padding
        arrows
        swipeable
        navigation
        infinite
        autoplay
        control-color="white"
        transition-prev="slide-right"
        transition-next="slide-left"
        height="700px"
        class="bg-transparent text-white">
        <q-carousel-slide style="padding-bottom: 160px" class="column flex flex-center" name="1">
          <div class="q-mt-md text-h6 text-center">
            <div>
              <a style="color: #ed6b00" href="https://www.curseforge.com/minecraft/modpacks/world-of-dragons"
                 rel="noopener" target="_blank">
                <img :style="'margin-top:100px;border-radius:5px;border: 4px solid ' + primaryColour"
                     src="~@/assets/serverpackcreator/testimonials/kreezxil.webp" width=150 height=150 alt="">
              </a>
              <br>
              <a style="color: #ed6b00" href="https://www.curseforge.com/minecraft/modpacks/world-of-dragons"
                 rel="noopener" target="_blank">
                World of Dragons
              </a>
              <br>
              <span class="text-overline">by Kreezxil</span>
            </div>
            <div>
              "Before ServerPackCreator I would use my own well-documented manual method to create server packs.<br>
              See <a href="https://kreezcraft.com/uupt" rel="noopener" target="_blank">
              Forge Server from Forge Pack</a> and also
              <a href="https://kreezcraft.com/n0cp" rel="noopener" target="_blank">
                Fabric Server from a Fabric Pack
              </a>.<br>
              The two methods are very similar but time consuming. ServerPackCreator (SPC) allows you to do all of
              that
              with a few simple quick button clicks!<br>
              Because of this, I'm also a sponsor."
            </div>
          </div>
        </q-carousel-slide>
        <q-carousel-slide class="column flex flex-center" name="2">
          <div class="q-mt-md text-h6 text-center">
            <div>
              <a style="color: #ed6b00" href="https://www.curseforge.com/minecraft/modpacks/fantasy-minecraft-fabric"
                 rel="noopener" target="_blank">
                <img :style="'margin-top:100px;border-radius:5px;border: 4px solid ' + primaryColour"
                     src="~@/assets/serverpackcreator/testimonials/fantasy_minecraft.webp" width=150 height=150 alt="">
              </a>
              <br>
              <a style="color: #ed6b00" href="https://www.curseforge.com/minecraft/modpacks/fantasy-minecraft-fabric"
                 rel="noopener" target="_blank">
                Fantasy Minecraft
              </a>
              <br>
              <span class="text-overline">by Kyber_6</span>
            </div>
            <br>
            <div>
              "ServerPackCreator has made the process of creating Server Packs for our Modpack much more quick and
              efficient."
            </div>
          </div>
        </q-carousel-slide>
        <q-carousel-slide class="column flex-center" name="3">
          <div class="q-mt-md text-h6 text-center">
            <div>
              <a style="color: #ed6b00" href="https://www.curseforge.com/minecraft/modpacks/modded-oneblock-skyblock"
                 rel="noopener" target="_blank">
                <img :style="'margin-top:100px;border-radius:5px;border: 4px solid ' + primaryColour"
                     src="~@/assets/serverpackcreator/testimonials/darkly.webp" width=150 height=150 alt="">
              </a>
              <br>
              <a style="color: #ed6b00" href="https://www.curseforge.com/minecraft/modpacks/modded-oneblock-skyblock"
                 rel="noopener" target="_blank">
                Modded Oneblock Skyblock
              </a>
              <br>
              <span class="text-overline">by Darkly</span>
            </div>
            <br>
            <div>
              "Server pack creator is like a Instant Pot, it has everything you need to make a yummy server pack."
            </div>
          </div>
        </q-carousel-slide>
        <q-carousel-slide class="column flex-center" name="4">
          <div class="q-mt-md text-h6 text-center">
            <div>
              <a style="color: #ed6b00" href="https://www.curseforge.com/minecraft/modpacks/monumental-experience"
                 rel="noopener" target="_blank">
                <img :style="'margin-top:100px;border-radius:5px;border: 4px solid ' + primaryColour"
                     src="~@/assets/serverpackcreator/testimonials/monumental-experience.webp" width=150 height=150 alt="">
              </a>
              <br>
              <a style="color: #ed6b00" href="https://www.curseforge.com/minecraft/modpacks/monumental-experience"
                 rel="noopener" target="_blank">
                Monumental Experience
              </a>
              <br>
              <span class="text-overline">by ModernGamingWorld</span>
            </div>
            <br>
            <div>
              "ServerPackCreator has made the process of creating server files much easier and simple for my modpacks. Without it. Creating server packs would be a huge pain to do."
            </div>
          </div>
        </q-carousel-slide>
      </q-carousel>
    </div>
    <div style="background: #f7f7f7">
      <div class="q-pa-xl">
        <div class="row">
          <div class="col-md-4 col-lg-4 col-sm-12 col-xs-12">
            <q-item class="box-shadow q-pa-none q-ml-xs ">
              <q-item-section side :style="'background-color:' + primaryColour" class="q-pa-sm q-mr-none text-white">
                <q-icon name="compare_arrows" size="65px"/>
              </q-item-section>
              <q-item-section class="q-pa-md q-ml-none">
                <q-item-label class="text-h6 text-grey-10 text-uppercase">Contributing</q-item-label>
                <q-item-label class="text-blue-grey-10">Want to contribute to ServerPackCreator? Check out the <br>
                  <strong>
                    <a href="https://github.com/Griefed/ServerPackCreator/blob/main/CONTRIBUTING.md"
                       rel="noopener" target="_blank">
                      Contributing-Guidelines
                    </a>
                  </strong>
                </q-item-label>
              </q-item-section>
            </q-item>
          </div>
          <div class="col-md-4 col-lg-4 col-sm-12 col-xs-12">
            <q-item class="box-shadow q-pa-none q-ml-xs">
              <q-item-section side :style="'background-color:' + primaryColour" class="q-pa-sm q-mr-none text-white">
                <q-icon name="far fa-life-ring" size="65px"/>
              </q-item-section>
              <q-item-section class="q-pa-md q-ml-none">
                <q-item-label class="text-h6 text-grey-10 text-uppercase">Support</q-item-label>
                <q-item-label class="text-blue-grey-10">Encountered a bug in ServerPackCreator?<br>
                  <strong>
                    <a href="https://github.com/Griefed/ServerPackCreator/issues" rel="noopener"
                       target="_blank">
                      Report issues on GitHub.
                    </a>
                  </strong>
                </q-item-label>
              </q-item-section>
            </q-item>
          </div>
          <div class="col-md-4 col-lg-4 col-sm-12 col-xs-12">
            <q-item class="box-shadow q-pa-none q-ml-xs ">
              <q-item-section side :style="'background-color:' + primaryColour" class="q-pa-sm q-mr-none text-white">
                <q-icon name="fas fa-code-branch" size="65px"/>
              </q-item-section>
              <q-item-section class="q-pa-md q-ml-none">
                <q-item-label class="text-h6 text-grey-10 text-uppercase">Sources</q-item-label>
                <q-item-label class="text-blue-grey-10">Interested in the source code of ServerPackCreator?<br>
                  <strong>
                    <a href="https://github.com/Griefed/ServerPackCreator" rel="noopener" target="_blank">
                      See the publicly available GitHub-Repository.
                    </a>
                  </strong>
                </q-item-label>
              </q-item-section>
            </q-item>
          </div>
        </div>
      </div>
      <div class="text-center">
        The <a href="https://github.com/mayank091193/vue-quasar-company-profile-website" rel="noopener"
               target="_blank">vue-quasar-company-profile-website</a>-template,
        by <a href="https://github.com/mayank091193" rel="noopener" target="_blank">mayank091193</a>, was
        used in the creation of the
        ServerPackCreator-website.
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import {getCssVar} from "quasar"
import {QFlashcard, QFlashcardSection} from '@quasar/quasar-ui-qflashcard'
import { defineComponent, ref } from 'vue';
import { readDownloadTotal, refreshDownloadTotal } from '@/services/downloads/downloadTotal';

/** The headings' colour while nothing is hovering them. */
const RESTING_HEADING_COLOUR = '#f7f7f7';

/**
 * Render a total for the chip. A null total means no forge could be counted, which is a different
 * thing from nobody having downloaded anything and must not read as zero.
 */
function formatDownloads(total: number | null): string {
  return total === null ? 'many' : new Intl.NumberFormat().format(total);
}


// Vite has no `require`. The nine flashcard images are collected at build time and looked up by
// number, so the template keeps the `v-for` indices it already counts with.
const cardImages: Record<string, string> = import.meta.glob('../assets/serverpackcreator/cards/*.png', {
  eager: true,
  import: 'default'
});

export default defineComponent({
  name: "ServerPackCreator",
  components: {
    QFlashcard,
    QFlashcardSection
  },
  data() {
    return {
      slide: 1,
      reviewSlide: '1',
      hover: false,
      active: false,
      primaryColour: getCssVar('primary'),
      secondaryColour: getCssVar('secondary'),
      background_style: 'background-color:rgba(0, 163, 82, 0.7);',
      // Keyed by the card index the hover handlers are called with. A record rather than six
      // numbered properties because the handlers address them dynamically, which no amount of
      // typing can express against `this`.
      headingAnimation: { 1: [], 2: [], 3: [], 4: [], 5: [], 6: [] } as Record<number, string[]>,
      /** The heading currently under the pointer, which is the only one that may be tinted. */
      hoveredHeading: null as number | null
    }
  },
  setup() {
    // The build-time figure, so the chip is correct on first paint with no request and no spinner.
    const baked = readDownloadTotal();
    return {
      year: new Date().getFullYear(),
      downloads: ref(formatDownloads(baked.total))
    }
  },
  computed: {
    styleTop() {
      return {
        width: '340px',
        height: '263px',
        textAlign: 'center',
        boxShadow: '1px 1px 2px #e6e6e6',
        display: 'inline-block',
        marginLeft: '15px',
        marginRight: '15px'
      }
    },
    styleBottom() {
      return {
        width: '340px',
        height: '263px',
        textAlign: 'center',
        boxShadow: '1px 1px 2px #e6e6e6',
        display: 'inline-block',
        marginLeft: '15px',
        marginRight: '15px'
      }
    }
  },
  methods: {
    cardImage(number: number): string | undefined {
      return cardImages[`../assets/serverpackcreator/cards/${number}.png`];
    },
    /**
     * The colour of a heading that tints while hovered. Only the two sponsor headings use it: they
     * sit on the dark panel, where the accent reads. The Features Excerpt headings sit on saturated
     * orange, salmon, purple and blue bands and keep their white, which the accent does not survive.
     */
    headingTint(index: number): string {
      const colour = this.hoveredHeading === index ? this.secondaryColour : RESTING_HEADING_COLOUR;
      return `color: ${colour}`;
    },
    hoverOver(index: number) {
      this.hoveredHeading = index;
      this.headingAnimation[index] = ['animated', 'bounceIn'];
    },
    hoverOutTimeout(index: number) {
      this.hoveredHeading = null;
      // Delayed so the bounce finishes before the heading snaps back.
      setTimeout(() => {
        this.headingAnimation[index] = [];
      }, 1000);
    }
  },
  mounted() {
    // Best-effort, and at most once a day per visitor. A forge that is unreachable from a browser
    // -- git.griefed.de sends no CORS header -- changes nothing: the baked figure already has it.
    refreshDownloadTotal()
      .then(snapshot => {
        this.downloads = formatDownloads(snapshot.total);
      })
      .catch(() => {
        // Keep showing the figure that is already on screen.
      });
  }
})
</script>

<style scoped>
.my-card {
  /*noinspection CssUnknownTarget*/
  background: transparent repeat fixed;
  width: 350px;
  max-width: 350px;
  margin-top: 10px;
}

.my-other-card {
  /*noinspection CssUnknownTarget*/
  background: #EEEEEE repeat fixed;
}

.main_line {
  font-size: 75px;
  letter-spacing: 5px;
  line-height: 60px;
  font-weight: 600;
}

.custom-caption {
  text-align: center;
  padding: 12px;
  color: white;
}

.animation_1 {
  -webkit-animation: bounceIn 1s ease-in 800ms both;
  animation: bounceIn 1s ease-in 800ms both;
}

.animation_2 {
  -webkit-animation: flipInX 2s ease-in-out 800ms both;
  animation: flipInX 1s ease-in-out 800ms both;
}

.my-header {
  width: 200px;
  top: 0;
  height: 45px;
  color: black;
  background-color: rgba(255, 255, 255, 0.8);
  text-transform: uppercase;
  text-align: center;
  font-size: 18px;
  margin: 20px 0 0 68px;
  padding: 10px;
  font-weight: bold;
}

.my-text {
  width: 100%;
  top: 0;
  height: 90px;
  color: white;
  text-align: center;
  font-size: 18px;
  margin: 79px 0 0 0;
  padding: 20px;
  line-height: normal;
  font-family: Georgia, serif;
  font-weight: bold;
  font-style: italic;
}

.box-shadow:hover {
  box-shadow: 0 10px 13px -6px rgba(0, 0, 0, 0.2), 0 20px 31px 3px rgba(0, 0, 0, 0.14), 0 8px 38px 7px rgba(0, 0, 0, 0.12) !important;
}
</style>
