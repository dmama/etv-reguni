package ch.vd.uniregctb.evenement.regpp.manager;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.context.MessageSourceAware;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.unireg.interfaces.infra.ServiceInfrastructureException;
import ch.vd.uniregctb.adresse.AdresseException;
import ch.vd.uniregctb.common.ParamPagination;
import ch.vd.uniregctb.evenement.civil.EvenementCivilCriteria;
import ch.vd.uniregctb.evenement.civil.engine.regpp.EvenementCivilProcessor;
import ch.vd.uniregctb.evenement.civil.regpp.EvenementCivilRegPP;
import ch.vd.uniregctb.evenement.civil.regpp.EvenementCivilRegPPDAO;
import ch.vd.uniregctb.evenement.civil.regpp.EvenementCivilRegPPErreur;
import ch.vd.uniregctb.evenement.common.manager.EvenementCivilManagerImpl;
import ch.vd.uniregctb.evenement.common.view.ErreurEvenementCivilView;
import ch.vd.uniregctb.evenement.regpp.view.EvenementCivilRegPPCriteriaView;
import ch.vd.uniregctb.evenement.regpp.view.EvenementCivilRegPPDetailView;
import ch.vd.uniregctb.evenement.regpp.view.EvenementCivilRegPPElementListeView;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.IndividuNotFoundException;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.EtatEvenementCivil;

/**
 * @inheritDoc
 */
public class EvenementCivilRegPPManagerImpl extends EvenementCivilManagerImpl implements EvenementCivilRegPPManager, MessageSourceAware {

	private final Logger LOGGER = Logger.getLogger(EvenementCivilRegPPManagerImpl.class);

	private EvenementCivilProcessor evenementCivilProcessor;
	private EvenementCivilRegPPDAO evenementDAO;

	@SuppressWarnings("unused")
	public void setEvenementCivilProcessor(EvenementCivilProcessor evenementCivilProcessor) {
		this.evenementCivilProcessor = evenementCivilProcessor;
	}

	@SuppressWarnings("unused")
	public void setEvenementDAO(EvenementCivilRegPPDAO evenementDAO) {
		this.evenementDAO = evenementDAO;
	}

	/**
	 * @inheritDoc
	 */
	@Override
	@Transactional(readOnly = true)
	public EvenementCivilRegPPDetailView get(Long id) throws AdresseException, ServiceInfrastructureException {

		final EvenementCivilRegPPDetailView evtView = new EvenementCivilRegPPDetailView();

		final EvenementCivilRegPP evt = evenementDAO.get(id);
		if (evt == null) {
			throw newObjectNotFoundException(id);
		}

		final Long individuPrincipal = evt.getNumeroIndividuPrincipal();
		fill(evt, evtView);
		evtView.setIndividuPrincipal(retrieveIndividu(individuPrincipal));
		evtView.setAdressePrincipal(retrieveAdresse(individuPrincipal));
		retrieveTiersAssociePrincipal(evt.getId(), individuPrincipal, evtView);
		retrieveTiersAssocieMenage(evt.getId(), individuPrincipal, evtView);

		final Long conjoint = evt.getNumeroIndividuConjoint();
		if (conjoint != null) {
			evtView.setIndividuConjoint(retrieveIndividu(conjoint));
			evtView.setAdresseConjoint(retrieveAdresse(conjoint));
			retrieveTiersAssocieConjoint(evt.getId(), conjoint , evtView);
		}

		return evtView;
	}

	private void fill(EvenementCivilRegPP source, EvenementCivilRegPPDetailView target) {
		target.setEvtCommentaireTraitement(source.getCommentaireTraitement());
		target.setEvtDate(source.getDateEvenement());
		target.setEvtDateTraitement(source.getDateTraitement());
		target.setEvtEtat(source.getEtat());
		target.setEvtId(source.getId());
		target.setEvtNumeroOfsCommuneAnnonce(source.getNumeroOfsCommuneAnnonce());
		target.setEvtType(source.getType());
		for (EvenementCivilRegPPErreur err : source.getErreurs() ) {
			target.addEvtErreur(new ErreurEvenementCivilView(err.getMessage(), err.getCallstack()));
		}
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public void traiteEvenementCivil(Long id) {
		evenementCivilProcessor.recycleEvenementCivil(id);
	}


	/**
	 * @inheritDoc
	 */
	@Override
	@Transactional(rollbackFor = Throwable.class)
	public void forceEtatTraite(Long id) {
		final EvenementCivilRegPP evenementCivilExterne = evenementDAO.get(id);

		// l'état "FORCE" n'est accessible qu'aux événements civils qui ne sont pas encore traités
		if (!evenementCivilExterne.getEtat().isTraite() || evenementCivilExterne.getEtat() == EtatEvenementCivil.A_VERIFIER) {
			evenementCivilProcessor.forceEvenementCivil(evenementCivilExterne);
		}
	}

	/**
	 * @inheritDoc
	 */
	@Override
	@Transactional(readOnly = true)
	public List<EvenementCivilRegPPElementListeView> find(EvenementCivilRegPPCriteriaView bean, ParamPagination pagination) throws AdresseException {
		final List<EvenementCivilRegPPElementListeView> evtsRegPPElementListeView = new ArrayList<EvenementCivilRegPPElementListeView>();
		final List<EvenementCivilRegPP> evts = evenementDAO.find(bean, pagination);
		for (EvenementCivilRegPP evt : evts) {
			final EvenementCivilRegPPElementListeView evtRegPPElementListeView = buildView(evt);
			evtsRegPPElementListeView.add(evtRegPPElementListeView);
		}

		return evtsRegPPElementListeView;
	}

	private EvenementCivilRegPPElementListeView buildView(EvenementCivilRegPP evt) throws AdresseException {
		final EvenementCivilRegPPElementListeView evtRegPPElementListeView = new EvenementCivilRegPPElementListeView(evt);
		final PersonnePhysique habitantPrincipal = tiersService.getPersonnePhysiqueByNumeroIndividu(evt.getNumeroIndividuPrincipal());
		try {
			if (habitantPrincipal != null) {
				final EnsembleTiersCouple couple = tiersService.getEnsembleTiersCouple(habitantPrincipal, null);
				if (couple != null && couple.getMenage() != null) {
					evtRegPPElementListeView.setNumeroCTB(couple.getMenage().getNumero());
				}
				else {
					evtRegPPElementListeView.setNumeroCTB(habitantPrincipal.getNumero());
				}
			}
			if (evt.getNumeroIndividuPrincipal() != null) {
				String nom1 = adresseService.getNomCourrier(evt.getNumeroIndividuPrincipal());
				evtRegPPElementListeView.setNom1(nom1);
			}
			if (evt.getNumeroIndividuConjoint() != null) {
				String nom2 = adresseService.getNomCourrier(evt.getNumeroIndividuConjoint());
				evtRegPPElementListeView.setNom2(nom2);
			}
		}
		catch (IndividuNotFoundException e) {
			// [UNIREG-1545] on cas d'incoherence des données, on évite de crasher (dans la mesure du possible)
			LOGGER.warn("Impossible d'afficher toutes les données de l'événement civil n°" + evt.getId(), e);
			evtRegPPElementListeView.setNom1("<erreur: individu introuvable>");
		}
		return evtRegPPElementListeView;
	}

	/**
	 * @inheritDoc
	 */
	@Override
	@Transactional(readOnly = true)
	public int count(EvenementCivilCriteria criterion) {
		return evenementDAO.count(criterion);
	}

}