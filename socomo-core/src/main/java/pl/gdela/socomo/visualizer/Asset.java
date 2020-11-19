package pl.gdela.socomo.visualizer;

import static pl.gdela.socomo.visualizer.Asset.Type.SCRIPT;
import static pl.gdela.socomo.visualizer.Asset.Type.SCRIPT_CONTENT;
import static pl.gdela.socomo.visualizer.Asset.Type.STYLE;
import static pl.gdela.socomo.visualizer.Asset.Type.STYLE_CONTENT;

/**
 * An HTML asset, like stylesheet or javascript.
 */
class Asset {
	final Type type;
	String url;
	String content;

	private Asset(Type type) {
		this.type = type;
	}

	static Asset style(String url) {
		return new Asset(STYLE).withUrl(url);
	}

	static Asset script(String url) {
		return new Asset(SCRIPT).withUrl(url);
	}

	static Asset asyncScript(String url) {
		return new Asset(Type.ASYNC_SCRIPT).withUrl(url);
	}

	static Asset styleContent(String content) {
		return new Asset(STYLE_CONTENT).withContent(content);
	}

	static Asset scriptContent(String content) {
		return new Asset(SCRIPT_CONTENT).withContent(content);
	}

	private Asset withUrl(String url) {
		this.url = url;
		return this;
	}

	private Asset withContent(String content) {
		this.content = content;
		return this;
	}

	enum Type { STYLE, SCRIPT, ASYNC_SCRIPT, STYLE_CONTENT, SCRIPT_CONTENT }
}
