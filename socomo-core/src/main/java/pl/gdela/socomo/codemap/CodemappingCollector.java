package pl.gdela.socomo.codemap;

import pl.gdela.socomo.bytecode.DependencyCollector;

import static org.apache.commons.lang3.StringUtils.substringAfterLast;
import static org.apache.commons.lang3.StringUtils.substringBeforeLast;
import static pl.gdela.socomo.codemap.Origin.MAIN;

/**
 * Builds a codemap from the information supplied via {@link DependencyCollector} methods.
 */
public class CodemappingCollector implements DependencyCollector {

	private final Codemap codemap = new Codemap();

	private CodePackage currentPackage;
	private String currentClassName;
	private CodeMember currentMember;

	@Override
	public void enterClass(String classFqn) {
		String packageFqn = substringBeforeLast(classFqn, ".");
		currentClassName = substringAfterLast(classFqn, ".");
		currentPackage = codemap.packet(packageFqn);
		currentMember = currentPackage.member(currentClassName);
		currentMember.markOrigin(MAIN);
	}

	@Override
	public void enterMember(String classMemberName) {
		currentMember = currentPackage.member(currentClassName, classMemberName);
		currentMember.markOrigin(MAIN);
	}

	@Override
	public void markDependency(DepType type, String classFqn, String classMemberName) {
		String packageFqn = substringBeforeLast(classFqn, ".");
		String className = substringAfterLast(classFqn, ".");
		CodePackage packet = codemap.packet(packageFqn);
		CodeMember member = packet.member(className, classMemberName);
		PackageDep packageDep = codemap.packageDep(currentPackage, packet);
		packageDep.memberDep(currentMember, member);
	}

	@Override
	public void exitMember() {
		currentMember = null;
	}

	@Override
	public void exitClass() {
		currentPackage = null;
		currentClassName = null;
	}

	public Codemap getCodemap() {
		return codemap;
	}
}
