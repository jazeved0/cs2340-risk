export const getCookie = (cname) => {
	const name = cname + "=";
	const decodedCookie = decodeURIComponent(document.cookie);
	const ca = decodedCookie.split(';');
	for(var i = 0; i <ca.length; i++) {
		const c = ca[i];
	  while (c.charAt(0) === ' ') {
			c = c.substring(1);
	  }
	  if (c.indexOf(name) === 0) {
			return c.substring(name.length, c.length);
	  }
	}
	return "";
}

export const pascalToUnderscore = (s) =>
	s.replace(/(?:^|\.?)([A-Z])/g, function(x, y) {
    return "_" + y.toLowerCase();
	}).replace(/^_/, "");