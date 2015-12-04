package ch.vd.uniregctb.migration.pm;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.common.BatchIterator;
import ch.vd.uniregctb.common.StandardBatchIterator;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.migration.pm.engine.probe.ProgressMeasurementProbe;
import ch.vd.uniregctb.migration.pm.indexeur.NonHabitantIndex;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.transaction.TransactionTemplate;

public class MigrationInitializer implements InitializingBean {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(MigrationInitializer.class);

	private static final int PREMIERE_PF = 1995;

	private MigrationInitializationRegistrar registrar;

	private SessionFactory uniregSessionFactory;
	private PlatformTransactionManager uniregTransactionManager;
	private NonHabitantIndex nonHabitantIndex;
	private boolean nonHabitantIndexToBeCleared = false;
	private int nbThreadsIndexation = 1;
	private ProgressMeasurementProbe indexationProgressMonitor;

	public void setRegistrar(MigrationInitializationRegistrar registrar) {
		this.registrar = registrar;
	}

	public void setUniregSessionFactory(SessionFactory uniregSessionFactory) {
		this.uniregSessionFactory = uniregSessionFactory;
	}

	public void setUniregTransactionManager(PlatformTransactionManager uniregTransactionManager) {
		this.uniregTransactionManager = uniregTransactionManager;
	}

	public void setNonHabitantIndex(NonHabitantIndex nonHabitantIndex) {
		this.nonHabitantIndex = nonHabitantIndex;
	}

	public void setNonHabitantIndexToBeCleared(boolean nonHabitantIndexToBeCleared) {
		this.nonHabitantIndexToBeCleared = nonHabitantIndexToBeCleared;
	}

	public void setNbThreadsIndexation(int nbThreadsIndexation) {
		this.nbThreadsIndexation = nbThreadsIndexation;
	}

	public void setIndexationProgressMonitor(ProgressMeasurementProbe indexationProgressMonitor) {
		this.indexationProgressMonitor = indexationProgressMonitor;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		// génération des PF éventuellement manquantes
		registrar.registerInitializationCallback(this::initMissingPeriodesFiscales);

		// création de l'index des non-habitants inconnus de RCPers
		if (nonHabitantIndexToBeCleared) {
			registrar.registerInitializationCallback(this::initNonHabitantIndex);
		}
	}
	
	private void initNonHabitantIndex() {
		LOGGER.info("Génération des données d'indexation des non-habitants inconnus RCPers dans Unireg.");
		try {
			// nettoyage préalable
			nonHabitantIndex.overwriteIndex();

			// récupération des identifiants à traiter
			//noinspection unchecked
			final List<Long> ids = doInUniregTransaction(true, status -> {
				final Session currentSession = uniregSessionFactory.getCurrentSession();
				final Query query = currentSession.createQuery("select p.numero from PersonnePhysique p where numeroIndividu is null");
				return query.list();
			});

			LOGGER.info(String.format("%d non-habitants inconnus de RCPers trouvés.", ids.size()));

			// commande des traitements d'indexation
			final ExecutorService executor = Executors.newFixedThreadPool(nbThreadsIndexation);
			try {
				final CompletionService<Integer> completionService = new ExecutorCompletionService<>(executor);
				final BatchIterator<Long> idsIterator = new StandardBatchIterator<>(ids, 100);
				int nbBatches = 0;
				while (idsIterator.hasNext()) {
					final List<Long> localIds = idsIterator.next();
					completionService.submit(() -> doInUniregTransaction(true, status -> {
						final Session currentSession = uniregSessionFactory.getCurrentSession();
						final Query query = currentSession.createQuery("select p from PersonnePhysique p where p.numero in (:ids)");
						query.setParameterList("ids", localIds);

						// boucle sur les personnes physiques
						//noinspection unchecked
						final Iterator<PersonnePhysique> iterator = query.iterate();
						while (iterator.hasNext()) {
							final PersonnePhysique pp = iterator.next();
							nonHabitantIndex.index(pp);
						}
					}), localIds.size());

					++nbBatches;
				}
				executor.shutdown();

				// réception des résultats
				int treated = 0;
				while (nbBatches > 0) {
					final Future<Integer> tailleBatch = completionService.poll(1, TimeUnit.SECONDS);
					if (tailleBatch != null) {
						--nbBatches;

						treated += tailleBatch.get();
						indexationProgressMonitor.setPercentProgress(treated * 100 / ids.size());
					}
				}
			}
			catch (ExecutionException e) {
				LOGGER.error("Exception lancée lors de l'indexation des non-habitants...", e.getCause());
				throw new RuntimeException(e.getCause());
			}
			finally {
				executor.shutdownNow();
				while (!executor.isTerminated()) {
					executor.awaitTermination(1, TimeUnit.SECONDS);
				}
			}
		}
		catch (InterruptedException e) {
			LOGGER.error("Thread interrompu!", e);
			throw new RuntimeException();
		}
		finally {
			LOGGER.info("Génération des données d'indexation des non-habitants terminée.");
		}
	}

	private void initMissingPeriodesFiscales() {
		LOGGER.info("Ajout des PF manquantes dans Unireg (depuis " + PREMIERE_PF + ").");
		AuthenticationHelper.pushPrincipal(MigrationConstants.VISA_MIGRATION);
		try {
			doInUniregTransaction(false, status -> {
				final Session currentSession = uniregSessionFactory.getCurrentSession();
				final Query query = currentSession.createQuery("select pf from PeriodeFiscale pf");
				//noinspection unchecked
				final List<PeriodeFiscale> pfExistantes = Optional.ofNullable((List<PeriodeFiscale>) query.list()).orElse(Collections.emptyList());
				final Set<Integer> anneesCouvertes = pfExistantes.stream().map(PeriodeFiscale::getAnnee).collect(Collectors.toSet());
				for (int annee = PREMIERE_PF ; annee <= RegDate.get().year() ; ++ annee) {
					if (!anneesCouvertes.contains(annee)) {
						LOGGER.info("Création de la PF manquante " + annee + ".");
						final PeriodeFiscale pf = new PeriodeFiscale();
						pf.setAnnee(annee);
						pf.setDefaultPeriodeFiscaleParametres();
						currentSession.save(pf);
					}
				}
			});
		}
		finally {
			AuthenticationHelper.popPrincipal();
			LOGGER.info("Ajout des PF manquantes terminé.");
		}
	}

	private <T> T doInUniregTransaction(boolean readonly, TransactionCallback<T> callback) {
		final TransactionTemplate template = new TransactionTemplate(uniregTransactionManager);
		template.setReadOnly(readonly);
		return template.execute(callback);
	}

	private void doInUniregTransaction(boolean readonly, Consumer<TransactionStatus> callback) {
		doInUniregTransaction(readonly, status -> {
			callback.accept(status);
			return null;
		});
	}
}
