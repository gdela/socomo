package pl.gdela.socomo.visualizer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.gdela.socomo.composition.Level;
import pl.gdela.socomo.composition.Module;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.io.FileUtils.readFileToString;
import static org.apache.commons.io.FileUtils.writeStringToFile;
import static pl.gdela.socomo.visualizer.Asset.asyncScript;
import static pl.gdela.socomo.visualizer.Asset.script;
import static pl.gdela.socomo.visualizer.Asset.scriptContent;
import static pl.gdela.socomo.visualizer.Asset.style;

/**
 * Builds the html file with composition analysis results. The file, typically
 * named socomo.html, is also used to "launch" visualizer single-page app.
 */
public class VisualizerBuilder {
	private static final Logger log = LoggerFactory.getLogger(VisualizerBuilder.class);

	private Module module;

	private Level level;

	public void setModule(Module module) {
		this.module = module;
	}

	public void setLevel(Level level) {
		this.level = level;
	}

	public void buildInto(File htmlFile, File dataFile) {
		log.info("visualizing into {}", htmlFile);
		try {
			VisualizerHtml template = new VisualizerHtml(module);
			template.addLevel(level);
			for (Asset asset : assets()) {
				template.addAsset(asset);
			}
			String dataFileUrl = urlFromTo(htmlFile, dataFile);
			template.addAsset(asyncScript(dataFileUrl));
			String visualizer = template.render();
			save(visualizer, htmlFile);
		} catch (IOException e) {
			throw new RuntimeException("cannot build visualizer", e);
		}
	}

	private static void save(String visualizer, File file) throws IOException {
		// avoid touching file if same contents is already there (ignoring git's autocrlf changes)
		try {
			String oldContents = readFileToString(file, UTF_8).replace("\r\n", "\n").replace('\r', '\n');
			String newContents = visualizer.replace("\r\n", "\n").replace('\r', '\n');
			if (newContents.equals(oldContents)) return;
		} catch (IOException ignore) {
			// try to write, even if could not read old contents
		}
		writeStringToFile(file, visualizer, UTF_8);
	}

	private List<Asset> assets() {
		String socomoVersion = SocomoVersion.get();
		if (!socomoVersion.contains("SNAPSHOT")) {
			log.debug("release version of socomo discovered");
			return productionAssets(socomoVersion);
		} else {
			log.info("snapshot version of socomo discovered");
			return developmentAssets(socomoVersion);
		}
	}

	private List<Asset> productionAssets(String socomoVersion) {
		List<Asset> assets = new ArrayList<>();
		String baseUrl = "https://cdn.jsdelivr.net/gh/gdela/socomo@" + socomoVersion + "/socomo-view/dist";
		assets.add(style(baseUrl + "/index.css"));
		assets.add(script(baseUrl + "/index.js"));
		return assets;
	}

	private List<Asset> developmentAssets(String socomoVersion) {
		List<Asset> assets = new ArrayList<>();
		String baseUrl = "http://localhost:8086";
		assets.add(style(baseUrl + "/index.css"));
		assets.add(script(baseUrl + "/index.js"));
		String note;
		note  = "if (typeof socomo === 'undefined') document.write(\n";
		note += "  '<i>Socomo "+socomoVersion+", an in-development version, was used to generate this file. To view it ' +\n";
		note += "  'you need to be running `npm --prefix socomo-view run serve` in your local clone of Socomo.</i>' +\n";
		note += "  '<style>script{display:block;white-space:pre;font-family:monospace;}i{color:red;}</style>'\n";
		note += ")";
		assets.add(scriptContent(note));
		return assets;
	}

	/**
	 * Returns relative path from parent directory of one file to another file.
	 */
	private static String urlFromTo(File fromFile, File toFile) {
		Path parentDirectory = fromFile.toPath().toAbsolutePath().getParent();
		Path target = toFile.toPath().toAbsolutePath();
		return parentDirectory.relativize(target).toString().replace('\\', '/');
	}
}
