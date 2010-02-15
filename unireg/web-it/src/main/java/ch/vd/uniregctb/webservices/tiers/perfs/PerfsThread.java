package ch.vd.uniregctb.webservices.tiers.perfs;

import java.util.Collections;
import java.util.Set;

import org.apache.log4j.Logger;

import ch.vd.uniregctb.perfs.PerfsAccessFileIterator;
import ch.vd.uniregctb.webservices.tiers.Date;
import ch.vd.uniregctb.webservices.tiers.GetTiers;
import ch.vd.uniregctb.webservices.tiers.GetTiersHisto;
import ch.vd.uniregctb.webservices.tiers.GetTiersPeriode;
import ch.vd.uniregctb.webservices.tiers.Tiers;
import ch.vd.uniregctb.webservices.tiers.TiersHisto;
import ch.vd.uniregctb.webservices.tiers.TiersPart;
import ch.vd.uniregctb.webservices.tiers.TiersPort;
import ch.vd.uniregctb.webservices.tiers.UserLogin;
import ch.vd.uniregctb.webservices.tiers.WebServiceException_Exception;

/**
 * Thread qui exécute un certain nombre de requêtes au web-service, puis s'arrête.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class PerfsThread extends Thread {

	private static final Logger LOGGER = Logger.getLogger(PerfsThread.class);

	private long queryTime = 0;
	private int queryCount = 0;
	private final TiersPort port;

	private final PerfsAccessFileIterator ids;
	private int errorsCount;

	private final Query query;

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

		public abstract Object execute(TiersPort port, long id) throws WebServiceException_Exception;

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
		public Object execute(TiersPort port, long id) throws WebServiceException_Exception {

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
		public String description() {
			return "operateur=" + getOperateurDescription() + ", date=" + date.getYear() + "." + date.getMonth() + "." + date.getDay() + ", parts=" + getPartsDescription();
		}
	}

	public static class PeriodeQuery extends Query {
		private final int annee;

		public PeriodeQuery(int annee, String operateur, int oid) {
			super(operateur, oid);
			this.annee = annee;
		}

		@Override
		public Object execute(TiersPort port, long id) throws WebServiceException_Exception {

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
		public String description() {
			return "operateur=" + getOperateurDescription() + ", periode=" + annee + ", parts=" + getPartsDescription();
		}
	}

	public static class HistoQuery extends Query {

		public HistoQuery(String operateur, int oid) {
			super(operateur, oid);
		}

		@Override
		public Object execute(TiersPort port, long id) throws WebServiceException_Exception {

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
		public String description() {
			return "operateur=" + getOperateurDescription() + ", histo, parts=" + getPartsDescription();
		}
	}

	public PerfsThread(TiersPort port, Query query, PerfsAccessFileIterator ids) {
		this.port = port;
		LOGGER.info("[thread-" + this.getId() + "] port is " + port.toString());
		this.query = query;
		this.ids = ids;
		this.errorsCount = 0;
	}

	@Override
	public void run() {

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

	public int getErrorsCount() {
		return errorsCount;
	}

	public long getQueryTime() {
		return queryTime;
	}

	public int getQueryCount() {
		return queryCount;
	}
}
