package ch.vd.unireg.tiers.manager;

import ch.vd.unireg.interfaces.infra.ServiceInfrastructureException;
import ch.vd.unireg.adresse.AdresseException;
import ch.vd.unireg.tiers.view.TiersEditView;

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
