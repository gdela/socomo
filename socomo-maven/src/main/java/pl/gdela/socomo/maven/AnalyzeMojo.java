package pl.gdela.socomo.maven;


import org.apache.maven.plugins.annotations.Mojo;

import static org.apache.maven.plugins.annotations.LifecyclePhase.PACKAGE;

/**
 * Maven plugin to execute Socomo analysis.
 */
@Mojo(name = "analyze", defaultPhase = PACKAGE)
class AnalyzeMojo extends SocomoMojo {

	@Override
	void beforeExecute() {
		// noop
	}

	@Override
	void afterExecute() {
		// noop
	}
}
