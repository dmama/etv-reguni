package ch.vd.unireg.evenement.entreprise.manager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.HibernateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.adresse.AdresseEnvoi;
import ch.vd.unireg.adresse.AdresseException;
import ch.vd.unireg.adresse.AdresseGenerique;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.adresse.TypeAdresseFiscale;
import ch.vd.unireg.common.ObjectNotFoundException;
import ch.vd.unireg.common.pagination.ParamPagination;
import ch.vd.unireg.evenement.common.view.TiersAssocieView;
import ch.vd.unireg.evenement.entreprise.EvenementEntreprise;
import ch.vd.unireg.evenement.entreprise.EvenementEntrepriseBasicInfo;
import ch.vd.unireg.evenement.entreprise.EvenementEntrepriseErreur;
import ch.vd.unireg.evenement.entreprise.EvenementEntrepriseException;
import ch.vd.unireg.evenement.entreprise.EvenementEntrepriseProcessingMode;
import ch.vd.unireg.evenement.entreprise.EvenementEntrepriseService;
import ch.vd.unireg.evenement.entreprise.engine.EvenementEntrepriseNotificationQueue;
import ch.vd.unireg.evenement.entreprise.engine.processor.EvenementEntrepriseProcessor;
import ch.vd.unireg.evenement.entreprise.view.ErreurEvenementEntrepriseView;
import ch.vd.unireg.evenement.entreprise.view.EvenementEntrepriseCriteriaView;
import ch.vd.unireg.evenement.entreprise.view.EvenementEntrepriseDetailView;
import ch.vd.unireg.evenement.entreprise.view.EvenementEntrepriseElementListeRechercheView;
import ch.vd.unireg.evenement.entreprise.view.EvenementEntrepriseSummaryView;
import ch.vd.unireg.interfaces.entreprise.ServiceEntrepriseException;
import ch.vd.unireg.interfaces.entreprise.data.EntrepriseCivile;
import ch.vd.unireg.interfaces.entreprise.data.EntrepriseCivileEvent;
import ch.vd.unireg.interfaces.infra.InfrastructureException;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.interfaces.infra.data.Pays;
import ch.vd.unireg.interfaces.service.ServiceEntreprise;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.message.MessageHelper;
import ch.vd.unireg.organisation.EntrepriseView;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.EntrepriseNotFoundException;
import ch.vd.unireg.tiers.ForFiscalPrincipal;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.tiers.TiersException;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.type.EtatEvenementEntreprise;
import ch.vd.unireg.utils.UniregProperties;

public class EvenementEntrepriseManagerImpl implements EvenementEntrepriseManager {

	private static final Logger LOGGER = LoggerFactory.getLogger(EvenementEntrepriseManagerImpl.class);

	private static final long TIMEOUT_RECYCLAGE = 5000; // ms

	private AdresseService adresseService;
	private TiersService tiersService;
	private ServiceEntreprise serviceEntreprise;
	private ServiceInfrastructureService serviceInfrastructureService;
	private MessageHelper messageHelper;
	private UniregProperties properties;

	private EvenementEntrepriseService evenementService;
	private EvenementEntrepriseNotificationQueue evenementNotificationQueue;
	private EvenementEntrepriseProcessor evenementProcessor;

	@SuppressWarnings("unused")
	public void setEvenementService(EvenementEntrepriseService evenementService) {
		this.evenementService = evenementService;
	}

	@SuppressWarnings("unused")
	public void setEvenementNotificationQueue(EvenementEntrepriseNotificationQueue queue) {
		this.evenementNotificationQueue = queue;
	}

	@SuppressWarnings("unused")
	public void setEvenementProcessor(EvenementEntrepriseProcessor evenementProcessor) {
		this.evenementProcessor = evenementProcessor;
	}

	public void setAdresseService(AdresseService adresseService) {
		this.adresseService = adresseService;
	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	public void setServiceEntreprise(ServiceEntreprise serviceEntreprise) {
		this.serviceEntreprise = serviceEntreprise;
	}

	public void setServiceInfrastructureService(ServiceInfrastructureService serviceInfrastructureService) {
		this.serviceInfrastructureService = serviceInfrastructureService;
	}

	public void setMessageHelper(MessageHelper messageSource) {
		this.messageHelper = messageSource;
	}

	public void setProperties(UniregProperties properties) {
		this.properties = properties;
	}

	@Override
    @Transactional(readOnly = true)
	public EvenementEntrepriseDetailView get(Long id) throws AdresseException, InfrastructureException {
		final EvenementEntreprise evt = evenementService.get(id);
		if (evt == null) {
			throw newObjectNotFoundException(id);
		}
		return buildDetailView(evt);
	}

	@Override
	@Transactional(readOnly = true)
	public EvenementEntrepriseSummaryView getSummary(Long id) throws AdresseException, InfrastructureException {
		final EvenementEntreprise evt = evenementService.get(id);
		if (evt == null) {
			throw newObjectNotFoundException(id);
		}
		return buildSummaryView(evt);
	}

	private EvenementEntrepriseDetailView buildDetailView(EvenementEntreprise evt) {

		final EvenementEntrepriseDetailView evtView = new EvenementEntrepriseDetailView();

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

		for (EvenementEntrepriseErreur err : evt.getErreurs() ) {
			evtView.addEvtErreur(new ErreurEvenementEntrepriseView(err.getId(), err.getMessage(), err.getCallstack()));
		}
		evtView.getEvtErreurs().sort(Comparator.comparingLong(ErreurEvenementEntrepriseView::getErrorId));

		final Long noEntrepriseCivile = evt.getNoEntrepriseCivile();
		evtView.setNoOrganisation(noEntrepriseCivile);
		try {
			final EntrepriseCivileEvent entrepriseEvent = retrieveEntrepriseEvent(evt.getNoEvenement(), noEntrepriseCivile);

			evtView.setFoscNumero(entrepriseEvent.getNumeroDocumentFOSC());
			evtView.setFoscDate(entrepriseEvent.getDatePublicationFOSC());
			if (entrepriseEvent.getNumeroDocumentFOSC() != null) {
				evtView.setFoscLienDirect(String.format(properties.getProperty(evt.getCleLienPublicationFosc()), entrepriseEvent.getNumeroDocumentFOSC()));
			}

			evtView.setOrganisation(new EntrepriseView(entrepriseEvent.getPseudoHistory(), evt.getDateEvenement()));
			evtView.setAdresse(retrieveAdresse(noEntrepriseCivile));
		}
		catch (Exception e) {
			evtView.setOrganisationError(e.getMessage());
		}

		try {
			retrieveTiersAssocie(evt.getNoEvenement(), noEntrepriseCivile, evtView);
		}
		catch (Exception e) {
			final String previousError = evtView.getOrganisationError();
			evtView.setOrganisationError(StringUtils.isBlank(previousError) ? e.getMessage() : String.format("%s; %s", previousError, e.getMessage()));
		}

		try {
			final List<EvenementEntrepriseBasicInfo> list = evenementService.buildLotEvenementsEntrepriseNonTraites(noEntrepriseCivile);
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

	private EvenementEntrepriseSummaryView buildSummaryView(EvenementEntreprise evt) {
		final EvenementEntrepriseSummaryView evtView = new EvenementEntrepriseSummaryView();

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
		for (EvenementEntrepriseErreur err : evt.getErreurs() ) {
			evtView.addEvtErreur(new ErreurEvenementEntrepriseView(err.getId(), err.getMessage(), err.getCallstack()));
		}
		evtView.getEvtErreurs().sort(Comparator.comparingLong(ErreurEvenementEntrepriseView::getErrorId));

		evtView.setNoOrganisation(evt.getNoEntrepriseCivile());

		try {
			final List<EvenementEntrepriseBasicInfo> list = evenementService.buildLotEvenementsEntrepriseNonTraites(evt.getNoEntrepriseCivile());
			evtView.setNonTraitesSurMemeOrganisation(list);
			evtView.setRecyclable(evaluateRecyclable(evtView.getEvtId(), list));
		}
		catch (Exception e) {
			evtView.setErreursEvt(e.getMessage());
		}
		evtView.setForcable(evaluateForcable(evt.getEtat(), evtView.isRecyclable()));
		return evtView;
	}

	private boolean evaluateRecyclable(Long evtId, List<EvenementEntrepriseBasicInfo> list) {
		if (list != null && list.size() > 0) {
			final EvenementEntrepriseBasicInfo evtPrioritaire = list.get(0);
			if (evtId == evtPrioritaire.getId()) {
				return true;
			}
		}
		return false;
	}

	private boolean evaluateForcable(EtatEvenementEntreprise etat, boolean recyclable) {
		return etat == EtatEvenementEntreprise.A_VERIFIER || recyclable;
	}


	@Override
	@Transactional
	public boolean recycleEvenementEntreprise(Long id) throws EvenementEntrepriseException {
		EvenementEntreprise evt = evenementService.get(id);
		if (evt == null) {
			throw newObjectNotFoundException(id);
		}
        else {
            return recycleEvenementEntreprise(evt);
        }
	}

    private boolean recycleEvenementEntreprise(EvenementEntreprise evt) {
        boolean recycle = false;
        final List<EvenementEntrepriseBasicInfo> list = evenementService.buildLotEvenementsEntrepriseNonTraites(evt.getNoEntrepriseCivile());
        if (list == null || list.isEmpty()) {
            throw new RuntimeException("La liste devrait toujours avoir au moins un élément");
        }
        if (list.get(0).getId() == evt.getId()) {
            // L'evenement est recyclable
            final EvenementEntrepriseProcessorListener processorListener = new EvenementEntrepriseProcessorListener(evt.getNoEntrepriseCivile(), TIMEOUT_RECYCLAGE);
            final EvenementEntrepriseProcessor.ListenerHandle handle =  evenementProcessor.registerListener(processorListener);
            try {
                evenementNotificationQueue.post(evt.getNoEntrepriseCivile(), EvenementEntrepriseProcessingMode.IMMEDIATE);
                recycle = processorListener.donneUneChanceAuTraitementDeSeTerminer();
            }
            finally {
                handle.unregister();
            }
        }
        else {
            LOGGER.warn(String.format("Tentative incohérente de recyclage de l'événement (%d), ne devrait pas se produire lors de l'utilisation normale de l'application", evt.getId()));
        }
        return recycle;
    }

    @Override
    @Transactional
	public void forceEvenement(Long id) {
	    final EvenementEntreprise evt = evenementService.get(id);
	    if (evt == null) {
		    throw new ObjectNotFoundException("Evénement entreprise " + id);
	    }
	    evenementProcessor.forceEvenement(new EvenementEntrepriseBasicInfo(evt, evt.getNoEntrepriseCivile()));
	}

	@Override
    @Transactional (readOnly = true)
	public List<EvenementEntrepriseElementListeRechercheView> find(EvenementEntrepriseCriteriaView bean, ParamPagination pagination) throws AdresseException {
		final List<EvenementEntrepriseElementListeRechercheView> evtsElementListeRechercheView = new ArrayList<>();
		if (bean.isModeLotEvenement()) {
			// cas spécial, on veut la liste des evenements en attente pour une entreprise
			List<EvenementEntreprise> list = evenementService.getEvenementsNonTraitesEntreprise(bean.getNumeroEntrepriseCivile());
			for (int i = (pagination.getNumeroPage() - 1) * pagination.getTaillePage();
			     i < list.size() && i < (pagination.getNumeroPage()) * pagination.getTaillePage(); ++i) {
				EvenementEntreprise evt = list.get(i);
				final EvenementEntrepriseElementListeRechercheView evtElementListeRechercheView = buildElementRechercheView(evt);
				evtsElementListeRechercheView.add(evtElementListeRechercheView);
			}
		}
		else {
			final List<EvenementEntreprise> evts = evenementService.find(bean, pagination);
			for (EvenementEntreprise evt : evts) {
				final EvenementEntrepriseElementListeRechercheView evtElementListeRechercheView = buildElementRechercheView(evt);
				evtsElementListeRechercheView.add(evtElementListeRechercheView);
			}
		}
		return evtsElementListeRechercheView;
	}

	@Override
	@Transactional(readOnly = true)
	public int count(EvenementEntrepriseCriteriaView bean) {
		if (bean.isModeLotEvenement()) {
			return evenementService.buildLotEvenementsEntrepriseNonTraites(bean.getNumeroEntrepriseCivile()).size();
		} else {
			return evenementService.count(bean);
		}
	}

	/**
	 * Créée une nouvelle entreprise pour l'entreprise de l'événement dont l'id est passé en paramètre.
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
	public Entreprise creerEntreprisePourEvenementEntreprise(Long id) throws TiersException {
		EvenementEntreprise evt = evenementService.get(id);
		return tiersService.createEntreprisePourEvenement(evt);
	}

	private EvenementEntrepriseElementListeRechercheView buildElementRechercheView(EvenementEntreprise evt) throws AdresseException {
		final EvenementEntrepriseElementListeRechercheView view = new EvenementEntrepriseElementListeRechercheView(evt);
		final long numeroOrganisation = evt.getNoEntrepriseCivile();
		try {
			final RegDate dateEvenement = evt.getDateEvenement();

			view.setNumeroCTB(getNumeroCtbPourNoEntrepriseCivile(numeroOrganisation));

			final EntrepriseCivile entrepriseCivile = serviceEntreprise.getEntrepriseHistory(evt.getNoEntrepriseCivile());
			view.setOrganisation(new EntrepriseView(entrepriseCivile, evt.getDateEvenement()));
			view.setNom(view.getOrganisation().getNom()); // Champ redondant mais nécessaire car utilisé en cas d'erreur de chargement de l'entreprise civile.
			view.setCorrectionDansLePasse(evt.getCorrectionDansLePasse());
			if (evt.getReferenceAnnonceIDE() != null) {
				view.setAnnonceIDEId(evt.getReferenceAnnonceIDE().getId());
			}
			final List<EvenementEntrepriseBasicInfo> list = evenementService.buildLotEvenementsEntrepriseNonTraites(evt.getNoEntrepriseCivile());
			view.setRecyclable(evaluateRecyclable(evt.getId(), list));
			view.setForcable(evaluateForcable(evt.getEtat(), view.isRecyclable()));
		}
		catch (ServiceEntrepriseException e) {
			LOGGER.warn("Impossible d'afficher toutes les données de l'événement entreprise " + evt.toString(), e);
			view.setNom("<erreur: entreprise civile introuvable>");
		}
		catch (Exception e) {
			LOGGER.warn("Impossible d'afficher toutes les données de l'événement entreprise " + evt.toString(), e);
			view.setNom("<erreur: " + e.getMessage() + ">");
		}
		return view;
	}

	private Long getNumeroCtbPourNoEntrepriseCivile(long noEntrepriseCivile) throws EntrepriseNotFoundException {
		Long noCtb = null;
		try {
			final Entreprise entreprise = tiersService.getEntrepriseByNoEntrepriseCivile(noEntrepriseCivile);
			if (entreprise != null) {
				noCtb = entreprise.getNumero();
			}
		}
		catch (HibernateException e) {
			noCtb = -1L;
			LOGGER.warn("Impossible de trouver le contribuable associé à l'entreprise " + noEntrepriseCivile, e);
		}
		return noCtb;
	}

	private void retrieveTiersAssocie(Long noEvenement, Long numeroEntrepriseCivile, EvenementEntrepriseDetailView evtView) throws AdresseException {
		try {
			final Entreprise entreprise = tiersService.getEntrepriseByNoEntrepriseCivile(numeroEntrepriseCivile);
			if (entreprise != null) {
				TiersAssocieView tiersAssocie;
				try {
					tiersAssocie = createTiersAssocieView(entreprise);
				}
				catch (ServiceEntrepriseException e) {
					tiersAssocie = createTiersAssocieViewSansCivil(entreprise);
				}
				evtView.setTiersAssocie(tiersAssocie);
			}
		}
		catch (RuntimeException e) {
			LOGGER.warn(String.format("Détermination impossible des tiers associés à l'événement civil entreprise %d : %s", noEvenement, e.getMessage()));
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

	protected TiersAssocieView createTiersAssocieView(Entreprise entreprise) throws AdresseException, InfrastructureException {
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

	protected TiersAssocieView createTiersAssocieViewSansCivil(Entreprise entreprise) throws AdresseException, InfrastructureException {
		final TiersAssocieView tiersAssocie = new TiersAssocieView();
		tiersAssocie.setNumero(entreprise.getNumero());
		final String message = "<erreur: entreprise introuvable>";

		tiersAssocie.setNomCourrier(Collections.singletonList(message));
		tiersAssocie.setLocaliteOuPays("&lt;erreur: entreprise introuvable&gt;");
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


	private static class EvenementEntrepriseProcessorListener implements EvenementEntrepriseProcessor.Listener {

		private final long dureeTimeout;
		private final Long entrepriseAttendue;
		private volatile boolean traitementOk = false;

		private EvenementEntrepriseProcessorListener(Long entrepriseAttendue, long dureeTimeout) {
			if (entrepriseAttendue == null) {
				throw new NullPointerException("l'entreprise attendue ne peut être null");
			}
			if (dureeTimeout < 10) {
				throw new IllegalArgumentException("la durée du timeout doit être supérieure à 10");
			}
			this.dureeTimeout = dureeTimeout;
			this.entrepriseAttendue = entrepriseAttendue;
		}
		
		@Override
		public void onEntrepriseTraitee(long noEntrepriseCivile) {
			if (noEntrepriseCivile == entrepriseAttendue) {
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
				messageHelper.getMessage("error.id.evenement.inexistant", id.toString()));
	}
	protected AdresseEnvoi retrieveAdresse(Long numeroEntrepriseCivile) throws AdresseException {
		final EntrepriseCivile entrepriseCivile = serviceEntreprise.getEntrepriseHistory(numeroEntrepriseCivile);
		//return adresseService.getAdresseEnvoi(entrepriseCivile, RegDate.get(), false);
		return null;
	}

	protected EntrepriseCivileEvent retrieveEntrepriseEvent(Long noEvenement, Long numeroEntrepriseCivile) {
		final EntrepriseCivileEvent event = serviceEntreprise.getEntrepriseEvent(noEvenement).get(numeroEntrepriseCivile);
		if (event == null) {
			throw new ObjectNotFoundException(messageHelper.getMessage("error.organisation.introuvable", Long.toString(numeroEntrepriseCivile)));
		}
		return event;
	}
}