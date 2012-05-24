package ch.vd.uniregctb.evenement.ech.manager;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.unireg.interfaces.infra.ServiceInfrastructureException;
import ch.vd.uniregctb.adresse.AdresseException;
import ch.vd.uniregctb.common.ParamPagination;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEch;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEchBasicInfo;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEchErreur;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEchService;
import ch.vd.uniregctb.evenement.civil.engine.ech.EvenementCivilEchProcessor;
import ch.vd.uniregctb.evenement.civil.engine.ech.EvenementCivilNotificationQueue;
import ch.vd.uniregctb.evenement.common.manager.EvenementCivilManagerImpl;
import ch.vd.uniregctb.evenement.common.view.ErreurEvenementCivilView;
import ch.vd.uniregctb.evenement.ech.view.EvenementCivilEchCriteriaView;
import ch.vd.uniregctb.evenement.ech.view.EvenementCivilEchDetailView;
import ch.vd.uniregctb.evenement.ech.view.EvenementCivilEchElementListeRechercheView;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.IndividuNotFoundException;
import ch.vd.uniregctb.tiers.PersonnePhysique;

public class EvenementCivilEchManagerImpl extends EvenementCivilManagerImpl implements EvenementCivilEchManager {

	private static final Logger LOGGER = Logger.getLogger(EvenementCivilEchManagerImpl.class);

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
		final EvenementCivilEchDetailView evtView = new EvenementCivilEchDetailView();
		final EvenementCivilEch evt = evenementService.get(id);
		if (evt == null) {
			throw newObjectNotFoundException(id);
		}
		fill(evt, evtView);
		final Long numeroIndividu = evt.getNumeroIndividu();
		evtView.setNoIndividu(numeroIndividu);
		if (numeroIndividu != null) {
			try { // [SIFISC-4834] on permet la visualisation de l'événement même si les données de l'individu sont invalides
				evtView.setIndividu(retrieveIndividu(numeroIndividu));
				evtView.setAdresse(retrieveAdresse(numeroIndividu));
				retrieveTiersAssociePrincipal(evt.getId(), numeroIndividu, evtView);
				retrieveTiersAssocieMenage(evt.getId(), numeroIndividu, evtView);
				retrieveEvenementAssocie(numeroIndividu, evtView);
			}
			catch (Exception e) {
				evtView.setIndividuError(e.getMessage());
			}
		} else {
            if (!evt.isAnnule() && !evt.getEtat().isTraite()) {
                evtView.setRecyclable(true);
            }
        }
		return evtView;
	}


	@Override
	public boolean recycleEvenementCivil(Long id) throws EvenementCivilException {
		EvenementCivilEch evt = evenementService.get(id);
		if (evt==null) {
			throw newObjectNotFoundException(id);
		}
        if (evt.getNumeroIndividu() == null) {
            return recycleEvenementCivilSansNumeroIndividu(evt);
        } else {
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
        List<EvenementCivilEchBasicInfo> list = evenementService.buildLotEvenementsCivils(evt.getNumeroIndividu());
        if (list == null || list.isEmpty()) {
            throw new RuntimeException("La liste devrait toujours avoir au moins un élément");
        }
        if (list.get(0).getId() == evt.getId()) {
            // L'evenement est recyclable
            final EvenementCivilEchProcessorListener processorListener = new EvenementCivilEchProcessorListener(evt.getNumeroIndividu(), TIMEOUT_RECYCLAGE);
            final EvenementCivilEchProcessor.ListenerHandle listnerHandle =  evenementProcessor.registerListener(processorListener);
            try {
                evenementNotificationQueue.post(evt.getNumeroIndividu(), true);
                individuRecycle = processorListener.donneUneChanceAuTraitementDeSeTerminer();
            } finally {
                evenementProcessor.unregisterListener(listnerHandle);
            }
        } else {
            LOGGER.warn(String.format("Tentative incohérente de recyclage de l'événement (%d), ne devrait pas se produire lors de l'utilisation normale de l'application", evt.getId()));
        }
        return individuRecycle;
    }

    @Override
	public void forceEvenement(Long id) {
        evenementService.forceEvenement(id);
	}

	@Override
    @Transactional (readOnly = true)
	public List<EvenementCivilEchElementListeRechercheView> find(EvenementCivilEchCriteriaView bean, ParamPagination pagination) throws AdresseException {
		final List<EvenementCivilEchElementListeRechercheView> evtsElementListeRechercheView = new ArrayList<EvenementCivilEchElementListeRechercheView>();
		if (bean.isModeLotEvenement()) {
			// cas spécial, on veut la liste des evenements en attente pour un individu
			List<EvenementCivilEchBasicInfo> list = evenementService.buildLotEvenementsCivils(bean.getNumeroIndividu());
			for (int i = (pagination.getNumeroPage() - 1) * pagination.getTaillePage();
			     i < list.size() && i < (pagination.getNumeroPage()) * pagination.getTaillePage(); ++i) {
				EvenementCivilEchBasicInfo evt = list.get(i);
				final EvenementCivilEchElementListeRechercheView evtElementListeRechercheView = buildView(evt);
				evtsElementListeRechercheView.add(evtElementListeRechercheView);
			}
		} else {
			final List<EvenementCivilEch> evts = evenementService.find(bean, pagination);
			for (EvenementCivilEch evt : evts) {
				final EvenementCivilEchElementListeRechercheView evtElementListeRechercheView = buildView(evt);
				evtsElementListeRechercheView.add(evtElementListeRechercheView);
			}
		}
		return evtsElementListeRechercheView;
	}

	@Override
	public int count(EvenementCivilEchCriteriaView bean) {
		if (bean.isModeLotEvenement()) {
			return evenementService.buildLotEvenementsCivils(bean.getNumeroIndividu()).size();
		} else {
			return evenementService.count(bean);
		}
	}

	private EvenementCivilEchElementListeRechercheView buildView(EvenementCivilEchBasicInfo evt) throws AdresseException {
		return buildView(evenementService.get(evt.getId()));
	}

	private EvenementCivilEchElementListeRechercheView buildView(EvenementCivilEch evt) throws AdresseException {
		final EvenementCivilEchElementListeRechercheView view = new EvenementCivilEchElementListeRechercheView(evt);
		if (evt.getNumeroIndividu() != null) {
			final long numeroIndividu = evt.getNumeroIndividu();
			final PersonnePhysique personnePhysique = tiersService.getPersonnePhysiqueByNumeroIndividu(numeroIndividu);
			try {
				if (personnePhysique != null) {
					final EnsembleTiersCouple couple = tiersService.getEnsembleTiersCouple(personnePhysique, null);
					if (couple != null && couple.getMenage() != null) {
						view.setNumeroCTB(couple.getMenage().getNumero());
					}
					else {
						view.setNumeroCTB(personnePhysique.getNumero());
					}
				}

				final String nom = adresseService.getNomCourrier(numeroIndividu);
				view.setNom(nom);
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

	private void retrieveEvenementAssocie(Long numeroIndividu, EvenementCivilEchDetailView evtView) {
		List<EvenementCivilEchBasicInfo> list = evenementService.buildLotEvenementsCivils(numeroIndividu);
		if (list!=null && list.size() > 0) {
			EvenementCivilEchBasicInfo evtPrioritaire = list.get(0);
			evtView.setEvtPrioritaire(evtPrioritaire);
			evtView.setTotalAutresEvenementsAssocies(list.size() - 1);
			if (evtView.getEvtId() == evtPrioritaire.getId()) {
				evtView.setRecyclable(true);
			}
		}
	}

	private void fill(EvenementCivilEch source, EvenementCivilEchDetailView target) {
		target.setEvtAction(source.getAction());
		target.setEvtCommentaireTraitement(source.getCommentaireTraitement());
		target.setEvtDate(source.getDateEvenement());
		target.setEvtDateTraitement(source.getDateTraitement());
		target.setEvtEtat(source.getEtat());
		target.setEvtId(source.getId());
		target.setEvtType(source.getType());
		for (EvenementCivilEchErreur err : source.getErreurs() ) {
			target.addEvtErreur(new ErreurEvenementCivilView(err.getMessage(), err.getCallstack()));
		}
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
				throw new IllegalArgumentException("la durée du timeout doit être supérieur à 10");
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