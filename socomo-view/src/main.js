/*
 * Entry point for the visualizer single-page app.
 * The socomo.html skeleton that contains the composition data will call the socomo() function
 * (via index.js) passing the composition of the module to be visualized in user's browser.
 */

import './main.scss';
import drawDiagram from './diagram';
import drawTable from './table';

function socomo(composition) {

	const module = composition[0];
	const moduleName = module.module;
	const level = composition[1];
	const levelName = level.level;

	document.body.innerHTML += `
	<div id="header-container">
		<h1>${moduleName} &nbsp;&#x276d;&nbsp; <code>${levelName}</code></h1>
	</div>
	<div id="content-container">
		<div id="main-diagram"></div>
		<div id="main-table"></div>
	</div>`;

	const mainDiagram = document.getElementById('main-diagram');
	const mainTable = document.getElementById('main-table');

	doLongTask(() => {
		drawDiagram(mainDiagram, level, onDependencySelected);
	});

	function onDependencySelected(fromComponent, toComponent) {
		console.info('selected dependency %s -> %s', fromComponent, toComponent);
		drawTable(mainTable, fromComponent, toComponent);
		mainTable.style.display = 'flex';
	}

	document.addEventListener('keyup', e => {
		if (e.key === 'Escape' || e.key === 'Esc') {
			mainTable.style.display = 'none';
		}
	});
	mainTable.addEventListener('click', () => {
		mainTable.style.display = 'none';
	});
}

function doLongTask(task) {
	document.body.className = 'loading-state';
	// the delay is to give browser a chance to render dom changes done so far
	setTimeout(() => { task(); document.body.className = ''; }, 10);
}

// expose socomo function to the index.js bootstrapping code
window.socomo = socomo;
