package ch.vd.uniregctb.tiers.manager;

import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureException;
import ch.vd.uniregctb.adresse.AdresseException;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.ForFiscalAutreElementImposable;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.ForFiscalSecondaire;
import ch.vd.uniregctb.tiers.view.ForFiscalView;
import ch.vd.uniregctb.tiers.view.TiersEditView;
import ch.vd.uniregctb.type.MotifFor;

/**
 * Service à disposition du controller pour gérer un for fiscal
 * @author xcifde
 */
public interface ForFiscalManager {

	/**
	 * Recupere la vue ForFiscalView
	 */
	@Transactional(readOnly = true)
	public abstract ForFiscalView get(Long id) throws Exception;

	/**
	 * Cree une nouvelle vue ForFiscalView
	 */
	@Transactional(readOnly = true)
	public abstract ForFiscalView create(Long numeroCtb, boolean dpi);

	/**
	 * Annulation du for
	 */
	@Transactional(rollbackFor = Throwable.class)
	public abstract void annulerFor(Long idFor);


	/**
	 * Reouverture du for
	 */
	@Transactional(rollbackFor = Throwable.class)
	public abstract void reouvrirFor(Long idFor);

	/**
	 * Charge les informations dans TiersView
	 * @return un objet TiersView
	 */
	@Transactional(readOnly = true)
	public TiersEditView getView(Long numero) throws AdresseException, ServiceInfrastructureException;

	/**
	 * Ajoute un nouveau for fiscal.
	 * @param forFiscalView le form-backing object de l'écran d'ajout de for fiscal
	 * @return le for fiscal créé
	 */
	@Transactional(rollbackFor = Throwable.class)
	ForFiscal addFor(ForFiscalView forFiscalView);

	/**
	 * Met-à-jour un for fiscal préexistant.
	 * @param forFiscalView le form-backing object de l'écran de fermeture du for fiscal
	 * @return le for fiscal mis-à-jour ou <b>null</b> si aucun changement n'était nécessaire
	 */
	@Transactional(rollbackFor = Throwable.class)
	ForFiscal updateFor(ForFiscalView forFiscalView);

	/**
	 * Change le mode d'imposition d'un contribuable à partir d'une certaine date. Cette méthode ferme le for principal courant à la date de la veille du changement, et ouvre un nouveau for fiscal
	 * principal avec le mode d'imposition voulu.
	 * @param forFiscalView le form-backing object de l'écran de mise-à-jour du mode d'imposition
	 * @return le nouveau for fiscal créé avec le nouveau mode d'imposition.
	 */
	@Transactional(rollbackFor = Throwable.class)
	ForFiscal updateModeImposition(ForFiscalView forFiscalView);

	ForFiscalSecondaire updateForSecondaire(ForFiscalSecondaire ffs, RegDate dateOuverture, MotifFor motifOuverture, RegDate dateFermeture, MotifFor motifFermeture, int noOfsAutoriteFiscale);

	ForFiscalPrincipal updateForPrincipal(ForFiscalPrincipal ffp, RegDate dateFermeture, MotifFor motifFermeture, int noOfsAutoriteFiscale);

	ForFiscalAutreElementImposable updateForAutreElementImposable(ForFiscalAutreElementImposable ffaei, RegDate dateFermeture, MotifFor motifFermeture);
}
