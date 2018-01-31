package ch.vd.uniregctb.tache.manager;

import java.util.List;

import org.springframework.transaction.annotation.Transactional;

import ch.vd.unireg.interfaces.infra.ServiceInfrastructureException;
import ch.vd.uniregctb.adresse.AdresseException;
import ch.vd.uniregctb.adresse.AdressesResolutionException;
import ch.vd.uniregctb.common.pagination.ParamPagination;
import ch.vd.uniregctb.editique.EditiqueException;
import ch.vd.uniregctb.editique.EditiqueResultat;
import ch.vd.uniregctb.tache.view.ImpressionNouveauxDossiersView;
import ch.vd.uniregctb.tache.view.NouveauDossierCriteriaView;
import ch.vd.uniregctb.tache.view.NouveauDossierListView;
import ch.vd.uniregctb.tache.view.TacheCriteriaView;
import ch.vd.uniregctb.tache.view.TacheListView;
import ch.vd.uniregctb.type.TypeTache;

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
	List<TacheListView> find(TacheCriteriaView tacheCriteria, ParamPagination paramPagination) throws ServiceInfrastructureException, AdressesResolutionException;

	/**
	 * Recherche des nouveaux dossiers suivant certains critères
	 *
	 * @param dossierCriteria
	 * @return
	 * @throws ServiceInfrastructureException
	 * @throws AdressesResolutionException
	 */
	@Transactional(readOnly = true)
	List<NouveauDossierListView> find(NouveauDossierCriteriaView dossierCriteria) throws ServiceInfrastructureException, AdresseException;

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
	List<NouveauDossierListView> find(NouveauDossierCriteriaView dossierCriteria, ParamPagination paramPagination) throws ServiceInfrastructureException, AdresseException;

	/**
	 * Imprime les nouveaux dossiers
	 */
	@Transactional(rollbackFor = Throwable.class)
	EditiqueResultat envoieImpressionLocalDossier(ImpressionNouveauxDossiersView view) throws EditiqueException;

	/**
	 * Retourne le nombre de tache correspondant aux criteres
	 *
	 * @param criterion
	 * @return
	 * @throws ServiceInfrastructureException
	 */
	@Transactional(readOnly = true)
	int count(TacheCriteriaView criterion) throws ServiceInfrastructureException ;

	/**
	 * Retourne le nombre de nouveaux dossiers correspondant aux criteres
	 *
	 * @param nouveauDossierCriteriaView
	 * @return
	 * @throws ServiceInfrastructureException
	 */
	@Transactional(readOnly = true)
	int count(NouveauDossierCriteriaView nouveauDossierCriteriaView) throws ServiceInfrastructureException ;

	/**
	 * Passe la tâche à l'état TRAITE
	 *
	 * @param id
	 */
	@Transactional(rollbackFor = Throwable.class)
	void traiteTache(Long id) ;

	/**
	 * @param typeTache le type de tâche
	 * @return les commentaires distincts (par ordre alphabétique) associés aux tâches (quel que soit leur état) de ce type
	 */
	@Transactional(readOnly = true)
	List<String> getCommentairesDistincts(TypeTache typeTache);
}
