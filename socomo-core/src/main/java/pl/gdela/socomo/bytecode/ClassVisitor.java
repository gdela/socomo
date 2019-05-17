package pl.gdela.socomo.bytecode;

import org.objectweb.asm.Attribute;
import org.objectweb.asm.ModuleVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.TypePath;
import org.objectweb.asm.signature.SignatureReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;
import static org.objectweb.asm.Type.getMethodType;
import static org.objectweb.asm.Type.getObjectType;
import static org.objectweb.asm.Type.getType;
import static pl.gdela.socomo.codemap.DepType.ANNOTATED;
import static pl.gdela.socomo.codemap.DepType.EXTENDS;
import static pl.gdela.socomo.codemap.DepType.HAS_PARAM;
import static pl.gdela.socomo.codemap.DepType.IMPLEMENTS;
import static pl.gdela.socomo.codemap.DepType.IS_OF_TYPE;
import static pl.gdela.socomo.codemap.DepType.RETURNS;
import static pl.gdela.socomo.codemap.DepType.THROWS;

class ClassVisitor extends org.objectweb.asm.ClassVisitor {
	private static final Logger log = LoggerFactory.getLogger(ClassVisitor.class);

	private final DependencyCollectorAdapter collector;

	ClassVisitor(DependencyCollectorAdapter collector) {
		super(Opcodes.ASM7);
		this.collector = collector;
	}

	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		log.trace("in class {}", name);
		collector.enterClass(getObjectType(name));
		if (signature == null) {
			collector.markDependency(EXTENDS, getObjectType(superName));
			for (String interfaceName : defaultIfNull(interfaces, new String[0])) {
				collector.markDependency(IMPLEMENTS, getObjectType(interfaceName));
			}
		} else {
			new SignatureReader(signature).accept(new SignatureVisitor(collector));
		}
	}

	@Override
	public void visitEnd() {
		collector.exitClass();
	}

	@Override
	public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
		log.trace("annotated with {}", desc);
		collector.markDependency(ANNOTATED, getType(desc));
		return new AnnotationVisitor(collector);
	}

	@Override
	public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
		log.trace("annotated type {} with {}", typePath, desc);
		// simplification: actually it is not the class that is annotated, a type used in class signature is annotated
		collector.markDependency(ANNOTATED, getType(desc));
		return new AnnotationVisitor(collector);
	}

	@Override
	public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
		log.trace("in field {}", name);
		collector.enterMember(name);
		if (signature == null) {
			collector.markDependency(IS_OF_TYPE, getType(desc));
		} else {
			new SignatureReader(signature).acceptType(new SignatureVisitor(collector, IS_OF_TYPE));
		}
		return new FieldVisitor(collector);
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		log.trace("in method {}", name);
		collector.enterMember(name + "()");
		if (signature == null) {
			Type methodType = getMethodType(desc);
			collector.markDependency(RETURNS, methodType.getReturnType());
			for (Type argumentType : methodType.getArgumentTypes()) {
				collector.markDependency(HAS_PARAM, argumentType);
			}
		} else {
			new SignatureReader(signature).accept(new SignatureVisitor(collector));
		}
		for (String exception : defaultIfNull(exceptions, new String[0])) {
			collector.markDependency(THROWS, getObjectType(exception));
		}
		return new MethodVisitor(collector);
	}

	@Override
	public void visitOuterClass(String owner, String name, String desc) {
		// outer classes are in separate *.class files, so they will be visited independently
		String encloser = owner + (name != null ? "." + name + "()" : "");
		log.trace("is enclosed within {}", encloser);
	}

	@Override
	public void visitInnerClass(String name, String outerName, String innerName, int access) {
		// this method is called for each used inner class, so this somewhat superfluous
		// to what we already record in other visitors: method visitors, field visitors, etc
	}

	@Override
	public ModuleVisitor visitModule(String name, int access, String version) {
		// not interesting
		return null;
	}

	@Override
	public void visitNestHost(String nestHost) {
		// not interesting
	}

	@Override
	public void visitNestMember(String nestMember) {
		// not interesting
	}

	@Override
	public void visitAttribute(Attribute attribute) {
		log.warn("ignoring non-standard attribute {} of class {}", attribute.type, attribute.getClass());
	}

	@Override
	public void visitSource(String source, String debug) {
		// no dependencies here
	}
}
