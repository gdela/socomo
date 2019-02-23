package fixture;

import java.util.List;
import java.util.Set;

import fixture.Targets.Alfa;
import fixture.Targets.Beta;
import fixture.Targets.BlaException;
import fixture.Targets.ColorEnum;
import fixture.Targets.FooException;
import fixture.Targets.Gamma;
import fixture.Targets.GenericAlfa;
import fixture.Targets.ShapeAnnotation;

@SuppressWarnings("all")
public abstract class MyMethods {

	@ShapeAnnotation(stringValue = "dummy", enumValue = ColorEnum.BLACK)
	abstract Alfa myRegularMethod
			(Beta myParam1, Gamma myParam2)
			throws FooException, BlaException;

	abstract <T extends Alfa> Set<? extends Beta> myGenericMethod
			(T myParam1, List<? extends Gamma> myParam2);

	abstract GenericAlfa<Alfa>.GenericInner<Beta> myDeeplyGenericMethod
			();

	abstract int myPrimitiveMethod
			(float myParam1, boolean myParam2);

}
