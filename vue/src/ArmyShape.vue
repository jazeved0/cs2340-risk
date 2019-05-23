<template>
  <v-shape :config="config">
  </v-shape>
</template>

<script>
  export default {
    props: {
      color: String,
      size: Number,
      x: Number,
      y: Number
    },
    computed: {
      config: function () {
        const size     = this.size;
        const color    = this.color;
        const radius   = size < 5 ? 5: (size > 9 ? 12 : 10);
        const bgOffset = 3;
        const bgFill   = 'rgba(245, 242, 242, 0.55)';
        const textFill = 'rgb(245, 242, 242)';
        const position = {
          a: this.x,
          b: this.y
        };
        return {
          x: position.a,
          y: position.b,
          width: radius,
          height: radius,
          listening : false,
          sceneFunc: function (context) {
            context.beginPath();
            const spacing = 1;
            if (size <= 4) {
              if (size === 1) {
                // background - circle
                context.beginPath();
                context.arc(0, 0, radius + bgOffset, 0, 2*Math.PI);
                context.closePath();
                context.fillStyle = bgFill;
                context.fill();

                // foreground
                context.beginPath();
                context.arc(0, 0, radius, 0, 2*Math.PI, false);
                context.closePath();
                context.fillStyle = color;
                context.fill();
              } else if (size === 2) {
                // background - capsule shape
                context.beginPath();
                context.arcTo(-radius-spacing, 0, radius + bgOffset, 0.5*Math.PI, 1.5*Math.PI);
                context.arcTo(radius+spacing, 0, radius + bgOffset, 1.5*Math.PI, 0.5*Math.PI, false);
                context.closePath();
                context.fillStyle = bgFill;
                context.fill();

                // foreground
                context.beginPath();
                context.arc(-radius-spacing, 0, radius, 0, 2*Math.PI, false);
                context.moveTo(radius+spacing, 0);
                context.arc(radius+spacing, 0, radius, 0, 2*Math.PI, false);
                context.closePath();
                context.fillStyle = color;
                context.fill();
              } else if (size === 3) {
                const correction = 1;

                // background - rounded triangle shape
                context.beginPath();
                context.arcTo(-radius-spacing, radius+spacing, radius + bgOffset, 1.3333*Math.PI, 0.3333*Math.PI, true);
                context.arcTo(radius+spacing, radius+spacing, radius + bgOffset, 0.6667*Math.PI, 1.6667*Math.PI, true);
                context.arcTo(0,-radius-spacing + correction, radius + bgOffset, 0, Math.PI, true);
                context.closePath();
                context.fillStyle = bgFill;
                context.fill();

                // foreground
                context.beginPath();
                context.arc(-radius-spacing, radius+spacing, radius, 0, 2*Math.PI,false);
                context.moveTo(radius+spacing, radius+spacing)
                context.arc(radius+spacing, radius+spacing, radius, 0, 2*Math.PI,false);
                context.moveTo(0, -radius-spacing + correction);
                context.arc(0,-radius-spacing + correction, radius, 0, 2*Math.PI,false);
                context.closePath();
                context.fillStyle = color;
                context.fill();
              } else {
                // background - rounded rectangle
                context.beginPath();
                context.arcTo(-radius - spacing, -radius - spacing, radius + bgOffset, 0.75*Math.PI, 1.75*Math.PI, false);
                context.arcTo(radius + spacing, -radius - spacing, radius + bgOffset, 1.25*Math.PI, 0.25*Math.PI, false);
                context.arcTo(radius + spacing, radius + spacing, radius + bgOffset, 1.75*Math.PI, 0.75*Math.PI, false);
                context.arcTo(-radius - spacing, radius + spacing, radius + bgOffset, 0.25*Math.PI, 1.25*Math.PI, false);
                context.closePath();
                context.fillStyle = bgFill;
                context.fill();


                // foreground
                context.beginPath();
                context.arc(-radius - spacing, -radius - spacing, radius, 0, 2 * Math.PI, false);
                context.moveTo(radius + spacing, -radius - spacing);
                context.arc(radius + spacing, -radius - spacing, radius, 0, 2 * Math.PI, false);
                context.moveTo(-radius - spacing, radius + spacing);
                context.arc(-radius - spacing, radius + spacing, radius, 0, 2 * Math.PI, false);
                context.moveTo(radius + spacing, radius + spacing);
                context.arc(radius + spacing, radius + spacing, radius, 0, 2 * Math.PI, false);
                context.closePath();
                context.fillStyle = color;
                context.fill();
              }
            } else {
              // background circle
              context.beginPath();
              context.arc(0, 0, radius, 0, 2*Math.PI, false);
              context.closePath();
              context.fillStyle = color;
              context.fill();

              // foreground text
              context.fillStyle = textFill;
              context.font = "bold 15px Roboto";
              context.shadowBlur = 6;
              context.shadowOpacity = 0.4;
              context.textBaseline = "middle";
              context.textAlign = "center";
              context.fillText(size, 0, 2);
            }
          }
        };
      }
    }
  }
</script>
