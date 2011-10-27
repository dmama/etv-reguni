package ch.vd.uniregctb.webservice.acicom;

import ch.vd.dfin.acicom.web.services.meldewesen.impl.ContenuMessage;
import ch.vd.dfin.acicom.web.services.meldewesen.impl.RecupererContenuMessage;

@SuppressWarnings({"UnusedDeclaration"})
public interface AciComClient {

	public ContenuMessage recupererMessage(RecupererContenuMessage infosMessage) throws AciComClientException;
}
