package ch.vd.unireg.wsclient.rcent;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.ByteArrayInputStream;
import java.util.List;

import org.apache.cxf.jaxrs.client.ServerWebApplicationException;
import org.apache.cxf.jaxrs.client.WebClient;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.evd0004.v3.Error;
import ch.vd.evd0004.v3.Errors;
import ch.vd.evd0022.v3.OrganisationData;
import ch.vd.evd0022.v3.OrganisationsOfNotice;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;

public class RcEntClientImpl implements RcEntClient, InitializingBean {

	private static final int RECEIVE_TIMEOUT = 600000; // 10 minutes

	private Unmarshaller errorunmarshaller;

	private final WebClientPool wcPool = new WebClientPool();

	private String organisationPath = "organisation/CT.VD.PARTY";
	private String organisationsOfNoticePath = "organisationsOfNotice";
	private String pingPath = "infrastructure/ping";

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

	public void setPingPath(String pingPath) {
		this.pingPath = pingPath;
	}

	public void setValidationEnabled(boolean enableValidation) {
		this.wcPool.setEnableValidation(enableValidation);
	}

	public void setSchemasLocations(List<String> schemasLocations) {
		this.wcPool.setSchemasLocations(schemasLocations);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		this.wcPool.init();

		JAXBContext jc = JAXBContext.newInstance(Errors.class);
		errorunmarshaller = jc.createUnmarshaller();
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
				final String message = e.getMessage();
				List<Error> errors = parseErrors(message);
				throw new RcEntClientException(e, errors);
			}
		}
		finally {
			wcPool.returnClient(wc);
		}
	}

	@Nullable
	protected List<Error> parseErrors(String message) {
		List<Error> errors = null;
		try {
			Errors unmarshalled = (Errors) errorunmarshaller.unmarshal(new ByteArrayInputStream(message.getBytes()));
			errors = unmarshalled.getError();
		}
		catch (JAXBException e1) {
		}
		return errors;
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
				List<Error> errors = parseErrors(e.getMessage());
				throw new RcEntClientException(e, errors);
			}
		}
		finally {
			wcPool.returnClient(wc);
		}
	}

	@Override
	public void ping() throws RcEntClientException {
		final WebClient wc = wcPool.borrowClient(RECEIVE_TIMEOUT);
		try {
			wc.path(pingPath);
			try {
				final String pong = wc.get(String.class);
				if (!"pong".equals(pong)) {
					throw new RcEntClientException("Wrong answer...", null);
				}
			}
			catch (ServerWebApplicationException e) {
				List<Error> errors = parseErrors(e.getMessage());
				throw new RcEntClientException(e, errors);
			}
		}
		finally {
			wcPool.returnClient(wc);
		}
	}
}
