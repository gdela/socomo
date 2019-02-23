package pl.gdela.socomo.codemap;

import static java.lang.Character.isLowerCase;
import static java.lang.Character.isUpperCase;
import static org.apache.commons.lang3.StringUtils.join;
import static org.apache.commons.lang3.StringUtils.split;
import static pl.gdela.socomo.codemap.Origin.EXTERNAL;

/**
 * Utility to build {@link Codemap} by providing just a list of from member / to member pairs.
 */
public class CodemapBuilder {

	private final Codemap codemap = new Codemap();

	/**
	 * Adds member and necessary package to the codemap.
	 * @param fqn fully qualified name of the from member, eg. {@code "java.lang.Object.toString()"},
	 *            the class name part is recognized by letter case
	 * @param origin the simulated origin of this member.
	 */
	public CodemapBuilder member(String fqn, Origin origin) {
		Names names = names(fqn);
		CodePackage packet = codemap.packet(names.packageFqn);
		CodeMember member = packet.member(names.className, names.classMemberName);
		member.markOrigin(origin);
		return this;
	}

	/**
	 * Adds member and necessary package to the codemap.
	 * @param fqn fully qualified name of the from member, eg. {@code "java.lang.Object.toString()"},
	 *            the class name part is recognized by letter case
	 */
	public CodemapBuilder member(String fqn) {
		return member(fqn, EXTERNAL);
	}


	/**
	 * Adds dependency and necessary packages and members to the codemap.
	 * @param fromFqn fully qualified name of the from member, eg. {@code "java.lang.Object.toString()"},
	 *                the class name part is recognized by letter case
	 * @param toFqn fully qualified name of the to member, eg. {@code "java.lang.String.ctor()}
	 *              the class name part is recognized by letter case
	 */
	public CodemapBuilder memberDep(String fromFqn, String toFqn) {
		Names fromNames = names(fromFqn);
		Names toNames = names(toFqn);
		CodePackage fromPackage = codemap.packet(fromNames.packageFqn);
		CodePackage toPackage = codemap.packet(toNames.packageFqn);
		PackageDep packageDep = codemap.packageDep(fromPackage, toPackage);
		CodeMember fromMember = fromPackage.member(fromNames.className, fromNames.classMemberName);
		CodeMember toMember = toPackage.member(toNames.className, toNames.classMemberName);
		packageDep.memberDep(fromMember, toMember);
		return this;
	}

	private static Names names(String memberFqn) {
		Names names = new Names();
		String[] elements = split(memberFqn, '.');
		if (elements.length == 1) {
			// just one element, must be a class name
			names.className = elements[0];
			names.packageFqn = "";
		} else {
			String last = elements[elements.length - 1];
			String nextToLast = elements[elements.length - 2];
			if (isUpperCase(last.charAt(0)) && isLowerCase(nextToLast.charAt(0))) {
				// probably just a class name, without class member name
				names.className = last;
				names.classMemberName = null;
				names.packageFqn = join(elements, '.', 0, elements.length - 1);
			} else {
				// probably class name and a specific member name
				names.className = nextToLast;
				names.classMemberName = last;
				names.packageFqn = join(elements, '.', 0, elements.length - 2);
			}
		}
		return names;
	}

	private static class Names {
		private String packageFqn;
		private String className;
		private String classMemberName;
	}

	public Codemap getCodemap() {
		return codemap;
	}
}
