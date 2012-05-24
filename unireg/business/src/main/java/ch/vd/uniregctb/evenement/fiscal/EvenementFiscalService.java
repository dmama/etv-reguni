package ch.vd.uniregctb.evenement.fiscal;

import java.util.Collection;

import org.jetbrains.annotations.Nullable;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.DeclarationImpotSource;
import ch.vd.uniregctb.evenement.EvenementFiscal;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;

/**
 * Interface du service des événement fiscaux
 *
 * @author xcicfh (last modified by $Author: $ @ $Date: $)
 * @version $Revision: $
 */
public interface EvenementFiscalService {


	/**
	 * Retourne la liste des événements fiscaux pour un tiers.
	 * @param tiers Tiers.
	 * @return  Retourne la liste des événements fiscaux pour un tiers.
	 */
	Collection<EvenementFiscal> getEvenementsFiscaux(Tiers tiers) ;

	/**
	 * Publie un événement fiscal
	 * @param evenementFiscal
	 * @throws Exception
	 */
	void publierEvenementFiscal(EvenementFiscal evenementFiscal);

	/**
	 * Publie un événement fiscal de type 'Ouverture de for'
	 * @param tiers
	 * @param dateEvenement
	 * @param motifFor motif d'ouverture du for
	 * @param id du for
	 * @throws Exception
	 */
	public void publierEvenementFiscalOuvertureFor(Tiers tiers, RegDate dateEvenement, @Nullable MotifFor motifFor, Long id) ;

	/**
	 * Publie un événement fiscal de type 'Fermeture de for'
	 * @param tiers
	 * @param dateEvenement
	 * @param motifFor motif de fermeture du for
	 * @param id du for
	 * @throws Exception
	 */
	public void publierEvenementFiscalFermetureFor(Tiers tiers, RegDate dateEvenement, @Nullable MotifFor motifFor, Long id) ;

	/**
	 * Publie un événement fiscal de type 'Annulation de for'
	 *
	 * @param forFiscal      le for fiscal qui vient d'être annulé
	 * @param dateAnnulation la date d'annulation effective
	 */
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
	public void publierEvenementFiscalAnnulationFor(ForFiscal forFiscal, RegDate dateAnnulation);

	/**
	 * Publie un événement fiscal de type 'Changement de mode d'imposition'
	 * @param contribuable
	 * @param dateEvenement
	 * @param modeImposition nouveau mode d'imposition
	 * @param id du nouveaui for
	 */
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
	public void publierEvenementFiscalChangementModeImposition(Contribuable contribuable, RegDate dateEvenement, ModeImposition modeImposition, Long id) ;

	/**
	 * [UNIREG-3244] Publie un événement de fin d'autorité parentale sur un contribuable parent suite à la majorité d'un enfant.
	 *
	 * @param contribuableEnfant le contribuable qui acquiert la majorité
	 * @param contribuableParent le contribuable dont l'autorité parentale prends fin
	 * @param dateEvenement      la date d'acquisition de la majorité
	 */
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
	public void publierEvenementFiscalFinAutoriteParentale(PersonnePhysique contribuableEnfant, Contribuable contribuableParent, RegDate dateEvenement);

	/**
	 * [UNIREG-3244] Publie un événement de naissance d'un contribuable enfant
	 *
	 * @param contribuableEnfant le contribuable nouveau né
	 * @param contribuableParent le contribuable qui possède l'autorité parentale du nouveau né
	 * @param dateEvenement      la date de naissance
	 */
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
	public void publierEvenementFiscalNaissance(PersonnePhysique contribuableEnfant, Contribuable contribuableParent, RegDate dateEvenement);

	/**
	 * Publie un événement fiscal de type 'Changement de situation de famille'
	 * @param contribuable
	 * @param dateEvenement
	 * @param id de la situation de famille
	 */
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
	public void publierEvenementFiscalChangementSituation(Contribuable contribuable, RegDate dateEvenement, Long id) ;

	/**
	 * Publie un événement fiscal de type 'Ouverture de période décompte LR'
	 * @param debiteur
	 * @param LR
	 * @param dateEvenement
	 */
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
	public void publierEvenementFiscalOuverturePeriodeDecompteLR(DebiteurPrestationImposable debiteur, DeclarationImpotSource lr, RegDate dateEvenement) ;

	/**
	 * Publie un événement fiscal de type 'Retour LR'
	 * @param debiteur
	 * @param LR
	 * @param dateEvenement
	 */
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
	public void publierEvenementFiscalRetourLR(DebiteurPrestationImposable debiteur, DeclarationImpotSource lr, RegDate dateEvenement) ;

	/**
	 * Publie un événement fiscal de type 'Sommation LR'
	 * @param debiteur
	 * @param LR
	 * @param dateEvenement
	 */
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
	void publierEvenementFiscalSommationLR(DebiteurPrestationImposable debiteur, DeclarationImpotSource lr, RegDate dateEvenement);

	/**
	 * Publie un événement fiscal de type 'LR manquante'
	 * @param debiteur
	 * @param LR
	 * @param dateEvenement
	 */
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
	void publierEvenementFiscalLRManquante(DebiteurPrestationImposable debiteur, DeclarationImpotSource lr, RegDate dateEvenement);

	/**
	 * Publie un événement fiscal de type 'Annulation LR'
	 * @param debiteur
	 * @param LR
	 * @param dateEvenement
	 */
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
	void publierEvenementFiscalAnnulationLR(DebiteurPrestationImposable debiteur, DeclarationImpotSource lr, RegDate dateEvenement);

	/**
	 * Publie un événement fiscal de type 'Envoi DI'
	 * @param contribuable
	 * @param DI
	 * @param dateEvenement
	 */
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
	public void publierEvenementFiscalEnvoiDI(Contribuable contribuable, DeclarationImpotOrdinaire di, RegDate dateEvenement) ;

	/**
	 * Publie un événement fiscal de type 'Retour DI'
	 * @param contribuable
	 * @param DI
	 * @param dateEvenement
	 */
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
	void publierEvenementFiscalRetourDI(Contribuable contribuable, DeclarationImpotOrdinaire di, RegDate dateEvenement);

	/**
	 * Publie un événement fiscal de type 'Sommation DI'
	 * @param contribuable
	 * @param DI
	 * @param dateEvenement
	 */
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
	void publierEvenementFiscalSommationDI(Contribuable contribuable, DeclarationImpotOrdinaire di, RegDate dateEvenement);

	/**
	 * Publie un événement fiscal de type 'Echéance DI'
	 * @param contribuable
	 * @param DI
	 * @param dateEvenement
	 */
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
	void publierEvenementFiscalEcheanceDI(Contribuable contribuable, DeclarationImpotOrdinaire di, RegDate dateEvenement);

	/**
	 * Publie un événement fiscal de type 'Taxation d'office'
	 * @param contribuable
	 * @param DI
	 * @param dateEvenement
	 */
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
	void publierEvenementFiscalTaxationOffice(Contribuable contribuable, DeclarationImpotOrdinaire di, RegDate dateEvenement);

	/**
	 * Publie un événement fiscal de type 'Annulation DI'
	 * @param contribuable
	 * @param DI
	 * @param dateEvenement
	 */
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
	void publierEvenementFiscalAnnulationDI(Contribuable contribuable, DeclarationImpotOrdinaire di, RegDate dateEvenement);

}
