package ch.vd.uniregctb.extraction;

import java.io.InputStream;

/**
 * Résultat Ok
 */
public class ExtractionResultOk extends ExtractionResult {

	private final InputStream stream;
	private final String mimeType;
	private final String filenameRadical;
	private final boolean interrupted;

	public ExtractionResultOk(InputStream stream, String mimeType, String filenameRadical, boolean interrupted) {
		this.stream = stream;
		this.mimeType = mimeType;
		this.filenameRadical = filenameRadical;
		this.interrupted = interrupted;
	}

	@Override
	public final State getSummary() {
		return State.OK;
	}

	public InputStream getStream() {
		return stream;
	}

	public String getMimeType() {
		return mimeType;
	}

	public String getFilenameRadical() {
		return filenameRadical;
	}

	public boolean isInterrupted() {
		return interrupted;
	}

	public String toString() {
		return String.format("OK%s", interrupted ? " (interrompu)" : "");
	}
}
