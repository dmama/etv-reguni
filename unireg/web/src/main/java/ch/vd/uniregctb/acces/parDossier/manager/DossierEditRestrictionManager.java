package ch.vd.uniregctb.acces.parDossier.manager;

import org.springframework.transaction.annotation.Transactional;

import ch.vd.unireg.interfaces.infra.ServiceInfrastructureException;
import ch.vd.uniregctb.acces.parDossier.view.DossierEditRestrictionView;
import ch.vd.uniregctb.acces.parDossier.view.DroitAccesView;
import ch.vd.uniregctb.security.DroitAccesException;

/**
 * Interface qui gère le controller DossierEditRestrictionController
 *
 * @author xcifde
 *
 */
public interface DossierEditRestrictionManager {

	/**
	 * Alimente la vue du controller
	 * @param numeroPP
	 * @return
	 * @throws ServiceInfrastructureException
	 */
	@Transactional(readOnly = true)
	public DossierEditRestrictionView get(Long numeroPP)  throws ServiceInfrastructureException;

	/**
	 * Persiste un droit d'acces
	 * @param droitAccesView
	 */
	@Transactional(rollbackFor = Throwable.class)
	public void save(DroitAccesView droitAccesView) throws DroitAccesException;

	/**
	 * Annule une restriction
	 *
	 * @param idRestriction
	 */
	@Transactional(rollbackFor = Throwable.class)
	public void annulerRestriction(Long idRestriction) throws DroitAccesException ;
}
