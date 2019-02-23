package pl.gdela.socomo.bytecode;

import org.objectweb.asm.Type;
import pl.gdela.socomo.codemap.DepType;

/**
 * Adapts the technology agnostic (string based) {@link DependencyCollector} interface, to the
 * ASM types used by the current implementation of {@link BytecodeAnalyzer}.
 */
class DependencyCollectorAdapter {

	private final DependencyCollector adaptee;

	DependencyCollectorAdapter(DependencyCollector adaptee) {
		this.adaptee = adaptee;
	}

	void enterClass(Type sourceClass) {
		adaptee.enterClass(sourceClass.getClassName());
	}

	void enterMember(String sourceMemberName) {
		adaptee.enterMember(sourceMemberName);
	}

	void markDependency(DepType type, Type targetClass, String targetMemberName) {

		// java built-ins on arrays, like .clone() or .length, are not interesting
		if (isArray(targetClass) && targetMemberName != null) return;

		// simplification: treat dependency to array as dependency to the type of elements
		Type flatTargetClass = flatten(targetClass);

		// dependencies to primitive types are not interesting
		if (isPrimitive(flatTargetClass)) return;

		// the rest is what we're interested about
		adaptee.markDependency(type, flatTargetClass.getClassName(), targetMemberName);
	}

	void markDependency(DepType type, Type targetClass) {
		markDependency(type, targetClass, null);
	}

	void exitMember() {
		adaptee.exitMember();
	}

	void exitClass() {
		adaptee.exitClass();
	}

	// below only utilities

	/**
	 * Returns true if type is one of the java primitive types or void.
	 */
	private static boolean isPrimitive(Type type) {
		switch (type.getSort()) {
			case Type.VOID:
			case Type.BOOLEAN:
			case Type.CHAR:
			case Type.BYTE:
			case Type.SHORT:
			case Type.INT:
			case Type.FLOAT:
			case Type.LONG:
			case Type.DOUBLE:
				return true;
			default:
				return false;
		}
	}

	/**
	 * Returns true if type is an array of objects or primitive types.
	 */
	private static boolean isArray(Type type) {
		return type.getSort() == Type.ARRAY;
	}

	/**
	 * Unwraps array types to get the underlying non-array element type, unless given
	 * type is already a non-array. For multidimensional arrays, the deepest element
	 * type is returned.
	 *
	 * <p>The flattening is used for simplification, when some code uses {@code SomeClass[]},
	 * we just say that it depends on {@code SomeClass} itself, not on the array.</p>
	 */
	private static Type flatten(Type type) {
		if (isArray(type)) {
			return flatten(type.getElementType());
		}
		return type; // already flat
	}
}
