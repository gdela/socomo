package pl.gdela.socomo.visualizer;

import java.util.ArrayList;
import java.util.List;

import pl.gdela.socomo.composition.Component;
import pl.gdela.socomo.composition.ComponentDep;
import pl.gdela.socomo.composition.Level;
import pl.gdela.socomo.composition.Module;

import static java.lang.Math.round;
import static java.lang.String.format;
import static java.util.Locale.ENGLISH;
import static org.apache.commons.lang3.StringUtils.repeat;
import static org.apache.commons.lang3.StringUtils.split;
import static org.apache.commons.text.StringEscapeUtils.escapeEcmaScript;
import static org.apache.commons.text.StringEscapeUtils.escapeHtml4;

/**
 * Template for generating socomo.html files.
 *
 * <p>We do not use freemarker, mustache or any other template, because we need complete
 * control over every space, newline, etc., as the generated html file is supposed to be
 * committed to the source repository and it also needs to be human-readable, or even
 * beautiful.</p>
 */
class VisualizerHtml {
	private final Module module;
	private final List<Level> levels = new ArrayList<>();
	private final List<Asset> assets = new ArrayList<>();

	private StringBuilder out;
	private String indent = "";

	VisualizerHtml(Module module) {
		this.module = module;
	}

	void addLevel(Level level) {
		levels.add(level);
	}

	void addAsset(Asset asset) {
		assets.add(asset);
	}

	String render() {
		out = new StringBuilder();
		printHtml();
		return out.toString();
	}

	private void printHtml() {
		out("<!-- File autogenerated by Socomo (https://github.com/gdela/socomo): do not edit by hand, but do commit to repository -->");
		out("<!doctype html>");
		out("<!--suppress ALL-->");
		out("<html lang='en'>");
		printHead();
		printBody();
		out("</html>");
	}

	private void printHead() {
		out("<head>");
		indent(+4);
		out("<title>Socomo: %s</title>", escapeHtml(module.name));
		for (Asset asset : assets) {
			switch (asset.type) {
				case STYLE:
					out("<link href='%s' rel='stylesheet'/>", asset.url);
					break;
				case SCRIPT:
					out("<script src='%s'></script>", asset.url);
					break;
				case ASYNC_SCRIPT:
					out("<script src='%s' async></script>", asset.url);
					break;
				case STYLE_CONTENT:
					out("<style>");
					for (String line : split(asset.content, '\n')) out(line);
					out("</style>");
					break;
				case SCRIPT_CONTENT:
					out("<script>");
					for (String line : split(asset.content, '\n')) out(line);
					out("</script>");
					break;
				default:
					throw new IllegalArgumentException("unsupported asset type " + asset.type);
			}
		}
		out("<meta http-equiv='content-type' content='text/html;charset=UTF-8'/>");
		indent(-4);
		out("</head>");
	}

	private void printBody() {
		out("<script>");
		out("composition = [{ module: %s },", ecmaString(module.name));
		for (Level level : levels) {
			out("");
			printLevel(level);
		}
		out("");
		out("];");
		out("</script>");
	}

	private void printLevel(Level level) {
		out("{");
		indent(+2);
		out("level: %s,", ecmaString(level.name));
		printComponents(level);
		printDependencies(level);
		indent(-2);
		out("},");
	}

	private void printComponents(Level level) {
		out("components: {");
		indent(+2);
		for (Component component : level.components) {
			out("%-36s :{ share: %2d },", ecmaString(component.name), shareOf(component, level));
		}
		indent(-2);
		out("},");
	}

	private void printDependencies(Level level) {
		out("dependencies: {");
		indent(+2);
		for (ComponentDep dep : level.dependencies) {
			String depName = dep.from.name + " -> " + dep.to.name;
			out("%-36s :{ strength: %1d },", ecmaString(depName), strengthOf(dep, level));
		}
		indent(-2);
		out("},");
	}

	private void indent(int change) {
		int newSize = indent.length() + change;
		indent = repeat(' ', newSize);
	}

	private void out(String line, Object... args) {
		out.append(indent).append(format(ENGLISH, line, args)).append("\n");
	}

	private static String ecmaString(String contents) {
		return "'" + escapeEcmaScript(contents) + "'";
	}

	private static String escapeHtml(String contents) {
		return escapeHtml4(contents);
	}

	/**
	 * Share of this component in the total size of the level, expressed as percentages. Depending
	 * on the number of components, the percentages are rounded to nearest multiply of 1, 5 or 10,
	 * to avoid changing the html output too often due to even minor changes. Almost zero share
	 * is expressed as 1%, to avoid misleading that component has no code at all.
	 */
	private static int shareOf(Component component, Level level) {
		float sumSize = level.sumComponentSize();
		float sharePercentage = component.size / sumSize * 100;
		int grain = 1;
		int numOfComponents = level.components.size();
		if (numOfComponents <= 20) grain = 5;
		if (numOfComponents <= 10) grain = 10;
		return roundToGrain(sharePercentage, grain);
	}

	/**
	 * Strength of this dependency relative to other dependencies in the level. Expressed as a value
	 * between 1 and 9, where 9 means the strongest dependency in this level. This range was chosen
	 * to avoid misleading 0-strength in the html output, and fit the value in one character only.
	 */
	private static int strengthOf(ComponentDep dependency, Level level) {
		float maxStrength = level.maxDependencyStrength();
		float strengthFraction = dependency.strength / maxStrength;
		return mapToRange(strengthFraction, 1, 9);
	}

	private static int roundToGrain(float value, int grain) {
		return round(value / grain) * grain;
	}

	@SuppressWarnings("SameParameterValue")
	private static int mapToRange(float fraction, int lowerBound, int upperBound) {
		int width = upperBound - lowerBound;
		return round(fraction * width + lowerBound);
	}
}
