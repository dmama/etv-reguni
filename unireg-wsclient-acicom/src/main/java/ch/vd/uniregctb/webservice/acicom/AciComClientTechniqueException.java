package ch.vd.uniregctb.webservice.acicom;

@SuppressWarnings({"UnusedDeclaration"})
public class AciComClientTechniqueException extends AciComClientException {

	public AciComClientTechniqueException() {
		setLibelleErreur("Probleme de communication avec le service ACICOM");
	}

	public AciComClientTechniqueException(String message) {
		super(message);
		setLibelleErreur("Probleme de communication avec le service ACICOM");
	}

	public AciComClientTechniqueException(String message, Throwable cause) {
		super(message, cause);
		setLibelleErreur("Probleme de communication avec le service ACICOM");
	}

	public AciComClientTechniqueException(Throwable cause) {
		super(cause);
		setLibelleErreur("Probleme de communication avec le service ACICOM");
	}
}
