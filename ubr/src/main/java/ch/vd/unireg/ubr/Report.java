package ch.vd.unireg.ubr;

import java.io.IOException;
import java.io.InputStream;

public class Report implements AutoCloseable {

	private final InputStream content;
	private final String fileName;

	public Report(InputStream content, String fileName) {
		this.content = content;
		this.fileName = fileName;
	}

	public InputStream getContent() {
		return content;
	}

	public String getFileName() {
		return fileName;
	}

	@Override
	public void close() throws IOException {
		if (content != null) {
			content.close();
		}
	}
}
