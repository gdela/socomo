/*
 * Drawing and controls of the component dependency table (what package deps
 * and their member deps constitute a given dependency between two components).
 */

import './table.scss';

export default drawTable;

function drawTable(tableContainer, codemap, fromComponentName, toComponentName, keyword) {
	const filteredDeps = filterAndRelativize(codemap.packageDeps, fromComponentName, toComponentName);
	const table = createTable(filteredDeps, codemap, fromComponentName, toComponentName, keyword);
	tableContainer.innerHTML = '';
	tableContainer.appendChild(table);
	return table;
}

function filterAndRelativize(packageDeps, fromComponentName, toComponentName) {
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
			belongsTo(fromComponentName, packageDep.from)
			&& belongsTo(toComponentName, packageDep.to)
		)
		.map(packageDep => {
			return {
				...packageDep,
				from: {
					fqn: packageDep.from.fqn,
					relativeName: packageDep.from.fqn.replace(trimRoot(fromComponentName), '')
				},
				to: {
					fqn: packageDep.to.fqn,
					relativeName: packageDep.to.fqn.replace(trimRoot(toComponentName), '')
				}
			};
		});
}

function createTable(packageDeps, codemap, fromComponentName, toComponentName, keyword) {
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
			if (!keyword ||
				(memberDep.from.className && memberDep.from.className.includes(keyword)) ||
				(memberDep.from.memberName && memberDep.from.memberName.includes(keyword)) ||
				(memberDep.to.className && memberDep.to.className.includes(keyword)) ||
				(memberDep.to.memberName && memberDep.to.memberName.includes(keyword))
			) {
				const row = tbody.insertRow();
				appendClassCell(row.insertCell(), {
					packageFqn: packageDep.from.fqn,
					className: memberDep.from.className,
					label: fromPackageAndClassName(packageDep, memberDep)
				});
				appendMemberCell(row.insertCell(), {
					packageFqn: packageDep.from.fqn,
					className: memberDep.from.className,
					memberName: memberDep.from.memberName,
					label: fromMemberName(memberDep)
				});
				row.insertCell().innerHTML = '&rightarrow;';
				appendMemberCell(row.insertCell(), {
					packageFqn: packageDep.to.fqn,
					className: memberDep.to.className,
					memberName: memberDep.to.memberName,
					label: toMemberName(memberDep)
				});
				appendClassCell(row.insertCell(), {
					packageFqn: packageDep.to.fqn,
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
	tableWrapper.appendChild(table);
	const tracePanel = document.createElement('div');
	tracePanel.id = 'call-trace-panel';
	tracePanel.innerHTML = '<div class="call-trace-placeholder">点击表格中的类名或方法名，向上追溯调用链。</div>';
	tableWrapper.appendChild(tracePanel);
	tableWrapper.addEventListener('click', event => {
		event.stopPropagation();
		const trigger = event.target.closest('.trace-link');
		if (!trigger) return;
		const target = {
			packageFqn: trigger.dataset.packageFqn,
			className: trigger.dataset.className,
			memberName: trigger.dataset.memberName || null
		};
		renderCallTrace(tracePanel, traceToTop(reverseGraph, target));
	});
	return tableWrapper;
}

function appendClassCell(cell, symbol) {
	appendTraceLink(cell, symbol.label, symbol.packageFqn, symbol.className, null);
}

function appendMemberCell(cell, symbol) {
	if (!symbol.memberName) {
		cell.innerHTML = symbol.label;
		return;
	}
	appendTraceLink(cell, symbol.label, symbol.packageFqn, symbol.className, symbol.memberName);
}

function appendTraceLink(cell, label, packageFqn, className, memberName) {
	const button = document.createElement('button');
	button.type = 'button';
	button.className = 'trace-link';
	button.dataset.packageFqn = packageFqn;
	button.dataset.className = className;
	if (memberName) {
		button.dataset.memberName = memberName;
	}
	button.textContent = label;
	cell.appendChild(button);
}

function buildReverseCallGraph(codemap) {
	const incomingByTarget = new Map();
	for (const packageDep of codemap.packageDeps) {
		for (const memberDep of packageDep.memberDeps) {
			if (!memberDep.from.memberName) continue;
			const caller = {
				packageFqn: packageDep.from.fqn,
				className: memberDep.from.className,
				memberName: memberDep.from.memberName
			};
			addIncoming(incomingByTarget, toKey({
				packageFqn: packageDep.to.fqn,
				className: memberDep.to.className,
				memberName: null
			}), caller);
			if (memberDep.to.memberName) {
				addIncoming(incomingByTarget, toKey({
					packageFqn: packageDep.to.fqn,
					className: memberDep.to.className,
					memberName: memberDep.to.memberName
				}), caller);
			}
		}
	}
	return incomingByTarget;
}

function addIncoming(incomingByTarget, key, caller) {
	if (!incomingByTarget.has(key)) {
		incomingByTarget.set(key, new Map());
	}
	incomingByTarget.get(key).set(toKey(caller), caller);
}

function traceToTop(reverseGraph, target) {
	const visitedCycles = [];
	const targetRef = normalizeRef(target);
	const chains = [];
	traceRecursively(reverseGraph, targetRef, [targetRef], chains, visitedCycles);
	chains.sort((left, right) => formatChain(left).localeCompare(formatChain(right)));
	return {target: targetRef, chains, visitedCycles};
}

function traceRecursively(reverseGraph, target, currentPath, chains, visitedCycles) {
	const callers = Array.from(reverseGraph.get(toKey(target)) || [])
		.map(([, caller]) => caller)
		.sort((left, right) => formatRef(left).localeCompare(formatRef(right)));
	if (callers.length === 0) {
		chains.push(currentPath.slice());
		return;
	}
	let hasExpanded = false;
	for (const caller of callers) {
		const callerRef = normalizeRef(caller);
		if (currentPath.some(step => toKey(step) === toKey(callerRef))) {
			visitedCycles.push([callerRef, ...currentPath]);
			continue;
		}
		hasExpanded = true;
		traceRecursively(reverseGraph, callerRef, [callerRef, ...currentPath], chains, visitedCycles);
	}
	if (!hasExpanded) {
		chains.push(currentPath.slice());
	}
}

function renderCallTrace(tracePanel, result) {
	tracePanel.innerHTML = '';
	const title = document.createElement('h3');
	title.textContent = '向上调用链';
	tracePanel.appendChild(title);

	const targetLine = document.createElement('p');
	targetLine.className = 'call-trace-target';
	targetLine.textContent = '目标: ' + formatRef(result.target);
	tracePanel.appendChild(targetLine);

	if (result.chains.length === 1 && result.chains[0].length === 1) {
		const empty = document.createElement('p');
		empty.className = 'call-trace-placeholder';
		empty.textContent = '没有找到继续向上调用它的方法。';
		tracePanel.appendChild(empty);
		return;
	}

	const list = document.createElement('div');
	list.className = 'call-trace-list';
	for (const chain of result.chains) {
		const item = document.createElement('div');
		item.className = 'call-trace-item';
		for (const [index, step] of chain.entries()) {
			if (index > 0) {
				const arrow = document.createElement('span');
				arrow.className = 'call-trace-arrow';
				arrow.textContent = ' -> ';
				item.appendChild(arrow);
			}
			const button = document.createElement('button');
			button.type = 'button';
			button.className = 'trace-link';
			button.dataset.packageFqn = step.packageFqn;
			button.dataset.className = step.className;
			if (step.memberName) {
				button.dataset.memberName = step.memberName;
			}
			button.textContent = formatRef(step);
			item.appendChild(button);
		}
		list.appendChild(item);
	}
	tracePanel.appendChild(list);

	if (result.visitedCycles.length > 0) {
		const note = document.createElement('p');
		note.className = 'call-trace-note';
		note.textContent = '检测到循环调用，已停止重复追溯。';
		tracePanel.appendChild(note);
	}
}

function normalizeRef(ref) {
	return {
		packageFqn: ref.packageFqn,
		className: ref.className,
		memberName: ref.memberName || null
	};
}

function toKey(ref) {
	return [ref.packageFqn, ref.className, ref.memberName || ''].join('#');
}

function formatRef(ref) {
	return [ref.packageFqn, ref.className, ref.memberName].filter(x => x).join('.');
}

function formatChain(chain) {
	return chain.map(formatRef).join(' -> ');
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
