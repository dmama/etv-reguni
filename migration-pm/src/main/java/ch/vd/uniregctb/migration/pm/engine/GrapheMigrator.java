package ch.vd.uniregctb.migration.pm.engine;

import java.util.Collection;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;

import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.migration.pm.Graphe;
import ch.vd.uniregctb.migration.pm.MigrationConstants;
import ch.vd.uniregctb.migration.pm.MigrationResultContextManipulation;
import ch.vd.uniregctb.migration.pm.MigrationResultMessageProvider;
import ch.vd.uniregctb.migration.pm.engine.collector.EntityLinkCollector;
import ch.vd.uniregctb.migration.pm.engine.collector.NeutralizedLinkAction;
import ch.vd.uniregctb.migration.pm.log.LogCategory;
import ch.vd.uniregctb.migration.pm.log.LogLevel;
import ch.vd.uniregctb.migration.pm.log.LoggedMessages;
import ch.vd.uniregctb.migration.pm.log.RapportEntreTiersLoggedElement;
import ch.vd.uniregctb.migration.pm.mapping.IdMapper;
import ch.vd.uniregctb.migration.pm.mapping.IdMapping;
import ch.vd.uniregctb.migration.pm.regpm.RegpmEntreprise;
import ch.vd.uniregctb.migration.pm.regpm.RegpmEtablissement;
import ch.vd.uniregctb.migration.pm.regpm.RegpmIndividu;
import ch.vd.uniregctb.migration.pm.store.UniregStore;
import ch.vd.uniregctb.migration.pm.utils.ValidationInterceptor;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.transaction.TransactionTemplate;

public class GrapheMigrator implements InitializingBean {

	private static final Logger LOGGER = LoggerFactory.getLogger(GrapheMigrator.class);

	private PlatformTransactionManager uniregTransactionManager;
	private UniregStore uniregStore;
	private ValidationInterceptor validationInterceptor;

	private IdMapper idMapper;

	private EntityMigrator<RegpmEntreprise> entrepriseMigrator;
	private EntityMigrator<RegpmEtablissement> etablissementMigrator;
	private EntityMigrator<RegpmIndividu> individuMigrator;

	public void setUniregTransactionManager(PlatformTransactionManager uniregTransactionManager) {
		this.uniregTransactionManager = uniregTransactionManager;
	}

	public void setUniregStore(UniregStore uniregStore) {
		this.uniregStore = uniregStore;
	}

	public void setEntrepriseMigrator(EntityMigrator<RegpmEntreprise> entrepriseMigrator) {
		this.entrepriseMigrator = entrepriseMigrator;
	}

	public void setEtablissementMigrator(EntityMigrator<RegpmEtablissement> etablissementMigrator) {
		this.etablissementMigrator = etablissementMigrator;
	}

	public void setIndividuMigrator(EntityMigrator<RegpmIndividu> individuMigrator) {
		this.individuMigrator = individuMigrator;
	}

	public void setValidationInterceptor(ValidationInterceptor validationInterceptor) {
		this.validationInterceptor = validationInterceptor;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		// chargement du mapper d'identifiants, non-vide lors d'une reprise sur incident
		final TransactionTemplate template = new TransactionTemplate(uniregTransactionManager);
		template.setReadOnly(true);
		this.idMapper = template.execute(status -> IdMapper.fromDatabase(uniregStore));
	}

	private static String dump(Throwable t) {
		return t.getClass().getName() + ": " + t.getMessage() + ExceptionUtils.getStackTrace(t);
	}

	/**
	 * Point d'entrée de la migration d'un graphe d'entités RegPM
	 * @param graphe le graphe en question
	 * @return des messages à logguer
	 * @throws MigrationException en cas de gros souci
	 */
	public LoggedMessages migrate(Graphe graphe) throws MigrationException {
		final MigrationResult mr = new MigrationResult(graphe);

		// on crée un mapper d'ID dont la référence est le mapper global, en prenant soin de transvaser les nouveaux mappings dans le mapper global après la fin de la transaction
		final IdMapper localIdMapper = idMapper.withReference();
		mr.addPostTransactionCallback(localIdMapper::pushToReference);

		// initialisation des structures de resultats
		entrepriseMigrator.initMigrationResult(mr, localIdMapper);
		etablissementMigrator.initMigrationResult(mr, localIdMapper);
		individuMigrator.initMigrationResult(mr, localIdMapper);

		// tout le graphe sera migré dans une transaction globale
		AuthenticationHelper.pushPrincipal(MigrationConstants.VISA_MIGRATION);
		try {
			final TransactionTemplate template = new TransactionTemplate(uniregTransactionManager);
			template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
			template.execute(status -> {
				// migration des entités et de leurs liens
				doMigrate(graphe, mr, localIdMapper);

				// consolidations de données en fin de transaction
				mr.consolidatePreTransactionCommitRegistrations();

				// publication des nouveaux mappings vers la DB
				localIdMapper.pushLocalPartToDatabase(uniregStore);

				return null;
			});

			// une fois la transaction terminée, on passe les callbacks enregistrés
			try {
				mr.runPostTransactionCallbacks();
			}
			catch (Exception e) {
				mr.addMessage(LogCategory.EXCEPTIONS, LogLevel.WARN, String.format("Exception levée lors de l'exécution des callbacks post-transaction : %s", dump(e)));
			}
		}
		catch (OptimisticLockingFailureException e) {
			// aha... un problème de verrou optimiste... vu une fois quand les deux individus de RegPM 518417 (Thomas Vonäsch) et 5005 (Thomas Vonaesch)
			// ont été traités en même temps dans deux threads concurrents... dans les deux cas (soit directement par le numéro RCPers 518417, soit
			// indirectement par le nom car le numéro ne correspond plus à rien), c'est la même personne physique qui a été identifiée (103.522.85),
			// d'où le souci...
			LOGGER.warn("Détecté un problème de verrou optimiste... on ré-essaie.", e);

			// on va juste ré-essayer... (ce devrait être tellement rare que la récursivité ne devrait pas gêner...)
			return migrate(graphe);
		}
		catch (Exception e) {
			final MigrationException me = new MigrationException(e, graphe, resolveInReadOnlyTransaction(mr));
			LOGGER.error("", me);
			throw me;
		}
		finally {
			AuthenticationHelper.popPrincipal();
		}

		return resolveInReadOnlyTransaction(mr);
	}

	/**
	 * @param mr un container de messages
	 * @return les messages sous leur forme résolue (nécessite l'ouverture d'une transaction read-only à la base de données Unireg)
	 */
	private LoggedMessages resolveInReadOnlyTransaction(MigrationResultMessageProvider mr) {
		// on expose les messages récoltés depuis l'intérieur d'une transaction read-only
		// (car certains suppliers ont besoin d'une connexion à la base de données...)
		final TransactionTemplate roTemplate = new TransactionTemplate(uniregTransactionManager);
		roTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		roTemplate.setReadOnly(true);
		return roTemplate.execute(status -> {
			status.setRollbackOnly();
			return LoggedMessages.resolutionOf(mr);
		});
	}

	/**
	 * Appelé dans un contexte transactionnel, pour effectuer la migration des entités et des liens qui les unissent
	 * @param graphe le graphe d'objets à migrer
	 * @param mr le collecteur de résultats/remarques de migration
	 * @param idMapper mapper des identifiants des entités migrées
	 */
	private void doMigrate(Graphe graphe, MigrationResultContextManipulation mr, IdMapping idMapper) {

		// on commence par les établissements, puis les entreprises, puis les individus (au final, je ne crois pas que l'ordre soit réellement important...)
		// on collecte les liens entre ces entités au fur et à mesure
		// à la fin, on ajoute les liens

		final EntityLinkCollector linkCollector = new EntityLinkCollector();
		doMigrateEtablissements(graphe.getEtablissements().values(), mr, linkCollector, idMapper);
		doMigrateEntreprises(graphe.getEntreprises().values(), mr, linkCollector, idMapper);
		doMigrateIndividus(graphe.getIndividus().values(), mr, linkCollector, idMapper);

		final List<Pair<EntityLinkCollector.NeutralizationReason, EntityLinkCollector.EntityLink>> neutralizedLinks = linkCollector.getNeutralizedLinks();
		addLinks(linkCollector.getCollectedLinks(), neutralizedLinks, mr);
		neutralizedLinks.forEach(pair -> {
			final EntityLinkCollector.NeutralizationReason reason = pair.getLeft();
			final EntityLinkCollector.EntityLink link = pair.getRight();
			final List<NeutralizedLinkAction> actions = linkCollector.getNeutralizedLinkActions(link);
			actions.forEach(action -> action.execute(link, reason, mr, idMapper));
		});
	}

	private void doMigrateEntreprises(Collection<RegpmEntreprise> entreprises, MigrationResultContextManipulation mr, EntityLinkCollector linkCollector, IdMapping idMapper) {
		entreprises.forEach(e -> entrepriseMigrator.migrate(e, mr, linkCollector, idMapper));
	}

	private void doMigrateEtablissements(Collection<RegpmEtablissement> etablissements, MigrationResultContextManipulation mr, EntityLinkCollector linkCollector, IdMapping idMapper) {
		etablissements.forEach(e -> etablissementMigrator.migrate(e, mr, linkCollector, idMapper));
	}

	private void doMigrateIndividus(Collection<RegpmIndividu> individus, MigrationResultContextManipulation mr, EntityLinkCollector linkCollector, IdMapping idMapper) {
		individus.forEach(i -> individuMigrator.migrate(i, mr, linkCollector, idMapper));
	}

	private void addLinks(Collection<EntityLinkCollector.EntityLink> collectedLinks,
	                      Collection<Pair<EntityLinkCollector.NeutralizationReason, EntityLinkCollector.EntityLink>> neutralizedLinks,
	                      MigrationResultContextManipulation mr) {

		if (collectedLinks != null && !collectedLinks.isEmpty()) {

			collectedLinks.forEach(link -> logCreationRapportEntreTiers(link, mr));

			// on désactive temporairement la validation (elle sera ré-activée une fois que TOUS les liens
			// auront été générés, pour éviter de sauter sur les états incohérents intermédiaires)
			// -> en contrepartie, c'est seulement le flush final de la transaction qui détectera les éventuels problèmes

			final boolean wasEnabled = validationInterceptor.isEnabled();
			validationInterceptor.setEnabled(false);
			try {
				collectedLinks.stream()
						.map(EntityLinkCollector.EntityLink::toRapportEntreTiers)
						.map(uniregStore::saveEntityToDb)
						.forEach(this::propagateRapportEntreTiers);
			}
			finally {
				validationInterceptor.setEnabled(wasEnabled);
			}
		}

		// log des liens qui n'ont finalement pas été créés car un des participants au moins n'a pas rempli les critères de migration
		if (neutralizedLinks != null && !neutralizedLinks.isEmpty()) {
			neutralizedLinks.forEach(pair -> logAbandonRapportEntreTiers(pair.getLeft(), pair.getRight(), mr));
		}
	}

	/**
	 * Génère un log dans la liste des rapports entre tiers générés
	 * @param link le lien (source du rapport entre tiers)
	 * @param mr le collecteur de résultats/remarques de migration et manipulateur de contexte de log
	 * @param <S> type de tiers source dans le lien
	 * @param <D> type de tiers destination dans le lien
	 * @param <R> type du rapport entre tiers généré par le lien
	 */
	private <S extends Tiers, D extends Tiers, R extends RapportEntreTiers> void logCreationRapportEntreTiers(EntityLinkCollector.EntityLink<S, D, R> link,
	                                                                                                          MigrationResultContextManipulation mr) {
		mr.pushContextValue(RapportEntreTiersLoggedElement.class, new RapportEntreTiersLoggedElement<>(link));
		try {
			mr.addMessage(LogCategory.RAPPORTS_ENTRE_TIERS, LogLevel.INFO, StringUtils.EMPTY);
		}
		finally {
			mr.popContexteValue(RapportEntreTiersLoggedElement.class);
		}
	}

	/**
	 * Génère un log dans la liste des rapports entre tiers finalement abandonnés
	 * @param link le lien (source du rapport entre tiers)
	 * @param mr le collecteur de résultats/remarques de migration et manipulateur de contexte de log
	 * @param <S> type de tiers source dans le lien
	 * @param <D> type de tiers destination dans le lien
	 * @param <R> type du rapport entre tiers généré par le lien
	 */
	private <S extends Tiers, D extends Tiers, R extends RapportEntreTiers> void logAbandonRapportEntreTiers(EntityLinkCollector.NeutralizationReason reason,
	                                                                                                         EntityLinkCollector.EntityLink<S, D, R> link,
	                                                                                                         MigrationResultContextManipulation mr) {
		mr.pushContextValue(RapportEntreTiersLoggedElement.class, new RapportEntreTiersLoggedElement<>(link, !reason.isSourceNeutralisee(), !reason.isDestinationNeutralisee()));
		try {
			mr.addMessage(LogCategory.RAPPORTS_ENTRE_TIERS, LogLevel.ERROR, "Lien non généré car l'une des parties au moins a finalement été exclue de la migration.");
		}
		finally {
			mr.popContexteValue(RapportEntreTiersLoggedElement.class);
		}
	}

	/**
	 * Remplissage des collections de rapports entre tiers de part et d'autre (afin que les éventuels contrôles de validation
	 * prennent bien en compte les nouveaux liens)
	 * @param ret le rapport à propager
	 */
	private void propagateRapportEntreTiers(RapportEntreTiers ret) {
		final Tiers object = uniregStore.getEntityFromDb(Tiers.class, ret.getObjetId());
		if (object == null) {
			throw new IllegalArgumentException("Aucun tiers connu avec l'identifiant " + ret.getObjetId() + " pourtant utilisé comme objet dans le rapport entre tiers " + ret);
		}
		object.addRapportObjet(ret);

		final Tiers sujet = uniregStore.getEntityFromDb(Tiers.class, ret.getSujetId());
		if (sujet == null) {
			throw new IllegalArgumentException("Aucun tiers connu avec l'identifiant " + ret.getSujetId() + " pourtant utilisé comme sujet dans le rapport entre tiers " + ret);
		}
		sujet.addRapportSujet(ret);
	}
}
