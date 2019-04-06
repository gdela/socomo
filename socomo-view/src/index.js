/*
 * Bootstrapping of the visualiser single-page app.
 * An upside-down solution, a socomo.html file contains just the data, and calls this file,
 * so here we have what is usually found in an html file - list of assets that make the app.
 */

import 'normalize.css';
import './index.scss';

function loadStyle(src) {
	console.log('loading style ' + src);
	const link = document.createElement('link');
	link.href = src;
	link.type = 'text/css';
	link.rel = 'stylesheet';
	document.head.appendChild(link);
}

function loadScript(src, onload) {
	console.log('loading script ' + src);
	const script = document.createElement('script');
	script.src = src;
	script.type = 'text/javascript';
	script.async = false;
	if (onload) script.onload = onload;
	document.head.appendChild(script);
}

window.addEventListener('DOMContentLoaded', () => {
	console.log('loading assets');
	const indexJsUrl = document.getElementsByTagName('script')[0].src;
	const baseUrl = indexJsUrl.slice(0, indexJsUrl.lastIndexOf('/'));
	// keep in sync with versions in package.json and externals declaration in webpack.config.js
	loadScript('https://cdn.jsdelivr.net/npm/cytoscape@3.2.22/dist/cytoscape.js');
	loadScript('https://cdn.jsdelivr.net/npm/klayjs@0.4.1/klay.js');
	loadScript('https://cdn.jsdelivr.net/gh/gdela/cytoscape.js-klay@v3.1.2-patch1/cytoscape-klay.js');
	loadStyle(baseUrl + '/bundle.css');
	loadScript(baseUrl + '/bundle.js', () => {
		console.log('assets loaded');
		// window.socomo comes from the bundle.js (main.js)
		// window.composition comes from a socomo.html
		window.socomo(window.composition);
	});
});

// a signal to the dev-mode script that assets are being served
window.socomo = 'will-load-eventually';
