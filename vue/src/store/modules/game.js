// noinspection ES6UnusedImports
import Vue from 'vue';
import Vuex from 'vuex';
import {ON_SEND_GAMEBOARD, ON_UPDATE_PLAYER_STATE, ON_UPDATE_BOARD_STATE} from '.././mutation-types';
import {initializeGameboardScreen, NETWORK_CTX} from "./game/InitializeGameboardScreen";

Vue.use(Vuex);

export default {
  state: {
    playerStateList: [],
    boardStateList: [],
    gameboard: {
      nodeCount: 0,
      pathData: [],
      iconData: [],
      waterConnections: [],
      centers: [],
      regions: [],
      territories: [],
      size: []
    },
    playerInfoCard: {
      w: 320,
      h: 200
    },
    tryInitializeGameboardScreen: initializeGameboardScreen
  },
  mutations: {
    [ON_UPDATE_PLAYER_STATE](state, data) {
      if ('seq' in data) {
        state.playerStateList = data.seq;
        // TODO Update the current turn
        //  data.turn // index of current turn
        // TODO Update the initial allocation if in reinforcement
        //  data.seq[n].turnState.state === "reinforcement"
        //  data.seq[n].turnState.payload.amount // initial allocation
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
        // TODO Add castle parsing & bind to GameboardScreen
        //  data.gameboard.nodes.filter((n) => 'castle' in n.dto).map((n) => n.dto.castle)
        //  // gives [{ a: float, b: float }, ...]
      }
      initializeGameboardScreen(NETWORK_CTX);
    },
    [ON_UPDATE_BOARD_STATE](state, data) {
      if ('armies' in data) {
        state.boardStateList = data.armies;
      }
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
        for (let i = 0; i < rootState.playersList.length; i++) {
          if (rootState.playersList[i].name === rootState.current) {
            return i;
          }
        }
      }
      return -1;
    },
    boardStates(state) {
      const stateMap = {};
      state.boardStateList.forEach((state) => stateMap[state[0]] = state[1]);
      const resolveMapping = (territory, index) => {
        if (index.toString() in stateMap) {
          const stateArr = stateMap[index.toString()];
          return {
            territory: index,
            amount: stateArr[0],
            owner: stateArr[1]
          };
        } else return {};
      };
      return state.gameboard.territories.map(resolveMapping).filter(obj => obj !== {});
    }
  }
};
