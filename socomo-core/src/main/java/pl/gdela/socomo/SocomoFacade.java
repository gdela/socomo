package pl.gdela.socomo;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.gdela.socomo.bytecode.BytecodeAnalyzer;
import pl.gdela.socomo.codemap.Codemap;
import pl.gdela.socomo.codemap.CodemappingCollector;
import pl.gdela.socomo.composition.CodemapToLevel;
import pl.gdela.socomo.composition.Level;
import pl.gdela.socomo.composition.LevelGuesser;
import pl.gdela.socomo.composition.Module;
import pl.gdela.socomo.visualizer.VisualizerBuilder;

import static java.awt.Desktop.getDesktop;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.Validate.isTrue;
import static org.apache.commons.lang3.Validate.validState;
import static pl.gdela.socomo.codemap.Origin.MAIN;
import static pl.gdela.socomo.codemap.Selector.nonSelfDepsAndFrom;

/**
 * Facade for easy invocation of whole socomo flow, from bytecode analysis up to building dependency visualiser.
 */
public class SocomoFacade {
	private static final Logger log = LoggerFactory.getLogger(SocomoFacade.class);

	private final Module module;
	private Codemap codemap;
	private Level level;
	private File output;

	public SocomoFacade(String moduleName) {
		this.module = new Module(moduleName);
	}

	/**
	 * Analyzes bytecode found in given input.
	 * @see BytecodeAnalyzer
	 * @param input {@code *.jar} file or directory with {@code *.class} files
	 */
	public void analyzeBytecode(File input) {
		CodemappingCollector collector = new CodemappingCollector();
		BytecodeAnalyzer analyzer = new BytecodeAnalyzer(collector);
		if (input.isFile()) {
			analyzer.analyzeJar(shorten(input));
		} else if (input.isDirectory()) {
			analyzer.analyzeDir(shorten(input));
		} else {
			throw new IllegalArgumentException(input + " is neither jar not dir");
		}
		codemap = collector.getCodemap().select(nonSelfDepsAndFrom(MAIN));
		log.trace("codemap:\n{}", codemap.formatted());
	}

	/**
	 * Guesses what will be the most interesting level.
	 * @see LevelGuesser
	 */
	public void guessLevel() {
		validState(codemap != null);
		String levelName = LevelGuesser.guessLevel(codemap);
		level = CodemapToLevel.transform(codemap, levelName);
		log.debug("level {}:\n{}", level.name, level.formatted());
	}

	/**
	 * Choose level to be displayed on the diagram.
	 * @param levelName the name of the package
	 */
	public void chooseLevel(String levelName) {
		validState(codemap != null);
		level = CodemapToLevel.transform(codemap, levelName);
		isTrue(isNotEmpty(level.components), "no code found in chosen level '%s'", levelName);
		log.debug("level {}:\n{}", level.name, level.formatted());
	}

	/**
	 * Builds the composition visualizer into given file.
	 * @param file the output file, typically named {@code socomo.html}
	 */
	public void visualizeInto(File file) {
		validState(level != null);
		output = shorten(file);
		VisualizerBuilder builder = new VisualizerBuilder();
		builder.setModule(module);
		builder.setLevel(level);
		builder.buildInto(output);
	}

	/**
	 * Opens created visualizer file in the browser.
	 */
	public void display() {
		validState(output != null);
		try {
			getDesktop().browse(output.toURI());
		} catch (Exception e) {
			log.warn("cannot open your browser, {}", e.getMessage());
			log.warn("open this file yourself: {}", output);
		}
	}

	private static File shorten(File location) {
		Path currentWorkingDirectory = Paths.get("").toAbsolutePath();
		Path target = location.toPath().toAbsolutePath();
		return currentWorkingDirectory.relativize(target).toFile();
	}
}
