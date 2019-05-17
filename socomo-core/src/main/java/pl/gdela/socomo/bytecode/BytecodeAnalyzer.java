package pl.gdela.socomo.bytecode;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

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

import static java.util.Collections.list;
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
	 * Analyzes all {@code *.class} files from a given {@code *.jar} archive.
	 */
	public void analyzeJar(File jar) {
		log.info("analyzing {}", jar);
		isTrue(jar.exists(), "%s does not exists", jar);
		isTrue(jar.isFile(), "%s is not a jar file", jar);
		StopWatch stopWatch = StopWatch.createStarted();
		try (ZipFile zip = new ZipFile(jar)) {
			List<ZipEntry> entries = new ArrayList<>();
			for (ZipEntry entry : list(zip.entries())) {
				if (isBytecode(entry)) entries.add(entry);
			}
			log.info("analyzing {} bytecode files", entries.size());
			for (ZipEntry entry : entries) {
				log.trace("analyzing {}", entry.getName());
				InputStream input = zip.getInputStream(entry);
				new ClassReader(input).accept(new ClassVisitor(collector), 0);
			}
		} catch (IOException e) {
			throw new IllegalArgumentException("cannot read file " + jar, e);
		}
		log.info("analysis took {} ms", stopWatch.getTime(MILLISECONDS));
	}

	/**
	 * Analyzes all {@code *.class} files from a given directory and its subdirectories.
	 */
	public void analyzeDir(File dir) {
		log.info("analyzing {}", dir);
		isTrue(dir.exists(), "directory %s does not exists", dir);
		isTrue(dir.isDirectory(), "%s is a file not a directory", dir);
		analyzeFiles(bytecodeFiles(dir));
	}

	/**
	 * Analyzes collection of {@code *.class} files.
	 */
	public void analyzeFiles(Collection<File> bytecodeFiles) {
		log.info("analyzing {} bytecode files", bytecodeFiles.size());
		StopWatch stopWatch = StopWatch.createStarted();
		for (File file : bytecodeFiles) {
			analyzeFile(file);
		}
		log.info("analysis took {} ms", stopWatch.getTime(MILLISECONDS));
	}

	/**
	 * Analyzes single {@code *.class} file.
	 */
	public void analyzeFile(File bytecodeFile) {
		log.trace("analyzing {}", bytecodeFile);
		isTrue(bytecodeFile.exists(), "%s does not exists", bytecodeFile);
		isTrue(bytecodeFile.isFile(), "%s is not a file", bytecodeFile);
		try (FileInputStream input = new FileInputStream(bytecodeFile)) {
			new ClassReader(input).accept(new ClassVisitor(collector), 0);
		} catch (IOException e) {
			throw new IllegalArgumentException("cannot read file " + bytecodeFile, e);
		}
	}

	private static boolean isBytecode(ZipEntry entry) {
		return entry.getName().endsWith(".class")
			&& !entry.getName().equals("package-info.class")
			&& !entry.getName().equals("module-info.class");
	}

	private static Collection<File> bytecodeFiles(File dir) {
		AndFileFilter fileFilter = new AndFileFilter();
		fileFilter.addFileFilter(new SuffixFileFilter(".class"));
		fileFilter.addFileFilter(new NotFileFilter(new NameFileFilter("package-info.class")));
		fileFilter.addFileFilter(new NotFileFilter(new NameFileFilter("module-info.class")));
		IOFileFilter dirFilter = new NotFileFilter(new WildcardFileFilter("*-INF")); // META-INF, APP-INF, etc.
		return listFiles(dir, fileFilter, dirFilter);
	}
}
