// noinspection ES6UnusedImports
import Vue from 'vue';
import Vuex from 'vuex';
import {ON_SEND_GAMEBOARD, ON_UPDATE_PLAYER_STATE, ON_UPDATE_BOARD_STATE,
        INCREMENT_TROOP, SUBMIT_REINFORCEMENTS, UNSUBMIT_REINFORCEMENTS,
        UPDATE_ATTACK_TERRITORY, UPDATE_DEFEND_TERRITORY, RESET_ATTACK,
        UPDATE_MOVE_ORIGIN, UPDATE_MOVE_TARGET, CLEAR_PLACEMENT} from '.././mutation-types';
 import {ADD_TROOPS} from '.././action-types';
import {initializeGameboardScreen, NETWORK_CTX} from "./game/InitializeGameboardScreen";
import {seqStringToArray, specialSeqToArray} from '../.././util.js'
import {UPDATE_ATTACKERS, UPDATE_DEFENDERS, UPDATE_DEFENDING_PLAYER_INDEX, UPDATE_ATTACKING_PLAYER_INDEX} from "../mutation-types";

Vue.use(Vuex);

export default {
  state: {
    playerStateList: [],
    boardStateList: [],
    turnIndex: -1,
    totalTurns: 0,
    attackingTerritory: -1,
    defendingTerritory: -1,
    movingTerritoryOrigin: -1,
    movingTerritoryGoal: -1,
    defendingPlayerIndex: -1,
    attackingPlayerIndex: -1,
    attackers: 0,
    defenders: 0,
    diceRolls: [],
    attackResults: [],
    gameboard: {
      nodeCount: 0,
      pathData: [],
      iconData: [],
      waterConnections: [],
      centers: [],
      castles: [],
      regions: [],
      territories: [],
      size: []
    },
    playerInfoCard: {
      w: 320,
      h: 200
    },
    placement: {
      total: 0,
      territories: {},
      submitted: false
    },
    tryInitializeGameboardScreen: initializeGameboardScreen
  },
  mutations: {
    [UPDATE_ATTACKING_PLAYER_INDEX](state, newAttackingPlayerIndex) {
      state.attackingPlayerIndex = newAttackingPlayerIndex;
    },
    [UPDATE_DEFENDING_PLAYER_INDEX](state, newDefendingPlayerIndex) {
      state.defendingPlayerIndex = newDefendingPlayerIndex;
    },
    [UPDATE_ATTACK_TERRITORY](state, territory) {
      state.attackingTerritory = territory;
    },
    [UPDATE_DEFEND_TERRITORY](state, territory) {
      state.defendingTerritory = territory;
    },
    [UPDATE_MOVE_TARGET](state, territory) {
      state.movingTerritoryGoal = territory;
    },
    [UPDATE_MOVE_ORIGIN](state, territory) {
      state.movingTerritoryOrigin = territory;
    },
    [ON_UPDATE_PLAYER_STATE](state, data) {
      if ('seq' in data) {
        state.playerStateList = data.seq;
        let attackFound = false;
        let attackResultFound = false;
        let length = 0;
        for (let i = 0; i < state.playerStateList.length; i++) {
          if ('turnState' in state.playerStateList[i]) {
            let turnState = state.playerStateList[i].turnState;
            if ('payload' in turnState) {
              if ('attack' in turnState.payload) {
                attackFound = true;
                let attack = seqStringToArray(turnState.payload.attack);
                state.attackingTerritory = attack[0];
                state.defendingTerritory = attack[1];
                state.attackers = attack[2];
                length = attack.length;
                if (attack.length === 4){
                  state.defenders = attack[3];
                }
              }
              if ('result' in turnState.payload) {
                attackResultFound = true;
                let result = specialSeqToArray(turnState.payload.result);
                state.diceRolls = result[0];
                state.attackResults = [result[1], result[2]];
              }
            }
          }
        }
        if (!attackFound) {
          state.attackingTerritory = -1;
          state.defendingTerritory = -1;
          state.attackers = 0;
        } else if (length < 4) {
          state.defenders = 0;
        }
        if (!attackResultFound) {
          state.diceRolls = [];
          state.attackResults = [];
        }

      }
      if ('turn' in data) {
        if (state.turnIndex !== data.turn) {
          state.totalTurns++;
        }
        state.turnIndex = data.turn // index of current turn
      }
    },
    [ON_SEND_GAMEBOARD](state, data) {
      if ('gameboard' in data) {
        state.gameboard.nodeCount        = data.gameboard.nodes.length;
        state.gameboard.regions          = data.gameboard.regions;
        state.gameboard.waterConnections = data.gameboard.waterConnections;
        state.gameboard.size             = data.gameboard.size;

        state.gameboard.territories      = data.gameboard.nodes.map(n => n.dto);
        state.gameboard.pathData         = data.gameboard.nodes.map(n => n.path);
        state.gameboard.iconData         = data.gameboard.nodes.map(n => n.iconPath);
        state.gameboard.centers          = data.gameboard.nodes.map(n => n.center);
        state.gameboard.castles          = data.gameboard.nodes
          .map   ((n, i) => { return {
            elem:  n,
            index: i
          };})
          .filter(n => 'castle' in n.elem.dto)
          .map   (n => n.index);
      }
      initializeGameboardScreen(NETWORK_CTX);
    },
    [ON_UPDATE_BOARD_STATE](state, data) {
      if ('armies' in data) {
        state.boardStateList = data.armies;
      }
    },
    [INCREMENT_TROOP](state, index) {
      const oldTerritory = state.boardStateList[index];
      const newTerritory = [oldTerritory[0], [oldTerritory[1][0] + 1, oldTerritory[1][1]]];
      state.boardStateList.splice(index, 1, newTerritory);
      const key = oldTerritory[0].toString();
      if (key in state.placement.territories) {
        ++state.placement.territories[key];
      } else {
        state.placement.territories[key] = 1;
      }
      ++state.placement.total;
    },
    [CLEAR_PLACEMENT](state) {
      state.placement.territories = {};
      state.placement.total = 0;
    },
    [SUBMIT_REINFORCEMENTS](state) {
      state.placement.submitted = true;
    },
    [UNSUBMIT_REINFORCEMENTS](state) {
      state.placement.submitted = false;
    },
    [RESET_ATTACK](state){
      state.attackingTerritory = -1;
      state.defendingTerritory = -1;
      state.attackers = 0;
      state.defenders = 0;
      state.diceRolls = [];
      state.attackResults = [];
    },
    [UPDATE_ATTACKERS](state, amount){
      state.attackers = amount;
    },
    [UPDATE_DEFENDERS](state, amount){
      state.defenders = amount;
    }
  },
  getters: {
    playerStates(state, getters, rootState) {
      const resolveMapping = (playerState, index) => {
        if ('player' in playerState && 'turnState' in playerState) {
          return {
            name: playerState.player.settings.name,
            color: '#' + rootState.settings.settings.colors[playerState.player.settings.ordinal],
            armies: playerState.units.size,
            turnOrder: index,
            currentTurn: playerState.player.settings.ordinal === 0,
            turnState: playerState.turnState.state
          };
        } else return {};
      };
      return state.playerStateList.map(resolveMapping);
    },
    getPlayerIndex(state, getters, rootState) {
      if (rootState.current !== "") {
        for (let i = 0; i < state.playerStateList.length; i++) {
          if (state.playerStateList[i].player.settings.name === rootState.current) {
            return i;
          }
        }
      }
      return -1;
    },
    boardStates(state) {
      // territory index -> owned army
      const stateMap = {};
      // Turn matrix sub-arrays into key-value mappings
      state.boardStateList.forEach((state) => stateMap[state[0]] = state[1]);
      const resolveMapping = (territory, index) => {
        // ensure key is in map
        if (index.toString() in stateMap) {
          // get OwnedArmy subArray; format of [amount, player index]
          const stateArr = stateMap[index.toString()];
          return {
            territory: index,
            amount: stateArr[0],
            owner: stateArr[1]
          };
        } else return {};
      };
      // apply mapping function and then filter by those that succeeded
      return state.gameboard.territories.map(resolveMapping).filter(obj => obj !== {});
    },
    winnerIndex(state, getters) {
      const playerStates = getters.playerStates;
      if (playerStates.length === 0) return -1;
      else {
        const max = playerStates.reduce((prev, current) => (prev.armies > current.armies) ? prev : current);
        return playerStates.indexOf(max);
      }
    }
  },
  actions: {
    [ADD_TROOPS]({ commit, getters, state}, territoryNumber) {
      const territoryFilter = state.boardStateList.filter(value => value[0] === territoryNumber);
      if (territoryFilter.length > 0) {
        const territory = territoryFilter[0];
        const territoryIndex = state.boardStateList.indexOf(territory);
        const owner = territory[1][1];
        if (owner === getters.getPlayerIndex) {
          commit(INCREMENT_TROOP, territoryIndex);
        }
      }
    }
  }
};
