<template>
  <div>
    <header>
      <nav class="navbar fixed-top navbar-dark bg-dark">
        <div>
          <a class="navbar-brand" href="https://github.gatech.edu/achafos3/CS2340Sp19Team10" name="github" target="_blank" rel="noopener">
            <fa-icon :icon="['fab', 'github']" size="lg" class="repoImg pl-2"></fa-icon>
          </a>
          <p class="nav-title">
            <span class="lobby-label">Lobby </span>
            <span>{{ this.$store.state.gameId }}</span>
          </p>
        </div>
        <div class="height-fix"></div>
        <popper v-if="this.$store.state.isHost"
            trigger="hover"
            :options="{placement: 'bottom'}"
            transition="fade"
            enter-active-class='fade-enter-active'
            leave-active-class='fade-leave-active'>
          <div class="popper">
            {{ startTooltip }}
          </div>
          <button slot="reference"
              :disabled="!canStart"
              id="search_button"
              v-on:click="startGame"
              class="btn btn-primary my-2 my-sm-0 mr-2 white dark_accent">
            Start Game
          </button>
        </popper>
      </nav>
    </header>
    <main>
      <div class="light-background-container">
        <div class="light-background url-container d-flex flex-column align-items-center overflow-container">
          <div class="d-flex align-justify-center url-label">
            Get your friends to join at
          </div>
          <div class="d-flex align-justify-center url-div">
            <!-- Small screens or smaller -->
            <popper class="d-md-none"
                trigger="click"
                :options="{
                  placement: 'top',
                  modifiers: { offset: { offset: '0, -12px' }, flip: { enabled: false } } }"
                transition="fade"
                enter-active-class='fade-enter-active'
                leave-active-class='fade-leave-active'>
              <div class="popper">
                Copied to clipboard
              </div>
              <p slot="reference" v-on:click="copyUrl" class="url ml-5 mr-3">{{ url }}</p>
            </popper>
            <!-- Medium screens or larger: -->
            <p class="d-none d-md-block url">{{ url }}</p>
            <popper class="d-none d-md-block"
                trigger="click"
                :options="{placement: 'top'}"
                transition="fade"
                enter-active-class='fade-enter-active'
                leave-active-class='fade-leave-active'>
              <div class="popper">
                Copied to clipboard
              </div>
              <button slot="reference" v-on:click="copyUrl" id="copy-button">
                <fa-icon :icon="['far', 'copy']"></fa-icon>
              </button>
            </popper>
          </div>
          <div class="d-flex align-justify-center url-subtext d-md-none">
            <!-- Small screens or smaller -->
            <p>Tap the above link to copy it</p>
          </div>
        </div>
      </div>
      <player-list class="player-list-container"
          v-bind:slots="this.$store.state.settings.gameplay.maxPlayers"
          v-bind:players="this.$store.getters.players"
          v-bind:host="this.$store.state.host"
          v-bind:current="this.$store.state.current">
      </player-list>
      <new-player-form v-if="!this.$store.state.isHost"
          v-bind:colors="this.$store.state.settings.settings.colors"
          v-bind:name-regex="this.$store.state.settings.settings.nameRegex"
          v-bind:taken-colors="this.takenColors"
          v-bind:taken-names="this.takenNames"
          v-bind:min-length="this.$store.state.settings.settings.minNameLength"
          v-bind:max-length="this.$store.state.settings.settings.maxNameLength"
          v-bind:visible="showPlayerForm"
          v-on:add-player="addPlayer($event)">
      </new-player-form>
    </main>
  </div>
</template>

<script>
  import PlayerList from './PlayerList.vue';
  import Popper from 'vue-popperjs';
  import NewPlayerForm from './NewPlayerForm';
  import {
    ADD_PLAYER, SET_CURRENT,
    UPDATE_HOST, UPDATE_IS_HOST
  } from "../../store/mutation-types";

  export default {
    data: function () {
      return {
        isReady: false,
        created: false
      }
    },
    components: {
      'player-list': PlayerList,
      'popper': Popper,
      'new-player-form': NewPlayerForm
    },
    methods: {
      copyUrl: function () {
        // append a temporary element to copy the text
        const el = document.createElement('textarea');
        el.value = this.fullUrl;
        document.body.appendChild(el);
        el.select();
        document.execCommand('copy');
        document.body.removeChild(el);
      },
      addPlayer: function (event) {
        this.$store.commit(ADD_PLAYER, event);
        this.$store.commit(SET_CURRENT, event.name);
        if (this.$store.state.playersList.length === 1) {
          this.$store.commit(UPDATE_HOST, event.name);
          this.$store.commit(UPDATE_IS_HOST, true);
        }
      },
      startGame: function () {
        this.$socket.sendObj({
            _type: "controllers.RequestStartGame",
            gameId: this.$store.state.gameId,
            playerId: this.$store.state.playerId
          }
        );
      }
    },
    computed: {
      url: function () {
        // Appends the host to the gameId
        return window.location.host + "/lobby/" + this.$store.state.gameId;
      },
      fullUrl: function () {
        return window.location.protocol + "//" + this.url;
      },
      canStart: function () {
        // Whether or not there are enough players in the lobby to start
        return this.$store.state.playersList.length >= this.$store.state.settings.gameplay.minPlayers;
      },
      startTooltip: function () {
        // The tooltip to display on Start Game
        return this.canStart ?
          "Ready to start game" :
          "Not enough players: minimum " + this.$store.state.settings.gameplay.minPlayers;
      },
      showPlayerForm: function () {
        // Current is set when the request is accepted, so show the form until then
        return this.$store.state.current === "" && !this.$store.state.isHost;
      },
      takenColors: function () {
        // List of taken colors
        return this.$store.state.playersList.map((player) => player.ordinal);
      },
      takenNames: function () {
        // List of taken names
        return this.$store.state.playersList.map((player) => player.name);
      }
    }
  }
</script>

<style lang="scss">
  #copy-button {
    border-color: #362A4D;
    background-color: #00000000;
  }

  #copy-button {
    color: #362A4D;
    font-size: 20px;
    border-radius: 20px;
    width: 40px;
    height: 40px;
    text-align: center;
    opacity: 0.8;
    position: relative;
    top: 9px;
    margin-left: 20px;
  }

  .height-fix {
    min-height: 62px;
    visibility: hidden;
  }

  .nav-title {
    font-size: 28px;
    font-weight: 200;
    font-family: Roboto, sans-serif;
    margin-left: 0;
    margin-top: 4px;
    color: #F5F2F2;
    display: inline;
    position: relative;
    top: -4px;
  }

  .nav-title span:not(.lobby-label) {
    font-weight: bold;
    text-transform: uppercase;
  }

  .nav-title span.lobby-label {
    display: none;
  }

  .url-container {
    padding-top: 98px; /* +58.4 px for the nav */
    padding-bottom: 15px;
    color: #362A4D;
  }

  .player-list-container {
    margin-top: 16px;
    margin-bottom: 16px;
  }

  .url {
    font-size: 28px;
    font-weight: 100;
    font-family: Roboto Slab, serif;
    margin-bottom: 0;
  }

  .overflow-container {
    overflow: auto;
  }

  .url-label {
    font-size: 20px;
    font-family: Roboto, sans-serif;
    margin-bottom: 4px;
  }

  .url-subtext {
    font-size: 17px;
    font-family: Roboto, sans-serif;
    margin-top: -6px;
  }

  .url-div {
    margin-top: -12px;
  }

  @media(min-width: 410px) {
    .nav-title span.lobby-label {
      display: inline;
    }
    .nav-title {
      margin-left: 12px;
    }

    .url {
      font-size: 32px;
    }
  }

  // Small device or larger
  @media(min-width: 768px) {
    .url-container {
      padding-top: 128px; /* +58.4 px for the nav */
      padding-bottom: 45px;
    }

    .player-list-container {
      margin-top: 36px;
      margin-bottom: 36px;
    }

    .url {
      font-size: 38px;
    }

    .url-label {
      margin-bottom: 0;
    }
  }
</style>
