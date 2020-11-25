package pl.gdela.socomo.bytecode;

import java.io.File;
import java.util.Set;

import fixture.MyRecord;
import fixture.Targets;
import fixture.Targets.Alfa;
import org.junit.Test;

import static pl.gdela.socomo.codemap.DepType.ANNOTATED;
import static pl.gdela.socomo.codemap.DepType.ANNOTATION_VALUE;
import static pl.gdela.socomo.codemap.DepType.IS_OF_TYPE;
import static pl.gdela.socomo.codemap.DepType.READS_WRITES;
import static pl.gdela.socomo.codemap.DepType.RETURNS;
import static pl.gdela.socomo.codemap.DepType.TYPE_PARAM;

public class RecordAnalysisTest extends BytecodeAnalyzerTestBase {

	public RecordAnalysisTest() {
		super(new File("target/classes/fixture/"));
	}

	RecordAnalysisTest(File fixtureRoot) {
		super(fixtureRoot);
	}

	@Test
	public void record_component_field() {
		analyzing("MyRecord.class");
		expectForSource(MyRecord.class, "myRecordComponent")
			.target(IS_OF_TYPE, Set.class)
			.target(TYPE_PARAM, Alfa.class)
			.target(ANNOTATED, Targets.ShapeAnnotation.class)
			.target(ANNOTATION_VALUE, Targets.ColorEnum.class, "GREEN")
		;
	}

	@Test
	public void record_component_method() {
		analyzing("MyRecord.class");
		expectForSource(MyRecord.class, "myRecordComponent()")
			.target(READS_WRITES, MyRecord.class, "myRecordComponent")
			.target(RETURNS, Set.class)
			.target(TYPE_PARAM, Alfa.class)
			.target(ANNOTATED, Targets.ShapeAnnotation.class)
			.target(ANNOTATION_VALUE, Targets.ColorEnum.class, "GREEN")
		;
	}

}
