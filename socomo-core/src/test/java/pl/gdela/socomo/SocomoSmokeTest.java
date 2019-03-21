package pl.gdela.socomo;

import java.io.File;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.contentOf;

/**
 * Smoke test of the whole socomo-core module integrated together.
 */
public class SocomoSmokeTest {

	private final SocomoFacade socomo = new SocomoFacade("dummy-module");
	private final File input = new File("target/classes");
	private final File output = new File("target/socomo-smoke-test-output.html");

	@Test
	public void basic_usage() {
		// when
		socomo.analyzeBytecode(input);
		socomo.guessLevel();
		socomo.visualizeInto(output);

		// then
		assertThat(output).exists();
		assertThat(contentOf(output))
				.contains("'dummy-module'") // module name
				.contains("'pl.gdela.socomo'") // guessed level
				.contains(
						// sample components:
						"'[root]'",
						"'bytecode'",
						"'codemap'",
						"'composition'"
				)
				.contains( // some dependencies:
						"'[root] -> bytecode'",
						"'bytecode -> codemap'",
						"'composition -> codemap"
				);
	}

	@Test
	public void explicitly_chosen_level() {
		// when
		socomo.analyzeBytecode(input);
		socomo.chooseLevel("pl.gdela");
		socomo.visualizeInto(output);

		// then
		assertThat(contentOf(output))
				.contains("'dummy-module'") // module name
				.contains("'pl.gdela'") // chosen level
				.contains("socomo"); // the only component at this level
	}

	@Test
	public void format_of_component_and_dependencies() {
		// when
		socomo.analyzeBytecode(input);
		socomo.chooseLevel("pl.gdela.socomo");
		socomo.visualizeInto(output);

		// then
		assertThat(contentOf(output)).as("format of a component")
				.containsPattern("'bytecode'\\s*:\\{ size: \\d.\\d \\},");

		assertThat(contentOf(output)).as("format of a dependency")
				.containsPattern("'bytecode -> codemap'\\s*:\\{ strength: \\d\\.\\d \\},");
	}
}
