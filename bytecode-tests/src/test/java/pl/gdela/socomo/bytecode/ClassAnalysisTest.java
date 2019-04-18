package pl.gdela.socomo.bytecode;

import java.io.File;
import java.io.Serializable;
import java.util.HashSet;

import fixture.MyGenericClass;
import fixture.MyRegularClass;
import fixture.Targets;
import org.junit.Test;

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
	public void regular_inner_class() {
		analyzing("MyRegularClass$MyInnerClass.class");
		expectForSource(MyRegularClass.MyInnerClass.class)
			.target(EXTENDS, Targets._Base.class)
			.target(IMPLEMENTS, Serializable.class)
		;
	}

	@Test
	public void generic_class() {
		analyzing("MyGenericClass.class");
		expectForSource(MyGenericClass.class)
			.target(TYPE_PARAM, Targets.Alfa.class)
			.target(EXTENDS, HashSet.class)
			.target(TYPE_PARAM, Targets.Beta.class)
			.target(IMPLEMENTS, Comparable.class)
			.target(TYPE_PARAM, Targets.Gamma.class)
		;
	}

	@Test
	public void generic_inner_class() {
		analyzing("MyGenericClass$MyInnerClass.class");
		expectForSource(MyGenericClass.MyInnerClass.class)
			.target(TYPE_PARAM, Targets.Delta.class)
			.target(EXTENDS, Object.class)
			.target(IMPLEMENTS, Serializable.class)
		;
	}

	@Test
	public void class_default_constructor() {
		analyzing("MyRegularClass.class");
		expectForSource(MyRegularClass.class, "<init>()")
			.target(CALLS, Targets._Base.class, "<init>()")
		;
	}

	@Test
	public void inner_class_default_constructor() {
		analyzing("MyRegularClass$MyInnerClass.class");
		expectForSource(MyRegularClass.MyInnerClass.class, "<init>()")
			.target(HAS_PARAM, MyRegularClass.class)
			.target(CALLS, Targets._Base.class, "<init>()")
		;
	}

}
