package ch.vd.uniregctb.tiers.manager;

import ch.vd.unireg.interfaces.infra.ServiceInfrastructureException;
import ch.vd.uniregctb.adresse.AdresseException;
import ch.vd.uniregctb.tiers.view.TiersEditView;

/**
 * Service à disposition du controller pour gérer un for fiscal
 * @author xcifde
 */
public interface ForFiscalManager {

	/**
	 * Charge les informations dans TiersView
	 * @return un objet TiersView
	 */
	TiersEditView getView(Long numero) throws AdresseException, ServiceInfrastructureException;
}
