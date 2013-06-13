package ch.vd.uniregctb.tiers.vignette;

public class ActionView {
	private String label;
	private String url;

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
