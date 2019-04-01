// noinspection ES6UnusedImports
import Vue from 'vue';
import Vuex from 'vuex';
import {ON_SEND_GAMEBOARD, ON_UPDATE_PLAYER_STATE, ON_UPDATE_BOARD_STATE,
        INCREMENT_TROOP, SUBMIT_REINFORCEMENTS, UNSUBMIT_REINFORCEMENTS} from '.././mutation-types';
import {ADD_TROOPS} from '.././action-types';
import {initializeGameboardScreen, NETWORK_CTX} from "./game/InitializeGameboardScreen";

Vue.use(Vuex);

export default {
  state: {
    playerStateList: [],
    boardStateList: [],
    turnIndex: -1,
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
    [ON_UPDATE_PLAYER_STATE](state, data) {
      if ('seq' in data) {
        state.playerStateList = data.seq;
      }
      if ('turn' in data) {
        state.turnIndex = data.turn // index of current turn
      }
    },
    [ON_SEND_GAMEBOARD](state, data) {
      if ('gameboard' in data) {
        state.gameboard.nodeCount = data.gameboard.nodes.length;
        state.gameboard.regions = data.gameboard.regions;
        state.gameboard.waterConnections = data.gameboard.waterConnections;
        state.gameboard.size = data.gameboard.size;
        // noinspection JSUnresolvedVariable
        state.gameboard.territories = data.gameboard.nodes.map((n) => n.dto);
        state.gameboard.pathData = data.gameboard.nodes.map((n) => n.path);
        // noinspection JSUnresolvedVariable
        state.gameboard.iconData = data.gameboard.nodes.map((n) => n.iconPath);
        state.gameboard.centers = data.gameboard.nodes.map((n) => n.center);
        state.gameboard.castles = data.gameboard.nodes.filter((n) => 'castle' in n.dto).map((n) => n.dto.castle)
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
    [SUBMIT_REINFORCEMENTS](state) {
      state.placement.submitted = true;
    },
    [UNSUBMIT_REINFORCEMENTS](state) {
      state.placement.submitted = false;
    }
  },
  getters: {
    playerStates(state, getters, rootState) {
      const resolveMapping = (playerState, index) => {
        if ('player' in playerState) {
          return {
            name: playerState.player.settings.name,
            color: '#' + rootState.settings.settings.colors[playerState.player.settings.ordinal],
            armies: playerState.units.size,
            turnOrder: index,
            currentTurn: playerState.player.settings.ordinal === 0
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
