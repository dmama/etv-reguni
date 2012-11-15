package ch.vd.uniregctb.tiers.manager;

import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Assert;
import ch.vd.unireg.interfaces.civil.data.Pays;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureException;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.uniregctb.adresse.AdresseException;
import ch.vd.uniregctb.common.ObjectNotFoundException;
import ch.vd.uniregctb.common.TiersNotFoundException;
import ch.vd.uniregctb.interfaces.InterfaceDataException;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.ForDebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.ForFiscalAutreElementImposable;
import ch.vd.uniregctb.tiers.ForFiscalDAO;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.ForFiscalSecondaire;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.NatureTiers;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.view.ForFiscalView;
import ch.vd.uniregctb.tiers.view.TiersEditView;
import ch.vd.uniregctb.type.GenreImpot;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.utils.WebContextUtils;

/**
 * Service à disposition du controller pour gérer un for fiscal
 * @author xcifde
 */
public class ForFiscalManagerImpl extends TiersManager implements ForFiscalManager {

	private ForFiscalDAO forFiscalDAO;
	private ServiceInfrastructureService serviceInfra;

	public void setForFiscalDAO(ForFiscalDAO forFiscalDAO) {
		this.forFiscalDAO = forFiscalDAO;
	}

	public void setServiceInfra(ServiceInfrastructureService service) {
		serviceInfra = service;
	}

	/**
	 * Charge les informations dans TiersView
	 */
	@Override
	@Transactional(readOnly = true)
	public TiersEditView getView(Long numero) throws AdresseException, ServiceInfrastructureException {
		TiersEditView tiersEditView = new TiersEditView();

		if (numero == null) {
			return null;
		}
		final Tiers tiers = getTiersDAO().get(numero);

		if (tiers == null) {
			throw new TiersNotFoundException(numero);
		}
		if (tiers != null) {
			setTiersGeneralView(tiersEditView, tiers);
			tiersEditView.setTiers(tiers);
			if (tiers instanceof Contribuable) {
				final Contribuable contribuable = (Contribuable) tiers;
				setForsFiscaux(tiersEditView, contribuable);
				try {
					setSituationsFamille(tiersEditView, contribuable);
				}
				catch (InterfaceDataException e) {
					LOGGER.warn(String.format("Exception lors de la récupération des situations de familles du contribuable %d", numero), e);
					tiersEditView.setSituationsFamilleEnErreurMessage(e.getMessage());
				}
			}
			if (tiers instanceof DebiteurPrestationImposable) {
				DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiers;
				setForsFiscauxDebiteur(tiersEditView, dpi);
				setPeriodiciteCourante(tiersEditView, dpi);
			}
		}

		final Autorisations autorisations = getAutorisations(tiers);
		tiersEditView.setAutorisations(autorisations);

		return tiersEditView;
	}


	/**
	 * Recupere la vue ForFiscalView
	 */
	@Override
	@Transactional(readOnly = true)
	public ForFiscalView get(Long id) throws Exception {
		final ForFiscal forFiscal = forFiscalDAO.get(id);

		if (forFiscal == null) {
			throw new ObjectNotFoundException(this.getMessageSource().getMessage("error.for.fiscal.inexistant", null, WebContextUtils.getDefaultLocale()));
		}

		final Tiers tiers = forFiscal.getTiers();
		final ForFiscalView forFiscalView = new ForFiscalView(forFiscal, false, false);
		forFiscalView.setChangementModeImposition(false);

		if (tiers.getNatureTiers() == NatureTiers.MenageCommun) {
			final MenageCommun menage = (MenageCommun) tiers;
			boolean isHabitant = false;
			for (PersonnePhysique pp : tiersService.getPersonnesPhysiques(menage)) {
				if (pp.isHabitantVD()) {
					isHabitant = true;
					break;
				}
			}
			if (isHabitant) {
				forFiscalView.setNatureTiers(NatureTiers.Habitant);
			}
			else {
				forFiscalView.setNatureTiers(NatureTiers.NonHabitant);
			}
		}
		else {
			forFiscalView.setNatureTiers(tiers.getNatureTiers());
		}

		final TypeAutoriteFiscale typeForFiscal = forFiscal.getTypeAutoriteFiscale();
		switch (typeForFiscal) {
		case COMMUNE_OU_FRACTION_VD:
			if (forFiscal.getNumeroOfsAutoriteFiscale() != null) {
				final Commune commune = serviceInfra.getCommuneByNumeroOfsEtendu(forFiscal.getNumeroOfsAutoriteFiscale(), forFiscal.getDateFin());
				forFiscalView.setLibFractionCommune(commune == null ? "" : commune.getNomMinuscule());
			}
			break;
		case COMMUNE_HC:
			if (forFiscal.getNumeroOfsAutoriteFiscale() != null) {
				final Commune commune = serviceInfra.getCommuneByNumeroOfsEtendu(forFiscal.getNumeroOfsAutoriteFiscale(), forFiscal.getDateFin());
				forFiscalView.setLibCommuneHorsCanton(commune == null ? "" : commune.getNomMinuscule());
			}
			break;
		case PAYS_HS:
			if (forFiscal.getNumeroOfsAutoriteFiscale() != null) {
				final Pays pays = serviceInfra.getPays(forFiscal.getNumeroOfsAutoriteFiscale());
				forFiscalView.setLibPays(pays == null ? "" : pays.getNomMinuscule());
			}
			break;
		default:
			break;
		}

		return forFiscalView;
	}

	/**
	 * Cree une nouvelle vue ForFiscalView
	 */
	@Override
	@Transactional(readOnly = true)
	public ForFiscalView create(Long numeroCtb, boolean dpi) {
		final ForFiscalView forFiscalView = new ForFiscalView();
		forFiscalView.setChangementModeImposition(false);
		forFiscalView.setNumeroCtb(numeroCtb);

		final Tiers tiers = tiersDAO.get(numeroCtb);
		if (NatureTiers.MenageCommun == tiers.getNatureTiers()) {
			final MenageCommun menage = (MenageCommun) tiers;
			boolean isHabitant = false;
			for (PersonnePhysique pp : tiersService.getPersonnesPhysiques(menage)) {
				if (pp.isHabitantVD()) {
					isHabitant = true;
					break;
				}
			}
			forFiscalView.setNatureTiers(isHabitant ? NatureTiers.Habitant : NatureTiers.NonHabitant);
		}
		else {
			forFiscalView.setNatureTiers(tiers.getNatureTiers());
		}

		if (dpi) {
			forFiscalView.setNatureForFiscal("ForDebiteurPrestationImposable");
			forFiscalView.setGenreImpot(GenreImpot.DEBITEUR_PRESTATION_IMPOSABLE);
			forFiscalView.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
		}
		else {
			forFiscalView.setNatureForFiscal("ForFiscalPrincipal");
			forFiscalView.setGenreImpot(GenreImpot.REVENU_FORTUNE);
			forFiscalView.setMotifRattachement(MotifRattachement.DOMICILE);
			forFiscalView.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
			forFiscalView.setModeImposition(ModeImposition.ORDINAIRE);
		}
		forFiscalView.setDateOuverture(RegDate.get());
		return forFiscalView;
	}

	@Override
	public ForFiscal updateFor(ForFiscalView view) {

		final ForFiscal forFiscal = forFiscalDAO.get(view.getId());
		ForFiscal updated = null;

		if (forFiscal instanceof ForFiscalPrincipal) {
			updated = updateForPrincipal((ForFiscalPrincipal) forFiscal, view.getRegDateFermeture(), view.getMotifFermeture(), view.getNumeroAutoriteFiscale());
		}
		else if (forFiscal instanceof ForFiscalSecondaire) {
			updated = updateForSecondaire((ForFiscalSecondaire) forFiscal, view.getRegDateOuverture(), view.getMotifOuverture(), view.getRegDateFermeture(), view.getMotifFermeture(),
					view.getNumeroAutoriteFiscale());
		}
		else if (forFiscal instanceof ForFiscalAutreElementImposable) {
			updated = updateForAutreElementImposable((ForFiscalAutreElementImposable) forFiscal, view.getRegDateFermeture(), view.getMotifFermeture());
		}
		else if (forFiscal instanceof ForDebiteurPrestationImposable) {
			updated = updateForDebiteur((ForDebiteurPrestationImposable) forFiscal, view.getRegDateFermeture());
		}
		//else les fors autreimpot ne sont éditables

		return updated;
	}

	public ForDebiteurPrestationImposable updateForDebiteur(ForDebiteurPrestationImposable fdpi, RegDate dateFermeture) {

		ForDebiteurPrestationImposable updated = null;

		if (fdpi.getDateFin() == null && dateFermeture != null) {
			// le for a été fermé
			updated = tiersService.closeForDebiteurPrestationImposable((DebiteurPrestationImposable) fdpi.getTiers(), fdpi, dateFermeture, true);
		}

		return updated;
	}

	@Override
	public ForFiscalAutreElementImposable updateForAutreElementImposable(ForFiscalAutreElementImposable ffaei, RegDate dateFermeture, MotifFor motifFermeture) {

		ForFiscalAutreElementImposable updated = null;

		if (ffaei.getDateFin() == null && dateFermeture != null) {
			// le for a été fermé
			updated = tiersService.closeForFiscalAutreElementImposable((Contribuable) ffaei.getTiers(), ffaei, dateFermeture, motifFermeture);
		}

		return updated;
	}

	@Override
	public ForFiscalSecondaire updateForSecondaire(ForFiscalSecondaire ffs, RegDate dateOuverture, MotifFor motifOuverture, RegDate dateFermeture, MotifFor motifFermeture,
	                                               int noOfsAutoriteFiscale) {

		ForFiscalSecondaire updated = null;

		if (ffs.getDateDebut() == dateOuverture && ffs.getDateFin() == null && dateFermeture != null) {
			// le for a été fermé
			updated = tiersService.closeForFiscalSecondaire((Contribuable) ffs.getTiers(), ffs, dateFermeture, motifFermeture);
		}

		if (dateOuverture != ffs.getDateDebut() || dateFermeture != ffs.getDateFin()) {
			// les dates de début ou de fin ont été changées
			updated = tiersService.corrigerPeriodeValidite(ffs, dateOuverture, motifOuverture, dateFermeture, motifFermeture);
		}

		if (!ffs.getNumeroOfsAutoriteFiscale().equals(noOfsAutoriteFiscale)) {
			// l'autorité fiscale a été changée
			updated = (ForFiscalSecondaire) tiersService.corrigerAutoriteFiscale((updated == null ? ffs : updated), noOfsAutoriteFiscale);
		}

		return updated;
	}

	@Override
	public ForFiscalPrincipal updateForPrincipal(ForFiscalPrincipal ffp, RegDate dateFermeture, MotifFor motifFermeture, int noOfsAutoriteFiscale) {

		ForFiscalPrincipal updated = null;

		if (ffp.getDateFin() == null && dateFermeture != null) {
			// le for a été fermé
			updated = tiersService.closeForFiscalPrincipal(ffp, dateFermeture, motifFermeture);
		}

		if (ffp.getNumeroOfsAutoriteFiscale() != noOfsAutoriteFiscale) {
			// l'autorité fiscale a été changée
			updated = (ForFiscalPrincipal) tiersService.corrigerAutoriteFiscale(ffp, noOfsAutoriteFiscale);
		}

		return updated;
	}

	@Override
	public ForFiscalPrincipal updateModeImposition(ForFiscalView forFiscalView) {
		Assert.notNull(forFiscalView.getRegDateChangement());
		Contribuable contribuable = (Contribuable) tiersDAO.get(forFiscalView.getNumeroCtb());
		return tiersService.changeModeImposition(contribuable, forFiscalView.getRegDateChangement(), forFiscalView.getModeImposition(), forFiscalView.getMotifImposition());
	}

	@Override
	public ForFiscal addFor(ForFiscalView forFiscalView) {
		if (forFiscalView.getGenreImpot() == GenreImpot.REVENU_FORTUNE) {
			if (forFiscalView.getMotifRattachement() == MotifRattachement.DOMICILE ||
					forFiscalView.getMotifRattachement() == MotifRattachement.DIPLOMATE_SUISSE ||
					forFiscalView.getMotifRattachement() == MotifRattachement.DIPLOMATE_ETRANGER) {
				return addForPrincipal(forFiscalView);
			}
			else if (forFiscalView.getMotifRattachement() == MotifRattachement.ACTIVITE_INDEPENDANTE ||
					forFiscalView.getMotifRattachement() == MotifRattachement.IMMEUBLE_PRIVE ||
					forFiscalView.getMotifRattachement() == MotifRattachement.SEJOUR_SAISONNIER ||
					forFiscalView.getMotifRattachement() == MotifRattachement.DIRIGEANT_SOCIETE) {
				return addForSecondaire(forFiscalView);
			}
			else {
				return addForAutreElementImposable(forFiscalView);
			}
		}
		else if (forFiscalView.getGenreImpot() == GenreImpot.DEBITEUR_PRESTATION_IMPOSABLE) {
			return addForDebiteur(forFiscalView);
		}
		else {
			return addForAutreImpot(forFiscalView);
		}
	}

	private ForFiscal addForAutreImpot(ForFiscalView forFiscalView) {

		final Contribuable contribuable = (Contribuable) tiersDAO.get(forFiscalView.getNumeroCtb());
		if (contribuable == null) {
			throw new ObjectNotFoundException("Le contribuable avec l'id=" + forFiscalView.getNumeroCtb() + " n'existe pas.");
		}

		final GenreImpot genreImpot = forFiscalView.getGenreImpot();
		final RegDate dateImpot = forFiscalView.getRegDateOuverture();
		final TypeAutoriteFiscale typeAutoriteFiscale = forFiscalView.getTypeAutoriteFiscale();
		final int autoriteFiscale = forFiscalView.getNumeroAutoriteFiscale();

		return tiersService.openForFiscalAutreImpot(contribuable, genreImpot, dateImpot, autoriteFiscale, typeAutoriteFiscale);
	}

	private ForFiscal addForDebiteur(ForFiscalView forFiscalView) {

		final DebiteurPrestationImposable debiteur = (DebiteurPrestationImposable) tiersDAO.get(forFiscalView.getNumeroCtb());
		if (debiteur == null) {
			throw new ObjectNotFoundException("Le débiteur avec l'id=" + forFiscalView.getNumeroCtb() + " n'existe pas.");
		}

		Assert.isEqual(GenreImpot.DEBITEUR_PRESTATION_IMPOSABLE, forFiscalView.getGenreImpot());
		final RegDate dateDebut = forFiscalView.getRegDateOuverture();
		final RegDate dateFin = forFiscalView.getRegDateFermeture();
		final int autoriteFiscale = forFiscalView.getNumeroAutoriteFiscale();
		final TypeAutoriteFiscale typeAutoriteFiscale = forFiscalView.getTypeAutoriteFiscale();

		return tiersService.addForDebiteur(debiteur, dateDebut, dateFin, typeAutoriteFiscale, autoriteFiscale);
	}

	private ForFiscal addForAutreElementImposable(ForFiscalView forFiscalView) {

		final Contribuable contribuable = (Contribuable) tiersDAO.get(forFiscalView.getNumeroCtb());
		if (contribuable == null) {
			throw new ObjectNotFoundException("Le contribuable avec l'id=" + forFiscalView.getNumeroCtb() + " n'existe pas.");
		}

		Assert.isEqual(GenreImpot.REVENU_FORTUNE, forFiscalView.getGenreImpot());
		final RegDate dateDebut = forFiscalView.getRegDateOuverture();
		final MotifFor motifOuverture = forFiscalView.getMotifOuverture();
		final RegDate dateFin = forFiscalView.getRegDateFermeture();
		final MotifFor motifFermeture = forFiscalView.getMotifFermeture();
		final MotifRattachement motifRattachement = forFiscalView.getMotifRattachement();
		final TypeAutoriteFiscale typeAutoriteFiscale = forFiscalView.getTypeAutoriteFiscale();
		final int autoriteFiscale = forFiscalView.getNumeroAutoriteFiscale();

		return tiersService.addForAutreElementImposable(contribuable, dateDebut, motifOuverture, dateFin, motifFermeture, motifRattachement, typeAutoriteFiscale, autoriteFiscale);
	}

	private ForFiscal addForSecondaire(ForFiscalView forFiscalView) {

		final Contribuable contribuable = (Contribuable) tiersDAO.get(forFiscalView.getNumeroCtb());
		if (contribuable == null) {
			throw new ObjectNotFoundException("Le contribuable avec l'id=" + forFiscalView.getNumeroCtb() + " n'existe pas.");
		}

		Assert.isEqual(GenreImpot.REVENU_FORTUNE, forFiscalView.getGenreImpot());
		final RegDate dateDebut = forFiscalView.getRegDateOuverture();
		final MotifFor motifOuverture = forFiscalView.getMotifOuverture();
		final RegDate dateFin = forFiscalView.getRegDateFermeture();
		final MotifFor motifFermeture = forFiscalView.getMotifFermeture();
		final MotifRattachement motifRattachement = forFiscalView.getMotifRattachement();
		final TypeAutoriteFiscale typeAutoriteFiscale = forFiscalView.getTypeAutoriteFiscale();
		final int autoriteFiscale = forFiscalView.getNumeroAutoriteFiscale();

		return tiersService.addForSecondaire(contribuable, dateDebut, dateFin, motifRattachement, autoriteFiscale, typeAutoriteFiscale, motifOuverture, motifFermeture);
	}

	private ForFiscal addForPrincipal(ForFiscalView forFiscalView) {

		final Contribuable contribuable = (Contribuable) tiersDAO.get(forFiscalView.getNumeroCtb());
		if (contribuable == null) {
			throw new ObjectNotFoundException("Le contribuable avec l'id=" + forFiscalView.getNumeroCtb() + " n'existe pas.");
		}

		Assert.isEqual(GenreImpot.REVENU_FORTUNE, forFiscalView.getGenreImpot());
		final RegDate dateDebut = forFiscalView.getRegDateOuverture();
		final MotifFor motifOuverture = forFiscalView.getMotifOuverture();
		final RegDate dateFin = forFiscalView.getRegDateFermeture();
		final MotifFor motifFermeture = forFiscalView.getMotifFermeture();
		final MotifRattachement motifRattachement = forFiscalView.getMotifRattachement();
		final TypeAutoriteFiscale typeAutoriteFiscale = forFiscalView.getTypeAutoriteFiscale();
		final int autoriteFiscale = forFiscalView.getNumeroAutoriteFiscale();
		final ModeImposition modeImposition = forFiscalView.getModeImposition();

		return tiersService.addForPrincipal(contribuable, dateDebut, motifOuverture, dateFin, motifFermeture, motifRattachement, autoriteFiscale, typeAutoriteFiscale, modeImposition);
	}

	/**
	 * Annulation du for
	 */
	@Override
	@Transactional(rollbackFor = Throwable.class)
	public void annulerFor(Long idFor) {
		ForFiscal forFiscal = forFiscalDAO.get(idFor);
		if (forFiscal == null) {
			throw new ObjectNotFoundException("Le for fiscal n°" + idFor + " n'existe pas.");
		}
		tiersService.annuleForFiscal(forFiscal, true);
	}

	@Override
	@Transactional(rollbackFor = Throwable.class)
	public void reouvrirFor(Long idFor) {
		ForFiscal forFiscal = forFiscalDAO.get(idFor);
		if (forFiscal == null) {
			throw new ObjectNotFoundException("Le for fiscal n°" + idFor + " n'existe pas.");
		}
		tiersService.traiterReOuvertureForDebiteur(forFiscal);
	}

}
