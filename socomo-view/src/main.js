/*
 * Entry point for the visualizer single-page app.
 * The socomo.html skeleton that contains the composition data will call the socomo() function
 * (via index.js) passing the composition of the module to be visualized in user's browser.
 */

import './main.scss';
import drawDiagram from './diagram';
import drawTable from './table';
import Mousetrap from 'mousetrap';

function socomo() {

	// window.composition comes from socomo.html and is ready before we are called
	const composition = window.composition;
	// window.codemap comes from socomo.data which is loaded asynchronously
	const hasCodemap = () => window.codemap !== undefined;
	const getCodemap = () => window.codemap;

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
		if (hasCodemap()) {
			console.info('selected dependency %s -> %s', fromComponent, toComponent);
			drawTable(mainTable, getCodemap(), fromComponent, toComponent);
			mainTable.style.display = 'flex';
		} else {
			console.warn('socomo.data not yet loaded or completely missing');
		}
	}

	Mousetrap.bind('esc', () => {
		mainTable.style.display = 'none';
	});
	mainTable.addEventListener('click', () => {
		mainTable.style.display = 'none';
	});
}

function doLongTask(task) {
	document.body.className = 'loading-state';
	// the delay is to give browser a chance to render dom changes done so far
	setTimeout(() => { task(); document.body.className = ''; }, 30);
}

// expose socomo function to the index.js bootstrapping code
window.socomo = socomo;
