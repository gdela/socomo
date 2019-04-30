package pl.gdela.socomo.bytecode;

import java.io.File;
import java.io.Serializable;
import java.util.HashSet;

import fixture.MyGenericClass;
import fixture.MyRegularClass;
import fixture.Targets.Alfa;
import fixture.Targets.Beta;
import fixture.Targets.ColorEnum;
import fixture.Targets.Delta;
import fixture.Targets.Gamma;
import fixture.Targets.ShapeAnnotation;
import fixture.Targets._Base;
import org.junit.Test;

import static pl.gdela.socomo.codemap.DepType.ANNOTATED;
import static pl.gdela.socomo.codemap.DepType.ANNOTATION_VALUE;
import static pl.gdela.socomo.codemap.DepType.CALLS;
import static pl.gdela.socomo.codemap.DepType.EXTENDS;
import static pl.gdela.socomo.codemap.DepType.HAS_PARAM;
import static pl.gdela.socomo.codemap.DepType.IMPLEMENTS;
import static pl.gdela.socomo.codemap.DepType.TYPE_PARAM;

public class ClassAnalysisTest extends BytecodeAnalyzerTestBase {

	public ClassAnalysisTest() {
		super(new File("target/classes/fixture/"));
	}

	ClassAnalysisTest(File fixtureRoot) {
		super(fixtureRoot);
	}

	@Test
	public void regular_class() {
		analyzing("MyRegularClass.class");
		expectForSource(MyRegularClass.class)
			.target(EXTENDS, _Base.class)
			.target(IMPLEMENTS, Comparable.class)
			.target(ANNOTATED, ShapeAnnotation.class)
			.target(ANNOTATION_VALUE, ColorEnum.class, "BLUE");
	}

	@Test
	public void regular_inner_class() {
		analyzing("MyRegularClass$MyInnerClass.class");
		expectForSource(MyRegularClass.MyInnerClass.class)
			.target(EXTENDS, _Base.class)
			.target(IMPLEMENTS, Serializable.class)
		;
	}

	@Test
	public void generic_class() {
		analyzing("MyGenericClass.class");
		expectForSource(MyGenericClass.class)
			.target(TYPE_PARAM, Alfa.class)
			.target(EXTENDS, HashSet.class)
			.target(TYPE_PARAM, Beta.class)
			.target(IMPLEMENTS, Comparable.class)
			.target(TYPE_PARAM, Gamma.class)
		;
	}

	@Test
	public void generic_inner_class() {
		analyzing("MyGenericClass$MyInnerClass.class");
		expectForSource(MyGenericClass.MyInnerClass.class)
			.target(TYPE_PARAM, Delta.class)
			.target(EXTENDS, Object.class)
			.target(IMPLEMENTS, Serializable.class)
		;
	}

	@Test
	public void class_default_constructor() {
		analyzing("MyRegularClass.class");
		expectForSource(MyRegularClass.class, "<init>()")
			.target(CALLS, _Base.class, "<init>()")
		;
	}

	@Test
	public void inner_class_default_constructor() {
		analyzing("MyRegularClass$MyInnerClass.class");
		expectForSource(MyRegularClass.MyInnerClass.class, "<init>()")
			.target(HAS_PARAM, MyRegularClass.class)
			.target(CALLS, _Base.class, "<init>()")
		;
	}

}
