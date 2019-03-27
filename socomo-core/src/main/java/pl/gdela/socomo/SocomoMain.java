package pl.gdela.socomo;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.IVersionProvider;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import pl.gdela.socomo.visualizer.SocomoVersion;

import static java.lang.System.err;
import static java.lang.System.exit;

/**
 * Main class to execute SoCoMo analysis. This is rarely used, rather the maven/gradle plugins are used.
 */
@Command(
		name = "java -jar socomo-standalone.jar",
		description = "Analyzes source code composition of a java project.",
		sortOptions = false,
		mixinStandardHelpOptions = true,
		versionProvider = SocomoMain.ManifestVersionProvider.class
)
@SuppressWarnings("UseOfSystemOutOrSystemErr")
class SocomoMain implements Callable<Void> {

	private static final Logger log = LoggerFactory.getLogger(SocomoMain.class);

	@Parameters(
			paramLabel = "INPUT",
			index = "0", arity = "1",
			description = "*.jar file or directory with *.class files to be analyzed."
	)
	private File input;

	@Option(
			names = { "-o", "--output" },
			paramLabel = "DIR",
			description = "Directory to which socomo.html will be written.",
			defaultValue = "."
	)
	private File output;

	@Option(
			names = { "-l", "--level" },
			paramLabel = "PACKAGE",
			description = "The level (name of a java package) which composition will be analyzed. If not specified, it will be guessed."
	)
	private String level;

	@Option(
			names = { "-d", "--display" },
			description = "Automatically open created socomo.html file in the browser."
	)
	private boolean display;

	public static void main(String[] args) {
		CommandLine.call(new SocomoMain(), args);
	}

	@Override
	public Void call() throws IOException {
		if (!output.isDirectory() || !output.canWrite()) {
			err.println("Invalid output directory: " + output);
			err.println("Please specify an existing, writable directory for output.");
			CommandLine.usage(this, err);
			exit(-1);
		}
		if (!input.canRead()) {
			err.println("Invalid input file/directory: " + input);
			err.println("Please specify an existing, readable file or directory for input.");
			CommandLine.usage(this, err);
			exit(-1);
		}

		File outputFile = new File(output.getCanonicalFile(), "socomo.html");
		SocomoFacade socomo = new SocomoFacade(input.toString().replace('\\', '/'));
		socomo.analyzeBytecode(input);
		if (level != null) {
			socomo.chooseLevel(level);
		} else {
			socomo.guessLevel();
		}
		socomo.visualizeInto(outputFile);
		if (display) {
			socomo.display();
		}
		return null;
	}

	static class ManifestVersionProvider implements IVersionProvider {
		@Override
		public String[] getVersion() {
			return new String[] { "Socomo " + SocomoVersion.get() };
		}
	}
}
