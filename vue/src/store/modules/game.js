// noinspection ES6UnusedImports
import Vue from 'vue'
import Vuex from 'vuex'
import {ON_SEND_GAMEBOARD, ON_UPDATE_PLAYER_STATE} from '.././mutation-types'

Vue.use(Vuex);

export default {
  state: {
    playerStateList: [],
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
    hasCurrentTurn: false, //TODO decide whether or not this is needed
    armyAmount: 0 //this one too
  },
  mutations: {
    [ON_UPDATE_PLAYER_STATE](state, data) {
      if ('seq' in data) {
        state.playerStateList = data.seq;
        const index = this.getters.getPlayerIndex;
        if (index >= 0) {
          state.armyAmount = data.seq[index].units.size;
        }
      }
    },
    [ON_SEND_GAMEBOARD](state, data) {
      if ('gameboard' in data) {
        state.gameboard.nodeCount = data.gameboard.nodeCount;
        state.gameboard.pathData = data.gameboard.pathData;
        state.gameboard.iconData = data.gameboard.iconData;
        state.gameboard.centers = data.gameboard.centers;
        state.gameboard.regions = data.gameboard.regions;
        state.gameboard.waterConnections = data.gameboard.waterConnections;
        state.gameboard.territories = data.gameboard.territories;
        state.gameboard.size = data.gameboard.size;
      }
    }
  },
  getters: {
    playerStates(state, getters, rootState) {
      const resolveMapping = (player, index) => {
        return {
          name: player.settings.name,
          color: '#' + rootState.settings.settings.colors[player.settings.ordinal],
          armies: player.units.size,
          turnOrder: index,
          currentTurn: player.settings.ordinal === 0
        }
      };
      return state.playerStateList.map(resolveMapping);
    },
    getPlayerIndex(state, getters, rootState) {
      if (rootState.current !== "") {
        for (var i = 0; i < rootState.playersList.length; i++) {
          if (rootState.playersList[i].name === rootState.current) {
            return i;
          }
        }
      }
      return -1;
    }
  }
}