package pl.gdela.socomo.bytecode;

import java.io.File;
import java.util.function.Consumer;
import java.util.stream.Stream;

import fixture.MyLambdas;
import fixture.Targets.Alfa;
import org.junit.Test;

import static pl.gdela.socomo.codemap.DepType.CALLS;
import static pl.gdela.socomo.codemap.DepType.CALLS_DYNAMIC;
import static pl.gdela.socomo.codemap.DepType.HAS_PARAM;
import static pl.gdela.socomo.codemap.DepType.READS_WRITES;
import static pl.gdela.socomo.codemap.DepType.REFERENCES;

public class LambdaAnalysisTest extends BytecodeAnalyzerTestBase {

	public LambdaAnalysisTest() {
		super(new File("target/classes/fixture/"));
	}

	LambdaAnalysisTest(File fixtureRoot) {
		super(fixtureRoot);
	}

	@Test
	public void lambda_decl() {
		analyzing("MyLambdas.class");
		expectForSource(MyLambdas.class, "simpleLambda()")
			.target(READS_WRITES, MyLambdas.class, "alfa")
			.target(CALLS, Stream.class, "of()")
			.target(CALLS, Stream.class, "forEach()")
			.target(CALLS_DYNAMIC, Consumer.class, "accept()")
		;
	}

	@Test
	public void lambda_impl() {
		analyzing("MyLambdas.class");
		expectForSource(MyLambdas.class, "lambda$simpleLambda$0()")
			.target(HAS_PARAM, Alfa.class)
			.target(REFERENCES, Alfa.class)
			.target(CALLS, Alfa.class, "instanceMethod()")
		;
	}

}
