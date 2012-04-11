package ch.vd.uniregctb.indexer.async;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.hibernate.FlushMode;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.dialect.Dialect;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Assert;
import ch.vd.registre.base.utils.Pair;
import ch.vd.uniregctb.hibernate.interceptor.HibernateFakeInterceptor;
import ch.vd.uniregctb.indexer.IndexerBatchException;
import ch.vd.uniregctb.indexer.tiers.GlobalTiersIndexer;
import ch.vd.uniregctb.indexer.tiers.GlobalTiersIndexerImpl;
import ch.vd.uniregctb.interfaces.model.AttributeIndividu;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersDAOImpl;
import ch.vd.uniregctb.worker.BatchWorker;


public class TiersIndexerWorker implements BatchWorker<Long> {

	private static final Logger LOGGER = Logger.getLogger(TiersIndexerWorker.class);

	public static final int BATCH_SIZE = 20;

	private final PlatformTransactionManager transactionManager;
	private final GlobalTiersIndexerImpl indexer;
	private final SessionFactory sessionFactory;
	private final Dialect dialect;

	private final GlobalTiersIndexer.Mode mode;

	private boolean prefetchIndividus;
	private ServiceCivilService serviceCivilService;

	private String name;

	/**
	 * Construit un thread d'indexation qui consomme les ids des tiers à indexer à partir d'une queue.
	 *
	 * @param mode                le mode d'indexation voulu. Renseigné dans le cas d'une réindexation complète ou partielle; ou <b>null</b> dans le cas d'une indexation au fil de l'eau des tiers.
	 * @param globalTiersIndexer  l'indexer des tiers
	 * @param sessionFactory      la session factory hibernate
	 * @param transactionManager  le transaction manager
	 * @param dialect             le dialect hibernate utilisé
	 * @param name                le nom du thread
	 * @param prefetchIndividus   <b>vrai</b> si le cache des individus doit être préchauffé par lot; <b>faux</b> autrement.
	 * @param serviceCivilService le service civil qui permet de préchauffer le cache des individus
	 */
	public TiersIndexerWorker(GlobalTiersIndexer.Mode mode, GlobalTiersIndexerImpl globalTiersIndexer, SessionFactory sessionFactory, PlatformTransactionManager transactionManager, Dialect dialect,
	                          String name, boolean prefetchIndividus, ServiceCivilService serviceCivilService) {
		this.indexer = globalTiersIndexer;
		this.transactionManager = transactionManager;
		this.sessionFactory = sessionFactory;
		this.mode = mode;
		this.dialect = dialect;
		this.name = name;
		this.prefetchIndividus = prefetchIndividus;
		this.serviceCivilService = serviceCivilService;
		Assert.notNull(this.indexer);
		Assert.notNull(this.transactionManager);
		Assert.notNull(this.sessionFactory);
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
				 * On crée à la main une nouvelle session hibernate avec un intercepteur vide (HibernateFakeInterceptor). Cela permet de désactiver
				 * la validation des tiers, et de flagger comme 'dirty' même les tiers qui ne valident pas. Autrement, le premier tiers qui ne valide pas
				 * fait péter une exception, qui remonte jusqu'à la méthode 'run' du thread et qui provoque l'arrêt immédiat du thread !
				 */
				Session session = sessionFactory.openSession(new HibernateFakeInterceptor());
				try {
					session.setFlushMode(FlushMode.MANUAL);
					final List<Tiers> list;

					if (batch.size() == 1) {
						final Tiers tiers = (Tiers) session.get(Tiers.class, batch.get(0));
						if (tiers != null) {
							list = new ArrayList<Tiers>(1);
							list.add(tiers);
						}
						else {
							list = null;
						}
					}
					else {
						if (prefetchIndividus && serviceCivilService.isWarmable()) {
							// Si le service est chauffable, on précharge les individus en vrac pour améliorer les performances.
							// Sans préchargement, chaque individu est obtenu séparemment à travers le service civil (= au minimum
							// une requête par individu); et avec le préchargement on peut charger 100 individus d'un coup.
							warmIndividuCache(session, batch);
						}

						final Query query = session.createQuery("from Tiers t where t.id in (:ids)");
						query.setParameterList("ids", batch);
						//noinspection unchecked
						list = query.list();
					}

					indexTiers(list, session);
					session.flush();
				}
				catch (Exception e) {
					LOGGER.error(e, e);
				}
				finally {
					session.close();
				}
				return null;
			}
		});
	}

	private void warmIndividuCache(Session session, List<Long> batch) {

		long start = System.nanoTime();

		final TiersDAOImpl.GetNumerosIndividusCallback callback = new TiersDAOImpl.GetNumerosIndividusCallback(batch, true);
		final Set<Long> numerosIndividus = callback.doInHibernate(session);

		if (!numerosIndividus.isEmpty()) { // on peut tomber sur une plage de tiers ne contenant pas d'habitant
			try {
				serviceCivilService.getIndividus(numerosIndividus, null, AttributeIndividu.ADRESSES); // chauffe le cache

				long nanosecondes = System.nanoTime() - start;
				LOGGER.info("=> Récupéré " + numerosIndividus.size() + " individus en " + (nanosecondes / 1000000000L) + "s.");
			}
			catch (Exception e) {
				LOGGER.error("Impossible de précharger le lot d'individus [" + numerosIndividus + "]. On continue un par un pour ce lot.", e);
			}
		}
	}

	private void indexTiers(List<Tiers> tiers, Session session) {

		if (tiers == null || tiers.isEmpty()) {
			return;
		}

		try {
			// on n'indexe pas les tiers liés au tiers courant lorsqu'on veut indexer toute ou une fraction déterminée
			// de la base de données : les tiers liés vont de toutes façons se faire indexer pour eux-même.
			final boolean followDependents = (mode == null);

			// on n'enlève pas préalablement les données indexées en mode FULL et INCREMENTAL, parce que - par définition -
			// ces données n'existent pas dans ces modes-là.
			final boolean removeBefore = (mode != GlobalTiersIndexer.Mode.FULL && mode != GlobalTiersIndexer.Mode.INCREMENTAL);

			indexer.indexTiers(tiers, removeBefore, followDependents);

			setDirtyFlag(extractIds(tiers), false, session);
		}
		catch (IndexerBatchException e) {
			// 1 ou plusieurs tiers n'ont pas pu être indexés (selon la liste fournie par l'exception)
			LOGGER.error(e.getMessage());
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug(e, e);
			}

			// la plupart des tiers ont pu être indexés...
			final Set<Long> indexedIds = new HashSet<Long>(extractIds(tiers));

			// ...sauf ceux-ci
			final Set<Long> inErrorIds = new HashSet<Long>();
			final List<Pair<Long, Exception>> list = e.getExceptions();
			for (Pair<Long, Exception> p : list) {
				final Long tiersId = p.getFirst();
				if (tiersId != null) {
					inErrorIds.add(tiersId);
				}
			}
			indexedIds.removeAll(inErrorIds);

			setDirtyFlag(indexedIds, false, session); // reset le flag dirty de tous les tiers qui ont été indexés
			setDirtyFlag(inErrorIds, true, session); // flag tous les tiers qui n'ont pas pu être indexés comme dirty, notamment ceux qui ne l'étaient pas avant
		}
		catch (Exception e) {
			// potentiellement aucun des tiers n'a pu être indexés
			LOGGER.error("Impossible d'indexer les tiers n°" + buildTiersNumeros(tiers), e);
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug(e, e);
			}

			setDirtyFlag(extractIds(tiers), true, session);
		}
	}

	private void setDirtyFlag(Collection<Long> ids, boolean flag, Session session) {

		if (ids == null || ids.isEmpty()) {
			return;
		}

		final SQLQuery query = session.createSQLQuery("update TIERS set INDEX_DIRTY = " + dialect.toBooleanValueString(flag) + " where NUMERO in (:ids)");
		query.setParameterList("ids", ids);
		query.executeUpdate();

		if (!flag) {
			// [UNIREG-1979] On remet aussi à zéro tous les tiers dont la date 'reindex_on' est atteinte aujourd'hui
			final SQLQuery q = session.createSQLQuery("update TIERS set REINDEX_ON = null where REINDEX_ON is not null and REINDEX_ON <= :today and NUMERO in (:ids)");
			q.setParameter("today", RegDate.get().index());
			q.setParameterList("ids", ids);
			q.executeUpdate();
		}
	}

	private List<Long> extractIds(List<Tiers> tiers) {
		if (tiers == null || tiers.isEmpty()) {
			return null;
		}
		final List<Long> ids = new ArrayList<Long>(tiers.size());
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
