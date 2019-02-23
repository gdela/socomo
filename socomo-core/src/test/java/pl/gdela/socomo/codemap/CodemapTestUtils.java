package pl.gdela.socomo.codemap;

import java.util.ArrayList;
import java.util.List;

import org.assertj.core.api.Condition;

final class CodemapTestUtils {
	private CodemapTestUtils() { /* no instances */	}

	/**
	 * Returns string representation of collection of objects for easy and human readable comparisons.
	 * Due to type erasure we must have this chain of instanceof checks instead of nice overloaded methods.
	 */
	static List<String> asStrings(Iterable<?> objects) {
		List<String> strings = new ArrayList<>();
		for (Object object : objects) {
			strings.add(asString(object));
		}
		return strings;
	}

	private static String asString(Object object) {
		if (object instanceof CodeMember) {
			return asString((CodeMember) object);
		}
		if (object instanceof MemberDep) {
			return asString((MemberDep) object);
		}
		return object.toString(); // for some objects standard string is good enough
	}

	private static String asString(CodeMember member) {
		return member.relativeName();
	}

	private static String asString(MemberDep memberDep) {
		return memberDep.from.relativeName() + " -> " + memberDep.to.relativeName();
	}

	/**
	 * Returns condition that will be satisfied by {@link CodeMember} objects that are marked
	 * as defined in given origin.
	 */
	static Condition<CodeMember> fromOrigin(final Origin origin) {
		return new Condition<CodeMember>("from " + origin + " origin") {
			@Override
			public boolean matches(CodeMember member) {
				return member.origin == origin;
			}
		};
	}
}
