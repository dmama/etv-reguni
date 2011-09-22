package ch.vd.unireg.wsclient.rcpers;

import java.util.Collection;

import org.apache.cxf.jaxrs.client.WebClient;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.evd0001.v2.ListOfPersons;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;

public class RcPersClientImpl implements RcPersClient, InitializingBean {

	private String baseUrl;
	private String username;
	private String password;
	private String peoplePath;

	public void setBaseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
	}

	public void setPeoplePath(String peoplePath) {
		this.peoplePath = peoplePath;
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
	public ListOfPersons getPeople(Collection<Long> ids, RegDate date) {

		final WebClient wc = WebClient.create(baseUrl, username, password, null);
		wc.path(peoplePath);

		// les ids
		int i = 0;
		for (Long id : ids) {
			if (i++ > 0) {
				wc.path(",");
			}
			wc.path(String.valueOf(id));
		}

		// la date
		if (date != null) {
			wc.path("?date=" + RegDateHelper.dateToDisplayString(date));
		}

		return wc.get(ListOfPersons.class);
	}
}
