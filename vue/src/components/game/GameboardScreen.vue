<!--suppress HtmlUnknownTag -->
<template>
  <div class="gameboard d-flex flex-column">
    <tool-bar>
      <span slot="left-element">
        <!-- TODO Should not be wrapping h1 in span -->
        <h1 style="color:white">RISK</h1>
      </span>
      <div slot="middle-element" class="turn-text text-center">
        <p class="banner-text font-weight-bold">{{ getBannerText }}</p>
      </div>
      <div slot="right-element" v-if="localTurn">
        <div class="button">
          <b-button class="button-title btn btn-primary text-center my-2 my-sm-0 ml-2 mr-2 white dark_accent"
                    v-on:click="turnEvent"
                    :disabled="!turnEventEnabled">
            <div style="min-width: 80px; min-height: 34px;">
              <!--suppress XmlUnboundNsPrefix -->
              <div v-if="!showTurnEventLoading" class="p-1">{{ buttonText }}</div>
              <b-spinner v-else variant="light"/>
            </div>
          </b-button>
        </div>
      </div>
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
              @mouseout="territoryMouseOut(pathConfig.num)"
              @mousedown="territoryClick(pathConfig.num)"
          ></v-path>
        </v-layer>
        <v-layer>
          <v-army-shape v-for="army in armyData"
              :data="army"
              :key="army.num"></v-army-shape>
        </v-layer>
        <v-layer>
          <v-castle-icon v-for="castle in castleData"
              :data="castle"
              :key="castle.num"></v-castle-icon>
        </v-layer>
      </v-stage>
    </div>
    <player-info-bar class="players" :overdraw="playerInfoBarOverdraw" ref="playerInfo">
    </player-info-bar>
    <territory-assignment-modal
        class="territories"
        v-bind:visible="showAssignmentModal"
        @hide="this.hasSeenAssignments = true">
    </territory-assignment-modal>
    <attack-popup
        v-if="displayAttackingPopup"
    ></attack-popup>
    <defender-popup
        v-if="displayDefenderPopup">
    </defender-popup>
    <!-- TODO Implement once movement state is implemented -->
    <!--<movement-popup>-->
        <!--v-if="displayMovementPopup"-->
    <!--</movement-popup>-->
    <div v-show="displayDiceRoll">
    <dice-roll-modal v-if="displayDiceRoll"></dice-roll-modal>
    </div>
    <b-toast v-if="displayResultToast" title="result toast" visible="true" solid>
      {{ this.$store.state.game.attackResults[0] }} attackers were destroyed, and
      {{ this.$store.state.game.attackResults[1] }} defenders were destroyed.
    </b-toast>
    <b-alert
        show
        dismissible
        variant="info"
        fade
        class="turn-alert"
        v-if="localTurn">
      <h2 class="turn-alert-text">It's Your Turn!</h2>
      <p> {{ getInstructions }}</p>
    </b-alert>
    <end-screen-modal></end-screen-modal>
  </div>
</template>

<script>
  //import DiceRollModal from './DiceRollModal'
  import AttackPopup from './AttackPopup';
  import DefenderPopup from './DefenderPopup'
  import PlayerInfoBar from './PlayerInfoBar';
  import ArmyShape from './ArmyShape';
  import CastleIcon from './CastleIcon';
  import TerritoryAssignmentModal from './TerritoryAssignmentModal';
  import DiceRollModal from './DiceRollModal'
  import Toolbar from './../Toolbar';
  import VueKonva from 'vue-konva';
  import EndScreen from './EndScreen';
  //import MovementScreen from './MovementScreen';
  // noinspection ES6UnusedImports
  import Vue from "vue";
  import {clamp, ColorLuminance, distance, colorSaturation} from './../../util'
  import {GUI_CTX} from "../../store/modules/game/InitializeGameboardScreen";
  import {ADD_TROOPS} from  "../../store/action-types.js"
  import {SUBMIT_REINFORCEMENTS, UNSUBMIT_REINFORCEMENTS, START_RESPONSE_WAIT,
          STOP_RESPONSE_WAIT, SET_ERROR_MESSAGE, UPDATE_DEFEND_TERRITORY, UPDATE_ATTACK_TERRITORY,
          UPDATE_DEFENDING_PLAYER_INDEX, UPDATE_ATTACKING_PLAYER_INDEX} from "../../store/mutation-types.js"
  import Toasted from 'vue-toasted';

  // noinspection JSUnresolvedFunction
  Vue.use(VueKonva);
  Vue.use(Toasted);

  export default {
    components: {
      /*'dice-roll-modal': DiceRollModal,*/
      'attack-popup': AttackPopup,
      'defender-popup': DefenderPopup,
      'tool-bar': Toolbar,
      'player-info-bar': PlayerInfoBar,
      'v-army-shape': ArmyShape,
      'v-castle-icon': CastleIcon,
      'territory-assignment-modal': TerritoryAssignmentModal,
      'dice-roll-modal': DiceRollModal,
      'end-screen-modal': EndScreen
      //'movement-popup': MovementScreen,
    },
    computed: {
      attackerName: function() {
        const attackerIndex = this.$store.state.game.attackingPlayerIndex;
        return this.$store.state.game.playerStateList[attackerIndex].settings.name;
      },
      defenderName: function() {
        const defenderIndex = this.$store.state.game.defendingPlayerIndex;
        return this.$store.state.game.playerStateList[defenderIndex].settings.name;
      },
      displayDiceRoll: function() {
        const state = this.$store.state;
        const playerIndex =  this.$store.getters.getPlayerIndex;
        const turnState = this.$store.getters.playerStates[playerIndex].turnState;
        return (state.game.diceRolls.length > 0 && (playerIndex == state.game.attackingPlayerIndex || playerIndex == state.game.defendingPlayerIndex));
      },
      displayResultToast: function() {
        const state = this.$store.state;
        const playerIndex =  this.$store.getters.getPlayerIndex;
        const turnState = this.$store.getters.playerStates[playerIndex].turnState;
        console.log(state.game.diceRolls.length > 0 && !(playerIndex == state.game.attackingPlayerIndex || playerIndex == state.game.defendingPlayerIndex));
        return (state.game.diceRolls.length > 0 && !(playerIndex == state.game.attackingPlayerIndex || playerIndex == state.game.defendingPlayerIndex));
      },
      getInstructions: function() {
        const turnIndex = this.$store.state.game.turnIndex;
        const playerObj = this.$store.state.game.playerStateList[turnIndex];
        if (turnIndex === -1) {
          return "";
        }
        else if (playerObj.turnState.state === "reinforcement") {
          return "You are currently in the reinforcement phase of your turn. " +
              "Click on specific territories to add a single reinforcement unit to that territory. " +
              "After you have applied all your reinforcements, end your turn!";
        } else if (playerObj.turnState.state === "attack") {
          return "You are currently in the attacking phase of your turn. " +
              "Click on one of your territories (which has at least 2 armies on it ) to chose where you are attacking from." +
              "Click on a neutral/enemy territory, that is connected to the your attacking territory, to attack that land." +
              "Then decide how many armies to commit to your attack. You must always leave at least one army in your attacking territory!";

        }
        return "";
      },
      displayAttackingPopup: function() {
        return (this.$store.state.game.attackingTerritory !== -1) && (this.$store.state.game.defendingTerritory !== -1) && this.isInAttacking;
      },
      // displayMovementPopup: function() {
      //   return (this.$store.state.game.movingTerritoryOrigin !== -1) && (this.$store.state.game.movingTerritoryGoal !== -1) && this.isInMoving;
      // },
      displayDefenderPopup: function() {
        return this.isDefending;
      },
      buttonText: function() {
        if (this.isInReinforcement) {
          return "Assign Army";
        } else if (this.isInAttacking) {
          return "End Attacking Turn";
        } else {
          return "";
        }
      },
      getBannerText: function() {
        const turnIndex = this.$store.state.game.turnIndex;
        if (turnIndex === -1) {
          return "";
        }
        const playerObj = this.$store.state.game.playerStateList[turnIndex];
        let suffix = '';
        if (this.isInReinforcement) suffix = '; ' + (this.allocation - this.$store.state.game.placement.total) + ' troops left';
        return playerObj.player.settings.name
            + " is in their " + playerObj.turnState.state + " turn" + suffix;
      },
      localTurn: function() {
        const turnIndex = this.$store.state.game.turnIndex;
        if (turnIndex === -1) {
          return false;
        }
        return this.$store.state.current === this.$store.state.game.playerStateList[turnIndex].player.settings.name;
      },
      // #TODO is in defending
      isInReinforcement: function() {
         const turnIndex = this.$store.state.game.turnIndex;
         if (turnIndex === -1) {
           return false;
         }
         return this.localTurn && this.$store.state.game.playerStateList[turnIndex].turnState.state === 'reinforcement';
      },
      isInAttacking: function() {
        const turnIndex = this.$store.state.game.turnIndex;
        if (turnIndex === -1) {
          return false;
        }
        const result = this.$store.state.game.attackers === 0 && this.localTurn && this.$store.state.game.playerStateList[turnIndex].turnState.state === 'attack'
        if (result == true) {
          this.$store.commit(UPDATE_ATTACKING_PLAYER_INDEX, turnIndex);
        }
        return result;
      },
      isDefending: function() {
        const currentIndex = this.$store.getters.getPlayerIndex;
        if (currentIndex === -1) {
          return false;
        }
        const result = this.$store.state.game.playerStateList[currentIndex].turnState.state === 'defense'
        if (result == true) {
          this.$store.commit(UPDATE_DEFENDING_PLAYER_INDEX, currentIndex);
        }
        return result;
      },
      allocation: function() {
        const turnIndex = this.$store.state.game.turnIndex;
        if (turnIndex === -1) {
          return 0;
        }
        return this.isInReinforcement
            ? parseInt(this.$store.state.game.playerStateList[turnIndex].turnState.payload.amount)
            : 0;
      },
      highlightSelectable: function() {
        return (this.isInReinforcement && this.$store.state.game.placement.total < this.allocation)
            || (this.isInAttacking);
      },
      turnEventEnabled: function() {
        if (this.isInReinforcement) {
          return this.allocation === this.$store.state.game.placement.total
              && !this.$store.state.game.placement.submitted;
        } else return true;
      },
      showTurnEventLoading: function() {
        if (this.isInReinforcement) {
          return this.$store.state.game.placement.submitted;
        } else return false;
      },
      castleData: function() {
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
      armyData: function () {
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
      },
      pathConfigs: function () {
        const mouseOver = this.mouseOver;
        let selectable = this.selectable; // highlights all territories owned by allies
        if (this.isInAttacking && this.$store.state.game.attackingTerritory !== -1) {
          selectable = this.getAdjacentEnemyTerritories; // highlights all enemy territory surrounding the attacking territory
        }
        // else if (this.isInMoving && this.$store.state.game.originalTerritory !== -1) {
        //   selectable = this.getTerritoryMoveLocations();  // highlights all territories that can be moved from one territory
        // } else if (this.isInMoving && this.$store.state.game.originalTerritory === -1) {
        //   selectable = this.moveSelectable; // highlights all allied territories that can move troops
        // }
        let highlightSelectable = this.highlightSelectable;
        const state = this.$store.state;
        return state.game.gameboard.pathData.map(function (item, index) {
          const region = state.game.gameboard.regions.findIndex(r => r.includes(index));
          let color = 'lightgray';
          if (state.settings.settings.territoryColors.length > region) {
            color = state.settings.settings.territoryColors[region];
          }
          const resolveColor = (i) => {
            if (i === mouseOver) {
              return ColorLuminance(color, 0.15);
            } else if (highlightSelectable === true && selectable.includes(i)) {
              return colorSaturation(color, 2.5);
            } else {
              return '#' + color;
            }
          };
          return {
            x: 0,
            y: 0,
            data: item,
            fill: resolveColor(index),
            scale: {
              x: 1,
              y: 1
            },
            shadowBlur: 10,
            num: index
          };
        });
      },
      selectable: function() {
        let selectableTerritories = this.$store.getters.boardStates.filter(ter => ter.owner === this.$store.getters.getPlayerIndex);
        return selectableTerritories.map(ter => ter.territory)
      },
      moveSelectable: function() {
        const turnIndex = this.$store.state.game.turnIndex;
        let selectableTerritories = this.$store.getters.boardStates.filter(ter => ter.owner === this.$store.getters.getPlayerIndex &&
            this.$store.state.game.gameboard.territories[ter.territory].connections.filter(t => this.$store.getters.boardStates[t].owner === turnIndex).length > 0);
        return selectableTerritories.map(ter => ter.territory);
      },
      getAdjacentEnemyTerritories: function() {
        if (this.isInAttacking) {
          const attacker = this.$store.state.game.attackingTerritory;
          if (attacker !== -1) {
            const turnIndex = this.$store.state.game.turnIndex;
            console.log(this.$store.state.game.gameboard);
            console.log(attacker);
            console.log(this.$store.state.game.gameboard.territories[attacker]);
            return this.$store.state.game.gameboard.territories[attacker].connections.filter(territory => this.$store.getters.boardStates[territory].owner !== turnIndex);
          }
        }
        return [];
      },
      // TODO
      // getTerritoryMoveLocations: function() {
      //   var visited = new Set();
      //   var que = [];
      //
      //   var startTerritoryIndex = this.$store.state.game.movingTerritoryOrigin;
      //   var startTerritory = this.$store.state.game.gameboard.territories[startTerritoryIndex];
      //   const turnIndex = this.$store.state.game.turnIndex;
      //
      //   que.push(startTerritory);
      //   visited.add(startTerritoryIndex);
      //
      //   while (que.length !== 0) {
      //     var dequed = que.shift();
      //     console.log(dequed);
      //     dequed.connections.filter(territory => !visited.has(territory) && this.$store.getters.boardStates[territory].owner === turnIndex).forEach(territory => {
      //       visited.add(territory);
      //     que.push(this.$store.state.game.gameboard.territories[territory]);
      //   });
      //   }
      //   visited.delete(startTerritoryIndex);
      //   return Array.from(visited);
      // },
      waterConnectionConfigs: function () {
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
      turnEvent: function() {
        if (this.isInReinforcement) {
          this.assignArmy();
        } else {
          // do nothing
          this.endTurn();
        }
      },
      assignArmy: function() {
        const territories = this.$store.state.game.placement.territories;
        const store = this.$store;
        const packet = {
          _type: "controllers.RequestPlaceReinforcements",
          gameId: store.state.gameId,
          playerId: store.state.playerId,
          assignments: Object.keys(territories).map(key => [parseInt(key), territories[key]])
        };
        store.commit(SUBMIT_REINFORCEMENTS);
        this.$socket.sendObj(packet);
        const thisObj = this;
        store.commit(START_RESPONSE_WAIT, function (data) {
          if ('response' in data) {
            store.commit(STOP_RESPONSE_WAIT);
            store.commit(UNSUBMIT_REINFORCEMENTS);
            if (data.response === "Rejected") {
              thisObj.responseFailed(data.message);
            }
          }
        });
      },
      endTurn: function() {
        //console.log('uwu');
        const store = this.$store;
        const packet = {
          _type: "controllers.RequestEndTurn",
          gameId: store.state.gameId,
          playerId: store.state.playerId,
        };
        this.$socket.sendObj(packet);
        const thisObj = this;
        store.commit(START_RESPONSE_WAIT, function(data) {
          if ('response' in data) {
            store.commit(STOP_RESPONSE_WAIT);
            if (data.response === "Rejected") {
              thisObj.responseFailed(data.message);
            }
          }
        })
      },
      territoryMouseOver: function (num) {
        this.mouseOver = num;
      },
      territoryMouseOut: function (num) {
        if (this.mouseOver === num) this.mouseOver = -1;
      },
      resizeCanvas: function () {
        if (this.$refs && 'stageWrapper' in this.$refs) {
          // noinspection JSUnresolvedVariable
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
          x: this.axisBounds(size.a * scale.x, bounds.w),
          y: this.axisBounds(size.b * scale.y, bounds.h)
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
        const kw = (totalW - 2 * margin) / size.a;
        const kh = (totalH - 2 * margin) / size.b;
        const k = Math.min(kw, kh);
        return {
          x: (totalW - (size.a * k)) / 2,
          y: (totalH - (size.b * k)) / 2,
          scale: k
        };
      },
      territoryClick: function (num) {
        const turnIndex = this.$store.state.game.turnIndex;
        const owned = this.$store.getters.boardStates[num].owner === turnIndex;
        if (this.isInReinforcement) {
          if (this.$store.state.game.placement.total < this.allocation) {
            this.addTerritory(num);
          }
        } else if (this.isInAttacking) { // TODO don't allow selection of territories with no surrounding enemies
          if (owned && this.$store.getters.boardStates[num].amount > 1) {
            this.$store.commit(UPDATE_ATTACK_TERRITORY, num);
          } else if (!owned) {
            if (this.$store.state.game.attackingTerritory !== -1) {
              if (this.$store.state.game.gameboard.territories[this.$store.state.game.attackingTerritory].connections.includes(num)) {
                this.$store.commit(UPDATE_DEFEND_TERRITORY, num);
              }
            }
          }
        }
        // else if (this.isInMovement) {
        // TODO
        //// Lets us selects the origin movement point and selects and verifies end points for movement turn
        //   const numAlliedSurrounding = this.$store.state.game.gameboard.territories[num].connections.filter(t => this.$store.getters.boardStates[t].owner === turnIndex).length;
        //   if (owned && numAlliedSurrounding > 0) {
        //     if (this.$store.state.game.movingTerritoryOrigin === -1) {
        //       this.$store.commit(UPDATE_MOVE_ORIGIN, num);
        //     }
        //     else {
        //       var visited = new Set();
        //       var que = [];
        //
        //       var startTerritoryIndex = this.$store.state.game.movingTerritoryOrigin;
        //       var startTerritory = this.$store.state.game.gameboard.territories[startTerritoryIndex];
        //       const turnIndex = this.$store.state.game.turnIndex;
        //
        //       que.push(startTerritory);
        //       visited.add(startTerritoryIndex);
        //
        //       while (que.length !== 0) {
        //         var dequed = que.shift();
        //         if (dequed.territory === num) {
        //           this.$store.commit(UPDATE_MOVE_TARGET, num);
        //           return;
        //         } else {
        //           dequed.connections.filter(territory => !visited.has(territory) && this.$store.getters.boardStates[territory].owner === turnIndex).forEach(territory => {
        //             visited.add(territory);
        //             que.push(this.$store.state.game.gameboard.territories[territory]);
        //           });
        //         }
        //       }
        //       return;
        //     }
        //   }
        // }
      },
      addTerritory: function (num) {
        // noinspection JSIgnoredPromiseFromCall
        this.$store.dispatch(ADD_TROOPS, num);
      },
      responseFailed: function (message) {
        this.$store.commit(SET_ERROR_MESSAGE, message);
      },
    },
    data() {
      return {
        showDiceRoll: false,
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
  @import '../../assets/stylesheets/include';

  .gameboard {
    height: 100vh;
    background-color: $ocean-color;
  }

  .stage-wrapper {
    overflow: hidden;
  }

  .button {
    border-radius: 5px;
    background: #5B78BB;
  }

  .button-title {
    padding: 7px;
    color: $light-shades;
    font-family: $roboto-font;
    font-size: 20px;
  }

  @media screen and (max-width: 600px) {
    .banner-text {
      font-family: $roboto-font;
      font-size: 17px;
    }

    .button-title {
      font-size: 17px;
    }
  }

  .players {
    position: absolute;
    bottom: 0;
  }

  .turn-alert {
    width: 80%;
    position: absolute;
    top: 9%;
    left: 50%;
    transform: translate(-50%,0);
    z-index: 1000;
  }

  .territories {
    z-index: 1001;
  }

  .turn-alert-text {
    font-family: $roboto-font;
    font-size: 30px;
    text-align: center;
    width: 75%;
  }

  .turn-text {
    color: $light-shades;
    font-family: $roboto-font;
    font-size: 24px;
    padding-top: 15px;
  }
</style>