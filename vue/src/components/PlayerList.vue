<template>
	<div class="container">
		<div class="d-flex flex-wrap">
			<player-slot class="col-12 col-md-6 col-lg-4 player-slot" v-for="player in playerList" :key="player.num" v-bind="player"></player-slot>
		</div>
	</div>
</template>

<script>
	import PlayerSlot from './PlayerSlot.vue';
	export default {
		name: "player-list",
		props: {
		  'players': Array,
		  'host': String,
		  'current': String,
		  'slots': Number
	 	},
		components: {
			'player-slot': PlayerSlot
		},
		computed: {
			playerList: function() {
			 	let paddedList = [];
				let i;
				for (i = 0; i < this.slots; i++) {
					if (i >= this.players.length) {
				 		paddedList.push({ name: "", color: "", num: i });
					} else {
						const player = this.players[i];
						let newPlayer = JSON.parse(JSON.stringify(player));
						if (player.name === this.current) {
							newPlayer.isCurrent = true;
						}
						if (player.name === this.host) {
							newPlayer.isHost = true;
						}
				 		newPlayer.num = i;
						paddedList.push(newPlayer)
					}
				}
				return paddedList;
			}
		}
	}
</script>

<style lang="scss">
	.player-slot {
		padding: 0!important;
	}
</style>