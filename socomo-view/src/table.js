/*
 * Drawing and controls of the component dependency table (what package deps
 * and their member deps constitute a given dependency between two components).
 */

import './table.scss';

export default drawTable;
export { drawClassList, buildDependencyExport };

function drawTable(tableContainer, codemap, fromComponentName, toComponentName, keyword, fromSourceName = null, toSourceName = null) {
	const filteredDeps = filterAndRelativize(codemap.packageDeps, fromComponentName, toComponentName, fromSourceName, toSourceName);
	const table = createTable(filteredDeps, codemap, fromComponentName, toComponentName, keyword);
	tableContainer.innerHTML = '';
	tableContainer.appendChild(table);
	return table;
}

function drawClassList(tableContainer, codemap, componentName, keyword, sourceName = null) {
	const classes = listClasses(codemap, componentName, keyword, sourceName);
	const panel = createClassList(classes, codemap, componentName, keyword, sourceName);
	tableContainer.innerHTML = '';
	tableContainer.appendChild(panel);
	return panel;
}

function buildDependencyExport(codemap, level, keyword) {
	const keywords = normalizeKeywords(keyword);
	const sheets = [];
	const keywordGroups = keywords.length > 0 ? keywords : [null];
	const usedSheetNames = new Set();

	for (const keywordItem of keywordGroups) {
		const rows = [];
		for (const dependencyName of Object.keys(level.dependencies || {})) {
			const [fromComponentName, toComponentName] = dependencyName.split(' -> ');
			const fromComponent = level.components[fromComponentName];
			const toComponent = level.components[toComponentName];
			if (!fromComponent || !toComponent) continue;
			rows.push(...dependencyRows(
				codemap,
				componentFilterName(fromComponent),
				componentFilterName(toComponent),
				keywordItem ? [keywordItem] : keywords,
				fromComponent.sourceName || null,
				toComponent.sourceName || null,
				componentLabel(fromComponent, fromComponentName),
				componentLabel(toComponent, toComponentName)
			));
		}
		sheets.push({
			name: buildSheetName(keywordItem ? keywordItem.raw : 'Dependencies', usedSheetNames),
			rows
		});
	}

	return {
		filename: 'socomo-dependencies.xls',
		content: renderExcelWorkbook(sheets)
	};
}

function filterAndRelativize(packageDeps, fromComponentName, toComponentName, fromSourceName, toSourceName) {
	const trimRoot = (componentName) => componentName.replace('.[root]', '').replace('[root]', '');
	const belongsTo = (componentName, packet) => {
		if (componentName.endsWith('[root]')) {
			return packet.fqn === trimRoot(componentName);
		} else {
			return packet.fqn.startsWith(componentName);
		}
	};
	return packageDeps
		.filter(packageDep =>
			(!fromSourceName || packageDep.from.sourceName === fromSourceName)
			&& (!toSourceName || packageDep.to.sourceName === toSourceName)
			&&
			belongsTo(fromComponentName, packageDep.from)
			&& belongsTo(toComponentName, packageDep.to)
		)
		.map(packageDep => {
			return {
				...packageDep,
				from: {
					fqn: packageDep.from.fqn,
					sourceName: packageDep.from.sourceName || null,
					relativeName: packageDep.from.fqn.replace(trimRoot(fromComponentName), '')
				},
				to: {
					fqn: packageDep.to.fqn,
					sourceName: packageDep.to.sourceName || null,
					relativeName: packageDep.to.fqn.replace(trimRoot(toComponentName), '')
				}
			};
		});
}

function dependencyRows(codemap, fromComponentName, toComponentName, keyword, fromSourceName, toSourceName, fromComponentLabel, toComponentLabel) {
	const packageDeps = filterAndRelativize(codemap.packageDeps, fromComponentName, toComponentName, fromSourceName, toSourceName);
	const keywords = normalizeKeywords(keyword);
	const rows = [];
	for (const packageDep of packageDeps) {
		for (const memberDep of packageDep.memberDeps) {
			if (!matchesDependencyKeyword(packageDep, memberDep, keywords)) continue;
			rows.push({
				fromComponent: fromComponentLabel,
				fromClass: fromPackageAndClassName(packageDep, memberDep),
				fromMember: fromMemberName(memberDep),
				dependency: memberDep.inLoop ? '-> 循环' : '->',
				toMember: plainText(toMemberName(memberDep)),
				toClass: toPackageAndClassName(packageDep, memberDep),
				toComponent: toComponentLabel
			});
		}
	}
	return rows;
}

function matchesDependencyKeyword(packageDep, memberDep, keywords) {
	return keywords.length === 0 ||
		matchesAnyKeyword(packageDep.from.fqn, keywords) ||
		matchesAnyKeyword(packageDep.to.fqn, keywords) ||
		matchesAnyKeyword(packageDep.from.sourceName, keywords) ||
		matchesAnyKeyword(packageDep.to.sourceName, keywords) ||
		matchesAnyKeyword(memberDep.from.className, keywords) ||
		matchesAnyKeyword(memberDep.from.memberName, keywords) ||
		matchesAnyKeyword(memberDep.to.className, keywords) ||
		matchesAnyKeyword(memberDep.to.memberName, keywords);
}

function normalizeKeywords(keywordInput) {
	if (Array.isArray(keywordInput)) {
		return keywordInput
			.map(keyword => typeof keyword === 'string'
				? {raw: keyword, normalized: keyword.toLowerCase()}
				: {
					raw: String(keyword.raw || keyword.normalized || ''),
					normalized: String(keyword.normalized || keyword.raw || '').toLowerCase()
				})
			.filter(keyword => keyword.normalized);
	}
	const seen = new Set();
	const keywords = [];
	for (const rawKeyword of String(keywordInput || '').split(/[\s,，;；]+/)) {
		const raw = rawKeyword.trim();
		const normalized = raw.toLowerCase();
		if (!normalized || seen.has(normalized)) continue;
		seen.add(normalized);
		keywords.push({raw, normalized});
	}
	return keywords;
}

function matchesAnyKeyword(value, keywords) {
	return value != null && keywords.some(keyword => String(value).toLowerCase().includes(keyword.normalized));
}

function createTable(packageDeps, codemap, fromComponentName, toComponentName, keyword) {
	const keywords = normalizeKeywords(keyword);
	const thead = document.createElement('thead');
	thead.insertRow().innerHTML = `
		<th colspan="2">${fromComponentName}</th>
		<th>&rightarrow;</th>
		<th colspan="2">${toComponentName}</th>
	`;
	const tbody = document.createElement('tbody');
	const reverseGraph = buildReverseCallGraph(codemap);
	for (const packageDep of packageDeps) {
		for (const memberDep of packageDep.memberDeps) {
			if (matchesDependencyKeyword(packageDep, memberDep, keywords)) {
				const row = tbody.insertRow();
				appendClassCell(row.insertCell(), {
					packageFqn: packageDep.from.fqn,
					sourceName: packageDep.from.sourceName,
					className: memberDep.from.className,
					label: fromPackageAndClassName(packageDep, memberDep)
				});
				appendMemberCell(row.insertCell(), {
					packageFqn: packageDep.from.fqn,
					sourceName: packageDep.from.sourceName,
					className: memberDep.from.className,
					memberName: memberDep.from.memberName,
					label: fromMemberName(memberDep)
				});
				appendArrowCell(row.insertCell(), memberDep);
				appendMemberCell(row.insertCell(), {
					packageFqn: packageDep.to.fqn,
					sourceName: packageDep.to.sourceName,
					className: memberDep.to.className,
					memberName: memberDep.to.memberName,
					label: toMemberName(memberDep)
				});
				appendClassCell(row.insertCell(), {
					packageFqn: packageDep.to.fqn,
					sourceName: packageDep.to.sourceName,
					className: memberDep.to.className,
					label: toPackageAndClassName(packageDep, memberDep)
				});
			}
		}
	}
	const table = document.createElement('table');
	table.appendChild(thead);
	table.appendChild(tbody);
	const tableWrapper = document.createElement('div');
	tableWrapper.id = 'table-wrapper';
	const tablePane = document.createElement('div');
	tablePane.id = 'table-pane';
	tablePane.appendChild(table);
	tableWrapper.appendChild(tablePane);
	const splitHandle = document.createElement('div');
	splitHandle.id = 'table-splitter';
	splitHandle.title = '拖动调整表格和调用链的高度占比';
	tableWrapper.appendChild(splitHandle);
	const tracePanel = document.createElement('div');
	tracePanel.id = 'call-trace-panel';
	tracePanel.innerHTML = '<div class="call-trace-placeholder">点击表格中的类名或方法名，向上追溯调用链。</div>';
	tableWrapper.appendChild(tracePanel);
	const resizeHandle = document.createElement('div');
	resizeHandle.className = 'table-resize-handle';
	resizeHandle.title = '拖动调整详情表格大小';
	tableWrapper.appendChild(resizeHandle);
	setupVerticalResize(tableWrapper, tablePane, tracePanel, splitHandle);
	setupPanelResize(tableWrapper, resizeHandle);
	tableWrapper.addEventListener('click', event => {
		event.stopPropagation();
		const trigger = event.target.closest('.trace-link');
		if (!trigger) return;
		const target = {
			packageFqn: trigger.dataset.packageFqn,
			sourceName: trigger.dataset.sourceName || null,
			className: trigger.dataset.className,
			memberName: trigger.dataset.memberName || null
		};
		renderCallTrace(tracePanel, traceToTop(reverseGraph, target));
	});
	return tableWrapper;
}

function listClasses(codemap, componentName, keyword, sourceName = null) {
	const trimRoot = componentName.replace('.[root]', '').replace('[root]', '');
	const keywords = normalizeKeywords(keyword);
	const classes = new Map();

	for (const packet of codemap.packages || []) {
		if (sourceName && packet.sourceName !== sourceName) continue;
		const packetMembers = packet.members || [];
		const belongsTo = componentName.endsWith('[root]')
			? packet.fqn === trimRoot
			: packet.fqn === trimRoot || packet.fqn.startsWith(trimRoot + '.');
		if (!belongsTo) continue;
		for (const member of packetMembers) {
			addClassEntry(classes, packet.fqn, packet.sourceName || null, member.className, keywords);
		}
	}

	if (classes.size > 0) {
		return Array.from(classes.values()).sort((left, right) => left.fullClassName.localeCompare(right.fullClassName));
	}

	for (const packageDep of codemap.packageDeps || []) {
		collectClassesFromMember(packageDep.from, packageDep.memberDeps, 'from', componentName, trimRoot, keywords, classes, sourceName);
		collectClassesFromMember(packageDep.to, packageDep.memberDeps, 'to', componentName, trimRoot, keywords, classes, sourceName);
	}

	return Array.from(classes.values()).sort((left, right) => left.fullClassName.localeCompare(right.fullClassName));
}

function collectClassesFromMember(packet, memberDeps, side, componentName, trimRoot, keywords, classes, sourceName) {
	if (sourceName && packet.sourceName !== sourceName) return;
	if (!belongsToComponent(componentName, trimRoot, packet.fqn)) return;
	for (const memberDep of memberDeps || []) {
		addClassEntry(classes, packet.fqn, packet.sourceName || null, memberDep[side].className, keywords);
	}
}

function belongsToComponent(componentName, trimRoot, packageFqn) {
	return componentName.endsWith('[root]')
		? packageFqn === trimRoot
		: packageFqn === trimRoot || packageFqn.startsWith(trimRoot + '.');
}

function addClassEntry(classes, packageFqn, sourceName, className, keywords) {
	if (!className) return;
	const fullClassName = packageFqn ? `${packageFqn}.${className}` : className;
	const entryKey = `${sourceName || ''}::${fullClassName}`;
	if (keywords.length > 0 && !matchesAnyKeyword([sourceName, fullClassName].filter(x => x).join(' '), keywords)) return;
	classes.set(entryKey, {
		packageFqn,
		sourceName,
		className,
		fullClassName
	});
}

function listMethods(codemap, packageFqn, className, keyword, sourceName = null) {
	const keywords = normalizeKeywords(keyword);
	const methods = new Set();

	for (const packet of codemap.packages || []) {
		if (sourceName && packet.sourceName !== sourceName) continue;
		if (packet.fqn !== packageFqn) continue;
		for (const member of packet.members || []) {
			if (member.className !== className || !isMethodName(member.memberName)) continue;
			if (keywords.length > 0 && !matchesAnyKeyword(member.memberName, keywords)) continue;
			methods.add(member.memberName);
		}
	}

	if (methods.size > 0) {
		return Array.from(methods).sort();
	}

	for (const packageDep of codemap.packageDeps || []) {
		collectMethodsFromMember(packageDep.from, packageDep.memberDeps, 'from', packageFqn, className, keywords, methods, sourceName);
		collectMethodsFromMember(packageDep.to, packageDep.memberDeps, 'to', packageFqn, className, keywords, methods, sourceName);
	}

	return Array.from(methods).sort();
}

function collectMethodsFromMember(packet, memberDeps, side, packageFqn, className, keywords, methods, sourceName) {
	if (sourceName && packet.sourceName !== sourceName) return;
	if (packet.fqn !== packageFqn) return;
	for (const memberDep of memberDeps || []) {
		const member = memberDep[side];
		if (member.className !== className || !isMethodName(member.memberName)) continue;
		if (keywords.length > 0 && !matchesAnyKeyword(member.memberName, keywords)) continue;
		methods.add(member.memberName);
	}
}

function isMethodName(memberName) {
	return memberName != null && memberName.includes('()');
}

function createClassList(classes, codemap, componentName, keyword) {
	const tableWrapper = document.createElement('div');
	tableWrapper.id = 'table-wrapper';
	const reverseGraph = buildReverseCallGraph(codemap);

	const tablePane = document.createElement('div');
	tablePane.id = 'table-pane';
	const classLayout = document.createElement('div');
	classLayout.className = 'class-list-layout';
	const classListPane = document.createElement('div');
	classListPane.className = 'class-list-pane';
	const methodListPane = document.createElement('div');
	methodListPane.className = 'method-list-pane';

	const table = document.createElement('table');
	const thead = document.createElement('thead');
	thead.insertRow().innerHTML = `<th>类清单: ${componentName}</th>`;
	table.appendChild(thead);
	const tbody = document.createElement('tbody');
	let selectedClassButton = null;
	let selectedMethodButton = null;

	for (const entry of classes) {
		const row = tbody.insertRow();
		const cell = row.insertCell();
		const button = document.createElement('button');
		button.type = 'button';
		button.className = 'trace-link class-list-link';
		button.dataset.packageFqn = entry.packageFqn;
		button.dataset.sourceName = entry.sourceName || '';
		button.dataset.className = entry.className;
		button.textContent = entry.className;
		cell.appendChild(button);
	}

	if (classes.length === 0) {
		const row = tbody.insertRow();
		const cell = row.insertCell();
		cell.textContent = '没有匹配到类。';
	}

	table.appendChild(tbody);
	classListPane.appendChild(table);
	classLayout.appendChild(classListPane);
	methodListPane.appendChild(createMethodListContent([], null, null, null));
	classLayout.appendChild(methodListPane);
	tablePane.appendChild(classLayout);
	tableWrapper.appendChild(tablePane);

	const splitHandle = document.createElement('div');
	splitHandle.id = 'table-splitter';
	splitHandle.title = '拖动调整表格和说明区域的高度占比';
	tableWrapper.appendChild(splitHandle);

	const tracePanel = document.createElement('div');
	tracePanel.id = 'call-trace-panel';
	tracePanel.innerHTML = `
		<h3>向上调用链</h3>
		<p class="call-trace-placeholder">当前节点是叶子目录，左侧展示类清单，点击类名后在右侧查看方法清单，再点击方法名查看调用链。</p>
	`;
	tableWrapper.appendChild(tracePanel);
	const resizeHandle = document.createElement('div');
	resizeHandle.className = 'table-resize-handle';
	resizeHandle.title = '拖动调整详情表格大小';
	tableWrapper.appendChild(resizeHandle);
	setupVerticalResize(tableWrapper, tablePane, tracePanel, splitHandle);
	setupPanelResize(tableWrapper, resizeHandle);
	tableWrapper.addEventListener('click', event => {
		event.stopPropagation();
		const classTrigger = event.target.closest('.class-list-link');
		if (classTrigger) {
			if (selectedClassButton) {
				selectedClassButton.classList.remove('selected-class');
			}
			selectedClassButton = classTrigger;
			selectedClassButton.classList.add('selected-class');
			selectedMethodButton = null;
			const methodPanel = createMethodListContent(
				listMethods(codemap, classTrigger.dataset.packageFqn, classTrigger.dataset.className, keyword, classTrigger.dataset.sourceName || null),
				classTrigger.dataset.packageFqn,
				classTrigger.dataset.sourceName || null,
				classTrigger.dataset.className
			);
			methodListPane.innerHTML = '';
			methodListPane.appendChild(methodPanel);
			tracePanel.innerHTML = `
				<h3>向上调用链</h3>
				<p class="call-trace-placeholder">已选中类 ${classTrigger.dataset.className}，点击右侧方法名查看调用链。</p>
			`;
			return;
		}
		const traceTrigger = event.target.closest('.method-list-link, .trace-node-link');
		if (!traceTrigger) return;
		if (traceTrigger.classList.contains('method-list-link')) {
			if (selectedMethodButton) {
				selectedMethodButton.classList.remove('selected-method');
			}
			selectedMethodButton = traceTrigger;
			selectedMethodButton.classList.add('selected-method');
		}
		renderCallTrace(tracePanel, traceToTop(reverseGraph, {
			packageFqn: traceTrigger.dataset.packageFqn,
			sourceName: traceTrigger.dataset.sourceName || null,
			className: traceTrigger.dataset.className,
			memberName: traceTrigger.dataset.memberName || null
		}));
	});

	return tableWrapper;
}

function createMethodListContent(methods, packageFqn, sourceName, className) {
	const container = document.createElement('div');
	container.className = 'method-list-content';
	const table = document.createElement('table');
	const thead = document.createElement('thead');
	thead.insertRow().innerHTML = `<th>方法清单${className ? `: ${className}` : ''}</th>`;
	table.appendChild(thead);
	const tbody = document.createElement('tbody');

	for (const method of methods) {
		const row = tbody.insertRow();
		const cell = row.insertCell();
		const button = document.createElement('button');
		button.type = 'button';
		button.className = 'trace-link method-list-link';
		button.dataset.packageFqn = packageFqn;
		button.dataset.sourceName = sourceName || '';
		button.dataset.className = className;
		button.dataset.memberName = method;
		button.textContent = method;
		cell.appendChild(button);
	}

	if (methods.length === 0) {
		const row = tbody.insertRow();
		row.insertCell().textContent = className ? '没有匹配到方法。' : '点击左侧类名查看方法清单。';
	}

	table.appendChild(tbody);
	container.appendChild(table);
	return container;
}

function setupVerticalResize(wrapper, tablePane, tracePanel, splitHandle) {
	const MIN_PANEL_HEIGHT = 140;
	const DEFAULT_TRACE_RATIO = 0.4;
	const MAX_TRACE_RATIO = 0.5;

	const clampTraceHeight = requestedHeight => {
		const wrapperHeight = wrapper.clientHeight || 0;
		if (!wrapperHeight) return requestedHeight;
		const maxTraceHeight = Math.max(MIN_PANEL_HEIGHT, Math.floor(wrapperHeight * MAX_TRACE_RATIO));
		const minTraceHeight = Math.min(MIN_PANEL_HEIGHT, maxTraceHeight);
		return Math.min(Math.max(requestedHeight, minTraceHeight), maxTraceHeight);
	};

	const applyTraceHeight = requestedHeight => {
		const wrapperHeight = wrapper.clientHeight || 0;
		if (!wrapperHeight) return;
		const splitHeight = splitHandle.offsetHeight || 8;
		const traceHeight = clampTraceHeight(requestedHeight);
		const tableHeight = Math.max(
			MIN_PANEL_HEIGHT,
			wrapperHeight - splitHeight - traceHeight
		);
		tablePane.style.flex = `0 0 ${tableHeight}px`;
		tracePanel.style.flex = `0 0 ${traceHeight}px`;
	};

	requestAnimationFrame(() => {
		const initialHeight = Math.floor((wrapper.clientHeight || 0) * DEFAULT_TRACE_RATIO);
		applyTraceHeight(initialHeight);
	});

	let startY = 0;
	let startTraceHeight = 0;

	const onPointerMove = event => {
		const nextTraceHeight = startTraceHeight - (event.clientY - startY);
		applyTraceHeight(nextTraceHeight);
	};

	const onPointerUp = () => {
		window.removeEventListener('pointermove', onPointerMove);
		window.removeEventListener('pointerup', onPointerUp);
		document.body.classList.remove('is-resizing-table');
	};

	splitHandle.addEventListener('pointerdown', event => {
		event.preventDefault();
		startY = event.clientY;
		startTraceHeight = tracePanel.getBoundingClientRect().height;
		document.body.classList.add('is-resizing-table');
		window.addEventListener('pointermove', onPointerMove);
		window.addEventListener('pointerup', onPointerUp);
	});

	window.addEventListener('resize', () => {
		const currentTraceHeight = tracePanel.getBoundingClientRect().height;
		applyTraceHeight(currentTraceHeight);
	});
}

function setupPanelResize(wrapper, resizeHandle) {
	const MIN_WIDTH = 480;
	const MIN_HEIGHT = 280;
	let startX = 0;
	let startY = 0;
	let startWidth = 0;
	let startHeight = 0;

	const onPointerMove = event => {
		const parentBox = wrapper.parentElement.getBoundingClientRect();
		const maxWidth = Math.max(MIN_WIDTH, parentBox.width);
		const maxHeight = Math.max(MIN_HEIGHT, parentBox.height);
		const nextWidth = Math.min(Math.max(startWidth + event.clientX - startX, MIN_WIDTH), maxWidth);
		const nextHeight = Math.min(Math.max(startHeight + event.clientY - startY, MIN_HEIGHT), maxHeight);
		wrapper.style.width = `${nextWidth}px`;
		wrapper.style.height = `${nextHeight}px`;
	};

	const onPointerUp = () => {
		window.removeEventListener('pointermove', onPointerMove);
		window.removeEventListener('pointerup', onPointerUp);
		setTimeout(() => document.body.classList.remove('is-resizing-panel'), 0);
	};

	resizeHandle.addEventListener('pointerdown', event => {
		event.preventDefault();
		event.stopPropagation();
		const box = wrapper.getBoundingClientRect();
		startX = event.clientX;
		startY = event.clientY;
		startWidth = box.width;
		startHeight = box.height;
		document.body.classList.add('is-resizing-panel');
		window.addEventListener('pointermove', onPointerMove);
		window.addEventListener('pointerup', onPointerUp);
	});
	resizeHandle.addEventListener('click', event => {
		event.preventDefault();
		event.stopPropagation();
	});
}

function appendClassCell(cell, symbol) {
	appendTraceLink(cell, symbol.label, symbol.packageFqn, symbol.sourceName, symbol.className, null);
}

function appendMemberCell(cell, symbol) {
	if (!symbol.memberName) {
		cell.innerHTML = symbol.label;
		return;
	}
	appendTraceLink(cell, symbol.label, symbol.packageFqn, symbol.sourceName, symbol.className, symbol.memberName);
}

function appendTraceLink(cell, label, packageFqn, sourceName, className, memberName) {
	const button = document.createElement('button');
	button.type = 'button';
	button.className = 'trace-link';
	button.dataset.packageFqn = packageFqn;
	button.dataset.sourceName = sourceName || '';
	button.dataset.className = className;
	if (memberName) {
		button.dataset.memberName = memberName;
	}
	button.textContent = label;
	cell.appendChild(button);
}

function appendArrowCell(cell, memberDep) {
	cell.innerHTML = '&rightarrow;';
	if (!memberDep.inLoop) return;
	const badge = document.createElement('span');
	badge.className = 'loop-badge';
	badge.textContent = '循环';
	badge.title = '该调用位于循环体内';
	cell.appendChild(document.createTextNode(' '));
	cell.appendChild(badge);
}

function buildReverseCallGraph(codemap) {
	const incomingByTarget = new Map();
	for (const packageDep of codemap.packageDeps) {
		for (const memberDep of packageDep.memberDeps) {
			if (!memberDep.from.memberName) continue;
			const caller = normalizeRef({
				packageFqn: packageDep.from.fqn,
				sourceName: packageDep.from.sourceName || null,
				className: memberDep.from.className,
				memberName: memberDep.from.memberName
			});
			addIncoming(incomingByTarget, toKey({
				packageFqn: packageDep.to.fqn,
				sourceName: packageDep.to.sourceName || null,
				className: memberDep.to.className,
				memberName: null
			}), caller, memberDep.inLoop === true);
			if (memberDep.to.memberName) {
				addIncoming(incomingByTarget, toKey({
					packageFqn: packageDep.to.fqn,
					sourceName: packageDep.to.sourceName || null,
					className: memberDep.to.className,
					memberName: memberDep.to.memberName
				}), caller, memberDep.inLoop === true);
			}
		}
	}
	return incomingByTarget;
}

function addIncoming(incomingByTarget, key, caller, inLoop) {
	if (!incomingByTarget.has(key)) {
		incomingByTarget.set(key, new Map());
	}
	const callers = incomingByTarget.get(key);
	const callerKey = toKey(caller);
	const existing = callers.get(callerKey);
	callers.set(callerKey, {
		ref: caller,
		inLoop: (existing && existing.inLoop) || inLoop === true
	});
}

function traceToTop(reverseGraph, target) {
	const visitedCycles = [];
	const targetRef = normalizeRef(target);
	const callers = Array.from(reverseGraph.get(toKey(target)) || [])
		.map(([, caller]) => caller)
		.sort((left, right) => formatRef(left.ref).localeCompare(formatRef(right.ref)));
	const roots = callers.map(caller =>
		buildTraceNode(reverseGraph, caller, [targetRef, normalizeRef(caller.ref)], visitedCycles)
	);
	return {
		target: targetRef,
		roots,
		visitedCycles,
		hasLoopCalls: containsLoopCalls(roots)
	};
}

function buildTraceNode(reverseGraph, currentEdge, path, visitedCycles) {
	const current = normalizeRef(currentEdge.ref);
	const callers = Array.from(reverseGraph.get(toKey(current)) || [])
		.map(([, caller]) => caller)
		.sort((left, right) => formatRef(left.ref).localeCompare(formatRef(right.ref)));
	const parents = [];
	for (const caller of callers) {
		const callerRef = normalizeRef(caller.ref);
		if (path.some(step => toKey(step) === toKey(callerRef))) {
			visitedCycles.push([callerRef, ...path.slice().reverse()]);
			continue;
		}
		parents.push(buildTraceNode(reverseGraph, caller, [...path, callerRef], visitedCycles));
	}
	return {
		ref: current,
		inLoop: currentEdge.inLoop === true,
		parents
	};
}

function renderCallTrace(tracePanel, result) {
	tracePanel.innerHTML = '';
	const title = document.createElement('h3');
	title.textContent = '向上调用链';
	tracePanel.appendChild(title);

	const targetLine = document.createElement('p');
	targetLine.className = 'call-trace-target';
	targetLine.textContent = '目标: ' + formatCompactRef(result.target);
	targetLine.title = formatRef(result.target);
	tracePanel.appendChild(targetLine);

	const loopInfo = document.createElement('p');
	loopInfo.className = 'call-trace-note';
	loopInfo.textContent = result.hasLoopCalls
		? '该调用链中包含位于循环体内的调用，节点旁会标记“循环”。'
		: '该调用链中未发现位于循环体内的调用。';
	tracePanel.appendChild(loopInfo);

	if (result.roots.length === 0) {
		const empty = document.createElement('p');
		empty.className = 'call-trace-placeholder';
		empty.textContent = '没有找到继续向上调用它的方法。';
		tracePanel.appendChild(empty);
		return;
	}

	tracePanel.appendChild(renderTracePaths(result.roots));

	if (result.visitedCycles.length > 0) {
		const note = document.createElement('p');
		note.className = 'call-trace-note';
		note.textContent = '检测到调用追溯环，已停止重复追溯。';
		tracePanel.appendChild(note);
	}
}

function normalizeRef(ref) {
	return {
		packageFqn: ref.packageFqn,
		sourceName: ref.sourceName || null,
		className: ref.className,
		memberName: ref.memberName || null
	};
}

function renderTracePaths(roots) {
	const container = document.createElement('div');
	container.className = 'call-trace-paths';
	const paths = [];
	for (const root of roots) {
		collectTracePaths(root, [], paths);
	}
	paths.forEach((path, index) => {
		container.appendChild(renderTracePath(path, index + 1));
	});
	return container;
}

function collectTracePaths(node, currentPath, paths) {
	const nextPath = [...currentPath, node];
	if (node.parents.length === 0) {
		paths.push(nextPath);
		return;
	}
	for (const parent of node.parents) {
		collectTracePaths(parent, nextPath, paths);
	}
}

function renderTracePath(path, index) {
	const section = document.createElement('section');
	section.className = 'call-trace-path';

	const title = document.createElement('h4');
	title.className = 'call-trace-path-title';
	title.textContent = `链路 ${index}`;
	section.appendChild(title);

	const list = document.createElement('ol');
	list.className = 'call-trace-path-list';
	for (const [depth, node] of path.entries()) {
		const item = document.createElement('li');
		item.className = 'call-trace-path-item';
		item.style.paddingLeft = `${depth * 18}px`;

		const arrow = document.createElement('span');
		arrow.className = 'call-trace-path-arrow';
		arrow.textContent = depth === 0 ? '↖' : '←';

		const content = document.createElement('div');
		content.className = 'call-trace-node-content';
		content.appendChild(arrow);
		content.appendChild(createTraceButton(node.ref));
		if (node.inLoop) {
			const badge = document.createElement('span');
			badge.className = 'loop-badge';
			badge.textContent = '循环';
			badge.title = '该调用位于循环体内';
			content.appendChild(badge);
		}
		item.appendChild(content);
		list.appendChild(item);
	}
	section.appendChild(list);
	return section;
}

function createTraceButton(ref) {
	const button = document.createElement('button');
	button.type = 'button';
	button.className = 'trace-link trace-node-link';
	button.dataset.packageFqn = ref.packageFqn;
	button.dataset.sourceName = ref.sourceName || '';
	button.dataset.className = ref.className;
	if (ref.memberName) {
		button.dataset.memberName = ref.memberName;
	}
	button.title = formatRef(ref);

	const primary = document.createElement('span');
	primary.className = 'trace-node-primary';
	primary.textContent = formatCompactRef(ref);
	button.appendChild(primary);

	const secondary = document.createElement('span');
	secondary.className = 'trace-node-secondary';
	secondary.textContent = shortenPackage(ref.packageFqn);
	button.appendChild(secondary);

	return button;
}

function toKey(ref) {
	return [ref.sourceName || '', ref.packageFqn, ref.className, ref.memberName || ''].join('#');
}

function formatRef(ref) {
	return [ref.sourceName, ref.packageFqn, ref.className, ref.memberName].filter(x => x).join('.');
}

function formatCompactRef(ref) {
	return ref.memberName ? (ref.className + '.' + ref.memberName) : ref.className;
}

function containsLoopCalls(nodes) {
	for (const node of nodes) {
		if (node.inLoop || containsLoopCalls(node.parents)) {
			return true;
		}
	}
	return false;
}

function shortenPackage(packageFqn) {
	const segments = packageFqn.split('.').filter(Boolean);
	if (segments.length <= 2) return packageFqn;
	return '...' + segments.slice(-2).join('.');
}

function componentFilterName(component) {
	return component.isRootMarker
		? `${component.packageName}.[root]`
		: component.packageName;
}

function componentLabel(component, fallbackName) {
	return component.label || component.packageName || fallbackName;
}

function renderExcelWorkbook(sheets) {
	if (!sheets || sheets.length === 0) {
		sheets = [{name: 'Dependencies', rows: []}];
	}
	return `\uFEFF<?xml version="1.0" encoding="UTF-8"?>
<?mso-application progid="Excel.Sheet"?>
<Workbook xmlns="urn:schemas-microsoft-com:office:spreadsheet"
	xmlns:o="urn:schemas-microsoft-com:office:office"
	xmlns:x="urn:schemas-microsoft-com:office:excel"
	xmlns:ss="urn:schemas-microsoft-com:office:spreadsheet"
	xmlns:html="http://www.w3.org/TR/REC-html40">
	<Styles>
		<Style ss:ID="header">
			<Font ss:Bold="1"/>
			<Interior ss:Color="#D0E0EC" ss:Pattern="Solid"/>
		</Style>
	</Styles>
${sheets.map(sheet => renderExcelWorksheet(sheet.name, sheet.rows)).join('\n')}
</Workbook>`;
}

function renderExcelWorksheet(sheetName, rows) {
	const headers = ['From Component', 'From Class', 'From Member', 'Dependency', 'To Member', 'To Class', 'To Component'];
	const bodyRows = rows.length > 0
		? rows.map(row => [
			row.fromComponent,
			row.fromClass,
			row.fromMember,
			row.dependency,
			row.toMember,
			row.toClass,
			row.toComponent
		])
		: [['No dependencies matched current view.', '', '', '', '', '', '']];

	return `
	<Worksheet ss:Name="${escapeXmlAttribute(sheetName)}">
		<Table>
			<Row>
				${headers.map(header => `<Cell ss:StyleID="header"><Data ss:Type="String">${escapeXml(header)}</Data></Cell>`).join('')}
			</Row>
			${bodyRows.map(row => `
				<Row>
					${row.map(value => `<Cell><Data ss:Type="String">${escapeXml(value)}</Data></Cell>`).join('')}
				</Row>`).join('')}
		</Table>
	</Worksheet>`;
}

function buildSheetName(sheetName, usedSheetNames) {
	const baseName = sanitizeSheetName(sheetName);
	let candidate = trimSheetName(baseName);
	let suffixIndex = 2;
	while (usedSheetNames.has(candidate)) {
		const suffix = ` ${suffixIndex}`;
		const trimmedBase = baseName.slice(0, Math.max(1, 31 - suffix.length)).trim();
		candidate = `${trimmedBase}${suffix}`;
		suffixIndex++;
	}
	usedSheetNames.add(candidate);
	return candidate;
}

function plainText(value) {
	const holder = document.createElement('div');
	holder.innerHTML = value == null ? '' : String(value);
	return holder.textContent || '';
}

function escapeXml(value) {
	return String(value == null ? '' : value)
		.replace(/&/g, '&amp;')
		.replace(/</g, '&lt;')
		.replace(/>/g, '&gt;');
}

function escapeXmlAttribute(value) {
	return escapeXml(value)
		.replace(/"/g, '&quot;')
		.replace(/'/g, '&apos;');
}

function sanitizeSheetName(sheetName) {
	const cleaned = String(sheetName == null ? '' : sheetName)
		.replace(/\[/g, '_')
		.replace(/\]/g, '_')
		.replace(/:/g, '_')
		.replace(/\*/g, '_')
		.replace(/\?/g, '_')
		.replace(/\//g, '_')
		.replace(/\\/g, '_')
		.trim();
	return cleaned || 'Sheet1';
}

function trimSheetName(sheetName) {
	return sheetName.length > 31 ? sheetName.slice(0, 31) : sheetName;
}

function fromPackageAndClassName(packageDep, memberDep) {
	return [packageDep.from.relativeName, memberDep.from.className].filter(x => x).join('.');
}

function toPackageAndClassName(packageDep, memberDep) {
	return [packageDep.to.relativeName, memberDep.to.className].filter(x => x).join('.');
}

function fromMemberName(memberDep) {
	return memberDep.from.memberName !== null
		? memberDep.from.memberName
		: '';
}

function toMemberName(memberDep) {
	return memberDep.to.memberName !== null
		? memberDep.to.memberName
		: '<i>' + memberDep.type.toLowerCase().replaceAll('_', ' ') + '</i>';
}
