package ch.vd.uniregctb.migration.pm.engine;

import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;

import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.migration.pm.Graphe;
import ch.vd.uniregctb.migration.pm.MigrationResultContextManipulation;
import ch.vd.uniregctb.migration.pm.MigrationResultMessageProvider;
import ch.vd.uniregctb.migration.pm.engine.collector.EntityLinkCollector;
import ch.vd.uniregctb.migration.pm.log.LogCategory;
import ch.vd.uniregctb.migration.pm.log.LogLevel;
import ch.vd.uniregctb.migration.pm.log.LoggedElementRenderer;
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

	private static final String VISA_MIGRATION = "[MigrationPM]";

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
	public MigrationResultMessageProvider migrate(Graphe graphe) throws MigrationException {
		final MigrationResult mr = new MigrationResult(graphe);

		// on crée un mapper d'ID dont la référence est le mapper global, en prenant soin de transvaser les nouveaux mappings dans le mapper global après la fin de la transaction
		final IdMapper localIdMapper = idMapper.withReference();
		mr.addPostTransactionCallback(localIdMapper::pushToReference);

		// initialisation des structures de resultats
		entrepriseMigrator.initMigrationResult(mr);
		etablissementMigrator.initMigrationResult(mr);
		individuMigrator.initMigrationResult(mr);

		// tout le graphe sera migré dans une transaction globale
		AuthenticationHelper.pushPrincipal(VISA_MIGRATION);
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
		catch (Exception e) {
			// un petit log pour voir l'avancement des travaux avant l'explosion (= les messages collectés jusque là).
			final String summary = Stream.of(LogCategory.values())
					.map(cat -> Pair.of(cat, mr.getMessages(cat)))
					.filter(pair -> pair.getRight() != null && !pair.getRight().isEmpty())
					.map(pair -> Pair.of(pair.getLeft(), pair.getRight().stream().map(LoggedElementRenderer.INSTANCE::toString).collect(Collectors.joining("\n- ", "- ", StringUtils.EMPTY))))
					.map(pair -> String.format("Catégorie %s :\n%s", pair.getLeft(), pair.getRight()))
					.collect(Collectors.joining("\n"));
			LOGGER.error(String.format("Exception lancée dans le traitement du graphe %s\nMessages collectés jusque là :\n%s", graphe, summary), e);
			throw e;
		}
		finally {
			AuthenticationHelper.popPrincipal();
		}

		return mr;
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
		addLinks(linkCollector.getCollectedLinks());
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

	private void addLinks(Collection<EntityLinkCollector.EntityLink> links) {
		if (links != null && !links.isEmpty()) {

			// on désactive temporairement la validation (elle sera ré-activée une fois que TOUS les liens
			// auront été générés, pour éviter de sauter sur les états incohérents intermédiaires)
			// -> en contrepartie, c'est seulement le flush final de la transaction qui détectera les éventuels problèmes

			final boolean wasEnabled = validationInterceptor.isEnabled();
			validationInterceptor.setEnabled(false);
			try {
				links.stream()
						.map(EntityLinkCollector.EntityLink::toRapportEntreTiers)
						.map(uniregStore::saveEntityToDb)
						.forEach(this::propagateRapportEntreTiers);
			}
			finally {
				validationInterceptor.setEnabled(wasEnabled);
			}
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
