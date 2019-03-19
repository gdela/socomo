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

import static java.awt.Desktop.getDesktop;
import static java.lang.System.err;
import static java.lang.System.exit;
import static org.apache.commons.lang3.StringUtils.firstNonBlank;

/**
 * Main class to execute SoCoMo analysis. This is rarely used, rather the maven/gradle plugins are used.
 */
@Command(
		name = "socomo",
		description = "Analyzes source code composition of a java project.",
		mixinStandardHelpOptions = true,
		versionProvider = Socomo.ManifestVersionProvider.class
)
@SuppressWarnings("UseOfSystemOutOrSystemErr")
class Socomo implements Callable<Void> {

	private static final Logger log = LoggerFactory.getLogger(Socomo.class);

	@Parameters(
			paramLabel = "INPUT",
			index = "0", arity = "1",
			description = "a *.jar file or directory with *.class files to be analyzed"
	)
	private File input;

	@Option(
			names = { "-o", "--output" },
			paramLabel = "DIR",
			description = "the directory to which socomo.html will be written",
			defaultValue = "."
	)
	private File output;

	@Option(
			names = { "-d", "--display" },
			description = "automatically opens created socomo.html file in the browser"
	)
	private boolean display;

	public static void main(String[] args) {
		CommandLine.call(new Socomo(), args);
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

		File socomoTargetFile = new File(output.getCanonicalFile(), "socomo.html");
		SocomoFacade socomo = new SocomoFacade(input.toString().replace('\\', '/'));
		socomo.analyzeBytecode(input);
		socomo.guessLevel();
		socomo.visualizeInto(socomoTargetFile);

		if (display) {
			try {
				getDesktop().browse(socomoTargetFile.toURI());
			} catch (Exception e) {
				log.warn("cannot open your browser, {}", e.getMessage());
				log.warn("open this file yourself: {}", socomoTargetFile);
			}
		}
		return null;
	}

	static class ManifestVersionProvider implements IVersionProvider {
		@Override
		public String[] getVersion() {
			String version = Socomo.class.getPackage().getImplementationVersion();
			version = firstNonBlank(version, "unknown");
			return new String[] { version };
		}
	}
}
