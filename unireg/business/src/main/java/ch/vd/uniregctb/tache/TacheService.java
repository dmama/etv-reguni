package ch.vd.uniregctb.tache;

import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.ForFiscalSecondaire;
import ch.vd.uniregctb.type.ModeImposition;

/**
 * Service permettant la génération de tâches à la suite d'événements fiscaux
 *
 * @author xcifde
 *
 */
public interface TacheService {

	/**
	 * Genere une tache à partir de l'ouverture d'un for principal
	 *
	 * @param contribuable
	 * @param forFiscal
	 * @param ancienModeImposition
	 */
	@Transactional(rollbackFor = Throwable.class)
	public void genereTacheDepuisOuvertureForPrincipal(Contribuable contribuable, ForFiscalPrincipal forFiscal, ModeImposition ancienModeImposition);

	/**
	 * Genere une tache à partir de l'ouverture d'un for secondaire
	 *
	 * @param contribuable
	 * @param forFiscal
	 */
	@Transactional(rollbackFor = Throwable.class)
	public void genereTacheDepuisOuvertureForSecondaire(Contribuable contribuable, ForFiscalSecondaire forFiscal);

	/**
	 * Genere une tache à partir de la fermetrure d'un for principal
	 *
	 * @param contribuable
	 * @param forFiscal
	 */
	@Transactional(rollbackFor = Throwable.class)
	public void genereTacheDepuisFermetureForPrincipal(Contribuable contribuable, ForFiscalPrincipal forFiscal);

	/**
	 * Genere une tache à partir de la fermetrure d'un for secondaire
	 *
	 * @param contribuable
	 * @param forFiscal
	 */
	@Transactional(rollbackFor = Throwable.class)
	public void genereTacheDepuisFermetureForSecondaire(Contribuable contribuable, ForFiscalSecondaire forFiscal);


	/**
	 * Genère les tâches d'annulation de di eventuelles suite à l'annulation d'un for
	 *
	 * @param contribuable dont le for a été annulé
	 */
	@Transactional(rollbackFor = Throwable.class)
	public void genereTachesDepuisAnnulationDeFor(Contribuable contribuable);

	/**
	 * @param oid
	 *            l'id de l'office d'impôt courant de l'utilisateur
	 * @return le nombre total de tâches en instance non-échues à la date du jour.
	 */
	public int getTachesEnInstanceCount(Integer oid);

	/**
	 * @param oid
	 *            l'id de l'office d'impôt courant de l'utilisateur
	 * @return le nombre total de dossiers en instance non-échus à la date du jour.
	 */
	public int getDossiersEnInstanceCount(Integer oid);

	/**
	 * Annule toutes les autres tâches associées au contribuable qui vient d'être annulé.
	 *
	 * @param contribuable
	 *            le contribuable qui vient d'être annulé.
	 * @see UNIREG-1218
	 */

	public void onAnnulationContribuable(Contribuable contribuable);

	/** Retourne la liste des tâches en instance par l'OID
	 *
	 * @return
	 * @throws Exception
	 */

	public ListeTachesEnIsntanceParOID produireListeTachesEnIstanceParOID(RegDate dateTraitement,StatusManager status) throws Exception;
}
