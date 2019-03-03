<template>
  <div class="gameboard">
    <nav class="navbar fixed-top navbar-dark bg-dark">
      <h1 style="color:white">RISK</h1>
      <a class="navbar-brand" href="https://github.gatech.edu/achafos3/CS2340Sp19Team10" name="github" target="_blank" rel="noopener">
        <fa-icon :icon="['fab', 'github']" size="lg" class="repoImg pl-2"></fa-icon>
      </a>
    </nav>
    <div :style="{ paddingTop: navHeight + 'px' }">
      <v-stage :config="configKonva">
        <v-layer>
          <v-path v-for="pathConfig in pathConfigs"
              :key="pathConfig.num"
              :config="pathConfig"
              @mouseover="territoryMouseOver(pathConfig.num)"
              @mouseout="territoryMouseOut(pathConfig.num)"></v-path>
        </v-layer>
      </v-stage>
    </div>
    <player-info-bar>
    </player-info-bar>
  </div>
</template>

<script>
  import PlayerInfoBar from './PlayerInfoBar.vue'
  import VueKonva from 'vue-konva';
  // noinspection ES6UnusedImports
  import Vue from "vue";

  Vue.use(VueKonva);

  export default {
    components: {
      'player-info-bar': PlayerInfoBar
    },
    computed: {
      pathConfigs: function () {
        const mouseOver = this.mouseOver;
        return this.$store.state.game.territoryPathData.map(function(item, index) {
          return {
            x: 40,
            y: 40,
            data: item,
            fill: mouseOver === index ? 'white' : 'lightgray',
            scale: {
              x: 1,
              y: 1
            },
            num: index
          };
        });
      }
    },
    methods: {
      territoryMouseOver: function (num) {
        this.mouseOver = num;
      },
      territoryMouseOut: function (num) {
        if (this.mouseOver === num) this.mouseOver = -1;
      }
    },
    data() {
      return {
        configKonva: {
          width: 400,
          height: 500
        },
        mouseOver: -1,
        navHeight: 62
      };
    }
  }
</script>

<style lang="scss">
  .gameboard {
    background-color: #362a4d;
  }

  img {
    display: block;
    margin-left: auto;
    margin-right: auto;
    width: 75%;
    max-width: 100%;
    max-height: 100%;
    padding: 0;
  }

  footer {
    background-color: #21252b;
    padding: 5%;
    min-height: 10%;
  }
</style>
