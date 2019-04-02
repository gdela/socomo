/*
 * Entry point for the visualizer single-page app.
 * The visualizer html skeleton will call the socomo() function passing
 * the composition of the module to be visualized in user's browser.
 */

import './visualizer.scss';
import drawDiagram from './diagram';

function socomo(moduleName, composition) {

	// todo: currently we show only one diagram for the first level
	const levelName = Object.keys(composition)[0];
	const level = Object.values(composition)[0];

	document.body.innerHTML = `
	<div id="header-container">
		<h1>${moduleName} &nbsp;&#x276d;&nbsp; <code>${levelName}</code></h1>
	</div>
	<div id="diagram-container">
		<div id="main-diagram"></div>
	</div>`;

	drawDiagram(document.getElementById('main-diagram'), level);
}

window.socomo = socomo;
