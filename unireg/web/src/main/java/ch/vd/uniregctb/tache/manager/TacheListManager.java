package ch.vd.uniregctb.tache.manager;

import java.util.List;

import ch.vd.uniregctb.adresse.AdresseException;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.infrastructure.service.InfrastructureException;
import ch.vd.uniregctb.adresse.AdressesResolutionException;
import ch.vd.uniregctb.common.ParamPagination;
import ch.vd.uniregctb.declaration.DeclarationException;
import ch.vd.uniregctb.editique.EditiqueException;
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
	 * @throws InfrastructureException
	 * @throws AdressesResolutionException
	 */
	public List<TacheListView> find(TacheCriteriaView tacheCriteria, ParamPagination paramPagination) throws InfrastructureException, AdressesResolutionException;

	/**
	 * Recherche des nouveaux dossiers suivant certains critères
	 *
	 * @param dossierCriteria
	 * @return
	 * @throws InfrastructureException
	 * @throws AdressesResolutionException
	 */
	public List<NouveauDossierListView> find(NouveauDossierCriteriaView dossierCriteria) throws InfrastructureException, AdresseException;

	/**
	 * Recherche des nouveaux dossiers suivant certains critères
	 *
	 * @param dossierCriteria
	 * @param paramPagination
	 * @return
	 * @throws InfrastructureException
	 * @throws AdressesResolutionException
	 */
	public List<NouveauDossierListView> find(NouveauDossierCriteriaView dossierCriteria, ParamPagination paramPagination) throws InfrastructureException, AdresseException;

	/**
	 * Imprime les nouveaux dossiers
	 *
	 * @param nouveauDossierCriteriaView
	 * @return
	 */
	@Transactional(rollbackFor = Throwable.class)
	public String envoieImpressionLocalDossier(NouveauDossierCriteriaView nouveauDossierCriteriaView) throws EditiqueException, InfrastructureException;

	/**
	 * Imprime un nouveau dossier
	 * Partie reception
	 * @param lrEditView
	 */
	@Transactional(rollbackFor = Throwable.class)
	public byte[] recoitImpressionLocalDossier(String docID) throws DeclarationException ;

	/**
	 * Retourne le nombre de tache correspondant aux criteres
	 *
	 * @param criterion
	 * @return
	 * @throws InfrastructureException
	 */
	public int count(TacheCriteriaView criterion) throws InfrastructureException ;

	/**
	 * Retourne le nombre de nouveaux dossiers correspondant aux criteres
	 *
	 * @param nouveauDossierCriteriaView
	 * @return
	 * @throws InfrastructureException
	 */
	public int count(NouveauDossierCriteriaView nouveauDossierCriteriaView) throws InfrastructureException ;

	/**
	 * Passe la tâche à l'état TRAITE
	 *
	 * @param id
	 */
	@Transactional(rollbackFor = Throwable.class)
	public void traiteTache(Long id) ;

}
