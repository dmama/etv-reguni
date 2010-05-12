package ch.vd.uniregctb.evenement.engine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Pair;
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
import ch.vd.uniregctb.evenement.EvenementCivilData;
import ch.vd.uniregctb.evenement.EvenementCivilErreur;
import ch.vd.uniregctb.evenement.EvenementCivilDAO;
import ch.vd.uniregctb.evenement.GenericEvenementAdapter;
import ch.vd.uniregctb.evenement.common.EvenementCivilHandler;
import ch.vd.uniregctb.interfaces.model.Commune;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.tiers.PersonnePhysique;
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
	private EvenementCivilDAO evenementCivilDAO;

	/**
	 * Liste de EvenementCivilHandler capables de gérer des événements recus par le moteur de règles.
	 */
	private final Map<TypeEvenementCivil, EvenementCivilHandler> eventsHandlers = new HashMap<TypeEvenementCivil, EvenementCivilHandler>();

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings({"unchecked"})
	public void traiteEvenementsCivils(StatusManager status) {

		// Récupère les ids des événements à traiter
		TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);
		final List<Long> ids = (List<Long>) template.execute(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				return evenementCivilDAO.getEvenementCivilsNonTraites();
			}
		});

		traiteEvenements(ids, false, status);
	}

	/**
	 * {@inheritDoc}
	 */
	public Long traiteEvenementCivil(final Long evenementCivilId) {
		traiteEvenements(Arrays.asList(evenementCivilId), true, null);
		return 0L;
	}

	/**
	 * {@inheritDoc}
	 */
	public Long recycleEvenementCivil(final Long evenementCivilId) {
		traiteEvenements(Arrays.asList(evenementCivilId), false, null);
		return 0L;
	}

	/**
	 * Lance la validation puis le traitement de l'événement civil.
	 *
	 * noRollbackfor = Exception.class => ne rollback dans aucun cas, de manière à toujours garder les traces d'Audit. L'appel de la méthod
	 * handle() n'est pas impactée car elle se passe dans une autre transaction (= donc les instructions seront rollées-back en cas de
	 * problème).
	 *
	 * @param evenementCivilId
	 *            l'id de l'événement civil à traiter
	 * @return le numéro d'individu traité, ou <i>-1</i> en cas d'erreur.
	 */
	private long traiteUnEvenementCivil(final Long evenementCivilId) {

		TransactionCallback gestionEvenement = new TransactionCallback() {

		public Object doInTransaction(TransactionStatus status) {
			Long result = 0L;
			try {

				EvenementCivilData evenementCivilData = evenementCivilDAO.get(evenementCivilId);
				Assert.notNull(evenementCivilData, "l'évènement est null");

				if (evenementCivilData.getEtat() == EtatEvenementCivil.TRAITE || evenementCivilData.getEtat() == EtatEvenementCivil.A_VERIFIER) {
					LOGGER.warn("Tentative de traitement de l'événement n°" + evenementCivilId
							+ " qui est déjà traité. Aucune opération effectuée.");
					return null;
				}

				/* 2 - Délégation du traitement au handler spécifique */
				List<EvenementCivilErreur> erreurs = new ArrayList<EvenementCivilErreur>();
				List<EvenementCivilErreur> warnings = new ArrayList<EvenementCivilErreur>();

				try {

					/* 0 - On enlève les erreurs précédentes */
					evenementCivilData.getErreurs().clear();

					// Récupération de l'individu
					final Individu individu = serviceCivilService.getIndividu(evenementCivilData.getNumeroIndividuPrincipal(), RegDateHelper.getAnneeVeille(evenementCivilData.getDateEvenement()));
					Assert.notNull(individu, "Individu inconnu");

					Assert.notNull(evenementCivilData.getType(), "le type de l'événement n'est pas renseigné");
					Assert.notNull(evenementCivilData.getDateEvenement(), "La date de l'événement n'est pas renseigné");
					Assert.notNull(evenementCivilData.getNumeroOfsCommuneAnnonce(), "Le numero de la commune d'annonce n'est pas renseigné");
					Assert.isTrue(evenementCivilData.getEtat() != EtatEvenementCivil.TRAITE, "l'évènement est déjà traité");
					Assert.isTrue(evenementCivilData.getEtat() != EtatEvenementCivil.A_VERIFIER, "l'évènement est déjà traité");

					// Controle la commune OFS
					int numeroOFS = evenementCivilData.getNumeroOfsCommuneAnnonce();
					if(numeroOFS != 0){
						// Commune introuvable => Exception
						Commune c = serviceInfrastructureService.getCommuneByNumeroOfsEtendu(numeroOFS, evenementCivilData.getDateEvenement());
						Assert.notNull(c, "La commune avec le numéro OFS "+numeroOFS+" n'existe pas");
					}

					//2 - traitement de l'événement
					Audit.info(evenementCivilData.getId(), "Début du traitement de l'événement civil de type "+ evenementCivilData.getType().name());

					if (!eventsHandlers.containsKey(evenementCivilData.getType())) {
						erreurs.add(new EvenementCivilErreur("Aucun handler défini pour ce type d'événement"));
					}
					else {
						traiteEvenement(evenementCivilData, erreurs, warnings);
					}

					/* 3 - Mise à jour du statut */
					if (!erreurs.isEmpty()) {
						evenementCivilData.setEtat(EtatEvenementCivil.EN_ERREUR);
						evenementCivilData.setDateTraitement(new Date());
						Audit.error(evenementCivilData.getId(), "Status changé à ERREUR");
						result = -1L;
						dumpForDebug(erreurs);
					}
					else if(!warnings.isEmpty()) {
						evenementCivilData.setEtat(EtatEvenementCivil.A_VERIFIER);
						evenementCivilData.setDateTraitement(new Date());
						Audit.warn(evenementCivilData.getId(), "Status changé à A VERIFIER");
						result = evenementCivilData.getNumeroIndividuPrincipal();
						dumpForDebug(warnings);
					}
					else {
						evenementCivilData.setEtat(EtatEvenementCivil.TRAITE);
						evenementCivilData.setDateTraitement(new Date());
						Audit.success(evenementCivilData.getId(), "Status changé à TRAITE");
						result = evenementCivilData.getNumeroIndividuPrincipal();
					}
				}
				catch (Exception e) {
					LOGGER.error("Erreur lors du traitement de l'événement : " + evenementCivilData.getId(), e);
					erreurs.add(new EvenementCivilErreur(e));
					evenementCivilData.setEtat(EtatEvenementCivil.EN_ERREUR);
					Audit.error(evenementCivilData.getId(), e);
					Audit.error(evenementCivilData.getId(), "Status changé à ERREUR");
				}

				for (EvenementCivilErreur e : erreurs) {
					Audit.error(evenementCivilData.getId(), e.getMessage());
				}
				for (EvenementCivilErreur w : warnings) {
					Audit.warn(evenementCivilData.getId(), w.getMessage());
				}

				evenementCivilData.addErrors(erreurs);
				evenementCivilData.addWarnings(warnings);
			}
			catch (IllegalArgumentException e) {
				LOGGER.error("Traitement impossible de l'événement : " + evenementCivilId, e);
			}
			return result;
		}
		};

		LOGGER.debug("Début du traitement de l'événement" + evenementCivilId);

		/*DefaultTransactionDefinition def = new DefaultTransactionDefinition();
		def.setPropagationBehavior(DefaultTransactionDefinition.PROPAGATION_REQUIRES_NEW);
		TransactionStatus tx = transactionManager.getTransaction(def);*/
		TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
		return (Long)template.execute(gestionEvenement);
	}

	private void traiteEvenement(final EvenementCivilData evenementCivilData,final List<EvenementCivilErreur> erreurs,final List<EvenementCivilErreur> warnings){

		TransactionCallback traitementEvenement = new TransactionCallback() {

			public Object doInTransaction(TransactionStatus status) {
				try{
					EvenementCivilHandler evenementCivilHandler = eventsHandlers.get(evenementCivilData.getType());

					/* Initialisation de l'Adapter */
					LOGGER.debug("Initialisation de l'adaptateur associé à l'événement : " + evenementCivilData.getId() );
					GenericEvenementAdapter adapter = evenementCivilHandler.createAdapter();
					//long indP = evenementCivilData.getNumeroIndividuPrincipal();
//					LOGGER.debug("adapter.init(evenementCivilData, serviceCivilService, serviceInfrastructureService)");
					adapter.init(evenementCivilData, serviceCivilService, serviceInfrastructureService);

					/* 2.1 - lancement de la validation par le handler */
//					LOGGER.debug("evenementCivilHandler.checkCompleteness(adapter, erreurs, warnings)");
					evenementCivilHandler.checkCompleteness(adapter, erreurs, warnings);

					/* 2.2 - lancement de la validation par le handler */
					if (erreurs.isEmpty()) {
//						LOGGER.debug("evenementCivilHandler.validate(adapter, erreurs, warnings)");
						evenementCivilHandler.validate(adapter, erreurs, warnings);
						/* 2.3 - lancement du traitement par le handler */
						if (erreurs.isEmpty()) {
//							LOGGER.debug("evenementCivilHandler.handle(adapter, warnings)");
							final Pair<PersonnePhysique, PersonnePhysique> nouveauxHabitants = evenementCivilHandler.handle(adapter, warnings);

							// adaptation des données dans l'événement civil en cas de création de nouveaux habitants
							if (nouveauxHabitants != null) {
								if (nouveauxHabitants.getFirst() != null && evenementCivilData.getNumeroIndividuPrincipal() != null && evenementCivilData.getNumeroIndividuPrincipal() > 0L) {
									if (evenementCivilData.getHabitantPrincipal() == null) {
										evenementCivilData.setHabitantPrincipal(nouveauxHabitants.getFirst());
									}
									else {
										Assert.isEqual(evenementCivilData.getHabitantPrincipal().getNumero(), nouveauxHabitants.getFirst().getNumero());
									}
								}
								if (nouveauxHabitants.getSecond() != null && evenementCivilData.getNumeroIndividuConjoint() != null && evenementCivilData.getNumeroIndividuConjoint() > 0L) {
									if (evenementCivilData.getHabitantConjoint() == null) {
										evenementCivilData.setHabitantConjoint(nouveauxHabitants.getSecond());
									}
									else {
										Assert.isEqual(evenementCivilData.getHabitantConjoint().getNumero(), nouveauxHabitants.getSecond().getNumero());
									}
								}
							}
//							LOGGER.debug("done.");
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
					LOGGER.debug("Impossible d'adapter l'événement civil : " + evenementCivilData.getId(), e);
					erreurs.add(new EvenementCivilErreur(e));
					Audit.error(evenementCivilData.getId(), e);
				}
				return null;
			}
		};

		TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
		try {
			// on ajoute le numéro de l'événement civil comme suffix à l'utilisateur principal, de manière à faciliter le tracing
			AuthenticationHelper.pushPrincipal("EvtCivil-" + evenementCivilData.getId());
			template.execute(traitementEvenement);
		}
		finally {
			AuthenticationHelper.popPrincipal();
		}
	}

	@SuppressWarnings({"unchecked"})
	private void retraiteEvenementsEnErreurIndividu(final Long numIndividu) {

		// 1 - Récupération des ids des événements civils en erreur de l'individu
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);
		final List<Long> ids = (List<Long>) template.execute(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				return evenementCivilDAO.getIdsEvenementCivilsErreurIndividu(numIndividu);
			}
		});

		/* 2 - Iteration sur les ids des événements civils */
		traiteEvenements(ids, false, null);
	}

	/**
	 * [UNIREG-1200] Traite tous les événements civils spécifiés, et retraite automatiquement les événements en erreur si possible.
	 *
	 * @param ids            les ids des événements civils à traiter
	 * @param forceRecyclage si <i>vrai</i>, force le recyclage de tous les événements en erreur associé aux individus traités
	 * @param status         un status manager (optionel, peut être nul)
	 */
	private void traiteEvenements(final List<Long> ids, boolean forceRecyclage, StatusManager status) {
		Set<Long> individusTraites = new HashSet<Long>();
		// Traite les événements spécifiées
		for (final Long id : ids) {
			if (status != null && status.interrupted()) {
				break;
			}
			final Long numInd = traiteUnEvenementCivil(id);
			if (numInd != null && numInd > 0L) {
				individusTraites.add(numInd);
			}
			else if (forceRecyclage && numInd == -1L) {//evt en erreur

				final TransactionTemplate template = new TransactionTemplate(transactionManager);
				template.setReadOnly(true);
				final Long numIndividu = (Long) template.execute(new TransactionCallback() {
					public Object doInTransaction(TransactionStatus status) {
						return evenementCivilDAO.get(id).getNumeroIndividuPrincipal();
					}
				});

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

	@SuppressWarnings({"UnusedDeclaration"})
	public void setEvenementCivilDAO(EvenementCivilDAO evenementCivilDAO) {
		this.evenementCivilDAO = evenementCivilDAO;
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
