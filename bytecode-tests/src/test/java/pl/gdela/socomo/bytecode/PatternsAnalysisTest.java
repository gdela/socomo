package pl.gdela.socomo.bytecode;

import java.io.File;
import java.util.Objects;

import fixture.MyPatterns;
import fixture.Path;
import fixture.Position;
import fixture.Position2D;
import fixture.Position3D;
import org.junit.Test;

import static pl.gdela.socomo.codemap.DepType.CALLS;
import static pl.gdela.socomo.codemap.DepType.CALLS_DYNAMIC;
import static pl.gdela.socomo.codemap.DepType.CASTS_TO;
import static pl.gdela.socomo.codemap.DepType.CATCHES;
import static pl.gdela.socomo.codemap.DepType.CREATES;
import static pl.gdela.socomo.codemap.DepType.HAS_PARAM;
import static pl.gdela.socomo.codemap.DepType.REFERENCES;
import static pl.gdela.socomo.codemap.DepType.RETURNS;

public class PatternsAnalysisTest extends BytecodeAnalyzerTestBase {

	public PatternsAnalysisTest() {
		super(new File("target/classes/fixture/"));
	}

	PatternsAnalysisTest(File fixtureRoot) {
		super(fixtureRoot);
	}

	@Test
	public void pattern_matching() {
		analyzing("MyPatterns.class");
		expectForSource(MyPatterns.class, "describe()")
			// signature
			.target(RETURNS, String.class)
			.target(HAS_PARAM, Object.class)
			// hidden boilerplate
			.target(CATCHES, Throwable.class)
			.target(CALLS, Throwable.class, "toString()")
			.target(CALLS, Objects.class, "requireNonNull()")
			.target(CALLS_DYNAMIC, String.class, "makeConcatWithConstants()")
			.target(CREATES, MatchException.class)
			.target(CALLS, MatchException.class, "<init>()")
			// branch in switch expression
			.target(CASTS_TO, String.class)
			.target(CALLS, String.class, "length()")
			.target(CALLS, String.class, "substring()")
			// branch in switch expression
			.target(CASTS_TO, Position2D.class)
			.target(CALLS, Position2D.class, "x()")
			.target(CALLS, Position2D.class, "y()")
			// branch in switch expression
			.target(CASTS_TO, Position3D.class)
			.target(CALLS, Position3D.class, "x()")
			.target(CALLS, Position3D.class, "y()")
			.target(CALLS, Position3D.class, "z()")
			// branch in switch expression
			.target(CASTS_TO, Path.class)
			.target(CALLS, Path.class, "from()")
			.target(CALLS, Path.class, "to()")
			.target(REFERENCES, Position.class)
			.target(CALLS, MyPatterns.class, "describe()")
			// branch in switch expression
			.target(CALLS, Object.class, "toString()")
			// other
			.target(REFERENCES, String.class)
			.target(REFERENCES, Object.class)
		;
	}

}
