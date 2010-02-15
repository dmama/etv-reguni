package ch.vd.uniregctb.common;

public class EditiqueCommunicationException extends RuntimeException {

	private static final long serialVersionUID = 2690032533236698709L;

	private final String docId;

	public EditiqueCommunicationException(String message, String docId) {
		super(message);
		this.docId = docId;
	}

	@Override
	public String getMessage() {
		return String.format("%s (%s)", super.getMessage(), docId);
	}
}