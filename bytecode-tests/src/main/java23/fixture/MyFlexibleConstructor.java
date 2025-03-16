package fixture;

import java.util.Objects;

@SuppressWarnings("all")
public class MyFlexibleConstructor {

	public MyFlexibleConstructor(Long value) {
		Objects.requireNonNull(value);
		super();
	}

}
