package ch.vd.uniregctb.evenement.civil.engine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
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
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilOptions;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterneDAO;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterneErreur;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.uniregctb.interfaces.model.Commune;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.type.EtatEvenementCivil;

/**
 * Moteur de règle permettant d'appliquer les règles métiers. Le moteur contient une liste de EvenementCivilTranslationStrategy capables de gérer les
 * événements en entrée. Pour chaque événement recu, il invoque les EvenementCivilTranslationStrategy capables de gérer cet événements.
 *
 * @author Akram BEN AISSI <mailto:akram.ben-aissi@vd.ch>
 */
public class EvenementCivilProcessorImpl implements EvenementCivilProcessor {

	private static final Logger LOGGER = Logger.getLogger(EvenementCivilProcessorImpl.class);

	private PlatformTransactionManager transactionManager;
	private ServiceInfrastructureService serviceInfrastructureService;
	private EvenementCivilExterneDAO evenementCivilExterneDAO;
	private TiersDAO tiersDAO;

	private EvenementCivilTranslator evenementCivilTranslator;

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
				return evenementCivilExterneDAO.getEvenementCivilsNonTraites();
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

					final List<EvenementCivilExterneErreur> erreurs = new ArrayList<EvenementCivilExterneErreur>();
					final List<EvenementCivilExterneErreur> warnings = new ArrayList<EvenementCivilExterneErreur>();

					// Charge l'événement
					final EvenementCivilExterne evenementCivilExterne = evenementCivilExterneDAO.get(evenementCivilId);
					Assert.notNull(evenementCivilExterne, "l'événement est null");

					if (evenementCivilExterne.getEtat().isTraite()) {
						LOGGER.warn("Tentative de traitement de l'événement n°" + evenementCivilId + " qui est déjà traité. Aucune opération effectuée.");
						return evenementCivilExterne.getNumeroIndividuPrincipal();
					}

					// On enlève les erreurs précédentes
					evenementCivilExterne.getErreurs().clear();

					Assert.notNull(evenementCivilExterne.getType(), "le type de l'événement n'est pas renseigné");
					Assert.notNull(evenementCivilExterne.getDateEvenement(), "La date de l'événement n'est pas renseigné");
					Assert.notNull(evenementCivilExterne.getNumeroOfsCommuneAnnonce(), "Le numero de la commune d'annonce n'est pas renseigné");

					// Controle la commune OFS
					int numeroOFS = evenementCivilExterne.getNumeroOfsCommuneAnnonce();
					if (numeroOFS != 0) {
						Commune c;
						try {
							c = serviceInfrastructureService.getCommuneByNumeroOfsEtendu(numeroOFS, evenementCivilExterne.getDateEvenement());
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
					Audit.info(
							evenementCivilExterne.getId(), String.format("Début du traitement de l'événement civil %d de type %s", evenementCivilExterne.getId(), evenementCivilExterne.getType().name()));
					traiteEvenement(evenementCivilExterne, refreshCache, erreurs, warnings);

					return traiteErreurs(evenementCivilExterne, erreurs, warnings);
				}
			});
		}
		catch (final Exception e) {
			LOGGER.error("Erreur lors du traitement de l'événement : " + evenementCivilId, e);

			// En cas d'exception, on met-à-jour la liste d'erreur pour l'événement 
			result = (Long) template.execute(new TransactionCallback() {
				public Object doInTransaction(TransactionStatus status) {

					final List<EvenementCivilExterneErreur> erreurs = new ArrayList<EvenementCivilExterneErreur>();
					final List<EvenementCivilExterneErreur> warnings = new ArrayList<EvenementCivilExterneErreur>();
					erreurs.add(new EvenementCivilExterneErreur(e));

					final EvenementCivilExterne evenementCivilExterne = evenementCivilExterneDAO.get(evenementCivilId);
					evenementCivilExterne.getErreurs().clear();
					return traiteErreurs(evenementCivilExterne, erreurs, warnings);
				}
			});
		}
		finally {
			AuthenticationHelper.popPrincipal();
		}

		return result;
	}

	private Long traiteErreurs(EvenementCivilExterne evenementCivilExterne, List<EvenementCivilExterneErreur> erreurs, List<EvenementCivilExterneErreur> warnings) {

		final Long result;
		
		if (!erreurs.isEmpty()) {
			evenementCivilExterne.setEtat(EtatEvenementCivil.EN_ERREUR);
			evenementCivilExterne.setDateTraitement(DateHelper.getCurrentDate());
			Audit.error(evenementCivilExterne.getId(), "Status changé à ERREUR");
			result = null;
			dumpForDebug(erreurs);
		}
		else if (!warnings.isEmpty()) {
			evenementCivilExterne.setEtat(EtatEvenementCivil.A_VERIFIER);
			evenementCivilExterne.setDateTraitement(DateHelper.getCurrentDate());
			Audit.warn(evenementCivilExterne.getId(), "Status changé à A VERIFIER");
			result = evenementCivilExterne.getNumeroIndividuPrincipal();
			dumpForDebug(warnings);
		}
		else {
			evenementCivilExterne.setEtat(EtatEvenementCivil.TRAITE);
			evenementCivilExterne.setDateTraitement(DateHelper.getCurrentDate());
			Audit.success(evenementCivilExterne.getId(), "Status changé à TRAITE");
			result = evenementCivilExterne.getNumeroIndividuPrincipal();
		}

		for (EvenementCivilExterneErreur e : erreurs) {
			Audit.error(evenementCivilExterne.getId(), e.getMessage());
		}
		for (EvenementCivilExterneErreur w : warnings) {
			Audit.warn(evenementCivilExterne.getId(), w.getMessage());
		}

		evenementCivilExterne.addErrors(erreurs);
		evenementCivilExterne.addWarnings(warnings);

		return result;
	}

	private void traiteEvenement(final EvenementCivilExterne evenementCivilExterne, boolean refreshCache, final List<EvenementCivilExterneErreur> erreurs, final List<EvenementCivilExterneErreur> warnings) {
		try {
//			LOGGER.debug("evenementCivilData.getNumeroIndividuPrincipal()");

			// On complète l'événement à la volée (on est obligé de le faire ici dans tous les cas, car le tiers
			// correspondant peut avoir été créé entre la réception de l'événement et son re-traitement).
			final Long noIndPrinc = evenementCivilExterne.getNumeroIndividuPrincipal();
			Assert.notNull(noIndPrinc, "Le numéro d'individu de l'événement ne peut pas être nul");

			final Long principalID = tiersDAO.getNumeroPPByNumeroIndividu(noIndPrinc, true);
			evenementCivilExterne.setHabitantPrincipalId(principalID);

			final Long noIndConj = evenementCivilExterne.getNumeroIndividuConjoint();
			if (noIndConj != null) {
				final Long conjointID = tiersDAO.getNumeroPPByNumeroIndividu(noIndConj, true);
				evenementCivilExterne.setHabitantConjointId(conjointID);
			}

			final EvenementCivilOptions options = new EvenementCivilOptions(refreshCache);
			final EvenementCivilInterne event = evenementCivilTranslator.toInterne(evenementCivilExterne, options);

			/* 2.1 - lancement de la validation */
			event.checkCompleteness(erreurs, warnings);

			/* 2.2 - lancement de la validation */
			if (erreurs.isEmpty()) {
				event.validate(erreurs, warnings);
				/* 2.3 - lancement du traitement */
				if (erreurs.isEmpty()) {
					final Pair<PersonnePhysique, PersonnePhysique> nouveauxHabitants = event.handle(warnings);

					// adaptation des données dans l'événement civil en cas de création de nouveaux habitants
					if (nouveauxHabitants != null) {
						if (nouveauxHabitants.getFirst() != null && noIndPrinc != null && noIndPrinc > 0L) {
							if (evenementCivilExterne.getHabitantPrincipalId() == null) {
								evenementCivilExterne.setHabitantPrincipalId(nouveauxHabitants.getFirst().getId());
							}
							else {
								Assert.isEqual(evenementCivilExterne.getHabitantPrincipalId(), nouveauxHabitants.getFirst().getNumero());
							}
						}
						if (nouveauxHabitants.getSecond() != null && noIndConj != null && noIndConj > 0L) {
							if (evenementCivilExterne.getHabitantConjointId() == null) {
								evenementCivilExterne.setHabitantConjointId(nouveauxHabitants.getSecond().getId());
							}
							else {
								Assert.isEqual(evenementCivilExterne.getHabitantConjointId(), nouveauxHabitants.getSecond().getNumero());
							}
						}
					}
				}
				else {
					Audit.error(event.getNumeroEvenement(), "l'événement n'est pas valide");
				}
			}
			else {
				Audit.error(event.getNumeroEvenement(), "l'événement est incomplet");
			}
		}
		catch (EvenementCivilException e) {
			LOGGER.debug("Impossible d'adapter l'événement civil : " + evenementCivilExterne.getId(), e);
			erreurs.add(new EvenementCivilExterneErreur(e));
			Audit.error(evenementCivilExterne.getId(), e);
		}
	}

	@SuppressWarnings({"unchecked"})
	private void retraiteEvenementsEnErreurIndividu(final Long numIndividu, List<Long> evenementsExclus) {

		// 1 - Récupération des ids des événements civils en erreur de l'individu
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);
		final List<Long> ids = (List<Long>) template.execute(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				return evenementCivilExterneDAO.getIdsEvenementCivilsErreurIndividu(numIndividu);
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
						return evenementCivilExterneDAO.get(id).getNumeroIndividuPrincipal();
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

	private void dumpForDebug(List<EvenementCivilExterneErreur> erreurs) {

		boolean enabled = false;
		//enabled = true;
		//noinspection ConstantConditions
		if (enabled) {
			for (EvenementCivilExterneErreur e : erreurs) {
				LOGGER.debug("Erreur message: "+e);
			}
		}
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setEvenementCivilExterneDAO(EvenementCivilExterneDAO evenementCivilExterneDAO) {
		this.evenementCivilExterneDAO = evenementCivilExterneDAO;
	}

	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setServiceInfrastructureService(ServiceInfrastructureService serviceInfrastructureService) {
		this.serviceInfrastructureService = serviceInfrastructureService;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setEvenementCivilTranslator(EvenementCivilTranslator evenementCivilTranslator) {
		this.evenementCivilTranslator = evenementCivilTranslator;
	}
}
