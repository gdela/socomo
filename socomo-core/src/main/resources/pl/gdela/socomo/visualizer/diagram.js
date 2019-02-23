/*
 * Drawing and Controls of the composition diagram.
 */

/* global _ cytoscape diagramStyle diagramLayout */
/* exported drawDiagram */

function drawDiagram(diagramContainer, level) {

	const nodes = _.map(level.components, (props, componentName) => {
		return {data: {id: componentName}}; // cytoscape node format
	});

	const edges = _.map(level.dependencies, (props, dependencyName) => {
		const [from, to] = dependencyName.split(' -> ');
		return {data: {source: from, target: to, strength: props.strength}}; // cytoscape edge format
	});

	/* todo: in future we will support multiple diagrams per level
	var diagram = level.diagrams[0];
	var diagramElements = {
		nodes: _.filter(nodes, function (node) {
			return !diagram.packages || _.includes(diagram.packages, node.data.id)
		}),
		edges: _.filter(edges, function (edge) {
			return !diagram.packages || _.includes(diagram.packages, edge.data.source) && _.includes(diagram.packages, edge.data.target);
		})
	}; */
	const diagramElements = {nodes, edges};

	// draw the diagram
	const cy = cytoscape({
		container: diagramContainer,
		style: diagramStyle,
		layout: diagramLayout,
		boxSelectionEnabled: false,
		autounselectify: true,
		wheelSensitivity: 0.3,
		maxZoom: 1.5,
		elements: diagramElements
	});

	// workaround for google font not used initially by the browser for node labels, only when the graph is redrawn
	// todo: replace this diagram refresh with "font face observer", https://filamentgroup.com/lab/font-events.html
	diagramContainer.style.visibility = 'hidden';
	setTimeout(function () {
		cy.elements().addClass('dummy');
		cy.elements().removeClass('dummy');
		diagramContainer.style.visibility = 'visible';
	}, 10);

	// mark upwards dependencies as violations
	cy.edges().filter( function (edge) {
		return edge.target().position('y') < edge.source().position('y');
	}).addClass('violation');

	// highlighting on hover
	cy.on('mouseover', 'node', function(e){
		const node = e.target;
		cy.elements().subtract(node.outgoers()).subtract(node.incomers()).subtract(node).addClass('hushed');
		node.addClass('highlight');
		node.outgoers().addClass('highlight-outgoer');
		node.incomers().addClass('highlight-ingoer');
	});
	cy.on('mouseout', 'node', function(e){
		const node = e.target;
		cy.elements().removeClass('hushed');
		node.removeClass('highlight');
		node.outgoers().removeClass('highlight-outgoer');
		node.incomers().removeClass('highlight-ingoer');
	});

}
