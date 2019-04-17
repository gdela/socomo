package fixture;

import java.io.Serializable;
import java.util.HashSet;

import fixture.Targets.Alfa;
import fixture.Targets.Beta;
import fixture.Targets.Delta;
import fixture.Targets.Gamma;

@SuppressWarnings("all")
public abstract class MyGenericClass<T extends Alfa>
		extends HashSet<Beta>
		implements Comparable<Gamma> {

	public abstract class MyInnerClass<U extends Delta>
		implements Serializable {
	}
}
