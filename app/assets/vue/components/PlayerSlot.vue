<template>
	<div>
		<div v-if="isEmpty" class="player-slot-outer placeholder">
			<div>
			</div>
		</div>
		<div v-else class="player-slot-outer" v-bind:class="{ current: isCurrent }">
			<div class="player-slot-inner">
				<i class="fas fa-circle" v-bind:style="{color: color}"></i>
				<p class="d-inline">{{ name }}</p>
				<i v-if="isHost" class="fas fa-crown"></i>
			</div>
		</div>
	</div>
</template>

<script>
	export default {
		props: [
			'isEmpty', 'isHost', 'color', 'name', 'isCurrent'
		]
	}
</script>

<style lang="scss">
.player-slot-outer {
	padding: 12px 20px 12px 30px;
	margin: 12px;
	border-style: solid;
	border-radius: 8px;
	border-width: 2px;
	border-color: #FFFFFF00;
}
.player-slot-outer.current {
	background-color: #D9B34F0A;
	border-style: dashed;
	border-width: 2px;
	border-color: #D9B34FB0;
}
.player-slot-outer.placeholder {
	background-image: url("/assets/images/stripes.svg");
	background-size: 250px;
}
.player-slot-outer div {
	color: #333333;
	font-size: 28px;
	font-family: Roboto Slab, serif;
	min-height: 42px;
}
.player-slot-outer div i {
	position: relative;
	font-size: 15px;
}
.player-slot-outer div i.fa-circle {
	top: -2px;
	margin-right: 8px;
}
.player-slot-outer div i.fa-crown {
	top: -4px;
	text-shadow: 0 0 5px #FFFFFF30;
	margin-left: 8px;
	color: #D9B34F;
}
</style>