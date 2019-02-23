package pl.gdela.socomo.codemap;

/**
 * The origin of a codemap element, eg. where a given class was defined.
 * If class is defined in different source roots, or in a source root and
 * external library, then the enum value with lowest order takes precedence.
 */
public enum Origin {

	/**
	 * Main source root of the module.
	 */
	MAIN,

	/**
	 * Test source root of the module.
	 */
	TEST,

	/**
	 * Not a part of the module, typically a library that we know about, because
	 * module sources refer to them.
	 */
	EXTERNAL
}
