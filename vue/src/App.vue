<template>
  <div id="app">
    <lobby v-if="showLobby"></lobby>
    <gameboard v-else></gameboard>
    <div id="error-wrapper">
      <b-alert v-if="this.$store.state.errorMessage.length > 0"
          class="mx-2 mx-sm-auto"
          show
          variant="danger"
          id="error-button"
          fade>
        <h6 class="alert-heading">Error!</h6>
        <p>
          {{this.$store.state.errorMessage}}
        </p>
      </b-alert>
    </div>
  </div>
</template>

<script>
  // noinspection ES6UnusedImports
  import Vue from 'vue'
  import Lobby from './components/lobby/LobbyScreen.vue'
  import store from './store'
  import {getCookie, pascalToUnderscore} from './util.js'
  import VueNativeSock from 'vue-native-websocket'
  import {SET_GAME_ID, SET_PLAYER_ID, UPDATE_IS_HOST} from './store/mutation-types'

  // Initialize store
  const slash = document.URL.lastIndexOf('/');
  const gameId = document.URL.substr(slash + 1);
  store.commit(SET_GAME_ID, gameId);
  const isHost = document.URL.charAt(slash - 1) === "t";
  if (isHost) store.commit(UPDATE_IS_HOST, isHost);
  const playerId = getCookie('playerId');
  store.commit(SET_PLAYER_ID, playerId);

  // Replace the URL
  window.history.replaceState('', 'Lobby', '/lobby/' + store.state.gameId);

  // Initialize the websocket
  const protocol = location.protocol === 'http:' ? 'ws:' : 'wss:';
  const webSocketUrl = protocol + '//' + document.location.host + '/ws/' +
    store.state.gameId + '/' + store.state.playerId;
  // Open the websocket and set it to propagate message events to the store
  Vue.use(VueNativeSock, webSocketUrl, {
    store: store,
    format: 'json',
    // Handler when messages are received
    passToStoreHandler: function (eventName, event) {
      if (!eventName.startsWith('SOCKET_')) {
        return
      }
      let target = eventName.toUpperCase();
      let msg = event;
      if ('data' in event) {
        // If this was a data, ensure it was JSON and has the '_type'
        msg = JSON.parse(event.data);
        if (!('_type' in msg)) {
          return;
        }
        // Transform the target to a packet-specific format
        // ex. packet controllers.PingPlayer would become ON_PING_PLAYER
        const packetType = msg._type.substring(msg._type.lastIndexOf('.') + 1);
        target = "ON_" + pascalToUnderscore(packetType).toUpperCase();
        // Attach a _callback to the message
        msg._callback = (obj) => {
          event.target.sendObj(obj);
        };
      }
      this.store['commit'](target, msg);
    }
  });

  const gameboardFactory = () => ({
    // Use a dynamic import to flag webpack to compile into separate modules
    component: import('./components/game/GameboardScreen.vue'),
    loading: {
      template: `
        <b-spinner class="loading-circle" variant="light"></b-spinner>
      `
    },
    error: {
      template: `
        <b-alert class="mx-3 mx-auto" show variant="danger" fade>
          <h6 class="alert-heading">Error!</h6>
          <p>The game couldn't be loaded properly</p>
        </b-alert>
      `
    },
    delay: 200,
    timeout: 3000
  });

  export default {
    name: 'app',
    components: {
      'lobby': Lobby,
      'gameboard': gameboardFactory
    },
    computed: {
      showLobby() {
        return !this.$store.state.inGameState;
      }
    },
    // Pass the store down the virtual DOM tree
    store
  }
</script>

<style lang="scss">
  #app {
    min-height: 100%;
  }

  #error-wrapper {
    position: absolute;
    top: 2%;
    width: 100%;
    z-index: 10000;
  }

  #error-button {
    background-color: #D9534F;
    border-color: #D9534F;
    color: white;
  }

  @media(min-width: 576px) {
    #error-button {
      width: 80%;
    }
  }

  @media(min-width: 768px) {
    #error-button {
      width: 60%;
    }
  }

  @media(min-width: 992px) {
    #error-button {
      width: 35%;
    }
  }

  .loading-circle {
    position:absolute;
    top:50%;
    left:50%;
    padding:15px;
    -ms-transform: translateX(-50%) translateY(-50%);
    -webkit-transform: translate(-50%,-50%);
    transform: translate(-50%,-50%);
  }
</style>