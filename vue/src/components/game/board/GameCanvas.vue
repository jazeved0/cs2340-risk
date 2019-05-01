<!--suppress HtmlUnknownTag -->
<template>
  <div class="stage-wrapper" ref="stageWrapper">
    <v-stage :config="stageConfig" ref="stage">
      <v-layer>
        <v-line v-for="waterConnection in waterConnectionConfigs"
            :key="waterConnection.num"
            :config="waterConnection"></v-line>
      </v-layer>
      <v-layer>
        <!-- Territory custom shapes -->
        <v-territory
            v-for="(territory, index) in territoryData"
            :key="index"
            :castle=   "territory.castle"
            :highlight="territory.highlight"
            :baseColor="territory.baseColor"
            :path=     "territory.path"
            @territory-click-raw="handleTerritoryMouseDown(index)">
        </v-territory>
      </v-layer>
      <v-layer>
        <v-castle-icon v-for="castle in castleData"
            :data="castle"
            :key="castle.num"></v-castle-icon>
        <v-army-shape v-for="army in armyData"
            :data="army"
            :key="army.num"></v-army-shape>
      </v-layer>
    </v-stage>
  </div>
</template>

<script>
  import {clamp, distance, exists, logError} from "../../../util";
  import {GUI_CTX} from "../../../store/modules/game/InitializeGameboardScreen";

  // Components
  import ArmyShape from './ArmyShape';
  import CastleIcon from './CastleIcon';

  // hook Konva
  import VueKonva from 'vue-konva';
  import Vue from "vue";
  import Territory from "./Territory";
  // noinspection JSUnresolvedFunction
  Vue.use(VueKonva);

  // Smart component: accesses state
  export default {
    props: {
      // Array of territory indices to highlight
      highlight:      Array,
      // Border color to add to the highlighted territories
      highlightColor: String
    },

    components: {
      'v-army-shape': ArmyShape,
      'v-territory': Territory,
      'v-castle-icon': CastleIcon
    },

    data () {
      return {
        // DOM object for the stage, gotten through refs upon mount
        stageObj: undefined,
        // Current stage dimensions updated upon resize
        stageDimensions: {
          w: 100,
          h: 100
        },
        // Current touch state (unused if on desktop)
        touchState: {
          lastDist: 0,
          point: undefined
        },
        // Current scale bounds (may be updated upon initialization as appropriate)
        scaleBounds: {
          min: 0.8,
          max: 5
        },
        // Current index of territory with mouse over
        mouseOver: -1
      }
    },

    computed: {
      // Configuration objects for the stage
      stageConfig () {
        return {
          // Depend on dimensions changed upon resize
          width: this.stageDimensions.w,
          height: this.stageDimensions.h,
          draggable: true,
          // Clamp dragging
          dragBoundFunc: (pos) => this.clampPosition(pos)
        }
      },

      // *****************
      // Rendering configs
      // *****************

      // Creates array of territory data
      territoryData () {
        const state        = this.$store.state;
        const regionColors = state.settings.settings.territoryColors;
        if (exists(state.game.gameboard)) {
          return state.game.gameboard.territories.map((territory, index) => {
            const region = state.game.gameboard.regions.findIndex(r => r.includes(index));
            // noinspection JSUnresolvedFunction
            return {
              castle:    ('castle' in territory)        ? territory.castle     : null,
              highlight: this.highlight.includes(index) ? this.highlightColor  : null,
              baseColor: (region < regionColors.length) ? regionColors[region] : 'lightgrey',
              path:      state.game.gameboard.pathData[index]
            }
          })
        } else {
          logError("Gameboard not found. Skipping rendering territories");
          return [];
        }
      },

      // Config data for water connection paths
      waterConnectionConfigs () {
        const gameState = this.$store.state.game;
        return gameState.gameboard.waterConnections.map(function (item, index) {
          const node1 = gameState.gameboard.centers[item.a];
          const node2 = gameState.gameboard.centers[item.b];
          let bezier = 'bz' in item ? item.bz : false;
          let tension = 'tension' in item ? item.tension : 0;
          let points = [node1.a, node1.b];
          if ('midpoints' in item && item.midpoints.length > 0) {
            item.midpoints.forEach(function (point) {
              points.push(point[0]);
              points.push(point[1])
            });
          }
          points.push(node2.a, node2.b);
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

      // Rendering data for each castle
      castleData () {
        const store = this.$store;
        return store.getters.boardStates.filter(
          ts => 'owner' in ts
            && 'amount' in ts
            && 'territory' in ts
            && store.state.game.gameboard.castles.length > ts.territory
        ).map(ter => {
          return {
            position: ter.territory
          }
        })
      },

      // Rendering data for each army
      armyData () {
        const store = this.$store;
        return store.getters.boardStates.filter(
          ts => 'owner' in ts
            && 'amount' in ts
            && 'territory' in ts)
        .map(territoryState => {
          let color = null;
          if (territoryState.owner < 0) {
            color = -1;
          } else if (territoryState.owner < store.state.game.playerStateList.length) {
            color = store.state.game.playerStateList[territoryState.owner];
          }
          if (color !== - 1) {
            if (color !== null && 'player' in color) {
              color = color.player.settings.ordinal;
            } else {
              color = 0;
            }
          }
          return {
            size: territoryState.amount,
            color: color,
            position: territoryState.territory,
            num: territoryState.territory
          }
        });
      }
    },

    methods: {
      // ***************
      // Utility methods
      // ***************

      // Calculates the initial position & scale of the map in the viewport
      calculateInitialTransform () {
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
        const kw = (totalW - 2 * margin) / size.a;
        const kh = (totalH - 2 * margin) / size.b;
        const k = Math.min(kw, kh);
        return {
          x: (totalW - (size.a * k)) / 2,
          y: (totalH - (size.b * k)) / 2,
          scale: k
        };
      },

      // Translates the pointer mouse pointer minus the stage offset
      relativePointer (clientX, clientY, stage) {
        return {
          x: clientX - stage.getContent().offsetLeft,
          y: clientY - stage.getContent().offsetTop,
        }
      },

      // Clamps the scale according to the current bounds
      clampScale (scale) {
        return clamp(scale, this.scaleBounds.min, this.scaleBounds.max);
      },

      // Clamps the position according to newly calculated position bounds
      clampPosition (pos) {
        const bounds = this.calculatePositionBounds();
        return {
          x: clamp(pos.x, bounds.x.min, bounds.x.max),
          y: clamp(pos.y, bounds.y.min, bounds.y.max)
        }
      },

      // Calculates the position bounds according to the current scale & dimensions
      calculatePositionBounds () {
        if (exists(this.stageObj) && exists(this.$store.state.game.gameboard)) {
          const scale  = this.stageObj.scale();
          const bounds = this.stageDimensions;
          const size   = this.$store.state.game.gameboard.size;
          return {
            x: this.axisBounds(size.a * scale.x, bounds.w),
            y: this.axisBounds(size.b * scale.y, bounds.h)
          };
        } else {
          logError("StageObj or Gameboard are undefined. Ignoring");
          // default value
          return {
            x: {
              min: 0,
              max: 1
            },
            y: {
              min: 0,
              max: 1
            },
          };
        }
      },

      // Applies bounds, wrapping as necessary
      axisBounds (size, bound) {
        return (size < bound) ? {
          min: -(size / 2),
          max: bound - (size / 2)
        } : {
          min: bound / 2 - size,
          max: bound / 2
        }
      },

      // ***************
      // Event listeners
      // ***************

      // Handles territory clicks
      handleTerritoryMouseDown (index) {
        this.$emit('territory-click', index)
      },

      // Handles resize events
      handleResize () {
        if (exists(this.$refs) && exists(this.$refs.stageWrapper)) {
          this.stageDimensions = {
            w: this.$refs.stageWrapper.clientWidth,
            h: this.$refs.stageWrapper.clientHeight
          }
        } else {
          logError("StageWrapper not found. Skipping resize event");
        }
      },

      // Handles scroll wheel input (event callback)
      handleScroll (event) {
        const stage = this.stageObj;
        if (exists(stage)) {
          event.preventDefault();
          const oldScale = stage.scaleX();

          const pointer = stage.getPointerPosition();
          const startPos = {
            x: pointer.x / oldScale - stage.x() / oldScale,
            y: pointer.y / oldScale - stage.y() / oldScale,
          };

          let deltaYBounded = 0;
          if (!(event.deltaY % 1)) {
            deltaYBounded = Math.abs(Math.min(-10, Math.max(10, event.deltaY)))
          } else {
            // noinspection JSSuspiciousNameCombination
            deltaYBounded = Math.abs(event.deltaY)
          }
          const scaleBy = 1.01 + deltaYBounded / 70;
          let newScale = 0;
          if (event.deltaY > 0) {
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
        } else {
          logError("StageObj not found. Scroll events will not work");
        }
      },

      // Handles touch move input (event callback)
      handleTouchMove (event) {
        const t1 = event.touches[0];
        const t2 = event.touches[1];
        if (t1 && t2) {
          event.preventDefault();
          event.stopPropagation();
          const stage = this.stageObj;
          const touchState = this.touchState;
          if (exists(stage)) {
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
            // Update touch state
            touchState.lastDist = dist;
          } else {
            logError("StageObj not found. Touch events will not work");
          }
        }
      },

      // Handles touch end input (event callback)
      handleTouchEnd () {
        // Reset touch state
        this.touchState.lastDist = 0;
        this.touchState.point = undefined;
      },

      // Attach event listeners upon mounting
      attachEventListeners (stageRef) {
        window.addEventListener('resize', this.handleResize);
        // attach events to the stage
        if (exists(stageRef)) {
          stageRef.addEventListener('wheel',     this.handleScroll);
          stageRef.addEventListener('touchmove', this.handleTouchMove);
          stageRef.addEventListener('touchend',  this.handleTouchEnd);
        } else {
          logError("StageRef not found. Canvas events will not work");
        }
      },

      // Detach any event listeners upon being destroyed
      detachEventListeners (stageRef) {
        window.removeEventListener('resize', this.handleResize);
        if (exists(stageRef)) {
          stageRef.removeEventListener('wheel',     this.handleScroll);
          stageRef.removeEventListener('touchmove', this.handleTouchMove);
          stageRef.removeEventListener('touchend',  this.handleTouchEnd);
        }
      },

      // *********************
      // Async synchronization
      // *********************

      // Attempts to unlock the GUI side of the async initialization lock
      unlockInitialization () {
        const callback = this.initializeTransforms;
        this.$store.state.game.tryInitializeGameboardScreen(GUI_CTX, () => callback());
      },

      // Initializes transforms once information from the server is loaded
      initializeTransforms () {
        const stage            = this.stageObj;
        const initialTransform = this.calculateInitialTransform();
        const scaleBounds      = this.scaleBounds;
        if (exists(stage)) {
          // Scale
          stage.scale({
            x: initialTransform.scale,
            y: initialTransform.scale
          });
          // Translate
          stage.x(initialTransform.x);
          stage.y(initialTransform.y);
          // Update scale bounds
          scaleBounds.min = Math.min(scaleBounds.min, initialTransform.scale);
          scaleBounds.max = Math.max(scaleBounds.max, initialTransform.scale)
        } else {
          logError("StageObj not found. Canvas will not be positioned");
        }
      }
    },

    // Mounted lifecycle hook
    mounted () {
      this.$nextTick(function () {
        // initialize canvas dimensions
        this.handleResize();
        if (exists(this.$refs.stage)) {
          // load the stageObj
          this.stageObj = this.$refs.stage.getStage();
          this.attachEventListeners(this.stageObj.getContent());
          this.unlockInitialization();
        }
      });
    },

    // Destruction lifecycle hook
    beforeDestroy () {
      this.detachEventListeners();
    }
  }
</script>

<style scoped>
  /* Wrapping div */
  .stage-wrapper {
    overflow: hidden;
    height: 100%;
  }
</style>
