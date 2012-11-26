package ch.vd.uniregctb.tiers.manager;

import org.springframework.transaction.annotation.Transactional;

import ch.vd.unireg.interfaces.infra.ServiceInfrastructureException;
import ch.vd.uniregctb.adresse.AdresseException;
import ch.vd.uniregctb.tiers.view.TiersEditView;

/**
 * Service à disposition du controller pour gérer un for fiscal
 * @author xcifde
 */
public interface ForFiscalManager {

	/**
	 * Reouverture du for
	 */
	@Transactional(rollbackFor = Throwable.class)
	public abstract void reouvrirFor(Long idFor);

	/**
	 * Charge les informations dans TiersView
	 * @return un objet TiersView
	 */
	@Transactional(readOnly = true)
	public TiersEditView getView(Long numero) throws AdresseException, ServiceInfrastructureException;
}
