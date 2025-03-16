@SuppressWarnings("all")

void main() {
	try {
		new BigDecimal("123E456");
	} catch (NumberFormatException _) {
		// ignore
	}
}
