package pl.gdela.socomo.codemap;

import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static pl.gdela.socomo.codemap.CodemapTestUtils.asStrings;
import static pl.gdela.socomo.codemap.CodemapTestUtils.fromOrigin;
import static pl.gdela.socomo.codemap.Origin.EXTERNAL;
import static pl.gdela.socomo.codemap.Origin.MAIN;
import static pl.gdela.socomo.codemap.Origin.TEST;
import static pl.gdela.socomo.codemap.Selector.nonSelfDepsAndFrom;

public class CodemapTest {
	private static final Logger log = LoggerFactory.getLogger(CodemapTest.class);

	@Rule
	public final JUnitSoftAssertions softly = new JUnitSoftAssertions();

	@Test
	@SuppressWarnings("SpellCheckingInspection")
	public void selecting_codemap_by_origin() {
		// given
		Codemap allCode = new CodemapBuilder()
				.member("intrn.A", MAIN)
				.member("intrn.B", MAIN)
				.member("intrn.C", MAIN)
				.member("mixed.I", MAIN)
				.memberDep("intrn.A", "mixed.I")
				.memberDep("intrn.B", "mixed.E")
				.memberDep("intrn.A", "extrn.X")
				.memberDep("intrn.B", "extrn.X")
				.memberDep("mixed.I", "extrn.X")
				.memberDep("mixed.I", "intrn.C")
				.getCodemap();
		log.info("given codemap:\n{}", allCode.formatted());
		assertThat(allCode.packet("intrn").members()).are(fromOrigin(MAIN));
		assertThat(allCode.packet("extrn").members()).are(fromOrigin(EXTERNAL));
		assertThat(allCode.packet("mixed").member("I")).is(fromOrigin(MAIN));
		assertThat(allCode.packet("mixed").member("E")).is(fromOrigin(EXTERNAL));

		// when
		Codemap mainCode = allCode.select(nonSelfDepsAndFrom(MAIN));
		log.info("main code is:\n{}", mainCode.formatted());

		// then
		softly.assertThat(asStrings(mainCode.packages())).as("packages").containsExactly("intrn", "mixed");
		CodePackage intrn = mainCode.packet("intrn");
		CodePackage mixed = mainCode.packet("mixed");
		softly.assertThat(asStrings(intrn.members())).as("intrn members").containsExactly("A", "B", "C");
		softly.assertThat(asStrings(mixed.members())).as("mixed members").containsExactly("I");

		PackageDep intToMix = mainCode.packageDep(intrn, mixed);
		PackageDep mixToInt = mainCode.packageDep(mixed, intrn);
		softly.assertThat(asStrings(intToMix.memberDeps())).as("member deps from intrn to mixed").containsExactly("A -> I");
		softly.assertThat(asStrings(mixToInt.memberDeps())).as("member deps from mixed to intrn").containsExactly("I -> C");

		for (CodePackage packet : mainCode.packages()) {
			assertThat(packet.members()).are(fromOrigin(MAIN));
		}
	}

	@Test
	@SuppressWarnings("SpellCheckingInspection")
	public void selecting_codemap_non_self_dependencies_only() {
		// given
		Codemap allCode = new CodemapBuilder()
			.member("alfa.X", TEST)
			.member("alfa.Y", TEST)
			.member("beta.X", TEST)
			.member("beta.Y", TEST)
			.member("gama.Z", TEST)
			.memberDep("alfa.X", "alfa.Y")
			.memberDep("alfa.X", "beta.Y")
			.memberDep("beta.X", "alfa.Y")
			.memberDep("beta.X", "beta.Y")
			.getCodemap();
		log.info("given codemap:\n{}", allCode.formatted());

		// when
		Codemap mainCode = allCode.select(nonSelfDepsAndFrom(TEST));
		log.info("non self dependcies are:\n{}", mainCode.formatted());

		// then
		softly.assertThat(asStrings(mainCode.packages())).as("packages").containsExactly("alfa", "beta", "gama");
		CodePackage alfa = mainCode.packet("alfa");
		CodePackage beta = mainCode.packet("beta");
		CodePackage gama = mainCode.packet("gama");
		softly.assertThat(asStrings(alfa.members())).as("alfa members").containsExactly("X", "Y");
		softly.assertThat(asStrings(beta.members())).as("beta members").containsExactly("X", "Y");
		softly.assertThat(asStrings(gama.members())).as("gama members").containsExactly("Z");

		PackageDep alfaToBeta = mainCode.packageDep(alfa, beta);
		PackageDep betaToAlfa = mainCode.packageDep(beta, alfa);
		PackageDep alfaToAlfa = mainCode.packageDep(alfa, alfa);
		PackageDep betaToBeta = mainCode.packageDep(beta, beta);
		softly.assertThat(asStrings(alfaToBeta.memberDeps())).as("member deps from alfa to beta").containsExactly("X -> Y");
		softly.assertThat(asStrings(betaToAlfa.memberDeps())).as("member deps from beta to alfa").containsExactly("X -> Y");
		softly.assertThat(asStrings(alfaToAlfa.memberDeps())).as("member deps from alfa to alfa").isEmpty();
		softly.assertThat(asStrings(betaToBeta.memberDeps())).as("member deps from beta to beta").isEmpty();
	}
}
