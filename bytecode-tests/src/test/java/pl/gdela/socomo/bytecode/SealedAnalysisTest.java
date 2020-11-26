package pl.gdela.socomo.bytecode;

import java.io.File;

import fixture.MyCircle;
import fixture.MySealedClass;
import fixture.MySquare;
import org.junit.Test;

import static pl.gdela.socomo.codemap.DepType.EXTENDS;
import static pl.gdela.socomo.codemap.DepType.PERMITS;

public class SealedAnalysisTest extends BytecodeAnalyzerTestBase {

	public SealedAnalysisTest() {
		super(new File("target/classes/fixture/"));
	}

	SealedAnalysisTest(File fixtureRoot) {
		super(fixtureRoot);
	}

	@Test
	public void sealed_class() {
		analyzing("MySealedClass.class");
		expectForSource(MySealedClass.class)
			.target(EXTENDS, Object.class)
			.target(PERMITS, MyCircle.class)
			.target(PERMITS, MySquare.class);
	}

}
