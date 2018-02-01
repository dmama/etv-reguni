package ch.vd.unireg.tiers.vignette;

public class ActionView {
	private final String label;
	private final String url;

	public ActionView(String label, String url) {
		this.label = label;
		this.url = url;
	}

	public String getLabel() {
		return label;
	}

	public String getUrl() {
		return url;
	}
}
