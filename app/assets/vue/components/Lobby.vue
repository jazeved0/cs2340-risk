<template>
	<div>
		<p>Get your friends to join at http://localhost:9000/lobby/uwu</p>
		<hr>
		<player-list v-bind:slots="this.slots"
			v-bind:players="this.players"
			v-bind:host="this.host"
			v-bind:current="this.current"></player-list>
		<hr>
	</div>
</template>

<script>
	import PlayerList from './PlayerList.vue';
	export default {
		data: function() {
			return {
				isReady: false,
			 	players: [
					{ name: "joazlazer", color: "#27AE60" },
			  	{ name: "iPhish", color: "#C0392B" },
			  	{ name: "Chafos", color: "#2980B9" }
				],
			 	isHost: false,
			 	current: "Chafos",
			 	host: "joazlazer",
			 	lobbyId: "xbwe",
			 	slots: 6
			}
		},
		components: {
			'player-list': PlayerList,
		},
		methods: {
			logIsHost: function() {
				console.log(this.isHost);
			},
			changeReadyState: function() {
				this.isReady = !this.isReady;
			}
		}
	}
</script>

<style>

</style>
