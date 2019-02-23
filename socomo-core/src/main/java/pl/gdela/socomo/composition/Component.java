package pl.gdela.socomo.composition;

import java.util.Objects;

public class Component implements Comparable<Component> {

	public final String name;
	public int size;

	Component(String name) {
		this.name = name;
	}

	public void incrementSizeBy(int increment) {
		size += increment;
	}

	@Override
	public String toString() {
		return name;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Component that = (Component) o;
		return Objects.equals(this.name, that.name);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.name);
	}

	@Override
	public int compareTo(Component that) {
		return this.name.compareTo(that.name);
	}
}
