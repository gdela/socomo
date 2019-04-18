package pl.gdela.socomo.bytecode;

import java.io.File;
import java.util.function.Function;

import fixture.MyLambdas;
import org.junit.Test;

import static pl.gdela.socomo.codemap.DepType.CALLS;
import static pl.gdela.socomo.codemap.DepType.HAS_PARAM;
import static pl.gdela.socomo.codemap.DepType.REFERENCES;
import static pl.gdela.socomo.codemap.DepType.RETURNS;
import static pl.gdela.socomo.codemap.DepType.TYPE_PARAM;

/**
 * The tests for analyzing bytecode produced for Java 8 runtime. Mind that probably your IDE
 * won't compile fixture classes to the {@code target/classes_java8} folder by itself,
 * you need to run {@code mvn compile} to have them compiled by maven as defined in the
 * {@code pom.xml} file of this module.
 */
public class Java8TestSuite extends Java7TestSuite {

	public Java8TestSuite() {
		super(new File("target/classes_java8/fixture/"));
	}

	Java8TestSuite(File file) {
		super(file);
	}

	@Test
	public void lambda_factory() {
		analyzing("MyLambdas.class");
		expectForSource(MyLambdas.class, "simpleLambda()")
			.target(RETURNS, Function.class)
			.target(TYPE_PARAM, Integer.class)
			.target(TYPE_PARAM, Long.class);
	}

	@Test
	public void lambda_body() {
		analyzing("MyLambdas.class");
		expectForSource(MyLambdas.class, "lambda$simpleLambda$0()")
			.target(RETURNS, Long.class)
			.target(HAS_PARAM, Integer.class)
			.target(CALLS, Integer.class, "longValue()")
			.target(CALLS, Long.class, "valueOf()")
			.target(REFERENCES, Integer.class);
	}
}
