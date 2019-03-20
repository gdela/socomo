package pl.gdela.socomo.maven;


import org.apache.maven.plugins.annotations.Mojo;

/**
 * Maven plugin to execute SoCoMo analysis and display results in the browser.
 */
@Mojo(name = "display")
class DisplayMojo extends SocomoMojo {

	@Override
	void beforeExecute() {
		// noop
	}

	@Override
	void afterExecute() {
		socomo.display();
	}
}
