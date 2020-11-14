package pl.gdela.socomo.codemap;

/**
 * Decides which members and member dependencies should be selected.
 */
public abstract class Selector {

	abstract boolean shouldSelect(CodeMember member);

	abstract boolean shouldSelect(MemberDep memberDep);

	/**
	 * Returns selector that selects members only from given origin and only those dependencies
	 * between them, which are not dependencies between members in the same package. Remaining
	 * self dependencies that are not selected are typically uninteresting.
	 */
	public static Selector nonSelfDepsAndFrom(final Origin origin) {
		return new Selector() {
			@Override
			boolean shouldSelect(CodeMember member) {
				return member.origin == origin;
			}
			@Override
			boolean shouldSelect(MemberDep memberDep) {
				return memberDep.from.packet != memberDep.to.packet // non self
					&& memberDep.from.origin == origin
					&& memberDep.to.origin == origin;
			}
		};
	}
}
