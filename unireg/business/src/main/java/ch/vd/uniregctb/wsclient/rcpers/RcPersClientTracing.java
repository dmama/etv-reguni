package ch.vd.uniregctb.wsclient.rcpers;

import java.util.Collection;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.evd0001.v3.ListOfFoundPersons;
import ch.vd.evd0001.v3.ListOfPersons;
import ch.vd.evd0001.v3.ListOfRelations;
import ch.vd.evd0006.v1.Event;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.wsclient.rcpers.RcPersClient;
import ch.vd.uniregctb.stats.ServiceTracing;
import ch.vd.uniregctb.stats.StatsService;

public class RcPersClientTracing implements RcPersClient, InitializingBean, DisposableBean {

	public static final String SERVICE_NAME = "RcPersClient";

	private RcPersClient target;

	private StatsService statsService;

	private final ServiceTracing tracing = new ServiceTracing(SERVICE_NAME);

	public void setTarget(RcPersClient target) {
		this.target = target;
	}

	public void setStatsService(StatsService statsService) {
		this.statsService = statsService;
	}

	@Override
	public ListOfPersons getPersons(final Collection<Long> ids, final RegDate date, final boolean withHistory) {
		Throwable t = null;
		int items = 0;
		final long time = tracing.start();
		try {
			final ListOfPersons list = target.getPersons(ids, date, withHistory);
			items = list == null ? 0 : list.getNumberOfResults().intValue();
			return list;
		}
		catch (RuntimeException e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getPersons", items, new Object() {
				@Override
				public String toString() {
					return String.format("ids=%s, date=%s, withHistory=%s", ServiceTracing.toString(ids), ServiceTracing.toString(date), withHistory);
				}
			});
		}
	}

	@Override
	public ListOfPersons getPersonsBySocialsNumbers(final Collection<String> numbers, final RegDate date, final boolean withHistory) {
		Throwable t = null;
		int items = 0;
		final long time = tracing.start();
		try {
			final ListOfPersons list = target.getPersonsBySocialsNumbers(numbers, date, withHistory);
			items = list == null ? 0 : list.getNumberOfResults().intValue();
			return list;
		}
		catch (RuntimeException e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getPersonsBySocialsNumbers", items, new Object() {
				@Override
				public String toString() {
					return String.format("numbers=%s, date=%s, withHistory=%s", ServiceTracing.toString(numbers), ServiceTracing.toString(date), withHistory);
				}
			});
		}
	}

	@Override
	public ListOfRelations getRelations(final Collection<Long> ids, final RegDate date, final boolean withHistory) {
		Throwable t = null;
		int items = 0;
		final long time = tracing.start();
		try {
			final ListOfRelations list = target.getRelations(ids, date, withHistory);
			items = list == null ? 0 : list.getNumberOfResults().intValue();
			return list;
		}
		catch (RuntimeException e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getRelations", items, new Object() {
				@Override
				public String toString() {
					return String.format("ids=%s, date=%s, withHistory=%s", ServiceTracing.toString(ids), ServiceTracing.toString(date), withHistory);
				}
			});
		}
	}

	@Override
	public Event getEvent(final long eventId) {
		Throwable t = null;
		final long time = tracing.start();
		try {
			return target.getEvent(eventId);
		}
		catch (RuntimeException e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getEvent", new Object() {
				@Override
				public String toString() {
					return String.format("eventId=%d", eventId);
				}
			});
		}
	}

	@Override
	public ListOfFoundPersons findPersons(final String sex, final String firstNames, final String officialName, final String swissZipCode, final String municipalityId, final String dataSource,
	                                      final String contains, final Boolean history,
	                                      final String originalName, final String alliancePartnershipName, final String aliasName, final Integer nationalityStatus,
	                                      final Integer nationalityCountryId, final String town,
	                                      final String passportName, final String otherNames, final RegDate birthDateFrom, final RegDate birthDateTo) {
		Throwable t = null;
		final long time = tracing.start();
		try {
			return target.findPersons(sex, firstNames, officialName, swissZipCode, municipalityId, dataSource, contains, history, originalName, alliancePartnershipName, aliasName,
					nationalityStatus, nationalityCountryId, town, passportName, otherNames, birthDateFrom, birthDateTo);
		}
		catch (RuntimeException e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "findPersons", new Object() {
				@Override
				public String toString() {
					StringBuilder s = new StringBuilder();
					addCriterion(s, "sex", sex);
					addCriterion(s, "firstNames", firstNames);
					addCriterion(s, "officialName", officialName);
					addCriterion(s, "swissZipCode", swissZipCode);
					addCriterion(s, "municipalityId", municipalityId);
					addCriterion(s, "dataSource", dataSource);
					addCriterion(s, "contains", contains);
					addCriterion(s, "history", history);
					addCriterion(s, "originalName", originalName);
					addCriterion(s, "alliancePartnershipName", alliancePartnershipName);
					addCriterion(s, "aliasName", aliasName);
					addCriterion(s, "nationalityStatus", nationalityStatus);
					addCriterion(s, "nationalityCountryId", nationalityCountryId);
					addCriterion(s, "town", town);
					addCriterion(s, "passportName", passportName);
					addCriterion(s, "otherNames", otherNames);
					addCriterion(s, "birthDateFrom", birthDateFrom == null ? null : RegDateHelper.dateToDisplayString(birthDateFrom));
					addCriterion(s, "birthDateTo", birthDateTo == null ? null : RegDateHelper.dateToDisplayString(birthDateTo));
					return s.toString();
				}

				private void addCriterion(StringBuilder s, String key, Object value) {
					if (value != null) {
						if (s.length() > 0) {
							s.append(", ");
						}
						s.append(key).append('=').append(value);
					}
				}
			});
		}
	}

	@Override
	public void destroy() throws Exception {
		if (statsService != null) {
			statsService.unregisterService(SERVICE_NAME);
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (statsService != null) {
			statsService.registerService(SERVICE_NAME, tracing);
		}
	}
}
