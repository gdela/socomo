package pl.gdela.socomo.codemap;

import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * Key for dependency between two objects, to be used as key in dependency collections.
 */
class DepKey implements Comparable<DepKey> {

	private final Comparable from;
	private final Comparable to;

	private DepKey(Comparable from, Comparable to) {
		this.from = from;
		this.to = to;
	}

	static DepKey depKey(CodePackage from, CodePackage to) {
		return new DepKey(from, to);
	}

	static DepKey depKey(CodeMember from, CodeMember to) {
		return new DepKey(from, to);
	}

	@Override
	public String toString() {
		return from + " -> " + to;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		DepKey that = (DepKey) o;
		return new EqualsBuilder()
				.append(from, that.from)
				.append(to, that.to)
				.isEquals();
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder(17, 37)
				.append(from)
				.append(to)
				.toHashCode();
	}

	@Override
	public int compareTo(DepKey that) {
		return new CompareToBuilder()
				.append(from, that.from)
				.append(to, that.to)
				.toComparison();
	}
}
