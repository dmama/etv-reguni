package ch.vd.uniregctb.identification.contribuable;

import ch.vd.dfin.acicom.web.services.meldewesen.impl.AciComException_Exception;
import ch.vd.dfin.acicom.web.services.meldewesen.impl.ContenuMessage;
import ch.vd.dfin.acicom.web.services.meldewesen.impl.DocumentNotFoundException_Exception;
import ch.vd.dfin.acicom.web.services.meldewesen.impl.RecupererContenuMessage;
import ch.vd.uniregctb.identification.contribuable.ContenuMessageWrapper;
import ch.vd.uniregctb.identification.contribuable.FichierOrigine;
import ch.vd.uniregctb.webservice.acicom.AciComClient;
import ch.vd.uniregctb.webservice.acicom.AciComClientException;
import ch.vd.uniregctb.webservice.acicom.AciComClientImpl;


public class AciComServiceImpl implements AciComService {

	private AciComClient aciComClient;

	public FichierOrigine getMessageFile(String businessId) throws AciComClientException {
	

		RecupererContenuMessage demande = new RecupererContenuMessage();
		demande.setMessageId(businessId);
		final ContenuMessage	reponse = aciComClient.recupererMessage(demande);
		return new ContenuMessageWrapper(reponse);

	}

	public AciComClient getAciComClient() {
		return aciComClient;
	}

	public void setAciComClient(AciComClient aciComClient) {
		this.aciComClient = aciComClient;
	}
}
