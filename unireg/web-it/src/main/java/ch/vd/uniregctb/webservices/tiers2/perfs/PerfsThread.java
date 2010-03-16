package ch.vd.uniregctb.webservices.tiers2.perfs;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.ArrayUtils;
import org.apache.log4j.Logger;

import ch.vd.registre.base.utils.NotImplementedException;
import ch.vd.uniregctb.perfs.PerfsAccessFileIterator;
import ch.vd.uniregctb.webservices.tiers2.AccessDeniedException_Exception;
import ch.vd.uniregctb.webservices.tiers2.BatchTiers;
import ch.vd.uniregctb.webservices.tiers2.BusinessException_Exception;
import ch.vd.uniregctb.webservices.tiers2.Date;
import ch.vd.uniregctb.webservices.tiers2.GetBatchTiers;
import ch.vd.uniregctb.webservices.tiers2.GetTiers;
import ch.vd.uniregctb.webservices.tiers2.GetTiersHisto;
import ch.vd.uniregctb.webservices.tiers2.GetTiersPeriode;
import ch.vd.uniregctb.webservices.tiers2.TechnicalException_Exception;
import ch.vd.uniregctb.webservices.tiers2.Tiers;
import ch.vd.uniregctb.webservices.tiers2.TiersHisto;
import ch.vd.uniregctb.webservices.tiers2.TiersPart;
import ch.vd.uniregctb.webservices.tiers2.TiersPort;
import ch.vd.uniregctb.webservices.tiers2.UserLogin;

/**
 * Thread qui exécute un certain nombre de requêtes au web-service, puis s'arrête.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class PerfsThread extends Thread {

	private static final Logger LOGGER = Logger.getLogger(PerfsThread.class);

	private long queryTime = 0;
	private int queryCount = 0;
	private int tiersCount = 0;
	private final TiersPort port;

	private final PerfsAccessFileIterator ids;
	private int errorsCount;

	private final Query query;
	private final Integer batch;

	public static abstract class Query {

		protected UserLogin login = new UserLogin();

		protected Set<TiersPart> parts = null;

		public Query(String operateur, int oid) {
			login.setUserId(operateur);
			login.setOid(oid);
		}

		public void setParts(Set<TiersPart> parts) {
			this.parts = Collections.unmodifiableSet(parts);
		}

		public abstract Object execute(TiersPort port, long id) throws TechnicalException_Exception, AccessDeniedException_Exception,
				BusinessException_Exception;

		public abstract List<?> executeBatch(TiersPort port, Collection<Long> ids) throws TechnicalException_Exception,
				AccessDeniedException_Exception, BusinessException_Exception;

		public abstract String description();

		protected String getOperateurDescription() {
			return login.getUserId() + "/" + login.getOid();
		}

		protected String getPartsDescription() {
			if (parts == null || parts.isEmpty()) {
				return "<none>";
			}
			else {
				String s = null;
				for (TiersPart e : parts) {
					if (s == null) {
						s = e.name();
					}
					else {
						s += "+" + e.name();
					}
				}
				return s;
			}
		}
	}

	public static class DateQuery extends Query {
		private final Date date;

		public DateQuery(Date date, String operateur, int oid) {
			super(operateur, oid);
			this.date = date;
		}

		@Override
		public Object execute(TiersPort port, long id) throws TechnicalException_Exception, AccessDeniedException_Exception,
				BusinessException_Exception {

			GetTiers params = new GetTiers();
			params.setLogin(login);
			params.setDate(date);
			params.setTiersNumber(id);
			if (parts != null) {
				params.getParts().addAll(parts);
			}

			Tiers tiers = port.getTiers(params);
			return tiers;
		}

		@Override
		public List<?> executeBatch(TiersPort port, Collection<Long> ids) throws TechnicalException_Exception,
				AccessDeniedException_Exception, BusinessException_Exception {

			GetBatchTiers params = new GetBatchTiers();
			params.setLogin(login);
			params.setDate(date);
			params.getTiersNumbers().addAll(ids);
			if (parts != null) {
				params.getParts().addAll(parts);
			}

			BatchTiers batch = port.getBatchTiers(params);
			return batch.getEntries();
		}

		@Override
		public String description() {
			return "operateur=" + getOperateurDescription() + ", date=" + date.getYear() + "." + date.getMonth() + "." + date.getDay()
					+ ", parts=" + getPartsDescription();
		}
	}

	public static class PeriodeQuery extends Query {
		private final int annee;

		public PeriodeQuery(int annee, String operateur, int oid) {
			super(operateur, oid);
			this.annee = annee;
		}

		@Override
		public Object execute(TiersPort port, long id) throws TechnicalException_Exception, AccessDeniedException_Exception,
				BusinessException_Exception {

			GetTiersPeriode params = new GetTiersPeriode();
			params.setLogin(login);
			params.setPeriode(annee);
			params.setTiersNumber(id);
			if (parts != null) {
				params.getParts().addAll(parts);
			}

			TiersHisto tiers = port.getTiersPeriode(params);
			return tiers;
		}

		@Override
		public List<?> executeBatch(TiersPort port, Collection<Long> ids) {
			throw new NotImplementedException();
		}

		@Override
		public String description() {
			return "operateur=" + getOperateurDescription() + ", periode=" + annee + ", parts=" + getPartsDescription();
		}
	}

	public static class HistoQuery extends Query {

		public HistoQuery(String operateur, int oid) {
			super(operateur, oid);
		}

		@Override
		public Object execute(TiersPort port, long id) throws TechnicalException_Exception, AccessDeniedException_Exception,
				BusinessException_Exception {

			GetTiersHisto params = new GetTiersHisto();
			params.setLogin(login);
			params.setTiersNumber(id);
			if (parts != null) {
				params.getParts().addAll(parts);
			}

			TiersHisto tiers = port.getTiersHisto(params);
			return tiers;
		}

		@Override
		public List<?> executeBatch(TiersPort port, Collection<Long> ids) {
			throw new NotImplementedException();
		}

		@Override
		public String description() {
			return "operateur=" + getOperateurDescription() + ", histo, parts=" + getPartsDescription();
		}
	}

	public PerfsThread(TiersPort port, Query query, Integer batch, PerfsAccessFileIterator ids) {
		this.port = port;
		LOGGER.info("[thread-" + this.getId() + "] port is " + port.toString());
		this.query = query;
		this.batch = batch;
		this.ids = ids;
		this.errorsCount = 0;
	}

	@Override
	public void run() {

		if (batch == null || batch.intValue() < 2) {
			runNormal();
		}
		else {
			runBatch(batch.intValue());
		}
	}

	private void runNormal() {

		int i = 0;

		for (Long id = ids.getNextId(); id != null; id = ids.getNextId()) {

			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Traitement du tiers n°" + id);
			}

			if (++i % 100 == 0) {
				LOGGER.info("[thread-" + this.getId() + "] traité " + i + " tiers");
			}

			try {
				long before = System.nanoTime();

				// exécution de la requête
				Object tiers = query.execute(port, id);

				long after = System.nanoTime();
				long delta = after - before;
				queryTime += delta;
				++queryCount;
				++tiersCount;

				if (LOGGER.isTraceEnabled()) {
					long ping = delta / PerfsClient.NANO_TO_MILLI;
					if (tiers != null) {
						LOGGER.trace("Récupéré le tiers n°" + id + " de type " + tiers.getClass().getSimpleName() + " (" + ping + " ms)");
					}
					else {
						LOGGER.trace("Le tiers n°" + id + " n'existe pas (" + ping + " ms)");
					}
				}
			}
			catch (Exception e) {
				LOGGER.error("Impossible de récupérer le tiers n°" + id + ". Raison: " + e.getMessage());
				errorsCount++;
			}
		}
	}

	private void runBatch(final int size) {

		int i = 0;

		for (List<Long> batch = ids.getNextIds(size); batch != null; batch = ids.getNextIds(size)) {

			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Traitement du batch " + ArrayUtils.toString(batch.toArray()));
			}

			if (++i % 100 == 0) {
				LOGGER.info("[thread-" + this.getId() + "] traité " + i + " batches");
			}

			try {
				long before = System.nanoTime();

				// exécution de la requête
				List<?> tiers = query.executeBatch(port, batch);

				long after = System.nanoTime();
				long delta = after - before;
				queryTime += delta;
				++queryCount;
				tiersCount += (tiers == null ? 0 : tiers.size());

				if (LOGGER.isTraceEnabled()) {
					long ping = delta / PerfsClient.NANO_TO_MILLI;
					if (tiers != null) {
						LOGGER.trace("Récupéré " + tiers.size() + " tiers (" + ping + " ms)");
					}
				}
			}
			catch (Exception e) {
				LOGGER.error("Impossible de récupérer le batch " + ArrayUtils.toString(batch.toArray()) + ". Raison: " + e.getMessage());
				errorsCount++;
			}

		}
	}

	public int getErrorsCount() {
		return errorsCount;
	}

	public long getQueryTime() {
		return queryTime;
	}

	public int getTiersCount() {
		return tiersCount;
	}

	public int getQueryCount() {
		return queryCount;
	}
}
