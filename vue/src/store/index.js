import Vue from 'vue'
import Vuex from 'vuex'
import { SET_GAME_ID, SET_PLAYER_ID, UPDATE_IS_HOST,
		SOCKET_ONOPEN, SOCKET_ONCLOSE, SOCKET_ONERROR,
		ON_GAME_LOBBY_UPDATE, ON_REQUEST_REPLY, ON_BAD_PACKET,
		ON_START_GAME, ON_UPDATE_PLAYER_STATE, ON_PING_PLAYER,
		ON_SEND_CONFIG, SET_CURRENT } from './mutation-types'

Vue.use(Vuex)

export default new Vuex.Store({
	state: {
		gameId: "",
		playerId: "",
		isHost: false,
		socket: {
			isConnected: false
		},
		playersList: [ ],
		host: "",
		current: "",
		settings: {
			settings: {
				colors: [ ],
				nameRegex: "",
				playerIdLength: 0,
				minNameLength: 0,
				maxNameLength: 0
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
		[SET_GAME_ID] (state, newGameId) {
			if (state.gameId === "") {
				state.gameId = newGameId;
			}
		},
		[SET_PLAYER_ID] (state, newPlayerId) {
			if (state.playerId === "") {
				state.playerId = newPlayerId;
			}
		},
		[SET_CURRENT] (state, newCurrent) {
			if (state.current === "") {
				state.current = newCurrent;
			}
		},
		[UPDATE_IS_HOST] (state, newIsHost) {
			state.isHost = newIsHost;
		},

		// Socket handlers
		[SOCKET_ONOPEN] (state)  {
			state.socket.isConnected = true
		},
		[SOCKET_ONCLOSE] (state)  {
			state.socket.isConnected = false
			console.log("websocket closed")
		},
		[SOCKET_ONERROR] (state, event)  {
			console.error(state, event)
		},

		// Packet handlers
		[ON_GAME_LOBBY_UPDATE] (state, data) {
			// Load players list
			if ('seq' in data) {
				state.playersList = data.seq;
			}
			// Load current host
			if ('host' in data) {
				state.host = data.seq[data.host].name;
			}
			// Set current if the host
			if (state.isHost && data.seq.length > 0) {
				state.current = data.seq[0].name;
			}
		},
		[ON_REQUEST_REPLY] (state, data) {
			// TODO implement
		},
		[ON_BAD_PACKET] (state, data) {
			// TODO implement
		},
		[ON_START_GAME] (state, data) {
			// TODO implement
		},
		[ON_UPDATE_PLAYER_STATE] (state, data) {
			// TODO implement
		},
		[ON_PING_PLAYER] (state, data) {
			// Send back a ping response
			data._callback({
				_type: 'controllers.PingResponse',
				gameId: state.gameId,
				playerId: state.playerId
			});
		},
		[ON_SEND_CONFIG] (state, data) {
			// Load public config from the websocket
			if ('config' in data) {
				state.settings = JSON.parse(data.config);
			}
		}
	},
	strict: process.env.NODE_ENV !== 'production'
})
