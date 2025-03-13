/*
 * Drawing and controls of the composition diagram (components and dependencies between them).
 */

import {diagramLayout, diagramZooming, diagramStyle} from './diagram-skin';
import cytoscape from 'cytoscape';
import Mousetrap from 'mousetrap';
import saveAs from 'file-saver';

export default drawDiagram;

function drawDiagram(diagramContainer, level, dependencySelectedHandler) {

	const nodes = Object.entries(level.components).map(([componentName, props]) => {
		return {data: {id: componentName, size: props.size}}; // cytoscape node format
	});

	const edges = Object.entries(level.dependencies).map(([dependencyName, props]) => {
		const [from, to] = dependencyName.split(' -> ');
		return {data: {source: from, target: to, strength: props.strength}}; // cytoscape edge format
	});

	// draw the diagram
	const cy = cytoscape({
		container: diagramContainer,
		style: diagramStyle,
		layout: diagramLayout,
		boxSelectionEnabled: true,
		selectionType: 'additive',
		wheelSensitivity: 0.1,
		elements: {nodes, edges}
	});
	cy.ready(diagramZooming);
	cy.on('resize', diagramZooming);

	// mark upwards dependencies as violations
	const upwardDependency = edge => edge.target().position('y') < edge.source().position('y');
	cy.edges().filter(upwardDependency).addClass('violation');
	cy.on('drag', 'node', event => {
		const node = event.target;
		node.connectedEdges('.violation').removeClass('violation');
		node.connectedEdges().filter(upwardDependency).addClass('violation');
	});

	// highlighting on node hover (highlight single component and all its direct neighbour components)
	cy.on('mouseover', 'node', event => {
		const node = event.target;
		cy.elements().subtract(node.outgoers()).subtract(node.incomers()).subtract(node).addClass('hushed');
		node.addClass('highlight');
		node.outgoers().addClass('highlight-outgoer');
		node.incomers().addClass('highlight-ingoer');
	});
	cy.on('mouseout', 'node', event => {
		const node = event.target;
		cy.elements().removeClass('hushed');
		node.removeClass('highlight');
		node.outgoers().removeClass('highlight-outgoer');
		node.incomers().removeClass('highlight-ingoer');
	});

	// highlighting on edge hover (highlight single dependency with its source component and target component)
	let highlightDelay;
	cy.on('mouseover', 'edge', event => {
		const edge = event.target;
		edge.addClass('highlight-dependency');
		highlightDelay = setTimeout(() => {
			edge.source().addClass('highlight-dependency');
			edge.target().addClass('highlight-dependency');
		}, 150);
	});
	cy.on('mouseout', 'edge', event => {
		const edge = event.target;
		edge.removeClass('highlight-dependency');
		clearTimeout(highlightDelay);
		edge.source().removeClass('highlight-dependency');
		edge.target().removeClass('highlight-dependency');
	});

	// emphasis on nodes selection (emphasise selected components and dependencies between them)
	const markSelectedEdges = () => {
		const selected = cy.nodes(':selected');
		cy.edges().removeClass('between-selected');
		selected.edgesWith(selected).addClass('between-selected');
	};
	cy.on('select unselect', 'node', markSelectedEdges);

	// removing and restoring nodes (hide or undo hide of a component or selected components)
	const removed = [];
	cy.on('cxttap', 'node', event => {
		const node = event.target;
		const nodesToRemove = node.selected() ? cy.nodes(':selected') : node;
		node.emit('mouseout'); // clear highlighting
		nodesToRemove.unselect(); // clear selection state
		removed.push(nodesToRemove.remove());
	});
	cy.on('cxttap', event => {
		if (happenedOnBackground(event) && removed.length > 0) {
			const nodesToRestore = removed.pop();
			nodesToRestore.restore();
		}
	});
	Mousetrap.bind('u', () => {
		while (removed.length > 0) {
			const nodesToRestore = removed.pop();
			nodesToRestore.restore();
		}
	});

	// invoke dependency selected handler on edge click
	cy.on('tap', 'edge', event => {
		const edge = event.target;
		const prefix = level.level !== '' ? (level.level + '.') : '';
		dependencySelectedHandler(
			prefix + edge.source().id(),
			prefix + edge.target().id()
		);
	});

	// rearranging nodes according to standard diagram layout
	Mousetrap.bind('r', () => {
		doLongTask(() => {
			const refreshViolations = () => cy.nodes().emit('drag');
			const refreshIntervalId = setInterval(refreshViolations, 100);
			const layout = cy.layout({
				...diagramLayout,
				animate: true,
				animationDuration: 1800,
				animationEasing: 'ease-in-out-cubic'
			});
			layout.on('layoutstop', () => {
				clearInterval(refreshIntervalId);
				refreshViolations();
			});
			layout.run();
		});
	});

	// saving diagram to png file
	Mousetrap.bind('s', () => {
		doLongTask(() => {
			saveAs(cy.png({ full: true, scale: 3.0, bg: 'white' }), 'diagram.png');
		});
	});

	/**
	 * Shows progress indicator during execution of a long task.
	 */
	function doLongTask(task) {
		diagramContainer.style.cursor = 'wait';
		// the delay is to give browser a chance to render dom changes done so far
		setTimeout(() => { task(); diagramContainer.style.cursor = ''; }, 30);
	}
}


/**
 * Returns true if the event happened on diagram background, not on elements (not on a node or an edge).
 */
function happenedOnBackground(event) {
	return event.target === event.cy;
}
