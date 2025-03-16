package pl.gdela.socomo.bytecode;

import java.io.File;
import java.math.BigDecimal;

import org.junit.Test;

import static pl.gdela.socomo.codemap.DepType.CALLS;
import static pl.gdela.socomo.codemap.DepType.CATCHES;
import static pl.gdela.socomo.codemap.DepType.CREATES;

public class SimpleMainAnalysisTest extends BytecodeAnalyzerTestBase {

	public SimpleMainAnalysisTest() {
		super(new File("target/classes/fixture/"));
	}

	SimpleMainAnalysisTest(File fixtureRoot) {
		super(fixtureRoot);
	}

	@Test
	public void simple_main() {
		analyzing("../MySimpleMain.class");
		expectForSource("MySimpleMain", "main()")
			.target(CREATES, BigDecimal.class)
			.target(CALLS, BigDecimal.class, "<init>()")
			.target(CATCHES, NumberFormatException.class);
	}

}
