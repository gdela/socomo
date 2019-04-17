package pl.gdela.socomo.bytecode;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import pl.gdela.socomo.codemap.DepType;

import static java.util.Collections.sort;
import static org.apache.commons.lang3.Validate.notNull;
import static org.apache.commons.lang3.Validate.validState;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * Mock to be used with {@link BytecodeAnalyzer} to verify in unit tests if analyzer did its job correctly.
 *
 * This mock fails fast, whenever it detects that something's wrong, so that in the error stack trace
 * we can see which part of bytecode analyzer and ASM framework is buggy and needs fixing. The checks are
 * against the fixture loaded using reflection - we know we have test fixture on the class path, so we can
 * do this.
 */
class DependencyCollectorMock implements DependencyCollector {

	private Source subject;
	private final List<Target> expectedTargets = new ArrayList<>();

	private Class currentClass;
	private String currentMember;
	private final List<Source> actualSources = new ArrayList<>();
	private final List<Target> actualTargets = new ArrayList<>();

	DependencyCollectorMock expectForSource(Class sourceClass, String sourceMember) {
		validState(subject == null, "subject is already set");
		subject = new Source(sourceClass, sourceMember);
		return this;
	}

	DependencyCollectorMock expectForSource(Class sourceClass) {
		return expectForSource(sourceClass, null);
	}

	DependencyCollectorMock target(DepType type, Class targetClass, String targetMember) {
		validState(subject != null, "first set the subject");
		expectedTargets.add(new Target(type, targetClass, targetMember));
		return this;
	}

	DependencyCollectorMock target(DepType type, Class targetClass) {
		return target(type, targetClass, null);
	}

	DependencyCollectorMock noTargets() {
		expectedTargets.clear();
		return this;
	}

	@Override
	public void enterClass(String className) {
		notNull(className, "class name cannot be null");
		validState(currentClass == null, "cannot enter class %s, because %s was not exited", className, currentClass);
		currentClass = loadClass(className);
		actualSources.add(new Source(currentClass));
	}

	@Override
	public void enterMember(String memberName) {
		notNull(memberName, "member name cannot be null");
		validState(currentMember == null, "cannot enter member %s, because member %s was not exited", memberName, currentMember);
		validState(currentClass != null, "cannot enter member %s, when class is not yet entered", memberName);
		currentMember = memberName;
		actualSources.add(new Source(currentClass, currentMember));
	}

	@Override
	public void exitMember(int size) {
		validState(currentMember != null, "cannot exit member, because there is no current member");
		currentMember = null;
	}

	@Override
	public void exitClass() {
		validState(currentClass != null, "cannot exit class, because there is no current class");
		validState(currentMember == null, "cannot exit class, because member was not yet exited");
		currentClass = null;
	}

	@Override
	public void markDependency(DepType type, String className, String memberName) {
		notNull(type, "dependency type cannot be null (class:%s, member:%s)", className, memberName);
		notNull(className, "class name cannot be null (type:%s, member:%s)", type, memberName);
		validState(currentClass != null, "cannot mark dependency when not in class");
		if (!subject.matches(currentClass, currentMember)) {
			return; // mock only verifies targets from a single source
		}
		Target target = new Target(type, loadClass(className), memberName);
		if (!expectedTargets.contains(target)) {
			// fail fast, so that we can see in the stacktrace which ASM visitor is responsible for that
			fail("unexpected target '%s' from source '%s'\nexpected targets %s", target, subject, expectedTargets);
		}
		actualTargets.add(target);
	}

	void verify() {
		sort(actualTargets);
		sort(expectedTargets);
		// check that ASM visitor went into the source we're interested in
		assertThat(actualSources)
				.as("visited sources")
				.contains(subject);
		// check that all expected targets were found by ASM visitors
		assertThat(actualTargets)
				.as("targets from source %s", subject)
				.containsAll(expectedTargets);
	}

	/**
	 * The source of the dependency.
	 */
	private static class Source {
		private final Class sourceClass;
		private final String sourceMember;
		private Source(Class sourceClass, String sourceMember) {
			this.sourceClass = notNull(sourceClass);
			this.sourceMember = sourceMember;
		}
		private Source(Class sourceClass) {
			this(sourceClass, null);
		}
		private boolean matches(Class clazz, String member) {
			return Objects.equals(sourceClass, clazz) && Objects.equals(sourceMember, member);
		}
		@Override
		public String toString() {
			return sourceMember != null ? sourceClass.getName() + "." + sourceMember : sourceClass.getName();
		}
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			Source that = (Source) o;
			return new EqualsBuilder()
					.append(sourceClass, that.sourceClass)
					.append(sourceMember, that.sourceMember)
					.isEquals();
		}
		@Override
		public int hashCode() {
			return new HashCodeBuilder(17, 37)
					.append(sourceClass)
					.append(sourceMember)
					.toHashCode();
		}
	}

	/**
	 * The target of the dependency, together with dependency type.
	 */
	private static class Target implements Comparable<Target> {
		private final DepType type;
		private final Class targetClass;
		private final String targetMember;
		private Target(DepType type, Class targetClass, String targetMember) {
			this.type = notNull(type);
			this.targetClass = notNull(targetClass);
			this.targetMember = targetMember;
		}
		private Target(DepType type, Class targetClass) {
			this(type, targetClass, null);
		}
		@Override
		public String toString() {
			return type + " " + (targetMember != null ? targetClass.getName() + "." + targetMember : targetClass.getName());
		}
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			Target that = (Target) o;
			return new EqualsBuilder()
					.append(type, that.type)
					.append(targetClass, that.targetClass)
					.append(targetMember, that.targetMember)
					.isEquals();
		}
		@Override
		public int hashCode() {
			return new HashCodeBuilder(17, 37)
					.append(type)
					.append(targetClass)
					.append(targetMember)
					.toHashCode();
		}
		@Override
		public int compareTo(Target that) {
			return new CompareToBuilder()
					.append(type, that.type)
					.append(targetClass.getName(), that.targetClass.getName())
					.append(targetMember, that.targetMember)
					.toComparison();
		}
	}

	/**
	 * Loads the test fixture class.
	 */
	private static Class loadClass(String className) {
		try {
			return Class.forName(className);
		} catch (ClassNotFoundException ignored) {
			throw new IllegalArgumentException("class " + className + " not found using reflection");
		}
	}
}
