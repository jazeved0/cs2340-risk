<template>
  <div class="gameboard">
    <tool-bar>
      <span slot="left-element">
        <h1 style="color:white">RISK</h1>
      </span>
    </tool-bar>
    <div :style="{ paddingTop: navHeight + 'px' }" class="full-height d-flex flex-column">
      <div class="flex-fill stage-wrapper" ref="stageWrapper">
        <v-stage :config="stageConfig" ref="stage">
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
    </div>
    <player-info-bar class="players" :overdraw="playerInfoBarOverdraw">
    </player-info-bar>
  </div>
</template>

<script>
  import PlayerInfoBar from './PlayerInfoBar.vue'
  import Toolbar from './../Toolbar'
  import VueKonva from 'vue-konva';
  // noinspection ES6UnusedImports
  import Vue from "vue";
  import {ColorLuminance, distance} from './../../util'

  Vue.use(VueKonva);

  export default {
    components: {
      'tool-bar': Toolbar,
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
      },
      stageConfig: function () {
        return {
          width: this.stageDimensions.w,
          height: this.stageDimensions.h,
          draggable: true
        }
      }
    },
    methods: {
      territoryMouseOver: function (num) {
        this.mouseOver = num;
      },
      territoryMouseOut: function (num) {
        if (this.mouseOver === num) this.mouseOver = -1;
      },
      resizeCanvas: function () {
        if (this.$refs && 'stageWrapper' in this.$refs) {
          this.stageDimensions = {
            w: this.$refs.stageWrapper.clientWidth,
            h: this.$refs.stageWrapper.clientHeight
          }
        }
      },
      scrollZoom: function (wheelEvent) {
        const stage = this.stageObj;
        if (typeof stage !== 'undefined') {
          wheelEvent.preventDefault();
          const oldScale = stage.scaleX();

          const pointer = stage.getPointerPosition();
          const startPos = {
            x: pointer.x / oldScale - stage.x() / oldScale,
            y: pointer.y / oldScale - stage.y() / oldScale,
          };

          let deltaYBounded = 0;
          if (!(wheelEvent.deltaY % 1)) {
            deltaYBounded = Math.abs(Math.min(-10, Math.max(10, wheelEvent.deltaY)))
          } else {
            // noinspection JSSuspiciousNameCombination
            deltaYBounded = Math.abs(wheelEvent.deltaY)
          }
          const scaleBy = 1.01 + deltaYBounded / 70;
          let newScale = 0;
          if (wheelEvent.deltaY > 0) {
            newScale = oldScale / scaleBy;
          } else {
            newScale = oldScale * scaleBy;
          }
          stage.scale({ x: newScale, y: newScale });

          const newPosition = {
            x: (pointer.x / newScale - startPos.x) * newScale,
            y: (pointer.y / newScale - startPos.y) * newScale,
          };
          stage.position(newPosition);
          stage.batchDraw();
        }
      },
      touchMove: function (event) {
        const t1 = event.touches[0];
        const t2 = event.touches[1];

        if (t1 && t2) {
          event.preventDefault();
          event.stopPropagation();
          const stage = this.stageObj;
          const touchState = this.touchState;

          if (typeof stage !== 'undefined') {
            const oldScale = stage.scaleX();

            const dist = distance(
              { x: t1.clientX, y: t1.clientY },
              { x: t2.clientX, y: t2.clientY }
            );

            if (!touchState.lastDist) touchState.lastDist = dist;
            const delta = dist - touchState.lastDist;

            const px = (t1.clientX + t2.clientX) / 2;
            const py = (t1.clientY + t2.clientY) / 2;

            let pointer = {};
            if (typeof touchState.point === 'undefined') {
              pointer = this.relativePointer(px, py, stage);
              touchState.point = pointer;
            } else {
              pointer = touchState.point;
            }

            const startPos = {
              x: pointer.x / oldScale - stage.x() / oldScale,
              y: pointer.y / oldScale - stage.y() / oldScale,
            };

            const scaleBy = 1.01 + Math.abs(delta) / 100;
            const newScale = delta < 0 ? oldScale / scaleBy : oldScale * scaleBy;
            stage.scale({ x: newScale, y: newScale });

            const newPosition = {
              x: (pointer.x / newScale - startPos.x) * newScale,
              y: (pointer.y / newScale - startPos.y) * newScale,
            };

            stage.position(newPosition);
            stage.batchDraw();
            touchState.lastDist = dist;
          }
        }
      },
      touchEnd: function () {
        this.touchState.lastDist = 0;
        this.touchState.point = undefined;
      },
      relativePointer: function (clientX, clientY, stage) {
        return {
          x: clientX - stage.getContent().offsetLeft,
          y: clientY - stage.getContent().offsetTop,
        }
      }
    },
    data() {
      return {
        mouseOver: -1,
        navHeight: 62,
        base: [40, 40],
        playerInfoBarOverdraw: 32,
        stageDimensions: {
          w: 0, h: 0
        },
        stageObj: undefined,
        touchState: {
          lastDist: 0,
          point: undefined
        }
      };
    },
    mounted() {
      this.$nextTick(function() {
        // attach listener
        window.addEventListener('resize', this.resizeCanvas);
        // initialize canvas dimensions
        this.resizeCanvas();
        if (this.$refs.stage) {
          // load the stageObj
          this.stageObj = this.$refs.stage.getStage();
          // attach the scroll listener
          const stageContent = this.stageObj.getContent();
          stageContent.addEventListener('wheel', this.scrollZoom);
          // attach the touch listeners
          stageContent.addEventListener('touchmove', this.touchMove, false);
          stageContent.addEventListener('touchend', this.touchEnd, false);
        }
      })
    },
    beforeDestroy() {
      // detach listener
      window.removeEventListener('resize', this.resizeCanvas);
    }
  }
</script>

<style lang="scss">
  .gameboard {
    height: 100vh;
  }

  .full-height {
    height: 100%;
  }

  .stage-wrapper {
    overflow: hidden;
  }

  .players {
    position: absolute;
    bottom: 0;
  }
</style>
