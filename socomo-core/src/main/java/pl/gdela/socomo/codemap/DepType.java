package pl.gdela.socomo.codemap;

public enum DepType {
	REFERENCES,
	EXTENDS,
	IMPLEMENTS,
	IS_OF_TYPE,
	HAS_PARAM,
	RETURNS,
	CALLS,
	READS_WRITES,
	CREATES,
	CREATES_ARRAY,
	CASTS_TO,
	THROWS,
	CATCHES,
	TYPE_PARAM,
	ANNOTATED,
	ANNOTATION_VALUE;

	@Override
	public String toString() {
		return name().toLowerCase();
	}
}
