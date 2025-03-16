package fixture;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Target classes/method/fields which are used by the fixture code. We use names like
 * 'alfa', 'beta', 'gamma', so that it's easy to correlate the fixture code, with the
 * bytecode analyzer test assertions.
 */
@SuppressWarnings("all")
public final class Targets {

	public static class _Base {
		static Object staticField;
		Object instanceField;
		static void staticMethod(Object arg) {}
		void instanceMethod(Object arg) {}
	}

	public static class Alfa extends _Base {}
	public static class Beta extends _Base {}
	public static class Gamma extends _Base {}
	public static class Delta extends _Base {}
	public static class Epsilon extends _Base {}

	public static Alfa createAlfa() { return null; }
	public static Beta createBeta() { return null; }
	public static Gamma createGamma() { return null; }
	public static Delta createDelta() { return null; }
	public static Epsilon createEpsilon() { return null; }

	public static class _GenericBase<T> {
		public class GenericInner<U> {}
	}

	public static class GenericAlfa<T> extends _GenericBase<T> {}
	public static class GenericBeta<T> extends _GenericBase<T> {}

	public static class FooException extends RuntimeException {}
	public static class BlaException extends RuntimeException {}

	public enum ColorEnum {
		GREEN,
		BLACK,
		BLUE
	}

	@Target({ElementType.TYPE, ElementType.FIELD, ElementType.METHOD})
	public static @interface ShapeAnnotation {
		String stringValue();
		ColorEnum enumValue();
		int[] arrayValue() default {};
	}
}
