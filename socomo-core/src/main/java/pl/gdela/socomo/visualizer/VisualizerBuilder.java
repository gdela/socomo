package pl.gdela.socomo.visualizer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.gdela.socomo.composition.Level;
import pl.gdela.socomo.composition.Module;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.io.FileUtils.readFileToString;
import static org.apache.commons.io.FileUtils.writeStringToFile;
import static pl.gdela.socomo.visualizer.Asset.script;
import static pl.gdela.socomo.visualizer.Asset.scriptContent;
import static pl.gdela.socomo.visualizer.Asset.style;

/**
 * Builds the visualizer single-page app for given module.
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

	public void buildInto(File file) {
		log.info("visualizing into {}", file);
		try {
			VisualizerHtml template = new VisualizerHtml(module);
			template.addLevel(level);
			template.addAsset(style("https://fonts.googleapis.com/css?family=Lato:400,700"));
			template.addAsset(script("https://cdn.jsdelivr.net/npm/lodash@4.17.11/lodash.min.js"));
			template.addAsset(script("https://cdn.jsdelivr.net/npm/cytoscape@3.2.22/dist/cytoscape.min.js"));
			template.addAsset(script("https://cdn.jsdelivr.net/npm/klayjs@0.4.1/klay.min.js"));
			template.addAsset(script("https://cdn.jsdelivr.net/gh/gdela/cytoscape.js-klay@v3.1.2-patch1/cytoscape-klay.min.js"));
			for (Asset asset : ownAssets()) {
				template.addAsset(asset);
			}
			String visualizer = template.render();
			save(visualizer, file);
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

	private List<Asset> ownAssets() {
		String socomoVersion = SocomoVersion.get();
		if (socomoVersion.contains("SNAPSHOT")) {
			log.info("snapshot build of socomo discovered");
			return localOwnAssets(socomoVersion);
		} else {
			log.debug("packed release of socomo discovered");
			return remoteOwnAssets(socomoVersion);
		}
	}

	private List<Asset> remoteOwnAssets(String socomoVersion) {
		List<Asset> assets = new ArrayList<>();
		String baseUrl = "https://cdn.jsdelivr.net/gh/gdela/socomo@" + socomoVersion;
		assets.add(style(baseUrl + "/socomo-view/dist/bundle.css"));
		assets.add(script(baseUrl + "/socomo-view/dist/bundle.js"));
		return assets;
	}

	private List<Asset> localOwnAssets(String socomoVersion) {
		List<Asset> assets = new ArrayList<>();
		String baseUrl = "http://localhost:8086";
		assets.add(style(baseUrl + "/bundle.css"));
		assets.add(script(baseUrl + "/bundle.js"));
		String note;
		note  = "if (typeof socomo === 'undefined') document.write(\n";
		note += "  'Socomo "+socomoVersion+", an in-development version of Socomo, was used to generate this file. To view it you ' +\n";
		note += "  'need to be running `npm --prefix socomo-view run serve` in your local clone of Socomo. Hit Ctrl+U for raw view.'\n";
		note += ")";
		assets.add(scriptContent(note));
		return assets;
	}

}
