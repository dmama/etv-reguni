package ch.vd.uniregctb.evenement.organisation.manager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.HibernateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureException;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.interfaces.infra.data.Pays;
import ch.vd.unireg.interfaces.organisation.ServiceOrganisationException;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.interfaces.organisation.data.ServiceOrganisationEvent;
import ch.vd.uniregctb.adresse.AdresseEnvoi;
import ch.vd.uniregctb.adresse.AdresseException;
import ch.vd.uniregctb.adresse.AdresseGenerique;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.adresse.TypeAdresseFiscale;
import ch.vd.uniregctb.common.ObjectNotFoundException;
import ch.vd.uniregctb.common.pagination.ParamPagination;
import ch.vd.uniregctb.evenement.common.view.TiersAssocieView;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationBasicInfo;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationErreur;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationException;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationProcessingMode;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationService;
import ch.vd.uniregctb.evenement.organisation.engine.EvenementOrganisationNotificationQueue;
import ch.vd.uniregctb.evenement.organisation.engine.processor.EvenementOrganisationProcessor;
import ch.vd.uniregctb.evenement.organisation.view.ErreurEvenementOrganisationView;
import ch.vd.uniregctb.evenement.organisation.view.EvenementOrganisationCriteriaView;
import ch.vd.uniregctb.evenement.organisation.view.EvenementOrganisationDetailView;
import ch.vd.uniregctb.evenement.organisation.view.EvenementOrganisationElementListeRechercheView;
import ch.vd.uniregctb.evenement.organisation.view.EvenementOrganisationSummaryView;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.interfaces.service.ServiceOrganisationService;
import ch.vd.uniregctb.organisation.OrganisationView;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.OrganisationNotFoundException;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersException;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.EtatEvenementOrganisation;
import ch.vd.uniregctb.utils.WebContextUtils;

public class EvenementOrganisationManagerImpl implements EvenementOrganisationManager {

	private static final Logger LOGGER = LoggerFactory.getLogger(EvenementOrganisationManagerImpl.class);

	private static final long TIMEOUT_RECYCLAGE = 5000; // ms

	private AdresseService adresseService;
	private TiersService tiersService;
	private ServiceOrganisationService serviceOrganisationService;
	private ServiceInfrastructureService serviceInfrastructureService;
	private MessageSource messageSource;

	private EvenementOrganisationService evenementService;
	private EvenementOrganisationNotificationQueue evenementNotificationQueue;
	private EvenementOrganisationProcessor evenementProcessor;

	@SuppressWarnings("unused")
	public void setEvenementService(EvenementOrganisationService evenementService) {
		this.evenementService = evenementService;
	}

	@SuppressWarnings("unused")
	public void setEvenementNotificationQueue(EvenementOrganisationNotificationQueue evtOrganisationNotificationQueue) {
		this.evenementNotificationQueue = evtOrganisationNotificationQueue;
	}

	@SuppressWarnings("unused")
	public void setEvenementProcessor(EvenementOrganisationProcessor evenementProcessor) {
		this.evenementProcessor = evenementProcessor;
	}

	public void setAdresseService(AdresseService adresseService) {
		this.adresseService = adresseService;
	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	public void setServiceOrganisationService(ServiceOrganisationService serviceOrganisationService) {
		this.serviceOrganisationService = serviceOrganisationService;
	}

	public void setServiceInfrastructureService(ServiceInfrastructureService serviceInfrastructureService) {
		this.serviceInfrastructureService = serviceInfrastructureService;
	}

	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	@Override
    @Transactional(readOnly = true)
	public EvenementOrganisationDetailView get(Long id) throws AdresseException, ServiceInfrastructureException {
		final EvenementOrganisation evt = evenementService.get(id);
		if (evt == null) {
			throw newObjectNotFoundException(id);
		}
		return buildDetailView(evt);
	}

	@Override
	@Transactional(readOnly = true)
	public EvenementOrganisationSummaryView getSummary(Long id) throws AdresseException, ServiceInfrastructureException {
		final EvenementOrganisation evt = evenementService.get(id);
		if (evt == null) {
			throw newObjectNotFoundException(id);
		}
		return buildSummaryView(evt);
	}

	private EvenementOrganisationDetailView buildDetailView(EvenementOrganisation evt) {
		final String foscPublicationDirectLinkFormat = "https://www.fosc.ch/shabforms/servlet/Search/1925485.pdf?EID=7&DOCID=%d";

		final EvenementOrganisationDetailView evtView = new EvenementOrganisationDetailView();

		evtView.setEvtCommentaireTraitement(evt.getCommentaireTraitement());
		evtView.setEvtDate(evt.getDateEvenement());
		evtView.setEvtDateTraitement(evt.getDateTraitement());
		evtView.setEvtEtat(evt.getEtat());
		evtView.setEvtId(evt.getId());
		evtView.setNoEvenement(evt.getNoEvenement());
		evtView.setEvtType(evt.getType());
		evtView.setCorrectionDansLePasse(evt.getCorrectionDansLePasse());
		if (evt.getReferenceAnnonceIDE() != null) {
			evtView.setAnnonceIDEId(evt.getReferenceAnnonceIDE().getId());
		}

		for (EvenementOrganisationErreur err : evt.getErreurs() ) {
			evtView.addEvtErreur(new ErreurEvenementOrganisationView(err.getId(), err.getMessage(), err.getCallstack()));
		}
		evtView.getEvtErreurs().sort(Comparator.comparingLong(ErreurEvenementOrganisationView::getErrorId));

		final Long numeroOrganisation = evt.getNoOrganisation();
		evtView.setNoOrganisation(numeroOrganisation);
		try {
			final ServiceOrganisationEvent organisationEvent = retrieveOrganisation(evt.getNoEvenement(), numeroOrganisation);

			evtView.setFoscNumero(organisationEvent.getNumeroDocumentFOSC());
			evtView.setFoscDate(organisationEvent.getDatePublicationFOSC());
			if (organisationEvent.getNumeroDocumentFOSC() != null) {
				evtView.setFoscLienDirect(String.format(foscPublicationDirectLinkFormat, organisationEvent.getNumeroDocumentFOSC()));
			}

			evtView.setOrganisation(new OrganisationView(organisationEvent.getPseudoHistory(), evt.getDateEvenement()));
			evtView.setAdresse(retrieveAdresse(numeroOrganisation));
		}
		catch (Exception e) {
			evtView.setOrganisationError(e.getMessage());
		}

		try {
			retrieveTiersAssocie(evt.getNoEvenement(), numeroOrganisation, evtView);
		}
		catch (Exception e) {
			final String previousError = evtView.getOrganisationError();
			evtView.setOrganisationError(StringUtils.isBlank(previousError) ? e.getMessage() : String.format("%s; %s", previousError, e.getMessage()));
		}

		try {
			final List<EvenementOrganisationBasicInfo> list = evenementService.buildLotEvenementsOrganisationNonTraites(numeroOrganisation);
			evtView.setNonTraitesSurMemeOrganisation(list);
			evtView.setRecyclable(evaluateRecyclable(evtView.getEvtId(), list));
		}
		catch (Exception e) {
			final String previousError = evtView.getOrganisationError();
			evtView.setOrganisationError(StringUtils.isBlank(previousError) ? e.getMessage() : String.format("%s; %s", previousError, e.getMessage()));
		}
		evtView.setForcable(evaluateForcable(evt.getEtat(), evtView.isRecyclable()));
		return evtView;
	}

	private EvenementOrganisationSummaryView buildSummaryView(EvenementOrganisation evt) {
		final EvenementOrganisationSummaryView evtView = new EvenementOrganisationSummaryView();

		evtView.setEvtCommentaireTraitement(evt.getCommentaireTraitement());
		evtView.setEvtDate(evt.getDateEvenement());
		evtView.setEvtDateTraitement(evt.getDateTraitement());
		evtView.setEvtEtat(evt.getEtat());
		evtView.setEvtId(evt.getId());
		evtView.setNoEvenement(evt.getNoEvenement());
		evtView.setEvtType(evt.getType());
		evtView.setCorrectionDansLePasse(evt.getCorrectionDansLePasse());
		if (evt.getReferenceAnnonceIDE() != null) {
			evtView.setAnnonceIDEId(evt.getReferenceAnnonceIDE().getId());
		}
		for (EvenementOrganisationErreur err : evt.getErreurs() ) {
			evtView.addEvtErreur(new ErreurEvenementOrganisationView(err.getId(), err.getMessage(), err.getCallstack()));
		}
		evtView.getEvtErreurs().sort(Comparator.comparingLong(ErreurEvenementOrganisationView::getErrorId));

		evtView.setNoOrganisation(evt.getNoOrganisation());

		try {
			final List<EvenementOrganisationBasicInfo> list = evenementService.buildLotEvenementsOrganisationNonTraites(evt.getNoOrganisation());
			evtView.setNonTraitesSurMemeOrganisation(list);
			evtView.setRecyclable(evaluateRecyclable(evtView.getEvtId(), list));
		}
		catch (Exception e) {
			evtView.setErreursEvt(e.getMessage());
		}
		evtView.setForcable(evaluateForcable(evt.getEtat(), evtView.isRecyclable()));
		return evtView;
	}

	private boolean evaluateRecyclable(Long evtId, List<EvenementOrganisationBasicInfo> list) {
		if (list != null && list.size() > 0) {
			final EvenementOrganisationBasicInfo evtPrioritaire = list.get(0);
			if (evtId == evtPrioritaire.getId()) {
				return true;
			}
		}
		return false;
	}

	private boolean evaluateForcable(EtatEvenementOrganisation etat, boolean recyclable) {
		return etat == EtatEvenementOrganisation.A_VERIFIER || recyclable;
	}


	@Override
	@Transactional
	public boolean recycleEvenementOrganisation(Long id) throws EvenementOrganisationException {
		EvenementOrganisation evt = evenementService.get(id);
		if (evt == null) {
			throw newObjectNotFoundException(id);
		}
        else {
            return recycleEvenementOrganisation(evt);
        }
	}

    private boolean recycleEvenementOrganisation(EvenementOrganisation evt) {
        boolean organisationRecycle = false;
        final List<EvenementOrganisationBasicInfo> list = evenementService.buildLotEvenementsOrganisationNonTraites(evt.getNoOrganisation());
        if (list == null || list.isEmpty()) {
            throw new RuntimeException("La liste devrait toujours avoir au moins un élément");
        }
        if (list.get(0).getId() == evt.getId()) {
            // L'evenement est recyclable
            final EvenementOrganisationProcessorListener processorListener = new EvenementOrganisationProcessorListener(evt.getNoOrganisation(), TIMEOUT_RECYCLAGE);
            final EvenementOrganisationProcessor.ListenerHandle handle =  evenementProcessor.registerListener(processorListener);
            try {
                evenementNotificationQueue.post(evt.getNoOrganisation(), EvenementOrganisationProcessingMode.IMMEDIATE);
                organisationRecycle = processorListener.donneUneChanceAuTraitementDeSeTerminer();
            }
            finally {
                handle.unregister();
            }
        }
        else {
            LOGGER.warn(String.format("Tentative incohérente de recyclage de l'événement (%d), ne devrait pas se produire lors de l'utilisation normale de l'application", evt.getId()));
        }
        return organisationRecycle;
    }

    @Override
    @Transactional
	public void forceEvenement(Long id) {
	    final EvenementOrganisation evt = evenementService.get(id);
	    if (evt == null) {
		    throw new ObjectNotFoundException("Evénement organisation " + id);
	    }
	    evenementProcessor.forceEvenement(new EvenementOrganisationBasicInfo(evt, evt.getNoOrganisation()));
	}

	@Override
    @Transactional (readOnly = true)
	public List<EvenementOrganisationElementListeRechercheView> find(EvenementOrganisationCriteriaView bean, ParamPagination pagination) throws AdresseException {
		final List<EvenementOrganisationElementListeRechercheView> evtsElementListeRechercheView = new ArrayList<>();
		if (bean.isModeLotEvenement()) {
			// cas spécial, on veut la liste des evenements en attente pour une organisation
			List<EvenementOrganisation> list = evenementService.getEvenementsNonTraitesOrganisation(bean.getNumeroOrganisation());
			for (int i = (pagination.getNumeroPage() - 1) * pagination.getTaillePage();
			     i < list.size() && i < (pagination.getNumeroPage()) * pagination.getTaillePage(); ++i) {
				EvenementOrganisation evt = list.get(i);
				final EvenementOrganisationElementListeRechercheView evtElementListeRechercheView = buildElementRechercheView(evt);
				evtsElementListeRechercheView.add(evtElementListeRechercheView);
			}
		}
		else {
			final List<EvenementOrganisation> evts = evenementService.find(bean, pagination);
			for (EvenementOrganisation evt : evts) {
				final EvenementOrganisationElementListeRechercheView evtElementListeRechercheView = buildElementRechercheView(evt);
				evtsElementListeRechercheView.add(evtElementListeRechercheView);
			}
		}
		return evtsElementListeRechercheView;
	}

	@Override
	@Transactional(readOnly = true)
	public int count(EvenementOrganisationCriteriaView bean) {
		if (bean.isModeLotEvenement()) {
			return evenementService.buildLotEvenementsOrganisationNonTraites(bean.getNumeroOrganisation()).size();
		} else {
			return evenementService.count(bean);
		}
	}

	/**
	 * Créée une nouvelle entreprise pour l'organisation de l'événement dont l'id est passé en paramètre.
	 * Rapporte cette création dans une entrée "suivi" dans les erreurs de l'événement.
	 *
	 * <p>
	 *     La méthode vérifie que l'entreprise n'existe pas déjà et lance une {@link IllegalStateException} si c'est le cas.
	 * </p>
	 * @param id Le numéro de l'événement
	 * @return L'entreprise créé
	 */
	@Transactional
	@Override
	public Entreprise creerEntreprisePourEvenementOrganisation(Long id) throws TiersException {
		EvenementOrganisation evt = evenementService.get(id);
		return tiersService.createEntreprisePourEvenementOrganisation(evt);
	}

	private EvenementOrganisationElementListeRechercheView buildElementRechercheView(EvenementOrganisation evt) throws AdresseException {
		final EvenementOrganisationElementListeRechercheView view = new EvenementOrganisationElementListeRechercheView(evt);
		final long numeroOrganisation = evt.getNoOrganisation();
		try {
			final RegDate dateEvenement = evt.getDateEvenement();

			view.setNumeroCTB(getNumeroCtbPourNoOrganisation(numeroOrganisation));

			final Organisation organisation = serviceOrganisationService.getOrganisationHistory(evt.getNoOrganisation());
			view.setOrganisation(new OrganisationView(organisation, evt.getDateEvenement()));
			view.setNom(view.getOrganisation().getNom()); // Champ redondant mais nécessaire car utilisé en cas d'erreur de chargement de l'organisation.
			view.setCorrectionDansLePasse(evt.getCorrectionDansLePasse());
			if (evt.getReferenceAnnonceIDE() != null) {
				view.setAnnonceIDEId(evt.getReferenceAnnonceIDE().getId());
			}
			final List<EvenementOrganisationBasicInfo> list = evenementService.buildLotEvenementsOrganisationNonTraites(evt.getNoOrganisation());
			view.setRecyclable(evaluateRecyclable(evt.getId(), list));
			view.setForcable(evaluateForcable(evt.getEtat(), view.isRecyclable()));
		}
		catch (ServiceOrganisationException e) {
			LOGGER.warn("Impossible d'afficher toutes les données de l'événement organisation " + evt.toString(), e);
			view.setNom("<erreur: organisation introuvable>");
		}
		catch (Exception e) {
			LOGGER.warn("Impossible d'afficher toutes les données de l'événement organisation " + evt.toString(), e);
			view.setNom("<erreur: " + e.getMessage() + ">");
		}
		return view;
	}

	private Long getNumeroCtbPourNoOrganisation(long noOrganisation) throws OrganisationNotFoundException {
		Long noCtb = null;
		try {
			final Entreprise entreprise = tiersService.getEntrepriseByNumeroOrganisation(noOrganisation);
			if (entreprise != null) {
				noCtb = entreprise.getNumero();
			}
		}
		catch (HibernateException e) {
			noCtb = -1L;
			LOGGER.warn("Impossible de trouver le contribuable associé à l'organisation " + noOrganisation, e);
		}
		return noCtb;
	}

	private void retrieveTiersAssocie(Long noEvenement, Long numeroOrganisation, EvenementOrganisationDetailView evtView) throws AdresseException {
		try {
			final Entreprise entreprise = tiersService.getEntrepriseByNumeroOrganisation(numeroOrganisation);
			if (entreprise != null) {
				TiersAssocieView tiersAssocie;
				try {
					tiersAssocie = createTiersAssocieView(entreprise);
				}
				catch (ServiceOrganisationException e) {
					tiersAssocie = createTiersAssocieViewSansCivil(entreprise);
				}
				evtView.setTiersAssocie(tiersAssocie);
			}
		}
		catch (RuntimeException e) {
			LOGGER.warn(String.format("Détermination impossible des tiers associés à l'événement civil organisation %d : %s", noEvenement, e.getMessage()));
			evtView.addErreursTiersAssocies(e.getMessage());
		}
	}

	private String retrieveLocaliteOuPays(Tiers tiers) throws AdresseException {

		String localiteOuPays = "";

		final AdresseGenerique adresseCourrier = adresseService.getAdresseFiscale(tiers, TypeAdresseFiscale.COURRIER, null, false);
		if (adresseCourrier != null) {
			final Integer noOfsPays = adresseCourrier.getNoOfsPays();
			final Pays pays = (noOfsPays == null ? null : serviceInfrastructureService.getPays(noOfsPays, adresseCourrier.getDateDebut()));
			if (pays != null && !pays.isSuisse()) {
				localiteOuPays = pays.getNomCourt();
			}
			else {
				localiteOuPays = adresseCourrier.getLocalite();
			}
		}
		return localiteOuPays;
	}

	protected TiersAssocieView createTiersAssocieView(Entreprise entreprise) throws AdresseException, ServiceInfrastructureException {
		final TiersAssocieView tiersAssocie = new TiersAssocieView();
		tiersAssocie.setNumero(entreprise.getNumero());
		final List<String> nomCourrier = adresseService.getNomCourrier(entreprise, null, false);
		tiersAssocie.setNomCourrier(nomCourrier);

		tiersAssocie.setLocaliteOuPays(retrieveLocaliteOuPays(entreprise));
		final ForFiscalPrincipal forFiscalPrincipal = entreprise.getDernierForFiscalPrincipal();
		if (forFiscalPrincipal != null) {
			final Integer numeroOfsAutoriteFiscale = forFiscalPrincipal.getNumeroOfsAutoriteFiscale();
			final Commune commune = serviceInfrastructureService.getCommuneByNumeroOfs(numeroOfsAutoriteFiscale, forFiscalPrincipal.getDateFin());
			if (commune != null) {
				tiersAssocie.setForPrincipal(commune.getNomOfficiel());
			}
			tiersAssocie.setDateOuvertureFor(forFiscalPrincipal.getDateDebut());
			tiersAssocie.setDateFermetureFor(forFiscalPrincipal.getDateFin());
		}

		return tiersAssocie;
	}

	protected TiersAssocieView createTiersAssocieViewSansCivil(Entreprise entreprise) throws AdresseException, ServiceInfrastructureException {
		final TiersAssocieView tiersAssocie = new TiersAssocieView();
		tiersAssocie.setNumero(entreprise.getNumero());
		final String message = "<erreur: organisation introuvable>";

		tiersAssocie.setNomCourrier(Collections.singletonList(message));
		tiersAssocie.setLocaliteOuPays("&lt;erreur: organisation introuvable&gt;");
		final ForFiscalPrincipal forFiscalPrincipal = entreprise.getDernierForFiscalPrincipal();
		if (forFiscalPrincipal != null) {
			final Integer numeroOfsAutoriteFiscale = forFiscalPrincipal.getNumeroOfsAutoriteFiscale();
			final Commune commune = serviceInfrastructureService.getCommuneByNumeroOfs(numeroOfsAutoriteFiscale, forFiscalPrincipal.getDateFin());
			if (commune != null) {
				tiersAssocie.setForPrincipal(commune.getNomOfficiel());
			}
			tiersAssocie.setDateOuvertureFor(forFiscalPrincipal.getDateDebut());
			tiersAssocie.setDateFermetureFor(forFiscalPrincipal.getDateFin());
		}

		return tiersAssocie;
	}


	private static class EvenementOrganisationProcessorListener implements EvenementOrganisationProcessor.Listener {

		private final long dureeTimeout;
		private final Long organisationAttendu;
		private volatile boolean traitementOk = false;

		private EvenementOrganisationProcessorListener(Long organisationAttendu, long dureeTimeout) {
			if (organisationAttendu == null) {
				throw new NullPointerException("l'organisation attendue ne peut être null");
			}
			if (dureeTimeout < 10) {
				throw new IllegalArgumentException("la durée du timeout doit être supérieure à 10");
			}
			this.dureeTimeout = dureeTimeout;
			this.organisationAttendu = organisationAttendu;
		}
		
		@Override
		public void onOrganisationTraite(long noIndividu) {
			if (noIndividu == organisationAttendu) {
				synchronized (this) {
					traitementOk = true;
					notifyAll();
				}
			}
		}

		public synchronized boolean donneUneChanceAuTraitementDeSeTerminer() {
			long timeout = System.currentTimeMillis() + dureeTimeout;
			while (!traitementOk && System.currentTimeMillis() < timeout){
				try {
					wait(dureeTimeout / 10);
				}
				catch (InterruptedException e) {
					LOGGER.warn(e.getMessage(),e );
					break;
				}
			}
			return traitementOk;
		}

		@Override
		public void onStop() {}
	}


	/**
	 * Construit l'exception à lancer dans le cas ou on ne trouve pas un evenement par son id
	 * @param id de l'evt non trouvé
	 * @return l'exception construite
	 */
	private ObjectNotFoundException newObjectNotFoundException(Long id) {
		return new ObjectNotFoundException (
				messageSource.getMessage("error.id.evenement.inexistant", new Object[]{id.toString()},
				                         WebContextUtils.getDefaultLocale()
				));
	}
	protected AdresseEnvoi retrieveAdresse(Long numeroOrganisation) throws AdresseException {
		final Organisation organisation = serviceOrganisationService.getOrganisationHistory(numeroOrganisation);
		//return adresseService.getAdresseEnvoi(organisation, RegDate.get(), false);
		return null;
	}

	protected ServiceOrganisationEvent retrieveOrganisation(Long noEvenement, Long numeroOrganisation) {
		final ServiceOrganisationEvent organisationEvent = serviceOrganisationService.getOrganisationEvent(noEvenement).get(numeroOrganisation);
		if (organisationEvent == null) {
			throw new ObjectNotFoundException(this.messageSource.getMessage("error.organisation.introuvable", new Object[] {Long.toString(numeroOrganisation)},  WebContextUtils.getDefaultLocale()));
		}
		return organisationEvent;
	}
}