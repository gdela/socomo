package pl.gdela.socomo.bytecode;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import fixture.MyFields;
import fixture.Targets;
import fixture.Targets.Alfa;
import fixture.Targets.Beta;
import fixture.Targets.ColorEnum;
import fixture.Targets.GenericAlfa;
import fixture.Targets.ShapeAnnotation;
import fixture.Targets._Base;
import fixture.Targets._GenericBase;
import org.junit.Test;

import static pl.gdela.socomo.codemap.DepType.ANNOTATED;
import static pl.gdela.socomo.codemap.DepType.ANNOTATION_VALUE;
import static pl.gdela.socomo.codemap.DepType.CALLS;
import static pl.gdela.socomo.codemap.DepType.CREATES;
import static pl.gdela.socomo.codemap.DepType.CREATES_ARRAY;
import static pl.gdela.socomo.codemap.DepType.IS_OF_TYPE;
import static pl.gdela.socomo.codemap.DepType.READS_WRITES;
import static pl.gdela.socomo.codemap.DepType.TYPE_PARAM;

public class FieldAnalysisTest extends BytecodeAnalyzerTestBase {

	public FieldAnalysisTest() {
		super(new File("target/classes/fixture/"));
	}

	FieldAnalysisTest(File fixtureRoot) {
		super(fixtureRoot);
	}

	@Test
	public void regular_field() {
		analyzing("MyFields.class");
		expectForSource(MyFields.class, "myRegularField")
			.target(IS_OF_TYPE, Alfa.class)
			.target(ANNOTATED, ShapeAnnotation.class)
			.target(ANNOTATION_VALUE, ColorEnum.class, "GREEN")
		;
	}

	@Test
	public void generic_field() {
		analyzing("MyFields.class");
		expectForSource(MyFields.class, "myGenericField")
			.target(IS_OF_TYPE, Set.class)
			.target(TYPE_PARAM, _Base.class)
		;
	}

	@Test
	public void deeply_generic_field() {
		analyzing("MyFields.class");
		expectForSource(MyFields.class, "myDeeplyGenericField")
			.target(IS_OF_TYPE, _GenericBase.GenericInner.class)
			.target(TYPE_PARAM, Alfa.class)
			.target(TYPE_PARAM, Beta.class)
		;
	}

	@Test
	public void primitive_field() {
		analyzing("MyFields.class");
		expectForSource(MyFields.class, "myPrimitiveField")
			.noTargets()
		;
	}

	@Test
	public void array_field() {
		analyzing("MyFields.class");
		expectForSource(MyFields.class, "myArrayField")
			.target(IS_OF_TYPE, Float.class)
		;
	}

	@Test
	public void all_fields_init() {
		analyzing("MyFields.class");
		expectForSource(MyFields.class, "<init>()")
			// common
			.target(CALLS, Object.class, "<init>()")

			// regular field init using static method
			.target(READS_WRITES, MyFields.class, "myRegularField")
			.target(CALLS, Targets.class, "createAlfa()")

			// generic field init creating new object
			.target(READS_WRITES, MyFields.class, "myGenericField")
			.target(CREATES, HashSet.class)
			.target(CALLS, HashSet.class, "<init>()")

			// deeply generic field init creating new objects
			.target(READS_WRITES, MyFields.class, "myDeeplyGenericField")
			.target(CREATES, GenericAlfa.class)
			.target(CALLS, GenericAlfa.class, "<init>()")
			.target(CREATES, _GenericBase.GenericInner.class)
			.target(CALLS, _GenericBase.GenericInner.class, "<init>()")

			// primitive field init
			.target(READS_WRITES, MyFields.class, "myPrimitiveField")

			// array field init using array initializer
			.target(READS_WRITES, MyFields.class, "myArrayField")
			.target(CREATES_ARRAY, Float.class)
			.target(CALLS, Float.class, "valueOf()")
		;
	}

}
