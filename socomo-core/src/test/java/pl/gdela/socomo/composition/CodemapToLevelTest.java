package pl.gdela.socomo.composition;

import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.gdela.socomo.codemap.Codemap;
import pl.gdela.socomo.codemap.CodemapBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static pl.gdela.socomo.composition.CodemapToLevel.transform;

public class CodemapToLevelTest {
	private static final Logger log = LoggerFactory.getLogger(CodemapToLevelTest.class);

	private static Codemap codemap;

	@BeforeClass
	public static void buildSampleCodemap() {
		codemap = new CodemapBuilder()
				// cat
				.memberDep("animal.domestic.cat.Felix.eat()", "animal.rodent.mouse.Mickey.body()")
				.memberDep("animal.domestic.cat.Molly.eat()", "animal.domestic.fish.Nemo.body()")
				// dog
				.memberDep("animal.domestic.dog.Buddy.bark()", "animal.domestic.cat.Felix.ears()")
				.memberDep("animal.domestic.dog.Buddy.bark()", "animal.domestic.cat.Molly.ears()")
				.memberDep("animal.domestic.dog.Buddy.bark()", "animal.rodent.mouse.Mickey.ears()")
				// rodent
				.memberDep("animal.rodent.mouse.Mickey.eat()", "plant.Corn.leaves()")
				.memberDep("animal.rodent.Unknown.eat()", "plant.Corn.leaves()")
				// inanimate
				.memberDep("plant.Corn.isWateredBy()", "animal.domestic.dog.Buddy.thingy()")
				.member("ghost.Poltergeist.scream()")
		.getCodemap();
		log.info("sample codemap:\n{}", codemap.formatted());
	}

	@Test
	public void lower_level() {
		// given
		String levelName = "animal.domestic";

		// when
		Level level = transform(codemap, levelName);
		log.info("produced level '{}':\n{}", levelName, level.formatted());

		// then
		Component cat = new Component("cat");
		Component dog = new Component("dog");
		Component fish = new Component("fish");
		assertThat(level.name).isEqualTo(levelName);
		assertThat(level.components).containsExactly(cat, dog, fish);
		assertThat(level.dependencies).containsExactly(
				new ComponentDep(cat, fish),
				new ComponentDep(dog, cat)
		);
		assertThat(level.dependency(cat, fish).strength).isEqualTo(1);
		assertThat(level.dependency(dog, cat).strength).isEqualTo(2);
		assertThat(level.component("cat").size).isGreaterThan(level.component("dog").size);
		assertThat(level.component("dog").size).isEqualTo(level.component("fish").size);
	}

	@Test
	public void upper_level() {
		// given
		String levelName = "animal";

		// when
		Level level = transform(codemap, levelName);
		log.info("produced level '{}':\n{}", levelName, level.formatted());

		// then
		Component domestic = new Component("domestic");
		Component rodent = new Component("rodent");
		assertThat(level.name).isEqualTo(levelName);
		assertThat(level.components).containsExactly(domestic, rodent);
		assertThat(level.dependencies).containsExactly(
				new ComponentDep(domestic, rodent)
		);
		assertThat(level.dependency(domestic, rodent).strength).isEqualTo(2);
		assertThat(level.component("domestic").size).isGreaterThan(level.component("rodent").size);
	}

	@Test
	public void top_level() {
		// given
		String levelName = "";

		// when
		Level level = transform(codemap, levelName);
		log.info("produced level '{}':\n{}", levelName, level.formatted());

		// then
		Component animal = new Component("animal");
		Component ghost = new Component("ghost");
		Component plant = new Component("plant");
		assertThat(level.name).isEqualTo(levelName);
		assertThat(level.components).containsExactly(animal, ghost, plant);
		assertThat(level.dependencies).containsExactly(
				new ComponentDep(animal, plant),
				new ComponentDep(plant, animal)
		);
		assertThat(level.dependency(animal, plant).strength).isEqualTo(2);
		assertThat(level.dependency(plant, animal).strength).isEqualTo(1);
		assertThat(level.component("animal").size).isGreaterThan(level.component("plant").size);
	}

	@Test
	public void atypical_level() {
		// given
		String levelName = "animal.rodent";

		// when
		Level level = transform(codemap, levelName);
		log.info("produced level '{}':\n{}", levelName, level.formatted());

		// then
		Component root = new Component("[root]");
		Component mouse = new Component("mouse");
		assertThat(level.name).isEqualTo(levelName);
		assertThat(level.components).containsExactly(root, mouse);
		assertThat(level.dependencies).isEmpty();
	}
}
