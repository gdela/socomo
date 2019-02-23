/**
 * Model for complete map of code and its dependencies. The map starts in {@link pl.gdela.socomo.codemap.Codemap} object.
 *
 * <p>The code is represented as set of {@linkplain pl.gdela.socomo.codemap.CodePackage packages}
 * and {@linkplain pl.gdela.socomo.codemap.CodeMember members}. Here the package is understood
 * as a normal java package, for example {@code java.lang} and member is understood as member
 * of a package, i.e. it consists of simple class name and this class member name, for example
 * {@code Object.toString()}.</p>
 *
 * <p>The dependencies are expressed at {@linkplain pl.gdela.socomo.codemap.PackageDep package level}
 * and at {@linkplain pl.gdela.socomo.codemap.MemberDep package member level}.</p>
 *
 * <p>Note that because 'package' is a reserved word in java, in some places we use the synonym 'packet'.</p>
 *
 * @see pl.gdela.socomo.composition
 */
package pl.gdela.socomo.codemap;
