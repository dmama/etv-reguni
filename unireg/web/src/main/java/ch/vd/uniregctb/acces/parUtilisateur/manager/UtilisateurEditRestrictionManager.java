package ch.vd.uniregctb.acces.parUtilisateur.manager;

import org.springframework.transaction.annotation.Transactional;

import ch.vd.infrastructure.service.InfrastructureException;
import ch.vd.uniregctb.acces.parUtilisateur.view.RecapPersonneUtilisateurView;
import ch.vd.uniregctb.acces.parUtilisateur.view.UtilisateurEditRestrictionView;
import ch.vd.uniregctb.adresse.AdressesResolutionException;
import ch.vd.uniregctb.security.DroitAccesException;

public interface UtilisateurEditRestrictionManager {


	/**
	 * Alimente la vue du controller
	 * @param numeroPP
	 * @return
	 * @throws InfrastructureException
	 */
	public UtilisateurEditRestrictionView get(long noIndividuOperateur) throws InfrastructureException, AdressesResolutionException;


	/**
	 * Alimente la vue RecapPersonneUtilisateurView
	 *
	 * @param numeroPP
	 * @param noIndividuOperateur
	 * @return
	 * @throws InfrastructureException
	 * @throws AdressesResolutionException
	 */
	public RecapPersonneUtilisateurView get(Long numeroPP, Long noIndividuOperateur) throws InfrastructureException, AdressesResolutionException ;


	/**
	 * Annule une restriction
	 *
	 * @param idRestriction
	 */
	@Transactional(rollbackFor = Throwable.class)
	public void annulerRestriction( Long idRestriction) throws DroitAccesException ;

	/**
	 * Persiste le DroitAcces
	 * @param recapPersonneUtilisateurView
	 * @throws DroitAccesException TODO
	 */
	@Transactional(rollbackFor = Throwable.class)
	public void save(RecapPersonneUtilisateurView recapPersonneUtilisateurView) throws DroitAccesException ;

}
