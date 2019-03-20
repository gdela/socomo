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

import static pl.gdela.socomo.codemap.Origin.MAIN;

/**
 * Facade for easy invocation of whole socomo flow, from bytecode analysis up to building dependency visualiser.
 */
public class SocomoFacade {
	private static final Logger log = LoggerFactory.getLogger(SocomoFacade.class);

	private final Module module;
	private Codemap codemap;
	private Level level;

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
		codemap = collector.getCodemap().select(MAIN);
		log.trace("codemap:\n{}", codemap.formatted());
	}

	/**
	 * Guesses what will be the most interesting level.
	 * @see LevelGuesser
	 */
	public void guessLevel() {
		String levelName = LevelGuesser.guessLevel(codemap);
		level = CodemapToLevel.transform(codemap, levelName);
		log.debug("level {}:\n{}", level.name, level.formatted());
	}

	/**
	 * Builds the composition visualizer into given file.
	 * @param file the output file, typically named {@code socomo.html}
	 */
	public void visualizeInto(File file) {
		VisualizerBuilder builder = new VisualizerBuilder();
		builder.setModule(module);
		builder.setLevel(level);
		builder.buildInto(shorten(file));
	}

	private static File shorten(File location) {
		Path currentWorkingDirectory = Paths.get("").toAbsolutePath();
		Path target = location.toPath().toAbsolutePath();
		return currentWorkingDirectory.relativize(target).toFile();
	}
}
