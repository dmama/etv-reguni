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
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.ForFiscalDAO;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.NatureTiers;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.view.ForFiscalView;
import ch.vd.uniregctb.tiers.view.TiersEditView;
import ch.vd.uniregctb.type.GenreImpot;
import ch.vd.uniregctb.type.ModeImposition;
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
	 * @param numero un numéro de tiers
	 * @return un objet TiersView
	 * @throws ServiceInfrastructureException
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
	public ForFiscalPrincipal updateModeImposition(ForFiscalView forFiscalView) {
		Assert.notNull(forFiscalView.getRegDateChangement());
		Contribuable contribuable = (Contribuable) tiersDAO.get(forFiscalView.getNumeroCtb());
		return tiersService.changeModeImposition(contribuable, forFiscalView.getRegDateChangement(), forFiscalView.getModeImposition(), forFiscalView.getMotifImposition());
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
