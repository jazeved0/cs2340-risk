<template>
	<div class="container">
		<div class="d-flex flex-wrap">
			<player-slot class="player-slot" v-for="player in playerList" :key="player.name" v-bind="player"></player-slot>
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
				 		paddedList.push({ name: "", color: "" });
					} else {
						const player = this.players[i];
						let newPlayer = JSON.parse(JSON.stringify(player));
						if (player.name === this.current) {
							newPlayer.isCurrent = true;
						}
						if (player.name === this.host) {
							newPlayer.isHost = true;
						}
						paddedList.push(newPlayer)
					}
				}
				return paddedList;
			}
		}
	}
</script>

<style lang="scss">
	// aligns each box to take up 1/3 of the flex parent
	.player-slot {
		-webkit-box-flex: 0;
		-ms-flex: 0 0 33.333333%;
		flex: 0 0 33.333333%;
		max-width: 33.333333%;
	}
</style>