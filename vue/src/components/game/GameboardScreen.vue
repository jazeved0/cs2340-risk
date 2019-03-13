<!--suppress HtmlUnknownTag -->
<template>
  <div class="gameboard d-flex flex-column">
    <tool-bar>
      <span slot="left-element">
        <!-- TODO Should not be wrapping h1 in span -->
        <h1 style="color:white">RISK</h1>
      </span>
    </tool-bar>
    <div class="stage-wrapper flex-fill" ref="stageWrapper">
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
        <v-layer>
          <v-army-shape v-for="army in armyData"
              :data="army"
              :key="army.num"></v-army-shape>
        </v-layer>
      </v-stage>
    </div>
    <player-info-bar class="players" :overdraw="playerInfoBarOverdraw" ref="playerInfo">
    </player-info-bar>
    <territory-assignment-modal
        v-bind:visible="showAssignmentModal"
        @hide="this.hasSeenAssignments = true">
    </territory-assignment-modal>
  </div>
</template>

<script>
  import PlayerInfoBar from './PlayerInfoBar'
  import ArmyShape from './ArmyShape';
  import TerritoryAssignmentModal from './TerritoryAssignmentModal';
  import Toolbar from './../Toolbar'
  import VueKonva from 'vue-konva';
  // noinspection ES6UnusedImports
  import Vue from "vue";
  import {clamp, ColorLuminance, distance} from './../../util'
  import {GUI_CTX} from "../../store/modules/game/InitializeGameboardScreen";

  // noinspection JSUnresolvedFunction
  Vue.use(VueKonva);

  export default {
    components: {
      'tool-bar': Toolbar,
      'player-info-bar': PlayerInfoBar,
      'v-army-shape': ArmyShape,
      'territory-assignment-modal': TerritoryAssignmentModal
    },
    computed: {
      armyData: function () {
        const store = this.$store;
        return store.getters.boardStates.filter(
          ts => 'owner' in ts
            && 'amount' in ts
            && 'territory' in ts)
        .map(territoryState => {
          let color = territoryState.owner < store.state.game.playerStateList.length
            ? store.state.game.playerStateList[territoryState.owner]
            : null;
          if ('player' in color && color !== null) {
            color = color.player.settings.ordinal;
          } else color = 0;
          return {
            size: territoryState.amount,
            color: color,
            position: territoryState.territory,
            num: territoryState.territory
          }
        });
      },
      pathConfigs: function () {
        const mouseOver = this.mouseOver;
        const state = this.$store.state;
        return state.game.gameboard.pathData.map(function (item, index) {
          const region = state.game.gameboard.regions.findIndex(r => r.includes(index));
          let color = 'lightgray';
          if (state.settings.settings.territoryColors.length > region) {
            color = state.settings.settings.territoryColors[region];
          }
          return {
            x: 0,
            y: 0,
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
        return gameState.gameboard.waterConnections.map(function (item, index) {
          const node1 = gameState.gameboard.centers[item.a];
          const node2 = gameState.gameboard.centers[item.b];
          let bezier = 'bz' in item ? item.bz : false;
          let tension = 'tension' in item ? item.tension : 0;
          let points = [node1[0], node1[1]];
          if ('midpoints' in item && item.midpoints.length > 0) {
            item.midpoints.forEach(function (point) {
              points.push(point[0]);
              points.push(point[1])
            });
          }
          points.push(node2[0], node2[1]);
          return {
            x: 0,
            y: 0,
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
          draggable: true,
          dragBoundFunc: (pos) => this.clampPosition(pos)
        }
      },
      showAssignmentModal: function () {
        return !this.hasSeenAssignments &&
          this.$store.getters.playerStates.length > 0;
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
          newScale = this.clampScale(newScale);
          stage.scale({x: newScale, y: newScale});

          const newPosition = {
            x: (pointer.x / newScale - startPos.x) * newScale,
            y: (pointer.y / newScale - startPos.y) * newScale,
          };
          stage.position(this.clampPosition(newPosition));
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
              {x: t1.clientX, y: t1.clientY},
              {x: t2.clientX, y: t2.clientY}
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
            const newScale = this.clampScale(delta < 0 ? oldScale / scaleBy : oldScale * scaleBy);
            stage.scale({x: newScale, y: newScale});

            const newPosition = {
              x: (pointer.x / newScale - startPos.x) * newScale,
              y: (pointer.y / newScale - startPos.y) * newScale,
            };

            stage.position(this.clampPosition(newPosition));
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
      },
      clampScale: function (scale) {
        return clamp(scale, this.scaleBounds.min, this.scaleBounds.max);
      },
      clampPosition: function (pos) {
        const bounds = this.calculatePositionBounds();
        return {
          x: clamp(pos.x, bounds.x.min, bounds.x.max),
          y: clamp(pos.y, bounds.y.min, bounds.y.max)
        }
      },
      calculatePositionBounds: function () {
        const scale = this.stageObj.scale();
        const bounds = this.stageDimensions;
        const size = this.$store.state.game.gameboard.size;
        return {
          x: this.axisBounds(size[0] * scale.x, bounds.w),
          y: this.axisBounds(size[1] * scale.y, bounds.h)
        };
      },
      axisBounds: function (size, bound) {
        return (size < bound) ? {
          min: -(size / 2),
          max: bound - (size / 2)
        } : {
          min: bound / 2 - size,
          max: bound / 2
        }
      },
      calculateInitialTransform: function () {
        const bounds = this.stageDimensions;
        let totalW = bounds.w;
        let totalH = bounds.h;
        const state = this.$store.state;
        if ('playerInfo' in this.$refs) {
          const maxPlayerInfoWidth = (state.game.playerInfoCard.w *
            state.playersList.length);
          if (maxPlayerInfoWidth > (bounds.w * 0.7)) {
            totalH -= state.game.playerInfoCard.h;
          }
        }
        const size = this.$store.state.game.gameboard.size;
        // make initial map take up 3/4 of smaller dimension
        const margin = Math.min(totalW, totalH) / 8;
        const kw = (totalW - 2 * margin) / size[0];
        const kh = (totalH - 2 * margin) / size[1];
        const k = Math.min(kw, kh);
        return {
          x: (totalW - (size[0] * k)) / 2,
          y: (totalH - (size[1] * k)) / 2,
          scale: k
        };
      }
    },
    data() {
      return {
        mouseOver: -1,
        navHeight: 62,
        playerInfoBarOverdraw: 32,
        stageDimensions: {
          w: 0, h: 0
        },
        hasSeenAssignments: false,
        stageObj: undefined,
        touchState: {
          lastDist: 0,
          point: undefined
        },
        scaleBounds: {
          min: 0.8,
          max: 5
        }
      };
    },
    mounted() {
      this.$nextTick(function () {
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

          const closure = {
            'stage': this.stageObj,
            'scaleBounds': this.scaleBounds,
            'transformCallback': this.calculateInitialTransform };
          this.$store.state.game.tryInitializeGameboardScreen(GUI_CTX, function () {
            // transform by the initial transforms
            const initialTransform = closure.transformCallback();
            closure.stage.scale({
              x: initialTransform.scale,
              y: initialTransform.scale
            });
            closure.stage.x(initialTransform.x);
            closure.stage.y(initialTransform.y);
            closure.scaleBounds.min = Math.min(closure.scaleBounds.min, initialTransform.scale);
            closure.scaleBounds.max = Math.max(closure.scaleBounds.max, initialTransform.scale)
          });
        }
      });
    },
    beforeDestroy() {
      // detach listener
      window.removeEventListener('resize', this.resizeCanvas);
    }
  };
</script>

<style lang="scss">
  .gameboard {
    height: 100vh;
  }

  .stage-wrapper {
    overflow: hidden;
  }

  .players {
    position: absolute;
    bottom: 0;
  }
</style>
