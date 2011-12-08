package ch.vd.uniregctb.webservices.tiers2.impl;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.mutable.MutableBoolean;
import org.apache.log4j.Logger;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.webservices.tiers2.data.TiersPart;

/**
 * Ce thread reçoit une liste d'ids de tiers à charger de la base et à retourner sous forme des tiers du web-service.
 */
public class MappingThread implements Runnable {

	private static final Logger LOGGER = Logger.getLogger(MappingThread.class);

	private final Set<Long> ids;
	private final RegDate date;
	private final Set<TiersPart> parts;
	private final Context context;
	private final MapCallback callback;

	private final Map<Long, Object> results = new HashMap<Long, Object>();
	private final MutableBoolean processingDone = new MutableBoolean(false);

	public long loadTiersTime;
	public long warmIndividusTime;
	public long mapTiersTime;

	public MappingThread(Set<Long> ids, RegDate date, Set<TiersPart> parts, Context context, MapCallback callback) {
		this.ids = ids;
		this.date = date;
		this.parts = parts;
		this.context = context;
		this.callback = callback;
	}

	@Override
	public void run() {

		try {
			final TransactionTemplate template = new TransactionTemplate(context.transactionManager);
			template.setReadOnly(true); // on ne veut pas modifier la base

			template.execute(new TransactionCallback<Object>() {
				@Override
				public Object doInTransaction(TransactionStatus status) {
					return context.hibernateTemplate.execute(new HibernateCallback<Object>() {
						@Override
						public Object doInHibernate(Session session) throws HibernateException, SQLException {
							session.setFlushMode(FlushMode.MANUAL); // on ne veut vraiment pas modifier la base
							mapTiers();
							return null;
						}
					});
				}
			});
		}
		finally {
			synchronized (processingDone) {
				processingDone.setValue(true);
				processingDone.notifyAll();
			}
		}
	}

	/**
	 * Cet appel ne retourne que lorsque le processing de la méthode 'run' est terminé.
	 *
	 * @throws InterruptedException en cas d'interruption du thread appelant.
	 */
	public void waitForProcessingDone() throws InterruptedException {
		synchronized (processingDone) {
			while (!processingDone.booleanValue()) {
				processingDone.wait();
			}
		}
	}

	private void mapTiers() {

		LOGGER.trace("Chargement des tiers - start");
		long start = System.nanoTime();

		final Set<TiersDAO.Parts> coreParts = TiersWebServiceImpl.webToCoreWithForsFiscaux(parts);

		final Set<Long> idsFull;
		if (coreParts.contains(TiersDAO.Parts.RAPPORTS_ENTRE_TIERS)) {
			idsFull = context.tiersDAO.getIdsTiersLies(ids, false);
		}
		else {
			idsFull = ids;
		}

		// on charge les tiers demandés + ceux liés (optim pour le calcul des adresses, notamment)
		final List<Tiers> listFull = context.tiersDAO.getBatch(idsFull, coreParts);
		if (listFull == null || listFull.isEmpty()) {
			return;
		}

		// on restreint la liste retournée aux tiers demandés
		final List<Tiers> list = new ArrayList<Tiers>(ids.size());
		for (Tiers tiers : listFull) {
			if (ids.contains(tiers.getId())) {
				list.add(tiers);
			}
		}

		loadTiersTime = System.nanoTime() - start;
		LOGGER.trace("Chargement des tiers - end");

		// précharge la liste des individus (dans la mesure du possible)
		if (context.serviceCivilService.isWarmable()) {

			LOGGER.trace("Préchargement des individus - start");
			start = System.nanoTime();

			BusinessHelper.warmIndividus(idsFull, parts, context, false);

			warmIndividusTime = System.nanoTime() - start;
			LOGGER.trace("Préchargement des individus - end");
		}

		LOGGER.trace("Mapping des tiers - start");
		start = System.nanoTime();

		// map les tiers
		for (Tiers tiers : list) {
			results.put(tiers.getId(), callback.map(tiers, parts, date, context));
		}

		mapTiersTime = System.nanoTime() - start;
		LOGGER.trace("Mapping des tiers - end");
	}

	public Map<Long, Object> getResults() {
		return results;
	}
}
