package fixture;

import java.math.BigDecimal;

@SuppressWarnings("all")
public class MyMethods17 {

	public static Object mySwitchExpression(String what, String value) {
		return switch (what) {
			case "Integer", "int" -> Integer.parseInt(value);
			case "Long", "long" -> Long.parseLong(value);
			default -> {
				if (value.contains("."))
					yield new BigDecimal(value);
				else
					yield value;
			}
		};
	}

	public static int myPatternMatchingForInstanceOf(Object input) {
		if (input instanceof String s && s.length() > 5) {
			return s.length();
		} else {
			return -1;
		}
	}

}
