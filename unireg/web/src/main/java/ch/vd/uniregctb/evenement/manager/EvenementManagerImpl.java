package ch.vd.uniregctb.evenement.manager;

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
import ch.vd.uniregctb.adresse.AdressesResolutionException;
import ch.vd.uniregctb.adresse.TypeAdresseFiscale;
import ch.vd.uniregctb.common.ObjectNotFoundException;
import ch.vd.uniregctb.common.WebParamPagination;
import ch.vd.uniregctb.evenement.civil.engine.regpp.EvenementCivilProcessor;
import ch.vd.uniregctb.evenement.civil.regpp.EvenementCivilRegPP;
import ch.vd.uniregctb.evenement.civil.regpp.EvenementCivilRegPPCriteria;
import ch.vd.uniregctb.evenement.civil.regpp.EvenementCivilRegPPDAO;
import ch.vd.uniregctb.evenement.civil.regpp.EvenementService;
import ch.vd.uniregctb.evenement.view.EvenementCivilView;
import ch.vd.uniregctb.evenement.view.EvenementCriteriaView;
import ch.vd.uniregctb.evenement.view.EvenementView;
import ch.vd.uniregctb.evenement.view.TiersAssocieView;
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
 * Classe qui permet de collecter les informations nécessaires à l'affichage
 *
 * @author xcifde
 *
 */
public class EvenementManagerImpl implements EvenementManager, MessageSourceAware {

	private final Logger LOGGER = Logger.getLogger(EvenementManagerImpl.class);

	private AdresseService adresseService;
	private TiersService tiersService;
	private TiersDAO tiersDAO;
	private EvenementCivilProcessor evenementCivilProcessor;
	private ServiceCivilService serviceCivilService;
	private EvenementService evenementService;
	private ServiceInfrastructureService serviceInfrastructureService;
	private MessageSource messageSource;
	private HostCivilService hostCivilService;
	private EvenementCivilRegPPDAO evenementCivilRegPPDAO;

	public void setEvenementCivilProcessor(EvenementCivilProcessor evenementCivilProcessor) {
		this.evenementCivilProcessor = evenementCivilProcessor;
	}

	public void setEvenementCivilRegPPDAO(EvenementCivilRegPPDAO evenementCivilRegPPDAO) {
		this.evenementCivilRegPPDAO = evenementCivilRegPPDAO;
	}

	public void setHostCivilService(HostCivilService hostCivilService) {
		this.hostCivilService = hostCivilService;
	}

	public TiersDAO getTiersDAO() {
		return tiersDAO;
	}

	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

	public void setServiceInfrastructureService(ServiceInfrastructureService serviceInfrastructureService) {
		this.serviceInfrastructureService = serviceInfrastructureService;
	}

	/**
	 * Charge la structure EvenementView en fonction des informations de
	 * l'événement
	 *
	 * @param id ID d'evenement
	 * @return
	 * @throws ServiceInfrastructureException
	 */
	@Override
	@Transactional(readOnly = true)
	public EvenementView get(Long id) throws AdresseException, ServiceInfrastructureException {

		final EvenementView evtView = new EvenementView();
		final EvenementCivilRegPP evt = evenementCivilRegPPDAO.get(id);
		if (evt == null) {
			throw new ObjectNotFoundException(this.getMessageSource().getMessage("error.evenement.inexistant" , null,  WebContextUtils.getDefaultLocale()));
		}
		evtView.setEvenement(evt);

		evtView.setIndividuPrincipal(hostCivilService.getIndividu(evtView.getEvenement().getNumeroIndividuPrincipal()));

		final Individu individu = serviceCivilService.getIndividu(evtView.getEvenement().getNumeroIndividuPrincipal(), null);
		final AdresseEnvoi adressePrincipal = adresseService.getAdresseEnvoi(individu, RegDate.get(), false);
		evtView.setAdressePrincipal(adressePrincipal);

		final Long conjoint = evtView.getEvenement().getNumeroIndividuConjoint();
		if (conjoint != null) {
			evtView.setIndividuConjoint(hostCivilService.getIndividu(conjoint));
			final Individu conjointInd = serviceCivilService.getIndividu(evtView.getEvenement().getNumeroIndividuPrincipal(), null);
			AdresseEnvoi adresseConjoint = adresseService.getAdresseEnvoi(conjointInd, RegDate.get(), false);
			evtView.setAdresseConjoint(adresseConjoint);
		}

		final List<String> erreursTiersAssocies = new ArrayList<String>();
		final List<TiersAssocieView> tiersAssocies = new ArrayList<TiersAssocieView>();

		try {
			final PersonnePhysique habitantPrincipal = tiersDAO.getPPByNumeroIndividu(evtView.getEvenement().getNumeroIndividuPrincipal());
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

		if (evtView.getEvenement().getNumeroIndividuConjoint() != null) {
			try {
				final PersonnePhysique habitantConjoint = tiersDAO.getPPByNumeroIndividu(evtView.getEvenement().getNumeroIndividuConjoint());
				if (habitantConjoint != null) {
					final TiersAssocieView tiersAssocie = createTiersAssocieView(habitantConjoint);
					tiersAssocies.add(tiersAssocie);
				}
			}
			catch (PlusieursPersonnesPhysiquesAvecMemeNumeroIndividuException e) {
				LOGGER.warn(String.format("Détermination impossible des tiers associés à l'événement civil %d : %s", id, e.getMessage()));
				erreursTiersAssocies.add(e.getMessage());
			}
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
	 * Retourne la localite ou le pays de l'adresse courrier active du tiers
	 *
	 * @return
	 * @throws AdressesResolutionException
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
	 * Alimente un objet TiersAssocieView
	 * @param numero
	 * @return
	 * @throws AdressesResolutionException
	 * @throws ServiceInfrastructureException
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
	 * Traite un evenement civil designe par l'id
	 *
	 * @param id
	 */
	@Override
	public void traiteEvenementCivil(Long id) {
		evenementCivilProcessor.recycleEvenementCivil(id);
	}


	/**
	 * Force l'etat de l'evenement à TRAITE (en fait : FORCE)
	 *
	 * @param id
	 */
	@Override
	@Transactional(rollbackFor = Throwable.class)
	public void forceEtatTraite(Long id) {
		final EvenementCivilRegPP evenementCivilExterne = evenementCivilRegPPDAO.get(id);

		// l'état "FORCE" n'est accessible qu'aux événements civils qui ne sont pas encore traités
		if (!evenementCivilExterne.getEtat().isTraite()) {
			evenementCivilProcessor.forceEvenementCivil(evenementCivilExterne);
		}
	}

	/**
	 * Recherche des événements correspondant aux critères
	 * @param bean
	 * @param pagination
	 * @return
	 * @throws AdressesResolutionException
	 */
	@Override
	@Transactional(readOnly = true)
	public List<EvenementCivilView> find(EvenementCriteriaView bean, WebParamPagination pagination) throws AdresseException {
		final List<EvenementCivilView> evtsView = new ArrayList<EvenementCivilView>();
		final List<EvenementCivilRegPP> evts = evenementService.find(bean, pagination);
		for (EvenementCivilRegPP evt : evts) {
			final EvenementCivilView evtView = buildView(evt);
			evtsView.add(evtView);
		}

		return evtsView;
	}

	private EvenementCivilView buildView(EvenementCivilRegPP evt) throws AdresseException {
		final EvenementCivilView evtView = new EvenementCivilView(evt, tiersDAO);
		final PersonnePhysique habitantPrincipal = evtView.getHabitantPrincipal();
		try {
			if (habitantPrincipal != null) {
				final EnsembleTiersCouple couple = tiersService.getEnsembleTiersCouple(habitantPrincipal, null);
				if (couple != null && couple.getMenage() != null) {
					evtView.setNumeroCTB(couple.getMenage().getNumero());
				}
				else {
					evtView.setNumeroCTB(habitantPrincipal.getNumero());
				}
			}
			if (evt.getNumeroIndividuPrincipal() != null) {
				String nom1 = adresseService.getNomCourrier(evt.getNumeroIndividuPrincipal());
				evtView.setNom1(nom1);
			}
			if (evt.getNumeroIndividuConjoint() != null) {
				String nom2 = adresseService.getNomCourrier(evt.getNumeroIndividuConjoint());
				evtView.setNom2(nom2);
			}
		}
		catch (IndividuNotFoundException e) {
			// [UNIREG-1545] on cas d'incoherence des données, on évite de crasher (dans la mesure du possible)
			LOGGER.warn("Impossible d'afficher toutes les données de l'événement civil n°" + evt.getId(), e);
			evtView.setNom1("<erreur: individu introuvable>");
		}
		return evtView;
	}

	/**
	 * Cherche et compte les evenements correspondant aux criteres
	 * @param criterion
	 * @return
	 */
	@Override
	@Transactional(readOnly = true)
	public int count(EvenementCivilRegPPCriteria criterion) {
		return evenementService.count(criterion);
	}


	public TiersService getTiersService() {
		return tiersService;
	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	public AdresseService getAdresseService() {
		return adresseService;
	}

	public void setAdresseService(AdresseService adresseService) {
		this.adresseService = adresseService;
	}

	public ServiceCivilService getServiceCivilService() {
		return serviceCivilService;
	}

	public void setServiceCivilService(ServiceCivilService serviceCivilService) {
		this.serviceCivilService = serviceCivilService;
	}

	/**
	 * @return the messageSource
	 */
	protected MessageSource getMessageSource() {
		return messageSource;
	}

	@Override
	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	public EvenementService getEvenementService() {
		return evenementService;
	}

	public void setEvenementService(EvenementService evenementService) {
		this.evenementService = evenementService;
	}

}