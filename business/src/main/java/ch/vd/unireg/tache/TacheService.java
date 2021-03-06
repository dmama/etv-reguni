package ch.vd.unireg.tache;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.metier.assujettissement.AssujettissementException;
import ch.vd.unireg.tache.sync.SynchronizeAction;
import ch.vd.unireg.tiers.Contribuable;
import ch.vd.unireg.tiers.ForFiscalPrincipal;
import ch.vd.unireg.tiers.ForFiscalSecondaire;
import ch.vd.unireg.type.ModeImposition;
import ch.vd.unireg.type.TypeTache;

/**
 * Service permettant la génération de tâches à la suite d'événements fiscaux
 */
public interface TacheService {

	/**
	 * Genere une tache à partir de l'ouverture d'un for principal
	 *
	 * @param contribuable         le contribuable sur lequel un for principal a été ouvert
	 * @param forFiscal            le for principal ouvert
	 * @param ancienModeImposition le mode d'imposition de l'ancien for principal actif
	 */
	void genereTacheDepuisOuvertureForPrincipal(Contribuable contribuable, ForFiscalPrincipal forFiscal, @Nullable ModeImposition ancienModeImposition);

	/**
	 * Genere une tache à partir de l'ouverture d'un for secondaire
	 *
	 * @param contribuable le contribuable sur lequel un for secondaire a été ouvert
	 * @param forFiscal    le for secondaire ouvert
	 */
	void genereTacheDepuisOuvertureForSecondaire(Contribuable contribuable, ForFiscalSecondaire forFiscal);

	/**
	 * Genere une tache à partir de la fermetrure d'un for principal
	 *
	 * @param contribuable le contribuable sur lequel un for principal a été fermé
	 * @param forFiscal    le for principal fermé
	 */
	void genereTacheDepuisFermetureForPrincipal(Contribuable contribuable, ForFiscalPrincipal forFiscal);

	/**
	 * Genere une tache à partir de la fermetrure d'un for secondaire
	 *
	 * @param contribuable le contribuable sur lequel un for secondaire a été fermé
	 * @param forFiscal    le for secondaire fermé
	 */
	void genereTacheDepuisFermetureForSecondaire(Contribuable contribuable, ForFiscalSecondaire forFiscal);


	/**
	 * Genère les tâches d'annulation de di eventuelles suite à l'annulation d'un for
	 *
	 * @param contribuable dont le for a été annulé
	 */
	void genereTachesDepuisAnnulationDeFor(Contribuable contribuable);

	/**
	 * Génère une tâche de contrôle de dossier sur le contribuable indiqué (s'il n'en existe pas déjà une encore en instance)
	 * @param contribuable sur lequel une tâche de contrôle de dossier doit être ajoutée
	 * @param commentaire commentaire optionnel à associer à la tâche
	 */
	void genereTacheControleDossier(Contribuable contribuable, String commentaire);

	/**
	 * @param oid l'id de l'office d'impôt courant de l'utilisateur
	 * @return le nombre total de tâches en instance non-échues à la date du jour.
	 */
	int getTachesEnInstanceCount(Integer oid);

	/**
	 * @param oid l'id de l'office d'impôt courant de l'utilisateur
	 * @return le nombre total de dossiers en instance non-échus à la date du jour.
	 */
	int getDossiersEnInstanceCount(Integer oid);

	/**
	 * [UNIREG-1218] Annule toutes les autres tâches associées au contribuable qui vient d'être annulé.
	 *
	 * @param contribuable le contribuable qui vient d'être annulé.
	 */
	void onAnnulationContribuable(Contribuable contribuable);

	/**
	 * Retourne la liste des tâches en instance par l'OID
	 *
	 * @param dateTraitement la date de traitement du processing.
	 * @param status         un status manager
	 * @return une liste de tâhes
	 * @throws Exception si quelque chose n'a pas fonctionné
	 */
	ListeTachesEnInstanceParOID produireListeTachesEnInstanceParOID(RegDate dateTraitement, StatusManager status) throws Exception;

	/**
	 * [UNIREG-2305] Cette méthode détermine toutes les actions nécessaires pour synchroniser les déclarations d'impôt du contribuable avec ses fors fiscaux.
	 *
	 * @param contribuable un contribuable
	 * @return une liste d'actions à entreprendre (peut être vide, mais pas nulle)
	 * @throws ch.vd.unireg.metier.assujettissement.AssujettissementException en cas d'incohérence des données sur les fors fiscaux qui empêche de calculer l'assujettissement.
	 */
	@NotNull
	List<SynchronizeAction> determineSynchronizeActionsForDIs(Contribuable contribuable) throws AssujettissementException;

	/**
	 * Synchronize les tâches d'envoi de déclarations (DI + Questionnaires SNC) pour tous les contribuables spécifiés (UNIREG-2305) (SIFISC-3141).
	 * <br>
	 * <b>Note:</b> cette méthode gère elle-même les transactions et doit donc être utilisée en context non-transactionel.
	 * <br>
	 * <b>Attention !</b> Ne pas appeler cette méthode manuellement : elle est appelée automatiquement depuis un intercepteur après le commit de la transaction.
	 *
	 * @param ctbIds les ids des contribuables dont les tâches doivent être synchronisées.
	 * @return une indication des opérations effectuées
	 */
	TacheSyncResults synchronizeTachesDeclarations(Collection<Long> ctbIds);

    /**
     * Annule les tâches obsolètes suite à la modification d'un contribuable
     * <br>
     * <b>Note:</b> cette méthode gère elle-même les transactions et doit donc être utilisée en context non-transactionel.
     * <br>
     * <b>Attention !</b> Ne pas appeler cette méthode manuellement : elle est appelée automatiquement depuis un intercepteur après le commit de la transaction.
     *
     * @param ctbIds les ids des contribuables dont les tâches obsolètes doivent être annulées.

     */
    void annuleTachesObsoletes(Collection<Long> ctbIds);

	/**
	 * Cette méthode met-à-jour les statistiques des tâches et des mouvements de dossier en instance
	 */
	void updateStats();

	/**
	 * @param typeTache un type de tâche
	 * @return la liste (triée par ordre alphabétique) des commentaires distincts utilisés pour ce type de tâche
	 */
	@NotNull
	List<String> getCommentairesDistincts(TypeTache typeTache);

	/**
	 * @return l'ensemble des numéros de collectivités administratives sur lesquelles des tâches existent
	 */
	@NotNull
	Set<Integer> getCollectivitesAdministrativesAvecTaches();
}
