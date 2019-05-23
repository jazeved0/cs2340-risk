<!--suppress HtmlUnknownTag -->
<template>
    <v-group>
      <v-path
          :config="pathConfig"
          @mouseover="handleMouseOver"
          @mouseout= "handleMouseOut"
          @mousedown="handleMouseDown"
          @tap=      "handleMouseDown">
      </v-path>
      <v-group
          v-if="isHighlighted"
          :config="clipConfig">
        <v-path
            :config="highlightConfig">
        </v-path>
      </v-group>
    </v-group>
</template>

<script>
  import {alphaBlended, colorLuminance,
          colorSaturation, exists} from "./util";
  import Konva from 'konva'

  export default {
    props: {
      // Location of castle, or null if no castle
      castle:    Object,
      // Color of highlight, or null if no highlight
      highlight: String,
      // Base fill of the territory shape
      baseColor: String,
      // SVG path data
      path:      String
    },

    data () {
      return {
        mouseOver: false
      };
    },

    methods: {
      // Mouse over event callback
      handleMouseOver () {
        this.mouseOver = true;
      },

      // Mouse out event callback
      handleMouseOut () {
        this.mouseOver = false;
      },

      // Mouse down event callback
      handleMouseDown () {
        this.$emit('territory-click-raw')
      }
    },

    computed: {
      // Shape config for each shape
      pathConfig () {
        return {
          x: 0,
          y: 0,
          scale: {
            x: 1,
            y: 1
          },
          data: this.path,
          fill: this.color,
          shadowBlur: 10
        };
      },

      // Config of highlight object
      highlightConfig () {
        return {
          x: 0,
          y: 0,
          scale: {
            x: 1,
            y: 1
          },
          data: this.path,
          stroke: this.highlight,
          strokeWidth: 10,
          opacity: 0.4,
          lineJoin: 'round',
          fill: alphaBlended(this.color, 0.8),
          listening: false
        };
      },

      clipConfig () {
        const arr = Konva.Path.parsePathData(this.path);
        return {
          clipFunc (context) {
            // from https://github.com/konvajs/konva/blob/master/src/shapes/Path.ts
            context.beginPath();
            for (var n = 0; n < arr.length; n++) {
              var c = arr[n].command;
              var p = arr[n].points;
              switch (c) {
                case 'L':
                  context.lineTo(p[0], p[1]);
                  break;
                case 'M':
                  context.moveTo(p[0], p[1]);
                  break;
                case 'C':
                  context.bezierCurveTo(p[0], p[1], p[2], p[3], p[4], p[5]);
                  break;
                case 'Q':
                  context.quadraticCurveTo(p[0], p[1], p[2], p[3]);
                  break;
                case 'A':
                  var cx = p[0],
                    cy = p[1],
                    rx = p[2],
                    ry = p[3],
                    theta = p[4],
                    dTheta = p[5],
                    psi = p[6],
                    fs = p[7];
                  var r = rx > ry ? rx : ry;
                  var scaleX = rx > ry ? 1 : rx / ry;
                  var scaleY = rx > ry ? ry / rx : 1;
                  context.translate(cx, cy);
                  context.rotate(psi);
                  context.scale(scaleX, scaleY);
                  context.arc(0, 0, r, theta, theta + dTheta, 1 - fs);
                  context.scale(1 / scaleX, 1 / scaleY);
                  context.rotate(-psi);
                  context.translate(-cx, -cy);
                  break;
                case 'z':
                  context.closePath();
                  break;
              }
            }
          },
        }
      },

      // Color to render as
      color () {
        const color = this.baseColor;
        const saturOffset = 2.5;
        const luminOffset = 0.15;
        if (this.mouseOver && this.isHighlighted) return colorSaturation(color, saturOffset);
        else if (this.isHighlighted) return colorLuminance(colorSaturation(color, saturOffset), -luminOffset);
        else if (this.mouseOver) return colorLuminance(color, luminOffset);
        else return '#' + color;
      },

      // Whether or not the territory is highlighted
      isHighlighted () {
        return exists(this.highlight);
      }
    }
  }
</script>
