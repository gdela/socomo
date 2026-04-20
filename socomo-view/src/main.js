/*
 * Entry point for the visualizer single-page app.
 * The socomo.html skeleton that contains the composition data will call the socomo() function
 * (via index.js) passing the composition of the module to be visualized in user's browser.
 */

import './main.scss';
import drawDiagram from './diagram';
import drawTable, {drawClassList} from './table';
import Mousetrap from 'mousetrap';

function socomo() {

	// window.composition comes from socomo.html and is ready before we are called
	const composition = window.composition;
	// window.codemap comes from socomo.data which is loaded asynchronously
	const hasCodemap = () => window.codemap !== undefined;
	const getCodemap = () => window.codemap;

	const module = composition[0];
	const moduleName = module.module;
	const initialLevel = composition[1];
	const rootLevelName = initialLevel.level;

	document.body.innerHTML += `
	<div id="header-container">
		<div id="title-container">
			<div id="title-row">
				<h1>${moduleName}</h1>
				<button id="header-toggle-button" type="button">收起</button>
			</div>
			<div id="level-navigation">
				<button id="level-up-button" type="button">收起最近展开</button>
				<button id="expand-all-button" type="button">全部展开</button>
				<div id="breadcrumb"></div>
			</div>
		</div>
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
	const searchInput = document.getElementById('search-input');
	const searchButton = document.getElementById('search-button');
	const levelUpButton = document.getElementById('level-up-button');
	const expandAllButton = document.getElementById('expand-all-button');
	const breadcrumb = document.getElementById('breadcrumb');
	const headerToggleButton = document.getElementById('header-toggle-button');

	let currentDiagram;
	let currentKeyword = '';
	let headerCollapsed = false;
	const expandedLevels = [];

	renderCurrentLevel();

	function renderCurrentLevel() {
		updateNavigation();
		const baseLevel = buildExpandedLevel(getCodemap(), rootLevelName, expandedLevels, initialLevel);
		const displayedLevel = filterLevel(baseLevel, getCodemap(), currentKeyword, expandedLevels);
		doLongTask(() => {
			if (currentDiagram) {
				currentDiagram.updateLevel(displayedLevel);
				requestAnimationFrame(() => currentDiagram.refreshContainerGeometry());
				return;
			}
			currentDiagram = drawDiagram(
				mainDiagram,
				displayedLevel,
				onDependencySelected,
				onComponentSelected
			);
		});
	}

	function updateHeaderState() {
		document.body.classList.toggle('header-collapsed', headerCollapsed);
		headerToggleButton.textContent = headerCollapsed ? '展开' : '收起';
	}

	function updateNavigation() {
		levelUpButton.disabled = expandedLevels.length === 0;
		breadcrumb.innerHTML = '';

		const rootCrumb = document.createElement('span');
		rootCrumb.className = 'breadcrumb-item current';
		rootCrumb.textContent = rootLevelName || '[root]';
		breadcrumb.appendChild(rootCrumb);

		if (expandedLevels.length === 0) return;

		const label = document.createElement('span');
		label.className = 'breadcrumb-separator';
		label.textContent = '已展开';
		breadcrumb.appendChild(label);

		for (const expandedLevel of expandedLevels) {
			const button = document.createElement('button');
			button.type = 'button';
			button.className = 'breadcrumb-item';
			button.textContent = relativeLevelName(rootLevelName, expandedLevel);
			button.addEventListener('click', () => collapseExpandedLevel(expandedLevel));
			breadcrumb.appendChild(button);
		}
	}

	function buildExpandedLevel(codemap, levelName, expandedLevelNames, fallbackLevel) {
		if (!codemap || !codemap.packages) return fallbackLevel;

		const expandedSet = new Set(expandedLevelNames);
		const componentSizes = {};
		const componentMeta = {};
		const dependencyStrengths = {};
		const groups = {};

		for (const packet of codemap.packages) {
			const visibleComponent = visibleComponentForPackage(codemap, packet.fqn, levelName, expandedSet);
			if (!visibleComponent) continue;
			componentSizes[visibleComponent.name] = (componentSizes[visibleComponent.name] || 0) + packet.size;
			componentMeta[visibleComponent.name] = visibleComponent;
			if (visibleComponent.group) {
				registerGroupChain(groups, visibleComponent.group, levelName);
			}
		}

		const coverageIndex = buildCoverageIndex(componentMeta);

		for (const packageDep of codemap.packageDeps) {
			const fromComponent = mapPackageToVisibleComponent(packageDep.from.fqn, coverageIndex);
			const toComponent = mapPackageToVisibleComponent(packageDep.to.fqn, coverageIndex);
			if (!fromComponent || !toComponent || fromComponent.name === toComponent.name) continue;
			const dependencyName = fromComponent.name + ' -> ' + toComponent.name;
			dependencyStrengths[dependencyName] = (dependencyStrengths[dependencyName] || 0) + packageDep.memberDeps.length;
		}

		if (Object.keys(componentSizes).length === 0) return fallbackLevel;

		const components = {};
		for (const [name, size] of Object.entries(componentSizes)) {
			const meta = componentMeta[name];
			const isTopLevelComponent = !meta.group;
			const isLeaf = meta.isRootMarker !== true && !hasChildPackages(codemap, meta.packageName);
			const baseLabel = isTopLevelComponent && meta.sourceName
				? `${meta.sourceName} / ${meta.packageName || meta.label}`
				: meta.label;
			components[name] = {
				size,
				label: isLeaf ? underlineLabel(baseLabel) : baseLabel,
				packageName: meta.packageName,
				isRootMarker: meta.isRootMarker === true,
				sourceName: meta.sourceName || null,
				isLeaf
			};
			if (meta.group) {
				components[name].group = meta.group;
			}
		}

		const dependencies = {};
		const maxStrength = Math.max(1, ...Object.values(dependencyStrengths), 1);
		for (const [name, strength] of Object.entries(dependencyStrengths)) {
			dependencies[name] = {
				strength: Math.max(1, Math.round(strength / maxStrength * 8 + 1))
			};
		}

		return {
			level: levelName,
			groups,
			components,
			dependencies
		};
	}

	function visibleComponentForPackage(codemap, packageFqn, levelName, expandedSet) {
		const packet = codemap.packages.find(candidate => candidate.fqn === packageFqn);
		const sourceName = packet ? packet.sourceName : null;

		if (packageFqn === levelName) {
			return {
				name: '[root]',
				label: '[root]',
				packageName: levelName,
				group: null,
				sourceName
			};
		}

		const levelPrefix = levelName === '' ? '' : (levelName + '.');
		if (!packageFqn.startsWith(levelPrefix)) return null;

		const suffix = packageFqn.slice(levelPrefix.length);
		if (!suffix) {
			return {
				name: '[root]',
				label: '[root]',
				packageName: levelName,
				group: null,
				isRootMarker: false,
				sourceName
			};
		}

		const segments = suffix.split('.').filter(Boolean);
		const visibleSegments = [];

		for (const [index, segment] of segments.entries()) {
			visibleSegments.push(segment);
			const absoluteLevel = appendLevel(levelName, visibleSegments.join('.'));
			const hasMoreSegments = index < segments.length - 1;
			const isExpandedLevel = expandedSet.has(absoluteLevel);
			const hasChildren = hasChildPackages(codemap, absoluteLevel);
			const isExpandedParentPackage = isExpandedLevel && hasChildren && !hasMoreSegments;
			if (isExpandedParentPackage) {
				return {
					name: visibleSegments.join('.') + '.[root]',
					label: '[root]',
					packageName: absoluteLevel,
					isRootMarker: true,
					sourceName,
					group: {
						name: visibleSegments.join('.'),
						label: lastSegment(visibleSegments.join('.')),
						packageName: absoluteLevel,
						sourceName
					}
				};
			}
			if (!isExpandedLevel || !hasMoreSegments) {
				return {
					name: visibleSegments.join('.'),
					label: lastSegment(visibleSegments.join('.')),
					packageName: absoluteLevel,
					isRootMarker: false,
					sourceName,
					group: visibleSegments.length > 1
						? {
							name: visibleSegments.slice(0, -1).join('.'),
							label: lastSegment(visibleSegments.slice(0, -1).join('.')),
							packageName: appendLevel(levelName, visibleSegments.slice(0, -1).join('.')),
							sourceName
						}
						: null
				};
			}
		}

		return {
			name: visibleSegments.join('.'),
			label: lastSegment(visibleSegments.join('.')),
			packageName: appendLevel(levelName, visibleSegments.join('.')),
			isRootMarker: false,
			sourceName,
			group: visibleSegments.length > 1
				? {
					name: visibleSegments.slice(0, -1).join('.'),
					label: lastSegment(visibleSegments.slice(0, -1).join('.')),
					packageName: appendLevel(levelName, visibleSegments.slice(0, -1).join('.')),
					sourceName
				}
				: null
		};
	}

	function filterLevel(originalLevel, codemap, keyword) {
		if (!keyword || !codemap) return originalLevel;

		const normalizedKeyword = keyword.toLowerCase();
		const coverageIndex = buildCoverageIndex(originalLevel.components);
		const matchedPairs = new Set();
		const componentSet = new Set();

		for (const [componentName, component] of Object.entries(originalLevel.components)) {
			if (
				matchesKeyword(componentName, normalizedKeyword) ||
				matchesKeyword(component.label, normalizedKeyword) ||
				matchesKeyword(component.packageName, normalizedKeyword)
			) {
				componentSet.add(componentName);
			}
		}

		for (const pkg of codemap.packageDeps) {
			const packageMatched =
				matchesKeyword(pkg.from.fqn, normalizedKeyword) ||
				matchesKeyword(pkg.to.fqn, normalizedKeyword);
			for (const memberDep of pkg.memberDeps) {
				if (
					packageMatched ||
					matchesKeyword(memberDep.from.className, normalizedKeyword) ||
					matchesKeyword(memberDep.from.memberName, normalizedKeyword) ||
					matchesKeyword(memberDep.to.className, normalizedKeyword) ||
					matchesKeyword(memberDep.to.memberName, normalizedKeyword)
				) {
					const compFrom = mapPackageToVisibleComponent(pkg.from.fqn, coverageIndex);
					const compTo = mapPackageToVisibleComponent(pkg.to.fqn, coverageIndex);
					if (compFrom && compTo) {
						matchedPairs.add(JSON.stringify({from: compFrom.name, to: compTo.name}));
						componentSet.add(compFrom.name);
						componentSet.add(compTo.name);
					}
					break;
				}
			}
		}

		const filteredComponents = {};
		for (const componentName in originalLevel.components) {
			if (componentSet.has(componentName)) {
				filteredComponents[componentName] = originalLevel.components[componentName];
			}
		}

		const filteredGroups = {};
		for (const component of Object.values(filteredComponents)) {
			if (component.group) {
				registerGroupChain(filteredGroups, component.group, originalLevel.level);
			}
		}

		const filteredDependencies = {};
		for (const dependencyName in originalLevel.dependencies) {
			const [from, to] = dependencyName.split(' -> ');
			for (const pairStr of matchedPairs) {
				const pair = JSON.parse(pairStr);
				if (pair.from === from && pair.to === to) {
					filteredDependencies[dependencyName] = originalLevel.dependencies[dependencyName];
					break;
				}
			}
		}

		return {
			level: originalLevel.level,
			groups: filteredGroups,
			components: filteredComponents,
			dependencies: filteredDependencies
		};
	}

	function performSearch() {
		currentKeyword = searchInput.value.trim();
		renderCurrentLevel();
	}

	searchButton.addEventListener('click', performSearch);
	searchInput.addEventListener('keydown', event => {
		if (event.key === 'Enter') performSearch();
	});
	headerToggleButton.addEventListener('click', () => {
		headerCollapsed = !headerCollapsed;
		updateHeaderState();
		requestAnimationFrame(() => {
			if (currentDiagram) {
				currentDiagram.refreshContainerGeometry();
			}
		});
	});

	levelUpButton.addEventListener('click', () => {
		if (expandedLevels.length === 0) return;
		collapseExpandedLevel(expandedLevels[expandedLevels.length - 1]);
	});

	expandAllButton.addEventListener('click', () => {
		if (!hasCodemap()) return;
		expandedLevels.splice(0, expandedLevels.length, ...collectExpandableLevels(getCodemap(), rootLevelName));
		renderCurrentLevel();
	});
	updateHeaderState();

	function collapseExpandedLevel(levelName) {
		const levelPrefix = levelName + '.';
		for (let i = expandedLevels.length - 1; i >= 0; i--) {
			if (expandedLevels[i] === levelName || expandedLevels[i].startsWith(levelPrefix)) {
				expandedLevels.splice(i, 1);
			}
		}
		renderCurrentLevel();
	}

	function onDependencySelected(fromComponent, toComponent) {
		if (hasCodemap()) {
			console.info('selected dependency %s -> %s', fromComponent, toComponent);
			drawTable(mainTable, getCodemap(), fromComponent, toComponent, currentKeyword);
			mainTable.style.display = 'flex';
		} else {
			console.warn('socomo.data not yet loaded or completely missing');
		}
	}

	function onComponentSelected(packageName, isRootMarker) {
		if (!hasCodemap()) {
			console.warn('socomo.data not yet loaded or completely missing');
			return;
		}
		if (!packageName && !isRootMarker) return;

		if (isRootMarker) {
			const rootComponentName = packageName ? `${packageName}.[root]` : '[root]';
			drawClassList(mainTable, getCodemap(), rootComponentName, currentKeyword);
			mainTable.style.display = 'flex';
			return;
		}
		if (packageName === rootLevelName) return;

		const absoluteLevel = packageName;
		if (!hasChildPackages(getCodemap(), absoluteLevel)) {
			drawClassList(mainTable, getCodemap(), absoluteLevel, currentKeyword);
			mainTable.style.display = 'flex';
			return;
		}

		const existingIndex = expandedLevels.indexOf(absoluteLevel);
		if (existingIndex !== -1) {
			collapseExpandedLevel(absoluteLevel);
			return;
		}

		expandedLevels.push(absoluteLevel);
		renderCurrentLevel();
	}

	function hasChildPackages(codemap, levelName) {
		if (!codemap || !codemap.packages) return false;
		const prefix = levelName === '' ? '' : (levelName + '.');
		return codemap.packages.some(packet =>
			packet.fqn !== levelName
			&& packet.fqn.startsWith(prefix)
		);
	}

	function collectExpandableLevels(codemap, levelName) {
		if (!codemap || !codemap.packages) return [];
		const levels = new Set();
		const rootPrefix = levelName === '' ? '' : (levelName + '.');
		for (const packet of codemap.packages) {
			if (packet.fqn === levelName || !packet.fqn.startsWith(rootPrefix)) continue;
			const suffix = packet.fqn.slice(rootPrefix.length);
			const segments = suffix.split('.').filter(Boolean);
			const pathSegments = [];
			for (const segment of segments) {
				pathSegments.push(segment);
				const absoluteLevel = appendLevel(levelName, pathSegments.join('.'));
				if (hasChildPackages(codemap, absoluteLevel)) {
					levels.add(absoluteLevel);
				}
			}
		}
		return Array.from(levels).sort();
	}

	function matchesKeyword(value, normalizedKeyword) {
		return value != null && value.toLowerCase().includes(normalizedKeyword);
	}

	function appendLevel(levelName, suffix) {
		if (!suffix) return levelName;
		if (!levelName) return suffix;
		return levelName + '.' + suffix;
	}

	function relativeLevelName(rootLevel, absoluteLevel) {
		if (rootLevel === absoluteLevel) return '[root]';
		const prefix = rootLevel === '' ? '' : (rootLevel + '.');
		return absoluteLevel.startsWith(prefix)
			? absoluteLevel.slice(prefix.length)
			: absoluteLevel;
	}

	function lastSegment(levelName) {
		if (!levelName) return '[root]';
		const segments = levelName.split('.').filter(Boolean);
		return segments[segments.length - 1] || '[root]';
	}

	function underlineLabel(label) {
		return Array.from(label).map(character =>
			character === ' ' ? character : character + '\u0332'
		).join('');
	}

	function registerGroupChain(groups, group, levelName) {
		if (!group) return;
		const segments = group.name.split('.').filter(Boolean);
		for (let i = 0; i < segments.length; i++) {
			const groupName = segments.slice(0, i + 1).join('.');
			if (groups[groupName]) continue;
			const packageName = appendLevel(levelName, groupName);
			const isOutermostGroup = i === 0;
			groups[groupName] = {
				name: groupName,
				label: isOutermostGroup && group.sourceName
					? `${group.sourceName} / ${packageName}`
					: lastSegment(groupName),
				packageName,
				parentGroupName: i > 0 ? segments.slice(0, i).join('.') : null,
				sourceName: group.sourceName || null
			};
		}
	}

	function buildCoverageIndex(components) {
		return Object.entries(components).map(([name, props]) => ({
			name,
			packageName: props.packageName,
			exactOnly: props.isRootMarker === true
		})).sort((left, right) => right.packageName.length - left.packageName.length);
	}

	function mapPackageToVisibleComponent(packageFqn, coverageIndex) {
		for (const coverage of coverageIndex) {
			if (coverage.exactOnly) {
				if (packageFqn === coverage.packageName) {
					return coverage;
				}
				continue;
			}
			if (packageFqn === coverage.packageName || packageFqn.startsWith(coverage.packageName + '.')) {
				return coverage;
			}
		}
		return null;
	}

	Mousetrap.bind('esc', () => {
		mainTable.style.display = 'none';
	});
	mainTable.addEventListener('click', event => {
		if (event.target !== mainTable) return;
		mainTable.style.display = 'none';
	});
}

function doLongTask(task) {
	document.body.classList.add('loading-state');
	// the delay is to give browser a chance to render dom changes done so far
	setTimeout(() => {
		task();
		document.body.classList.remove('loading-state');
	}, 30);
}

// expose socomo function to the index.js bootstrapping code
window.socomo = socomo;
