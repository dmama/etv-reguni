package ch.vd.unireg.wsclient.rcpers;

import java.util.Collection;
import java.util.List;

import org.apache.cxf.jaxrs.client.WebClient;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.evd0001.v3.ListOfPersons;
import ch.vd.evd0001.v3.ListOfRelations;
import ch.vd.evd0001.v3.Person;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;

public class RcPersClientImpl implements RcPersClient, InitializingBean {

//	private static final Logger LOGGER = Logger.getLogger(RcPersClientImpl.class);

	private String baseUrl;
	private String username;
	private String password;
	private String peoplePath;
	private String relationsPath;
	private String eventPath;

	public void setBaseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
	}

	public void setPeoplePath(String peoplePath) {
		this.peoplePath = peoplePath;
	}

	public void setRelationsPath(String relationsPath) {
		this.relationsPath = relationsPath;
	}

	public void setEventPath(String eventPath) {
		this.eventPath = eventPath;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
	}

	@Override
	public ListOfPersons getPersons(Collection<Long> ids, RegDate date, boolean withHistory) {

		final WebClient wc = WebClient.create(baseUrl, username, password, null);
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

		return wc.get(ListOfPersons.class);
	}

	@Override
	public ListOfRelations getRelations(Collection<Long> ids, RegDate date, boolean withHistory) {
		final WebClient wc = WebClient.create(baseUrl, username, password, null);
		wc.path(relationsPath);

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

		return wc.get(ListOfRelations.class);
	}

	@Override
	public Person getPersonForEvent(long eventId) {
		final WebClient wc = WebClient.create(baseUrl, username, password, null);
		wc.path(eventPath);
		wc.path(String.valueOf(eventId));

		final ListOfPersons found = wc.get(ListOfPersons.class);
		Person p = null;
		if (found.getListOfResults() != null) {
			final List<Person> pList = found.getListOfResults().getPerson();
			if (pList != null && pList.size() > 0) {
				p = pList.get(0);
			}
		}

		return p;
	}
}
