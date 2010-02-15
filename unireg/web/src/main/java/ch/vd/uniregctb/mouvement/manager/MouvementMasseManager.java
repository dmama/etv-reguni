package ch.vd.uniregctb.mouvement.manager;

import java.util.List;

import org.apache.commons.lang.mutable.MutableLong;

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
	 * Trouve les mouvements de dossier qui satisfont aux critères donnés (avec pagination)
	 * @param view la vue où les critères de recherche sont remplis
	 * @param noCollAdmInitiatrice si non vide, la recherche ne s'effectue que sur les mouvements initiés par cette collectivité administrative
	 * @param paramPagination pagination à utiliser
	 * @param total En sortie, le nombre total de mouvements qui satifont aux critères   @return Liste paginée de mouvements (jamais null si la view n'est pas nulle)
	 */
	List<MouvementDetailView> find(MouvementMasseCriteriaView view, Integer noCollAdmInitiatrice, ParamPagination paramPagination, MutableLong total) throws InfrastructureException;

	/**
	 * Trouve tous les mouvements de dossier qui satisfont aux critères donnés
	 * @param criteria critères de recherche
	 * @return liste des mouvements trouvés, ou null si rien trouvé
	 */
	List<MouvementDetailView> find(MouvementDossierCriteria criteria) throws InfrastructureException;

	/**
	 * Met le mouvement donné par son ID dans l'état donné
	 * @param nouvelEtat nouvel état à associer au mouvement
	 * @param mvtId ID du mouvement de dossier dont l'état doit être modifié
	 */
	void changeEtat(EtatMouvementDossier nouvelEtat, long mvtId);

	/**
	 * Met les mouvement donnés par leur ID dans l'état donné
	 * @param nouvelEtat nouvel état à associer aux mouvements
	 * @param ids ID des mouvements de dossier dont l'état doit être modifié
	 */
	void changeEtat(EtatMouvementDossier nouvelEtat, long[] ids);

	/**
	 * Renvoie une liste des proto-borderaux imprimables
	 * @param noCollAdmInitiatrice si non-null, ne renvoie que les mouvements initiés par la collectivité administrative donnée
	 * @return la liste
	 */
	List<BordereauListElementView> getProtoBordereaux(Integer noCollAdmInitiatrice);

	/**
	 * Imprime un bordereau de mouvements de dossier avec les mouvements donnés par ID, et renvoie
	 * le flux binaire de données (PCL) du document à imprimer
	 */
	byte[] imprimerBordereau(long[] idsMouvement) throws EditiqueException;

	/**
	 * Renvoie les bordereaux de mouvements d'envois restant à réceptionner
	 * @param noCollAdmReceptrice si assigné, ne prend que les bordereaux d'envoi qui sont envoyés vers la collectivité administrative donnée
	 */
	List<BordereauEnvoiView> findBordereauxAReceptionner(Integer noCollAdmReceptrice);

	/**
	 * Renvoie les détails des mouvements composant ce bordereau d'envoi en vue de sa réception
	 * @param idBordereau ID technique du bordereau
	 * @return Détails du bordereau d'envoi
	 */
	BordereauEnvoiReceptionView getBordereauPourReception(long idBordereau)throws InfrastructureException;

	/**
	 * Génère les mouvements de réception associés aux mouvements d'envois donnés et passe
	 * les mouvements d'envoi à l'état "RECU_BORDEREAU"
	 * @param idsMouvements ID techniques des mouvements à traiter
	 */
	void receptionnerMouvementsEnvoi(long[] idsMouvements);

	/**
	 * Rafraîchit la vue du bordereau d'envoi après réception de quelques mouvements
	 * @param view view à rafraîchir
	 */
	void refreshView(BordereauEnvoiReceptionView view) throws InfrastructureException;
}
