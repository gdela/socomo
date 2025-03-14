package fixture;

@SuppressWarnings("ALL")
public record Path<P extends Position>(P from, P to) {}
