package ch.vd.uniregctb.webservices.tiers3.perfs;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.ArrayUtils;
import org.apache.log4j.Logger;

import ch.vd.registre.base.utils.NotImplementedException;
import ch.vd.unireg.webservices.party3.BatchParty;
import ch.vd.unireg.webservices.party3.GetBatchPartyRequest;
import ch.vd.unireg.webservices.party3.GetPartyRequest;
import ch.vd.unireg.webservices.party3.PartyPart;
import ch.vd.unireg.webservices.party3.PartyWebService;
import ch.vd.unireg.webservices.party3.SearchMode;
import ch.vd.unireg.webservices.party3.SearchPartyRequest;
import ch.vd.unireg.webservices.party3.WebServiceException;
import ch.vd.unireg.xml.common.v1.UserLogin;
import ch.vd.unireg.xml.party.person.v1.CommonHousehold;
import ch.vd.unireg.xml.party.person.v1.NaturalPerson;
import ch.vd.unireg.xml.party.v1.Party;
import ch.vd.uniregctb.common.ReflexionUtils;
import ch.vd.uniregctb.perfs.PerfsAccessFileIterator;

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
	private final PartyWebService service;

	private final PerfsAccessFileIterator ids;
	private int errorsCount;

	private final Query query;
	private final Integer batch;

	public static abstract class Query {

		protected final UserLogin login = new UserLogin();

		protected Set<PartyPart> parts = null;
		protected final FileWriter outputFilename;

		public Query(String operateur, int oid, FileWriter outputFilename) {
			this.outputFilename = outputFilename;
			login.setUserId(operateur);
			login.setOid(oid);
		}

		public void setParts(Set<PartyPart> parts) {
			this.parts = Collections.unmodifiableSet(parts);
		}

		public abstract Object execute(PartyWebService service, int id) throws WebServiceException;

		public abstract List<?> executeBatch(PartyWebService port, Collection<Integer> ids) throws WebServiceException;

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
				for (PartyPart e : parts) {
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

		protected void logToOutput(Object params, Object results) {
			if (outputFilename != null) {
				StringBuilder s = new StringBuilder();
				s.append(ReflexionUtils.toString(params, false)).append(" => ").append(ReflexionUtils.toString(results, false)).append('\n');
				try {
					outputFilename.write(s.toString());
				}
				catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		}
	}

	public static class GetQuery extends Query {

		public GetQuery(String operateur, int oid, FileWriter outputFilename) {
			super(operateur, oid, outputFilename);
		}

		@Override
		public Object execute(PartyWebService service, int id) throws WebServiceException {

			GetPartyRequest params = new GetPartyRequest();
			params.setLogin(login);
			params.setPartyNumber(id);
			if (parts != null) {
				params.getParts().addAll(parts);
			}

			final Party tiersHisto = service.getParty(params);
			logToOutput(params, tiersHisto);
			return tiersHisto;
		}

		@Override
		public List<?> executeBatch(PartyWebService port, Collection<Integer> ids) throws WebServiceException {
			GetBatchPartyRequest params = new GetBatchPartyRequest();
			params.setLogin(login);
			params.getPartyNumbers().addAll(ids);

			if (parts != null) {
				params.getParts().addAll(parts);
			}

			final BatchParty batch = port.getBatchParty(params);
			logToOutput(params, batch);
			return batch.getEntries();
		}

		@Override
		public String description() {
			return "operateur=" + getOperateurDescription() + ", histo, parts=" + getPartsDescription();
		}
	}

	public static class SearchQuery extends Query {

		public SearchQuery(String operateur, int oid, FileWriter outputFilename) {
			super(operateur, oid, outputFilename);
		}

		@Override
		public Object execute(PartyWebService service, int id) throws WebServiceException {

			// Récupère le tiers

			GetPartyRequest params = new GetPartyRequest();
			params.setLogin(login);
			params.setPartyNumber(id);
			params.getParts().add(PartyPart.HOUSEHOLD_MEMBERS);

			Party party = service.getParty(params);
			if (party == null) {
				return null;
			}

			// Effectue une recherche sur son nom courrier

			final String nomCourrier;
			if (party instanceof NaturalPerson) {
				NaturalPerson pp =(NaturalPerson) party;
				nomCourrier = pp.getIdentification().getFirstName() + " " + pp.getIdentification().getOfficialName();
			}
			else if (party instanceof CommonHousehold) {
				CommonHousehold mc = (CommonHousehold) party;
				NaturalPerson pp1 = mc.getMainTaxpayer();
				if (pp1 == null) {
					return null;
				}
				nomCourrier = pp1.getIdentification().getFirstName() + " " + pp1.getIdentification().getOfficialName();
			}
			else {
				return null;
			}

			SearchPartyRequest search = new SearchPartyRequest();
			search.setLogin(login);
			search.setSearchMode(SearchMode.CONTAINS);
			search.setContactName(nomCourrier);

			service.searchParty(search);

			return party; // on retourne le tiers, parce que le framework attend ça
		}

		@Override
		public List<?> executeBatch(PartyWebService port, Collection<Integer> ids) {
			throw new NotImplementedException();
		}

		@Override
		public String description() {
			return "operateur=" + getOperateurDescription() + ", get+search";
		}
	}

	public PerfsThread(PartyWebService service, Query query, Integer batch, PerfsAccessFileIterator ids) {
		this.service = service;
		LOGGER.info("[thread-" + this.getId() + "] port is " + service.toString());
		this.query = query;
		this.batch = batch;
		this.ids = ids;
		this.errorsCount = 0;
	}

	@Override
	public void run() {

		if (batch == null || batch < 2) {
			runNormal();
		}
		else {
			runBatch(batch);
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
				Object tiers = query.execute(service, id.intValue());

				long after = System.nanoTime();
				long delta = after - before;
				queryTime += delta;
				++queryCount;
				++tiersCount;

				if (LOGGER.isTraceEnabled()) {
					long ping = delta / ch.vd.uniregctb.webservices.tiers2.perfs.PerfsClient.NANO_TO_MILLI;
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
				List<?> tiers = query.executeBatch(service, toIntegerList(batch));

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

	private static List<Integer> toIntegerList(List<Long> list) {
		if (list == null || list.isEmpty()) {
			return Collections.emptyList();
		}
		final List<Integer> res = new ArrayList<Integer>(list.size());
		for (Long l : list) {
			res.add(l.intValue());
		}
		return res;
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
