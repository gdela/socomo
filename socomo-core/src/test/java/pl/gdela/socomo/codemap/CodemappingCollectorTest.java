package pl.gdela.socomo.codemap;

import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static pl.gdela.socomo.codemap.CodemapTestUtils.asStrings;
import static pl.gdela.socomo.codemap.CodemapTestUtils.fromOrigin;
import static pl.gdela.socomo.codemap.DepType.CALLS;
import static pl.gdela.socomo.codemap.Origin.EXTERNAL;
import static pl.gdela.socomo.codemap.Origin.MAIN;

public class CodemappingCollectorTest {
	private static final Logger log = LoggerFactory.getLogger(CodemappingCollectorTest.class);

	private static Codemap codemap;

	private static CodePackage alfa;
	private static CodePackage beta;
	private static CodePackage gamma;

	@BeforeClass
	public static void collect_dependencies_and_build_codemap() {
		CodemappingCollector collector = new CodemappingCollector();

		// when
		for (String className : asList("alfa.A1", "alfa.A2", "beta.B")) {
			collector.enterClass(className);
			collector.markDependency(CALLS, "gamma.G1", null);
			collector.markDependency(CALLS, "gamma.G2", null);
			collector.enterMember("bla");
			collector.markDependency(CALLS, "gamma.G1", "x");
			collector.markDependency(CALLS, "gamma.G2", "y");
			collector.markDependency(CALLS, "gamma.G3", "z");
			collector.exitMember();
			collector.enterMember("foo");
			collector.markDependency(CALLS, "gamma.G1", "y");
			collector.markDependency(CALLS, "gamma.G2", "x");
			collector.markDependency(CALLS, "gamma.G3", "z");
			collector.exitMember();
			collector.exitClass();
		}
		codemap = collector.getCodemap();
		log.info("collector built following codemap:\n{}", codemap.formatted());

		// then
		assertThat(asStrings(codemap.packages())).as("packages").containsExactly("alfa", "beta", "gamma");
		alfa = codemap.packet("alfa");
		beta = codemap.packet("beta");
		gamma = codemap.packet("gamma");
	}

	@Test
	public void check_member_deps() {
		// then
		PackageDep alfaToGamma = codemap.packageDep(alfa, gamma);
		assertThat(asStrings(alfaToGamma.memberDeps())).as("member deps from alfa to gamma").containsExactly(
				"A1 -> G1", "A1 -> G2",
				"A1.bla -> G1.x", "A1.bla -> G2.y", "A1.bla -> G3.z",
				"A1.foo -> G1.y", "A1.foo -> G2.x", "A1.foo -> G3.z",
				"A2 -> G1", "A2 -> G2",
				"A2.bla -> G1.x", "A2.bla -> G2.y", "A2.bla -> G3.z",
				"A2.foo -> G1.y", "A2.foo -> G2.x", "A2.foo -> G3.z"
		);
		assertThat(alfaToGamma.strength()).isEqualTo(16);
	}

	@Test
	public void check_package_deps() {
		// then
		assertThat(asStrings(codemap.packageDeps())).as("package deps").containsExactly(
				"alfa -> gamma",
				"beta -> gamma"
		);
	}

	@Test
	public void check_members() {
		// then
		assertThat(asStrings(alfa.members())).as("alfa members").containsExactly(
				"A1", "A1.bla", "A1.foo",
				"A2", "A2.bla", "A2.foo"
		);
		assertThat(asStrings(beta.members())).as("beta members").containsExactly(
				"B", "B.bla", "B.foo"
		);
		assertThat(asStrings(gamma.members())).as("gamma members").containsExactly(
				"G1", "G1.x", "G1.y",
				"G2", "G2.x", "G2.y",
				"G3.z"
		);
	}

	@Test
	public void check_origins() {
		assertThat(alfa.members()).are(fromOrigin(MAIN));
		assertThat(beta.members()).are(fromOrigin(MAIN));
		assertThat(gamma.members()).are(fromOrigin(EXTERNAL));
	}
}
