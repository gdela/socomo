package pl.gdela.socomo.visualizer;

import static pl.gdela.socomo.visualizer.Asset.Type.STYLE;
import static pl.gdela.socomo.visualizer.Asset.Type.SCRIPT;

/**
 * An HTML asset, like stylesheet or javascript.
 */
class Asset {
	final Type type;
	final String url;

	private Asset(Type type, String url) {
		this.type = type;
		this.url = url;
	}

	static Asset style(String url) {
		return new Asset(STYLE, url);
	}

	static Asset script(String url) {
		return new Asset(SCRIPT, url);
	}

	enum Type { STYLE, SCRIPT }
}
