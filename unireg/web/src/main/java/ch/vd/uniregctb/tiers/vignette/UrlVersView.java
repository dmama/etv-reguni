package ch.vd.uniregctb.tiers.vignette;

public class UrlVersView {
	private String name;
	private String label;
	private String url;

	public UrlVersView(String name, String label, String url) {
		this.name = name;
		this.label = label;
		this.url = url;
	}

	public String getName() {
		return name;
	}

	public String getLabel() {
		return label;
	}

	public String getUrl() {
		return url;
	}
}
