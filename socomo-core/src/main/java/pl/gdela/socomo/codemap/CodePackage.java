package pl.gdela.socomo.codemap;


import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;
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
@JsonAutoDetect(fieldVisibility = NONE, getterVisibility = NONE)
public class CodePackage implements Comparable<CodePackage> {

	/**
	 * Fully qualified name of the package, eg. {@code java.util.concurrent}.
	 */
	@JsonProperty
	public final String fqn;

	private final Map<String, CodeMember> members = new TreeMap<>();

	CodePackage(String fqn) {
		this.fqn = notNull(fqn);
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

	/**
	 * Returns the size of this package. The returned number doesn't have a real-world
	 * meaning, so can be used only to relatively compare sizes of two packages, and
	 * this comparision should match an average opinion of a human developer.
	 */
	public int size() {
		Set<String> classes = new HashSet<>();
		for (CodeMember member : members.values()) {
			classes.add(member.className);
		}
		int numberOfClasses = classes.size();

		int sizeOfMembers = 0;
		for (CodeMember member : members.values()) {
			sizeOfMembers += member.size;
		}

		// an average size (number of bytecode instruction) per class ranges from 100 to 250 in various projects
		// that were checked, so using 500 multiplier below gives more weight to the number of classes, while still
		// considering size of members - and that should match a human opinion about the relative size of a package
		return 500 * numberOfClasses + sizeOfMembers;
	}

	@Override
	public String toString() {
		return fqn.isEmpty() ? "[top]" : fqn;
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
