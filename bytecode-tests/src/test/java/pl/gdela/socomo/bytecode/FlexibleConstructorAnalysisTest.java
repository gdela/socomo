package pl.gdela.socomo.bytecode;

import java.io.File;
import java.util.Objects;

import fixture.MyFlexibleConstructor;
import org.junit.Test;

import static pl.gdela.socomo.codemap.DepType.CALLS;
import static pl.gdela.socomo.codemap.DepType.HAS_PARAM;
import static pl.gdela.socomo.codemap.DepType.REFERENCES;

public class FlexibleConstructorAnalysisTest extends BytecodeAnalyzerTestBase {

	public FlexibleConstructorAnalysisTest() {
		super(new File("target/classes/fixture/"));
	}

	FlexibleConstructorAnalysisTest(File fixtureRoot) {
		super(fixtureRoot);
	}

	@Test
	public void simple_main() {
		analyzing("MyFlexibleConstructor.class");
		expectForSource(MyFlexibleConstructor.class, "<init>()")
			.target(HAS_PARAM, Long.class)
			.target(REFERENCES, Long.class)
			.target(CALLS, Objects.class, "requireNonNull()")
			.target(CALLS, Object.class, "<init>()");
	}

}
