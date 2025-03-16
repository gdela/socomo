package fixture;

@SuppressWarnings("all")
public record Path<P extends Position>(P from, P to) {}
