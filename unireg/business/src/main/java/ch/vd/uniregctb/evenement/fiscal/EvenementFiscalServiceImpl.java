package ch.vd.uniregctb.evenement.fiscal;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.DeclarationImpotSource;
import ch.vd.uniregctb.evenement.*;
import ch.vd.uniregctb.parametrage.ParametreAppService;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.TypeEvenementFiscal;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.Collection;

/**
 * Service des événement fiscaux
 *
 * @author xcicfh (last modified by $Author: $ @ $Date: $)
 * @version $Revision: $
 */
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
public class EvenementFiscalServiceImpl implements EvenementFiscalService {

	private EvenementFiscalDAO evenementFiscalDAO;
	private EvenementFiscalSender evenementFiscalSender;
	private ParametreAppService parametres;

	@SuppressWarnings({"UnusedDeclaration"})
	public void setEvenementFiscalDAO(EvenementFiscalDAO evenementFiscalDAO) {
		this.evenementFiscalDAO = evenementFiscalDAO;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setEvenementFiscalSender(EvenementFiscalSender evenementFiscalSender) {
		this.evenementFiscalSender = evenementFiscalSender;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setParametres(ParametreAppService parametres) {
		this.parametres = parametres;
	}

	/**
	 * {@inheritDoc}
	 */
	public Collection<EvenementFiscal> getEvenementFiscals(Tiers tiers) {
		return evenementFiscalDAO.getEvenementFiscals(tiers);
	}

	/**
	 * {@inheritDoc}
	 */
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
	public void publierEvenementFiscal(EvenementFiscal evenementFiscal) {
		Assert.notNull(evenementFiscal, "evenementFiscal ne peut être null.");

		// sauve evenementFiscal
		evenementFiscal = evenementFiscalDAO.save(evenementFiscal);

		// publication de l'evenementFiscal
		try {
			evenementFiscalSender.sendEvent(evenementFiscal);
		}
		catch (EvenementFiscalException e) {
			throw new RuntimeException("Erreur survenu lors de la publication de l'evenement Fiscal [" + evenementFiscal.getId() + "].", e);
		}
	}

	private int getAnneePremierePeriodeFiscale() {
		return parametres.getPremierePeriodeFiscale();
	}

	private boolean peutPublierEvenementFiscal(RegDate dateEvenement) {
		return dateEvenement.year() >= getAnneePremierePeriodeFiscale();
	}

	/**
	 * {@inheritDoc}
	 */
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
	public void publierEvenementFiscalOuvertureFor(Tiers tiers, RegDate dateEvenement, MotifFor motifFor, Long id) {
		final EvenementFiscal evenementFiscal = new EvenementFiscalFor(tiers, dateEvenement, TypeEvenementFiscal.OUVERTURE_FOR, motifFor, null, id);
		publierEvenementFiscal(evenementFiscal);
	}

	/**
	 * {@inheritDoc}
	 */
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
	public void publierEvenementFiscalFermetureFor(Tiers tiers, RegDate dateEvenement, MotifFor motifFor, Long id) {
		final EvenementFiscal evenementFiscal = new EvenementFiscalFor(tiers, dateEvenement, TypeEvenementFiscal.FERMETURE_FOR, motifFor, null, id);
		publierEvenementFiscal(evenementFiscal);
	}

	/**
	 * {@inheritDoc}
	 */
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
	public void publierEvenementFiscalAnnulationFor(ForFiscal forFiscal, RegDate dateAnnulation) {
		// on ne bloque l'envoi des événements fiscaux d'annulation que pour les fors fermés avant 2003
		final RegDate dateFin = forFiscal.getDateFin();
		if (dateFin == null || peutPublierEvenementFiscal(dateFin)) {
			final EvenementFiscal evenementFiscal = new EvenementFiscalFor(forFiscal.getTiers(), dateAnnulation, TypeEvenementFiscal.ANNULATION_FOR, null, null, forFiscal.getId());
			publierEvenementFiscal(evenementFiscal);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
	public void publierEvenementFiscalChangementModeImposition(Contribuable contribuable, RegDate dateEvenement, ModeImposition modeImposition, Long id) {
		if (peutPublierEvenementFiscal(dateEvenement)) {
			final EvenementFiscal evenementFiscal = new EvenementFiscalFor(contribuable, dateEvenement, TypeEvenementFiscal.CHANGEMENT_MODE_IMPOSITION, null, modeImposition, id);
			publierEvenementFiscal(evenementFiscal);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
	public void publierEvenementFiscalChangementSituation(Contribuable contribuable, RegDate dateEvenement, Long id) {
		if (peutPublierEvenementFiscal(dateEvenement)) {
			final EvenementFiscal evenementFiscal = new EvenementFiscalSituationFamille(contribuable, dateEvenement, id);
			publierEvenementFiscal(evenementFiscal);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
	public void publierEvenementFiscalRetourLR(DebiteurPrestationImposable debiteur, DeclarationImpotSource lr, RegDate dateEvenement) {
		if (peutPublierEvenementFiscal(dateEvenement)) {
			final EvenementFiscal evenementFiscal = new EvenementFiscalLR(debiteur, dateEvenement, TypeEvenementFiscal.RETOUR_LR, lr.getDateDebut(), lr.getDateFin(), lr.getId());
			publierEvenementFiscal(evenementFiscal);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
	public void publierEvenementFiscalOuverturePeriodeDecompteLR(DebiteurPrestationImposable debiteur, DeclarationImpotSource lr, RegDate dateEvenement) {
		if (peutPublierEvenementFiscal(dateEvenement)) {
			final EvenementFiscal evenementFiscal = new EvenementFiscalLR(debiteur, dateEvenement, TypeEvenementFiscal.OUVERTURE_PERIODE_DECOMPTE_LR, lr.getDateDebut(), lr.getDateFin(),
					lr.getId());
			publierEvenementFiscal(evenementFiscal);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
	public void publierEvenementFiscalAnnulationLR(DebiteurPrestationImposable debiteur, DeclarationImpotSource lr, RegDate dateEvenement) {
		if (peutPublierEvenementFiscal(dateEvenement)) {
			final EvenementFiscal evenementFiscal = new EvenementFiscalLR(debiteur, dateEvenement, TypeEvenementFiscal.ANNULATION_LR, lr.getDateDebut(), lr.getDateFin(), lr.getId());
			publierEvenementFiscal(evenementFiscal);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
	public void publierEvenementFiscalLRManquante(DebiteurPrestationImposable debiteur, DeclarationImpotSource lr, RegDate dateEvenement) {
		if (peutPublierEvenementFiscal(dateEvenement)) {
			final EvenementFiscal evenementFiscal = new EvenementFiscalLR(debiteur, dateEvenement, TypeEvenementFiscal.LR_MANQUANTE, lr.getDateDebut(), lr.getDateFin(), lr.getId());
			publierEvenementFiscal(evenementFiscal);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
	public void publierEvenementFiscalSommationLR(DebiteurPrestationImposable debiteur, DeclarationImpotSource lr, RegDate dateEvenement) {
		if (peutPublierEvenementFiscal(dateEvenement)) {
			final EvenementFiscal evenementFiscal = new EvenementFiscalLR(debiteur, dateEvenement, TypeEvenementFiscal.SOMMATION_LR, lr.getDateDebut(), lr.getDateFin(), lr.getId());
			publierEvenementFiscal(evenementFiscal);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
	public void publierEvenementFiscalEnvoiDI(Contribuable contribuable, DeclarationImpotOrdinaire di, RegDate dateEvenement) {
		if (peutPublierEvenementFiscal(dateEvenement)) {
			final EvenementFiscal evenementFiscal = new EvenementFiscalDI(contribuable, dateEvenement, TypeEvenementFiscal.ENVOI_DI, di.getDateDebut(), di.getDateFin(), di.getId());
			publierEvenementFiscal(evenementFiscal);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
	public void publierEvenementFiscalEcheanceDI(Contribuable contribuable, DeclarationImpotOrdinaire di, RegDate dateEvenement) {
		if (peutPublierEvenementFiscal(dateEvenement)) {
			final EvenementFiscal evenementFiscal = new EvenementFiscalDI(contribuable, dateEvenement, TypeEvenementFiscal.ECHEANCE_DI, di.getDateDebut(), di.getDateFin(), di.getId());
			publierEvenementFiscal(evenementFiscal);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
	public void publierEvenementFiscalRetourDI(Contribuable contribuable, DeclarationImpotOrdinaire di, RegDate dateEvenement) {
		if (peutPublierEvenementFiscal(dateEvenement)) {
			final EvenementFiscal evenementFiscal = new EvenementFiscalDI(contribuable, dateEvenement, TypeEvenementFiscal.RETOUR_DI, di.getDateDebut(), di.getDateFin(), di.getId());
			publierEvenementFiscal(evenementFiscal);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
	public void publierEvenementFiscalSommationDI(Contribuable contribuable, DeclarationImpotOrdinaire di, RegDate dateEvenement) {
		if (peutPublierEvenementFiscal(dateEvenement)) {
			final EvenementFiscal evenementFiscal = new EvenementFiscalDI(contribuable, dateEvenement, TypeEvenementFiscal.SOMMATION_DI, di.getDateDebut(), di.getDateFin(), di.getId());
			publierEvenementFiscal(evenementFiscal);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
	public void publierEvenementFiscalTaxationOffice(Contribuable contribuable, DeclarationImpotOrdinaire di, RegDate dateEvenement) {
		if (peutPublierEvenementFiscal(dateEvenement)) {
			final EvenementFiscal evenementFiscal = new EvenementFiscalDI(contribuable, dateEvenement, TypeEvenementFiscal.TAXATION_OFFICE, di.getDateDebut(), di.getDateFin(), di.getId());
			publierEvenementFiscal(evenementFiscal);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
	public void publierEvenementFiscalAnnulationDI(Contribuable contribuable, DeclarationImpotOrdinaire di, RegDate dateEvenement) {
		if (peutPublierEvenementFiscal(dateEvenement)) {
			final EvenementFiscal evenementFiscal = new EvenementFiscalDI(contribuable, dateEvenement, TypeEvenementFiscal.ANNULATION_DI, di.getDateDebut(), di.getDateFin(), di.getId());
			publierEvenementFiscal(evenementFiscal);
		}
	}
}
