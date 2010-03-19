package ch.vd.uniregctb.mouvement.manager;

import java.util.List;

import org.apache.commons.lang.mutable.MutableLong;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.infrastructure.service.InfrastructureException;
import ch.vd.uniregctb.common.ParamPagination;
import ch.vd.uniregctb.editique.EditiqueException;
import ch.vd.uniregctb.mouvement.EtatMouvementDossier;
import ch.vd.uniregctb.mouvement.MouvementDossierCriteria;
import ch.vd.uniregctb.mouvement.view.BordereauEnvoiReceptionView;
import ch.vd.uniregctb.mouvement.view.BordereauListElementView;
import ch.vd.uniregctb.mouvement.view.BordereauEnvoiView;
import ch.vd.uniregctb.mouvement.view.MouvementDetailView;
import ch.vd.uniregctb.mouvement.view.MouvementMasseCriteriaView;

/**
 * Interface du manager qui met à disposition du controlleur quelques
 * méthodes plus "métier"
 */
public interface MouvementMasseManager extends AbstractMouvementManager {

	/**
	 * Trouve les mouvements de dossiers qui satisfont aux critères donnés (avec pagination)
	 * @param view la vue où les critères de recherche sont remplis
	 * @param noCollAdmInitiatrice si non vide, la recherche ne s'effectue que sur les mouvements initiés par cette collectivité administrative
	 * @param paramPagination pagination à utiliser
	 * @param total En sortie, le nombre total de mouvements qui satifont aux critères   @return Liste paginée de mouvements (jamais null si la view n'est pas nulle)
	 */
	@Transactional(readOnly = true)
	List<MouvementDetailView> find(MouvementMasseCriteriaView view, Integer noCollAdmInitiatrice, ParamPagination paramPagination, MutableLong total) throws InfrastructureException;

	/**
	 * Trouve tous les mouvements de dossiers qui satisfont aux critères donnés
	 * @param criteria critères de recherche
	 * @return liste des mouvements trouvés, ou null si rien trouvé
	 */
	@Transactional(readOnly = true)
	List<MouvementDetailView> find(MouvementDossierCriteria criteria) throws InfrastructureException;

	/**
	 * Met le mouvement donné par son ID dans l'état donné
	 * @param nouvelEtat nouvel état à associer au mouvement
	 * @param mvtId ID du mouvement de dossier dont l'état doit être modifié
	 */
	@Transactional(rollbackFor = Throwable.class)
	void changeEtat(EtatMouvementDossier nouvelEtat, long mvtId);

	/**
	 * Met les mouvement donnés par leur ID dans l'état donné
	 * @param nouvelEtat nouvel état à associer aux mouvements
	 * @param ids ID des mouvements de dossiers dont l'état doit être modifié
	 */
	@Transactional(rollbackFor = Throwable.class)
	void changeEtat(EtatMouvementDossier nouvelEtat, long[] ids);

	/**
	 * Renvoie une liste des proto-borderaux imprimables
	 * @param noCollAdmInitiatrice si non-null, ne renvoie que les mouvements initiés par la collectivité administrative donnée
	 * @return la liste
	 */
	@Transactional(readOnly = true)
	List<BordereauListElementView> getProtoBordereaux(Integer noCollAdmInitiatrice);

	/**
	 * Effectue la demande d'impression d'un bordereau de mouvements de dossiers avec les mouvements donnés par ID, et renvoie
	 * l'identifiant à utiliser dans la méthode {@link #recevoirImpressionBordereau} pour récupérer le flux PCL
	 */
	@Transactional(rollbackFor = Throwable.class)
	String imprimerBordereau(long[] idsMouvement) throws EditiqueException;

	/**
	 * Réceptionne le flux éditique concernant l'impression du bordereau d'envoi de dossiers
	 */
	@Transactional(rollbackFor = Throwable.class)
	byte[] recevoirImpressionBordereau(String docId) throws EditiqueException;

	/**
	 * Annule le bordereau composé des mouvements dont les ID sont donnés ici
	 * @param idsMouvement ID techniques des mouvements considérés
	 */
	@Transactional(rollbackFor = Throwable.class)
	void annulerBordereau(long[] idsMouvement);

	/**
	 * Renvoie les bordereaux de mouvements d'envois restant à réceptionner
	 * @param noCollAdmReceptrice si assigné, ne prend que les bordereaux d'envoi qui sont envoyés vers la collectivité administrative donnée
	 */
	@Transactional(readOnly = true)
	List<BordereauEnvoiView> findBordereauxAReceptionner(Integer noCollAdmReceptrice);

	/**
	 * Renvoie les détails des mouvements composant ce bordereau d'envoi en vue de sa réception
	 * @param idBordereau ID technique du bordereau
	 * @return Détails du bordereau d'envoi
	 */
	@Transactional(readOnly = true)
	BordereauEnvoiReceptionView getBordereauPourReception(long idBordereau)throws InfrastructureException;

	/**
	 * Génère les mouvements de réception associés aux mouvements d'envois donnés et passe
	 * les mouvements d'envoi à l'état "RECU_BORDEREAU"
	 * @param idsMouvements ID techniques des mouvements à traiter
	 */
	@Transactional(rollbackFor = Throwable.class)
	void receptionnerMouvementsEnvoi(long[] idsMouvements);

	/**
	 * Rafraîchit la vue du bordereau d'envoi après réception de quelques mouvements
	 * @param view view à rafraîchir
	 */
	@Transactional(readOnly = true)
	void refreshView(BordereauEnvoiReceptionView view) throws InfrastructureException;
}
