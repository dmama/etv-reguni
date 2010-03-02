package ch.vd.uniregctb.evenement.engine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ch.vd.uniregctb.common.StatusManager;
import org.apache.log4j.Logger;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.evenement.EvenementAdapterException;
import ch.vd.uniregctb.evenement.EvenementCivilErreur;
import ch.vd.uniregctb.evenement.EvenementCivilRegroupe;
import ch.vd.uniregctb.evenement.EvenementCivilRegroupeDAO;
import ch.vd.uniregctb.evenement.GenericEvenementAdapter;
import ch.vd.uniregctb.evenement.common.EvenementCivilHandler;
import ch.vd.uniregctb.interfaces.model.Commune;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.TypeEvenementCivil;

/**
 * Moteur de règle permettant d'appliquer les règles métiers. Le moteur contient une liste de EvenementCivilHandler capables de gérer les
 * événements en entrée. Pour chaque événement recu, il invoque les EvenementCivilHandler capables de gérer cet événements.
 *
 * @author Akram BEN AISSI <mailto:akram.ben-aissi@vd.ch>
 */
public class EvenementCivilProcessorImpl implements EvenementCivilProcessor, EvenementHandlerRegistrar {

	private static final Logger LOGGER = Logger.getLogger(EvenementCivilProcessorImpl.class);

	private PlatformTransactionManager transactionManager;

	private ServiceCivilService serviceCivilService;

	private ServiceInfrastructureService serviceInfrastructureService;

	/**
	 * Liste de EvenementCivilHandler capables de gérer des événements recus par le moteur de règles.
	 */
	private final Map<TypeEvenementCivil, EvenementCivilHandler> eventsHandlers = new HashMap<TypeEvenementCivil, EvenementCivilHandler>();

	/**
	 * Le DAO des événements civils regroupes.
	 */
	private EvenementCivilRegroupeDAO evenementCivilRegroupeDAO;

	/**
	 * {@inheritDoc}
	 */
	public void traiteEvenementsCivilsRegroupes(StatusManager status) {
		final List<Long> ids = evenementCivilRegroupeDAO.getEvenementCivilsNonTraites();
		traiteEvenements(ids, false, status);
	}

	/**
	 * {@inheritDoc}
	 */
	public Long traiteEvenementCivilRegroupe(final Long evenementCivilRegroupeId) {
		traiteEvenements(Arrays.asList(evenementCivilRegroupeId), true, null);
		return 0L;
	}

	/**
	 * {@inheritDoc}
	 */
	public Long recycleEvenementCivilRegroupe(final Long evenementCivilRegroupeId) {
		traiteEvenements(Arrays.asList(evenementCivilRegroupeId), false, null);
		return 0L;
	}

	/**
	 * Lance la validation puis le traitement de l'événement civil regroupé.
	 *
	 * noRollbackfor = Exception.class => ne rollback dans aucun cas, de manière à toujours garder les traces d'Audit. L'appel de la méthod
	 * handle() n'est pas impactée car elle se passe dans une autre transaction (= donc les instructions seront rollées-back en cas de
	 * problème).
	 *
	 * @param evenementCivilRegroupeId
	 *            l'id de l'événement civil regroupé à traiter
	 * @return le numéro d'individu traité, ou <i>-1</i> en cas d'erreur.
	 */
	private long traiteUnEvenementCivilRegroupe(final Long evenementCivilRegroupeId) {

		TransactionCallback gestionEvenement = new TransactionCallback() {

		public Object doInTransaction(TransactionStatus status) {
			Long result = 0L;
			try {

				EvenementCivilRegroupe evenementCivilRegroupe = evenementCivilRegroupeDAO.get(evenementCivilRegroupeId);
				Assert.notNull(evenementCivilRegroupe, "l'évènement est null");

				if (evenementCivilRegroupe.getEtat() == EtatEvenementCivil.TRAITE || evenementCivilRegroupe.getEtat() == EtatEvenementCivil.A_VERIFIER) {
					LOGGER.warn("Tentative de traitement de l'événement n°" + evenementCivilRegroupeId
							+ " qui est déjà traité. Aucune opération effectuée.");
					return null;
				}

				/* 2 - Délégation du traitement au handler spécifique */
				List<EvenementCivilErreur> erreurs = new ArrayList<EvenementCivilErreur>();
				List<EvenementCivilErreur> warnings = new ArrayList<EvenementCivilErreur>();

				try {

					/* 0 - On enlève les erreurs précédentes */
					evenementCivilRegroupe.getErreurs().clear();

					Assert.notNull(evenementCivilRegroupe.getType(), "le type de l'événement n'est pas renseigné");
					Assert.notNull(evenementCivilRegroupe.getDateEvenement(), "La date de l'événement n'est pas renseigné");
					Assert.notNull(evenementCivilRegroupe.getNumeroOfsCommuneAnnonce(), "Le numero de la commune d'annonce n'est pas renseigné");
					Assert.isTrue(evenementCivilRegroupe.getEtat() != EtatEvenementCivil.TRAITE, "l'évènement est déjà traité");
					Assert.isTrue(evenementCivilRegroupe.getEtat() != EtatEvenementCivil.A_VERIFIER, "l'évènement est déjà traité");

					// Controle la commune OFS
					int numeroOFS = evenementCivilRegroupe.getNumeroOfsCommuneAnnonce();
					if(numeroOFS != 0){
						// Commune introuvable => Exception
						Commune c = serviceInfrastructureService.getCommuneByNumeroOfsEtendu(numeroOFS, evenementCivilRegroupe.getDateEvenement());
						Assert.notNull(c, "La commune avec le numéro OFS "+numeroOFS+" n'existe pas");
					}

					//2 - traitement de l'événement
					Audit.info(evenementCivilRegroupe.getId(), "Début du traitement de l'événement civil de type "+evenementCivilRegroupe.getType().name());

					if (!eventsHandlers.containsKey(evenementCivilRegroupe.getType())) {
						erreurs.add(new EvenementCivilErreur("Aucun handler défini pour ce type d'événement"));
					}
					else {
						traiteEvenement(evenementCivilRegroupe, erreurs, warnings);
					}

					/* 3 - Mise à jour du statut */
					if (!erreurs.isEmpty()) {
						evenementCivilRegroupe.setEtat(EtatEvenementCivil.EN_ERREUR);
						evenementCivilRegroupe.setDateTraitement(new Date());
						Audit.error(evenementCivilRegroupe.getId(), "Status changé à ERREUR");
						result = -1L;
						dumpForDebug(erreurs);
					}
					else if(!warnings.isEmpty()) {
						evenementCivilRegroupe.setEtat(EtatEvenementCivil.A_VERIFIER);
						evenementCivilRegroupe.setDateTraitement(new Date());
						Audit.warn(evenementCivilRegroupe.getId(), "Status changé à A VERIFIER");
						result = evenementCivilRegroupe.getNumeroIndividuPrincipal();
						dumpForDebug(warnings);
					}
					else {
						evenementCivilRegroupe.setEtat(EtatEvenementCivil.TRAITE);
						evenementCivilRegroupe.setDateTraitement(new Date());
						Audit.success(evenementCivilRegroupe.getId(), "Status changé à TRAITE");
						result = evenementCivilRegroupe.getNumeroIndividuPrincipal();
					}
				}
				catch (Exception e) {
					LOGGER.error("Erreur lors du traitement de l'événement : " + evenementCivilRegroupe.getId(), e);
					erreurs.add(new EvenementCivilErreur(e));
					evenementCivilRegroupe.setEtat(EtatEvenementCivil.EN_ERREUR);
					Audit.error(evenementCivilRegroupe.getId(), e);
					Audit.error(evenementCivilRegroupe.getId(), "Status changé à ERREUR");
				}

				for (EvenementCivilErreur e : erreurs) {
					Audit.error(evenementCivilRegroupe.getId(), e.getMessage());
				}
				for (EvenementCivilErreur w : warnings) {
					Audit.warn(evenementCivilRegroupe.getId(), w.getMessage());
				}

				evenementCivilRegroupe.addErrors(erreurs);
				evenementCivilRegroupe.addWarnings(warnings);
			}
			catch (IllegalArgumentException e) {
				LOGGER.error("Traitement impossible de l'événement : " + evenementCivilRegroupeId, e);
			}
			return result;
		}
		};

		LOGGER.debug("Début du traitement de l'événement" + evenementCivilRegroupeId);

		/*DefaultTransactionDefinition def = new DefaultTransactionDefinition();
		def.setPropagationBehavior(DefaultTransactionDefinition.PROPAGATION_REQUIRES_NEW);
		TransactionStatus tx = transactionManager.getTransaction(def);*/
		TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
		return (Long)template.execute(gestionEvenement);
	}

	private void traiteEvenement(final EvenementCivilRegroupe evenementCivilRegroupe,final List<EvenementCivilErreur> erreurs,final List<EvenementCivilErreur> warnings){

		TransactionCallback traitementEvenement = new TransactionCallback() {

			public Object doInTransaction(TransactionStatus status) {
				try{
					EvenementCivilHandler evenementCivilHandler = eventsHandlers.get(evenementCivilRegroupe.getType());

					/* Initialisation de l'Adapter */
					LOGGER.debug("Initialisation de l'adaptateur associé à l'événement : " + evenementCivilRegroupe.getId() );
					GenericEvenementAdapter adapter = evenementCivilHandler.createAdapter();
					//long indP = evenementCivilRegroupe.getNumeroIndividuPrincipal();
					adapter.init(evenementCivilRegroupe, serviceCivilService, serviceInfrastructureService);

					/* 2.1 - lancement de la validation par le handler */
					evenementCivilHandler.checkCompleteness(adapter, erreurs, warnings);

					/* 2.2 - lancement de la validation par le handler */
					if (erreurs.isEmpty()) {
						evenementCivilHandler.validate(adapter, erreurs, warnings);
						/* 2.3 - lancement du traitement par le handler */
						if (erreurs.isEmpty()) {
							evenementCivilHandler.handle(adapter, warnings);
						}
						else {
							Audit.error(adapter.getNumeroEvenement(), "l'événement n'est pas valide");
						}
					}
					else {
						Audit.error(adapter.getNumeroEvenement(), "l'événement est incomplet");
					}
				}
				catch (EvenementAdapterException e) {
					LOGGER.debug("Impossible d'adapter l'événement regroupé : " + evenementCivilRegroupe.getId(), e);
					erreurs.add(new EvenementCivilErreur(e));
					Audit.error(evenementCivilRegroupe.getId(), e);
				}
				return null;
			}
		};

		TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
		try {
			// on ajoute le numéro de l'événement civil comme suffix à l'utilisateur principal, de manière à faciliter le tracing
			AuthenticationHelper.pushPrincipal("EvtCivil-" + evenementCivilRegroupe.getId());
			template.execute(traitementEvenement);
		}
		finally {
			AuthenticationHelper.popPrincipal();
		}
	}

	private void retraiteEvenementsEnErreurIndividu(Long numIndividu){
		// 1 - Récupération des ids des événements civils regroupés en erreur de l'individu
		final List<Long> ids = evenementCivilRegroupeDAO.getIdsEvenementCivilsErreurIndividu(numIndividu);

		/* 2 - Iteration sur les ids des événements regroupés */
		traiteEvenements(ids, false, null);
	}

	/**
	 * [UNIREG-1200] Traite tous les événements civils spécifiés, et retraite automatiquement les événements en erreur si possible.
	 *
	 * @param ids            les ids des événements regroupés à traiter
	 * @param forceRecyclage si <i>vrai</i>, force le recyclage de tous les événements en erreur associé aux individus traités
	 * @param status         un status manager (optionel, peut être nul)
	 */
	private void traiteEvenements(final List<Long> ids, boolean forceRecyclage, StatusManager status) {
		Set<Long> individusTraites = new HashSet<Long>();
		// Traite les événements spécifiées
		for (Long id : ids) {
			if (status != null && status.interrupted()) {
				break;
			}
			final Long numInd = traiteUnEvenementCivilRegroupe(id);
			if (numInd != null && numInd > 0L) {
				individusTraites.add(numInd);
			}
			else if (forceRecyclage && numInd == -1L) {//evt en erreur
				Long numIndividu = evenementCivilRegroupeDAO.get(id).getNumeroIndividuPrincipal();
				individusTraites.add(numIndividu);
			}
		}

		// Re-traite automatiquement les (éventuels) événements en erreur sur les individus traités
		for (Long numInd : individusTraites) {
			if (status != null && status.interrupted()) {
				break;
			}
			retraiteEvenementsEnErreurIndividu(numInd);
		}
	}

	private void dumpForDebug(List<EvenementCivilErreur> erreurs) {

		boolean enabled = false;
		//enabled = true;
		//noinspection ConstantConditions
		if (enabled) {
			for (EvenementCivilErreur e : erreurs) {
				LOGGER.debug("Erreur message: "+e);
			}
		}
	}

	/**
	 * @param evenementCivilRegroupeDAO
	 *            the evenementCivilRegroupeDAO to set
	 */
	@SuppressWarnings({"UnusedDeclaration"})
	public void setEvenementCivilRegroupeDAO(EvenementCivilRegroupeDAO evenementCivilRegroupeDAO) {
		this.evenementCivilRegroupeDAO = evenementCivilRegroupeDAO;
	}

	public void register(TypeEvenementCivil type, EvenementCivilHandler handler) {

		eventsHandlers.put(type, handler);
	}

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	public void setServiceCivilService(ServiceCivilService serviceCivilService) {
		this.serviceCivilService = serviceCivilService;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setServiceInfrastructureService(ServiceInfrastructureService serviceInfrastructureService) {
		this.serviceInfrastructureService = serviceInfrastructureService;
	}

}
