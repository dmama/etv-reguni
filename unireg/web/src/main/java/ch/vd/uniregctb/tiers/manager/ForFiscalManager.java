package ch.vd.uniregctb.tiers.manager;

import org.springframework.transaction.annotation.Transactional;
import org.springmodules.xt.ajax.component.Component;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adresse.AdresseException;
import ch.vd.uniregctb.adresse.AdressesResolutionException;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureException;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.view.ForFiscalView;
import ch.vd.uniregctb.tiers.view.TiersEditView;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;

/**
 * Service à disposition du controller pour gérer un for fiscal
 *
 * @author xcifde
 *
 */
public interface ForFiscalManager {

	/**
	 * Recupere la vue ForFiscalView
	 *
	 * @param id
	 * @return
	 */
	@Transactional(readOnly = true)
	public abstract ForFiscalView get(Long id) throws Exception;

	/**
	 * Cree une nouvelle vue ForFiscalView
	 *
	 * @param id
	 * @return
	 */
	@Transactional(readOnly = true)
	public abstract ForFiscalView create(Long numeroCtb, boolean dpi);

	/**
	 * Annulation du for
	 *
	 * @param idFor
	 */
	@Transactional(rollbackFor = Throwable.class)
	public abstract void annulerFor(Long idFor);

	/**
	 * Charge les informations dans TiersView
	 *
	 * @param numero
	 * @return un objet TiersView
	 * @throws AdressesResolutionException
	 * @throws ServiceInfrastructureException
	 */
	@Transactional(readOnly = true)
	public TiersEditView getView(Long numero) throws AdresseException, ServiceInfrastructureException;

	/**
	 * Ajoute un nouveau for fiscal.
	 *
	 * @param forFiscalView le form-backing object de l'écran d'ajout de for fiscal
	 * @return le for fiscal créé
	 */
	@Transactional(rollbackFor = Throwable.class)
	ForFiscal addFor(ForFiscalView forFiscalView);

	/**
	 * Met-à-jour un for fiscal préexistant.
	 *
	 * @param forFiscalView le form-backing object de l'écran de fermeture du for fiscal
	 * @return le for fiscal mis-à-jour ou <b>null</b> si aucun changement n'était nécessaire
	 */
	@Transactional(rollbackFor = Throwable.class)
	ForFiscal updateFor(ForFiscalView forFiscalView);

	/**
	 * Change le mode d'imposition d'un contribuable à partir d'une certaine date. Cette méthode ferme le for principal courant à la date de la veille du changement, et ouvre un nouveau for fiscal
	 * principal avec le mode d'imposition voulu.
	 *
	 * @param forFiscalView le form-backing object de l'écran de mise-à-jour du mode d'imposition
	 * @return le nouveau for fiscal créé avec le nouveau mode d'imposition.
	 */
	@Transactional(rollbackFor = Throwable.class)
	ForFiscal updateModeImposition(ForFiscalView forFiscalView);

	/**
	 * Construite le composant Ajax contenant les actions de synchronisation qui seront générées si le for fiscal spécifié est fermé.
	 *
	 * @param forId                l'id d'un for fiscal
	 * @param dateOuverture        la date d'ouverture du for fiscal
	 * @param motifOuverture       le motif d'ouverture du for fiscal
	 * @param dateFermeture        la date de fermeture du for fiscal
	 * @param motifFermeture       le motif de fermeture du for fiscal
	 * @param noOfsAutoriteFiscale @return un composant Ajax ou <b>null</b> si aucune action ne sera générée.
	 * @return le composant Ajax qui représente les tâches de synchronisation; ou <b>null</b> si aucune tâche n'est générée.
	 */
	Component buildSynchronizeActionsTableSurModificationDeFor(long forId, RegDate dateOuverture, MotifFor motifOuverture, RegDate dateFermeture, MotifFor motifFermeture, int noOfsAutoriteFiscale);

	/**
	 * Construite le composant Ajax contenant les actions de synchronisation qui seront générées si le mode d'imposition d'un contribuable est changé.
	 *
	 * @param forId           l'id du for fiscal principal actif d'un contribuable
	 * @param dateChangement  la date du changement du mode d'imposition
	 * @param modeImposition  le nouveau mode d'imposition
	 * @param motifChangement le motif de changement du mode d'imposition
	 * @return un composant Ajax ou <b>null</b> si aucune action ne sera générée.
	 */
	Component buildSynchronizeActionsTableSurModificationDuModeImposition(long forId, RegDate dateChangement, ModeImposition modeImposition, MotifFor motifChangement);
}
