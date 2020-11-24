/*
 * Style and layout of the composition diagrams.
 */

import cytoscape from 'cytoscape';

// see http://js.cytoscape.org/#layouts
const diagramLayout = {
	name: 'klay', // todo: choose between dagre and klay, check also BRANDES_KOEPF node placement
	fit: true,

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

// see http://js.cytoscape.org/#style
const diagramStyle = cytoscape.stylesheet()

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

	.selector('node.highlight')
	.css({
		'background-color': '#aac0f6',
	})

	.selector('node.hushed')
	.css({
		'opacity': '0.25'
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

	.selector('edge.violation')
	.css({
		'line-color': '#ff4444',
		'target-arrow-color': '#ff4444'
	})

	.selector('edge.hushed')
	.css({
		'line-color': '#cfcfcf',
		'target-arrow-color': '#cfcfcf',
		'opacity': '0.15',
		'z-index': 0
	})

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
		'line-style': 'dotted'
	})

	.selector('edge.violation.highlight-dependency')
	.css({
		'line-color': '#B80000',
		'target-arrow-color': '#B80000'
	});

export { diagramLayout, diagramStyle };
