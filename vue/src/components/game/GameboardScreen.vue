<!--suppress HtmlUnknownTag -->
<template>
  <div class="gameboard d-flex flex-column">
    <!-- Top bar -->
    <tool-bar>
      <!-- Title -->
      <div slot="left-element">
        <h1 class="title-text">Risk</h1>
      </div>
      <!-- Center text -->
      <div slot="middle-element" class="turn-text text-center">
        <p class="banner-text">
          <fa-icon class="color" icon="circle" v-bind:style="{ color: bannerColor }"></fa-icon>
          <span v-html="bannerContent"></span>
        </p>
      </div>
      <!-- Turn event container (only show if local turn) -->
      <div slot="right-element" v-if="localTurn">
        <!-- Turn Event button -->
        <div class="button mb-2 mt-1 my-md-0">
          <b-button class="button-title btn btn-primary text-center m-0 my-md-1 mx-md-2 white dark_accent"
                    v-on:click="turnEvent"
                    :disabled="!turnEventEnabled">
            <div style="min-width: 80px; min-height: 34px;">
              <!-- Text/spinner -->
              <div v-if="!showTurnEventLoading" class="p-1">{{ turnEventLabel }}</div>
              <b-spinner v-else variant="light"/>
            </div>
          </b-button>
        </div>
      </div>
    </tool-bar>

    <!-- Main game canvas -->
    <div class="flex-fill">
      <game-canvas :highlight=      "highlighted"
                   :highlightColor= "highlightColor"
                   @territory-click="territoryClick"></game-canvas>
    </div>

    <!-- Bottom info bar -->
    <player-info-bar class="players" ref="playerInfo">
    </player-info-bar>

    <!-- Off screen components -->
    <territory-assignment-modal
        class="territories"
        v-bind:visible="showAssignmentModal"
        @hide="this.hasSeenAssignments = true">
    </territory-assignment-modal>

    <attack-popup
        v-if="displayAttackingPopup">
    </attack-popup>

    <defender-popup
        v-if="displayDefenderPopup">
    </defender-popup>

    <movement-popup
        v-if="displayMovementPopup">
    </movement-popup>

    <end-screen-modal
        v-if="displayEndScreenModal">
    </end-screen-modal>

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
  </div>
</template>

<script>
  import AttackPopup from './AttackPopup';
  import DefenderPopup from './DefenderPopup'
  import PlayerInfoBar from './PlayerInfoBar';
  import TerritoryAssignmentModal from './TerritoryAssignmentModal';
  import DiceRollModal from './DiceRollModal'
  import Toolbar from './../Toolbar';
  import GameCanvas from "./board/GameCanvas";
  import MovementScreen from './MovementScreen';
  import EndScreen from './EndScreen';

  import Vue from "vue";
  import Toasted from 'vue-toasted';

  import {SUBMIT_REINFORCEMENTS, UNSUBMIT_REINFORCEMENTS, START_RESPONSE_WAIT,
          STOP_RESPONSE_WAIT, UPDATE_DEFENDING_PLAYER_INDEX, UPDATE_ATTACKING_PLAYER_INDEX,
          SET_ERROR_MESSAGE, UPDATE_ATTACK_TERRITORY, UPDATE_DEFEND_TERRITORY, UPDATE_MOVE_ORIGIN, UPDATE_MOVE_TARGET} from "../../store/mutation-types.js"
  import {ADD_TROOPS} from "../../store/action-types";

  // Register toast components
  Vue.use(Toasted);

  export default {
    components: {
      'attack-popup': AttackPopup,
      'defender-popup': DefenderPopup,
      'tool-bar': Toolbar,
      'player-info-bar': PlayerInfoBar,
      'territory-assignment-modal': TerritoryAssignmentModal,
      'dice-roll-modal': DiceRollModal,
      'movement-popup': MovementScreen,
      'end-screen-modal': EndScreen,
      'game-canvas': GameCanvas
    },

    data () {
      return {
        showDiceRoll: false,
        navHeight: 62,
        hasSeenAssignments: false
      };
    },

    computed: {
      // ***********
      // UI computed
      // ***********

      // Name of the current attacker
      attackerName () {
        const attackerIndex = this.$store.state.game.attackingPlayerIndex;
        return attackerIndex !== -1
          ? this.$store.state.game.playerStateList[attackerIndex].settings.name
          : "";
      },

      // Name of the current defender
      defenderName () {
        const defenderIndex = this.$store.state.game.defendingPlayerIndex;
        return defenderIndex !== -1
          ? this.$store.state.game.playerStateList[defenderIndex].settings.name
          : "";
      },

      // Whether to display dice roll modal
      displayDiceRoll () {
        const state = this.$store.state;
        const playerIndex = this.$store.getters.getPlayerIndex;
        return (state.game.diceRolls.length > 0
          &&  (playerIndex === state.game.attackingPlayerIndex
            || playerIndex === state.game.defendingPlayerIndex));
      },

      // Whether to display the dice roll toast
      displayResultToast () {
        const state = this.$store.state;
        const playerIndex =  this.$store.getters.getPlayerIndex;
        return (state.game.diceRolls.length > 0
          && !(playerIndex === state.game.attackingPlayerIndex
            || playerIndex === state.game.defendingPlayerIndex));
      },

      // Gets the current instructions
      getInstructions () {
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

        } else if (this.isInMoving) {
            return "You are currently in the maneuvering  phase of your turn. " +
                "Click on one of your territories (which has at least two armies on it) to choose where you are moving armies from." +
                "Then click on a connected ally territory to move your armies to that territory" +
                "Then select how many armies you wish to move, ending your turn."
        }

        return "";
      },

      // Whether to display the attacker selection popup
      displayAttackingPopup () {
        return (this.$store.state.game.attackingTerritory !== -1)
          &&   (this.$store.state.game.defendingTerritory !== -1)
          && this.isInAttacking;
      },

      // Whether to display the movement selection popup
      displayMovementPopup () {
        return (this.$store.state.game.movingTerritoryOrigin !== -1)
          && (this.$store.state.game.movingTerritoryGoal !== -1)
          && this.isInMoving;
      },

      // Whether to display the defender selection popup
      displayDefenderPopup () {
        return this.isDefending;
      },

      // Whether to display the end game modal dialog
      displayEndScreenModal () {
        return this.$store.state.game.totalTurns / this.$store.state.playersList.length >= 3;
      },

      // Gets top text
      bannerContent () {
        return this.consumeTurnIndex((turnIndex, playerState) => {
          // noinspection JSUnresolvedVariable
          const nameVerb = this.localTurn
            ? `<b>You</b> are in your `
            : `<b>` + playerState.player.settings.name + `</b> is in their `;
          const turn = `<em>` + playerState.turnState.state + `</em> turn`;
          const suffix = this.isInReinforcement
            ? `; <i>` + (this.allocation - this.$store.state.game.placement.total) + ` troops left</i>`
            : '';
          return nameVerb + turn + suffix;
        }, "");
      },

      // Color of banner icon at the top of the screen
      bannerColor () {
        return this.consumeTurnIndex((turnIndex) => {
          return this.$store.getters.playerStates[turnIndex].color;
        }, "white");
      },

      // Gets the label for the turn event button
      turnEventLabel () {
        if (this.isInReinforcement) {
          return "Assign Army";

        } else if (this.isInAttacking) {
          return "End Attacking Turn";

        } else if (this.isInMoving) {
          return "End Moving Turn";

        } else {
          return "End Turn";
        }
      },

      // Whether the turn event button is enabled
      turnEventEnabled () {
        // In reinforcement, only enable before submission & once total is reached
        if (this.isInReinforcement) {
          return this.allocation === this.$store.state.game.placement.total
            && !this.$store.state.game.placement.submitted;

          // For all else, keep enabled
          // TODO is this right?
        } else if (this.isInMoving) {
          return false; // #TODO make it so we can not move
        }
        return true;
      },

      // Whether the turn event button should show a loading bar
      showTurnEventLoading () {
        // In reinforcement, only show after submission
        if (this.isInReinforcement) {
          return this.$store.state.game.placement.submitted;

          // For all else, don't show
        } else return false;
      },

      // Whether to show the territory assignment modal
      showAssignmentModal () {
        return !this.hasSeenAssignments &&
          this.$store.getters.playerStates.length > 0;
      },

      // **********************************
      // Game state / highlighting computed
      // **********************************

      // Gets whether the local player is the currently active player
      localTurn () {
        return this.consumeTurnIndex((turnIndex, playerState) => {
          // noinspection JSUnresolvedVariable
          return this.$store.state.current === playerState.player.settings.name
        });
      },

      // Gets whether the local player is in their reinforcement stage
      isInReinforcement () {
        return this.consumeTurnIndex((turnIndex, playerState) => {
          return this.localTurn && playerState.turnState.state === 'reinforcement';
        });
      },

      // Gets whether the local player is in their attacking phase
      // TODO has nasty side effects
      isInAttacking () {
        return this.consumeTurnIndex((turnIndex, playerState) => {
          const result = this.$store.state.game.attackers === 0
            && this.localTurn
            && playerState.turnState.state === 'attack';
          if (result) this.$store.commit(UPDATE_ATTACKING_PLAYER_INDEX, turnIndex);
          return result;
        });
      },

      // Gets whether the local player is in their maneuvering phase
      isInMoving: function() {
        return this.consumeTurnIndex((turnIndex, playerState) => {
          return this.localTurn
            && playerState.turnState.state === 'maneuver';
        });
      },

      // Gets whether the local player is in their defending phase
      // TODO has nasty side effects
      isDefending () {
        const currentIndex = this.$store.getters.getPlayerIndex;
        if (currentIndex === -1) return false;
        const result = this.$store.state.game.playerStateList[currentIndex].turnState.state === 'defense';
        if (result) this.$store.commit(UPDATE_DEFENDING_PLAYER_INDEX, currentIndex);
        return result;
      },

      // Gets all territory objects that the current player owns
      owned () {
        return this.$store.getters.boardStates
          .filter(ter => ter.owner === this.$store.getters.getPlayerIndex);
      },

      // Gets list of territories to highlight
      highlighted () {
        return this.consumeTurnIndex(turnIndex => {
          // Reinforcement territories (all owned) only if not fully allocated
          if (this.isInReinforcement && this.$store.state.game.placement.total < this.allocation) {
            return this.owned
              .map(ter => ter.territory);

          // Attacking territories (all owned) but need to be adjacent to enemy & have >1 troop
          } else if (this.isInAttacking && this.$store.state.game.attackingTerritory === -1) {
            return this.owned
              .filter(ter => {
                // Has more than one troop in the territory
                return ter.amount > 1;
              })
              .filter(ter => {
                // Has at least one connected territory that is not the current player
                return this.$store.state.game.gameboard.territories[ter.territory].connections
                  .some(territory => this.$store.getters.boardStates[territory].owner !== turnIndex)
              })
              .map(ter => ter.territory);

          // Attacking territories that are adjacent to the currently selected one
          } else if (this.isInAttacking && this.$store.state.game.attackingTerritory !== -1) {
            const attacker = this.$store.state.game.attackingTerritory;
              // Has to be connected and be owned by another player
              return this.$store.state.game.gameboard.territories[attacker].connections
                .filter(territory => this.$store.getters.boardStates[territory].owner !== turnIndex);

          // Highlights all territories that can be moved from one territory
          } else if (this.isInMoving && this.$store.state.game.movingTerritoryOrigin !== -1) {
            return this.getTerritoryMoveLocations;

          // Highlights all allied territories that can move troops
          } else if (this.isInMoving && this.$store.state.game.movingTerritoryOrigin === -1) {
            return this.moveSelectable; //
          }

          // default, highlight none
          return [];
        }, [])
      },

      // Gets the correct color to highlight territory borders with
      highlightColor () {
        if (this.isInAttacking
            && this.$store.state.game.attackingTerritory !== -1)
          return 'red';
        else if (this.isInMoving)
          return 'green';
        else
          return 'blue';
      },

      // Finds all territories that can be moved from one territory
      moveSelectable () {
        const turnIndex = this.$store.state.game.turnIndex;
        const currentIndex = this.$store.getters.getPlayerIndex;
        return this.$store.getters.boardStates
          // Owned by current player and has at least one connection owned by turnIndex
          .filter(ter => ter.owner === currentIndex
            && this.$store.state.game.gameboard.territories[ter.territory].connections
              .some(t => this.$store.getters.boardStates[t].owner === turnIndex))
          .map(ter => ter.territory);
      },

      // Gets the current allocation for the player
      allocation () {
        return this.consumeTurnIndex((turnIndex, playerState) => {
          return this.isInReinforcement
            ? parseInt(playerState.turnState.payload.amount)
            : 0;
        }, 0)
      },

      // Applies a breadth first search to find all possible connected territories
      getTerritoryMoveLocations () {
        const visited = new Set();
        const queue = [];

        const startTerritoryIndex = this.$store.state.game.movingTerritoryOrigin;
        const startTerritory = this.$store.state.game.gameboard.territories[startTerritoryIndex];
        const turnIndex = this.$store.state.game.turnIndex;

        queue.push(startTerritory);
        visited.add(startTerritoryIndex);

        while (queue.length !== 0) {
          const dequeued = queue.shift();
          dequeued.connections
            .filter(territory => !visited.has(territory)
              && this.$store.getters.boardStates[territory].owner === turnIndex)
                .forEach(territory => {
                  visited.add(territory);
                  queue.push(this.$store.state.game.gameboard.territories[territory]);
                });
        }
        visited.delete(startTerritoryIndex);
        return Array.from(visited);
      }
    },

    methods: {
      // Applies turn index partial function to make fully defined map
      consumeTurnIndex (consume, defaultVal) {
        if (typeof defaultVal === 'undefined') defaultVal = false;
        const turnIndex = this.$store.state.game.turnIndex;
        if (turnIndex === -1) return defaultVal;
        else return consume(turnIndex, this.$store.state.game.playerStateList[turnIndex]);
      },

      // Handles the turn event button click
      turnEvent () {
        if (this.isInReinforcement) {
          this.assignArmy();
        } else if (this.isInAttacking) {
          this.endAttacking();
        } else {
          this.endTurn();
        }
      },

      // Send an end attack request to the server
      endAttacking () {
        const store = this.$store;
        const packet = {
          _type: "controllers.RequestEndAttack",
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

      // Send an army assignment request to the server
      assignArmy () {
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

      // Send a turn end request to the server
      endTurn () {
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

      // Handles territory click
      // TODO look at
      territoryClick (num) {
        const turnIndex = this.$store.state.game.turnIndex;
        const owned = this.$store.getters.boardStates[num].owner === turnIndex;

        // Handle clicks in reinforcement
        if (this.isInReinforcement) {
          if (this.$store.state.game.placement.total < this.allocation) {
            this.addTerritory(num);
          }

        // If attacking
        } else if (this.isInAttacking) {
          // If selecting attacking territory
          if (owned && this.$store.getters.boardStates[num].amount > 1) {
            this.$store.commit(UPDATE_ATTACK_TERRITORY, num);

          // If selecting attacked territory
          } else if (!owned) {
            if (this.$store.state.game.attackingTerritory !== -1) {
              if (this.$store.state.game.gameboard.territories[this.$store.state.game.attackingTerritory].connections.includes(num)) {
                this.$store.commit(UPDATE_DEFEND_TERRITORY, num);
              }
            }
          }
        }

        // If maneuvering
        else if (this.isInMoving) {
        // Lets us selects the origin movement point and selects and verifies end points for movement turn
          const numAlliedSurrounding = this.$store.state.game.gameboard.territories[num].connections.filter(t => this.$store.getters.boardStates[t].owner === turnIndex).length;
          if (owned) {
            if (this.$store.state.game.movingTerritoryOrigin === -1) {
              if (numAlliedSurrounding > 0) {
                this.$store.commit(UPDATE_MOVE_ORIGIN, num);
              }
            }
            else {
              const visited = new Set();
              const queue = [];

              const startTerritoryIndex = this.$store.state.game.movingTerritoryOrigin;
              const startTerritory = this.$store.state.game.gameboard.territories[startTerritoryIndex];
              const turnIndex = this.$store.state.game.turnIndex;

              queue.push(startTerritory);
              visited.add(startTerritoryIndex);

              while (queue.length !== 0) {
                const dequeued = queue.shift();
                if (dequeued.connections.includes(num)) {
                  this.$store.commit(UPDATE_MOVE_TARGET, num);
                  return;
                } else {
                  dequeued.connections.filter(territory => !visited.has(territory) && this.$store.getters.boardStates[territory].owner === turnIndex).forEach(territory => {
                    visited.add(territory);
                    queue.push(this.$store.state.game.gameboard.territories[territory]);
                  });
                }
              }
            }
          }
        }
      },

      // Adds a territory to the given index
      addTerritory (num) {
        // noinspection JSIgnoredPromiseFromCall
        this.$store.dispatch(ADD_TROOPS, num);
      },

      // Handles a failed response
      responseFailed (message) {
        this.$store.commit(SET_ERROR_MESSAGE, message);
      },
    }
  };
</script>

<style lang="scss">
  @import '../../assets/stylesheets/include';

  .gameboard {
    height: 100vh;
    background-color: $ocean-color;
  }

  .button {
    border-radius: 5px;
    background: #5B78BB;
  }

  .button-title {
    color: $light-shades;
    font-family: $roboto-font;
    padding: 0.2em 7px;
    font-size: 18px;
  }

  .title-text {
    color: $light-shades;
    font-family: $roboto-slab-font;
    font-weight: 400;
    margin-bottom: 0;
    font-size: 2rem;
  }

  .players {
    position: absolute;
    bottom: 0;
  }

  .turn-alert {
    width: 80%;
    position: absolute;
    left: 50%;
    transform: translate(-50%,0);
    z-index: 1000;
    top: 80px;
    padding: 1.4rem 3rem 1.4rem 1.9rem;
    max-width: 1200px;
  }

  .turn-alert p {
    margin-bottom: 0;
  }

  .alert-info {
    color: #43365d;
    background-color: #e8daef;
    border-color: #c0bad1;
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
    font-weight: 300;
  }

  p.banner-text {
    font-size: 20px;
    background-color: rgba($light-shades, 0.1);
    padding: 0.3em 1em;
    border-radius: 100px;
    margin-bottom: 8px;
    margin-top: 8px;
  }

  p.banner-text b, p.banner-text em, p.banner-text i {
    font-weight: 600;
  }

  p.banner-text em {
    color: #d4d1ee;
    font-style: normal;
  }

  p.banner-text i {
    color: #d9c594;
    font-style: normal;
  }

  p.banner-text .color {
    transform: scale(0.7, 0.7);
    margin-right: 0.5em;
  }

  @media screen and (max-width: 600px) {
    p.banner-text {
      font-family: $roboto-font;
      font-size: 17px;
      line-height: 24px;
    }

    .button-title {
      font-size: 17px;
    }
  }
</style>
