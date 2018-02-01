package ch.vd.uniregctb.extraction;

/**
 * Résultat en erreur
 */
public class ExtractionResultError extends ExtractionResult {

	private final String msg;
	private final Throwable cause;

	public ExtractionResultError(String msg, Throwable cause) {
		this.msg = msg;
		this.cause = cause;
	}

	public ExtractionResultError(String msg) {
		this(msg, null);
	}

	public ExtractionResultError(Throwable cause) {
		this.msg = cause.getMessage() != null ? cause.getMessage() : cause.getClass().getName();
		this.cause = cause;
	}

	@Override
	public final State getSummary() {
		return State.ERROR;
	}

	public String getMsg() {
		return msg;
	}

	public Throwable getThrowableCause() {
		return cause;
	}

	public String toString() {
		return String.format("Erreur : %s", msg);
	}
}
