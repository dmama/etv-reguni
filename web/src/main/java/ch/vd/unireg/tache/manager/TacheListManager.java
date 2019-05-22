package ch.vd.unireg.tache.manager;

import java.util.List;

import org.jetbrains.annotations.Nullable;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.unireg.adresse.AdresseException;
import ch.vd.unireg.adresse.AdressesResolutionException;
import ch.vd.unireg.common.pagination.ParamPagination;
import ch.vd.unireg.editique.EditiqueException;
import ch.vd.unireg.editique.EditiqueResultat;
import ch.vd.unireg.interfaces.infra.InfrastructureException;
import ch.vd.unireg.tache.view.ImpressionNouveauxDossiersView;
import ch.vd.unireg.tache.view.NouveauDossierCriteriaView;
import ch.vd.unireg.tache.view.NouveauDossierListView;
import ch.vd.unireg.tache.view.TacheCriteriaView;
import ch.vd.unireg.tache.view.TacheListView;
import ch.vd.unireg.tiers.Contribuable;
import ch.vd.unireg.type.TypeTache;

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
	@Transactional(readOnly = true)
	List<TacheListView> find(TacheCriteriaView tacheCriteria, ParamPagination paramPagination) throws InfrastructureException, AdressesResolutionException;

	/**
	 * Recherche des nouveaux dossiers suivant certains critères
	 *
	 * @param dossierCriteria
	 * @return
	 * @throws InfrastructureException
	 * @throws AdressesResolutionException
	 */
	@Transactional(readOnly = true)
	List<NouveauDossierListView> find(NouveauDossierCriteriaView dossierCriteria) throws InfrastructureException, AdresseException;

	/**
	 * Recherche des nouveaux dossiers suivant certains critères
	 *
	 * @param dossierCriteria
	 * @param paramPagination
	 * @return
	 * @throws InfrastructureException
	 * @throws AdressesResolutionException
	 */
	@Transactional(readOnly = true)
	List<NouveauDossierListView> find(NouveauDossierCriteriaView dossierCriteria, ParamPagination paramPagination) throws InfrastructureException, AdresseException;

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
	 * @throws InfrastructureException
	 */
	@Transactional(readOnly = true)
	int count(TacheCriteriaView criterion) throws InfrastructureException;

	/**
	 * Retourne le nombre de nouveaux dossiers correspondant aux criteres
	 *
	 * @param nouveauDossierCriteriaView
	 * @return
	 * @throws InfrastructureException
	 */
	@Transactional(readOnly = true)
	int count(NouveauDossierCriteriaView nouveauDossierCriteriaView) throws InfrastructureException;

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

	@Nullable
	Contribuable getContribuableFromTache(long tacheId);
}
