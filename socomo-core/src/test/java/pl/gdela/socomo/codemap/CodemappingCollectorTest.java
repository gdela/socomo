package pl.gdela.socomo.codemap;

import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Arrays.asList;
import static org.apache.commons.collections4.CollectionUtils.union;
import static org.apache.commons.lang3.ObjectUtils.firstNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static pl.gdela.socomo.codemap.CodemapTestUtils.asStrings;
import static pl.gdela.socomo.codemap.CodemapTestUtils.fromOrigin;
import static pl.gdela.socomo.codemap.DepType.CALLS;
import static pl.gdela.socomo.codemap.DepType.EXTENDS;
import static pl.gdela.socomo.codemap.DepType.REFERENCES;
import static pl.gdela.socomo.codemap.Origin.EXTERNAL;
import static pl.gdela.socomo.codemap.Origin.MAIN;

public class CodemappingCollectorTest {
	private static final Logger log = LoggerFactory.getLogger(CodemappingCollectorTest.class);

	private static Codemap codemap;

	private static CodePackage alfa;
	private static CodePackage beta;
	private static CodePackage gamma;

	private static final int sizeOfBla = 3;
	private static final int sizeOfFoo = 7;

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
			collector.exitMember(sizeOfBla);
			collector.enterMember("foo");
			collector.markDependency(CALLS, "gamma.G1", "y");
			collector.markDependency(CALLS, "gamma.G2", "x");
			collector.markDependency(CALLS, "gamma.G3", "z");
			collector.exitMember(sizeOfFoo);
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

	@Test
	public void check_member_sizes() {
		for (CodeMember member : union(alfa.members(), beta.members())) {
			int expectedSize;
			switch (firstNonNull(member.memberName, "_none_")) {
				case "bla": expectedSize = sizeOfBla; break;
				case "foo": expectedSize = sizeOfFoo; break;
				default: expectedSize = 0; break;
			}
			assertThat(member.size).as("size of %s", member).isEqualTo(expectedSize);
		}
		for (CodeMember member : gamma.members()) {
			assertThat(member.size).as("size of %s", member).isZero();
		}
	}

	@Test
	public void check_package_sizes() {
		assertThat(alfa.size()).as("size of %s", alfa).isEqualTo(beta.size() * 2);
	}

	@Test
	public void root_package() {
		// when
		CodemappingCollector collector = new CodemappingCollector();
		collector.enterClass("Top1");
		collector.markDependency(EXTENDS, "Top2", null);
		collector.enterMember("main1");
		collector.markDependency(CALLS, "Top2", "main2");
		collector.markDependency(REFERENCES, "sub.Sub", null);
		collector.markDependency(CALLS, "sub.Sub", "subMain");
		Codemap topLevelMap = collector.getCodemap();
		CodePackage topLevelPacket = topLevelMap.packet("");

		// then
		assertThat(asStrings(topLevelMap.packageDeps())).as("package deps").containsExactly(
			"[top] -> [top]",
			"[top] -> sub"
		);

		PackageDep selfDeps = topLevelMap.packageDep(topLevelPacket, topLevelPacket);
		assertThat(asStrings(selfDeps.memberDeps())).as("self deps in top level").containsExactly(
			"Top1 -> Top2",
			"Top1.main1 -> Top2.main2"
		);

		assertThat(asStrings(topLevelPacket.members())).as("top level members").containsExactly(
			"Top1", "Top1.main1",
			"Top2", "Top2.main2"
		);
	}
}
