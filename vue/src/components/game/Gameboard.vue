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
          <v-line v-for="waterConnection in waterConnectionConfigs"
              :key="waterConnection.num"
              :config="waterConnection"></v-line>
        </v-layer>
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
  import {ColorLuminance} from './../../util'

  Vue.use(VueKonva);

  export default {
    components: {
      'player-info-bar': PlayerInfoBar
    },
    computed: {
      pathConfigs: function () {
        const mouseOver = this.mouseOver;
        const base = this.base;
        const state = this.$store.state;
        return state.game.gameboard.pathData.map(function(item, index) {
          const region = state.game.gameboard.regions.findIndex(r => r.includes(index));
          let color = 'lightgray';
          if (state.settings.settings.territoryColors.length > region) {
            color = state.settings.settings.territoryColors[region];
          }
          return {
            x: base[0],
            y: base[1],
            data: item,
            fill: mouseOver === index ? ColorLuminance(color, 0.2) : ('#' + color),
            scale: {
              x: 1,
              y: 1
            },
            shadowBlur: 10,
            num: index
          };
        });
      },
      waterConnectionConfigs: function () {
        const gameState = this.$store.state.game;
        const base = this.base;
        return gameState.gameboard.waterConnections.map(function(item, index) {
          const node1 = gameState.gameboard.centers[item.a];
          const node2 = gameState.gameboard.centers[item.b];
          let bezier = item.bz;
          let tension = item.tension;
          let points = [node1[0], node1[1]];
          if (item.midpoints.length > 0) {
            item.midpoints.forEach(function(point) {
              points.push(point[0]);
              points.push(point[1])
            });
          }
          points.push(node2[0], node2[1]);
          return {
            x: base[0],
            y: base[1],
            points: points,
            bezier: bezier,
            tension: tension,
            strokeWidth: 3,
            stroke: 'white',
            dash: [6, 6],
            opacity: 0.7,
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
          width: 1200,
          height: 800
        },
        mouseOver: -1,
        navHeight: 62,
        base: [40, 40]
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
