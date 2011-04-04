package ch.vd.uniregctb.webservice.acicom;

@SuppressWarnings({"UnusedDeclaration"})
public class AciComClientDocumentNotFoundException extends AciComClientException {

	public AciComClientDocumentNotFoundException(String message, Throwable cause) {
		super(message, cause);
		setLibelleErreur("Document non trouve par le service ACICOM");
	}
}
