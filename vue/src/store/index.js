// noinspection ES6UnusedImports
import Vue from 'vue'
import Vuex from 'vuex'
import {
  SET_GAME_ID, SET_PLAYER_ID, UPDATE_IS_HOST,
  SOCKET_ONOPEN, SOCKET_ONCLOSE, SOCKET_ONERROR,
  ON_GAME_LOBBY_UPDATE, ON_REQUEST_REPLY, ON_BAD_PACKET,
  ON_START_GAME, ON_PING_PLAYER, ON_SEND_CONFIG,
  SET_CURRENT, START_RESPONSE_WAIT, STOP_RESPONSE_WAIT,
  TRANSITION_TO_GAME, SET_ERROR_MESSAGE, CLEAR_ERROR_MESSAGE, UPDATE_HOST, ADD_PLAYER
} from './mutation-types'
import game from './modules/game'

Vue.use(Vuex);

export default new Vuex.Store({
  modules: {
    game
  },
  state: {
    gameId: "",
    playerId: "",
    errorMessage: "",
    errorMessageDisappearing: false,
    isHost: false,
    socket: {
      isConnected: false
    },
    playersList: [],
    playerIndex: -1,
    host: "",
    current: "",
    inGameState: false,
    responseTarget: null,
    settings: {
      settings: {
        colors: [],
        territoryColors: [],
        nameRegex: "",
        playerIdLength: 0,
        minNameLength: 0,
        maxNameLength: 0,
        errorMessageTimeout: 0
      },
      gameplay: {
        minPlayers: 0,
        maxPlayers: 0
      }
    }
  },
  getters: {
    // Gets mapped list of players with their colors resolved
    players: state => {
      const resolveColor = (player) => {
        return {
          name: player.name, color: '#' + state.settings.settings.colors[player.ordinal]
        }
      };
      return state.playersList.map(resolveColor);
    }
  },
  mutations: {
    // General mutations
    [SET_GAME_ID](state, newGameId) {
      if (state.gameId === "") {
        state.gameId = newGameId;
      }
    },
    [SET_PLAYER_ID](state, newPlayerId) {
      if (state.playerId === "") {
        state.playerId = newPlayerId;
      }
    },
    [SET_CURRENT](state, newCurrent) {
      if (state.current === "") {
        state.current = newCurrent;
      }
    },
    [UPDATE_IS_HOST](state, newIsHost) {
      state.isHost = newIsHost;
    },
    [START_RESPONSE_WAIT](state, callback) {
      state.responseTarget = callback;
    },
    [STOP_RESPONSE_WAIT](state) {
      state.responseTarget = null;
    },
    [ADD_PLAYER](state, player) {
      state.playersList.push({name: player.name, ordinal: player.ordinal})
    },
    [UPDATE_HOST](state, newHost) {
      state.host = newHost;
    },

    // Socket handlers
    [SOCKET_ONOPEN](state) {
      state.socket.isConnected = true;
    },
    [SOCKET_ONCLOSE](state) {
      state.socket.isConnected = false;
      this.commit(SET_ERROR_MESSAGE,
        { message: 'The connection with the server has been terminated',
          hold: true });
    },
    [SOCKET_ONERROR](state, event) {
      this.commit(SET_ERROR_MESSAGE,
        'An error occurred: ' + event);
    },

    // Packet handlers
    [ON_GAME_LOBBY_UPDATE](state, data) {
      // Load players list
      if ('seq' in data) {
        state.playersList = data.seq;
      }
      // Load current host
      if ('host' in data) {
        if (data.host === -1) state.host = "";
        else state.host = data.seq[data.host].name;
        if (state.host !== "" && state.host === state.current) {
          this.commit(UPDATE_IS_HOST, true);
        }
      }
      // Set current if the host
      if (state.isHost && data.seq.length > 0 && state.current === "") {
        state.current = data.seq[0].name;
      }
    },
    [ON_REQUEST_REPLY](state, data) {
      if (state.responseTarget !== null) {
        state.responseTarget(data);
      } else {
        // Invalid reply
        this.commit(SET_ERROR_MESSAGE,
          ('message' in data) ? data.message : "Unknown Request Reply");
      }
    },
    [ON_BAD_PACKET](state, data) {
      this.commit(SET_ERROR_MESSAGE,
        ('message' in data) ? data.message : "Unknown Bad Packet");
    },
    [ON_START_GAME]() {
      this.commit(TRANSITION_TO_GAME);
    },
    [ON_PING_PLAYER](state, data) {
      // Send back a ping response
      data._callback({
        _type: 'controllers.PingResponse',
        gameId: state.gameId,
        playerId: state.playerId
      });
    },
    [ON_SEND_CONFIG](state, data) {
      // Load public config from the websocket
      if ('config' in data) {
        state.settings = JSON.parse(data.config);
      }
    },
    [TRANSITION_TO_GAME](state) {
      state.inGameState = true;
    },
    [SET_ERROR_MESSAGE](state, message) {
      let hold = false;
      let text = message;
      if (typeof(message) === 'object') {
        if ('message' in message) text = message.message;
        if ('hold' in message) hold = message.hold;
      }
      state.errorMessage = text;
      if (!state.errorMessageDisappearing && !hold) {
        setTimeout(() => this.commit(CLEAR_ERROR_MESSAGE),
          state.settings.settings.errorMessageTimeout);
        state.errorMessageDisappearing = true;
      }
    },
    [CLEAR_ERROR_MESSAGE](state) {
      if (state.errorMessage.length > 0) {
        state.errorMessage = "";
        state.errorMessageDisappearing = false;
      }
    }
  },
  strict: process.env.NODE_ENV !== 'production'
})
