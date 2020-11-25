package pl.gdela.socomo.bytecode;

import org.objectweb.asm.Attribute;
import org.objectweb.asm.ConstantDynamic;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.TypePath;
import org.objectweb.asm.signature.SignatureReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.gdela.socomo.codemap.DepType;

import static org.objectweb.asm.Type.getMethodType;
import static org.objectweb.asm.Type.getObjectType;
import static org.objectweb.asm.Type.getType;
import static pl.gdela.socomo.codemap.DepType.ANNOTATED;
import static pl.gdela.socomo.codemap.DepType.CALLS;
import static pl.gdela.socomo.codemap.DepType.CALLS_DYNAMIC;
import static pl.gdela.socomo.codemap.DepType.CASTS_TO;
import static pl.gdela.socomo.codemap.DepType.CATCHES;
import static pl.gdela.socomo.codemap.DepType.CREATES;
import static pl.gdela.socomo.codemap.DepType.CREATES_ARRAY;
import static pl.gdela.socomo.codemap.DepType.READS_WRITES;
import static pl.gdela.socomo.codemap.DepType.REFERENCES;

class MethodVisitor extends org.objectweb.asm.MethodVisitor {
	private static final Logger log = LoggerFactory.getLogger(MethodVisitor.class);

	private final DependencyCollectorAdapter collector;

	private int instructionCount = 1;

	MethodVisitor(DependencyCollectorAdapter collector) {
		super(Opcodes.ASM9);
		this.collector = collector;
	}

	@Override
	public void visitEnd() {
		// to exit member entered in ClassVisitor.visitMethod()
		collector.exitMember(instructionCount);
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
		// simplification: actually it is not the method that is annotated, a type used in method signature is annotated
		collector.markDependency(ANNOTATED, getType(desc));
		return new AnnotationVisitor(collector);
	}

	@Override
	public void visitAnnotableParameterCount(int parameterCount, boolean visible) {
		// not interesting
	}

	@Override
	public AnnotationVisitor visitParameterAnnotation(int parameter, String desc, boolean visible) {
		log.trace("annotated parameter {} with {}", parameter, desc);
		// simplification: actually it is not the method that is annotated, its parameter is annotated
		collector.markDependency(ANNOTATED, getType(desc));
		return new AnnotationVisitor(collector);
	}

	@Override
	public AnnotationVisitor visitAnnotationDefault() {
		log.trace("annotated default");
		return new AnnotationVisitor(collector);
	}

	@Override
	public void visitParameter(String name, int access) {
		// not interesting
	}

	@Override
	public void visitCode() {
		// not interesting
	}

	@Override
	public void visitMaxs(int maxStack, int maxLocals) {
		// not interesting
	}

	@Override
	public void visitLineNumber(int line, Label start) {
		// not interesting
	}

	@Override
	public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
		instructionCount++;
		if (name.equals("this") || name.startsWith("this$")) {
			return; // not interesting, java creates such 'variable' in constructor, it's just dependency to self
		}
		if (signature == null) {
			collector.markDependency(REFERENCES, getType(desc));
		} else {
			new SignatureReader(signature).acceptType(new SignatureVisitor(collector, REFERENCES));
		}
	}

	@Override
	public void visitTypeInsn(int opcode, String type) {
		instructionCount++;
		DepType dep = REFERENCES;
		if (opcode == Opcodes.NEW) dep = CREATES;
		if (opcode == Opcodes.ANEWARRAY) dep = CREATES_ARRAY;
		if (opcode == Opcodes.CHECKCAST) dep = CASTS_TO;
		collector.markDependency(dep, getObjectType(type));
	}

	@Override
	public void visitMultiANewArrayInsn(String desc, int dims) {
		instructionCount++;
		collector.markDependency(CREATES_ARRAY, getType(desc));
	}

	@Override
	public void visitFieldInsn(int opcode, String owner, String name, String desc) {
		instructionCount++;
		if (name.equals("this") || name.startsWith("this$")) {
			return; // not interesting, java uses such 'field' in constructor of anonymous classes, it's just dependency to self
		}
		collector.markDependency(READS_WRITES, getObjectType(owner), name);
	}

	@Override
	public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
		instructionCount++;
		collector.markDependency(CALLS, getObjectType(owner), name + "()");
	}

	@Override
	@SuppressWarnings({"StatementWithEmptyBody", "ChainOfInstanceofChecks"})
	public void visitLdcInsn(final Object cst) {
		instructionCount++;
		if (cst instanceof Number) {
			// not interesting
		}
		else if (cst instanceof String) {
			// not interesting
		}
		else if (cst instanceof Type) {
			collector.markDependency(REFERENCES, (Type) cst);
		}
		else if (cst instanceof Handle) {
			// todo: read dependencies also from handles
			log.warn("ignoring dependencies in handle {}: not yet supported", cst);
		}
		else if (cst instanceof ConstantDynamic) {
			// todo: read dependencies also from constant dynamic
			log.warn("ignoring dependencies in constant dynamic {}: not yet supported", cst);
		}
		else {
			throw new IllegalArgumentException("unsupported constant type " + cst.getClass() + " (" + cst + ")");
		}
	}

	@Override
	public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
		instructionCount++;
		if (type != null) {
			collector.markDependency(CATCHES, getObjectType(type));
		}
	}

	@Override
	public void visitInsn(int opcode) {
		instructionCount++;
		// not interesting
	}

	@Override
	public void visitIntInsn(int opcode, int operand) {
		instructionCount++;
		// not interesting
	}

	@Override
	public void visitVarInsn(int opcode, int var) {
		instructionCount++;
		// not interesting
	}

	@Override
	public void visitIincInsn(int var, int increment) {
		instructionCount++;
		// not interesting
	}

	@Override
	public void visitLabel(Label label) {
		// not interesting
	}

	@Override
	public void visitJumpInsn(int opcode, Label label) {
		instructionCount++;
		// not interesting
	}

	@Override
	public void visitFrame(int type, int nLocal, Object[] local, int nStack, Object[] stack) {
		instructionCount++;
		// not interesting
	}

	@Override
	public void visitTableSwitchInsn(int min, int max, Label dflt, Label[] labels) {
		instructionCount++;
		// not interesting
	}

	@Override
	public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
		instructionCount++;
		// not interesting
	}

	@Override
	public void visitInvokeDynamicInsn(String name, String desc, Handle bsm, Object... bsmArgs) {
		instructionCount++;
		Type bootstrapMethodType = getMethodType(desc); // the method that returns instance of FunctionInterface (lambda)
		Type lambdaInterface = bootstrapMethodType.getReturnType();
		String lambdaInterfaceMethod = name + "()";
		collector.markDependency(CALLS_DYNAMIC, lambdaInterface, lambdaInterfaceMethod);
	}

	@Override
	public AnnotationVisitor visitLocalVariableAnnotation(int typeRef, TypePath typePath, Label[] start, Label[] end, int[] index, String desc, boolean visible) {
		instructionCount++;
		// todo: read dependencies also from local variable annotation
		log.warn("ignoring dependencies in local variable annotation {}, {}: not yet supported", typePath, desc);
		return null;
	}

	@Override
	public AnnotationVisitor visitInsnAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
		instructionCount++;
		// todo: read dependencies also from instruction annotation
		log.warn("ignoring dependencies in instruction annotation {}, {}: not yet supported", typePath, desc);
		return null;
	}

	@Override
	public AnnotationVisitor visitTryCatchAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
		instructionCount++;
		// todo: read dependencies also from try catch annotation
		log.warn("ignoring dependencies in try catch annotation {}, {}: not yet supported", typePath, desc);
		return null;
	}

	@Override
	public void visitAttribute(Attribute attribute) {
		log.warn("ignoring non-standard attribute {} of class {}", attribute.type, attribute.getClass());
	}
}
