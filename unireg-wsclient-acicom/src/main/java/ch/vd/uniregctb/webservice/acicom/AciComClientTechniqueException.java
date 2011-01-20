package ch.vd.uniregctb.webservice.acicom;

public class AciComClientTechniqueException extends AciComClientException{
	public AciComClientTechniqueException(String message) {
		super(message);
		setLibelleErreur("Probleme de communication avec le service ACICOM");
		
	}

}
