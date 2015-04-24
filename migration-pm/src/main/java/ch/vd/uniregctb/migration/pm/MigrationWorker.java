package ch.vd.uniregctb.migration.pm;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;

import ch.vd.unireg.wsclient.rcent.RcEntClient;
import ch.vd.unireg.wsclient.rcpers.RcPersClient;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.common.DefaultThreadFactory;
import ch.vd.uniregctb.common.DefaultThreadNameGenerator;
import ch.vd.uniregctb.migration.pm.adresse.StreetDataMigrator;
import ch.vd.uniregctb.migration.pm.indexeur.NonHabitantIndex;
import ch.vd.uniregctb.migration.pm.regpm.RegpmEntreprise;
import ch.vd.uniregctb.migration.pm.regpm.RegpmEtablissement;
import ch.vd.uniregctb.migration.pm.regpm.RegpmIndividu;
import ch.vd.uniregctb.migration.pm.utils.EntityLinkCollector;
import ch.vd.uniregctb.migration.pm.utils.EntityMigrationSynchronizer;
import ch.vd.uniregctb.migration.pm.utils.IdMapper;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.transaction.TransactionTemplate;

public class MigrationWorker implements Worker, InitializingBean, DisposableBean {

	private static final Logger LOGGER = LoggerFactory.getLogger(MigrationWorker.class);

	private static final RejectedExecutionHandler ABORT_POLICY = new ThreadPoolExecutor.AbortPolicy();

	private static final String VISA_MIGRATION = "[MigrationPM]";
	private final AtomicInteger nbEnCours = new AtomicInteger(0);
	private final EntityMigrationSynchronizer synchronizer = new EntityMigrationSynchronizer();
	private ExecutorService executor;
	private CompletionService<MigrationResultMessageProvider> completionService;
	private Thread gatheringThread;
	private volatile boolean started;
	private MigrationMode mode;

	private StreetDataMigrator streetDataMigrator;
	private PlatformTransactionManager uniregTransactionManager;
	private SessionFactory uniregSessionFactory;
	private RcPersClient rcpersClient;
	private RcEntClient rcentClient;
	private TiersDAO tiersDAO;
	private NonHabitantIndex nonHabitantIndex;

	private EntityMigrator<RegpmEntreprise> entrepriseMigrator;
	private EntityMigrator<RegpmEtablissement> etablissementMigrator;
	private EntityMigrator<RegpmIndividu> individuMigrator;

	/**
	 * Appelé quand une tâche ne peut être ajoutée à la queue d'entrée d'un {@link ThreadPoolExecutor}, afin d'attendre patiemment
	 * qu'une place se libère
	 * @param r action à placer sur la queue
	 * @param executor exécuteur qui a refusé la tâche
	 */
	private static void rejectionExecutionHandler(Runnable r, ThreadPoolExecutor executor) {
		final BlockingQueue<Runnable> queue = executor.getQueue();
		boolean sent = false;
		try {
			while (!sent) {
				if (executor.isShutdown()) {
					// this will trigger the abort policy
					break;
				}

				// if sent successfully, that's the key out of the loop!
				sent = queue.offer(r, 1, TimeUnit.SECONDS);
			}
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}

		if (!sent) {
			ABORT_POLICY.rejectedExecution(r, executor);
		}
	}

	private static void log(Logger logger, MigrationResultMessage.Niveau niveau, String msg) {
		final Consumer<String> realLogger;
		switch (niveau) {
		case DEBUG:
			realLogger = logger::debug;
			break;
		case ERROR:
			realLogger = logger::error;
			break;
		case INFO:
			realLogger = logger::info;
			break;
		case WARN:
			realLogger = logger::warn;
			break;
		default:
			throw new IllegalArgumentException("Niveau invalide : " + niveau);
		}

		// envoi dans le log
		realLogger.accept(msg);
	}

	private static String dump(Throwable t) {
		try (StringWriter sw = new StringWriter(); PrintWriter pw = new PrintWriter(sw)) {
			pw.print(t.getClass().getName());
			pw.print(": ");
			pw.println(t.getMessage());
			t.printStackTrace(pw);
			pw.flush();
			return sw.toString();
		}
		catch (IOException e) {
			LOGGER.error("Pas pu générer le message d'erreur pour l'exception reçue", t);
			return "Erreur inattendue (voir logs applicatifs à " + new Date() + ")";
		}
	}

	public void setStreetDataMigrator(StreetDataMigrator streetDataMigrator) {
		this.streetDataMigrator = streetDataMigrator;
	}

	public void setUniregTransactionManager(PlatformTransactionManager uniregTransactionManager) {
		this.uniregTransactionManager = uniregTransactionManager;
	}

	public void setUniregSessionFactory(SessionFactory uniregSessionFactory) {
		this.uniregSessionFactory = uniregSessionFactory;
	}

	public void setRcpersClient(RcPersClient rcpersClient) {
		this.rcpersClient = rcpersClient;
	}

	public void setRcentClient(RcEntClient rcentClient) {
		this.rcentClient = rcentClient;
	}

	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

	public void setNonHabitantIndex(NonHabitantIndex nonHabitantIndex) {
		this.nonHabitantIndex = nonHabitantIndex;
	}

	public void setMode(MigrationMode mode) {
		this.mode = mode;
	}

	@Override
	public void afterPropertiesSet() throws Exception {

		this.entrepriseMigrator = new EntrepriseMigrator(uniregSessionFactory, streetDataMigrator, tiersDAO, rcentClient);
		this.etablissementMigrator = new EtablissementMigrator(uniregSessionFactory, streetDataMigrator, tiersDAO, rcentClient);
		this.individuMigrator = new IndividuMigrator(uniregSessionFactory, streetDataMigrator, tiersDAO, rcpersClient, nonHabitantIndex);

		this.executor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.SECONDS,
		                                       new ArrayBlockingQueue<>(20),
		                                       new DefaultThreadFactory(new DefaultThreadNameGenerator("Migrator")),
		                                       MigrationWorker::rejectionExecutionHandler);

		this.completionService = new ExecutorCompletionService<>(this.executor);
		this.gatheringThread = new Thread("Gathering") {
			@Override
			public void run() {
				LOGGER.info("Gathering thread starting...");
				try {
					gather();
				}
				catch (InterruptedException e) {
					LOGGER.error("Gathering thread interrupted!", e);
				}
				catch (RuntimeException | Error e) {
					LOGGER.error("Exception thrown in gathering thread", e);
				}
				finally {
					LOGGER.info("Gathering thread is now done.");
				}
			}
		};
		if (this.mode == MigrationMode.FROM_DUMP || this.mode == MigrationMode.DIRECT) {
			this.gatheringThread.start();
		}
	}

	@Override
	public void destroy() throws Exception {
		final List<Runnable> remaining = executor.shutdownNow();
		nbEnCours.addAndGet(-remaining.size());
		awaitTermination();
		gatheringThread.join();
	}

	@Override
	public void onGraphe(Graphe graphe) throws Exception {
		completionService.submit(new MigrationTask(graphe));
		nbEnCours.incrementAndGet();
		started = true;
	}

	@Override
	public void feedingOver() throws InterruptedException {
		executor.shutdown();
		awaitTermination();
	}

	private void awaitTermination() throws InterruptedException {
		while (!executor.isTerminated()) {
			executor.awaitTermination(1, TimeUnit.SECONDS);
		}
	}

	private void gather() throws InterruptedException {
		while ((!started && !executor.isTerminated()) || (started && nbEnCours.intValue() > 0)) {
			final Future<MigrationResultMessageProvider> future = completionService.poll(1, TimeUnit.SECONDS);
			if (future != null) {
				nbEnCours.decrementAndGet();

				try {
					final MigrationResultMessageProvider res = future.get();
					if (LOGGER.isDebugEnabled()) {
						LOGGER.debug("Résultat de migration reçu : " + res);
					}

					// utilisation des loggers pour les fichiers/listes de contrôle
					for (MigrationResultMessage.CategorieListe cat : MigrationResultMessage.CategorieListe.values()) {
						final List<MigrationResultMessage> messages = res.getMessages(cat);
						if (!messages.isEmpty()) {
							final Logger logger = LoggerFactory.getLogger(String.format("%s.%s", MigrationResultMessage.CategorieListe.class.getName(), cat.name()));
							messages.forEach(msg -> log(logger, msg.niveau, msg.texte));
						}
					}
				}
				catch (ExecutionException e) {
					LOGGER.error("Exception inattendue", e.getCause());
				}
			}
		}
	}

	private MigrationResultMessageProvider migrate(Graphe graphe) throws MigrationException {
		final MigrationResult mr = new MigrationResult();

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
				doMigrate(graphe, mr);
				return null;
			});

			// une fois la transaction terminée, on passe les callbacks enregistrés
			try {
				mr.runPostTransactionCallbacks();
			}
			catch (Exception e) {
				mr.addMessage(MigrationResultMessage.CategorieListe.GENERIQUE, MigrationResultMessage.Niveau.WARN, String.format("Exception levée lors de l'exécution des callbacks post-transaction : %s", dump(e)));
			}
		}
		finally {
			AuthenticationHelper.popPrincipal();
		}

		// un dernier log avant de partir
		graphe.getEntreprises().keySet().forEach(id -> mr.addMessage(MigrationResultMessage.CategorieListe.PM_MIGREE, MigrationResultMessage.Niveau.INFO, String.format("Entreprise %d migrée", id)));
		return mr;
	}

	/**
	 * Appelé dans un contexte transactionnel
	 * @param graphe le graphe d'objets à migrer
	 * @param mr le collecteur de résultats/remarques de migration
	 */
	private void doMigrate(Graphe graphe, MigrationResult mr) {

		// on commence par les entreprises, puis les établissements, puis les individus TODO ne faudrait-il pas traiter les individus d'abord ?
		// on collecte les liens entre ces entités au fur et à mesure
		// à la fin, on ajoute les liens

		final IdMapper idMapper = new IdMapper();
		final EntityLinkCollector linkCollector = new EntityLinkCollector();
		doMigrateEntreprises(graphe.getEntreprises().values(), mr, linkCollector, idMapper);
		doMigrateEtablissements(graphe.getEtablissements().values(), mr, linkCollector, idMapper);
		doMigrateIndividus(graphe.getIndividus().values(), mr, linkCollector, idMapper);
		addLinks(linkCollector.getCollectedLinks());

		// lance les consolidations nécessaires
		mr.consolidatePreTransactionCommitRegistrations();
	}

	private void doMigrateEntreprises(Collection<RegpmEntreprise> entreprises, MigrationResult mr, EntityLinkCollector linkCollector, IdMapper idMapper) {
		entreprises.forEach(e -> entrepriseMigrator.migrate(e, mr, linkCollector, idMapper));
	}

	private void doMigrateEtablissements(Collection<RegpmEtablissement> etablissements, MigrationResult mr, EntityLinkCollector linkCollector, IdMapper idMapper) {
		etablissements.forEach(e -> etablissementMigrator.migrate(e, mr, linkCollector, idMapper));
	}

	private void doMigrateIndividus(Collection<RegpmIndividu> individus, MigrationResult mr, EntityLinkCollector linkCollector, IdMapper idMapper) {
		individus.forEach(i -> individuMigrator.migrate(i, mr, linkCollector, idMapper));
	}

	private void addLinks(Collection<EntityLinkCollector.EntityLink> links) {
		if (links != null && !links.isEmpty()) {
			links.stream()
					.map(EntityLinkCollector.EntityLink::toRapportEntreTiers)
					.forEach(ret -> uniregSessionFactory.getCurrentSession().merge(ret));
		}
	}

	private class MigrationTask implements Callable<MigrationResultMessageProvider> {
		private final Graphe graphe;

		private MigrationTask(Graphe graphe) {
			this.graphe = graphe;
		}

		@Override
		public MigrationResultMessageProvider call() {
			final Set<Long> idsEntreprise = graphe.getEntreprises().keySet();
			final Set<Long> idsIndividus = graphe.getIndividus().keySet();
			try {
				while (true) {
					final EntityMigrationSynchronizer.Ticket ticket = synchronizer.hold(idsEntreprise, idsIndividus, 1000);
					if (ticket != null) {
						try {
							return migrate(graphe);
						}
						finally {
							synchronizer.release(ticket);
						}
					}
					else {
						LOGGER.info(String.format("L'une des entreprises %s ou des individus %s est déjà en cours de migration en ce moment même... On attend...",
						                          Arrays.toString(idsEntreprise.toArray(new Long[idsEntreprise.size()])),
						                          Arrays.toString(idsIndividus.toArray(new Long[idsIndividus.size()]))));
					}
				}
			}
			catch (Throwable t) {
				if (t instanceof InterruptedException) {
					Thread.currentThread().interrupt();
				}

				final MigrationResult res = new MigrationResult();
				final String msg = String.format("Les entreprises %s n'ont pas pu être migrées : %s", Arrays.toString(idsEntreprise.toArray(new Long[idsEntreprise.size()])), dump(t));
				res.addMessage(MigrationResultMessage.CategorieListe.GENERIQUE, MigrationResultMessage.Niveau.ERROR, msg);
				return res;
			}
		}
	}
}
