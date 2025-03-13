package pl.gdela.socomo.composition;

import org.junit.Test;
import pl.gdela.socomo.codemap.Codemap;
import pl.gdela.socomo.codemap.CodemapBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static pl.gdela.socomo.composition.LevelGuesser.guessLevel;

public class LevelGuesserTest {

	@Test
	public void guesses_most_interesting_level() {
		// given
		Codemap codemap = new CodemapBuilder()
				.member("com.alfa.C01")
				.member("com.beta.foo.C02")
				.member("com.beta.bla.C03")
				.member("com.beta.bla.x.C04")
				.member("com.beta.bla.x.C05")
				.member("com.beta.bla.x.C06")
				.member("com.beta.bla.x.C07")
				.member("com.beta.bla.x.C08")
				.member("com.beta.bla.y.C09")
				.member("com.beta.bla.y.C10")
				.member("com.beta.bla.y.C11")
				.member("com.beta.bla.y.C12")
				.member("com.beta.bla.y.C13")
				.getCodemap();

		// when
		String mostInterestingLevel = guessLevel(codemap);

		// then
		assertThat(mostInterestingLevel).isEqualTo("com.beta.bla");
	}

	@Test
	public void guesses_top_level() {
		// given
		Codemap codemap = new CodemapBuilder()
			.member("C01")
			.member("C02")
			.member("C03")
			.member("foo.C05")
			.member("bla.C06")
			.getCodemap();

		// when
		String mostInterestingLevel = guessLevel(codemap);

		// then
		assertThat(mostInterestingLevel).isEqualTo("");
	}
}
