package fixture;

@SuppressWarnings("ALL")
public class MyPatterns {
	String describe(Object object) {
		return switch (object) {
			case String s when s.length() > 5 -> s.substring(0, 5) + "...";
			case String s -> s;
			case Position2D(int x, int y) -> "[" + x + "," + y + "]";
			case Position3D(int x, int y, int z) -> "[" + x + "," + y + "," + z + "]";
			case Path(Position from, Position to) -> describe(from) + " -> " + describe(to);
			default -> object.toString();
		};
	}
}
