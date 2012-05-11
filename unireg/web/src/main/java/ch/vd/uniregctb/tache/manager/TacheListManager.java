package ch.vd.uniregctb.tache.manager;

import java.util.List;

import org.springframework.transaction.annotation.Transactional;

import ch.vd.unireg.interfaces.infra.ServiceInfrastructureException;
import ch.vd.uniregctb.adresse.AdresseException;
import ch.vd.uniregctb.adresse.AdressesResolutionException;
import ch.vd.uniregctb.common.ParamPagination;
import ch.vd.uniregctb.editique.EditiqueException;
import ch.vd.uniregctb.editique.EditiqueResultat;
import ch.vd.uniregctb.tache.view.NouveauDossierCriteriaView;
import ch.vd.uniregctb.tache.view.NouveauDossierListView;
import ch.vd.uniregctb.tache.view.TacheCriteriaView;
import ch.vd.uniregctb.tache.view.TacheListView;

/**
 * Manager de recherche de taches
 *
 * @author xcifde
 *
 */
public interface TacheListManager {

	/**
	 * Recherche des tâches suivant certains criteres
	 *
	 * @param tacheCriteria
	 * @param paramPagination
	 * @return
	 * @throws ServiceInfrastructureException
	 * @throws AdressesResolutionException
	 */
	@Transactional(readOnly = true)
	public List<TacheListView> find(TacheCriteriaView tacheCriteria, ParamPagination paramPagination) throws ServiceInfrastructureException, AdressesResolutionException;

	/**
	 * Recherche des nouveaux dossiers suivant certains critères
	 *
	 * @param dossierCriteria
	 * @return
	 * @throws ServiceInfrastructureException
	 * @throws AdressesResolutionException
	 */
	@Transactional(readOnly = true)
	public List<NouveauDossierListView> find(NouveauDossierCriteriaView dossierCriteria) throws ServiceInfrastructureException, AdresseException;

	/**
	 * Recherche des nouveaux dossiers suivant certains critères
	 *
	 * @param dossierCriteria
	 * @param paramPagination
	 * @return
	 * @throws ServiceInfrastructureException
	 * @throws AdressesResolutionException
	 */
	@Transactional(readOnly = true)
	public List<NouveauDossierListView> find(NouveauDossierCriteriaView dossierCriteria, ParamPagination paramPagination) throws ServiceInfrastructureException, AdresseException;

	/**
	 * Imprime les nouveaux dossiers
	 *
	 * @param nouveauDossierCriteriaView
	 * @return
	 */
	@Transactional(rollbackFor = Throwable.class)
	public EditiqueResultat envoieImpressionLocalDossier(NouveauDossierCriteriaView nouveauDossierCriteriaView) throws EditiqueException;

	/**
	 * Retourne le nombre de tache correspondant aux criteres
	 *
	 * @param criterion
	 * @return
	 * @throws ServiceInfrastructureException
	 */
	@Transactional(readOnly = true)
	public int count(TacheCriteriaView criterion) throws ServiceInfrastructureException ;

	/**
	 * Retourne le nombre de nouveaux dossiers correspondant aux criteres
	 *
	 * @param nouveauDossierCriteriaView
	 * @return
	 * @throws ServiceInfrastructureException
	 */
	@Transactional(readOnly = true)
	public int count(NouveauDossierCriteriaView nouveauDossierCriteriaView) throws ServiceInfrastructureException ;

	/**
	 * Passe la tâche à l'état TRAITE
	 *
	 * @param id
	 */
	@Transactional(rollbackFor = Throwable.class)
	public void traiteTache(Long id) ;

}
