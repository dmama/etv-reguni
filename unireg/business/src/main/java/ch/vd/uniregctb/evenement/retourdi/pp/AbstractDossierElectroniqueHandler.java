package ch.vd.uniregctb.evenement.retourdi.pp;

import java.util.Map;

public abstract class AbstractDossierElectroniqueHandler {

	private EvenementCediService evenementCediService;

	public void setEvenementCediService(EvenementCediService evenementCediService) {
		this.evenementCediService = evenementCediService;
	}

	protected void onEvent(EvenementCedi evt, Map<String, String> incomingHeaders) throws EvenementCediException {
		evenementCediService.onEvent(evt, incomingHeaders);
	}
}
