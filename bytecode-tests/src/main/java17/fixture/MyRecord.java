package fixture;

import java.util.Set;

import fixture.Targets.Alfa;
import fixture.Targets.ColorEnum;
import fixture.Targets.ShapeAnnotation;

public record MyRecord(
	@ShapeAnnotation(stringValue = "dummy", enumValue = ColorEnum.GREEN)
	Set<Alfa> myRecordComponent
) {
	// noop
}
