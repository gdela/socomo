package pl.gdela.socomo.bytecode;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.gdela.socomo.codemap.DepType;

import static org.apache.commons.lang3.Validate.notNull;
import static org.objectweb.asm.Type.getObjectType;
import static pl.gdela.socomo.codemap.DepType.HAS_PARAM;
import static pl.gdela.socomo.codemap.DepType.IMPLEMENTS;
import static pl.gdela.socomo.codemap.DepType.RETURNS;
import static pl.gdela.socomo.codemap.DepType.THROWS;
import static pl.gdela.socomo.codemap.DepType.TYPE_PARAM;

class SignatureVisitor extends org.objectweb.asm.signature.SignatureVisitor {
	private static final Logger log = LoggerFactory.getLogger(SignatureVisitor.class);

	private final DependencyCollectorAdapter collector;
	private final DepType currentType;
	private final String indent;

	private Type resolvedClassType;
	private Type visitedInnerClassType;

	private SignatureVisitor(DependencyCollectorAdapter collector, DepType initialType, String indent) {
		super(Opcodes.ASM9);
		this.collector = collector;
		this.currentType = initialType;
		this.indent = indent;
	}

	SignatureVisitor(DependencyCollectorAdapter collector, DepType initialType) {
		this(collector, initialType, "");
	}

	SignatureVisitor(DependencyCollectorAdapter collector) {
		this(collector, null);
	}

	private SignatureVisitor dive(DepType newScope) {
		return new SignatureVisitor(collector, newScope, indent + "  ");
	}

	@Override
	public SignatureVisitor visitSuperclass() {
		log.trace("{} superclass", indent);
		return dive(DepType.EXTENDS);
	}

	@Override
	public SignatureVisitor visitInterface() {
		log.trace("{} interface", indent);
		return dive(IMPLEMENTS);
	}

	@Override
	public SignatureVisitor visitParameterType() {
		log.trace("{} parameter type", indent);
		return dive(HAS_PARAM);
	}

	@Override
	public SignatureVisitor visitReturnType() {
		log.trace("{} return type", indent);
		return dive(RETURNS);
	}

	@Override
	public SignatureVisitor visitExceptionType() {
		log.trace("{} exception type", indent);
		return dive(THROWS);
	}

	@Override
	public void visitTypeArgument() {
		log.trace("{} type argument", indent);
		// noop
	}

	@Override
	public SignatureVisitor visitTypeArgument(char wildcard) {
		log.trace("{} type argument {}", indent, wildcard);
		return dive(TYPE_PARAM);
	}

	@Override
	public void visitFormalTypeParameter(String name) {
		log.trace("{} format type parameter {}", indent, name);
		// noop
	}

	@Override
	public SignatureVisitor visitClassBound() {
		log.trace("{} class bound", indent);
		return dive(TYPE_PARAM);
	}

	@Override
	public SignatureVisitor visitInterfaceBound() {
		log.trace("{} interface bound", indent);
		return dive(TYPE_PARAM);
	}

	@Override
	public void visitBaseType(char descriptor) {
		log.trace("{} base type {}", indent, descriptor);
		// noop
	}

	@Override
	public void visitTypeVariable(String name) {
		log.trace("{} type variable {}", indent, name);
		// noop
	}

	@Override
	public SignatureVisitor visitArrayType() {
		log.trace("{} array type", indent);
		return dive(currentType);
	}

	@Override
	public void visitClassType(String name) {
		log.trace("{} class type {}", indent, name);
		resolvedClassType = getObjectType(name);
	}

	@Override
	public void visitInnerClassType(String name) {
		log.trace("{} inner class type {}", indent, name);
		notNull(resolvedClassType, "this method should be called only after class type has been visited");
		visitedInnerClassType = getObjectType(resolvedClassType.getInternalName() + "$" + name);
	}

	@Override
	public void visitEnd() {
		if (visitedInnerClassType != null) {
			collector.markDependency(currentType, visitedInnerClassType);
			visitedInnerClassType = null;
		}
		else if (resolvedClassType != null) {
			collector.markDependency(currentType, resolvedClassType);
			resolvedClassType = null;
		}
		log.trace("{} end", indent);
	}
}
