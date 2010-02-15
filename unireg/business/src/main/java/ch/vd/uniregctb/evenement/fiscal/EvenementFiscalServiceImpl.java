package ch.vd.uniregctb.evenement.fiscal;

import java.util.Collection;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.DeclarationImpotSource;
import ch.vd.uniregctb.evenement.EvenementFiscal;
import ch.vd.uniregctb.evenement.EvenementFiscalDAO;
import ch.vd.uniregctb.parametrage.ParametreAppService;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.TypeEvenementFiscal;

/**
 * Service des événement fiscaux
 *
 * @author xcicfh (last modified by $Author: $ @ $Date: $)
 * @version $Revision: $
 *
 */
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
public class EvenementFiscalServiceImpl implements EvenementFiscalService, InitializingBean {

	private EvenementFiscalDAO evenementFiscalDAO;

	private EvenementFiscalFacade evenementFiscalFacade;

	private ParametreAppService parametres;

	public void afterPropertiesSet() throws Exception {
		Assert.notNull(evenementFiscalDAO, "evenementFiscalDAO is required");
		Assert.notNull(evenementFiscalFacade, "evenementFiscalFacade is required");
	}

	/**
	 * {@inheritDoc}
	 */
	public Collection<EvenementFiscal>  getEvenementFiscals(Tiers tiers) {
		return evenementFiscalDAO.getEvenementFiscals(tiers);
	}


	/**
	 * {@inheritDoc}
	 */
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
	public void publierEvenementFiscal(EvenementFiscal evenementFiscal) {
		if (evenementFiscal == null) {
			throw new IllegalArgumentException("evenementFiscal ne peut être null.");
		}
		// sauve evenementFiscal
		evenementFiscal = evenementFiscalDAO.save(evenementFiscal);
		// publication de l'evenementFiscal
		try {
			evenementFiscalFacade.publierEvenement(evenementFiscal);
		}
		catch (EvenementFiscalException e) {
			throw new RuntimeException("Erreur survenu lors de la publication de l'evenement Fiscal [" + evenementFiscal.getId() + "].", e);
		}

	}

	/**
	 * @param evenementFiscalDAO
	 *            the evenementFiscalDAO to set
	 */
	public void setEvenementFiscalDAO(EvenementFiscalDAO evenementFiscalDAO) {
		this.evenementFiscalDAO = evenementFiscalDAO;
	}

	/**
	 * @param evenementFiscalFacade
	 *            the evenementFiscalFacade to set
	 */
	public void setEvenementFiscalFacade(EvenementFiscalFacade evenementFiscalFacade) {
		this.evenementFiscalFacade = evenementFiscalFacade;
	}

	/**
	 * @param parametres the parametres to set
	 */
	public void setParametres(ParametreAppService parametres) {
		this.parametres = parametres;
	}

	private int getAnneePremierePeriodeFiscale() {
		return parametres.getPremierePeriodeFiscale();
	}

	private boolean peutPublierEvenementFiscal(RegDate dateEvenement) {
		return dateEvenement.year() >= getAnneePremierePeriodeFiscale();
	}

	/**
	 * Publie un événement fiscal de type 'Ouverture for'
	 * @param contribuable
	 * @param dateEvenement
	 * @param motifFor motif d'ouverture du for
	 * @param id du for
	 */
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
	public void publierEvenementFiscalOuvertureFor(Tiers tiers, RegDate dateEvenement, MotifFor motifFor, Long id)  {
		final EvenementFiscal evenementFiscal = evenementFiscalDAO.creerEvenementFor(tiers, TypeEvenementFiscal.OUVERTURE_FOR, dateEvenement, motifFor, null, id);
		publierEvenementFiscal(evenementFiscal);
	}

	/**
	 * Publie un événement fiscal de type 'Fermeture for'
	 * @param contribuable
	 * @param dateEvenement
	 * @param motifFor motif de fermeture du for
	 * @param id du for
	 */
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
	public void publierEvenementFiscalFermetureFor(Tiers tiers, RegDate dateEvenement, MotifFor motifFor, Long id)  {
		final EvenementFiscal evenementFiscal = evenementFiscalDAO.creerEvenementFor(tiers, TypeEvenementFiscal.FERMETURE_FOR, dateEvenement, motifFor, null, id);
		publierEvenementFiscal(evenementFiscal);
	}

	/**
	 * Publie un événement fiscal de type 'Annulation for'
	 * @param contribuable
	 * @param dateDebut
	 * @param dateFin
	 * @param id du for
	 */
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
	public void publierEvenementFiscalAnnulationFor(Tiers tiers, RegDate dateDebut, RegDate dateFin, Long id)  {
		// on ne bloque l'envoi des événements fiscaux d'annulation que pour les fors fermés avant 2003
		if (dateFin == null || peutPublierEvenementFiscal(dateFin)) {
			final EvenementFiscal evenementFiscal = evenementFiscalDAO.creerEvenementFor(tiers, TypeEvenementFiscal.ANNULATION_FOR, dateDebut, null, null,id);
			publierEvenementFiscal(evenementFiscal);
		}
	}

	/**
	 * Publie un événement fiscal de type 'Changement de mode d'imposition'
	 * @param contribuable
	 * @param dateEvenement
	 * @param modeImposition nouveau mode d'imposition
	 * @param id du nouveaui for
	 */
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
	public void publierEvenementFiscalChangementModeImposition(Contribuable contribuable, RegDate dateEvenement, ModeImposition modeImposition, Long id)  {
		if (peutPublierEvenementFiscal(dateEvenement)) {
			final EvenementFiscal evenementFiscal = evenementFiscalDAO.creerEvenementFor(contribuable, TypeEvenementFiscal.CHANGEMENT_MODE_IMPOSITION, dateEvenement, null, modeImposition, id);
			publierEvenementFiscal(evenementFiscal);
		}
	}

	/**
	 * Publie un événement fiscal de type 'Changement de situation de famille'
	 * @param contribuable
	 * @param dateEvenement
	 * @param id de la situation de famille
	 */
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
	public void publierEvenementFiscalChangementSituation(Contribuable contribuable, RegDate dateEvenement, Long id) {
		if (peutPublierEvenementFiscal(dateEvenement)) {
			final EvenementFiscal evenementFiscal = evenementFiscalDAO.creerEvenementSituationFamille(contribuable, TypeEvenementFiscal.CHANGEMENT_SITUATION_FAMILLE, dateEvenement, id);
			publierEvenementFiscal(evenementFiscal);
		}
	}

	/**
	 * Publie un événement fiscal de type 'Retour LR'
	 * @param debiteur
	 * @param LR
	 * @param dateEvenement
	 */
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
	public void publierEvenementFiscalRetourLR(DebiteurPrestationImposable debiteur, DeclarationImpotSource lr, RegDate dateEvenement) {
		if (peutPublierEvenementFiscal(dateEvenement)) {
			final EvenementFiscal evenementFiscal = evenementFiscalDAO.creerEvenementLR(debiteur, TypeEvenementFiscal.RETOUR_LR, dateEvenement, lr.getId(), lr.getDateDebut(), lr.getDateFin());
			publierEvenementFiscal(evenementFiscal);
		}
	}

	/**
	 * Publie un événement fiscal de type 'Ouverture de période décompte LR'
	 * @param debiteur
	 * @param LR
	 * @param dateEvenement
	 */
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
	public void publierEvenementFiscalOuverturePeriodeDecompteLR(DebiteurPrestationImposable debiteur, DeclarationImpotSource lr, RegDate dateEvenement) {
		if (peutPublierEvenementFiscal(dateEvenement)) {
			final EvenementFiscal evenementFiscal = evenementFiscalDAO.creerEvenementLR(debiteur, TypeEvenementFiscal.OUVERTURE_PERIODE_DECOMPTE_LR, dateEvenement, lr.getId(), lr.getDateDebut(), lr.getDateFin());
			publierEvenementFiscal(evenementFiscal);
		}
	}

	/**
	 * Publie un événement fiscal de type 'Annulation LR'
	 * @param debiteur
	 * @param LR
	 * @param dateEvenement
	 */
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
	public void publierEvenementFiscalAnnulationLR(DebiteurPrestationImposable debiteur, DeclarationImpotSource lr, RegDate dateEvenement) {
		if (peutPublierEvenementFiscal(dateEvenement)) {
			final EvenementFiscal evenementFiscal = evenementFiscalDAO.creerEvenementLR(debiteur, TypeEvenementFiscal.ANNULATION_LR, dateEvenement, lr.getId(), lr.getDateDebut(), lr.getDateFin());
			publierEvenementFiscal(evenementFiscal);
		}
	}

	/**
	 * Publie un événement fiscal de type 'LR manquante'
	 * @param debiteur
	 * @param LR
	 * @param dateEvenement
	 */
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
	public void publierEvenementLRManquante(DebiteurPrestationImposable debiteur, DeclarationImpotSource lr, RegDate dateEvenement) {
		if (peutPublierEvenementFiscal(dateEvenement)) {
			final EvenementFiscal evenementFiscal = evenementFiscalDAO.creerEvenementLR(debiteur, TypeEvenementFiscal.LR_MANQUANTE, dateEvenement, lr.getId(), lr.getDateDebut(), lr.getDateFin());
			publierEvenementFiscal(evenementFiscal);
		}
	}

	/**
	 * Publie un événement fiscal de type 'Sommation LR'
	 * @param debiteur
	 * @param LR
	 * @param dateEvenement
	 */
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
	public void publierEvenementFiscalSommationLR(DebiteurPrestationImposable debiteur, DeclarationImpotSource lr, RegDate dateEvenement) {
		if (peutPublierEvenementFiscal(dateEvenement)) {
			final EvenementFiscal evenementFiscal = evenementFiscalDAO.creerEvenementLR(debiteur, TypeEvenementFiscal.SOMMATION_LR, dateEvenement, lr.getId(), lr.getDateDebut(), lr.getDateFin());
			publierEvenementFiscal(evenementFiscal);
		}
	}

	/**
	 * Publie un événement fiscal de type 'Envoi DI'
	 * @param contribuable
	 * @param DI
	 * @param dateEvenement
	 */
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
	public void publierEvenementFiscalEnvoiDI(Contribuable contribuable, DeclarationImpotOrdinaire di, RegDate dateEvenement) {
		if (peutPublierEvenementFiscal(dateEvenement)) {
			final EvenementFiscal evenementFiscal = evenementFiscalDAO.creerEvenementDI(contribuable, TypeEvenementFiscal.ENVOI_DI, dateEvenement, di.getId(), di.getDateDebut(), di.getDateFin());
			publierEvenementFiscal(evenementFiscal);
		}
	}

	/**
	 * Publie un événement fiscal de type 'Echéance DI'
	 * @param contribuable
	 * @param DI
	 * @param dateEvenement
	 */
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
	public void publierEvenementFiscalEcheanceDI(Contribuable contribuable, DeclarationImpotOrdinaire di, RegDate dateEvenement) {
		if (peutPublierEvenementFiscal(dateEvenement)) {
			final EvenementFiscal evenementFiscal = evenementFiscalDAO.creerEvenementDI(contribuable, TypeEvenementFiscal.ECHEANCE_DI, dateEvenement, di.getId(), di.getDateDebut(), di.getDateFin());
			publierEvenementFiscal(evenementFiscal);
		}
	}

	/**
	 * Publie un événement fiscal de type 'Retour DI'
	 * @param contribuable
	 * @param DI
	 * @param dateEvenement
	 */
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
	public void publierEvenementFiscalRetourDI(Contribuable contribuable, DeclarationImpotOrdinaire di, RegDate dateEvenement) {
		if (peutPublierEvenementFiscal(dateEvenement)) {
			final EvenementFiscal evenementFiscal = evenementFiscalDAO.creerEvenementDI(contribuable, TypeEvenementFiscal.RETOUR_DI, dateEvenement, di.getId(), di.getDateDebut(), di.getDateFin());
			publierEvenementFiscal(evenementFiscal);
		}
	}

	/**
	 * Publie un événement fiscal de type 'Sommation DI'
	 * @param contribuable
	 * @param DI
	 * @param dateEvenement
	 */
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
	public void publierEvenementFiscalSommationDI(Contribuable contribuable, DeclarationImpotOrdinaire di, RegDate dateEvenement) {
		if (peutPublierEvenementFiscal(dateEvenement)) {
			final EvenementFiscal evenementFiscal = evenementFiscalDAO.creerEvenementDI(contribuable, TypeEvenementFiscal.SOMMATION_DI, dateEvenement, di.getId(), di.getDateDebut(), di.getDateFin());
			publierEvenementFiscal(evenementFiscal);
		}
	}

	/**
	 * Publie un événement fiscal de type 'Taxation d'office'
	 * @param contribuable
	 * @param DI
	 * @param dateEvenement
	 */
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
	public void publierEvenementFiscalTaxationOffice(Contribuable contribuable, DeclarationImpotOrdinaire di, RegDate dateEvenement) {
		if (peutPublierEvenementFiscal(dateEvenement)) {
			final EvenementFiscal evenementFiscal = evenementFiscalDAO.creerEvenementDI(contribuable, TypeEvenementFiscal.TAXATION_OFFICE, dateEvenement, di.getId(), di.getDateDebut(), di.getDateFin());
			publierEvenementFiscal(evenementFiscal);
		}
	}

	/**
	 * Publie un événement fiscal de type 'Annulation DI'
	 * @param contribuable
	 * @param DI
	 * @param dateEvenement
	 */
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
	public void publierEvenementFiscalAnnulationDI(Contribuable contribuable, DeclarationImpotOrdinaire di, RegDate dateEvenement) {
		if (peutPublierEvenementFiscal(dateEvenement)) {
			final EvenementFiscal evenementFiscal = evenementFiscalDAO.creerEvenementDI(contribuable, TypeEvenementFiscal.ANNULATION_DI, dateEvenement, di.getId(), di.getDateDebut(), di.getDateFin());
			publierEvenementFiscal(evenementFiscal);
		}
	}

}
