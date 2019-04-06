/*
 * Drawing and controls of the composition diagram.
 */

import {diagramLayout, diagramStyle} from './diagram-skin';
import cytoscape from 'cytoscape';

function drawDiagram(diagramContainer, level) {

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
		boxSelectionEnabled: false,
		autounselectify: true,
		wheelSensitivity: 0.3,
		maxZoom: 1.5,
		elements: {nodes, edges}
	});

	// mark upwards dependencies as violations
	const upwardDependency = edge => edge.target().position('y') < edge.source().position('y');
	cy.edges().filter(upwardDependency).addClass('violation');
	cy.on('drag', 'node', event => {
		const node = event.target;
		node.connectedEdges('.violation').removeClass('violation');
		node.connectedEdges().filter(upwardDependency).addClass('violation');
	});

	// highlighting on hover
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
}

export default drawDiagram;
