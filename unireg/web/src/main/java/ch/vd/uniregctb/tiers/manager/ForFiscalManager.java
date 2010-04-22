package ch.vd.uniregctb.tiers.manager;

import ch.vd.uniregctb.adresse.AdresseException;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.infrastructure.service.InfrastructureException;
import ch.vd.uniregctb.adresse.AdressesResolutionException;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.ForFiscalDAO;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.view.ForFiscalView;
import ch.vd.uniregctb.tiers.view.TiersEditView;

/**
 * Service à disposition du controller pour gérer un for fiscal
 *
 * @author xcifde
 *
 */
public interface ForFiscalManager {

	/**
	 * Recupere la vue ForFiscalView
	 *
	 * @param id
	 * @return
	 */
	@Transactional(readOnly = true)
	public abstract ForFiscalView get(Long id) throws Exception;

	/**
	 * Cree une nouvelle vue ForFiscalView
	 *
	 * @param id
	 * @return
	 */
	@Transactional(readOnly = true)
	public abstract ForFiscalView create(Long numeroCtb, boolean dpi);

	/**
	 * Sauvegarde du for
	 *
	 * @param forFiscalView
	 * @return
	 */
	@Transactional(rollbackFor = Throwable.class)
	public abstract ForFiscal save(ForFiscalView forFiscalView);

	/**
	 * Annulation du for
	 *
	 * @param idFor
	 */
	@Transactional(rollbackFor = Throwable.class)
	public abstract void annulerFor(Long idFor);

	/**
	 * Charge les informations dans TiersView
	 *
	 * @param numero
	 * @return un objet TiersView
	 * @throws AdressesResolutionException
	 * @throws InfrastructureException
	 */
	@Transactional(readOnly = true)
	public TiersEditView getView(Long numero) throws AdresseException, InfrastructureException;
}
