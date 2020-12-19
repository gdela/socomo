/*
 * Style and layout of the composition diagrams.
 */

import cytoscape from 'cytoscape';

export { diagramLayout, diagramZooming, diagramStyle };

// see http://js.cytoscape.org/#layouts
const diagramLayout = {
	name: 'klay', // todo: choose between dagre and klay, check also BRANDES_KOEPF node placement
	fit: true,
	padding: 10,

	// dagre - https://github.com/cytoscape/cytoscape.js-dagre
	rankDir: 'TB',
	nodeSep: 40,
	rankSep: 60,

	// klay - https://github.com/cytoscape/cytoscape.js-klay
	klay: {
		direction: 'DOWN',
		thoroughness: 50,
		nodePlacement: 'LINEAR_SEGMENTS',
		cycleBreaking: 'GREEDY',
		spacing: 8,
		inLayerSpacingFactor: 2.0,
		edgeSpacingFactor: 0.2,
		borderSpacing: 10,
	},
	priority: edge => Math.round(edge.data('strength'))
};

// match zoom limits and element to the size of the diagram
const diagramZooming = event => {
	const cy = event.cy;
	const fitAll = () => cy.fit(cy.nodes(), diagramLayout.padding);
	cy.minZoom(0.1); fitAll(); // to measure zoom needed to fit everything
	const bigDiagram = cy.zoom() < 0.9;
	cy.maxZoom(bigDiagram ? 1.1 : 1.5);
	cy.minZoom(Math.min(1.0, cy.zoom() - 0.1));
	cy.nodes().toggleClass('on-big-diagram', bigDiagram);
	cy.edges().toggleClass('on-big-diagram', bigDiagram);
	fitAll(); // to bring back zoom to allowed min-max range
};

// see http://js.cytoscape.org/#style
const diagramStyle = cytoscape.stylesheet()

	//--- base styles ---//
	.selector('node')
	.css({
		'content': 'data(id)',
		'shape': 'rectangle',
		'height': '25px',
		'width': '90px',
		'padding': 0,
		'color': '#000',
		'background-color': '#ede9ed',
		'border-width': '1px',
		'border-color': '#ede9ed',
		'border-style': 'solid',
		'text-valign': 'center',
		'text-halign': 'center',
		'font-family': 'Lato, Verdana, Geneva, sans-serif',
		'font-size': '10px',
	})
	.selector('node.on-big-diagram')
	.css({
		'height': '30px',
		'width': '100px',
		'font-size': '14px'
	})
	.selector('edge')
	.css({
		'width': 'mapData(strength, 1, 9, 0.33, 1.45)',
		'line-color': '#dce6d7',
		'line-style': 'solid',
		'target-arrow-shape': 'triangle-backcurve',
		'target-arrow-color': '#dce6d7',
		'target-arrow-fill': 'filled',
		'target-distance-from-node': '0px',
		'curve-style': 'bezier',
		'arrow-scale': 0.6
	})
	.selector('edge.on-big-diagram')
	.css({
		'arrow-scale': 0.8
	})
	.selector('edge.violation')
	.css({
		'line-color': '#ff4444',
		'target-arrow-color': '#ff4444'
	})

	//--- highlighting on node hover ---//
	.selector('node.highlight')
	.css({
		'background-color': '#aac0f6',
		'border-color': '#aac0f6',
	})
	.selector('node.hushed')
	.css({
		'opacity': '0.25',
		'border-opacity': '0.25'
	})
	.selector('edge.highlight-ingoer')
	.css({
		'line-color': '#afb8a2',
		'target-arrow-color': '#b1baa4',
		'z-index': 1
	})
	.selector('edge.highlight-outgoer')
	.css({
		'line-color': '#143ea6',
		'target-arrow-color': '#1441bb',
		'z-index': 1
	})
	.selector('edge.hushed')
	.css({
		'line-color': '#cfcfcf',
		'target-arrow-color': '#cfcfcf',
		'opacity': '0.15',
		'z-index': 0
	})
	.selector('edge.violation.highlight-ingoer, edge.violation.highlight-outgoer')
	.css({
		'line-color': '#FC0000',
		'target-arrow-color': '#FC0000'
	})

	//--- highlighting on edge hover ---//
	.selector('node.highlight-dependency')
	.css({
		'background-blacken': 0.15,
		'border-color': '#927A92',
		'border-style': 'dotted',
	})

	.selector('edge.highlight-dependency')
	.css({
		'line-color': '#668855',
		'target-arrow-color': '#668855',
		'line-style': 'dashed'
	})
	.selector('edge.violation.highlight-dependency')
	.css({
		'line-color': '#FC0000',
		'target-arrow-color': '#FC0000'
	})

	//--- emphasis on nodes selection ---//
	.selector('node:selected')
	.css({
		'background-color': '#C8ACF6',
		'border-color': '#C8ACF6',
	})
	.selector('edge.between-selected')
	.css({
		'width': 'mapData(strength, 1, 9, 0.99, 4.35)',
		'line-color': '#1D2718',
		'target-arrow-color': '#1D2718',
		'arrow-scale': 0.8,
		'z-index': 1
	})
	.selector('edge.violation.between-selected')
	.css({
		'line-color': '#FC0000',
		'target-arrow-color': '#FC0000'
	})
;
