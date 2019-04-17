package pl.gdela.socomo.bytecode;

import java.io.File;
import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import fixture.MyFields;
import fixture.MyGenericClass;
import fixture.MyMethodBody;
import fixture.MyMethods;
import fixture.MyRegularClass;
import fixture.Targets;
import fixture.Targets.Alfa;
import fixture.Targets.Beta;
import fixture.Targets.BlaException;
import fixture.Targets.ColorEnum;
import fixture.Targets.Delta;
import fixture.Targets.Epsilon;
import fixture.Targets.FooException;
import fixture.Targets.Gamma;
import fixture.Targets.GenericAlfa;
import fixture.Targets.ShapeAnnotation;
import fixture.Targets._Base;
import fixture.Targets._GenericBase;
import org.junit.Ignore;
import org.junit.Test;

import static pl.gdela.socomo.codemap.DepType.*;

/**
 * The tests for analyzing bytecode produced for Java 7 runtime. Mind that probably your IDE
 * won't compile fixture classes to the {@code target/classes_java8} folder by itself,
 * you need to run {@code mvn compile} to have them compiled by maven as defined in the
 * {@code pom.xml} file of this module.
 */
@SuppressWarnings("OverlyCoupledClass")
public class Java7TestSuite extends BytecodeAnalyzerTestBase {

	public Java7TestSuite() {
		super(new File("target/classes_java7/fixture/"));
	}

	Java7TestSuite(File file) {
		super(file);
	}

	@Test
	public void regular_inner_class() {
		analyzing("MyRegularClass$MyInnerClass.class");
		expectForSource(MyRegularClass.MyInnerClass.class)
				.target(EXTENDS, _Base.class)
				.target(IMPLEMENTS, Serializable.class);
	}

	@Test
	public void generic_class() {
		analyzing("MyGenericClass.class");
		expectForSource(MyGenericClass.class)
				.target(TYPE_PARAM, Alfa.class)
				.target(EXTENDS, HashSet.class)
				.target(TYPE_PARAM, Beta.class)
				.target(IMPLEMENTS, Comparable.class)
				.target(TYPE_PARAM, Gamma.class);
	}

	@Test
	public void generic_inner_class() {
		analyzing("MyGenericClass$MyInnerClass.class");
		expectForSource(MyGenericClass.MyInnerClass.class)
				.target(TYPE_PARAM, Delta.class)
				.target(EXTENDS, Object.class)
				.target(IMPLEMENTS, Serializable.class);
	}

	@Test
	public void class_default_constructor() {
		analyzing("MyRegularClass.class");
		expectForSource(MyRegularClass.class, "<init>()")
				.target(CALLS, _Base.class, "<init>()");
	}

	@Test
	public void inner_class_default_constructor() {
		analyzing("MyRegularClass$MyInnerClass.class");
		expectForSource(MyRegularClass.MyInnerClass.class, "<init>()")
				.target(HAS_PARAM, MyRegularClass.class)
				.target(CALLS, _Base.class, "<init>()");
	}

	@Test
	public void regular_field() {
		analyzing("MyFields.class");
		expectForSource(MyFields.class, "myRegularField")
				.target(IS_OF_TYPE, Alfa.class)
				.target(ANNOTATED, ShapeAnnotation.class)
				.target(ANNOTATION_VALUE, ColorEnum.class, "GREEN");
	}

	@Test
	public void generic_field() {
		analyzing("MyFields.class");
		expectForSource(MyFields.class, "myGenericField")
				.target(IS_OF_TYPE, Set.class)
				.target(TYPE_PARAM, _Base.class);
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
				.noTargets();
	}

	@Test
	public void array_field() {
		analyzing("MyFields.class");
		expectForSource(MyFields.class, "myArrayField")
				.target(IS_OF_TYPE, Float.class);
	}

	@Ignore // FIXME: fix method_with_body test for various Java versions
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
				.target(CALLS, Object.class, "getClass()") // dunno why

				// primitive field init
				.target(READS_WRITES, MyFields.class, "myPrimitiveField")

				// array field init using array initializer
				.target(READS_WRITES, MyFields.class, "myArrayField")
				.target(CREATES_ARRAY, Float.class)
				.target(CALLS, Float.class, "valueOf()")
		;
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
				.target(ANNOTATION_VALUE, ColorEnum.class, "BLACK");
	}

	@Test
	public void generic_method() {
		analyzing("MyMethods.class");
		expectForSource(MyMethods.class, "myGenericMethod()")
				.target(TYPE_PARAM, Alfa.class)
				.target(RETURNS, Set.class)
				.target(TYPE_PARAM, Beta.class)
				.target(HAS_PARAM, List.class)
				.target(TYPE_PARAM, Gamma.class);
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
				.noTargets();
	}

	@Ignore // FIXME: fix method_with_body test for various Java versions
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

	// todo: think about refactoring bytecode analyzer tests
	// - maybe gather all field-related test into one fields() test method and rework the mock to do a complete verification of all dependencies
	// - and/or split bytecode analyzer test into sever test: ClassAnalysisTest, FieldAnalysisTest, MethodSignatureAnalysisTest, MethodBodyAnalysisTest
}
