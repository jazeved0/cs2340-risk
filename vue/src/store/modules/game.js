import Vue from 'vue'
import Vuex from 'vuex'
import {ON_UPDATE_PLAYER_STATE} from '.././mutation-types'

Vue.use(Vuex);

export default {
  state: {
    playerStateList: [ ],
    hasCurrentTurn: false, //TODO decide whether or not this is needed
    armyAmount: 0 //this one too
  },
  mutations: {
    [ON_UPDATE_PLAYER_STATE] (state, data) {
      if ('seq' in data) {
        state.playerStateList = data.seq;
        console.log(data.seq);
        const index = state.getPlayerIndex();
        console.log(index);
        if (index >= 0) {
          state.armyAmount = data.seq[index].units.size;
          console.log(state.armyAmount);
        }
      }
    },
  },
  getters: {
    playerStates(state, getters, rootState) {
      const resolveMapping = (player) => {
        return {
          name: player.settings.name,
          color: '#' + rootState.settings.settings.colors[player.settings.ordinal],
          armies: player.units.size,
          turnorder: player.settings.ordinal,
          currentTurn: player.settings.ordinal === 0
        }
      };
      const temp = [
        {
          settings: {
            name: "Andrew",
            ordinal: 0
          },
          units: {
            size: 5
          }
        },
        {
          settings: {
            name: "Joseph",
            ordinal: 1
          },
          units: {
            size: 4
          }
        },
        {
          settings: {
            name: "Tommy",
            ordinal: 2
          },
          units: {
            size: 3
          }
        },
        {
          settings: {
            name: "Patrick",
            ordinal: 3
          },
          units: {
            size: 2
          }
        },
        {
          settings: {
            name: "Julian",
            ordinal: 4
          },
          units: {
            size: 1
          }
        },
        {
          settings: {
            name: "Ur Mom",
            ordinal: 5
          },
          units: {
            size: 0
          }
        },
      ];
      //return state.playerStateList.map(resolveMapping);
      return temp.map(resolveMapping);
    },
    getPlayerIndex (state, getters, rootState) {
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