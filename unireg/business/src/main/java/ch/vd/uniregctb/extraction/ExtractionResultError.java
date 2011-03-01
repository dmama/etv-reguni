package ch.vd.uniregctb.extraction;

/**
 * Résultat en erreur
 */
public class ExtractionResultError extends ExtractionResult {

	private final String msg;
	private final Exception e;

	public ExtractionResultError(String msg, Exception e) {
		this.msg = msg;
		this.e = e;
	}

	public ExtractionResultError(String msg) {
		this(msg, null);
	}

	public ExtractionResultError(Exception e) {
		this.msg = e.getMessage() != null ? e.getMessage() : e.getClass().getName();
		this.e = e;
	}

	@Override
	public final State getSummary() {
		return State.ERROR;
	}

	public String getMsg() {
		return msg;
	}

	public Exception getException() {
		return e;
	}

	public String toString() {
		return String.format("Erreur : %s", msg);
	}
}
