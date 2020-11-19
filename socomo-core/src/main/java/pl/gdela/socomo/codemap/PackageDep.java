package pl.gdela.socomo.codemap;

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.CompareToBuilder;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;
import static org.apache.commons.lang3.Validate.isTrue;
import static org.apache.commons.lang3.Validate.notNull;
import static pl.gdela.socomo.codemap.DepKey.depKey;

/**
 * Directed dependency between two {@link CodePackage}s.
 */
@JsonAutoDetect(fieldVisibility = NONE, getterVisibility = NONE)
public class PackageDep implements Comparable<PackageDep> {

	/**
	 * The package that uses {@code to} package.
	 */
	@JsonProperty
	public final CodePackage from;

	/**
	 * The package that is used by {@code from} package.
	 */
	@JsonProperty
	public final CodePackage to;

	/**
	 * Detailed set of dependencies between {@code from} and {@code to} package's members.
	 * This is always a non-empty set, because from member dependencies we derived that
	 * packages are dependent.
	 */
	private final Map<DepKey, MemberDep> memberDeps = new TreeMap<>();

	PackageDep(CodePackage from, CodePackage to) {
		this.from = notNull(from);
		this.to = notNull(to);
	}

	@JsonProperty
	public Collection<MemberDep> memberDeps() {
		return memberDeps.values();
	}

	/**
	 * Returns dependency between members, creating one if needed.
	 * @param from the source of member dependency, it must belong to the 'from' side of this package dependency
	 * @param to the member on which one depends, it must belong to the 'to' side of this package dependency
	 */
	MemberDep memberDep(CodeMember from, CodeMember to) {
		notNull(from, "from cannot be null");
		notNull(to, "to cannot be null");
		isTrue(from.packet.equals(this.from), "'from' member %s must belong to the 'from' package %s", from, this.from);
		isTrue(to.packet.equals(this.to), "'to' member %s must belong to the 'to' package %s", to, this.to);
		DepKey key = depKey(from, to);
		MemberDep memberDep = memberDeps.get(key);
		if (memberDep == null) {
			memberDep = new MemberDep(from, to);
			memberDeps.put(key, memberDep);
		}
		return memberDep;
	}

	public int strength() {
		return memberDeps.size();
	}

	@Override
	public String toString() {
		return from + " -> " + to;
	}

	@Override
	public int compareTo(PackageDep that) {
		return new CompareToBuilder()
				.append(from, that.from)
				.append(to, that.to)
				.toComparison();
	}
}
