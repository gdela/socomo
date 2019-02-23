package fixture;

import java.io.Serializable;

import fixture.Targets.ColorEnum;
import fixture.Targets.ShapeAnnotation;
import fixture.Targets._Base;

@SuppressWarnings("all")
@ShapeAnnotation(
		stringValue = "dummy",
		enumValue = ColorEnum.BLUE,
		arrayValue = { 1, 2, 3 }
)
public abstract class MyRegularClass
		extends _Base
		implements Comparable {

	public abstract class MyInnerClass
			extends _Base
			implements Serializable {
	}

}
