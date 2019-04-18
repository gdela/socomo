package fixture;

import java.util.function.Function;

@SuppressWarnings("all")
public class MyLambdas {

	Function<Integer, Long> simpleLambda() {
		return x -> x.longValue();
	}

}
