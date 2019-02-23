package pl.gdela.socomo.composition;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.gdela.socomo.codemap.CodePackage;
import pl.gdela.socomo.codemap.Codemap;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;
import static org.apache.commons.lang3.StringUtils.removeEnd;
import static org.apache.commons.lang3.StringUtils.substringAfter;
import static org.apache.commons.lang3.StringUtils.substringBefore;
import static org.apache.commons.lang3.StringUtils.substringBeforeLast;

public final class LevelGuesser {
	private static final Logger log = LoggerFactory.getLogger(LevelGuesser.class);

	private static final double THRESHOLD = 0.9;

	private LevelGuesser() { /* no instances */ }

	/**
	 * Returns the level name which is most interesting for this codemap. This will be
	 * the level under which most of the code is defined.
	 */
	public static String guessLevel(Codemap codemap) {
		String levelName = guessLevel(codemap, "");
		log.info("guessed level {}", levelName);
		return levelName;
	}

	static String guessLevel(Codemap codemap, String prefix) {
		log.debug("guessing level under prefix '{}'", prefix);
		int totalSize = 0;
		Map<String,Integer> components = new HashMap<>();
		for (CodePackage packet : codemap.packages()) {
			if (packet.fqn.startsWith(prefix)) {
				String suffix = substringAfter(packet.fqn, prefix);
				String componentName = substringBefore(suffix, ".");
				int componentSize = defaultIfNull(components.get(componentName), 0);
				componentSize += packet.size();
				totalSize += packet.size();
				components.put(componentName, componentSize);
			}
		}
		for (String componentName : components.keySet()) {
			int componentSize = components.get(componentName);
			float componentShare = (float) componentSize / totalSize;
			log.trace("component '{}' share is {}", componentName, componentShare);
			if (componentShare > THRESHOLD) {
				String newPrefix = prefix + componentName + ".";
				return guessLevel(codemap, newPrefix);
			}
		}
		String levelName = removeEnd(prefix, ".");
		if (components.isEmpty()) {
			log.debug("taking step back, code is not divided into components here");
			return substringBeforeLast(levelName, ".");
		}
		log.debug("level '{}' is balanced, none of its components has more than {} share", levelName, THRESHOLD);
		return levelName;
	}
}
