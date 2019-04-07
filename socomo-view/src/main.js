/*
 * Entry point for the visualizer single-page app.
 * The socomo.html skeleton that contains the composition data will call the socomo() function
 * (via index.js) passing the composition of the module to be visualized in user's browser.
 */

import './main.scss';
import drawDiagram from './diagram';

function socomo(composition) {

	const module = composition[0];
	const moduleName = module.module;
	const level = composition[1];
	const levelName = level.level;

	document.body.insertAdjacentHTML('beforeend', `
	<div id="header-container">
		<h1>${moduleName} &nbsp;&#x276d;&nbsp; <code>${levelName}</code></h1>
	</div>
	<div id="diagram-container">
		<div id="main-diagram"></div>
	</div>`);

	doLongTask(() => {
		drawDiagram(document.getElementById('main-diagram'), level);
	});
}

function doLongTask(task) {
	const message = document.getElementById('loading');
	message.style.visibility = 'visible';
	// the delay is to give browser a chance to render dom changes done so far
	setTimeout(() => { task(); message.style.visibility = 'hidden'; }, 10);
}

// expose socomo function to the index.js bootstrapping code
window.socomo = socomo;
