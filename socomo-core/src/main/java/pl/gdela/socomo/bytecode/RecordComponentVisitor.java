package pl.gdela.socomo.bytecode;

import org.objectweb.asm.Attribute;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.TypePath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.objectweb.asm.Type.getType;
import static pl.gdela.socomo.codemap.DepType.ANNOTATED;

class RecordComponentVisitor extends org.objectweb.asm.RecordComponentVisitor {
	private static final Logger log = LoggerFactory.getLogger(RecordComponentVisitor.class);

	private final DependencyCollectorAdapter collector;

	RecordComponentVisitor(DependencyCollectorAdapter collector) {
		super(Opcodes.ASM9);
		this.collector = collector;
	}

	@Override
	public void visitEnd() {
		// to exit member entered in ClassVisitor.visitRecordComponent()
		collector.exitMember(0); // record component will be counted as field and as method
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
		// simplification: actually it is not the record component that is annotated, a type used in record component signature is annotated
		collector.markDependency(ANNOTATED, getType(desc));
		return new AnnotationVisitor(collector);
	}

	@Override
	public void visitAttribute(Attribute attribute) {
		log.warn("ignoring non-standard attribute {} of class {}", attribute.type, attribute.getClass());
	}

}
