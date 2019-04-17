package pl.gdela.socomo.visualizer;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SocomoVersionTest {

	@Test
	public void version_can_be_read() {
		// when
		String version = SocomoVersion.get();

		// then
		assertThat(version)
			.isNotNull()
			.startsWith("2.");
	}
}
