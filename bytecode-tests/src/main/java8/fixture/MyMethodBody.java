package fixture;

import fixture.Targets.Alfa;
import fixture.Targets.Beta;
import fixture.Targets.Delta;
import fixture.Targets.Epsilon;
import fixture.Targets.FooException;
import fixture.Targets.Gamma;

@SuppressWarnings("all")
public abstract class MyMethodBody {

	private Object dummy;

	protected abstract Object dummy();

	private void myMethod() {

		// item 1 - local variables
		Alfa objectVariable = null;
		long primitiveVariable = 1;

		// item 2 - static members usage
		Beta.staticMethod(dummy);
		dummy = Beta.staticField;

		// item 3 - new object of some type and instance members usage
		new Gamma().instanceMethod(dummy);
		dummy = new Gamma().instanceField;

		// item 4 - catch block
		try { dummy(); } catch (FooException e) { dummy = e; } finally { };

		// item 5 - class constant/fields references
		dummy = Delta.class;
		dummy = Delta[].class;
		dummy = int.class;
		dummy = int[].class;

		// item 6 - arrays
		dummy = new Epsilon[5];
		dummy = new Epsilon[5][5].clone();
		dummy = (Epsilon[][]) dummy;
		dummy = new int[5];
		dummy = new int[5][5].clone();
		dummy = (int[][]) dummy;
	}
}
