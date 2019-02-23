package pl.gdela.socomo;

import java.io.File;

import static java.lang.System.err;
import static org.apache.commons.lang3.Validate.isTrue;

/**
 * Main class to execute SoCoMo analysis. This is rarely used, rather the maven/gradle plugins are used.
 */
@SuppressWarnings("UseOfSystemOutOrSystemErr")
public final class Socomo {

	private Socomo() { /* no instances */ }

	public static void main(String[] args) {
		if (args.length < 2) {
			err.println("Usage: socomo <input-directory> <output-directory>");
			err.println("- bytecode from <input-directory> will be analyzed");
			err.println("- results will be written to files in <output-directory>");
			System.exit(-1);
		}
		File input = new File(args[0]);
		File output = new File(args[1]);
		isTrue(input.isDirectory(), "invalid <input-directory> %s, it is not a directory", input);
		isTrue(output.isDirectory(), "invalid <output-directory> %s, it is not a directory", output);

		SocomoFacade socomo = new SocomoFacade(input.toString().replace('\\', '/'));
		socomo.analyzeBytecode(input);
		socomo.guessLevel();
		socomo.visualizeInto(new File(output, "socomo.html"));
	}

}
