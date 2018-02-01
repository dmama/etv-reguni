package ch.vd.unireg.evenement.ech.manager;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.unireg.interfaces.infra.ServiceInfrastructureException;
import ch.vd.unireg.adresse.AdresseException;
import ch.vd.unireg.common.pagination.ParamPagination;
import ch.vd.unireg.evenement.civil.common.EvenementCivilException;
import ch.vd.unireg.evenement.civil.ech.EvenementCivilEch;
import ch.vd.unireg.evenement.civil.ech.EvenementCivilEchBasicInfo;
import ch.vd.unireg.evenement.civil.ech.EvenementCivilEchErreur;
import ch.vd.unireg.evenement.civil.ech.EvenementCivilEchProcessingMode;
import ch.vd.unireg.evenement.civil.ech.EvenementCivilEchService;
import ch.vd.unireg.evenement.civil.engine.ech.EvenementCivilEchProcessor;
import ch.vd.unireg.evenement.civil.engine.ech.EvenementCivilNotificationQueue;
import ch.vd.unireg.evenement.common.manager.EvenementCivilManagerImpl;
import ch.vd.unireg.evenement.common.view.ErreurEvenementCivilView;
import ch.vd.unireg.evenement.ech.view.EvenementCivilEchCriteriaView;
import ch.vd.unireg.evenement.ech.view.EvenementCivilEchDetailView;
import ch.vd.unireg.evenement.ech.view.EvenementCivilEchElementListeRechercheView;
import ch.vd.unireg.evenement.ech.view.EvenementCivilEchGrappeView;
import ch.vd.unireg.tiers.EnsembleTiersCouple;
import ch.vd.unireg.tiers.IndividuNotFoundException;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.PlusieursPersonnesPhysiquesAvecMemeNumeroIndividuException;

public class EvenementCivilEchManagerImpl extends EvenementCivilManagerImpl implements EvenementCivilEchManager {

	private static final Logger LOGGER = LoggerFactory.getLogger(EvenementCivilEchManagerImpl.class);

	private static final long TIMEOUT_RECYCLAGE = 5000; // ms

	private EvenementCivilEchService evenementService;
	private EvenementCivilNotificationQueue evenementNotificationQueue;
	private EvenementCivilEchProcessor evenementProcessor;

	@SuppressWarnings("unused")
	public void setEvenementService(EvenementCivilEchService evenementService) {
		this.evenementService = evenementService;
	}

	@SuppressWarnings("unused")
	public void setEvenementNotificationQueue(EvenementCivilNotificationQueue evtCivilNotificationQueue) {
		this.evenementNotificationQueue = evtCivilNotificationQueue;
	}

	@SuppressWarnings("unused")
	public void setEvenementProcessor(EvenementCivilEchProcessor evenementProcessor) {
		this.evenementProcessor = evenementProcessor;
	}

	@Override
    @Transactional(readOnly = true)
	public EvenementCivilEchDetailView get(Long id) throws AdresseException, ServiceInfrastructureException {
		final EvenementCivilEch evt = evenementService.get(id);
		if (evt == null) {
			throw newObjectNotFoundException(id);
		}
		return buildDetailView(evt);
	}

	private EvenementCivilEchDetailView buildDetailView(EvenementCivilEch evt) {
		final EvenementCivilEchDetailView evtView = new EvenementCivilEchDetailView();

		evtView.setEvtAction(evt.getAction());
		evtView.setEvtCommentaireTraitement(evt.getCommentaireTraitement());
		evtView.setEvtDate(evt.getDateEvenement());
		evtView.setEvtDateTraitement(evt.getDateTraitement());
		evtView.setEvtEtat(evt.getEtat());
		evtView.setEvtId(evt.getId());
		evtView.setEvtType(evt.getType());
		if (evt.getRefMessageId() != null) {
			// l'événement référencé peut exister dans la base... ou pas (cas des corrections des événements créés pendant la migration RCPers, par exemple)
			final EvenementCivilEch ref = evenementService.get(evt.getRefMessageId());
			if (ref != null) {
				evtView.setRefEvtId(ref.getId());
			}
		}
		for (EvenementCivilEchErreur err : evt.getErreurs() ) {
			evtView.addEvtErreur(new ErreurEvenementCivilView(err.getMessage(), err.getCallstack()));
		}

		// récupération de la grappe
		try {
			evtView.setGrappeComplete(buildGrappeView(evt));
		}
		catch (EvenementCivilException e) {
			evtView.setIndividuError(e.getMessage());
		}

		final Long numeroIndividu = evt.getNumeroIndividu();
		evtView.setNoIndividu(numeroIndividu);
		if (numeroIndividu != null) {
			try { // [SIFISC-4834] on permet la visualisation de l'événement même si les données de l'individu sont invalides
				evtView.setIndividu(retrieveIndividu(numeroIndividu, evt.getId()));
				evtView.setAdresse(retrieveAdresse(numeroIndividu));
				retrieveTiersAssociePrincipal(evt.getId(), numeroIndividu, evtView);
				retrieveTiersAssocieMenage(evt.getId(), numeroIndividu, evtView);
			}
			catch (Exception e) {
				evtView.setIndividuError(e.getMessage());
			}

			try {
				final List<EvenementCivilEchBasicInfo> list = evenementService.buildLotEvenementsCivilsNonTraites(numeroIndividu);
				evtView.setNonTraitesSurMemeIndividu(list);
				if (list != null && list.size() > 0) {
					final EvenementCivilEchBasicInfo evtPrioritaire = list.get(0);
					if (evtView.getEvtId() == evtPrioritaire.getId()) {
						evtView.setRecyclable(true);
					}
				}
			}
			catch (Exception e) {
				evtView.setIndividuError(e.getMessage());
			}
		}
		else if (!evt.isAnnule() && !evt.getEtat().isTraite()) {
            evtView.setRecyclable(true);
        }

		return evtView;
	}

	private EvenementCivilEchGrappeView buildGrappeView(EvenementCivilEch evt) throws EvenementCivilException {
		try {
			final List<EvenementCivilEchBasicInfo> list = evenementService.buildGrappe(evt);
			return new EvenementCivilEchGrappeView(list);
		}
		catch (EvenementCivilException e) {
			throw e;
		}
		catch (Exception e) {
			throw new EvenementCivilException(e);
		}
	}

	@Override
	@Transactional
	public boolean recycleEvenementCivil(Long id) throws EvenementCivilException {
		EvenementCivilEch evt = evenementService.get(id);
		if (evt == null) {
			throw newObjectNotFoundException(id);
		}
        if (evt.getNumeroIndividu() == null) {
            return recycleEvenementCivilSansNumeroIndividu(evt);
        }
        else {
            return recycleEvenementCivil(evt);
        }
	}

    private boolean recycleEvenementCivilSansNumeroIndividu(EvenementCivilEch evt) throws EvenementCivilException {
        if (evt.getNumeroIndividu() != null) {
            throw new IllegalArgumentException("l'événement " + evt.getId() + " doit référencer un événement sans individu. Or l'individu " + evt.getNumeroIndividu() + " y est associé");
        }
        // récupération de l'individu
        final long noIndividu = evenementService.getNumeroIndividuPourEvent(evt);

        // sauvegarde de l'individu dans l'événement
        evt = evenementService.assigneNumeroIndividu(evt, noIndividu);

        return recycleEvenementCivil(evt);
    }

    private boolean recycleEvenementCivil(EvenementCivilEch evt) {
        boolean individuRecycle = false;
        final List<EvenementCivilEchBasicInfo> list = evenementService.buildLotEvenementsCivilsNonTraites(evt.getNumeroIndividu());
        if (list == null || list.isEmpty()) {
            throw new RuntimeException("La liste devrait toujours avoir au moins un élément");
        }
        if (list.get(0).getId() == evt.getId()) {
            // L'evenement est recyclable
            final EvenementCivilEchProcessorListener processorListener = new EvenementCivilEchProcessorListener(evt.getNumeroIndividu(), TIMEOUT_RECYCLAGE);
            final EvenementCivilEchProcessor.ListenerHandle handle =  evenementProcessor.registerListener(processorListener);
            try {
                evenementNotificationQueue.post(evt.getNumeroIndividu(), EvenementCivilEchProcessingMode.IMMEDIATE);
                individuRecycle = processorListener.donneUneChanceAuTraitementDeSeTerminer();
            }
            finally {
                handle.unregister();
            }
        }
        else {
            LOGGER.warn(String.format("Tentative incohérente de recyclage de l'événement (%d), ne devrait pas se produire lors de l'utilisation normale de l'application", evt.getId()));
        }
        return individuRecycle;
    }

    @Override
    @Transactional
	public void forceEvenement(Long id) {
        evenementService.forceEvenement(id);
	}

	@Override
    @Transactional (readOnly = true)
	public List<EvenementCivilEchElementListeRechercheView> find(EvenementCivilEchCriteriaView bean, ParamPagination pagination) throws AdresseException {
		final List<EvenementCivilEchElementListeRechercheView> evtsElementListeRechercheView = new ArrayList<>();
		if (bean.isModeLotEvenement()) {
			// cas spécial, on veut la liste des evenements en attente pour un individu
			List<EvenementCivilEchBasicInfo> list = evenementService.buildLotEvenementsCivilsNonTraites(bean.getNumeroIndividu());
			for (int i = (pagination.getNumeroPage() - 1) * pagination.getTaillePage();
			     i < list.size() && i < (pagination.getNumeroPage()) * pagination.getTaillePage(); ++i) {
				EvenementCivilEchBasicInfo evt = list.get(i);
				final EvenementCivilEchElementListeRechercheView evtElementListeRechercheView = buildElementRechercheView(evt);
				evtsElementListeRechercheView.add(evtElementListeRechercheView);
			}
		}
		else {
			final List<EvenementCivilEch> evts = evenementService.find(bean, pagination);
			for (EvenementCivilEch evt : evts) {
				final EvenementCivilEchElementListeRechercheView evtElementListeRechercheView = buildElementRechercheView(evt);
				evtsElementListeRechercheView.add(evtElementListeRechercheView);
			}
		}
		return evtsElementListeRechercheView;
	}

	@Override
	@Transactional(readOnly = true)
	public int count(EvenementCivilEchCriteriaView bean) {
		if (bean.isModeLotEvenement()) {
			return evenementService.buildLotEvenementsCivilsNonTraites(bean.getNumeroIndividu()).size();
		} else {
			return evenementService.count(bean);
		}
	}

	private EvenementCivilEchElementListeRechercheView buildElementRechercheView(EvenementCivilEchBasicInfo evt) throws AdresseException {
		return buildElementRechercheView(evenementService.get(evt.getId()));
	}

	private EvenementCivilEchElementListeRechercheView buildElementRechercheView(EvenementCivilEch evt) throws AdresseException {
		final EvenementCivilEchElementListeRechercheView view = new EvenementCivilEchElementListeRechercheView(evt);
		if (evt.getNumeroIndividu() != null) {
			final long numeroIndividu = evt.getNumeroIndividu();
			try {
				view.setNumeroCTB(getNumeroCtbPourNoIndividu(numeroIndividu));

				final String nom = adresseService.getNomCourrier(numeroIndividu);
				view.setNom(nom);

				view.setIndividu(retrieveIndividu(numeroIndividu, evt.getId()));
				view.setAdresse(retrieveAdresse(numeroIndividu));
			}
			catch (IndividuNotFoundException e) {
				LOGGER.warn("Impossible d'afficher toutes les données de l'événement civil n°" + evt.getId(), e);
				view.setNom("<erreur: individu introuvable>");
			}
			catch (Exception e) {
				LOGGER.warn("Impossible d'afficher toutes les données de l'événement civil n°" + evt.getId(), e);
				view.setNom("<erreur: " + e.getMessage() + ">");
			}
		}
		return view;
	}

	private Long getNumeroCtbPourNoIndividu(long noIndividu) throws IndividuNotFoundException {
		Long noCtb = null;
		try {
			final PersonnePhysique personnePhysique = tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividu);
			if (personnePhysique != null) {
				final EnsembleTiersCouple couple = tiersService.getEnsembleTiersCouple(personnePhysique, null);
				if (couple != null && couple.getMenage() != null) {
					noCtb = couple.getMenage().getNumero();
				}
				else {
					noCtb = personnePhysique.getNumero();
				}
			}
		}
		catch (PlusieursPersonnesPhysiquesAvecMemeNumeroIndividuException e) {
			LOGGER.warn("Impossible de trouver le contribuable associé à l'individu " + noIndividu, e);
		}
		return noCtb;
	}

	private static class EvenementCivilEchProcessorListener implements EvenementCivilEchProcessor.Listener {

		private final long dureeTimeout;
		private final Long individuAttendu;
		private volatile boolean traitementOk = false;

		private EvenementCivilEchProcessorListener(Long individuAttendu, long dureeTimeout) {
			if (individuAttendu == null) {
				throw new NullPointerException("l'individu attendu ne peut être null");
			}
			if (dureeTimeout < 10) {
				throw new IllegalArgumentException("la durée du timeout doit être supérieure à 10");
			}
			this.dureeTimeout = dureeTimeout;
			this.individuAttendu = individuAttendu;
		}
		
		@Override
		public void onIndividuTraite(long noIndividu) {
			if (noIndividu == individuAttendu) {
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
}