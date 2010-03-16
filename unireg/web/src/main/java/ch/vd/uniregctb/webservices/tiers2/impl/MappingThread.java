package ch.vd.uniregctb.webservices.tiers2.impl;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.webservices.tiers2.data.TiersPart;
import org.apache.log4j.Logger;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Ce thread reçoit une liste d'ids de tiers à charger de la base et à retourner sous forme des tiers du web-service.
 */
public class MappingThread extends Thread {

	private static final Logger LOGGER = Logger.getLogger(MappingThread.class);

	private final Set<Long> ids;
	private final RegDate date;
	private final Set<TiersPart> parts;
	private final Context context;
	private final MapCallback callback;

	private final Map<Long, Object> results = new HashMap<Long, Object>();

	public long loadTiersTime;
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

		TransactionTemplate template = new TransactionTemplate(context.transactionManager);
		template.setReadOnly(true); // on ne veut pas modifier la base

		template.execute(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				return context.hibernateTemplate.execute(new HibernateCallback() {
					public Object doInHibernate(Session session) throws HibernateException, SQLException {
						session.setFlushMode(FlushMode.MANUAL); // on ne veut vraiment pas modifier la base
						mapTiers();
						return null;
					}
				});
			}
		});
	}

	private void mapTiers() {

		LOGGER.trace("Chargement des tiers - start");
		long start = System.nanoTime();

		// charge les tiers
		final Set<TiersDAO.Parts> coreParts = TiersWebServiceImpl.webToCoreWithForsFiscaux(parts);
		final List<Tiers> list = context.tiersDAO.getBatch(ids, coreParts);
		if (list == null || list.isEmpty()) {
			return;
		}

		loadTiersTime = System.nanoTime() - start;
		LOGGER.trace("Chargement des tiers - end");

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
