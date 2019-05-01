export const getCookie = (cname) => {
  const name = cname + "=";
  const decodedCookie = decodeURIComponent(document.cookie);
  const ca = decodedCookie.split(';');
  for (let i = 0; i < ca.length; i++) {
    let c = ca[i];
    while (c.charAt(0) === ' ') {
      c = c.substring(1);
    }
    if (c.indexOf(name) === 0) {
      return c.substring(name.length, c.length);
    }
  }
  return "";
};

export const pascalToUnderscore = (s) =>
  s.replace(/(?:^|\.?)([A-Z])/g, function (x, y) {
    return "_" + y.toLowerCase();
  }).replace(/^_/, "");

export const colorLuminance = (hex, lum) => {
  // validate hex string
  hex = String(hex).replace(/[^0-9a-f]/gi, '');
  if (hex.length < 6) {
    hex = hex[0]+hex[0]+hex[1]+hex[1]+hex[2]+hex[2];
  }
  lum = lum || 0;
  // convert to decimal and change luminosity
  let rgb = "#", c, i;
  for (i = 0; i < 3; i++) {
    c = parseInt(hex.substr(i*2,2), 16);
    c = Math.round(Math.min(Math.max(0, c + (c * lum)), 255)).toString(16);
    rgb += ("00"+c).substr(c.length);
  }
  return rgb;
};

//rgb to hsv function
const hsvToRgb = (color) => {
  let r, g, b, i, f, p, q, t;
  let h = color.h;
  let s = color.s;
  let v = color.v; 
  i = Math.floor(h * 6);
  f = h * 6 - i;
  p = v * (1 - s);
  q = v * (1 - f * s);
  t = v * (1 - (1 - f) * s);
  switch (i % 6) {
      case 0: r = v, g = t, b = p; break;
      case 1: r = q, g = v, b = p; break;
      case 2: r = p, g = v, b = t; break;
      case 3: r = p, g = q, b = v; break;
      case 4: r = t, g = p, b = v; break;
      case 5: r = v, g = p, b = q; break;
  }
  return {
      r: Math.round(r * 255),
      g: Math.round(g * 255),
      b: Math.round(b * 255)
  };
};

//hsv to rgb function
const rgbToHsv = (color) => {
  let r = color.r;
  let g = color.g;
  let b = color.b;
  let max = Math.max(r, g, b), min = Math.min(r, g, b),
      d = max - min,
      h,
      s = (max === 0 ? 0 : d / max),
      v = max / 255;

  switch (max) {
      case min: h = 0; break;
      case r: h = (g - b) + d * (g < b ? 6: 0); h /= 6 * d; break;
      case g: h = (b - r) + d * 2; h /= 6 * d; break;
      case b: h = (r - g) + d * 4; h /= 6 * d; break;
  }

  return {
      h: h,
      s: s,
      v: v
  };
};

const componentToHex = (c) => {
  let hex = c.toString(16);
  return hex.length === 1 ? "0" + hex : hex;
};

const rgbToHex = (color) => {
  return "#" + componentToHex(color.r) + componentToHex(color.g) + componentToHex(color.b);
};

const hexToRgb = (hex) => {
  let result = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(hex);
  return result ? {
    r: parseInt(result[1], 16),
    g: parseInt(result[2], 16),
    b: parseInt(result[3], 16)
  } : null;
};

export const alphaBlended = (hex, alpha) => {
  const rgb = hexToRgb(hex);
  return "rgba(" + rgb.r + "," + rgb.g + "," + rgb.b + "," + alpha + ")";
};

export const colorSaturation = (hex, sat) => {
  let rgb = hexToRgb(hex);

  let hsv = rgbToHsv(rgb);
  const brightness = hsv.v >= 0.7 ? 0.85 : 1;
  hsv.s *= sat;
  hsv.v *= brightness;
  rgb = hsvToRgb(hsv);
  hex = rgbToHex(rgb);
  return hex;
};

export const distance = (p1, p2) => {
  return Math.sqrt(Math.pow((p2.x - p1.x), 2) + Math.pow((p2.y - p1.y), 2));
};

export const clamp = (num, min, max) => {
  return num <= min ? min : num >= max ? max : num;
};

export const seqStringToArray = (string) => {
  return string.indexOf("List(") === 0 ?
    string.slice(5, string.length - 1).split(', ').map(function(item){return parseInt(item, 10);}) : [];
};

export const specialSeqToArray = (string) => {
  let matches = /^\([A-Z][a-z]+\((.+)\),(\d),(\d)\)$/gi.exec(string);
  return matches.length > 3 ? [matches[1].split(', ').map(function(item){return parseInt(item, 10);}),
    parseInt(matches[2], 10), parseInt(matches[3], 10)] : [];
};

export const exists = (object) => !(typeof object === 'undefined' || object === null);

/* eslint-disable no-console */
export const logError = (message) => console.log("[Error] " + message);
/* eslint-enable no-console */
