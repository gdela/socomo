/*
 * Entry point for the visualizer single-page app.
 * The socomo.html skeleton that contains the composition data will call the socomo() function
 * (via index.js) passing the composition of the module to be visualized in user's browser.
 */

import './main.scss';
import drawDiagram from './diagram';
import drawTable, {buildDependencyExport, drawClassList} from './table';
import Mousetrap from 'mousetrap';
import saveAs from 'file-saver';

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
			<div id="search-controls">
				<input type="text" id="search-input" placeholder="search">
				<button id="search-button">搜索</button>
				<button id="export-button" type="button">导出</button>
			</div>
			<div id="keyword-status"></div>
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
	const exportButton = document.getElementById('export-button');
	const keywordStatus = document.getElementById('keyword-status');
	const levelUpButton = document.getElementById('level-up-button');
	const expandAllButton = document.getElementById('expand-all-button');
	const breadcrumb = document.getElementById('breadcrumb');
	const headerToggleButton = document.getElementById('header-toggle-button');

	let currentDiagram;
	let currentKeywords = [];
	let currentDisplayedLevel = initialLevel;
	let headerCollapsed = false;
	let codemapSynchronized = hasCodemap();
	const expandedLevels = [];
	let searchExpandedLevels = [];

	renderCurrentLevel();
	waitForCodemap();

	function renderCurrentLevel() {
		const activeKeywords = getActiveKeywords();
		updateNavigation();
		const effectiveExpandedLevels = mergeExpandedLevels(expandedLevels, searchExpandedLevels);
		const baseLevel = buildExpandedLevel(getCodemap(), rootLevelName, effectiveExpandedLevels, initialLevel);
		const displayedLevel = currentKeywords.length > 0 && activeKeywords.length === 0
			? {
				level: baseLevel.level,
				groups: {},
				components: {},
				dependencies: {}
			}
			: filterLevel(baseLevel, getCodemap(), activeKeywords, effectiveExpandedLevels);
		currentDisplayedLevel = displayedLevel;
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
			const expandedRef = parseExpandedLevelKey(expandedLevel);
			const button = document.createElement('button');
			button.type = 'button';
			button.className = 'breadcrumb-item';
			button.textContent = expandedRef.sourceName
				? `${expandedRef.sourceName} / ${relativeLevelName(rootLevelName, expandedRef.packageName)}`
				: relativeLevelName(rootLevelName, expandedRef.packageName);
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
			const visibleComponent = visibleComponentForPackage(codemap, packet, levelName, expandedSet);
			if (!visibleComponent) continue;
			componentSizes[visibleComponent.name] = (componentSizes[visibleComponent.name] || 0) + packet.size;
			componentMeta[visibleComponent.name] = visibleComponent;
			if (visibleComponent.group) {
				registerGroupChain(groups, visibleComponent.group, levelName);
			}
		}

		const coverageIndex = buildCoverageIndex(componentMeta);

		for (const packageDep of codemap.packageDeps) {
			const fromComponent = mapPackageToVisibleComponent(packageDep.from, coverageIndex);
			const toComponent = mapPackageToVisibleComponent(packageDep.to, coverageIndex);
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
				expansionKey: meta.expansionKey,
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

	function visibleComponentForPackage(codemap, packet, levelName, expandedSet) {
		const packageFqn = packet.fqn;

		if (packageFqn === levelName) {
			const sourceName = findSourceNameForPackageTree(codemap, levelName);
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
			const sourceName = findSourceNameForPackageTree(codemap, levelName);
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
			const splitBySource = shouldSplitPackageTreeBySource(codemap, absoluteLevel);
			const sourceName = splitBySource ? packet.sourceName : findSourceNameForPackageTree(codemap, absoluteLevel);
			const visibleName = scopedVisibleName(visibleSegments.join('.'), sourceName, splitBySource);
			const parentVisibleName = scopedVisibleName(visibleSegments.slice(0, -1).join('.'), sourceName, splitBySource);
			const expansionKey = expandedLevelKey(absoluteLevel, splitBySource ? sourceName : null);
			const hasMoreSegments = index < segments.length - 1;
			const isExpandedLevel = expandedSet.has(expansionKey);
			const hasChildren = hasChildPackages(codemap, absoluteLevel, splitBySource ? sourceName : null);
			const isExpandedParentPackage = isExpandedLevel && hasChildren && !hasMoreSegments;
			if (isExpandedParentPackage) {
				return {
					name: visibleName + '.[root]',
					label: '[root]',
					packageName: absoluteLevel,
					isRootMarker: true,
					sourceName,
					expansionKey,
					group: {
						name: visibleName,
						label: lastSegment(visibleSegments.join('.')),
						packageName: absoluteLevel,
						sourceName,
						expansionKey
					}
				};
			}
			if (!isExpandedLevel || !hasMoreSegments) {
				return {
					name: visibleName,
					label: lastSegment(visibleSegments.join('.')),
					packageName: absoluteLevel,
					isRootMarker: false,
					sourceName,
					expansionKey,
					group: visibleSegments.length > 1
						? {
							name: parentVisibleName,
							label: lastSegment(visibleSegments.slice(0, -1).join('.')),
							packageName: appendLevel(levelName, visibleSegments.slice(0, -1).join('.')),
							sourceName,
							expansionKey: expandedLevelKey(appendLevel(levelName, visibleSegments.slice(0, -1).join('.')), splitBySource ? sourceName : null)
						}
						: null
				};
			}
		}

		const absoluteLevel = appendLevel(levelName, visibleSegments.join('.'));
		const splitBySource = shouldSplitPackageTreeBySource(codemap, absoluteLevel);
		const sourceName = splitBySource ? packet.sourceName : findSourceNameForPackageTree(codemap, absoluteLevel);
		return {
			name: scopedVisibleName(visibleSegments.join('.'), sourceName, splitBySource),
			label: lastSegment(visibleSegments.join('.')),
			packageName: absoluteLevel,
			isRootMarker: false,
			sourceName,
			expansionKey: expandedLevelKey(absoluteLevel, splitBySource ? sourceName : null),
			group: visibleSegments.length > 1
				? {
					name: scopedVisibleName(visibleSegments.slice(0, -1).join('.'), sourceName, splitBySource),
					label: lastSegment(visibleSegments.slice(0, -1).join('.')),
					packageName: appendLevel(levelName, visibleSegments.slice(0, -1).join('.')),
					sourceName,
					expansionKey: expandedLevelKey(appendLevel(levelName, visibleSegments.slice(0, -1).join('.')), splitBySource ? sourceName : null)
				}
				: null
		};
	}

	function filterLevel(originalLevel, codemap, keywords) {
		if (!keywords.length || !codemap) return originalLevel;

		const coverageIndex = buildCoverageIndex(originalLevel.components);
		const matchedPairs = new Set();
		const componentSet = new Set();

		for (const [componentName, component] of Object.entries(originalLevel.components)) {
			if (
				matchesAnyKeyword(componentName, keywords) ||
				matchesAnyKeyword(component.label, keywords) ||
				matchesAnyKeyword(component.packageName, keywords)
			) {
				componentSet.add(componentName);
			}
		}

		for (const packet of codemap.packages || []) {
			const packetMatched =
				matchesAnyKeyword(packet.fqn, keywords) ||
				matchesAnyKeyword(packet.sourceName, keywords);
			for (const member of packet.members || []) {
				if (
					packetMatched ||
					matchesAnyKeyword(member.className, keywords) ||
					matchesAnyKeyword(member.memberName, keywords)
				) {
					const component = mapPackageToVisibleComponent(packet, coverageIndex);
					if (component) {
						componentSet.add(component.name);
					}
					break;
				}
			}
		}

		for (const pkg of codemap.packageDeps || []) {
			const packageMatched =
				matchesAnyKeyword(pkg.from.fqn, keywords) ||
				matchesAnyKeyword(pkg.to.fqn, keywords) ||
				matchesAnyKeyword(pkg.from.sourceName, keywords) ||
				matchesAnyKeyword(pkg.to.sourceName, keywords);
			for (const memberDep of pkg.memberDeps || []) {
				if (
					packageMatched ||
					matchesAnyKeyword(memberDep.from.className, keywords) ||
					matchesAnyKeyword(memberDep.from.memberName, keywords) ||
					matchesAnyKeyword(memberDep.to.className, keywords) ||
					matchesAnyKeyword(memberDep.to.memberName, keywords)
				) {
					const compFrom = mapPackageToVisibleComponent(pkg.from, coverageIndex);
					const compTo = mapPackageToVisibleComponent(pkg.to, coverageIndex);
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
		currentKeywords = parseSearchKeywords(searchInput.value);
		updateSearchState();
	}

	function waitForCodemap() {
		if (codemapSynchronized) return;
		const synchronize = () => {
			if (!hasCodemap()) {
				window.setTimeout(synchronize, 50);
				return;
			}
			codemapSynchronized = true;
			updateSearchState();
		};
		window.setTimeout(synchronize, 50);
	}

	searchButton.addEventListener('click', performSearch);
	searchInput.addEventListener('keydown', event => {
		if (event.key === 'Enter') performSearch();
	});
	exportButton.addEventListener('click', () => {
		if (!hasCodemap()) {
			console.warn('socomo.data not yet loaded or completely missing');
			return;
		}
		const workbook = buildDependencyExport(getCodemap(), currentDisplayedLevel, getActiveKeywords());
		saveAs(new Blob([workbook.content], {type: 'application/vnd.ms-excel;charset=utf-8'}), workbook.filename);
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
		const target = parseExpandedLevelKey(levelName);
		for (let i = expandedLevels.length - 1; i >= 0; i--) {
			const candidate = parseExpandedLevelKey(expandedLevels[i]);
			const sameSource = candidate.sourceName === target.sourceName;
			const sameLevel = candidate.packageName === target.packageName;
			const nestedLevel = candidate.packageName.startsWith(target.packageName + '.');
			if (sameSource && (sameLevel || nestedLevel)) {
				expandedLevels.splice(i, 1);
			}
		}
		renderCurrentLevel();
	}

	function onDependencySelected(fromComponent, toComponent, fromSourceName, toSourceName) {
		if (hasCodemap()) {
			console.info('selected dependency %s -> %s', fromComponent, toComponent);
			drawTable(mainTable, getCodemap(), fromComponent, toComponent, getActiveKeywords(), fromSourceName, toSourceName);
			mainTable.style.display = 'flex';
		} else {
			console.warn('socomo.data not yet loaded or completely missing');
		}
	}

	function onComponentSelected(packageName, isRootMarker, sourceName) {
		if (!hasCodemap()) {
			console.warn('socomo.data not yet loaded or completely missing');
			return;
		}
		if (!packageName && !isRootMarker) return;

		if (isRootMarker) {
			const rootComponentName = packageName ? `${packageName}.[root]` : '[root]';
			drawClassList(mainTable, getCodemap(), rootComponentName, getActiveKeywords(), sourceName);
			mainTable.style.display = 'flex';
			return;
		}
		if (packageName === rootLevelName) return;

		const absoluteLevel = packageName;
		if (!hasChildPackages(getCodemap(), absoluteLevel, sourceName)) {
			drawClassList(mainTable, getCodemap(), absoluteLevel, getActiveKeywords(), sourceName);
			mainTable.style.display = 'flex';
			return;
		}

		const currentLevelKey = expandedLevelKey(absoluteLevel, shouldSplitPackageTreeBySource(getCodemap(), absoluteLevel) ? sourceName : null);
		const existingIndex = expandedLevels.indexOf(currentLevelKey);
		if (existingIndex !== -1) {
			collapseExpandedLevel(currentLevelKey);
			return;
		}

		expandedLevels.push(currentLevelKey);
		renderCurrentLevel();
	}

	function hasChildPackages(codemap, levelName, sourceName = null) {
		if (!codemap || !codemap.packages) return false;
		const prefix = levelName === '' ? '' : (levelName + '.');
		return codemap.packages.some(packet =>
			(sourceName == null || packet.sourceName === sourceName)
			&&
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
				if (hasChildPackages(codemap, absoluteLevel, shouldSplitPackageTreeBySource(codemap, absoluteLevel) ? packet.sourceName : null)) {
					levels.add(expandedLevelKey(absoluteLevel, shouldSplitPackageTreeBySource(codemap, absoluteLevel) ? packet.sourceName : null));
				}
			}
		}
		return Array.from(levels).sort();
	}

	function collectSearchExpansionLevels(codemap, levelName, keywords) {
		if (!codemap || !codemap.packages || !keywords.length) return [];
		const levels = new Set();
		for (const packet of codemap.packages) {
			const packetMatched = matchesAnyKeyword(packet.fqn, keywords);
			const memberMatched = (packet.members || []).some(member =>
				matchesAnyKeyword(member.className, keywords) ||
				matchesAnyKeyword(member.memberName, keywords)
			);
			if (!packetMatched && !memberMatched) continue;
			addExpansionPathForPacket(codemap, levelName, packet, levels);
		}
		for (const packageDep of codemap.packageDeps || []) {
			if (dependencyMatchesKeyword(packageDep, keywords)) {
				addExpansionPathForPacket(codemap, levelName, packageDep.from, levels);
				addExpansionPathForPacket(codemap, levelName, packageDep.to, levels);
			}
		}
		return Array.from(levels).sort();
	}

	function addExpansionPathForPacket(codemap, levelName, packet, levels) {
		const rootPrefix = levelName === '' ? '' : (levelName + '.');
		if (packet.fqn === levelName || !packet.fqn.startsWith(rootPrefix)) return;
		const suffix = packet.fqn.slice(rootPrefix.length);
		const segments = suffix.split('.').filter(Boolean);
		const pathSegments = [];
		for (const segment of segments) {
			pathSegments.push(segment);
			const absoluteLevel = appendLevel(levelName, pathSegments.join('.'));
			const sourceName = shouldSplitPackageTreeBySource(codemap, absoluteLevel) ? packet.sourceName : null;
			if (hasChildPackages(codemap, absoluteLevel, sourceName)) {
				levels.add(expandedLevelKey(absoluteLevel, sourceName));
			}
		}
	}

	function dependencyMatchesKeyword(packageDep, keywords) {
		if (
			matchesAnyKeyword(packageDep.from.fqn, keywords) ||
			matchesAnyKeyword(packageDep.to.fqn, keywords) ||
			matchesAnyKeyword(packageDep.from.sourceName, keywords) ||
			matchesAnyKeyword(packageDep.to.sourceName, keywords)
		) {
			return true;
		}
		return (packageDep.memberDeps || []).some(memberDep =>
			matchesAnyKeyword(memberDep.from.className, keywords) ||
			matchesAnyKeyword(memberDep.from.memberName, keywords) ||
			matchesAnyKeyword(memberDep.to.className, keywords) ||
			matchesAnyKeyword(memberDep.to.memberName, keywords)
		);
	}

	function parseSearchKeywords(searchText) {
		const keywords = [];
		const seen = new Set();
		for (const rawKeyword of String(searchText || '').split(/[\s,，;；]+/)) {
			const raw = rawKeyword.trim();
			if (!raw) continue;
			const normalized = raw.toLowerCase();
			if (seen.has(normalized)) continue;
			seen.add(normalized);
			keywords.push({raw, normalized, enabled: true});
		}
		return keywords;
	}

	function getActiveKeywords() {
		return currentKeywords.filter(keyword => keyword.enabled !== false);
	}

	function updateSearchState() {
		const activeKeywords = getActiveKeywords();
		searchExpandedLevels = activeKeywords.length
			? collectSearchExpansionLevels(getCodemap(), rootLevelName, activeKeywords)
			: [];
		updateKeywordStatus();
		renderCurrentLevel();
	}

	function updateKeywordStatus() {
		keywordStatus.innerHTML = '';
		if (!currentKeywords.length) return;
		const hitCounts = countKeywordHits(getCodemap(), currentKeywords);
		for (const keyword of currentKeywords) {
			const count = hitCounts.get(keyword.normalized) || 0;
			const tag = document.createElement('button');
			tag.type = 'button';
			tag.className = `keyword-status-tag ${count > 0 ? 'hit' : 'miss'} ${keyword.enabled === false ? 'inactive' : 'active'}`;
			tag.title = count > 0
				? (keyword.enabled === false ? `当前未参与过滤，命中 ${count} 条记录` : `当前参与过滤，命中 ${count} 条记录`)
				: '没有命中记录';
			tag.textContent = `${keyword.raw}: ${count > 0 ? count : '未命中'}`;
			tag.addEventListener('click', () => {
				keyword.enabled = keyword.enabled === false;
				updateSearchState();
			});
			keywordStatus.appendChild(tag);
		}
	}

	function countKeywordHits(codemap, keywords) {
		const hits = new Map(keywords.map(keyword => [keyword.normalized, new Set()]));
		if (!codemap) return new Map(keywords.map(keyword => [keyword.normalized, 0]));

		for (const packet of codemap.packages || []) {
			addKeywordHit(hits, keywords, [packet.sourceName, packet.fqn].filter(x => x).join(' '), `pkg:${packet.sourceName || ''}:${packet.fqn}`);
			for (const member of packet.members || []) {
				addKeywordHit(
					hits,
					keywords,
					[packet.sourceName, member.className, member.memberName].filter(x => x).join('.'),
					`member:${packet.sourceName || ''}:${packet.fqn}:${member.className || ''}:${member.memberName || ''}`
				);
			}
		}
		for (const packageDep of codemap.packageDeps || []) {
			addKeywordHit(
				hits,
				keywords,
				[packageDep.from.sourceName, packageDep.from.fqn, packageDep.to.sourceName, packageDep.to.fqn].filter(x => x).join(' -> '),
				`dep:${packageDep.from.sourceName || ''}:${packageDep.from.fqn}->${packageDep.to.sourceName || ''}:${packageDep.to.fqn}`
			);
			for (const memberDep of packageDep.memberDeps || []) {
				addKeywordHit(
					hits,
					keywords,
					[
						packageDep.from.sourceName,
						memberDep.from.className,
						memberDep.from.memberName,
						packageDep.to.sourceName,
						memberDep.to.className,
						memberDep.to.memberName
					].filter(x => x).join(' -> '),
					`memberDep:${packageDep.from.sourceName || ''}:${packageDep.from.fqn}:${memberDep.from.className || ''}:${memberDep.from.memberName || ''}->${packageDep.to.sourceName || ''}:${packageDep.to.fqn}:${memberDep.to.className || ''}:${memberDep.to.memberName || ''}`
				);
			}
		}

		return new Map(Array.from(hits.entries()).map(([keyword, records]) => [keyword, records.size]));
	}

	function addKeywordHit(hits, keywords, value, recordKey) {
		for (const keyword of keywords) {
			if (matchesKeyword(value, keyword.normalized)) {
				hits.get(keyword.normalized).add(recordKey);
			}
		}
	}

	function matchesAnyKeyword(value, keywords) {
		return keywords.some(keyword => matchesKeyword(value, keyword.normalized));
	}

	function matchesKeyword(value, normalizedKeyword) {
		return value != null && String(value).toLowerCase().includes(normalizedKeyword);
	}

	function mergeExpandedLevels(primaryLevels, secondaryLevels) {
		return Array.from(new Set([...primaryLevels, ...secondaryLevels]));
	}

	function findSourceNameForPackageTree(codemap, packageFqn) {
		if (!codemap || !codemap.packages) return null;
		const exactMatch = codemap.packages.find(candidate => candidate.fqn === packageFqn && candidate.sourceName);
		if (exactMatch) return exactMatch.sourceName;
		const prefix = packageFqn ? (packageFqn + '.') : '';
		const sourceNames = new Set();
		for (const packet of codemap.packages) {
			if (!packet.sourceName) continue;
			if (packet.fqn === packageFqn || packet.fqn.startsWith(prefix)) {
				sourceNames.add(packet.sourceName);
				if (sourceNames.size > 1) {
					return null;
				}
			}
		}
		return sourceNames.size === 1 ? Array.from(sourceNames)[0] : null;
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
				sourceName: group.sourceName || null,
				expansionKey: group.expansionKey || expandedLevelKey(packageName, group.sourceName)
			};
		}
	}

	function buildCoverageIndex(components) {
		return Object.entries(components).map(([name, props]) => ({
			name,
			packageName: props.packageName,
			sourceName: props.sourceName || null,
			exactOnly: props.isRootMarker === true
		})).sort((left, right) => right.packageName.length - left.packageName.length);
	}

	function mapPackageToVisibleComponent(packet, coverageIndex) {
		for (const coverage of coverageIndex) {
			if (coverage.sourceName && packet.sourceName !== coverage.sourceName) continue;
			if (coverage.exactOnly) {
				if (packet.fqn === coverage.packageName) {
					return coverage;
				}
				continue;
			}
			if (packet.fqn === coverage.packageName || packet.fqn.startsWith(coverage.packageName + '.')) {
				return coverage;
			}
		}
		return null;
	}

	function shouldSplitPackageTreeBySource(codemap, packageName) {
		return collectSourceNamesForPackageTree(codemap, packageName).length > 1;
	}

	function collectSourceNamesForPackageTree(codemap, packageName) {
		if (!codemap || !codemap.packages) return [];
		const prefix = packageName ? (packageName + '.') : '';
		const sourceNames = new Set();
		for (const packet of codemap.packages) {
			if (!packet.sourceName) continue;
			if (packet.fqn === packageName || packet.fqn.startsWith(prefix)) {
				sourceNames.add(packet.sourceName);
			}
		}
		return Array.from(sourceNames).sort();
	}

	function expandedLevelKey(packageName, sourceName) {
		return `${sourceName || ''}::${packageName}`;
	}

	function parseExpandedLevelKey(key) {
		const separatorIndex = key.indexOf('::');
		return {
			sourceName: separatorIndex > 0 ? key.slice(0, separatorIndex) : '',
			packageName: separatorIndex >= 0 ? key.slice(separatorIndex + 2) : key
		};
	}

	function scopedVisibleName(baseName, sourceName, splitBySource) {
		return splitBySource && sourceName ? `${baseName}@@${sourceName}` : baseName;
	}

	Mousetrap.bind('esc', () => {
		mainTable.style.display = 'none';
	});
	mainTable.addEventListener('click', event => {
		if (document.body.classList.contains('is-resizing-panel')) return;
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
