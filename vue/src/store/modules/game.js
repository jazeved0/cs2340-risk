// noinspection ES6UnusedImports
import Vue from 'vue'
import Vuex from 'vuex'
import { ON_UPDATE_PLAYER_STATE, ON_SEND_GAMEBOARD } from '.././mutation-types'

Vue.use(Vuex);

export default {
  state: {
    playerStateList: [ ],
    territoryPathData: [ ],
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
      // TODO load connections
      if ('gameboard' in data) {
        if ('pathData' in data.gameboard) {
          state.territoryPathData = data.gameboard.pathData;
        }
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