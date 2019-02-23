package pl.gdela.socomo.visualizer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.gdela.socomo.composition.Level;
import pl.gdela.socomo.composition.Module;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.io.FileUtils.listFiles;
import static org.apache.commons.io.FileUtils.writeStringToFile;
import static org.apache.commons.io.FilenameUtils.getExtension;
import static pl.gdela.socomo.visualizer.Asset.script;
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
			template.addAsset(script("https://cdn.jsdelivr.net/npm/cytoscape-klay@3.1.2/cytoscape-klay.min.js"));
			for (Asset asset : ownAssets(file)) {
				template.addAsset(asset);
			}
			String visualizer = template.render();
			writeStringToFile(file, visualizer, UTF_8);
		} catch (IOException e) {
			throw new RuntimeException("cannot build visualizer", e);
		}
	}

	private List<Asset> ownAssets(File file) throws IOException {
		@SuppressWarnings("unchecked")
		Collection<File> assetFiles = listFiles(ownAssetsLocation(), new String[]{"js", "css"}, false);
		List<Asset> assets = new ArrayList<>();
		for (File assetFile : assetFiles) {
			String extension = getExtension(assetFile.getName());
			if (extension.equals("css")) {
				assets.add(style(shortPath(file, assetFile)));
			}
			if (extension.equals("js")) {
				assets.add(script(shortPath(file, assetFile)));
			}
		}
		return assets;
	}

	private File ownAssetsLocation() throws IOException {
		URL url = getClass().getResource("builder.properties");
		Properties properties = new Properties();
		try (InputStream propertiesStream = url.openStream()) {
			properties.load(propertiesStream);
		}

		File assetsLocation;
		if (url.getProtocol().equals("file")) {
			log.info("unpacked build of socomo discovered");
			assetsLocation = new File(url.getFile()).getParentFile();
		}
		else if (!properties.getProperty("socomo-version").contains("SNAPSHOT")) {
			log.debug("packed release of socomo discovered");
			throw new UnsupportedOperationException("using non-snapshot socomo not yet implemented");
		}
		else {
			log.info("snapshot build of socomo discovered");
			File root = new File(properties.getProperty("snapshot-assets-root"));
			assetsLocation = new File(root, getClass().getPackage().getName().replace('.', '/'));
		}
		log.info("using assets from {}", assetsLocation);

		if (!assetsLocation.isDirectory()) {
			String message = "snapshot assets directory not found: ";
			message += "either use released version of socomo, or build snapshot version of socomo yourself";
			throw new IllegalStateException(message);
		}
		return assetsLocation;
	}

	private static String shortPath(File from, File to) {
		Path fromPath = from.toPath().toAbsolutePath().getParent();
		Path toPath = to.toPath().toAbsolutePath();
		return fromPath.relativize(toPath).toString();
	}
}
