package ch.vd.uniregctb.admin;


public class LoadableFileDescription {

	private final String description;
	private final String filename;

	public LoadableFileDescription(String description, String filename) {

		this.description = description;
		this.filename = filename;
	}

	public String getDescription() {
		return description;
	}

	public String getFilename() {
		return filename;
	}

}
