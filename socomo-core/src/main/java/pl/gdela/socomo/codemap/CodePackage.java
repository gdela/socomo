package pl.gdela.socomo.codemap;


import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.Validate.isTrue;
import static org.apache.commons.lang3.Validate.notNull;

/**
 * Package of java code. Grouped in a package are classes (interfaces, enums and annotations
 * are also considered as classes) and their methods and fields. Each, a class, a method and
 * a field, is represented as {@link CodeMember}.
 *
 * @see Codemap
 */
public class CodePackage implements Comparable<CodePackage> {

	/**
	 * Fully qualified name of the package, eg. {@code java.util.concurrent}.
	 */
	public final String fqn;

	private final Map<String, CodeMember> members = new TreeMap<>();

	CodePackage(String fqn) {
		this.fqn = fqn;
	}

	public Collection<CodeMember> members() {
		return members.values();
	}

	/**
	 * Returns a method of field member by given class name in which the member is defined
	 * and the member name. If needed, the member will be created.
	 * @param className simple class name, eg. {@code HashMap}
	 * @param classMemberName simple member name, eg. {@code put()} for methods or {@code size} for fields
	 */
	CodeMember member(String className, String classMemberName) {
		notNull(className, "class name cannot be null");
		isTrue(isNotBlank(className), "class name cannot be blank");
		isTrue(classMemberName == null || isNotBlank(classMemberName), "class member name cannot be blank (but can be null)");
		String key = classMemberName != null ? className + "." + classMemberName : className;
		CodeMember member = members.get(key);
		if (member == null) {
			member = new CodeMember(this, className, classMemberName);
			members.put(key, member);
		}
		return member;
	}

	/**
	 * Returns a class member by given class name, creating one if needed.
	 * @param className simple class name, eg. {@code HashMap}
	 */
	CodeMember member(String className) {
		return member(className, null);
	}

	public int size() {
		return members.size();
	}

	@Override
	public String toString() {
		return fqn;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		CodePackage that = (CodePackage) o;
		return new EqualsBuilder()
				.append(fqn, that.fqn)
				.isEquals();
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder(17, 37)
				.append(fqn)
				.toHashCode();
	}

	@Override
	public int compareTo(CodePackage that) {
		return new CompareToBuilder()
				.append(fqn, that.fqn)
				.toComparison();
	}
}
