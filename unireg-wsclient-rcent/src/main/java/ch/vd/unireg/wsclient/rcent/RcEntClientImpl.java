package ch.vd.unireg.wsclient.rcent;

import org.apache.cxf.jaxrs.client.ServerWebApplicationException;
import org.apache.cxf.jaxrs.client.WebClient;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.evd0022.v1.OrganisationData;
import ch.vd.evd0022.v1.OrganisationsOfNotice;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;

public class RcEntClientImpl implements RcEntClient, InitializingBean {

	private static final int RECEIVE_TIMEOUT = 600000; // 10 minutes

	private final WebClientPool wcPool = new WebClientPool();

	private String organisationPath = "organisation/ct.vd.party";
	private String organisationsOfNoticePath = "organisationsOfNotice";

	public void setBaseUrl(String url) {
		this.wcPool.setBaseUrl(url);
	}

	public void setUsername(String username) {
		this.wcPool.setUsername(username);
	}

	public void setPassword(String pwd) {
		this.wcPool.setPassword(pwd);
	}

	public void setOrganisationPath(String organisationPath) {
		this.organisationPath = organisationPath;
	}

	public void setOrganisationsOfNoticePath(String organisationsOfNoticePath) {
		this.organisationsOfNoticePath = organisationsOfNoticePath;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		this.wcPool.init();
	}

	@Override
	public OrganisationData getOrganisation(long id, RegDate referenceDate, boolean withHistory) throws RcEntClientException {
		final WebClient wc = wcPool.borrowClient(RECEIVE_TIMEOUT);
		try {
			wc.path(organisationPath);
			wc.path(Long.toString(id));

			if (withHistory) {
				wc.query("history", "true");
			}
			else {
				final RegDate effectiveReferenceDate = referenceDate == null ? RegDate.get() : referenceDate;
				wc.query("date", RegDateHelper.dateToDisplayString(effectiveReferenceDate));
			}

			try {
				return wc.get(OrganisationData.class);
			}
			catch (ServerWebApplicationException e) {
				throw new RcEntClientException(e);
			}
		}
		finally {
			wcPool.returnClient(wc);
		}
	}
	@Override
	public OrganisationsOfNotice getOrganisationsOfNotice(long noticeId, OrganisationState when) throws RcEntClientException {
		final WebClient wc = wcPool.borrowClient(RECEIVE_TIMEOUT);
		try {
			wc.path(organisationsOfNoticePath);
			wc.path(Long.toString(noticeId));
			wc.query("organisationsState", when != null ? when.toString() : null);

			try {
				return wc.get(OrganisationsOfNotice.class);
			}
			catch (ServerWebApplicationException e) {
				throw new RcEntClientException(e);
			}
		}
		finally {
			wcPool.returnClient(wc);
		}
	}
}
