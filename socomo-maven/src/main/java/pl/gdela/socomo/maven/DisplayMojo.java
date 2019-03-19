package pl.gdela.socomo.maven;


import org.apache.maven.plugins.annotations.Mojo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.awt.Desktop.getDesktop;

/**
 * Maven plugin to execute SoCoMo analysis and display results in the browser.
 */
@Mojo(name = "display")
class DisplayMojo extends SocomoMojo {

	private static final Logger log = LoggerFactory.getLogger(DisplayMojo.class);

	@Override
	void beforeExecute() {
		// noop
	}

	@Override
	void afterExecute() {
		try {
			getDesktop().browse(socomoTargetFile.toURI());
		} catch (Exception e) {
			log.warn("cannot open your browser, {}", e.getMessage());
			log.warn("open this file yourself: {}", socomoTargetFile);
		}
	}
}
