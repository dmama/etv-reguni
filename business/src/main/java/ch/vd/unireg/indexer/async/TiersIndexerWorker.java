package ch.vd.unireg.indexer.async;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.FlushMode;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.registre.base.utils.Pair;
import ch.vd.unireg.cache.ServiceCivilCacheWarmer;
import ch.vd.unireg.common.Switchable;
import ch.vd.unireg.indexer.IndexerBatchException;
import ch.vd.unireg.indexer.tiers.GlobalTiersIndexerImpl;
import ch.vd.unireg.interfaces.civil.data.AttributeIndividu;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.tiers.TiersDAO;
import ch.vd.unireg.tiers.TiersDAOImpl;
import ch.vd.unireg.worker.BatchWorker;


public class TiersIndexerWorker implements BatchWorker<Long> {

	private static final Logger LOGGER = LoggerFactory.getLogger(TiersIndexerWorker.class);

	public static final int BATCH_SIZE = 20;

	private final TiersDAO tiersDAO;
	private final PlatformTransactionManager transactionManager;
	private final GlobalTiersIndexerImpl indexer;
	private final SessionFactory sessionFactory;
	private final boolean followDependents;
	private final boolean removeBefore;

	@Nullable
	private final ServiceCivilCacheWarmer serviceCivilCacheWarmer;

	private final String name;

	/**
	 * Construit un thread d'indexation qui consomme les ids des tiers à indexer à partir d'une queue.
	 *
	 * @param followDependentsTiers   <i>vrai</i> s'il faut aussi indexer les tiers liés au tiers courant.
	 * @param removeBeforeIndexing    <i>vrai</i> s'il faut supprimer le tiers de l'indexe avant de l'indexer.
	 * @param globalTiersIndexer      l'indexer des tiers
	 * @param sessionFactory          la session factory hibernate
	 * @param transactionManager      le transaction manager
	 * @param name                    le nom du thread
	 * @param serviceCivilCacheWarmer le warmer du cache du service civil
	 * @param tiersDAO                le tiers DAO
	 */
	public TiersIndexerWorker(boolean followDependentsTiers, boolean removeBeforeIndexing, @NotNull GlobalTiersIndexerImpl globalTiersIndexer, @NotNull SessionFactory sessionFactory, @NotNull PlatformTransactionManager transactionManager,
	                          String name, @Nullable ServiceCivilCacheWarmer serviceCivilCacheWarmer, TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
		this.indexer = globalTiersIndexer;
		this.transactionManager = transactionManager;
		this.sessionFactory = sessionFactory;
		this.name = name;
		this.serviceCivilCacheWarmer = serviceCivilCacheWarmer;
		this.followDependents = followDependentsTiers;
		this.removeBefore = removeBeforeIndexing;
	}

	@Override
	public int maxBatchSize() {
		return BATCH_SIZE;
	}

	@Override
	public String getName() {
		return name;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void process(final List<Long> batch) throws Exception {

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("ASYNC indexation des tiers n° " + Arrays.toString(batch.toArray()));
		}

		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.execute(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				/*
				 * On crée à la main une nouvelle session hibernate en ayant pris soin de désactiver l'intercepteur. Cela permet de désactiver
				 * la validation des tiers, et de flagger comme 'dirty' même les tiers qui ne valident pas. Autrement, le premier tiers qui ne valide pas
				 * fait péter une exception, qui remonte jusqu'à la méthode 'run' du thread et qui provoque l'arrêt immédiat du thread !
				 */
				final Switchable interceptorSwitch = (Switchable) sessionFactory.getSessionFactoryOptions().getInterceptor();
				final boolean enabled = interceptorSwitch.isEnabled();
				interceptorSwitch.setEnabled(false);
				try {
					final Session session = sessionFactory.openSession();
					try {
						session.setFlushMode(FlushMode.MANUAL);
						final List<Tiers> list;

						if (batch.size() == 1) {
							final Tiers tiers = (Tiers) session.get(Tiers.class, batch.get(0));
							if (tiers != null) {
								list = new ArrayList<>(1);
								list.add(tiers);
							}
							else {
								list = null;
							}
						}
						else {
							// Si le service est chauffable, on précharge les individus en vrac pour améliorer les performances.
							warmIndividuCache(session, batch);

							final Query query = session.createQuery("from Tiers t where t.id in (:ids)");
							query.setParameterList("ids", batch);
							//noinspection unchecked
							list = query.list();
						}

						indexTiers(list, session);
						session.flush();
					}
					catch (Exception e) {
						LOGGER.error(e.getMessage(), e);
					}
					finally {
						session.close();
					}
				}
				finally {
					interceptorSwitch.setEnabled(enabled);
				}
				return null;
			}
		});
	}

	private void warmIndividuCache(Session session, List<Long> batch) {
		if (serviceCivilCacheWarmer != null && serviceCivilCacheWarmer.isServiceWarmable()) {
			final Set<Long> numerosIndividus = TiersDAOImpl.getNumerosIndividu(batch, true, session);
			final AttributeIndividu[] parties = AttributeIndividu.values(); // toutes les parties (pour charger le cache persistent)
			serviceCivilCacheWarmer.warmIndividus(numerosIndividus, null, parties);
		}
	}

	private void indexTiers(List<Tiers> tiers, Session session) {

		if (tiers == null || tiers.isEmpty()) {
			return;
		}

		try {
			// on index le tiers
			indexer.indexTiers(tiers, removeBefore, followDependents);
			tiersDAO.setDirtyFlag(extractIds(tiers), false, session);
		}
		catch (IndexerBatchException e) {
			// 1 ou plusieurs tiers n'ont pas pu être indexés (selon la liste fournie par l'exception)
			LOGGER.error(e.getMessage());
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug(e.getMessage(), e);
			}

			// la plupart des tiers ont pu être indexés...
			final Set<Long> indexedIds = new HashSet<>(extractIds(tiers));

			// ...sauf ceux-ci
			final Set<Long> inErrorIds = new HashSet<>();
			final List<Pair<Long, Exception>> list = e.getExceptions();
			for (Pair<Long, Exception> p : list) {
				final Long tiersId = p.getFirst();
				if (tiersId != null) {
					inErrorIds.add(tiersId);
				}
			}
			indexedIds.removeAll(inErrorIds);

			// reset le flag dirty de tous les tiers qui ont été indexés
			tiersDAO.setDirtyFlag(indexedIds, false, session);
			// flag tous les tiers qui n'ont pas pu être indexés comme dirty, notamment ceux qui ne l'étaient pas avant
			tiersDAO.setDirtyFlag(inErrorIds, true, session);
		}
		catch (Exception e) {
			// potentiellement aucun des tiers n'a pu être indexés
			LOGGER.error("Impossible d'indexer les tiers n°" + buildTiersNumeros(tiers), e);
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug(e.getMessage(), e);
			}

			tiersDAO.setDirtyFlag(extractIds(tiers), true, session);
		}
	}

	private List<Long> extractIds(List<Tiers> tiers) {
		if (tiers == null || tiers.isEmpty()) {
			return null;
		}
		final List<Long> ids = new ArrayList<>(tiers.size());
		for (Tiers t : tiers) {
			if (t != null) {
				ids.add(t.getNumero());
			}
		}
		return ids;
	}

	private static String buildTiersNumeros(List<Tiers> list) {
		StringBuilder builder = new StringBuilder("{");
		for (int i = 0, listSize = list.size(); i < listSize; i++) {
			final Tiers t = list.get(i);
			builder.append(t.getNumero());
			if (i < listSize - 1) {
				builder.append(", ");
			}
		}
		builder.append('}');
		return builder.toString();
	}
}
