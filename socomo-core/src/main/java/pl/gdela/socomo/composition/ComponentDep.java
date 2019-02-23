package pl.gdela.socomo.composition;

import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class ComponentDep implements Comparable<ComponentDep> {

	public final Component from;
	public final Component to;
	public int strength;

	ComponentDep(Component from, Component to) {
		this.from = from;
		this.to = to;
	}

	void incrementStrengthBy(int increment) {
		strength += increment;
	}

	@Override
	public String toString() {
		return from + " -> " + to;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		ComponentDep that = (ComponentDep) o;
		return new EqualsBuilder()
				.append(from, that.from)
				.append(to, that.to)
				.isEquals();
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder(17, 9)
				.append(from)
				.append(to)
				.toHashCode();
	}

	@Override
	public int compareTo(ComponentDep that) {
		return new CompareToBuilder()
				.append(from, that.from)
				.append(to, that.to)
				.toComparison();
	}
}
