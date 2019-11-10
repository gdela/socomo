/*
 * Bootstrapping of the visualiser single-page app.
 * An upside-down solution, a socomo.html file contains just the data, and calls this file,
 * so here we have what is usually found in an html file - list of assets that make the app.
 */

import 'normalize.css';
import './index.scss';
import FontFaceObserver from 'fontfaceobserver';

function loadStyle(src) {
	return new Promise((resolve, reject) => {
		console.debug('loading style ' + src);
		const link = document.createElement('link');
		link.href = src;
		link.type = 'text/css';
		link.rel = 'stylesheet';
		link.onload = () => resolve();
		link.onerror = () => reject(new Error('could not load ' + src));
		document.head.appendChild(link);
	});
}

function loadScript(src) {
	return new Promise((resolve, reject) => {
		console.debug('loading script ' + src);
		const script = document.createElement('script');
		script.src = src;
		script.type = 'text/javascript';
		script.async = false;
		script.onload = () => resolve();
		script.onerror = () => reject(new Error('could not load ' + src));
		document.head.appendChild(script);
	});
}

window.addEventListener('DOMContentLoaded', () => {
	console.debug('loading assets');
	document.body.className = 'loading-state';
	document.body.innerHTML = '<div id="status-message"></div>';
	const indexJsUrl = document.getElementsByTagName('script')[0].src;
	const baseUrl = indexJsUrl.slice(0, indexJsUrl.lastIndexOf('/'));

	const fontsLoaded = Promise
		.all([
			loadStyle('https://fonts.googleapis.com/css?family=Lato:400,700'),
			new FontFaceObserver('Lato', {weight:400}).load(),
			new FontFaceObserver('Lato', {weight:700}).load()
		]).catch(error => {
			console.warn('fonts not loaded: ' + error.message);
		});

	const scriptsLoaded = Promise
		.all([
			// keep in sync with versions in package.json and externals declaration in webpack.config.js
			loadScript('https://cdn.jsdelivr.net/npm/cytoscape@3.2.22/dist/cytoscape.js'),
			loadScript('https://cdn.jsdelivr.net/npm/klayjs@0.4.1/klay.js'),
			loadScript('https://cdn.jsdelivr.net/npm/cytoscape-klay@v3.1.3/cytoscape-klay.js'),
			loadStyle(baseUrl + '/bundle.css'),
			loadScript(baseUrl + '/bundle.js')
		]);

	Promise
		.all([fontsLoaded, scriptsLoaded])
		.then(() => {
			console.debug('assets loaded');
			// window.socomo comes from the bundle.js (main.js)
			// window.composition comes from a socomo.html
			window.socomo(window.composition);
		}).catch(error => {
			document.body.className = 'error-state';
			throw error;
		});
});

// a signal to the dev-mode script that assets are being served
window.socomo = 'will-load-eventually';
