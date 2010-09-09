package ch.vd.uniregctb.evenement.engine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.infrastructure.service.InfrastructureException;
import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.registre.base.utils.Pair;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.data.DataEventService;
import ch.vd.uniregctb.evenement.EvenementAdapterException;
import ch.vd.uniregctb.evenement.EvenementCivilDAO;
import ch.vd.uniregctb.evenement.EvenementCivilData;
import ch.vd.uniregctb.evenement.EvenementCivilErreur;
import ch.vd.uniregctb.evenement.GenericEvenementAdapter;
import ch.vd.uniregctb.evenement.common.EvenementCivilHandler;
import ch.vd.uniregctb.interfaces.model.Commune;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersDAO;
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
	private TiersDAO tiersDAO;
	private DataEventService dataEventService;

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

		traiteEvenements(ids, false, false, status);
	}

	/**
	 * {@inheritDoc}
	 */
	public Long traiteEvenementCivil(final Long evenementCivilId, boolean refreshCache) {
		traiteEvenements(Arrays.asList(evenementCivilId), true, refreshCache, null);
		return 0L;
	}

	/**
	 * {@inheritDoc}
	 */
	public Long recycleEvenementCivil(final Long evenementCivilId) {
		traiteEvenements(Arrays.asList(evenementCivilId), false, false, null);
		return 0L;
	}

	/**
	 * Lance le traitement de l'événement civil.
	 *
	 * @param evenementCivilId l'id de l'événement civil à traiter
	 * @param refreshCache   si <i>vrai</i> le cache individu des personnes concernées par l'événement doit être rafraîchi avant le traitement
	 * @return le numéro d'individu traité, ou <i>null</i> en cas d'erreur
	 */
	private Long traiteUnEvenementCivil(final Long evenementCivilId, final boolean refreshCache) {

		Long result;

		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
		try {
			// on ajoute le numéro de l'événement civil comme suffix à l'utilisateur principal, de manière à faciliter le tracing
			AuthenticationHelper.pushPrincipal(String.format("EvtCivil-%d", evenementCivilId));

			// Tout d'abord, on essaie de traiter l'événement
			result = (Long) template.execute(new TransactionCallback() {
				public Object doInTransaction(TransactionStatus status) {

					final List<EvenementCivilErreur> erreurs = new ArrayList<EvenementCivilErreur>();
					final List<EvenementCivilErreur> warnings = new ArrayList<EvenementCivilErreur>();

					// Charge l'événement
					final EvenementCivilData evenementCivilData = evenementCivilDAO.get(evenementCivilId);
					Assert.notNull(evenementCivilData, "l'événement est null");

					if (evenementCivilData.getEtat().isTraite()) {
						LOGGER.warn("Tentative de traitement de l'événement n°" + evenementCivilId + " qui est déjà traité. Aucune opération effectuée.");
						return evenementCivilData.getNumeroIndividuPrincipal();
					}

					// On enlève les erreurs précédentes
					evenementCivilData.getErreurs().clear();

					Assert.notNull(evenementCivilData.getType(), "le type de l'événement n'est pas renseigné");
					Assert.notNull(evenementCivilData.getDateEvenement(), "La date de l'événement n'est pas renseigné");
					Assert.notNull(evenementCivilData.getNumeroOfsCommuneAnnonce(), "Le numero de la commune d'annonce n'est pas renseigné");

					// Controle la commune OFS
					int numeroOFS = evenementCivilData.getNumeroOfsCommuneAnnonce();
					if (numeroOFS != 0) {
						Commune c;
						try {
							c = serviceInfrastructureService.getCommuneByNumeroOfsEtendu(numeroOFS, evenementCivilData.getDateEvenement());
						}
						catch (InfrastructureException e) {
							LOGGER.error(e, e);
							c = null;
						}
						if (c == null) {
							// Commune introuvable => Exception
							throw new IllegalArgumentException("La commune avec le numéro OFS " + numeroOFS + " n'existe pas");
						}
					}

					// Traitement de l'événement
					Audit.info(evenementCivilData.getId(), String.format("Début du traitement de l'événement civil %d de type %s", evenementCivilData.getId(), evenementCivilData.getType().name()));
					traiteEvenement(evenementCivilData, refreshCache, erreurs, warnings);

					return traiteErreurs(evenementCivilData, erreurs, warnings);
				}
			});
		}
		catch (final Exception e) {
			LOGGER.error("Erreur lors du traitement de l'événement : " + evenementCivilId, e);

			// En cas d'exception, on met-à-jour la liste d'erreur pour l'événement 
			result = (Long) template.execute(new TransactionCallback() {
				public Object doInTransaction(TransactionStatus status) {

					final List<EvenementCivilErreur> erreurs = new ArrayList<EvenementCivilErreur>();
					final List<EvenementCivilErreur> warnings = new ArrayList<EvenementCivilErreur>();
					erreurs.add(new EvenementCivilErreur(e));

					final EvenementCivilData evenementCivilData = evenementCivilDAO.get(evenementCivilId);
					evenementCivilData.getErreurs().clear();
					return traiteErreurs(evenementCivilData, erreurs, warnings);
				}
			});
		}
		finally {
			AuthenticationHelper.popPrincipal();
		}

		return result;
	}

	private Long traiteErreurs(EvenementCivilData evenementCivilData, List<EvenementCivilErreur> erreurs, List<EvenementCivilErreur> warnings) {

		final Long result;
		
		if (!erreurs.isEmpty()) {
			evenementCivilData.setEtat(EtatEvenementCivil.EN_ERREUR);
			evenementCivilData.setDateTraitement(DateHelper.getCurrentDate());
			Audit.error(evenementCivilData.getId(), "Status changé à ERREUR");
			result = null;
			dumpForDebug(erreurs);
		}
		else if (!warnings.isEmpty()) {
			evenementCivilData.setEtat(EtatEvenementCivil.A_VERIFIER);
			evenementCivilData.setDateTraitement(DateHelper.getCurrentDate());
			Audit.warn(evenementCivilData.getId(), "Status changé à A VERIFIER");
			result = evenementCivilData.getNumeroIndividuPrincipal();
			dumpForDebug(warnings);
		}
		else {
			evenementCivilData.setEtat(EtatEvenementCivil.TRAITE);
			evenementCivilData.setDateTraitement(DateHelper.getCurrentDate());
			Audit.success(evenementCivilData.getId(), "Status changé à TRAITE");
			result = evenementCivilData.getNumeroIndividuPrincipal();
		}

		for (EvenementCivilErreur e : erreurs) {
			Audit.error(evenementCivilData.getId(), e.getMessage());
		}
		for (EvenementCivilErreur w : warnings) {
			Audit.warn(evenementCivilData.getId(), w.getMessage());
		}

		evenementCivilData.addErrors(erreurs);
		evenementCivilData.addWarnings(warnings);

		return result;
	}

	private void traiteEvenement(final EvenementCivilData evenementCivilData, boolean refreshCache, final List<EvenementCivilErreur> erreurs, final List<EvenementCivilErreur> warnings) {
		try {
//			LOGGER.debug("evenementCivilData.getNumeroIndividuPrincipal()");

			// On complète l'événement à la volée (on est obligé de le faire ici dans tous les cas, car le tiers
			// correspondant peut avoir été créé entre la réception de l'événement et son re-traitement).
			final Long noIndPrinc = evenementCivilData.getNumeroIndividuPrincipal();
			Assert.notNull(noIndPrinc, "Le numéro d'individu de l'événement ne peut pas être nul");

			final Long principalID = tiersDAO.getNumeroPPByNumeroIndividu(noIndPrinc, true);
			evenementCivilData.setHabitantPrincipalId(principalID);

			final Long noIndConj = evenementCivilData.getNumeroIndividuConjoint();
			if (noIndConj != null) {
				final Long conjointID = tiersDAO.getNumeroPPByNumeroIndividu(noIndConj, true);
				evenementCivilData.setHabitantConjointId(conjointID);
			}

			final EvenementCivilHandler evenementCivilHandler = eventsHandlers.get(evenementCivilData.getType());
			if (evenementCivilHandler == null) {
				erreurs.add(new EvenementCivilErreur("Aucun handler défini pour ce type d'événement"));
				return;
			}

			final GenericEvenementAdapter adapter = evenementCivilHandler.createAdapter();

			adapter.init(evenementCivilData, serviceCivilService, serviceInfrastructureService, refreshCache ? dataEventService : null);

			/* 2.1 - lancement de la validation par le handler */
			evenementCivilHandler.checkCompleteness(adapter, erreurs, warnings);

			/* 2.2 - lancement de la validation par le handler */
			if (erreurs.isEmpty()) {
				evenementCivilHandler.validate(adapter, erreurs, warnings);
				/* 2.3 - lancement du traitement par le handler */
				if (erreurs.isEmpty()) {
					final Pair<PersonnePhysique, PersonnePhysique> nouveauxHabitants = evenementCivilHandler.handle(adapter, warnings);

					// adaptation des données dans l'événement civil en cas de création de nouveaux habitants
					if (nouveauxHabitants != null) {
						if (nouveauxHabitants.getFirst() != null && noIndPrinc != null && noIndPrinc > 0L) {
							if (evenementCivilData.getHabitantPrincipalId() == null) {
								evenementCivilData.setHabitantPrincipalId(nouveauxHabitants.getFirst().getId());
							}
							else {
								Assert.isEqual(evenementCivilData.getHabitantPrincipalId(), nouveauxHabitants.getFirst().getNumero());
							}
						}
						if (nouveauxHabitants.getSecond() != null && noIndConj != null && noIndConj > 0L) {
							if (evenementCivilData.getHabitantConjointId() == null) {
								evenementCivilData.setHabitantConjointId(nouveauxHabitants.getSecond().getId());
							}
							else {
								Assert.isEqual(evenementCivilData.getHabitantConjointId(), nouveauxHabitants.getSecond().getNumero());
							}
						}
					}
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
	}

	@SuppressWarnings({"unchecked"})
	private void retraiteEvenementsEnErreurIndividu(final Long numIndividu, List<Long> evenementsExclus) {

		// 1 - Récupération des ids des événements civils en erreur de l'individu
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);
		final List<Long> ids = (List<Long>) template.execute(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				return evenementCivilDAO.getIdsEvenementCivilsErreurIndividu(numIndividu);
			}
		});

		/* 2 - Iteration sur les ids des événements civils */
		final List<Long> aRelancer = new ArrayList<Long>(ids);
		aRelancer.removeAll(evenementsExclus);
		if (aRelancer.size() > 0) {
			traiteEvenements(aRelancer, false, false, null);
		}
	}

	/**
	 * [UNIREG-1200] Traite tous les événements civils spécifiés, et retraite automatiquement les événements en erreur si possible.
	 *
	 * @param ids            les ids des événements civils à traiter
	 * @param forceRecyclage si <i>vrai</i>, force le recyclage de tous les événements en erreur associé aux individus traités
	 * @param refreshCache   si <i>vrai</i> le cache individu des personnes concernées par l'événement doit être rafraîchi avant le traitement
	 * @param status         un status manager (optionel, peut être nul)
	 */
	private void traiteEvenements(final List<Long> ids, boolean forceRecyclage, boolean refreshCache, StatusManager status) {
		final Set<Long> individusTraites = new HashSet<Long>();
		// Traite les événements spécifiées
		for (final Long id : ids) {
			if (status != null && status.interrupted()) {
				break;
			}
			final Long numInd = traiteUnEvenementCivil(id, refreshCache);
			if (numInd != null && numInd > 0L) {
				individusTraites.add(numInd);
			}
			else if (forceRecyclage && numInd == null) {//evt en erreur

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
			retraiteEvenementsEnErreurIndividu(numInd, ids);
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

	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
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

	public void setDataEventService(DataEventService dataEventService) {
		this.dataEventService = dataEventService;
	}
}
