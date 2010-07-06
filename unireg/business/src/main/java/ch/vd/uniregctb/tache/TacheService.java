package ch.vd.uniregctb.tache;

import java.util.List;

import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.metier.assujettissement.AssujettissementException;
import ch.vd.uniregctb.tache.sync.SynchronizeAction;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.ForFiscalSecondaire;
import ch.vd.uniregctb.type.ModeImposition;

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
	@Transactional(rollbackFor = Throwable.class)
	void genereTacheDepuisOuvertureForPrincipal(Contribuable contribuable, ForFiscalPrincipal forFiscal, ModeImposition ancienModeImposition);

	/**
	 * Genere une tache à partir de l'ouverture d'un for secondaire
	 *
	 * @param contribuable le contribuable sur lequel un for secondaire a été ouvert
	 * @param forFiscal    le for secondaire ouvert
	 */
	@Transactional(rollbackFor = Throwable.class)
	void genereTacheDepuisOuvertureForSecondaire(Contribuable contribuable, ForFiscalSecondaire forFiscal);

	/**
	 * Genere une tache à partir de la fermetrure d'un for principal
	 *
	 * @param contribuable le contribuable sur lequel un for principal a été fermé
	 * @param forFiscal    le for principal fermé
	 */
	@Transactional(rollbackFor = Throwable.class)
	void genereTacheDepuisFermetureForPrincipal(Contribuable contribuable, ForFiscalPrincipal forFiscal);

	/**
	 * Genere une tache à partir de la fermetrure d'un for secondaire
	 *
	 * @param contribuable le contribuable sur lequel un for secondaire a été fermé
	 * @param forFiscal    le for secondaire fermé
	 */
	@Transactional(rollbackFor = Throwable.class)
	void genereTacheDepuisFermetureForSecondaire(Contribuable contribuable, ForFiscalSecondaire forFiscal);


	/**
	 * Genère les tâches d'annulation de di eventuelles suite à l'annulation d'un for
	 *
	 * @param contribuable dont le for a été annulé
	 */
	@Transactional(rollbackFor = Throwable.class)
	void genereTachesDepuisAnnulationDeFor(Contribuable contribuable);

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
	ListeTachesEnIsntanceParOID produireListeTachesEnIstanceParOID(RegDate dateTraitement, StatusManager status) throws Exception;

	/**
	 * [UNIREG-2305] Cette méthode détermine toutes les actions nécessaires pour synchroniser les déclarations d'impôt du contribuable avec ses fors fiscaux.
	 *
	 * @param contribuable un contribuable
	 * @return une liste d'actions à entreprendre
	 * @throws ch.vd.uniregctb.metier.assujettissement.AssujettissementException en cas d'incohérence des données sur les fors fiscaux qui empêche de calculer l'assujettissement.
	 */
	List<SynchronizeAction> determineSynchronizeActionsForDIs(Contribuable contribuable) throws AssujettissementException;

	/**
	 * [UNIREG-2305] Cette méthode génére les tâches d'envoi de DIs ou les tâches d'annulation de DIs qui conviennent suite à la modification des fors fiscaux d'un contribuable. Les tâches de
	 * manipulation de DIs en instance sont inspectées et annulées si nécessaire. Toutes les périodes fiscales sont traitées automatiquement.
	 * <p>
	 * <b>Attention !</b> Ne pas appeler cette méthode manuellement : elle est appelée automatiquement depuis un intercepteur après le commit de la transaction.
	 *
	 * @param contribuable le contribuable sur lequel les tâches relatives aux DIs doivent être générées.
	 */
	void synchronizeTachesDIs(Contribuable contribuable);

	/**
	 * Cette méthode met-à-jour les statistiques des tâches et des mouvements de dossier en instance
	 */
	void updateStats();
}
