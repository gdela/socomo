package pl.gdela.socomo;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

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
 * Main class to execute Socomo analysis. This is rarely used, rather the maven/gradle plugins are used.
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
			index = "0..*", arity = "1..*",
			description = "One or more *.jar files or directories with *.class files to be analyzed together."
	)
	private List<File> inputs;

	@Option(
			names = { "-o", "--output" },
			paramLabel = "DIR",
			description = "Directory to which socomo.html and socomo.data will be written.",
			defaultValue = "."
	)
	private File outputDir;

	@Option(
		names = { "--output-for-html" },
		paramLabel = "DIR",
		description = "Directory to which socomo.html will be written. Overwrites common '-o' option.",
		hidden = true
	)
	private File outputHtmlDir;

	@Option(
		names = { "--output-for-data" },
		paramLabel = "DIR",
		description = "Directory to which socomo.data will be written. Overwrites common '-o' option.",
		hidden = true
	)
	private File outputDataDir;

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

	@Option(
			names = { "--local-assets" },
			description = "Generate socomo.html to load all javascript/css dependencies from the same directory."
	)
	private boolean localAssets;

	public static void main(String[] args) {
		CommandLine.call(new SocomoMain(), args);
	}

	@Override
	public Void call() throws IOException {
		if (outputHtmlDir == null) outputHtmlDir = outputDir;
		if (!outputHtmlDir.isDirectory() || !outputHtmlDir.canWrite()) {
			err.println("Invalid output directory: " + outputHtmlDir);
			err.println("Please specify an existing, writable directory for html output.");
			CommandLine.usage(this, err);
			exit(1);
		}
		if (outputDataDir == null) outputDataDir = outputDir;
		if (!outputDataDir.isDirectory() || !outputDataDir.canWrite()) {
			err.println("Invalid output directory: " + outputDataDir);
			err.println("Please specify an existing, writable directory for data output.");
			CommandLine.usage(this, err);
			exit(1);
		}
		for (File input : inputs) {
			if (!input.canRead()) {
				err.println("Invalid input file/directory: " + input);
				err.println("Please specify existing, readable files or directories for input.");
				CommandLine.usage(this, err);
				exit(1);
			}
		}

		File outputHtmlFile = new File(outputHtmlDir.getCanonicalFile(), "socomo.html");
		File outputDataFile = new File(outputDataDir.getCanonicalFile(), "socomo.data");
		SocomoFacade socomo = new SocomoFacade(moduleName(inputs));
		socomo.analyzeBytecode(inputs);
		if (level != null) {
			socomo.chooseLevel(level);
		} else {
			socomo.guessLevel();
		}
		socomo.visualizeInto(outputHtmlFile, outputDataFile, localAssets);
		if (display) {
			socomo.display();
		}
		return null;
	}

	private static String moduleName(List<File> inputs) {
		return inputs.stream()
			.map(File::getName)
			.map(name -> name.replace('\\', '/'))
			.collect(Collectors.joining(" + "));
	}

	static class ManifestVersionProvider implements IVersionProvider {
		@Override
		public String[] getVersion() {
			return new String[] { "Socomo " + SocomoVersion.get() };
		}
	}
}
