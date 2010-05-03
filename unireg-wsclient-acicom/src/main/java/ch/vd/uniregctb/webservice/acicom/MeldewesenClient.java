package ch.vd.uniregctb.webservice.acicom;

import ch.vd.dfin.acicom.web.services.meldewesen.impl.AciComException_Exception;
import ch.vd.dfin.acicom.web.services.meldewesen.impl.ContenuMessage;
import ch.vd.dfin.acicom.web.services.meldewesen.impl.DocumentNotFoundException_Exception;
import ch.vd.dfin.acicom.web.services.meldewesen.impl.RecupererContenuMessage;
import ch.vd.dfin.acicom.web.services.meldewesen.impl.RecupererContenuMessageResponse;

public interface MeldewesenClient {

	public ContenuMessage recupererMessage(RecupererContenuMessage infosMessage) throws AciComException_Exception, DocumentNotFoundException_Exception;
}
