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
				.member("mixed.I", MAIN)
				.memberDep("intrn.A", "mixed.I")
				.memberDep("intrn.B", "mixed.E")
				.memberDep("mixed.I", "extrn.E")
				.memberDep("intrn.A", "extrn.E")
				.memberDep("intrn.B", "extrn.E")
				.getCodemap();
		log.info("given codemap:\n{}", allCode.formatted());
		assertThat(allCode.packet("intrn").members()).are(fromOrigin(MAIN));
		assertThat(allCode.packet("extrn").members()).are(fromOrigin(EXTERNAL));
		assertThat(allCode.packet("mixed").member("I")).is(fromOrigin(MAIN));
		assertThat(allCode.packet("mixed").member("E")).is(fromOrigin(EXTERNAL));

		// when
		Codemap mainCode = allCode.select(MAIN);
		log.info("main code is:\n{}", mainCode.formatted());

		// then
		softly.assertThat(asStrings(mainCode.packages())).as("packages").containsExactly("intrn", "mixed");
		CodePackage intrn = mainCode.packet("intrn");
		CodePackage mixed = mainCode.packet("mixed");
		softly.assertThat(asStrings(intrn.members())).as("intrn members").containsExactly("A", "B");
		softly.assertThat(asStrings(mixed.members())).as("mixed members").containsExactly("I");

		PackageDep intToMix = mainCode.packageDep(intrn, mixed);
		PackageDep mixToMix = mainCode.packageDep(mixed, mixed);
		softly.assertThat(asStrings(intToMix.memberDeps())).as("member deps from intrn to mixed").containsExactly("A -> I");
		softly.assertThat(asStrings(mixToMix.memberDeps())).as("member deps from mixed to mixed").isEmpty();
	}
}
