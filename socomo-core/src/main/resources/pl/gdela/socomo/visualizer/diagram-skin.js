/*
 * Style and Layout of the composition diagrams.
 */

/* global cytoscape */
/* exported diagramLayout diagramStyle */

const diagramLayout = {
	name: 'klay', // todo: choose between dagre and klay, check also BRANDES_KOEPF node placement

	// dagre
	rankDir: 'TB',
	nodeSep: 40,
	rankSep: 60,

	// klay
	fit: true,
	klay: {
		direction: 'DOWN',
		thoroughness: 7,
		nodePlacement: 'LINEAR_SEGMENTS',
		spacing: 8,
		inLayerSpacingFactor: 2.0,
		edgeSpacingFactor: 0.2,
		borderSpacing: 10
	}
};

const diagramStyle = cytoscape.stylesheet()

	.selector('node')
	.css({
		'content': 'data(id)',
		'shape': 'rectangle',
		'height': '25px',
		'width': '90px',
		'padding': 0,
		// 'border-color': '#e8e4e8',
		// 'border-width': 1,
		'color': '#000',
		'background-color': '#ede9ed',
		'text-valign': 'center',
		'text-halign': 'center',
		'font-family': 'Lato, Verdana, Geneva, sans-serif',
		'font-size': '10px',
		// 'font-weight': 'bold'
	})

	.selector('edge')
	.css({
		'width': 'mapData(strength, 0, 1, 0.33, 1.00)',
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
		'line-color': '#1a991a',
		'target-arrow-color': '#1a831a',
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
		'opacity': '0.2',
		'z-index': 0
	});
