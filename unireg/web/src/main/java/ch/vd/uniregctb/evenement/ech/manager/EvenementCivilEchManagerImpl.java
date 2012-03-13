package ch.vd.uniregctb.evenement.ech.manager;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.uniregctb.adresse.AdresseException;
import ch.vd.uniregctb.common.ParamPagination;
import ch.vd.uniregctb.evenement.civil.EvenementCivilCriteria;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEch;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEchBasicInfo;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEchDAO;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEchErreur;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEchService;
import ch.vd.uniregctb.evenement.civil.engine.ech.EvenementCivilNotificationQueue;
import ch.vd.uniregctb.evenement.common.manager.EvenementCivilManagerImpl;
import ch.vd.uniregctb.evenement.common.view.ErreurEvenementCivilView;
import ch.vd.uniregctb.evenement.ech.view.EvenementCivilEchCriteriaView;
import ch.vd.uniregctb.evenement.ech.view.EvenementCivilEchDetailView;
import ch.vd.uniregctb.evenement.ech.view.EvenementCivilEchElementListeRechercheView;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureException;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.IndividuNotFoundException;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.EtatEvenementCivil;


public class EvenementCivilEchManagerImpl extends EvenementCivilManagerImpl implements EvenementCivilEchManager {

	private final Logger LOGGER = Logger.getLogger(EvenementCivilEchManagerImpl.class);

	private EvenementCivilEchDAO evenementDAO;
	private EvenementCivilEchService evenementService;
	private EvenementCivilNotificationQueue evenementNotificationQueue;

	@SuppressWarnings("unused")
	public void setEvenementDAO(EvenementCivilEchDAO evenementDAO) {
		this.evenementDAO = evenementDAO;
	}

	@SuppressWarnings("unused")
	public void setEvenementService(EvenementCivilEchService evenementService) {
		this.evenementService = evenementService;
	}

	@SuppressWarnings("unused")
	public void setEvenementNotificationQueue(EvenementCivilNotificationQueue evtCivilNotificationQueue) {
		this.evenementNotificationQueue = evtCivilNotificationQueue;
	}

	@Override
	@Transactional(readOnly = true)
	public EvenementCivilEchDetailView get(Long id) throws AdresseException, ServiceInfrastructureException {
		final EvenementCivilEchDetailView evtView = new EvenementCivilEchDetailView();
		final EvenementCivilEch evt = evenementDAO.get(id);
		if (evt == null) {
			throw newObjectNotFoundException(id);
		}
		fill(evt, evtView);
		final Long numeroIndividu = evt.getNumeroIndividu();
		evtView.setIndividu(retrieveIndividu(numeroIndividu));
		evtView.setAdresse(retrieveAdresse(numeroIndividu));
		retrieveTiersAssociePrincipal(evt.getId(), numeroIndividu, evtView);
		retrieveTiersAssocieMenage(evt.getId(), numeroIndividu, evtView);
		retrieveEvenementAssocie(numeroIndividu, evtView);
		return evtView;
	}

	@Override
	@Transactional (rollbackFor = Throwable.class)
	public void recycleEvenementCivil(Long id) {
		EvenementCivilEch evt = evenementDAO.get(id);
		if (evt==null) {
			throw newObjectNotFoundException(id);
		}
		List<EvenementCivilEchBasicInfo> list = evenementService.buildLotEvenementsCivils(evt.getNumeroIndividu());
		if (list == null || list.isEmpty()) {
			throw new IllegalStateException("la liste devrait toujours avoir au moins un element");
		}
		if (list.get(0).getId() == id) {
			// L'evenement est recyclable
			evenementNotificationQueue.post(evt.getNumeroIndividu());
		} else {
			LOGGER.warn(String.format("Tentative incoherente de recyclage de l'evenement (%d), ne devrait pas se produire lors de l'utilisation normale de l'application", id));
		}
	}

	@Override
	@Transactional (rollbackFor = Throwable.class)
	public void forceEtatTraite(Long id) {
		EvenementCivilEch evt = evenementDAO.get(id);
		if (evt==null) {
			throw newObjectNotFoundException(id);
		}
		if (!evt.getEtat().isTraite() || evt.getEtat() == EtatEvenementCivil.A_VERIFIER) {
			evt.setEtat(EtatEvenementCivil.FORCE);
		}
	}

	@Override
	@Transactional(readOnly = true)
	public List<EvenementCivilEchElementListeRechercheView> find(EvenementCivilEchCriteriaView bean, ParamPagination pagination) throws AdresseException {
		final List<EvenementCivilEchElementListeRechercheView> evtsElementListeRechercheView = new ArrayList<EvenementCivilEchElementListeRechercheView>();
		final List<EvenementCivilEch> evts = evenementDAO.find(bean, pagination);
		for (EvenementCivilEch evt : evts) {
			final EvenementCivilEchElementListeRechercheView evtElementListeRechercheView = buildView(evt);
			evtsElementListeRechercheView.add(evtElementListeRechercheView);
		}
		return evtsElementListeRechercheView;
	}

	@Override
	@Transactional(readOnly = true)
	public int count(EvenementCivilCriteria criterion) {
		return evenementDAO.count(criterion);
	}

	private EvenementCivilEchElementListeRechercheView buildView(EvenementCivilEch evt) throws AdresseException {
		final EvenementCivilEchElementListeRechercheView eltListe = new EvenementCivilEchElementListeRechercheView(evt);
		final PersonnePhysique personnePhysique = tiersService.getPersonnePhysiqueByNumeroIndividu(evt.getNumeroIndividu());
		try {
			if (personnePhysique != null) {
				final EnsembleTiersCouple couple = tiersService.getEnsembleTiersCouple(personnePhysique, null);
				if (couple != null && couple.getMenage() != null) {
					eltListe.setNumeroCTB(couple.getMenage().getNumero());
				}
				else {
					eltListe.setNumeroCTB(personnePhysique.getNumero());
				}
			}
			if (evt.getNumeroIndividu() != null) {
				String nom = adresseService.getNomCourrier(evt.getNumeroIndividu());
				eltListe.setNom(nom);
			}
		}
		catch (IndividuNotFoundException e) {
			LOGGER.warn("Impossible d'afficher toutes les données de l'événement civil n°" + evt.getId(), e);
			eltListe.setNom("<erreur: individu introuvable>");
		}
		return eltListe;
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

}