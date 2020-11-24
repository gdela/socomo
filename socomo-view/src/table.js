/*
 * Drawing and controls of the component dependency table (what package deps
 * and their member deps constitute a given dependency between two components).
 */

import './table.scss';

export default drawTable;

function drawTable(tableContainer, fromComponentName, toComponentName) {
	const filteredDeps = filterAndRelativize(window.codemap.packageDeps, fromComponentName, toComponentName);
	const table = createTable(filteredDeps, fromComponentName, toComponentName);
	tableContainer.innerHTML = '';
	tableContainer.appendChild(table);
	return table;
}

function filterAndRelativize(packageDeps, fromComponentName, toComponentName) {
	const belongsTo = (componentName, packet) => {
		if (componentName.endsWith('[root]')) {
			const rootPacketName = componentName.replace('.[root]', '');
			return packet.fqn === rootPacketName;
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
					relativeName: packageDep.from.fqn.replace(fromComponentName.replace('.[root]', ''), '')
				},
				to: {
					relativeName: packageDep.to.fqn.replace(toComponentName.replace('.[root]', ''), '')
				}
			};
		});
}

function createTable(packageDeps, fromComponentName, toComponentName) {
	const thead = document.createElement('thead');
	thead.insertRow().innerHTML = `
		<th colspan="2">${fromComponentName}</th>
		<th>&rightarrow;</th>
		<th colspan="2">${toComponentName}</th>
	`;
	const tbody = document.createElement('tbody');
	for (const packageDep of packageDeps) {
		for (const memberDep of packageDep.memberDeps) {
			tbody.insertRow().innerHTML = `
				<td>${packageDep.from.relativeName}.${memberDep.from.className}</td>
				<td>${nn(memberDep.from.memberName)}</td>
				<td>&rightarrow;</td>
				<td>${nn(memberDep.to.memberName)}</td>
				<td>${packageDep.to.relativeName}.${memberDep.to.className}</td>
			`;
		}
	}
	const table = document.createElement('table');
	table.appendChild(thead);
	table.appendChild(tbody);
	const tableWrapper = document.createElement('div');
	tableWrapper.id = 'table-wrapper';
	tableWrapper.appendChild(table);
	return tableWrapper;
}

function nn(value) {
	return value !== null ? value : '';
}


