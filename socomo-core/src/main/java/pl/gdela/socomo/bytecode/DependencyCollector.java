package pl.gdela.socomo.bytecode;

import pl.gdela.socomo.codemap.DepType;

/**
 * Collects dependencies from the bytecode.
 */
public interface DependencyCollector {

	/**
	 * Signals the source class of the dependencies that will be marked subsequently.
	 * @param className fully qualified class name, for example {@code "java.lang.Object"}.
	 */
	void enterClass(String className);

	/**
	 * Signals the source member of the dependencies that will be marked subsequently.
	 * @param memberName class member name, for example {@code "equals()"}.
	 */
	void enterMember(String memberName);

	/**
	 * Marks a dependency to a target class and member. The source of the dependency is the current source class
	 * and current source member, as were set in {@code enterClass()} and {@code enterMember()} methods. Note that
	 * the they may be no specific source member of this dependency.
	 * @param type type of dependency
	 * @param className fully qualified target class name
	 * @param memberName target class member name, may be {@code null} if dependency is not to a specific member
	 */
	void markDependency(DepType type, String className, String memberName);

	/**
	 * Signals end of the source member.
	 * @param size of the member, roughly the number of bytecode instruction that this member consists of
	 */
	void exitMember(int size);

	/**
	 * Signals end of the source class.
	 */
	void exitClass();
}
