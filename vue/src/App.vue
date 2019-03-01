<template>
	<div id="app">
		<Lobby></Lobby>
	</div>
</template>

<script>
	import Vue from 'vue'
	import Lobby from './components/Lobby.vue'
	import store from './store'
	import {getCookie, pascalToUnderscore} from './util.js'
	import VueNativeSock from 'vue-native-websocket'
	import {SET_GAME_ID, UPDATE_IS_HOST, SET_PLAYER_ID} from './store/mutation-types'

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
	const webSocketUrl = 'ws://' + document.location.host + '/ws/' +
			store.state.gameId + '/' + store.state.playerId;
	// Open the websocket and set it to propagate message events to the store
	Vue.use(VueNativeSock, webSocketUrl, {
		store: store,
		format: 'json',
		// Handler when messages are received
		passToStoreHandler: function (eventName, event) {
			if (!eventName.startsWith('SOCKET_')) { return }
			let target = eventName.toUpperCase();
			let msg = event;
			if ('data' in event) {
				// If this was a data, ensure it was JSON and has the '_type'
				msg = JSON.parse(event.data);
				if (!('_type' in msg)) { return; }
				// Transform the target to a packet-specific format
				// ex. packet controllers.PingPlayer would become ON_PING_PLAYER
				const packetType = msg._type.substring(msg._type.lastIndexOf('.') + 1);
				target = "ON_" + pascalToUnderscore(packetType).toUpperCase();
				// Attach a _callback to the message
				const callback = (obj) => { event.target.sendObj(obj); };
				msg._callback = callback;
			}
			this.store['commit'](target, msg);
		}
	});
				
	export default {
		name: 'app',
		components: {
			Lobby
		},
		// Pass the store down the virtual DOM tree
		store
	}
</script>

<style lang="scss">
	@import './assets/stylesheets/app.css';
</style>
