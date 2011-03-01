package ch.vd.uniregctb.extraction;

import java.io.InputStream;

/**
 * Résultat Ok
 */
public class ExtractionResultOk extends ExtractionResult {

	private final InputStream stream;
	private final boolean interrupted;

	public ExtractionResultOk(InputStream stream, boolean interrupted) {
		this.stream = stream;
		this.interrupted = interrupted;
	}

	@Override
	public final State getSummary() {
		return State.OK;
	}

	public InputStream getStream() {
		return stream;
	}

	public boolean isInterrupted() {
		return interrupted;
	}

	public String toString() {
		return String.format("OK%s", interrupted ? " (interrompu)" : "");
	}
}
