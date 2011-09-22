package ch.vd.uniregctb.acces.parUtilisateur.manager;

import org.springframework.transaction.annotation.Transactional;

import ch.vd.uniregctb.acces.parUtilisateur.view.RecapPersonneUtilisateurView;
import ch.vd.uniregctb.acces.parUtilisateur.view.UtilisateurEditRestrictionView;
import ch.vd.uniregctb.adresse.AdresseException;
import ch.vd.uniregctb.adresse.AdressesResolutionException;
import ch.vd.uniregctb.extraction.ExtractionJob;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureException;
import ch.vd.uniregctb.security.DroitAccesException;

public interface UtilisateurEditRestrictionManager {


	/**
	 * Alimente la vue du controller
	 * @return
	 * @throws ServiceInfrastructureException
	 */
	@Transactional(readOnly = true)
	public UtilisateurEditRestrictionView get(long noIndividuOperateur) throws ServiceInfrastructureException, AdresseException;


	/**
	 * Alimente la vue RecapPersonneUtilisateurView
	 *
	 * @param numeroPP
	 * @param noIndividuOperateur
	 * @return
	 * @throws ServiceInfrastructureException
	 * @throws AdressesResolutionException
	 */
	@Transactional(readOnly = true)
	public RecapPersonneUtilisateurView get(Long numeroPP, Long noIndividuOperateur) throws ServiceInfrastructureException, AdressesResolutionException ;


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

	/**
	 * Demande l'export des droits d'accès d'un utilisateur
	 *
	 * @param operateurId l'id de l'operateur pour lequel on veux exporter les droits
	 * @return la demande d'extraction
	 */
	@Transactional(readOnly = true)
	public ExtractionJob exportListeDroitsAcces( Long operateurId);

}
