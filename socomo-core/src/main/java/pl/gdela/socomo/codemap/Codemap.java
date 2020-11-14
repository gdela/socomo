package pl.gdela.socomo.codemap;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static org.apache.commons.lang3.Validate.isTrue;
import static org.apache.commons.lang3.Validate.notNull;
import static pl.gdela.socomo.codemap.DepKey.depKey;

/**
 * Map of the java code: what we have in it and what is dependant on what. Java code consists
 * of methods, fields and classes, which are grouped into packages, we represent this organization
 * as {@link CodeMember} objects that belong to {@link CodePackage} objects.
 *
 * <p>Note that here we follow java approach, that there's no such concept as subpackages.
 * What by name seems like a subpackage is considered a completely separate entity,
 * and classes in subpackage belong only to it, they do not belong transitively to the parent
 * package. Although later, when we will transform codemap to level composition data, we will
 * do exactly that, i.e. flatten subpackages from the same parent into one component.
 */
public class Codemap {

	private final Map<String, CodePackage> packages = new TreeMap<>();

	private final Map<DepKey, PackageDep> packageDeps = new TreeMap<>();

	public Collection<CodePackage> packages() {
		return packages.values();
	}

	public Collection<PackageDep> packageDeps() {
		return packageDeps.values();
	}

	/**
	 * Returns the package represented by given name, creating one if needed.
	 * @param fqn fully qualified name of the package, eg. {@code java.util.concurrent}
	 */
	CodePackage packet(String fqn) {
		notNull(fqn, "fqn cannot be null");
		CodePackage packet = packages.get(fqn);
		if (packet == null) {
			packet = new CodePackage(fqn);
			packages.put(fqn, packet);
		}
		return packet;
	}

	/**
	 * Returns dependency between packages, creating one if needed.
	 */
	PackageDep packageDep(CodePackage from, CodePackage to) {
		notNull(from, "from cannot be null");
		notNull(to, "to cannot be null");
		isTrue(packages.containsKey(from.fqn), "'from' package %s must belong to this codemap", from);
		isTrue(packages.containsKey(to.fqn), "'to' package %s must belong to this codemap", to);
		DepKey key = depKey(from, to);
		PackageDep packageDep = packageDeps.get(key);
		if (packageDep == null) {
			packageDep = new PackageDep(from, to);
			packageDeps.put(key, packageDep);
		}
		return packageDep;
	}

	/**
	 * Returns a new codemap that contains only code elements selected by given selector.
	 */
	public Codemap select(Selector selector) {
		Codemap selected = new Codemap();
		for (CodePackage packet : this.packages()) {
			for (CodeMember member : packet.members()) {
				if (selector.shouldSelect(member)) {
					CodeMember selectedMember = selected.packet(packet.fqn).member(member.className, member.memberName);
					selectedMember.size = member.size;
					selectedMember.origin = member.origin;
				}
			}
		}
		for (PackageDep packageDep : this.packageDeps()) {
			for (MemberDep memberDep : packageDep.memberDeps()) {
				if (selector.shouldSelect(memberDep)) {
					MemberDep selectedMemberDep = selected.packageDep(packageDep.from, packageDep.to).memberDep(memberDep.from, memberDep.to);
					selectedMemberDep.type = memberDep.type;
				}
			}
		}
		return selected;
	}

	/**
	 * Returns a human-friendly, textual multiline representation of the codemap.
	 */
	public String formatted() {
		StringBuilder out = new StringBuilder();
		Set<CodePackage> shown = new HashSet<>();
		for (PackageDep packageDep : packageDeps()) {
			out.append(packageDep.from);
			out.append(" => ");
			out.append(packageDep.to);
			out.append("\n");
			shown.add(packageDep.from);
			shown.add(packageDep.to);
			for (MemberDep memberDep : packageDep.memberDeps()) {
				out.append("  ");
				out.append(memberDep.from.relativeName());
				out.append(" -> ");
				out.append(memberDep.to.relativeName());
				out.append("\n");
			}
		}
		for (CodePackage packet : packages()) {
			if (!shown.contains(packet)) {
				out.append(packet);
				out.append("\n");
			}
		}
		return out.toString();
	}
}
