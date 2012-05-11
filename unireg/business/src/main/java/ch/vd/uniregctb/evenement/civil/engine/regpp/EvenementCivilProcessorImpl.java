package ch.vd.uniregctb.evenement.civil.engine.regpp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureException;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.common.CheckedTransactionCallback;
import ch.vd.uniregctb.common.CheckedTransactionTemplate;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.evenement.civil.EvenementCivilErreurCollector;
import ch.vd.uniregctb.evenement.civil.EvenementCivilHelper;
import ch.vd.uniregctb.evenement.civil.EvenementCivilMessageCollector;
import ch.vd.uniregctb.evenement.civil.EvenementCivilWarningCollector;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilOptions;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.uniregctb.evenement.civil.regpp.EvenementCivilRegPP;
import ch.vd.uniregctb.evenement.civil.regpp.EvenementCivilRegPPDAO;
import ch.vd.uniregctb.evenement.civil.regpp.EvenementCivilRegPPErreur;
import ch.vd.uniregctb.evenement.civil.regpp.EvenementCivilRegPPErreurFactory;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.type.EtatEvenementCivil;

/**
 * Moteur de règle permettant d'appliquer les règles métiers. Le moteur contient une liste de EvenementCivilTranslationStrategy capables de gérer les
 * événements en entrée. Pour chaque événement recu, il invoque les EvenementCivilTranslationStrategy capables de gérer cet événements.
 *
 * @author Akram BEN AISSI <mailto:akram.ben-aissi@vd.ch>
 */
public class EvenementCivilProcessorImpl implements EvenementCivilProcessor {

	private static final Logger LOGGER = Logger.getLogger(EvenementCivilProcessorImpl.class);
	private static final Logger EVT_INTERNE_LOGGER = Logger.getLogger(EvenementCivilInterne.class);

	private static final EvenementCivilRegPPErreurFactory ERREUR_FACTORY = new EvenementCivilRegPPErreurFactory();

	private PlatformTransactionManager transactionManager;
	private ServiceInfrastructureService serviceInfrastructureService;
	private EvenementCivilRegPPDAO evenementCivilRegPPDAO;
	private EvenementCivilTranslator evenementCivilTranslator;
	private ServiceCivilService serviceCivil;

	/**
	 * {@inheritDoc}
	 */
	@Override
	@SuppressWarnings({"unchecked"})
	public void traiteEvenementsCivils(StatusManager status) {

		// Récupère les ids des événements à traiter
		TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);
		final List<Long> ids = template.execute(new TransactionCallback<List<Long>>() {
			@Override
			public List<Long> doInTransaction(TransactionStatus status) {
				return evenementCivilRegPPDAO.getEvenementCivilsNonTraites();
			}
		});

		traiteEvenements(ids, false, false, status);
	}

	@Override
	public void traiteEvenementCivil(final Long evenementCivilId) {
		traiteEvenements(Arrays.asList(evenementCivilId), true, true, null);
	}

	@Override
	public void recycleEvenementCivil(final Long evenementCivilId) {
		traiteEvenements(Arrays.asList(evenementCivilId), false, true, null);
	}

	@Override
	public void forceEvenementCivil(EvenementCivilRegPP evenementCivilExterne) {
		evenementCivilExterne.setEtat(EtatEvenementCivil.FORCE);
		try {
			buildInterne(evenementCivilExterne, true);
		}
		catch (EvenementCivilException e) {
			// tant pis, on aura au moins essayé...
			LOGGER.warn(String.format("Impossible de rafraîchir le cache civil relatif à l'événement civil %d", evenementCivilExterne.getId()), e);
		}
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

		final CheckedTransactionTemplate template = new CheckedTransactionTemplate(transactionManager);
		template.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);

		// on ajoute le numéro de l'événement civil comme suffix à l'utilisateur principal, de manière à faciliter le tracing
		AuthenticationHelper.pushPrincipal(String.format("EvtCivil-%d", evenementCivilId));
		try {
			serviceCivil.setIndividuLogger(EVT_INTERNE_LOGGER.isTraceEnabled());

			// Tout d'abord, on essaie de traiter l'événement
			result = template.execute(new CheckedTransactionCallback<Long>() {
				@Override
				public Long doInTransaction(TransactionStatus status) throws Exception {

					final EvenementCivilMessageCollector<EvenementCivilRegPPErreur> collector = new EvenementCivilMessageCollector<EvenementCivilRegPPErreur>(ERREUR_FACTORY);

					// Charge l'événement
					final EvenementCivilRegPP evenementCivilExterne = evenementCivilRegPPDAO.get(evenementCivilId);
					Assert.notNull(evenementCivilExterne, "l'événement est null");

					if (evenementCivilExterne.getEtat().isTraite()) {
						LOGGER.warn("Tentative de traitement de l'événement n°" + evenementCivilId + " qui est déjà traité. Aucune opération effectuée.");
						return evenementCivilExterne.getNumeroIndividuPrincipal();
					}

					// On enlève les erreurs précédentes
					evenementCivilExterne.getErreurs().clear();

					// Traitement de l'événement
					final EtatEvenementCivil etat = traiteEvenement(evenementCivilExterne, refreshCache, collector, collector);

					return traiteErreurs(etat, evenementCivilExterne, collector.getErreurs(), collector.getWarnings());
				}
			});
		}
		catch (final Exception e) {
			LOGGER.error("Erreur lors du traitement de l'événement : " + evenementCivilId, e);
			result = traiteRollbackSurException(evenementCivilId, e);
		}
		finally {
			AuthenticationHelper.popPrincipal();
			serviceCivil.setIndividuLogger(false);
		}

		return result;
	}

	/**
	 * En cas d'exception, on met-à-jour la liste d'erreur pour l'événement
	 *
	 * @param evenementCivilId l'id d'un événement civil
	 * @param exception        l'exception qui a provoqué le rollback
	 * @return le numéro d'individu principal concerné par l'événement civil
	 */
	private Long traiteRollbackSurException(final Long evenementCivilId, final Exception exception) {

		final TransactionTemplate t = new TransactionTemplate(transactionManager);
		t.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);

		return t.execute(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {

				final EvenementCivilRegPP evenementCivilExterne = evenementCivilRegPPDAO.get(evenementCivilId);
				final List<EvenementCivilRegPPErreur> erreurs = new ArrayList<EvenementCivilRegPPErreur>();
				final List<EvenementCivilRegPPErreur> warnings = new ArrayList<EvenementCivilRegPPErreur>();
				erreurs.add(new EvenementCivilRegPPErreur(exception));

				evenementCivilExterne.getErreurs().clear();
				return traiteErreurs(EtatEvenementCivil.EN_ERREUR, evenementCivilExterne, erreurs, warnings);
			}
		});
	}

	/**
	 * Vérifie qu'un événement possède bien les données minimales attendues sur celui-ci
	 * @param event un événement
	 */
	private void assertEvenement(EvenementCivilRegPP event) {

		Assert.notNull(event.getType(), "le type de l'événement n'est pas renseigné");
		Assert.notNull(event.getDateEvenement(), "La date de l'événement n'est pas renseigné");
		Assert.notNull(event.getNumeroOfsCommuneAnnonce(), "Le numero de la commune d'annonce n'est pas renseigné");

		// Controle la commune OFS
		int numeroOFS = event.getNumeroOfsCommuneAnnonce();
		if (numeroOFS != 0) {
			Commune c;
			try {
				c = serviceInfrastructureService.getCommuneByNumeroOfsEtendu(numeroOFS, event.getDateEvenement());
			}
			catch (ServiceInfrastructureException e) {
				LOGGER.error(e, e);
				c = null;
			}
			if (c == null) {
				// Commune introuvable => Exception
				throw new IllegalArgumentException("La commune avec le numéro OFS " + numeroOFS + " n'existe pas");
			}
		}

		Assert.notNull(event.getNumeroIndividuPrincipal(), "Le numéro d'individu de l'événement ne peut pas être nul");
	}

	private Long traiteErreurs(EtatEvenementCivil etat, EvenementCivilRegPP evenementCivilExterne, List<EvenementCivilRegPPErreur> errorList, List<EvenementCivilRegPPErreur> warningList) {

		final List<EvenementCivilRegPPErreur> erreurs = EvenementCivilHelper.eliminerDoublons(errorList);
		final List<EvenementCivilRegPPErreur> warnings = EvenementCivilHelper.eliminerDoublons(warningList);

		final Long result;
		
		if (!erreurs.isEmpty() || etat == EtatEvenementCivil.EN_ERREUR) {
			evenementCivilExterne.setEtat(EtatEvenementCivil.EN_ERREUR);
			evenementCivilExterne.setDateTraitement(DateHelper.getCurrentDate());
			Audit.error(evenementCivilExterne.getId(), "Status changé à ERREUR");
			result = null;
			dumpForDebug(erreurs);
		}
		else if (!warnings.isEmpty() || etat == EtatEvenementCivil.A_VERIFIER) {
			evenementCivilExterne.setEtat(EtatEvenementCivil.A_VERIFIER);
			evenementCivilExterne.setDateTraitement(DateHelper.getCurrentDate());
			Audit.warn(evenementCivilExterne.getId(), "Status changé à A VERIFIER");
			result = evenementCivilExterne.getNumeroIndividuPrincipal();
			dumpForDebug(warnings);
		}
		else {
			evenementCivilExterne.setEtat(etat);
			evenementCivilExterne.setDateTraitement(DateHelper.getCurrentDate());
			Audit.success(evenementCivilExterne.getId(), "Status changé à " + etat.name() + "");
			result = evenementCivilExterne.getNumeroIndividuPrincipal();
		}

		for (EvenementCivilRegPPErreur e : erreurs) {
			Audit.error(evenementCivilExterne.getId(), e.getMessage());
		}
		for (EvenementCivilRegPPErreur w : warnings) {
			Audit.warn(evenementCivilExterne.getId(), w.getMessage());
		}

		evenementCivilExterne.addErrors(erreurs);
		evenementCivilExterne.addWarnings(warnings);

		return result;
	}

	private EvenementCivilInterne buildInterne(EvenementCivilRegPP evenementCivilExterne, boolean refreshCache) throws EvenementCivilException {
		assertEvenement(evenementCivilExterne);
		// On converti l'événement externe en événement interne, qui contient tout l'information nécessaire à l'exécution de l'événement.
		final EvenementCivilOptions options = new EvenementCivilOptions(refreshCache);
		return evenementCivilTranslator.toInterne(evenementCivilExterne, options);
	}

	private EtatEvenementCivil traiteEvenement(final EvenementCivilRegPP evenementCivilExterne, boolean refreshCache, final EvenementCivilErreurCollector erreurs,
	                                           final EvenementCivilWarningCollector warnings) throws EvenementCivilException {

		final String message = String.format("Début du traitement de l'événement civil %d de type %s", evenementCivilExterne.getId(), evenementCivilExterne.getType());
		Audit.info(evenementCivilExterne.getId(), message);

		// On converti l'événement externe en événement interne, qui contient tout l'information nécessaire à l'exécution de l'événement.
		final EvenementCivilInterne event = buildInterne(evenementCivilExterne, refreshCache);

		// 2.2 - lancement de la validation
		event.validate(erreurs, warnings);
		if (erreurs.hasErreurs()) {
			Audit.error(event.getNumeroEvenement(), "L'événement n'est pas valide");
			return EtatEvenementCivil.EN_ERREUR;
		}

		// 2.3 - lancement du traitement
		return event.handle(warnings).toEtat();
	}

	@SuppressWarnings({"unchecked"})
	private void retraiteEvenementsEnErreurIndividu(final Long numIndividu, List<Long> evenementsExclus) {

		// 1 - Récupération des ids des événements civils en erreur de l'individu
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);
		final List<Long> ids = template.execute(new TransactionCallback<List<Long>>() {
			@Override
			public List<Long> doInTransaction(TransactionStatus status) {
				return evenementCivilRegPPDAO.getIdsEvenementCivilsErreurIndividu(numIndividu);
			}
		});

		/* 2 - Iteration sur les ids des événements civils */
		final List<Long> aRelancer = new ArrayList<Long>(ids);
		aRelancer.removeAll(evenementsExclus);
		if (!aRelancer.isEmpty()) {
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
	private void traiteEvenements(final List<Long> ids, boolean forceRecyclage, boolean refreshCache, @Nullable StatusManager status) {
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
				final Long numIndividu = template.execute(new TransactionCallback<Long>() {
					@Override
					public Long doInTransaction(TransactionStatus status) {
						return evenementCivilRegPPDAO.get(id).getNumeroIndividuPrincipal();
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

	private void dumpForDebug(List<EvenementCivilRegPPErreur> erreurs) {

		boolean enabled = false;
		//enabled = true;
		//noinspection ConstantConditions
		if (enabled) {
			for (EvenementCivilRegPPErreur e : erreurs) {
				LOGGER.debug("Erreur message: "+e);
			}
		}
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setEvenementCivilRegPPDAO(EvenementCivilRegPPDAO evenementCivilRegPPDAO) {
		this.evenementCivilRegPPDAO = evenementCivilRegPPDAO;
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

	public void setServiceCivil(ServiceCivilService serviceCivil) {
		this.serviceCivil = serviceCivil;
	}
}
