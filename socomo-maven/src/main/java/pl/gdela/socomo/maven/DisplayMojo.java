package pl.gdela.socomo.maven;


import org.apache.maven.plugins.annotations.Mojo;

/**
 * Maven plugin to execute Socomo analysis and display results in the browser.
 */
@Mojo(name = "display")
class DisplayMojo extends SocomoMojo {

	private static int numberOfDisplays;

	@Override
	void beforeExecute() {
		// noop
	}

	@Override
	void afterExecute() {
		if (tooManyDisplays()) return;
		socomo.display();

	}

	/**
	 * Prevent open too many tabs in the browser, when running this mojo on a multi-module
	 * maven project, which has plenty of modules, and thus plenty of socomo.html files
	 * will be generated. Better future solution: create and open just the diagram for
	 * the topmost parent pom with links to per-module socomo.html files.
	 */
	private static boolean tooManyDisplays() {
		if (numberOfDisplays >= 5) {
			return true;
		} else {
			numberOfDisplays++;
			return false;
		}
	}
}
