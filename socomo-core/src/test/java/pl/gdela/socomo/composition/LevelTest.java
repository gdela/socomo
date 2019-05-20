package pl.gdela.socomo.composition;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class LevelTest {

	@Test
	public void components_are_identified_by_name() {
		Level level = new Level("dummy");
		Component a = level.component("a");
		Component b = level.component("b");
		assertThat(a).isNotSameAs(b);
		Component secondReferenceToA = level.component("a");
		assertThat(a).isSameAs(secondReferenceToA);
	}

	@Test
	public void dependencies_are_identified_by_from_and_to() {
		Level level = new Level("dummy");
		Component a = level.component("a");
		Component b = level.component("b");
		ComponentDep ab = level.dependency(a, b);
		ComponentDep ba = level.dependency(b, a);
		assertThat(ab).isNotSameAs(ba);
		ComponentDep secondReferenceToAB = level.dependency(a, b);
		assertThat(ab).isSameAs(secondReferenceToAB);
	}

	@Test
	public void max_component_size() {
		Level level = new Level("dummy");
		assertThat(level.maxComponentSize()).isEqualTo(0);
		level.component("a").incrementSizeBy(3);
		assertThat(level.maxComponentSize()).isEqualTo(3);
		level.component("b").incrementSizeBy(2);
		level.component("b").incrementSizeBy(2);
		level.component("c").incrementSizeBy(3);
		assertThat(level.maxComponentSize()).isEqualTo(4);
	}

	@Test
	public void sum_component_size() {
		Level level = new Level("dummy");
		assertThat(level.sumComponentSize()).isEqualTo(0);
		level.component("a").incrementSizeBy(3);
		assertThat(level.sumComponentSize()).isEqualTo(3);
		level.component("b").incrementSizeBy(2);
		level.component("b").incrementSizeBy(2);
		level.component("c").incrementSizeBy(3);
		assertThat(level.sumComponentSize()).isEqualTo(10);
	}

	@Test
	public void max_dependency_strength() {
		Level level = new Level("dummy");
		Component a = level.component("a");
		Component b = level.component("b");
		Component c = level.component("c");
		assertThat(level.maxDependencyStrength()).isEqualTo(0);
		level.dependency(a, b).incrementStrengthBy(3);
		assertThat(level.maxDependencyStrength()).isEqualTo(3);
		level.dependency(b, c).incrementStrengthBy(2);
		level.dependency(b, c).incrementStrengthBy(2);
		level.dependency(c, a).incrementStrengthBy(3);
		assertThat(level.maxDependencyStrength()).isEqualTo(4);
	}

}
