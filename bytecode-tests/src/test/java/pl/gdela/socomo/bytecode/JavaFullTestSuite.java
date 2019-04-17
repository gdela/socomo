package pl.gdela.socomo.bytecode;

import java.io.File;

/**
 * The tests for analyzing bytecode produced for highest supported Java runtime, as defined
 * in the {@code pom.xml} file of this module. Your IDE should also compile classes to the
 * {@code target/classes} automatically and on each file save.
 */
public class JavaFullTestSuite extends Java8TestSuite {

	public JavaFullTestSuite() {
		super(new File("target/classes/fixture/"));
	}

}
