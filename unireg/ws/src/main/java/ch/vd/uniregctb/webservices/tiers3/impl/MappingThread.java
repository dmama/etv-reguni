package ch.vd.uniregctb.webservices.tiers3.impl;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.webservices.tiers3.PartyPart;
import ch.vd.uniregctb.interfaces.model.AttributeIndividu;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersDAO;

/**
 * Ce thread reçoit une liste d'ids de tiers à charger de la base et à retourner sous forme des tiers du web-service.
 */
public class MappingThread extends Thread {

	private static final Logger LOGGER = Logger.getLogger(MappingThread.class);

	private final Set<Long> ids;
	private final RegDate date;
	private final Set<PartyPart> parts;
	private final Context context;
	private final MapCallback callback;

	private final Map<Long, Object> results = new HashMap<Long, Object>();

	public long loadTiersTime;
	public long warmIndividusTime;
	public long mapTiersTime;

	public MappingThread(Set<Long> ids, RegDate date, Set<PartyPart> parts, Context context, MapCallback callback) {
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

		template.execute(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				status.setRollbackOnly(); // on ne veut vraiment pas modifier la base
				return context.hibernateTemplate.execute(new HibernateCallback<Object>() {
					@Override
					public Object doInHibernate(Session session) throws HibernateException, SQLException {
						session.setFlushMode(FlushMode.MANUAL); // on ne veut vraiment pas modifier la base
						mapParties();
						return null;
					}
				});
			}
		});
	}

	private void mapParties() {

		LOGGER.trace("Chargement des tiers - start");
		long start = System.nanoTime();

		// charge les tiers
		final Set<TiersDAO.Parts> coreParts = PartyWebServiceImpl.webToCoreWithForsFiscaux(parts);
		final List<Tiers> list = context.tiersDAO.getBatch(ids, coreParts);
		if (list == null || list.isEmpty()) {
			return;
		}

		loadTiersTime = System.nanoTime() - start;
		LOGGER.trace("Chargement des tiers - end");

		// précharge la liste des individus (dans la mesure du possible)
		if (context.serviceCivilService.isWarmable()) {

			LOGGER.trace("Préchargement des individus - start");
			start = System.nanoTime();

			final Set<Long> numerosIndividus = context.tiersDAO.getNumerosIndividu(ids, true);
			if (!numerosIndividus.isEmpty()) { // on peut tomber sur une plage de tiers ne contenant pas d'habitant
				try {
					final AttributeIndividu[] attributs;
					if (parts != null && (parts.contains(PartyPart.ADDRESSES) || parts.contains(PartyPart.FORMATTED_ADDRESSES))) {
						attributs = new AttributeIndividu[]{AttributeIndividu.ADRESSES, AttributeIndividu.PERMIS};
					}
					else {
						attributs = new AttributeIndividu[]{AttributeIndividu.PERMIS};
					}
					// date=null => parce qu'on s'intéresse à l'historique complete de l'individu
					context.serviceCivilService.getIndividus(numerosIndividus, null, attributs); // chauffe le cache
				}
				catch (Exception e) {
					LOGGER.warn("Impossible de précharger le lot d'individus [" + numerosIndividus + "].", e);
				}
			}

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
