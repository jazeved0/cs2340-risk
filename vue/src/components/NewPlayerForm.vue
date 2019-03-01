<template>
	<div>
			<p class="prompt">Name:</p> <input type="text" id="name_input"><br>
			<p class="prompt">Color:</p>
			<input type="radio" name="color" value='0' class="color_input"> <p class="color_label">0</P>
			<input type="radio" name="color" value='1' class="color_input"> <p class="color_label">1</P>
			<input type="radio" name="color" value='2' class="color_input"> <p class="color_label">2</P>
			<input type="radio" name="color" value='3' class="color_input"> <p class="color_label">3</P>
			<input type="radio" name="color" value='4' class="color_input"> <p class="color_label">4</P>
						<input type="radio" name="color" value='5' class="color_input"> <p class="color_label">5</P>
			<button @click="onSubmit($event)">Submit</button>
	</div>
</template>

<script>
	export default {
		data: function() {
			return {
				playerName: "",
				playerColor: ""
			}
		},
		methods: {
			onSubmit: function(event) {
				this.playerName = document.querySelector("#name_input").value;
				this.playerColor = document.querySelector('input[name = "color"]:checked').value;
				this.$emit("add-slot", this.playerName, this.playerColor);
			}
		}
	}
</script>

<style lang="scss">
	.prompt {
		color: #F5F2F2;
		font-size: 28px;
		font-family: Roboto Slab, serif;
		min-height: 42px;
	}

	.color_label {
		color: #F5F2F2
	}

	p {
		display: inline
	}
</style>