package ch.vd.unireg.rt.manager;

import java.util.List;

import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceAware;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.adresse.AdresseException;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.adresse.AdressesResolutionException;
import ch.vd.unireg.common.ActionException;
import ch.vd.unireg.common.ObjectNotFoundException;
import ch.vd.unireg.common.TiersNotFoundException;
import ch.vd.unireg.general.manager.TiersGeneralManager;
import ch.vd.unireg.general.view.TiersGeneralView;
import ch.vd.unireg.rapport.SensRapportEntreTiers;
import ch.vd.unireg.rt.view.DebiteurListView;
import ch.vd.unireg.rt.view.RapportPrestationView;
import ch.vd.unireg.rt.view.SourcierListView;
import ch.vd.unireg.security.SecurityHelper;
import ch.vd.unireg.security.SecurityProviderInterface;
import ch.vd.unireg.tiers.DebiteurPrestationImposable;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.RapportEntreTiers;
import ch.vd.unireg.tiers.RapportEntreTiersDAO;
import ch.vd.unireg.tiers.RapportPrestationImposable;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.tiers.TiersCriteria;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.type.Niveau;
import ch.vd.unireg.utils.WebContextUtils;

public class RapportPrestationEditManagerImpl implements RapportPrestationEditManager, MessageSourceAware{

	private TiersService tiersService;

	private AdresseService adresseService;

	private TiersGeneralManager tiersGeneralManager;

	private RapportEntreTiersDAO rapportEntreTiersDAO;

	private MessageSource messageSource;
	private SecurityProviderInterface securityProvider;


	public void setRapportEntreTiersDAO(RapportEntreTiersDAO rapportEntreTiersDAO) {
		this.rapportEntreTiersDAO = rapportEntreTiersDAO;
	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	public TiersGeneralManager getTiersGeneralManager() {
		return tiersGeneralManager;
	}

	public void setTiersGeneralManager(TiersGeneralManager tiersGeneralManager) {
		this.tiersGeneralManager = tiersGeneralManager;
	}

	public AdresseService getAdresseService() {
		return adresseService;
	}

	public void setAdresseService(AdresseService adresseService) {
		this.adresseService = adresseService;
	}

	public void setSecurityProvider(SecurityProviderInterface securityProvider) {
		this.securityProvider = securityProvider;
	}

	/**
	 * Alimente la vue RapportView
	 *
	 * @param numeroSrc
	 * @param numeroDpi
	 * @return
	 * @throws AdressesResolutionException
	 */
	@Override
	@Transactional(readOnly = true)
	public RapportPrestationView get (Long numeroSrc, Long numeroDpi, String provenance) {
		RapportPrestationView rapportView =  new RapportPrestationView();

		rapportView.setProvenance(provenance);

		PersonnePhysique sourcier = (PersonnePhysique) tiersService.getTiers(numeroSrc);
		if (sourcier == null) {
			throw new ObjectNotFoundException(messageSource.getMessage("error.sourcier.inexistant", null, WebContextUtils.getDefaultLocale()));
		}
		TiersGeneralView sourcierView = tiersGeneralManager.getPersonnePhysique(sourcier, true);
		rapportView.setSourcier(sourcierView);

		DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersService.getTiers(numeroDpi);
		if (dpi == null) {
			throw new ObjectNotFoundException(messageSource.getMessage("error.debiteur.inexistant", null, WebContextUtils.getDefaultLocale()));
		}
		TiersGeneralView dpiView = tiersGeneralManager.getDebiteur(dpi, true);
		rapportView.setDebiteur(dpiView);

		return rapportView;
	}

	/**
	 * Alimente la vue RapportView
	 * @return
	 */
	public RapportPrestationView get (Long idRapport, SensRapportEntreTiers sensRapportEntreTiers) throws AdresseException {
		RapportPrestationView rapportView =  new RapportPrestationView();

		RapportEntreTiers rapportEntreTiers	= rapportEntreTiersDAO.get(idRapport);
		if (rapportEntreTiers == null) {
			throw new ObjectNotFoundException(messageSource.getMessage("error.rapport.inexistant", null, WebContextUtils.getDefaultLocale()));
		}

		rapportView.setSensRapportEntreTiers(sensRapportEntreTiers);
		rapportView.setTypeRapportEntreTiers(rapportEntreTiers.getType());
		Long numero = null;
		if (sensRapportEntreTiers == SensRapportEntreTiers.OBJET) {
			numero = rapportEntreTiers.getSujetId();
		}
		if (sensRapportEntreTiers == SensRapportEntreTiers.SUJET) {
			numero = rapportEntreTiers.getObjetId();
		}
		setNomCourrier(rapportView, numero);
		rapportView.setId(rapportEntreTiers.getId());
		rapportView.setDateDebut(rapportEntreTiers.getDateDebut());
		rapportView.setDateFin(rapportEntreTiers.getDateFin());
		rapportView.setNatureRapportEntreTiers(rapportEntreTiers.getClass().getSimpleName());
		if (rapportEntreTiers instanceof RapportPrestationImposable) {
			RapportPrestationImposable rapportPrestationImposable = (RapportPrestationImposable) rapportEntreTiers;
			rapportView.setNatureRapportEntreTiers(rapportPrestationImposable.getClass().getSimpleName());
		}

		return rapportView;
	}

	/**
	 * Mise à jour de numero, nomCourrier1 et nomCourrier2 en fonction du numero
	 * @param rapportView
	 * @param numero
	 * @throws AdressesResolutionException
	 */
	private void setNomCourrier(RapportPrestationView rapportView, Long numero) throws AdresseException {
		rapportView.setNumero(numero);

		final Tiers tiers = tiersService.getTiers(numero);
		if (tiers == null) {
			throw new TiersNotFoundException(numero);
		}
		List<String> nomCourrier = getAdresseService().getNomCourrier(tiers, null, false);
		rapportView.setNomCourrier(nomCourrier);

	}


	/**
	 * Persiste le rapport de travail
	 * @param rapportView
	 */
	@Override
	@Transactional(rollbackFor = Throwable.class)
	public void save(RapportPrestationView rapportView) {
		final PersonnePhysique sourcier = (PersonnePhysique) tiersService.getTiers(rapportView.getSourcier().getNumero());
		final DebiteurPrestationImposable debiteur = (DebiteurPrestationImposable) tiersService.getTiers(rapportView.getDebiteur().getNumero());

		final List<RapportPrestationImposable> existants = tiersService.getAllRapportPrestationImposable(debiteur, sourcier, true, true);
		if (existants != null && !existants.isEmpty()) {
			final DateRange newRange = new DateRangeHelper.Range(rapportView.getDateDebut(), null);
			if (DateRangeHelper.intersect(newRange, existants)) {
				throw new ActionException("Un rapport de travail existe déjà entre ces mêmes débiteur et sourcier sur une période d'au moins un jour après le " + RegDateHelper.dateToDisplayString(newRange.getDateDebut()));
			}
		}

		tiersService.addRapportPrestationImposable(sourcier, debiteur, rapportView.getDateDebut(), null);
	}

	/**
	 * Charge l'écran de recherche débiteurs pour un sourcier
	 *
	 * @param numeroSrc
	 * @return
	 */
	@Override
	@Transactional(readOnly = true)
	public DebiteurListView getDebiteurList(Long numeroSrc) {
		PersonnePhysique pp = (PersonnePhysique) tiersService.getTiers(numeroSrc) ;
		DebiteurListView bean = new DebiteurListView();
		TiersGeneralView sourcierView = tiersGeneralManager.getPersonnePhysique(pp, true);
		bean.setSourcier(sourcierView);
		bean.setTypeRechercheDuNom(TiersCriteria.TypeRecherche.EST_EXACTEMENT);
		bean.setTypeTiersImperatif(TiersCriteria.TypeTiers.DEBITEUR_PRESTATION_IMPOSABLE);
		bean.setNumeroSourcier(numeroSrc);
		return bean;
	}

	/**
	 * Charge l'écran de recherche sourciers pour un debiteur
	 *
	 * @param numeroDpi
	 * @return
	 */
	@Override
	@Transactional(readOnly = true)
	public SourcierListView getSourcierList(Long numeroDpi) {
		DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersService.getTiers(numeroDpi) ;
		SourcierListView bean = new SourcierListView();
		TiersGeneralView dpiView = tiersGeneralManager.getDebiteur(dpi, true);
		bean.setDebiteur(dpiView);
		bean.setTypeRechercheDuNom(TiersCriteria.TypeRecherche.EST_EXACTEMENT);
		bean.setTypeTiersImperatif(TiersCriteria.TypeTiers.PERSONNE_PHYSIQUE);
		bean.setNumeroDebiteur(numeroDpi);
		return bean;
	}

	@Override
	@Transactional(readOnly = true)
	public Niveau getAccessLevel(long tiersId) {
		final Tiers tiers = tiersService.getTiers(tiersId);
		return SecurityHelper.getDroitAcces(securityProvider, tiers);
	}

	@Override
	@Transactional(readOnly = true)
	public boolean isExistingTiers(long tiersId) {
		final Tiers tiers = tiersService.getTiers(tiersId);
		return tiers != null;
	}

	@Override
	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}
}
