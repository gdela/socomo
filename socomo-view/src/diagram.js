/*
 * Drawing and controls of the composition diagram (components and dependencies between them).
 */

import {diagramLayout, diagramZooming, diagramStyle} from './diagram-skin';
import cytoscape from 'cytoscape';
import Mousetrap from 'mousetrap';
import saveAs from 'file-saver';

export default drawDiagram;

const TOP_LEVEL_GAP = 36;

function drawDiagram(diagramContainer, level, dependencySelectedHandler, componentSelectedHandler) {
	let currentLevel = level;

	let cy;

	// draw the diagram
	cy = cytoscape({
		container: diagramContainer,
		style: diagramStyle,
		layout: diagramLayout,
		boxSelectionEnabled: true,
		selectionType: 'additive',
		wheelSensitivity: 0.1,
		elements: buildElements(currentLevel)
	});
	cy.ready(event => {
		resolveTopLevelSpacing(event.cy);
		diagramZooming(event);
	});
	cy.on('resize', diagramZooming);

	// highlighting on node hover (highlight single component and all its direct neighbour components)
	cy.on('mouseover', 'node', event => {
		const node = event.target;
		if (node.isParent()) return;
		node.addClass('highlight');
		node.outgoers().addClass('highlight-outgoer');
		node.incomers().addClass('highlight-ingoer');
	});
	cy.on('mouseout', 'node', event => {
		const node = event.target;
		if (node.isParent()) return;
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
		const prefix = currentLevel.level !== '' ? (currentLevel.level + '.') : '';
		dependencySelectedHandler(
			prefix + edge.source().id(),
			prefix + edge.target().id()
		);
	});

	cy.on('tap', 'node', event => {
		if (!componentSelectedHandler) return;
		if (event.target.isParent() && !clickedGroupHeader(event)) return;
		componentSelectedHandler(
			event.target.data('packageName'),
			event.target.data('isRootMarker') === true
		);
	});

	// rearranging nodes according to standard diagram layout
	Mousetrap.bind('r', () => {
		doLongTask(() => {
			const layout = cy.layout({
				...diagramLayout,
				animate: true,
				animationDuration: 1800,
				animationEasing: 'ease-in-out-cubic'
			});
			layout.on('layoutstop', () => {
				resolveTopLevelSpacing(cy);
				diagramZooming({cy});
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

	function updateLevel(nextLevel) {
		currentLevel = nextLevel;
		const previousPositions = new Map();
		cy.nodes().forEach(node => {
			previousPositions.set(node.id(), {...node.position()});
		});
		const previousPan = cy.pan();
		const previousZoom = cy.zoom();

		const targetElements = buildElements(nextLevel);
		const targetById = new Map(targetElements.nodes.map(node => [node.data.id, node]));

		cy.batch(() => {
			cy.edges().remove();

			cy.nodes().forEach(node => {
				if (!targetById.has(node.id())) {
					node.remove();
				}
			});

			for (const element of targetElements.nodes) {
				const existing = cy.getElementById(element.data.id);
				if (existing.nonempty()) {
					existing.data('label', element.data.label);
					existing.data('packageName', element.data.packageName);
					existing.data('isGroup', element.data.isGroup === true);
					existing.data('isRootMarker', element.data.isRootMarker === true);
					moveNode(existing, element.data.parent);
					continue;
				}

				const added = cy.add(element);
				const initialPosition = initialNodePosition(element.data, cy, previousPositions);
				if (initialPosition) {
					added.position(initialPosition);
				}
			}

			for (const element of targetElements.edges) {
				cy.add(element);
			}
		});

		const layout = cy.layout({
			...diagramLayout,
			animate: true,
			animationDuration: 420,
			animationEasing: 'ease-in-out-cubic',
			fit: false
		});
		layout.on('layoutstop', () => {
			resolveTopLevelSpacing(cy);
			cy.zoom(previousZoom);
			cy.pan(previousPan);
		});
		layout.run();
	}

	return {
		cy,
		updateLevel,
		destroy: () => cy.destroy()
	};
}

function buildElements(level) {
	const groups = Object.entries(level.groups || {}).map(([groupName, props]) => ({
		data: {
			id: 'group:' + groupName,
			label: props.label,
			packageName: props.packageName,
			isGroup: true,
			parent: props.parentGroupName ? ('group:' + props.parentGroupName) : undefined
		}
	}));

	const nodes = Object.entries(level.components).map(([componentName, props]) => ({
		data: {
			id: componentName,
			label: props.label || componentName,
			size: props.size,
			packageName: props.packageName,
			isRootMarker: props.isRootMarker === true,
			parent: props.group ? ('group:' + props.group.name) : undefined
		}
	}));

	const edges = Object.entries(level.dependencies).map(([dependencyName, props]) => {
		const [from, to] = dependencyName.split(' -> ');
		return {
			data: {
				id: 'edge:' + from + '->' + to,
				source: from,
				target: to,
				strength: props.strength
			}
		};
	});

	return {nodes: [...groups, ...nodes], edges};
}

function moveNode(node, targetParentId) {
	const currentParent = node.parent().nonempty() ? node.parent().id() : undefined;
	if (currentParent === targetParentId) return;
	node.move({parent: targetParentId});
}

function initialNodePosition(data, cy, previousPositions) {
	if (previousPositions.has(data.id)) {
		return previousPositions.get(data.id);
	}

	const parentId = data.parent;
	if (parentId) {
		const parent = cy.getElementById(parentId);
		if (parent.nonempty()) {
			const position = parent.position();
			const siblingCount = parent.children().length;
			return {
				x: position.x + Math.min(40, siblingCount * 8),
				y: position.y + 36 + Math.min(30, siblingCount * 6)
			};
		}
	}

	return null;
}


/**
 * Returns true if the event happened on diagram background, not on elements (not on a node or an edge).
 */
function happenedOnBackground(event) {
	return event.target === event.cy;
}

function clickedGroupHeader(event) {
	const node = event.target;
	if (!node.isParent()) return false;
	const renderedPosition = event.renderedPosition || event.position;
	const renderedBox = node.renderedBoundingBox();
	const headerHeight = 28;
	return renderedPosition.y <= renderedBox.y1 + headerHeight;
}

function resolveTopLevelSpacing(cy) {
	resolveSiblingSpacing(cy, cy.nodes().filter(node => node.parent().empty()));
	cy.nodes(':parent').forEach(parent => {
		resolveSiblingSpacing(cy, parent.children());
	});
}

function resolveSiblingSpacing(cy, siblings) {
	const movable = siblings.filter(node => node.nonempty && node.nonempty());
	if (movable.length <= 1) return;
	for (let i = 0; i < 6; i++) {
		let moved = false;
		for (let leftIndex = 0; leftIndex < movable.length; leftIndex++) {
			for (let rightIndex = leftIndex + 1; rightIndex < movable.length; rightIndex++) {
				const left = movable[leftIndex];
				const right = movable[rightIndex];
				const adjustment = spacingAdjustment(left, right, TOP_LEVEL_GAP);
				if (!adjustment) continue;
				shiftWithinParent(cy, left, -adjustment.dx / 2, -adjustment.dy / 2);
				shiftWithinParent(cy, right, adjustment.dx / 2, adjustment.dy / 2);
				moved = true;
			}
		}
		if (!moved) break;
	}
}

function shiftWithinParent(cy, node, dx, dy) {
	if (node.parent().empty()) {
		shiftSubtree(node, dx, dy);
		return;
	}

	const parent = node.parent();
	const childBoxes = parent.children().map(child =>
		child.boundingBox({includeLabels: true, includeOverlays: false})
	);
	const bounds = mergeBoxes(childBoxes);
	if (!bounds) return;

	const parentBox = parent.boundingBox({includeLabels: true, includeOverlays: false});
	const allowedLeft = parentBox.x1 - bounds.x1;
	const allowedRight = parentBox.x2 - bounds.x2;
	const allowedUp = parentBox.y1 - bounds.y1;
	const allowedDown = parentBox.y2 - bounds.y2;

	const clampedDx = clamp(dx, allowedLeft, allowedRight);
	const clampedDy = clamp(dy, allowedUp, allowedDown);
	shiftSubtree(node, clampedDx, clampedDy);
}

function spacingAdjustment(left, right, gap) {
	const leftBox = left.boundingBox({includeLabels: true, includeOverlays: false});
	const rightBox = right.boundingBox({includeLabels: true, includeOverlays: false});

	const xDistance = edgeDistance(leftBox.x1, leftBox.x2, rightBox.x1, rightBox.x2);
	const yDistance = edgeDistance(leftBox.y1, leftBox.y2, rightBox.y1, rightBox.y2);

	const needHorizontal = xDistance < gap;
	const needVertical = yDistance < gap;

	if (!needHorizontal || !needVertical) return null;

	const centerDx = center(rightBox.x1, rightBox.x2) - center(leftBox.x1, leftBox.x2);
	const centerDy = center(rightBox.y1, rightBox.y2) - center(leftBox.y1, leftBox.y2);

	if (Math.abs(centerDx) >= Math.abs(centerDy)) {
		const direction = centerDx >= 0 ? 1 : -1;
		return {dx: direction * (gap - xDistance), dy: 0};
	}
	const direction = centerDy >= 0 ? 1 : -1;
	return {dx: 0, dy: direction * (gap - yDistance)};
}

function shiftSubtree(node, dx, dy) {
	node.descendants().union(node).forEach(element => {
		const position = element.position();
		element.position({
			x: position.x + dx,
			y: position.y + dy
		});
	});
}

function edgeDistance(startA, endA, startB, endB) {
	if (endA < startB) return startB - endA;
	if (endB < startA) return startA - endB;
	return 0;
}

function center(start, end) {
	return (start + end) / 2;
}

function mergeBoxes(boxes) {
	if (boxes.length === 0) return null;
	return boxes.reduce((merged, box) => ({
		x1: Math.min(merged.x1, box.x1),
		y1: Math.min(merged.y1, box.y1),
		x2: Math.max(merged.x2, box.x2),
		y2: Math.max(merged.y2, box.y2)
	}));
}

function clamp(value, min, max) {
	return Math.min(Math.max(value, min), max);
}
