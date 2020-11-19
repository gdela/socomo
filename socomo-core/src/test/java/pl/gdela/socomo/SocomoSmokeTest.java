package pl.gdela.socomo;

import java.io.File;

import org.junit.Test;

import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;
import static net.javacrumbs.jsonunit.jsonpath.JsonPathAdapter.inPath;
import static org.apache.commons.lang3.StringUtils.substringAfter;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.contentOf;

/**
 * Smoke test of the whole socomo-core module integrated together.
 */
public class SocomoSmokeTest {

	private final SocomoFacade socomo = new SocomoFacade("dummy-module");
	private final File input = new File("target/classes");
	private final File outputHtml = new File("target/socomo-smoke-test-output.html");
	private final File outputData = new File("target/socomo-smoke-test-output.data");

	@Test
	public void basic_usage() {
		// when
		socomo.analyzeBytecode(input);
		socomo.guessLevel();
		socomo.visualizeInto(outputHtml, outputData);

		// then
		assertThat(outputHtml).exists();
		assertThat(contentOf(outputHtml))
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
		socomo.visualizeInto(outputHtml, outputData);

		// then
		assertThat(contentOf(outputHtml))
				.contains("'dummy-module'") // module name
				.contains("'pl.gdela'") // chosen level
				.contains("socomo"); // the only component at this level
	}

	@Test
	public void format_of_component_and_dependencies() {
		// when
		socomo.analyzeBytecode(input);
		socomo.chooseLevel("pl.gdela.socomo");
		socomo.visualizeInto(outputHtml, outputData);

		// then
		assertThat(contentOf(outputHtml)).as("format of a component")
				.containsPattern("'bytecode'\\s*:\\{ share: \\s?\\d+ \\},");

		assertThat(contentOf(outputHtml)).as("format of a dependency")
				.containsPattern("'bytecode -> codemap'\\s*:\\{ strength: \\d \\},");
	}

	@Test
	public void data_file_with_details() {
		// when
		socomo.analyzeBytecode(input);
		socomo.chooseLevel("pl.gdela.socomo");
		socomo.visualizeInto(outputHtml, outputData);

		// then
		assertThat(outputData).exists();
		String json = substringAfter(contentOf(outputData), "codemap = ");

		assertThatJson(inPath(json, "$.packageDeps"))
			.isArray()
			.isNotEmpty();

		assertThatJson(inPath(json, "$.packageDeps[?(@.from.fqn == 'pl.gdela.socomo.visualizer' && @.to.fqn == 'pl.gdela.socomo.composition')].memberDeps"))
			.isArray()
			.isNotEmpty();
	}
}
