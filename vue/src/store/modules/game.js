// noinspection ES6UnusedImports
import Vue from 'vue'
import Vuex from 'vuex'
import {ON_UPDATE_PLAYER_STATE} from '.././mutation-types'

Vue.use(Vuex);

export default {
  state: {
    playerStateList: [],
    // TODO sample data, load from websocket
    territoryPathData: [
      'M74.49 81.63 46.93 325.63 290.49 338.96 284.71 123.41 122.93 239.85z',
      'M42.04 117.19 60.27 37.63 83.38 37.63 136.27 198.52 192.71 162.07 131.82 1.19 0.71 10.96z',
      'm209.6 152.3l-58.22-151.11s111.56-8 81.78 30.67 21.33 44.55 21.33 44.55l22.22 27.9-67.11 47.99z'
    ],
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
  },
  getters: {
    playerStates(state, getters, rootState) {
      const resolveMapping = (player, index) => {
        return {
          name: player.settings.name,
          color: '#' + rootState.settings.settings.colors[player.settings.ordinal],
          armies: player.units.size,
          turnorder: index,
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