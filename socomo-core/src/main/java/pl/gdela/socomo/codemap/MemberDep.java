package pl.gdela.socomo.codemap;

import org.apache.commons.lang3.builder.CompareToBuilder;

import static org.apache.commons.lang3.Validate.notNull;

/**
 * Directed dependency between two {@link CodeMember}s.
 */
public class MemberDep implements Comparable<MemberDep> {

	/**
	 * The member that uses {@code to} member.
	 */
	public final CodeMember from;

	/**
	 * The member that is used by {@code from} member.
	 */
	public final CodeMember to;

	/**
	 * Type of the usage of {@code to} member by {@code from} member;
	 */
	DepType type;

	MemberDep(CodeMember from, CodeMember to) {
		this.from = notNull(from);
		this.to = notNull(to);
	}

	@Override
	public String toString() {
		return from + " -> " + to;
	}

	@Override
	public int compareTo(MemberDep that) {
		return new CompareToBuilder()
				.append(from, that.from)
				.append(to, that.to)
				.toComparison();
	}
}
