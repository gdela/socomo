package pl.gdela.socomo.composition;

import pl.gdela.socomo.codemap.CodePackage;
import pl.gdela.socomo.codemap.Codemap;
import pl.gdela.socomo.codemap.PackageDep;

import static org.apache.commons.lang3.StringUtils.substringAfter;
import static org.apache.commons.lang3.StringUtils.substringBefore;

public class CodemapToLevel {

	public static Level transform(Codemap codemap, String levelName) {
		Level level = new Level(levelName);

		for (CodePackage packet : codemap.packages()) {

			Component component = componentFor(packet, level);
			if (component == null) continue;

			component.incrementSizeBy(packet.size());
		}

		for (PackageDep packageDep : codemap.packageDeps()) {

			Component fromComponent = componentFor(packageDep.from, level);
			if (fromComponent == null) continue;

			Component toComponent = componentFor(packageDep.to, level);
			if (toComponent == null) continue;

			if (fromComponent == toComponent) continue; // uninteresting information

			ComponentDep componentDep = level.dependency(fromComponent, toComponent);
			componentDep.incrementStrengthBy(packageDep.strength());
		}

		return level;
	}

	private static Component componentFor(CodePackage packet, Level level) {
		if (packet.fqn.equals(level.name)) {
			return level.component("[root]");
		}
		String levelPrefix = level.name.equals("") ? "" : level.name + ".";
		if (packet.fqn.startsWith(levelPrefix)) {
			String suffix = substringAfter(packet.fqn, levelPrefix);
			String componentName = substringBefore(suffix, ".");
			return level.component(componentName);
		}
		// this package does not belong to requested level
		return null;
	}
}
