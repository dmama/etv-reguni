package ch.vd.uniregctb.evenement.ech.manager;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceAware;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adresse.AdresseEnvoi;
import ch.vd.uniregctb.adresse.AdresseException;
import ch.vd.uniregctb.adresse.AdresseGenerique;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.adresse.TypeAdresseFiscale;
import ch.vd.uniregctb.common.ObjectNotFoundException;
import ch.vd.uniregctb.common.ParamPagination;
import ch.vd.uniregctb.evenement.civil.EvenementCivilCriteria;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEch;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEchDAO;
import ch.vd.uniregctb.evenement.common.manager.EvenementCivilManageImpl;
import ch.vd.uniregctb.evenement.common.view.TiersAssocieView;
import ch.vd.uniregctb.evenement.ech.view.EvenementCivilEchCriteriaView;
import ch.vd.uniregctb.evenement.ech.view.EvenementCivilEchElementListeRechercheView;
import ch.vd.uniregctb.evenement.ech.view.EvenementCivilEchDetailView;
import ch.vd.uniregctb.individu.HostCivilService;
import ch.vd.uniregctb.interfaces.model.Commune;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.model.Pays;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureException;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.IndividuNotFoundException;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.PlusieursPersonnesPhysiquesAvecMemeNumeroIndividuException;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.utils.WebContextUtils;

/**
 * @inheritDoc
 *
 */
public class EvenementCivilEchManagerImpl extends EvenementCivilManageImpl implements EvenementCivilEchManager, MessageSourceAware {

	private final Logger LOGGER = Logger.getLogger(EvenementCivilEchManagerImpl.class);

	private AdresseService adresseService;
	private TiersService tiersService;
	private TiersDAO tiersDAO;
	private ServiceCivilService serviceCivilService;
	private ServiceInfrastructureService serviceInfrastructureService;
	private MessageSource messageSource;
	private HostCivilService hostCivilService;
	private EvenementCivilEchDAO evenementCivilEchDAO;

	@SuppressWarnings("unused")
	public void setEvenementCivilEchDAO(EvenementCivilEchDAO evenementCivilEchDAO) {
		this.evenementCivilEchDAO = evenementCivilEchDAO;
	}

	@SuppressWarnings("unused")
	public void setHostCivilService(HostCivilService hostCivilService) {
		this.hostCivilService = hostCivilService;
	}

	public TiersDAO getTiersDAO() {
		return tiersDAO;
	}

	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

	@SuppressWarnings("unused")
	public void setServiceInfrastructureService(ServiceInfrastructureService serviceInfrastructureService) {
		this.serviceInfrastructureService = serviceInfrastructureService;
	}

	/**
	 * @inheritDoc
	 */
	@Override
	@Transactional(readOnly = true)
	public EvenementCivilEchDetailView get(Long id) throws AdresseException, ServiceInfrastructureException {

		final EvenementCivilEchDetailView evtView = new EvenementCivilEchDetailView();
		final EvenementCivilEch evt = evenementCivilEchDAO.get(id);
		if (evt == null) {
			throw new ObjectNotFoundException(this.getMessageSource().getMessage("error.evenement.inexistant" , null,  WebContextUtils.getDefaultLocale()));
		}
		evtView.setEvenement(evt);
		evtView.setIndividu(hostCivilService.getIndividu(evtView.getEvenement().getNumeroIndividu()));

		final Individu individu = serviceCivilService.getIndividu(evtView.getEvenement().getNumeroIndividu(), null);
		final AdresseEnvoi adresse = adresseService.getAdresseEnvoi(individu, RegDate.get(), false);
		evtView.setAdresse(adresse);

		final List<String> erreursTiersAssocies = new ArrayList<String>();
		final List<TiersAssocieView> tiersAssocies = new ArrayList<TiersAssocieView>();

		try {
			final PersonnePhysique habitantPrincipal = tiersDAO.getPPByNumeroIndividu(evtView.getEvenement().getNumeroIndividu());
			if (habitantPrincipal != null) {
				final TiersAssocieView tiersAssocie = createTiersAssocieView(habitantPrincipal);
				tiersAssocie.setLocaliteOuPays(getLocaliteOuPays(habitantPrincipal));
				final ForFiscalPrincipal forFiscalPrincipal = habitantPrincipal.getDernierForFiscalPrincipal();
				if (forFiscalPrincipal != null) {
					final Integer numeroOfsAutoriteFiscale = forFiscalPrincipal.getNumeroOfsAutoriteFiscale();
					final Commune commune = serviceInfrastructureService.getCommuneByNumeroOfsEtendu(numeroOfsAutoriteFiscale, forFiscalPrincipal.getDateFin());
					if (commune != null) {
						tiersAssocie.setForPrincipal(commune.getNomMinuscule());
					}
					tiersAssocie.setDateOuvertureFor(forFiscalPrincipal.getDateDebut());
					tiersAssocie.setDateFermetureFor(forFiscalPrincipal.getDateFin());
				}
				tiersAssocies.add(tiersAssocie);
			}

			final EnsembleTiersCouple ensembleTiersCouple = tiersService.getEnsembleTiersCouple(habitantPrincipal, RegDate.get());
			if (ensembleTiersCouple != null) {
				final MenageCommun menageCommun = ensembleTiersCouple.getMenage();
				if (menageCommun != null) {
					final TiersAssocieView tiersAssocie = createTiersAssocieView(menageCommun);
					tiersAssocies.add(tiersAssocie);
				}
			}
		}
		catch (PlusieursPersonnesPhysiquesAvecMemeNumeroIndividuException e) {
			LOGGER.warn(String.format("Détermination impossible des tiers associés à l'événement civil %d : %s", id, e.getMessage()));
			erreursTiersAssocies.add(e.getMessage());
		}

		if (!tiersAssocies.isEmpty()) {
			evtView.setTiersAssocies(tiersAssocies);
		}
		if (!erreursTiersAssocies.isEmpty()) {
			evtView.setErreursTiersAssocies(erreursTiersAssocies);
		}

		return evtView;
	}

	/**
	 * @inheritDoc
	 */
	private String getLocaliteOuPays(Tiers tiers) throws AdresseException {

		String localiteOuPays = "";

		AdresseGenerique adresseCourrier = adresseService.getAdresseFiscale(tiers, TypeAdresseFiscale.COURRIER, null, false);
		if (adresseCourrier != null) {
			Integer noOfsPays = adresseCourrier.getNoOfsPays();
			Pays pays;
			pays = (noOfsPays == null ? null : serviceInfrastructureService.getPays(noOfsPays));
			if (pays != null && !pays.isSuisse()) {
				localiteOuPays = pays.getNomMinuscule();
			}
			else {
				localiteOuPays = adresseCourrier.getLocalite();
			}
		}
		return localiteOuPays;
	}


	/**
	 * @inheritDoc
	 */
	private TiersAssocieView createTiersAssocieView(Tiers tiers) throws AdresseException, ServiceInfrastructureException {
		final TiersAssocieView tiersAssocie = new TiersAssocieView();
		tiersAssocie.setNumero(tiers.getNumero());

		final List<String> nomCourrier = adresseService.getNomCourrier(tiers, null, false);
		tiersAssocie.setNomCourrier(nomCourrier);

		tiersAssocie.setLocaliteOuPays(getLocaliteOuPays(tiers));
		final ForFiscalPrincipal forFiscalPrincipal = tiers.getDernierForFiscalPrincipal();
		if (forFiscalPrincipal != null) {
			final Integer numeroOfsAutoriteFiscale = forFiscalPrincipal.getNumeroOfsAutoriteFiscale();
			final Commune commune = serviceInfrastructureService.getCommuneByNumeroOfsEtendu(numeroOfsAutoriteFiscale, forFiscalPrincipal.getDateFin());
			if (commune != null) {
				tiersAssocie.setForPrincipal(commune.getNomMinuscule());
			}
			tiersAssocie.setDateOuvertureFor(forFiscalPrincipal.getDateDebut());
			tiersAssocie.setDateFermetureFor(forFiscalPrincipal.getDateFin());
		}

		return tiersAssocie;
	}


	/**
	 * @inheritDoc
	 *
	 */
	@Override
	public void traiteEvenementCivil(Long id) {
		// TODO FRED Implementer
		// - Seulement le premier evenement d'un individu peut etre recyclé
		// - Il faut reposter l'evenement dans la notification queue EvenementCivilNotificationQueueImpl

	}


	/**
	 * @inheritDoc
	 *
	 */
	@Override
	@Transactional(rollbackFor = Throwable.class)
	public void forceEtatTraite(Long id) {
		// TODO FRED Implementer
		//  - Seulement le premier evenement d'un individu peut etre forcé
		// - Il faut reposter l'evenement dans la notification queue EvenementCivilNotificationQueueImpl

//		// l'état "FORCE" n'est accessible qu'aux événements civils qui ne sont pas encore traités
//		if (!evenementCivilExterne.getEtat().isTraite() || evenementCivilExterne.getEtat() == EtatEvenementCivil.A_VERIFIER) {
//			evenementCivilProcessor.forceEvenementCivil(evenementCivilExterne);
//		}

	}

	/**
	 * @inheritDoc
	 */
	@Override
	@Transactional(readOnly = true)
	public List<EvenementCivilEchElementListeRechercheView> find(EvenementCivilEchCriteriaView bean, ParamPagination pagination) throws AdresseException {
		final List<EvenementCivilEchElementListeRechercheView> evtsElementListeRechercheView = new ArrayList<EvenementCivilEchElementListeRechercheView>();
		final List<EvenementCivilEch> evts = evenementCivilEchDAO.find(bean, pagination);
		for (EvenementCivilEch evt : evts) {
			final EvenementCivilEchElementListeRechercheView evtElementListeRechercheView = buildView(evt);
			evtsElementListeRechercheView.add(evtElementListeRechercheView);
		}

		return evtsElementListeRechercheView;
	}

	private EvenementCivilEchElementListeRechercheView buildView(EvenementCivilEch evt) throws AdresseException {
		final EvenementCivilEchElementListeRechercheView evtElementListeRechercheView = new EvenementCivilEchElementListeRechercheView(evt, tiersDAO);
		final PersonnePhysique personnePhysique = evtElementListeRechercheView.getPersonnePhysique();
		try {
			if (personnePhysique != null) {
				final EnsembleTiersCouple couple = tiersService.getEnsembleTiersCouple(personnePhysique, null);
				if (couple != null && couple.getMenage() != null) {
					evtElementListeRechercheView.setNumeroCTB(couple.getMenage().getNumero());
				}
				else {
					evtElementListeRechercheView.setNumeroCTB(personnePhysique.getNumero());
				}
			}
			if (evt.getNumeroIndividu() != null) {
				String nom1 = adresseService.getNomCourrier(evt.getNumeroIndividu());
				evtElementListeRechercheView.setNom(nom1);
			}
		}
		catch (IndividuNotFoundException e) {
			LOGGER.warn("Impossible d'afficher toutes les données de l'événement civil n°" + evt.getId(), e);
			evtElementListeRechercheView.setNom("<erreur: individu introuvable>");
		}
		return evtElementListeRechercheView;
	}

	/**
	 * @inheritDoc
	 */
	@Override
	@Transactional(readOnly = true)
	public int count(EvenementCivilCriteria criterion) {
		return evenementCivilEchDAO.count(criterion);
	}


	public TiersService getTiersService() {
		return tiersService;
	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	public void setAdresseService(AdresseService adresseService) {
		this.adresseService = adresseService;
	}

	@SuppressWarnings("unused")
	public void setServiceCivilService(ServiceCivilService serviceCivilService) {
		this.serviceCivilService = serviceCivilService;
	}

	protected MessageSource getMessageSource() {
		return messageSource;
	}

	@Override
	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

}