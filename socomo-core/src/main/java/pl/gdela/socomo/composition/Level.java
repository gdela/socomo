package pl.gdela.socomo.composition;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import static java.lang.Math.max;

/**
 * Level at which we model source code composition. Level name is actually a java package name,
 * all sibling and parent packages are not included in determining the composition. Only code in
 * subpackages (transitively) forms components of this level.
 */
public class Level {

	public final String name;
	public Collection<Component> components = new TreeSet<>();
	public Collection<ComponentDep> dependencies = new TreeSet<>();

	Level(String name) {
		this.name = name;
	}

	Component component(String name) {
		for (Component component : components) {
			if (component.name.equals(name)) {
				return component;
			}
		}
		Component component = new Component(name);
		components.add(component);
		return component;
	}

	ComponentDep dependency(Component from, Component to) {
		for (ComponentDep dependency : dependencies) {
			if (dependency.from.equals(from) && dependency.to.equals(to)) {
				return dependency;
			}
		}
		ComponentDep dependency = new ComponentDep(from, to);
		dependencies.add(dependency);
		return dependency;
	}

	public int maxComponentSize() {
		int maxSize = 0;
		for (Component component : components) {
			maxSize = max(maxSize, component.size);
		}
		return maxSize;
	}

	public int maxDependencyStrength() {
		int maxStrength = 0;
		for (ComponentDep dependency : dependencies) {
			maxStrength = max(maxStrength, dependency.strength);
		}
		return maxStrength;
	}

	/**
	 * Returns a human-friendly, textual multiline representation of the level.
	 */
	public String formatted() {
		StringBuilder out = new StringBuilder();
		Set<Component> shown = new HashSet<>();
		for (ComponentDep dependency : dependencies) {
			out.append(dependency.from);
			out.append(" => ");
			out.append(dependency.to);
			out.append("\n");
			shown.add(dependency.from);
			shown.add(dependency.to);
		}
		for (Component component : components) {
			if (!shown.contains(component)) {
				out.append(component);
				out.append("\n");
			}
		}
		return out.toString();
	}
}
