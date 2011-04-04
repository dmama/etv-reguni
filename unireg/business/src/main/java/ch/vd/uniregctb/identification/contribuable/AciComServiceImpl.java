package ch.vd.uniregctb.identification.contribuable;

import ch.vd.dfin.acicom.web.services.meldewesen.impl.ContenuMessage;
import ch.vd.dfin.acicom.web.services.meldewesen.impl.RecupererContenuMessage;
import ch.vd.uniregctb.webservice.acicom.AciComClient;
import ch.vd.uniregctb.webservice.acicom.AciComClientException;


public class AciComServiceImpl implements AciComService {

	private AciComClient aciComClient;

	public FichierOrigine getMessageFile(String businessId) throws AciComClientException {
		RecupererContenuMessage demande = new RecupererContenuMessage();
		demande.setMessageId(businessId);
		final ContenuMessage reponse = aciComClient.recupererMessage(demande);
		return new ContenuMessageWrapper(reponse);
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setAciComClient(AciComClient aciComClient) {
		this.aciComClient = aciComClient;
	}
}
