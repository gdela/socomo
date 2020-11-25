package pl.gdela.socomo.bytecode;

import org.objectweb.asm.Opcodes;

import static org.objectweb.asm.Type.getType;
import static pl.gdela.socomo.codemap.DepType.ANNOTATED;
import static pl.gdela.socomo.codemap.DepType.ANNOTATION_VALUE;

class AnnotationVisitor extends org.objectweb.asm.AnnotationVisitor {

	private final DependencyCollectorAdapter collector;

	AnnotationVisitor(DependencyCollectorAdapter collector) {
		super(Opcodes.ASM9);
		this.collector = collector;
	}

	@Override
	public void visitEnd() {
		// noop
	}

	@Override
	public void visit(String name, Object value) {
		// noop only primitives and string as annotation values here
	}

	@Override
	public AnnotationVisitor visitAnnotation(String name, String desc) {
		collector.markDependency(ANNOTATED, getType(desc));
		return this;
	}

	@Override
	public void visitEnum(String name, String desc, String value) {
		collector.markDependency(ANNOTATION_VALUE, getType(desc), value);
	}

	@Override
	public AnnotationVisitor visitArray(String name) {
		return this;
	}
}
