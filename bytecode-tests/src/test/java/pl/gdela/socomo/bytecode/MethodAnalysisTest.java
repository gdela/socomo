package pl.gdela.socomo.bytecode;

import java.io.File;
import java.util.List;
import java.util.Set;

import fixture.MyMethodBody;
import fixture.MyMethods;
import fixture.Targets.Alfa;
import fixture.Targets.Beta;
import fixture.Targets.BlaException;
import fixture.Targets.ColorEnum;
import fixture.Targets.Delta;
import fixture.Targets.Epsilon;
import fixture.Targets.FooException;
import fixture.Targets.Gamma;
import fixture.Targets.ShapeAnnotation;
import fixture.Targets._GenericBase;
import org.junit.Test;

import static pl.gdela.socomo.codemap.DepType.*;

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
			.target(RETURNS, Alfa.class)
			.target(HAS_PARAM, Beta.class)
			.target(HAS_PARAM, Gamma.class)
			.target(THROWS, FooException.class)
			.target(THROWS, BlaException.class)
			.target(ANNOTATED, ShapeAnnotation.class)
			.target(ANNOTATION_VALUE, ColorEnum.class, "BLACK")
		;
	}

	@Test
	public void generic_method() {
		analyzing("MyMethods.class");
		expectForSource(MyMethods.class, "myGenericMethod()")
			.target(TYPE_PARAM, Alfa.class)
			.target(RETURNS, Set.class)
			.target(TYPE_PARAM, Beta.class)
			.target(HAS_PARAM, List.class)
			.target(TYPE_PARAM, Gamma.class)
		;
	}

	@Test
	public void deeply_generic_method() {
		analyzing("MyMethods.class");
		expectForSource(MyMethods.class, "myDeeplyGenericMethod()")
			.target(RETURNS, _GenericBase.GenericInner.class)
			.target(TYPE_PARAM, Alfa.class)
			.target(TYPE_PARAM, Beta.class)
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
			.target(REFERENCES, Alfa.class)

			// item 2 - static members usage
			.target(CALLS, Beta.class, "staticMethod()")
			.target(READS_WRITES, Beta.class, "staticField")

			// item 3 - new object of some type and instance members usage
			.target(CREATES, Gamma.class)
			.target(CALLS, Gamma.class, "<init>()")
			.target(CALLS, Gamma.class, "instanceMethod()")
			.target(READS_WRITES, Gamma.class, "instanceField")

			// item 4 - catch block
			.target(CATCHES, FooException.class)
			.target(REFERENCES, FooException.class)

			// item 5 - class constant/fields references
			.target(REFERENCES, Delta.class)
			.target(READS_WRITES, Integer.class, "TYPE")

			// item 6 - arrays
			.target(CREATES_ARRAY, Epsilon.class)
			.target(CASTS_TO, Epsilon.class)
		;
	}

}
