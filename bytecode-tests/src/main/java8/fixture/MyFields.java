package fixture;

import java.util.HashSet;
import java.util.Set;

import fixture.Targets.Alfa;
import fixture.Targets.Beta;
import fixture.Targets.ColorEnum;
import fixture.Targets.GenericAlfa;
import fixture.Targets.ShapeAnnotation;
import fixture.Targets._Base;

@SuppressWarnings("all")
public abstract class MyFields {

	@ShapeAnnotation(stringValue = "dummy", enumValue = ColorEnum.GREEN)
	private Alfa myRegularField
			= Targets.createAlfa();

	private Set<? extends _Base> myGenericField
			= new HashSet<Beta>();

	private GenericAlfa<Alfa>.GenericInner<Beta> myDeeplyGenericField
			= new GenericAlfa<Alfa>().new GenericInner<Beta>();

	private int myPrimitiveField
			= 456;

	private Float[][] myArrayField
			= { { 1.1f, 2.2f }, { 3.3f, 4.4f } };
}
