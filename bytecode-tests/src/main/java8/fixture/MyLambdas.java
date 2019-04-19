package fixture;

import java.util.stream.Stream;

import fixture.Targets.Alfa;

@SuppressWarnings("all")
public class MyLambdas {

	private Alfa alfa;

	void simpleLambda() {
		Stream.of(alfa).forEach(x -> x.instanceMethod(null));
	}

}
