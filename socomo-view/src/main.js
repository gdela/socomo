/*
 * Entry point for the visualizer single-page app.
 * The socomo.html skeleton that contains the composition data will call the socomo() function
 * (via index.js) passing the composition of the module to be visualized in user's browser.
 */

import drawDiagram from './diagram';
import './main.scss';

function socomo(composition) {
	console.log('drawing first diagram');

	const module = composition[0];
	const moduleName = module.module;
	const level = composition[1];
	const levelName = level.level;

	document.body.innerHTML = `
	<div id="header-container">
		<h1>${moduleName} &nbsp;&#x276d;&nbsp; <code>${levelName}</code></h1>
	</div>
	<div id="diagram-container">
		<div id="main-diagram"></div>
	</div>`;

	drawDiagram(document.getElementById('main-diagram'), level);
}

console.log('exposing socomo in window');
window.socomo = socomo;
