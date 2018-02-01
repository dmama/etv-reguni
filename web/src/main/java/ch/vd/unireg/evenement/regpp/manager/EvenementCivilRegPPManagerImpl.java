package ch.vd.unireg.evenement.regpp.manager;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSourceAware;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.unireg.interfaces.civil.ServiceCivilException;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureException;
import ch.vd.unireg.adresse.AdresseException;
import ch.vd.unireg.common.pagination.ParamPagination;
import ch.vd.unireg.evenement.civil.EvenementCivilCriteria;
import ch.vd.unireg.evenement.civil.engine.regpp.EvenementCivilProcessor;
import ch.vd.unireg.evenement.civil.regpp.EvenementCivilRegPP;
import ch.vd.unireg.evenement.civil.regpp.EvenementCivilRegPPDAO;
import ch.vd.unireg.evenement.civil.regpp.EvenementCivilRegPPErreur;
import ch.vd.unireg.evenement.common.manager.EvenementCivilManagerImpl;
import ch.vd.unireg.evenement.common.view.ErreurEvenementCivilView;
import ch.vd.unireg.evenement.regpp.view.EvenementCivilRegPPCriteriaView;
import ch.vd.unireg.evenement.regpp.view.EvenementCivilRegPPDetailView;
import ch.vd.unireg.evenement.regpp.view.EvenementCivilRegPPElementListeView;
import ch.vd.unireg.tiers.EnsembleTiersCouple;
import ch.vd.unireg.tiers.IndividuNotFoundException;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.PlusieursPersonnesPhysiquesAvecMemeNumeroIndividuException;
import ch.vd.unireg.type.EtatEvenementCivil;
import ch.vd.unireg.type.TypeEvenementCivil;

/**
 * @inheritDoc
 */
public class EvenementCivilRegPPManagerImpl extends EvenementCivilManagerImpl implements EvenementCivilRegPPManager, MessageSourceAware {

	private final Logger LOGGER = LoggerFactory.getLogger(EvenementCivilRegPPManagerImpl.class);

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
		evtView.setNoIndividu(individuPrincipal);
		try {
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
		}
		catch (ServiceCivilException e) {
			evtView.setIndividuError(e.getMessage());
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
		final List<EvenementCivilRegPPElementListeView> evtsRegPPElementListeView = new ArrayList<>();
		final List<EvenementCivilRegPP> evts = evenementDAO.find(bean, pagination);
		for (EvenementCivilRegPP evt : evts) {
			final EvenementCivilRegPPElementListeView evtRegPPElementListeView = buildView(evt);
			evtsRegPPElementListeView.add(evtRegPPElementListeView);
		}

		return evtsRegPPElementListeView;
	}

	private EvenementCivilRegPPElementListeView buildView(EvenementCivilRegPP evt) throws AdresseException {
		final EvenementCivilRegPPElementListeView evtRegPPElementListeView = new EvenementCivilRegPPElementListeView(evt);

		try {
			final PersonnePhysique habitantPrincipal = tiersService.getPersonnePhysiqueByNumeroIndividu(evt.getNumeroIndividuPrincipal());
			if (habitantPrincipal != null) {
				try {
					final EnsembleTiersCouple couple = tiersService.getEnsembleTiersCouple(habitantPrincipal, null);
					if (couple != null && couple.getMenage() != null) {
						evtRegPPElementListeView.setNumeroCTB(couple.getMenage().getNumero());
					}
					else {
						evtRegPPElementListeView.setNumeroCTB(habitantPrincipal.getNumero());
					}
				}
				catch (ServiceCivilException e) {
					LOGGER.warn("Impossible de reconstruire le couple du contribuable " + habitantPrincipal.getNumero(), e);
					evtRegPPElementListeView.setNumeroCTB(habitantPrincipal.getNumero());
				}
			}
		}
		catch (PlusieursPersonnesPhysiquesAvecMemeNumeroIndividuException e) {
			LOGGER.warn(String.format("Détermination impossible des tiers associés à l'événement civil %d : %s", evt.getId(), e.getMessage()), e);
		}

		try {
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
		catch (ServiceCivilException e) {
			// [SIFISC-7485] il ne faut plas planter l'écran si RCPers revoie une erreur...
			LOGGER.warn("Impossible d'afficher toutes les données de l'événement civil n°" + evt.getId(), e);
			evtRegPPElementListeView.setNom1("<erreur: individu inaccessible dans le registre civil>");
		}

		return evtRegPPElementListeView;
	}

	/**
	 * @inheritDoc
	 */
	@Override
	@Transactional(readOnly = true)
	public int count(EvenementCivilCriteria<TypeEvenementCivil> criterion) {
		return evenementDAO.count(criterion);
	}

}