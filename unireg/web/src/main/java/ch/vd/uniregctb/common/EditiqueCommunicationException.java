package ch.vd.uniregctb.common;

public class EditiqueCommunicationException extends RuntimeException {

	private static final long serialVersionUID = 7526086717346076337L;

	public EditiqueCommunicationException() {

	}

	public EditiqueCommunicationException(String message) {
		super(message);
	}

	public EditiqueCommunicationException(String message, Throwable cause) {
		super(message, cause);
	}

}
