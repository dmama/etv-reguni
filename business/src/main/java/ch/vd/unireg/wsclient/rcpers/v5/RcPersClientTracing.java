package ch.vd.unireg.wsclient.rcpers.v5;

import java.util.Collection;
import java.util.function.Supplier;

import ch.ech.ech0085.v1.GetInfoPersonResponse;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.evd0001.v5.Event;
import ch.vd.evd0001.v5.ListOfFoundPersons;
import ch.vd.evd0001.v5.ListOfPersons;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.stats.ServiceTracing;
import ch.vd.unireg.stats.StatsService;

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
		catch (RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getPersons", items, () -> String.format("ids=%s, date=%s, withHistory=%s", ServiceTracing.toString(ids), ServiceTracing.toString(date), withHistory));
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
		catch (RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getPersonsBySocialsNumbers", items, () -> String.format("numbers=%s, date=%s, withHistory=%s", ServiceTracing.toString(numbers), ServiceTracing.toString(date), withHistory));
		}
	}

	@Override
	public ListOfPersons getPersonByEvent(final long evtId, final RegDate date, final boolean withHistory) {
		Throwable t = null;
		int items = 0;
		final long time = tracing.start();
		try {
			final ListOfPersons list = target.getPersonByEvent(evtId, date, withHistory);
			items = list == null ? 0 : list.getNumberOfResults().intValue();
			return list;
		}
		catch (RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getPersonByEvent", items, () -> String.format("evtId=%d, date=%s, withHistory=%s", evtId, ServiceTracing.toString(date), withHistory));
		}
	}

	@Override
	public Event getEvent(final long eventId) {
		Throwable t = null;
		int items = 0;
		final long time = tracing.start();
		try {
			final Event event = target.getEvent(eventId);
			if (event != null) {
				items = 1;
			}
			return event;
		}
		catch (RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getEvent", items, () -> String.format("eventId=%d", eventId));
		}
	}

	@Override
	public ListOfFoundPersons findPersons(final String sex, final String firstNames, final String officialName, final String swissZipCode, final String municipalityId, final String dataSource,
	                                      final String contains, final Boolean history,
	                                      final String originalName, final String alliancePartnershipName, final String aliasName, final Integer nationalityStatus,
	                                      final Integer nationalityCountryId, final String town,
	                                      final String passportName, final String otherNames, final RegDate birthDateFrom, final RegDate birthDateTo) {
		Throwable t = null;
		int items = 0;
		final long time = tracing.start();
		try {
			final ListOfFoundPersons list =
					target.findPersons(sex, firstNames, officialName, swissZipCode, municipalityId, dataSource, contains, history, originalName, alliancePartnershipName, aliasName,
							nationalityStatus, nationalityCountryId, town, passportName, otherNames, birthDateFrom, birthDateTo);
			if (list != null) {
				items = list.getNumberOfResults().intValue();
			}
			return list;
		}
		catch (RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "findPersons", items, new Supplier<String>() {
				@Override
				public String get() {
					final StringBuilder s = new StringBuilder();
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
	public GetInfoPersonResponse getInfoPersonUpi(final long avs13) {
		Throwable t = null;
		final long time = tracing.start();
		try {
			return target.getInfoPersonUpi(avs13);
		}
		catch (RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getInfoPersonUpi", () -> String.format("avs13=%d", avs13));
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
