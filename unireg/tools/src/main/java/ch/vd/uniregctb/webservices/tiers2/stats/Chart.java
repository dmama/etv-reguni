package ch.vd.uniregctb.webservices.tiers2.stats;

class Chart {
	private final String url;
	private final int width;
	private final int height;

	Chart(String url, int width, int height) {
		this.url = url;
		this.width = width;
		this.height = height;
	}

	public String getUrl() {
		return url;
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}
}
