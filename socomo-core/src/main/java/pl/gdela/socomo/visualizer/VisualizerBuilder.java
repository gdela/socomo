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
import static org.apache.commons.io.FileUtils.readFileToString;
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
			template.addAsset(script("https://cdn.jsdelivr.net/gh/gdela/cytoscape.js-klay@v3.1.2-patch1/cytoscape-klay.min.js"));
			for (Asset asset : ownAssets(file)) {
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

	private List<Asset> ownAssets(File visualizerFile) throws IOException {
		BuilderProperties props = BuilderProperties.read();
		if (props.socomoVersion.contains("SNAPSHOT")) {
			log.info("snapshot build of socomo discovered");
			return localOwnAssets(props.assetsLocation, visualizerFile);
		} else {
			log.debug("packed release of socomo discovered");
			return remoteOwnAssets(props.socomoVersion);
		}
	}

	private List<Asset> remoteOwnAssets(String socomoVersion) {
		List<Asset> assets = new ArrayList<>();
		String baseUrl = "https://cdn.jsdelivr.net/gh/gdela/socomo@" + socomoVersion;
		assets.add(script(baseUrl + "/dist/bundle.min.js"));
		assets.add(style(baseUrl + "/dist/bundle.min.css"));
		return assets;
	}

	private List<Asset> localOwnAssets(File assetsLocation, File visualizerFile) {
		log.info("using assets from {}", assetsLocation);
		if (!assetsLocation.isDirectory()) {
			String message = "snapshot assets directory not found: ";
			message += "either use released version of socomo, or build snapshot version of socomo yourself";
			throw new IllegalStateException(message);
		}
		Collection<File> assetFiles = listFiles(assetsLocation, new String[]{"js", "css"}, false);
		List<Asset> assets = new ArrayList<>();
		for (File assetFile : assetFiles) {
			String extension = getExtension(assetFile.getName());
			if (extension.equals("js")) {
				assets.add(script(shortPath(visualizerFile, assetFile)));
			}
			if (extension.equals("css")) {
				assets.add(style(shortPath(visualizerFile, assetFile)));
			}
		}
		return assets;
	}

	private static class BuilderProperties {
		private String socomoVersion;
		private File assetsLocation;
		static BuilderProperties read() throws IOException {
			URL propsFileUrl = BuilderProperties.class.getResource("builder.properties");
			Properties propsInFile = new Properties();
			try (InputStream propertiesStream = propsFileUrl.openStream()) {
				propsInFile.load(propertiesStream);
			}
			BuilderProperties props = new BuilderProperties();
			props.socomoVersion = propsInFile.getProperty("socomo-version");
			File snapshotAssetsRoot = new File(propsInFile.getProperty("snapshot-assets-root"));
			props.assetsLocation = new File(snapshotAssetsRoot, BuilderProperties.class.getPackage().getName().replace('.', '/'));
			return props;
		}
	}

	private static String shortPath(File from, File to) {
		Path fromPath = from.toPath().toAbsolutePath().getParent();
		Path toPath = to.toPath().toAbsolutePath();
		return fromPath.relativize(toPath).toString();
	}
}
