package ch.vd.uniregctb.tiers.vignette;

public class UrlVersView {
	private String name;
	private String label;
	private String appName;

	public UrlVersView(String name, String label, String appName) {
		this.name = name;
		this.label = label;
		this.appName = appName;
	}

	public String getName() {
		return name;
	}

	public String getLabel() {
		return label;
	}

	public String getAppName() {
		return appName;
	}
}
