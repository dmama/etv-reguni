package ch.vd.unireg.tiers.vignette;

public class UrlVersView {
	private final String name;
	private final String label;
	private final String appName;

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
