package ch.vd.uniregctb.evenement.civil.engine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.registre.base.utils.Pair;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.common.CheckedTransactionCallback;
import ch.vd.uniregctb.common.CheckedTransactionTemplate;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilOptions;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterneDAO;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterneErreur;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.uniregctb.interfaces.model.Commune;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureException;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.TypeEvenementErreur;

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
	@Override
	@SuppressWarnings({"unchecked"})
	public void traiteEvenementsCivils(StatusManager status) {

		// Récupère les ids des événements à traiter
		TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);
		final List<Long> ids = template.execute(new TransactionCallback<List<Long>>() {
			@Override
			public List<Long> doInTransaction(TransactionStatus status) {
				return evenementCivilExterneDAO.getEvenementCivilsNonTraites();
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
	public void forceEvenementCivil(EvenementCivilExterne evenementCivilExterne) {
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
		try {
			// on ajoute le numéro de l'événement civil comme suffix à l'utilisateur principal, de manière à faciliter le tracing
			AuthenticationHelper.pushPrincipal(String.format("EvtCivil-%d", evenementCivilId));

			// Tout d'abord, on essaie de traiter l'événement
			result = template.execute(new CheckedTransactionCallback<Long>() {
				@Override
				public Long doInTransaction(TransactionStatus status) throws Exception {

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

					// Traitement de l'événement
					traiteEvenement(evenementCivilExterne, refreshCache, erreurs, warnings);

					return traiteErreurs(evenementCivilExterne, erreurs, warnings);
				}
			});
		}
		catch (final Exception e) {
			LOGGER.error("Erreur lors du traitement de l'événement : " + evenementCivilId, e);
			result = traiteRollbackSurException(evenementCivilId, e);
		}
		finally {
			AuthenticationHelper.popPrincipal();
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

				final EvenementCivilExterne evenementCivilExterne = evenementCivilExterneDAO.get(evenementCivilId);

				// [SIFISC-982] En cas d'erreur ayant provoqué le rollback de la transaction, on veut quand même garder (autant que possible) les liens vers
				// les personnes physiques. Pour cela, on va chercher on nouvelle fois ces derniers par le numéro d'individu.
				try {
					fillHabitants(evenementCivilExterne);
				}
				catch (Exception e1) {
					LOGGER.error("Impossible d'établir les liens vers les personnes physiques", e1);
				}

				final List<EvenementCivilExterneErreur> erreurs = new ArrayList<EvenementCivilExterneErreur>();
				final List<EvenementCivilExterneErreur> warnings = new ArrayList<EvenementCivilExterneErreur>();
				erreurs.add(new EvenementCivilExterneErreur(exception));

				evenementCivilExterne.getErreurs().clear();
				return traiteErreurs(evenementCivilExterne, erreurs, warnings);
			}
		});
	}

	/**
	 * Renseigne sur un événement les liens vers les habitants à partir des numéros d'individus.
	 *
	 * @param event un événement
	 */
	private void fillHabitants(EvenementCivilExterne event) {

		final Long noIndPrincipal = event.getNumeroIndividuPrincipal();
		if (noIndPrincipal != null) {
			final Long principalID = tiersDAO.getNumeroPPByNumeroIndividu(noIndPrincipal, true);
			event.setHabitantPrincipalId(principalID);
		}

		final Long noIndConj = event.getNumeroIndividuConjoint();
		if (noIndConj != null) {
			final Long conjointID = tiersDAO.getNumeroPPByNumeroIndividu(noIndConj, true);
			event.setHabitantConjointId(conjointID);
		}
	}

	/**
	 * Vérifie qu'un événement possède bien les données minimales attendues sur celui-ci
	 * @param event un événement
	 */
	private void assertEvenement(EvenementCivilExterne event) {

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

	private Long traiteErreurs(EvenementCivilExterne evenementCivilExterne, List<EvenementCivilExterneErreur> errorList, List<EvenementCivilExterneErreur> warningList) {

		final List<EvenementCivilExterneErreur> erreurs = eliminerDoublons(errorList);
		final List<EvenementCivilExterneErreur> warnings = eliminerDoublons(warningList);

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

	private static class EvenementCivilExterneErreurKey {
		private final Long id;
		private final String message;
		private final TypeEvenementErreur type;
		private final String callstack;

		private EvenementCivilExterneErreurKey(EvenementCivilExterneErreur erreur) {
			this.id = erreur.getId();
			this.message = erreur.getMessage();
			this.type = erreur.getType();
			this.callstack = erreur.getCallstack();
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			final EvenementCivilExterneErreurKey that = (EvenementCivilExterneErreurKey) o;

			if (id != null ? !id.equals(that.id) : that.id != null) return false;
			if (type != that.type) return false;
			if (message != null ? !message.equals(that.message) : that.message != null) return false;
			if (callstack != null ? !callstack.equals(that.callstack) : that.callstack != null) return false;

			return true;
		}

		@Override
		public int hashCode() {
			int result = id != null ? id.hashCode() : 0;
			result = 31 * result + (message != null ? message.hashCode() : 0);
			result = 31 * result + (type != null ? type.hashCode() : 0);
			result = 31 * result + (callstack != null ? callstack.hashCode() : 0);
			return result;
		}
	}

	private static List<EvenementCivilExterneErreur> eliminerDoublons(List<EvenementCivilExterneErreur> source) {
		if (source == null || source.size() < 2) {
			return source;
		}
		final Map<EvenementCivilExterneErreurKey, EvenementCivilExterneErreur> map = new LinkedHashMap<EvenementCivilExterneErreurKey, EvenementCivilExterneErreur>(source.size());
		for (EvenementCivilExterneErreur src : source) {
			final EvenementCivilExterneErreurKey key = new EvenementCivilExterneErreurKey(src);
			if (!map.containsKey(key)) {
				map.put(key, src);
			}
		}
		if (map.size() < source.size()) {
			return new ArrayList<EvenementCivilExterneErreur>(map.values());
		}
		else {
			return source;
		}
	}

	private EvenementCivilInterne buildInterne(EvenementCivilExterne evenementCivilExterne, boolean refreshCache) throws EvenementCivilException {
		assertEvenement(evenementCivilExterne);

		// On complète l'événement à la volée (on est obligé de le faire ici dans tous les cas, car le tiers
		// correspondant peut avoir été créé entre la réception de l'événement et son re-traitement).
		fillHabitants(evenementCivilExterne);

		// On converti l'événement externe en événement interne, qui contient tout l'information nécessaire à l'exécution de l'événement.
		final EvenementCivilOptions options = new EvenementCivilOptions(refreshCache);
		return evenementCivilTranslator.toInterne(evenementCivilExterne, options);
	}

	private void traiteEvenement(final EvenementCivilExterne evenementCivilExterne, boolean refreshCache, final List<EvenementCivilExterneErreur> erreurs,
	                             final List<EvenementCivilExterneErreur> warnings) throws EvenementCivilException {

		final String message = String.format("Début du traitement de l'événement civil %d de type %s", evenementCivilExterne.getId(), evenementCivilExterne.getType());
		Audit.info(evenementCivilExterne.getId(), message);

		// On converti l'événement externe en événement interne, qui contient tout l'information nécessaire à l'exécution de l'événement.
		final EvenementCivilInterne event = buildInterne(evenementCivilExterne, refreshCache);

		final Long noIndPrinc = evenementCivilExterne.getNumeroIndividuPrincipal();
		final Long noIndConj = evenementCivilExterne.getNumeroIndividuConjoint();

		// 2.1 - lancement de la validation
		event.checkCompleteness(erreurs, warnings);
		if (!erreurs.isEmpty()) {
			Audit.error(event.getNumeroEvenement(), "l'événement est incomplet");
			return;
		}

		// 2.2 - lancement de la validation
		event.validate(erreurs, warnings);
		if (!erreurs.isEmpty()) {
			Audit.error(event.getNumeroEvenement(), "l'événement n'est pas valide");
			return;
		}

		// 2.3 - lancement du traitement
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

	@SuppressWarnings({"unchecked"})
	private void retraiteEvenementsEnErreurIndividu(final Long numIndividu, List<Long> evenementsExclus) {

		// 1 - Récupération des ids des événements civils en erreur de l'individu
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);
		final List<Long> ids = template.execute(new TransactionCallback<List<Long>>() {
			@Override
			public List<Long> doInTransaction(TransactionStatus status) {
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
