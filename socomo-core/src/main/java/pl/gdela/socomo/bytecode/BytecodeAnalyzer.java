package pl.gdela.socomo.bytecode;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collection;

import org.apache.commons.io.filefilter.AndFileFilter;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.NameFileFilter;
import org.apache.commons.io.filefilter.NotFileFilter;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.commons.lang3.time.StopWatch;
import org.objectweb.asm.ClassReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.apache.commons.io.FileUtils.listFiles;
import static org.apache.commons.lang3.Validate.isTrue;

/**
 * Analyzes bytecode to discover source classes and members and their dependencies.
 */
public class BytecodeAnalyzer {
	private static final Logger log = LoggerFactory.getLogger(BytecodeAnalyzer.class);

	private final DependencyCollectorAdapter collector;

	/**
	 * Creates analyzer that will emit information to the given collector.
	 */
	public BytecodeAnalyzer(DependencyCollector collector) {
		this.collector = new DependencyCollectorAdapter(collector);
	}

	/**
	 * Analyzes single {@code *.class} file.
	 */
	public void analyze(File bytecodeFile) {
		log.trace("analyzing {}", bytecodeFile);
		try (FileInputStream input = new FileInputStream(bytecodeFile)) {
			new ClassReader(input).accept(new ClassVisitor(collector), 0);
		} catch (IOException e) {
			throw new IllegalArgumentException("cannot read file " + bytecodeFile, e);
		}
	}

	/**
	 * Analyzes collection of {@code *.class} files.
	 */
	public void analyze(Collection<File> bytecodeFiles) {
		log.info("analyzing {} bytecode files", bytecodeFiles.size());
		StopWatch stopWatch = StopWatch.createStarted();
		for (File file : bytecodeFiles) {
			analyze(file);
		}
		log.info("analysis took {} ms", stopWatch.getTime(MILLISECONDS));
	}

	/**
	 * Analyzes {@code *.class} files from a given directory and its subdirectories.
	 */
	public void analyzeDir(File dir) {
		log.info("analyzing {}", dir);
		isTrue(dir.exists(), "directory %s does not exists", dir);
		isTrue(dir.isDirectory(), "%s is a file not a directory", dir);
		analyze(bytecodeFiles(dir));
	}

	@SuppressWarnings("unchecked")
	private static Collection<File> bytecodeFiles(File dir) {
		IOFileFilter fileFilter = new AndFileFilter(
				new SuffixFileFilter(".class"),
				new NotFileFilter(new NameFileFilter("package-info.class")) // no dependencies in such file
		);
		IOFileFilter dirFilter = new NotFileFilter(new WildcardFileFilter("*-INF")); // META-INF, APP-INF, etc.
		return listFiles(dir, fileFilter, dirFilter);
	}
}
