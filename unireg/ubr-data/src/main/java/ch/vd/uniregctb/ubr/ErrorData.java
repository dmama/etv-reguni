package ch.vd.uniregctb.ubr;

/**
 * Classe exposée en cas de retour d'un message d'erreur
 */
public class ErrorData {

	private String errorMessage;

	public ErrorData() {
	}

	public ErrorData(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}
}
