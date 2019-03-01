<template>
	<div>
		<header>
			<nav class="navbar fixed-top navbar-dark bg-dark">
				<div>
					<a class="navbar-brand" href="https://github.gatech.edu/achafos3/CS2340Sp19Team10" target="_blank" rel="noopener">
						<i class="fab fa-github repoImg pl-2"></i>
					</a>
					<p class="nav-title">Lobby</p>
				</div>
				<div class="height-fix"></div>
				<button v-if="this.$store.state.isHost" id="search_button" v-on:click="beginGame" class="btn btn-primary my-2 my-sm-0 mr-2 white dark_accent">Start Game</button>
			</nav>
		</header>
		<main>
			<div class="light-background-container">
				<div class="light-background url-container d-flex flex-column align-items-center">
					<div class="d-flex align-justify-center url-label">
						Get your friends to join at
					</div>
					<div class="d-flex align-justify-center url">
						<p id="url-text">{{ url }}</p>
						<popper trigger="click" :options="{placement: 'top'}">
							<div class="popper">
								Copied to clipboard
							</div>
							<button slot="reference" v-on:click="copyUrl" id="copy-button">
								<i class="far fa-copy"></i>
							</button>
						</popper>
					</div>
				</div>
			</div>
			<player-list class="player-list-container"
					v-bind:slots="this.$store.state.settings.gameplay.maxPlayers"
					v-bind:players="this.$store.getters.players"
					v-bind:host="this.$store.state.host"
					v-bind:current="this.$store.state.current">
			</player-list>
			<center>
				<new-player-form v-if="!created && !this.$store.state.isHost" v-on:add-slot="addSlot"></new-player-form>
			</center>
		</main>
	</div>
</template>

<script>
	import PlayerList from './PlayerList.vue';
	import Popper from 'vue-popperjs';
	import NewPlayerForm from './NewPlayerForm';

	export default {
		data: function() {
			return {
				isReady: false,
				created: false
			}
		},
		components: {
			'player-list': PlayerList,
			'popper' : Popper,
			'new-player-form': NewPlayerForm
		},
		methods: {
			copyUrl: function () {
				// append a temporary element to copy the text
				const el = document.createElement('textarea');
				el.value = this.url;
				document.body.appendChild(el);
				el.select();
				document.execCommand('copy');
				document.body.removeChild(el);
			},
			addSlot: function() {
				// TODO implement
			},
			beginGame: function() {
				// TODO implement
			}
		},
		computed: {
			url: function() {
				// Appends the host to the gameId
				return window.location.host + "/lobby/" + this.$store.state.gameId;
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
		top: -6px;
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
		margin-left: 20px;
		color: #F5F2F2;
		display: inline;
		position: relative;
		top: -4px;
	}
	.url-container {
		padding-top: 128px; /* +58.4 px for the nav */
		padding-bottom: 45px;
		color: #362A4D;
	}
	.player-list-container {
		margin-top: 36px;
	}
	.url-label {
		font-size: 20px;
		font-family: Roboto, sans-serif;
	}
	.url {
		font-size: 42px;
		font-weight: 100;
		font-family: Roboto Slab, serif;
		margin-top: -12px;
	}
	#player-form {
		margin: auto
	}
</style>
