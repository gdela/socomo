package fixture;

import java.util.function.Function;

@SuppressWarnings("all")
public class MyLambdas {

	Function<Integer, Boolean> simpleLambda() {
		return x -> x < 1024;
	}

}
