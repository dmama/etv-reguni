package ch.vd.uniregctb.tiers.manager;

import org.springframework.transaction.annotation.Transactional;

import ch.vd.unireg.interfaces.infra.ServiceInfrastructureException;
import ch.vd.uniregctb.adresse.AdresseException;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.view.ForFiscalView;
import ch.vd.uniregctb.tiers.view.TiersEditView;

/**
 * Service à disposition du controller pour gérer un for fiscal
 * @author xcifde
 */
public interface ForFiscalManager {

	/**
	 * Recupere la vue ForFiscalView
	 */
	@Transactional(readOnly = true)
	public abstract ForFiscalView get(Long id) throws Exception;

	/**
	 * Cree une nouvelle vue ForFiscalView
	 */
	@Transactional(readOnly = true)
	public abstract ForFiscalView create(Long numeroCtb, boolean dpi);

	/**
	 * Annulation du for
	 */
	@Transactional(rollbackFor = Throwable.class)
	public abstract void annulerFor(Long idFor);


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

	/**
	 * Change le mode d'imposition d'un contribuable à partir d'une certaine date. Cette méthode ferme le for principal courant à la date de la veille du changement, et ouvre un nouveau for fiscal
	 * principal avec le mode d'imposition voulu.
	 * @param forFiscalView le form-backing object de l'écran de mise-à-jour du mode d'imposition
	 * @return le nouveau for fiscal créé avec le nouveau mode d'imposition.
	 */
	@Transactional(rollbackFor = Throwable.class)
	ForFiscal updateModeImposition(ForFiscalView forFiscalView);
}
