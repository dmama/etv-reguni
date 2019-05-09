package ch.vd.unireg.webservices.party3.impl;

import javax.persistence.FlushModeType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.tiers.TiersDAO;
import ch.vd.unireg.xml.Context;
import ch.vd.unireg.xml.party.v1.PartyPart;

/**
 * Ce thread reçoit une liste d'ids de tiers à charger de la base et à retourner sous forme des tiers du web-service.
 */
public class MappingThread implements Runnable {

	private static final Logger LOGGER = LoggerFactory.getLogger(MappingThread.class);

	private final Set<Long> ids;
	private final RegDate date;
	private final Set<PartyPart> parts;
	private final Context context;
	private final MapCallback callback;

	private final Map<Long, Object> results = new HashMap<>();
	private final MutableBoolean processingDone = new MutableBoolean(false);
	private RuntimeException processingException;

	public long loadTiersTime;
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

		try {
			final TransactionTemplate template = new TransactionTemplate(context.transactionManager);
			template.setReadOnly(true); // on ne veut pas modifier la base

			template.execute(status -> context.hibernateTemplate.execute(FlushModeType.COMMIT, session -> {
				mapParties();
				return null;
			}));
		}
		catch (RuntimeException e) {
			LOGGER.warn(e.getMessage(), e);
			processingException = e;
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

	private void mapParties() {

		LOGGER.trace("Chargement des tiers - start");
		long start = System.nanoTime();

		final Set<TiersDAO.Parts> coreParts = PartyWebServiceImpl.xmlToCoreWithForsFiscaux(parts);

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
		final List<Tiers> list = new ArrayList<>(ids.size());
		for (Tiers tiers : listFull) {
			if (ids.contains(tiers.getId())) {
				list.add(tiers);
			}
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

	public RuntimeException getProcessingException() {
		return processingException;
	}

	public Map<Long, Object> getResults() {
		return results;
	}
}
