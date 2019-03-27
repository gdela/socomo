package pl.gdela.socomo.visualizer;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import org.apache.commons.io.LineIterator;

import static java.lang.String.format;
import static org.apache.commons.io.FileUtils.lineIterator;
import static org.apache.commons.lang3.StringUtils.substringBetween;
import static org.apache.commons.lang3.Validate.validState;

public final class SocomoVersion {

	private SocomoVersion() { /* no instances */ }

	/**
	 * Returns the version of Socomo that is currently being used.
	 */
	public static String get() {
		return version;
	}

	private static final String version;
	static {
		String v = readFromManifest();
		if (v == null) {
			v = readFromPom();
		}
		version = v;
	}

	/**
	 * Reads version from MANIFEST.MF contained in a socomo-core.jar file.
	 * @return version or null if we're not in a jar file
	 */
	private static String readFromManifest() {
		return SocomoVersion.class.getPackage().getImplementationVersion();
	}

	/**
	 * Reads version from pom.xml of an unpacked and locally build socomo-core maven module.
	 * @return version or throws exception if couldn't do that
	 */
	private static String readFromPom() {
		try {
			URL classesUrl = SocomoVersion.class.getResource("/");
			validState(classesUrl.getProtocol().equals("file"), "expected %s to be plain file path", classesUrl);
			File classesDir = new File(classesUrl.toURI()); // the socomo/target/classes dir
			File pomFile = new File(classesDir.getParentFile().getParentFile(), "pom.xml");
			validState(pomFile.canRead(), "incorrectly resolved pom.xml location to %s", pomFile);
			try (LineIterator it = lineIterator(pomFile)) {
				while (it.hasNext()) {
					String line = it.nextLine().trim();
					if (line.startsWith("<version>") && line.endsWith("</version>")) {
						return substringBetween(line, "<version>", "</version>");
					}
				}
			}
			throw new IllegalStateException(format("cannot find version declaration in %s", pomFile));
		} catch (URISyntaxException | IOException e) {
			throw new RuntimeException("cannot read version from pom.xml file", e);
		}
	}

}
