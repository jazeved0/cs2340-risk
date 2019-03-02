<template>
	<b-modal id="playerFormModal"
					 no-close-on-esc
					 no-close-on-backdrop
					 hide-header-close
					 centered
					 v-bind:visible="visible"
					 @shown="focusInput">
		<!--suppress HtmlUnknownBooleanAttribute, XmlUnboundNsPrefix -->
		<template v-slot:modal-title>
			<p id="settingsTitle">
				Select Settings
			</p>
		</template>
		<!--suppress HtmlUnknownBooleanAttribute, XmlUnboundNsPrefix -->
		<template v-slot:modal-footer>
			<p class="d-none">y</p>
			<!-- empty -->
		</template>
		<div>
			<div class="p-1">
				<div class="row mb-1 mb-sm-3">
					<div class="col-12 col-sm-3 form-label-container">
						<p>Username:</p>
					</div>
					<div class="col-12 col-sm-9 d-flex form-fix">
						<b-form-input ref="nameInput" type="text" v-model="currentName" :maxlength="maxLength" :disabled="hasSubmitted">
						</b-form-input>
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
											 :class="[{ selected: (!initialSelected || effectiveSelectedColor === color.num) }, { taken: takenColors.includes(color.num)}, { locked: hasSubmitted } ]"
											 :style="{ backgroundColor: color.hex }"
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
					<b-button :style="{ backgroundColor: buttonColor, borderColor: buttonColor }"
										:disabled="!canSubmit"
										v-on:click="submitPlayer"
										:class="{ locked: hasSubmitted, error: errorAnim }">
						<div style="min-width: 80px; min-height: 34px;">
							<!--suppress XmlUnboundNsPrefix -->
							<div v-if="this.$store.state.responseTarget === null" class="p-1">Join Lobby</div>
							<b-spinner v-else variant="light"/>
						</div>
					</b-button>
				</div>
			</div>
		</div>
	</b-modal>
</template>

<script>
  import {START_RESPONSE_WAIT, STOP_RESPONSE_WAIT} from './../store/mutation-types';
  import {SET_ERROR_MESSAGE} from "../store/mutation-types";

  export default {
    props: {
      colors: Array,
      nameRegex: String,
      takenNames: Array,
      takenColors: Array,
      minLength: Number,
      maxLength: Number,
      isWaiting: Boolean,
      visible: Boolean
    },
    data: function () {
      return {
        selectedColor: 0,
        currentName: "",
        initialSelected: false,
        hasSubmitted: false,
        errorAnim: false
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
      onColorClick: function (index) {
        if(!this.takenColors.includes(index) && !this.hasSubmitted) {
          this.selectedColor = index;
          this.initialSelected = true;
        }
      },
      submitPlayer: function () {
        // validate
        if (this.canSubmit && !this.hasSubmitted) {
          // send a packet to the websocket and wait for a response
          const store = this.$store;
          const name = this.currentName;
          const color = this.effectiveSelectedColor;
          const thisRef = this;
          this.$store.commit(START_RESPONSE_WAIT, function (data) {
            if ('response' in data) {
              if (data.response === "Accepted") {
                store.commit(STOP_RESPONSE_WAIT);
                thisRef.$emit('add-player', { name: name, ordinal: color });
              } else {
                thisRef.responseFailed(data.message);
              }
            }
          });
          this.hasSubmitted = true;
          this.$socket.sendObj({
            _type: 'controllers.RequestPlayerJoin',
            playerId: this.$store.state.playerId,
            gameId: this.$store.state.gameId,
            withSettings: {
              name: this.currentName,
              ordinal: this.effectiveSelectedColor
            }
          });
          // set timeout
          setTimeout(() => {
            if (this.$store.state.responseTarget !== null &&
              this.$store.state.current === "") {
              // Wasn't committed properly
              this.responseFailed('Request timed out');
            }
          }, 2000);
        }
      },
      responseFailed: function (message) {
        this.hasSubmitted = false;
        this.$store.commit(STOP_RESPONSE_WAIT);
        this.errorAnim = true;
        this.$store.commit(SET_ERROR_MESSAGE, message);
        setTimeout(() => {
          this.errorAnim = false;
        }, 900);
      },
      focusInput: function () {
        this.$refs.nameInput.focus();
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
	.locked {
		cursor: default!important;
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
	.error {
		animation: shake 0.82s cubic-bezier(.36, .07, .19, .97) both;
		transform: translate3d(0, 0, 0);
		backface-visibility: hidden;
		perspective: 1000px;
	}
	@keyframes shake {
		10%, 90% {
			transform: translate3d(-1px, 0, 0);
		}
		20%, 80% {
			transform: translate3d(2px, 0, 0);
		}
		30%, 50%, 70% {
			transform: translate3d(-4px, 0, 0);
		}
		40%, 60% {
			transform: translate3d(4px, 0, 0);
		}
	}
	#player-form {
		margin: auto
	}
	.modal-content {
		background-color: #F5F2F2!important;
		color: #362A4D;
	}
	.modal-footer {
		padding: 4px!important;
	}
	#settingsTitle {
		font-size: 32px;
		line-height: 30px;
		font-weight: 200;
		font-family: Roboto, sans-serif;
		margin-top: 6px;
		margin-left: 2px;
		margin-bottom: -6px;
	}
</style>