package pl.gdela.socomo.bytecode;

import java.io.File;

import org.junit.After;
import org.junit.Before;

import static org.apache.commons.lang3.Validate.notBlank;
import static org.apache.commons.lang3.Validate.notNull;
import static org.apache.commons.lang3.Validate.validState;

/**
 * Base for building bytecode analyzer tests. The test methods just have to define expectations
 * on the return value of {@code expectForSource()}, the actual verification will take place
 * in the common method {@link #analyzeAndVerify()} invoked automatically after each test.
 */
@SuppressWarnings("AbstractClassWithoutAbstractMethods")
public abstract class BytecodeAnalyzerTestBase {

	private final File fixtureRoot;
	private String bytecodeFile;
	private DependencyCollectorMock collector;

	BytecodeAnalyzerTestBase(File fixtureRoot) {
		this.fixtureRoot = notNull(fixtureRoot);
	}

	@Before
	public void createCollector() {
		collector = new DependencyCollectorMock();
	}

	void analyzing(String fileName) {
		bytecodeFile = notBlank(fileName);
	}

	DependencyCollectorMock expectForSource(Class sourceClass) {
		return collector.expectForSource(sourceClass);
	}

	DependencyCollectorMock expectForSource(Class sourceClass, String sourceMember) {
		return collector.expectForSource(sourceClass, sourceMember);
	}

	@After
	public void analyzeAndVerify() {
		validState(bytecodeFile != null, "the file to be analyzed was not specified");
		new BytecodeAnalyzer(collector).analyzeFile(new File(fixtureRoot, bytecodeFile));
		collector.verify();
	}
}
