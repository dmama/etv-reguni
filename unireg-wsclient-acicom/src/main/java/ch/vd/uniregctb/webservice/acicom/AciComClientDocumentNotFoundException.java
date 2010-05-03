package ch.vd.uniregctb.webservice.acicom;

public class AciComClientDocumentNotFoundException extends AciComClientException {
	public AciComClientDocumentNotFoundException(String message) {
		super(message);
		setLibelleErreur("Document non trouve par le service ACICOM");
	}

}
