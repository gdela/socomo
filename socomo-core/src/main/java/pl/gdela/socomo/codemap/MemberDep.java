package pl.gdela.socomo.codemap;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.CompareToBuilder;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;
import static org.apache.commons.lang3.Validate.notNull;

/**
 * Directed dependency between two {@link CodeMember}s.
 */
@JsonAutoDetect(fieldVisibility = NONE, getterVisibility = NONE)
public class MemberDep implements Comparable<MemberDep> {

	/**
	 * The member that uses {@code to} member.
	 */
	@JsonProperty
	public final CodeMember from;

	/**
	 * The member that is used by {@code from} member.
	 */
	@JsonProperty
	public final CodeMember to;

	/**
	 * Type of the usage of {@code to} member by {@code from} member;
	 */
	@JsonProperty
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
