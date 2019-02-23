package pl.gdela.socomo.codemap;

import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import static pl.gdela.socomo.codemap.Origin.EXTERNAL;

/**
 * Member of the java code package. Java classes, methods and fields are represented
 * as members. Each member belongs to exactly one {@link CodePackage}.
 *
 * @see Codemap
 */
public class CodeMember implements Comparable<CodeMember> {

	/**
	 * The package to which member belongs.
	 */
	public final CodePackage packet;

	/**
	 * Simple name of the class to which member belongs.
	 */
	public final String className;

	/**
	 * Simple name of the member. May be {@code null}, and this means that this member represents the whole class.
	 */
	public final String memberName;

	/**
	 * Origin of the source code where member was declared.
	 */
	Origin origin = EXTERNAL;

	CodeMember(CodePackage packet, String className, String memberName) {
		this.packet = packet;
		this.className = className;
		this.memberName = memberName;
	}

	/**
	 * Returns name relative to the package to which this member belongs. The relative
	 * name consist of class name and member name, eg. {@code "Object.toString()"},
	 * or just class name if this member represents whole class, eg. {@code "Serializable"}.
	 */
	String relativeName() {
		return memberName != null ? className + "." + memberName : className;
	}

	void markOrigin(Origin newOrigin) {
		if (newOrigin.ordinal() < this.origin.ordinal()) {
			this.origin = newOrigin;
		}
	}

	@Override
	public String toString() {
		return packet + "." + relativeName();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		CodeMember that = (CodeMember) o;
		return new EqualsBuilder()
				.append(packet, that.packet)
				.append(className, that.className)
				.append(memberName, that.memberName)
				.isEquals();
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder(17, 37)
				.append(packet)
				.append(className)
				.append(memberName)
				.toHashCode();
	}

	@Override
	public int compareTo(CodeMember that) {
		return new CompareToBuilder()
				.append(packet, that.packet)
				.append(className, that.className)
				.append(memberName, that.memberName)
				.toComparison();
	}
}
