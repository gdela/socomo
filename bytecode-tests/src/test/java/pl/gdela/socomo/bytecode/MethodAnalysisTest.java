package pl.gdela.socomo.bytecode;

import java.io.File;
import java.util.List;
import java.util.Set;

import fixture.MyMethodBody;
import fixture.MyMethods;
import fixture.Targets;
import org.junit.Test;

import static pl.gdela.socomo.codemap.DepType.*;
import static pl.gdela.socomo.codemap.DepType.CALLS;
import static pl.gdela.socomo.codemap.DepType.CASTS_TO;
import static pl.gdela.socomo.codemap.DepType.CATCHES;
import static pl.gdela.socomo.codemap.DepType.CREATES_ARRAY;
import static pl.gdela.socomo.codemap.DepType.READS_WRITES;
import static pl.gdela.socomo.codemap.DepType.REFERENCES;

public class MethodAnalysisTest extends BytecodeAnalyzerTestBase {

	public MethodAnalysisTest() {
		super(new File("target/classes/fixture/"));
	}

	MethodAnalysisTest(File fixtureRoot) {
		super(fixtureRoot);
	}

	@Test
	public void regular_method() {
		analyzing("MyMethods.class");
		expectForSource(MyMethods.class, "myRegularMethod()")
			.target(RETURNS, Targets.Alfa.class)
			.target(HAS_PARAM, Targets.Beta.class)
			.target(HAS_PARAM, Targets.Gamma.class)
			.target(THROWS, Targets.FooException.class)
			.target(THROWS, Targets.BlaException.class)
			.target(ANNOTATED, Targets.ShapeAnnotation.class)
			.target(ANNOTATION_VALUE, Targets.ColorEnum.class, "BLACK")
		;
	}

	@Test
	public void generic_method() {
		analyzing("MyMethods.class");
		expectForSource(MyMethods.class, "myGenericMethod()")
			.target(TYPE_PARAM, Targets.Alfa.class)
			.target(RETURNS, Set.class)
			.target(TYPE_PARAM, Targets.Beta.class)
			.target(HAS_PARAM, List.class)
			.target(TYPE_PARAM, Targets.Gamma.class)
		;
	}

	@Test
	public void deeply_generic_method() {
		analyzing("MyMethods.class");
		expectForSource(MyMethods.class, "myDeeplyGenericMethod()")
			.target(RETURNS, Targets._GenericBase.GenericInner.class)
			.target(TYPE_PARAM, Targets.Alfa.class)
			.target(TYPE_PARAM, Targets.Beta.class)
		;
	}

	@Test
	public void primitive_method() {
		analyzing("MyMethods.class");
		expectForSource(MyMethods.class, "myPrimitiveMethod()")
			.noTargets()
		;
	}

	@Test
	public void method_with_body() {
		analyzing("MyMethodBody.class");
		expectForSource(MyMethodBody.class, "myMethod()")
			// common
			.target(READS_WRITES, MyMethodBody.class, "dummy") // usage of own field
			.target(CALLS, MyMethodBody.class, "dummy()") // usage of own method

			// item 1 - local variables
			.target(REFERENCES, Targets.Alfa.class)

			// item 2 - static members usage
			.target(CALLS, Targets.Beta.class, "staticMethod()")
			.target(READS_WRITES, Targets.Beta.class, "staticField")

			// item 3 - new object of some type and instance members usage
			.target(CREATES, Targets.Gamma.class)
			.target(CALLS, Targets.Gamma.class, "<init>()")
			.target(CALLS, Targets.Gamma.class, "instanceMethod()")
			.target(READS_WRITES, Targets.Gamma.class, "instanceField")

			// item 4 - catch block
			.target(CATCHES, Targets.FooException.class)
			.target(REFERENCES, Targets.FooException.class)

			// item 5 - class constant/fields references
			.target(REFERENCES, Targets.Delta.class)
			.target(READS_WRITES, Integer.class, "TYPE")

			// item 6 - arrays
			.target(CREATES_ARRAY, Targets.Epsilon.class)
			.target(CASTS_TO, Targets.Epsilon.class)
		;
	}

}
