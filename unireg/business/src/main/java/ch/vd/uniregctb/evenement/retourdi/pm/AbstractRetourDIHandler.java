package ch.vd.uniregctb.evenement.retourdi.pm;

import java.util.Map;

import ch.vd.uniregctb.jms.EsbBusinessException;

/**
 * Classe de base des traitements de retour des DI PM
 */
public abstract class AbstractRetourDIHandler {

	private RetourDIPMService retourService;

	public void setRetourService(RetourDIPMService retourService) {
		this.retourService = retourService;
	}

	protected void traiterRetour(RetourDI retour, Map<String, String> headers) throws EsbBusinessException {
		retourService.traiterRetour(retour, headers);
	}
}
