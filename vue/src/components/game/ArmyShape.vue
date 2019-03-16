<template>
	<v-shape :config="config">
	</v-shape>
</template>

<script>
  export default {
    props: {
      data: Object,
    },
    computed: {
      config: function () {
        const state = this.$store.state;
        if (state) {
          const color = ('color' in this.data
						&& state.settings.settings.colors.length > this.data.color)
            ? '#' + state.settings.settings.colors[this.data.color]
            : '#FFFFFF';
          const position = ('position' in this.data
						&& state.game.gameboard.centers.length > this.data.position)
						? state.game.gameboard.centers[this.data.position]
						: { x: 0, y: 0 };
					const size = ('size' in this.data) ? this.data.size : 0;
          const radius = size < 5 ? 3 : 10;
          return {
            fill: color,
            stroke: color,
            x: position.a,
            y: position.b,
						width: radius,
						height: radius,
            shadowBlur: 3,
						shadowOpacity: 0.2,
            listening : false,
            sceneFunc: function (context, shape) {
              context.beginPath();
              const spacing = 2;
              if (size === 1) {
                context.arc(0, 0, radius, 0, 2*Math.PI, false);
                context.closePath();
                context.fillStrokeShape(shape);
              } else if (size === 2) {
                context.arc(-radius-spacing, 0, radius, 0, 2*Math.PI, false);
                context.moveTo(radius+spacing, 0);
                context.arc(radius+spacing, 0, radius, 0, 2*Math.PI, false);
                context.closePath();
                context.fillStrokeShape(shape);
              } else if (size === 3) {
                context.arc(-radius-spacing, radius+spacing, radius, 0, 2*Math.PI,false);
                context.moveTo(radius+spacing, radius+spacing)
                context.arc(radius+spacing, radius+spacing, radius, 0, 2*Math.PI,false);
                context.moveTo(0, -radius-spacing);
                context.arc(0,-radius-spacing, radius, 0, 2*Math.PI,false);
                context.closePath();
                context.fillStrokeShape(shape);
              } else if (size === 4) {
                context.arc(-radius-spacing, -radius-spacing, radius, 0, 2*Math.PI,false);
                context.moveTo(radius+spacing, -radius-spacing);
                context.arc(radius+spacing, -radius-spacing, radius, 0, 2*Math.PI,false);
                context.moveTo(-radius-spacing,radius+spacing);
                context.arc(-radius-spacing, radius+spacing, radius, 0, 2*Math.PI,false);
                context.moveTo(radius+spacing, radius+spacing);
                context.arc(radius+spacing, radius+spacing, radius, 0, 2*Math.PI,false);
                context.closePath();
                context.fillStrokeShape(shape);
              } else {
                context.arc(0, 0, radius, 0, 2*Math.PI, false);
                context.closePath();
                context.strokeShape(shape)
                const offset = size >= 10 ? -5 : -3;
                context.fillText(size, offset, 3);
              }
            }
          };
        } else return {};
      }
    }
  }
</script>
