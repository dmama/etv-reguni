package ch.vd.unireg.wsclient.rcpers;

import javax.ws.rs.WebApplicationException;
import java.util.Collection;

import ch.ech.ech0085.v1.GetInfoPersonRequest;
import ch.ech.ech0085.v1.GetInfoPersonResponse;
import org.apache.cxf.jaxrs.client.WebClient;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.evd0001.v5.Event;
import ch.vd.evd0001.v5.ListOfFoundPersons;
import ch.vd.evd0001.v5.ListOfPersons;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;

@SuppressWarnings("UnusedDeclaration")
public class RcPersClientImpl implements RcPersClient, InitializingBean {

//	private static final Logger LOGGER = Logger.getLogger(RcPersClientImpl.class);

	private WebClientPool wcPool = new WebClientPool();

	private String peoplePath;
	private String peopleBySocialNumberPath;
	private String peopleByEventIdPath;
	private String eventPath;
	private String searchPath;
	private String upiGetInfoPersonPath;

	public void setBaseUrl(String baseUrl) {
		this.wcPool.setBaseUrl(baseUrl);
	}

	public void setPeoplePath(String peoplePath) {
		this.peoplePath = peoplePath;
	}

	public void setPeopleBySocialNumberPath(String peopleBySocialNumberPath) {
		this.peopleBySocialNumberPath = peopleBySocialNumberPath;
	}

	public void setPeopleByEventIdPath(String peopleByEventIdPath) {
		this.peopleByEventIdPath = peopleByEventIdPath;
	}

	public void setEventPath(String eventPath) {
		this.eventPath = eventPath;
	}

	public void setSearchPath(String searchPath) {
		this.searchPath = searchPath;
	}

	public void setUpiGetInfoPersonPath(String upiGetInfoPersonPath) {
		this.upiGetInfoPersonPath = upiGetInfoPersonPath;
	}

	public void setUsername(String username) {
		this.wcPool.setUsername(username);
	}

	public void setPassword(String password) {
		this.wcPool.setPassword(password);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		this.wcPool.init();
	}

	@Override
	public ListOfPersons getPersons(Collection<Long> ids, RegDate date, boolean withHistory) {

		final WebClient wc = wcPool.borrowClient(600000); // 10 minutes
		try {
			wc.path(peoplePath);

			// les ids
			final StringBuilder param = new StringBuilder();
			int i = 0;
			for (Long id : ids) {
				if (i++ > 0) {
					param.append(",");
				}
				param.append(String.valueOf(id));
			}
			wc.path(param.toString());

			// l'historique
			if (withHistory) {
				wc.query("history", "true");
			}

			// la date
			if (date != null) {
				wc.query("date", RegDateHelper.dateToDisplayString(date));
			}

			try {
				return wc.get(ListOfPersons.class);
			}
			catch (WebApplicationException e) {
				throw new RcPersClientException(e);
			}
		}
		finally {
			wcPool.returnClient(wc);
		}
	}

	@Override
	public ListOfPersons getPersonByEvent(long evtId, RegDate date, boolean withHistory) {
		final WebClient wc = wcPool.borrowClient(600000); // 10 minutes
		try {
			wc.path(peopleByEventIdPath);

			// l'id de l'événement civil
			wc.path(Long.toString(evtId));

			// l'historique
			if (withHistory) {
				wc.query("history", "true");
			}

			// la date
			if (date != null) {
				wc.query("date", RegDateHelper.dateToDisplayString(date));
			}

			try {
				return wc.get(ListOfPersons.class);
			}
			catch (WebApplicationException e) {
				throw new RcPersClientException(e);
			}
		}
		finally {
			wcPool.returnClient(wc);
		}
	}

	@Override
	public ListOfPersons getPersonsBySocialsNumbers(Collection<String> numbers, RegDate date, boolean withHistory) {

		final WebClient wc = wcPool.borrowClient(600000); // 10 minutes
		try {
			wc.path(peopleBySocialNumberPath);

			// les ids
			final StringBuilder param = new StringBuilder();
			int i = 0;
			for (String number : numbers) {
				if (i++ > 0) {
					param.append(",");
				}
				param.append(number);
			}
			wc.path(param.toString());

			// l'historique
			if (withHistory) {
				wc.query("history", "true");
			}

			// la date
			if (date != null) {
				wc.query("date", RegDateHelper.dateToDisplayString(date));
			}

			try {
				return wc.get(ListOfPersons.class);
			}
			catch (WebApplicationException e) {
				throw new RcPersClientException(e);
			}
		}
		finally {
			wcPool.returnClient(wc);
		}
	}

	@Override
	public Event getEvent(long eventId) {
		final WebClient wc = wcPool.borrowClient(60000); // 1 minute
		try {
			wc.path(eventPath);
			wc.path(String.valueOf(eventId));

			try {
				return wc.get(Event.class);
			}
			catch (WebApplicationException e) {
				if (e.getResponse().getStatus() == 404) {
					// la ressource n'existe pas
					return null;
				}
				throw new RcPersClientException(e);
			}
		}
		finally {
			wcPool.returnClient(wc);
		}
	}

	@Override
	public ListOfFoundPersons findPersons(String sex, String firstNames, String officialName, String swissZipCode, String municipalityId, String dataSource, String contains, Boolean history,
	                                      String originalName, String alliancePartnershipName, String aliasName, Integer nationalityStatus, Integer nationalityCountryId, String town,
	                                      String passportName, String otherNames, RegDate birthDateFrom, RegDate birthDateTo) {
		final WebClient wc = wcPool.borrowClient(60000); // 1 minute
		try {
			wc.path(searchPath);
			addCriterion(wc, "sex", sex);
			addCriterion(wc, "firstNames", firstNames);
			addCriterion(wc, "officialName", officialName);
			addCriterion(wc, "swissZipCode", swissZipCode);
			addCriterion(wc, "municipalityId", municipalityId);
			addCriterion(wc, "dataSource", dataSource);
			addCriterion(wc, "contains", contains);
			addCriterion(wc, "history", history);
			addCriterion(wc, "originalName", originalName);
			addCriterion(wc, "alliancePartnershipName", alliancePartnershipName);
			addCriterion(wc, "aliasName", aliasName);
			addCriterion(wc, "nationalityStatus", nationalityStatus);
			addCriterion(wc, "nationalityCountryId", nationalityCountryId);
			addCriterion(wc, "town", town);
			addCriterion(wc, "passportName", passportName);
			addCriterion(wc, "otherNames", otherNames);
			addCriterion(wc, "dateOfBirthFrom", birthDateFrom == null ? null : RegDateHelper.dateToDisplayString(birthDateFrom));
			addCriterion(wc, "dateOfBirthTo", birthDateTo == null ? null : RegDateHelper.dateToDisplayString(birthDateTo));

			try {
				return wc.get(ListOfFoundPersons.class);
			}
			catch (WebApplicationException e) {
				throw new RcPersClientException(e);
			}
		}
		finally {
			wcPool.returnClient(wc);
		}
	}

	private static void addCriterion(WebClient wc, String key, String value) {
		if (value != null) {
			wc.query(key, value);
		}
	}

	private static void addCriterion(WebClient wc, String key, Boolean value) {
		if (value != null) {
			wc.query(key, value ? "1" : "0");
		}
	}

	private static void addCriterion(WebClient wc, String key, Integer value) {
		if (value != null) {
			wc.query(key, value);
		}
	}

	@Override
	public GetInfoPersonResponse getInfoPersonUpi(long noAvs13) {
		final WebClient wc = wcPool.borrowClient(60000);    // 1 minute
		try {
			wc.path(upiGetInfoPersonPath);

			final ch.ech.ech0085.v1.ObjectFactory objectFactory = new ch.ech.ech0085.v1.ObjectFactory();
			final GetInfoPersonRequest request = new GetInfoPersonRequest(noAvs13);
			return wc.post(objectFactory.createGetInfoPersonRequest(request), GetInfoPersonResponse.class);
		}
		catch (WebApplicationException e) {
			throw new RcPersClientException(e);
		}
		finally {
			wcPool.returnClient(wc);
		}
	}
}
