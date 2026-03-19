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
		<div id="search-container">
			<input type="text" id="search-input" placeholder="search">
			<button id="search-button">搜索</button>
		</div>
	</div>
	<div id="content-container">
		<div id="main-diagram"></div>
		<div id="main-table"></div>
	</div>`;

	const mainDiagram = document.getElementById('main-diagram');
	const mainTable = document.getElementById('main-table');

	let currentCy;
	let currentLevel = level;
	let currentKeyword = '';

	doLongTask(() => {
		currentCy = drawDiagram(mainDiagram, currentLevel, onDependencySelected);
	});

	// Filter function
	function filterLevel(originalLevel, codemap, keyword) {
		if (!keyword || !codemap) return originalLevel;

		const matchedPairs = new Set();
		const levelPrefix = originalLevel.level + '.';

		// Traverse codemap to find packages matching className or memberName
		for (const pkg of codemap.packageDeps) {
			for (const cls of pkg.memberDeps) {
				if (
					(cls.from.className && cls.from.className.includes(keyword)) ||
					(cls.from.memberName && cls.from.memberName.includes(keyword)) ||
					(cls.to.className && cls.to.className.includes(keyword)) ||
					(cls.to.memberName && cls.to.memberName.includes(keyword))
				) {
					// Calculate component names
					let compFrom;
					if (pkg.from.fqn === originalLevel.level) {
						compFrom = '[root]';
					} else if (pkg.from.fqn.startsWith(levelPrefix)) {
						const afterLevel = pkg.from.fqn.slice(levelPrefix.length);
						const firstDotIndex = afterLevel.indexOf('.');
						compFrom = firstDotIndex === -1 ? afterLevel : afterLevel.slice(0, firstDotIndex);
					}

					let compTo;
					if (pkg.to.fqn === originalLevel.level) {
						compTo = '[root]';
					} else if (pkg.to.fqn.startsWith(levelPrefix)) {
						const afterLevel = pkg.to.fqn.slice(levelPrefix.length);
						const firstDotIndex = afterLevel.indexOf('.');
						compTo = firstDotIndex === -1 ? afterLevel : afterLevel.slice(0, firstDotIndex);
					}

					if (compFrom && compTo) {
						matchedPairs.add(JSON.stringify({from: compFrom, to: compTo}));
					}
					break; // Break out of inner loop after finding a match
				}
			}
		}

		// Collect all involved components
		const componentSet = new Set();
		for (const pairStr of matchedPairs) {
			const pair = JSON.parse(pairStr);
			componentSet.add(pair.from);
			componentSet.add(pair.to);
		}

		const filteredComponents = {};
		// Filter components
		for (const comp in originalLevel.components) {
			if (componentSet.has(comp)) {
				filteredComponents[comp] = originalLevel.components[comp];
			}
		}

		const filteredDependencies = {};
		// Filter dependencies, must match the same pair of from and to
		for (const dep in originalLevel.dependencies) {
			const [from, to] = dep.split(' -> ');
			for (const pairStr of matchedPairs) {
				const pair = JSON.parse(pairStr);
				if (pair.from === from && pair.to === to) {
					filteredDependencies[dep] = originalLevel.dependencies[dep];
					break;
				}
			}
		}

		return {
			level: originalLevel.level,
			components: filteredComponents,
			dependencies: filteredDependencies
		};
	}

	// Search functionality
	const searchInput = document.getElementById('search-input');
	const searchButton = document.getElementById('search-button');

	function performSearch() {
		const keyword = searchInput.value.trim();
		currentKeyword = keyword;
		currentLevel = filterLevel(level, getCodemap(), keyword);
		if (currentCy) currentCy.destroy();
		currentCy = drawDiagram(mainDiagram, currentLevel, onDependencySelected);
	}

	searchButton.addEventListener('click', performSearch);
	searchInput.addEventListener('keypress', (e) => {
		if (e.key === 'Enter') performSearch();
	});

	function onDependencySelected(fromComponent, toComponent) {
		if (hasCodemap()) {
			console.info('selected dependency %s -> %s', fromComponent, toComponent);
			drawTable(mainTable, getCodemap(), fromComponent, toComponent, currentKeyword);
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
