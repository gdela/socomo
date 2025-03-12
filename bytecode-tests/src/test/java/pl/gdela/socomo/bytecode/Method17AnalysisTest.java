package pl.gdela.socomo.bytecode;

import java.io.File;
import java.math.BigDecimal;

import fixture.MyMethods17;
import org.junit.Test;

import static pl.gdela.socomo.codemap.DepType.CALLS;
import static pl.gdela.socomo.codemap.DepType.CASTS_TO;
import static pl.gdela.socomo.codemap.DepType.CREATES;
import static pl.gdela.socomo.codemap.DepType.HAS_PARAM;
import static pl.gdela.socomo.codemap.DepType.REFERENCES;
import static pl.gdela.socomo.codemap.DepType.RETURNS;

public class Method17AnalysisTest extends BytecodeAnalyzerTestBase {

	public Method17AnalysisTest() {
		super(new File("target/classes/fixture/"));
	}

	Method17AnalysisTest(File fixtureRoot) {
		super(fixtureRoot);
	}

	@Test
	public void switch_expression() {
		analyzing("MyMethods17.class");
		expectForSource(MyMethods17.class, "mySwitchExpression()")
			.target(RETURNS, Object.class)
			.target(REFERENCES, String.class)
			.target(HAS_PARAM, String.class)
			.target(CALLS, String.class, "hashCode()")
			.target(CALLS, String.class, "equals()")
			.target(CALLS, Integer.class, "parseInt()")
			.target(CALLS, Integer.class, "valueOf()")
			.target(CALLS, Long.class, "parseLong()")
			.target(CALLS, Long.class, "valueOf()")
			.target(CALLS, String.class, "contains()")
			.target(CREATES, BigDecimal.class)
			.target(CALLS, BigDecimal.class, "<init>()")
		;
	}

	@Test
	public void pattern_matching_for_instance_of() {
		analyzing("MyMethods17.class");
		expectForSource(MyMethods17.class, "myPatternMatchingForInstanceOf()")
			.target(REFERENCES, Object.class)
			.target(HAS_PARAM, Object.class)
			.target(REFERENCES, String.class)
			.target(CASTS_TO, String.class)
			.target(CALLS, String.class, "length()")
		;
	}

}
