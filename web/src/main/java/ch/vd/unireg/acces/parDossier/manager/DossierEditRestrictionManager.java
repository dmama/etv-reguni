package ch.vd.unireg.acces.parDossier.manager;

import org.springframework.transaction.annotation.Transactional;

import ch.vd.unireg.acces.parDossier.view.DossierEditRestrictionView;
import ch.vd.unireg.acces.parDossier.view.DroitAccesView;
import ch.vd.unireg.interfaces.infra.InfrastructureException;
import ch.vd.unireg.security.DroitAccesException;

/**
 * Interface qui gère le controller DossierEditRestrictionController
 *
 * @author xcifde
 *
 */
public interface DossierEditRestrictionManager {

	/**
	 * Alimente la vue du controller
	 * @param numeroTiers
	 * @return
	 * @throws InfrastructureException
	 */
	@Transactional(readOnly = true)
	DossierEditRestrictionView get(Long numeroTiers)  throws InfrastructureException;

	/**
	 * Persiste un droit d'acces
	 * @param droitAccesView
	 */
	@Transactional(rollbackFor = Throwable.class)
	void save(DroitAccesView droitAccesView) throws DroitAccesException;

	/**
	 * Annule une restriction
	 *
	 * @param idRestriction
	 */
	@Transactional(rollbackFor = Throwable.class)
	void annulerRestriction(Long idRestriction) throws DroitAccesException ;
}
