<template>
	<div>
		<div class="p-1">
			<div class="row mb-1 mb-sm-3">
				<div class="col-12 col-sm-3 form-label-container">
					<p>Username:</p>
				</div>
				<div class="col-12 col-sm-9 d-flex form-fix">
					<b-input v-bind="currentName" :maxlength="maxLength"></b-input>
				</div>
			</div>
			<div class="row">
				<div class="col-12 col-sm-3 form-label-container">
					<p id="color-label">Color:</p>
				</div>
				<div class="col-12 col-sm-9 d-flex form-fix">
					<div id="colorButtonGroup" class="btn-group-lg btn-group-toggle" data-toggle="buttons">
						<div class="d-flex colorButtonParent">
							<label v-for="color in colorList"
										 :key="color.num"
										 class="btn-circle btn btn-secondary colorButton"
										 :class="[{ selected: (!initialSelected || effectiveSelectedColor === color.num) }, { taken: takenColors.includes(color.num) } ]"
										 :style="{ backgroundColor: color.hex, borderColor: color.hex }"
										 v-on:click="onColorClick(color.num)">
								<input type="radio" name = "colorButton" :value="color.num" autocomplete="off" style="display:none"/> &nbsp;
							</label>
						</div>
					</div>
				</div>
			</div>
		</div>
		<hr style="opacity: 0.6;"/>
		<div class="p-1 pb-0" style="margin-bottom: -8px;">
			<div class="d-flex flex-row-reverse">
				<b-button :style="{ backgroundColor: buttonColor }"
									:disabled="canSubmit">
					<div class="p-1">Join Lobby</div>
				</b-button>
			</div>
		</div>
	</div>
</template>

<script>
	export default {
		props: {
			colors: Array,
			nameRegex: String,
			takenNames: Array,
			takenColors: Array,
			minLength: Number,
			maxLength: Number
		},
		data: function () {
			return {
				selectedColor: 0,
				currentName: "",
				initialSelected: false
			}
		},
		computed: {
			colorList: function () {
				return this.colors.map((val, index) => {
					return {
						hex: '#' + val,
						num: index
					}
				});
			},
			effectiveSelectedColor: function() {
				if (this.takenColors.includes(this.selectedColor)) {
					if (this.takenColors.length === this.colors.length) return -1;
					else {
						const taken = this.takenColors;
						const available = this.colors.filter(c => !taken.includes(c));
						if (available.length === 0) return -1;
						else return available[0];
					}
				} else return this.selectedColor;
			},
			buttonColor: function () {
				return this.initialSelected ?
						"#" + this.colors[this.effectiveSelectedColor] :
						"#9793C7";
			},
			canSubmit: function () {
				// Whether or not the form can be submitted
				const regex = this.nameRegexObj;
				return this.currentName.length >= this.minLength &&
						this.currentName.length <= this.maxLength &&
						this.takenNames.length !== this.colors.length &&
						this.effectiveSelectedColor !== -1 &&
						regex.test(this.currentName) &&
						!this.takenNames.includes(this.currentName);
			},
			nameRegexObj: function () {
				return new RegExp(this.nameRegex);
			}
		},
		methods: {
			onColorClick: function(index) {
				if(!this.takenColors.includes(index)) {
					this.selectedColor = index;
					this.initialSelected = true;
				}
			}
		}
	}
</script>

<style lang="scss">
	.colorButton {
		opacity: 0.4;
		-webkit-transition: opacity 0.2s;
		transition: opacity 0.2s;
		cursor: pointer;
	}
	.colorButton.taken {
		cursor: default;
		background-color: transparent!important;
		// noinspection CssUnknownTarget
		background-image: url("/static/images/disabledPattern.svg");
		background-size: 150px;
		border: 1px solid #77777780!important;
		opacity: 0.4;
		-webkit-transition: opacity 0.2s;
		transition: opacity 0.2s;
	}
	.colorButton.selected:not(.taken) {
		opacity: 1.0;
		-webkit-transition: opacity 0.2s;
		transition: opacity 0.2s;
	}
	/* Form label font settings */
	.form-label-container {
		display: flex;
		-webkit-box-orient: horizontal!important;
		-webkit-box-direction: reverse!important;
		-ms-flex-direction: row-reverse!important;
		flex-direction: row-reverse!important;
	}
	.form-label-container p {
		font-size: 18px;
		font-family: Roboto, sans-serif;
		color: #362A4D;
		margin-bottom: 0;
		margin-top: 4px;
		margin-right: -16px;
		display: inline;
		-webkit-box-orient: horizontal!important;
		-webkit-box-direction: reverse!important;
		-ms-flex-direction: row-reverse!important;
		flex-direction: row-reverse!important;
	}
	#color-label {
		margin-top: 8px;
	}
</style>