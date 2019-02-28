<template>
	<div>
            <p class="prompt">Name:</p> <input type="text" id="name_input"><br>
            <p class="prompt">Color:</p>
            <input type="radio" name="color" value='green' class="color_input"> <p class="color_label">Green</P>
            <input type="radio" name="color" value='purple' class="color_input"> <p class="color_label">Purple</P>
            <input type="radio" name="color" value='red' class="color_input"> <p class="color_label">Red</P>
            <input type="radio" name="color" value='blue' class="color_input"> <p class="color_label">Blue</P>
            <input type="radio" name="color" value='orange' class="color_input"> <p class="color_label">Orange</P>
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
                this.playerName = document.querySelector("#name_input").value
                this.playerColor = document.querySelector('input[name = "color"]:checked').value
                this.$emit("addSlot", this.playerName, this.playerColor)
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