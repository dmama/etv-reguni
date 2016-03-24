package ch.vd.unireg.wsclient.rcent;

import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.cxf.jaxrs.client.ServerWebApplicationException;
import org.apache.cxf.jaxrs.client.WebClient;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.evd0004.v3.Error;
import ch.vd.evd0004.v3.Errors;
import ch.vd.evd0022.v3.OrganisationData;
import ch.vd.evd0022.v3.OrganisationsOfNotice;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;

public class RcEntClientImpl implements RcEntClient, InitializingBean {

	private static final int RECEIVE_TIMEOUT = 600000; // 10 minutes

	private static final Logger LOGGER = LoggerFactory.getLogger(RcEntClientImpl.class);

	private Unmarshaller errorunmarshaller;

	private final WebClientPool wcPool = new WebClientPool();

	private String organisationPath = "organisation/CT.VD.PARTY";
	private String organisationsOfNoticePath = "organisationsOfNotice";
	private String pingPath = "infrastructure/ping";
	private String findByNoIDEPath = "organisation/CH.IDE";

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

	public void setFindByNoIDEPath(String findByNoIDEPath) {
		this.findByNoIDEPath = findByNoIDEPath;
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

		final JAXBContext jc = JAXBContext.newInstance(Errors.class);
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
				throw new RcEntClientException(e, parseErrors(e.getMessage()));
			}
		}
		finally {
			wcPool.returnClient(wc);
		}
	}

	@Override
	public OrganisationData getOrganisationByNoIDE(String noide, RegDate referenceDate, boolean withHistory) throws RcEntClientException {
		final WebClient wc = wcPool.borrowClient(RECEIVE_TIMEOUT);
		try {
			wc.path(findByNoIDEPath);
			wc.path(noide);

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
				throw new RcEntClientException(e, parseErrors(e.getMessage()));
			}
		}
		finally {
			wcPool.returnClient(wc);
		}
	}

	@Nullable
	protected List<RcEntClientErrorMessage> parseErrors(String message) {
		try {
			final Errors unmarshalled = (Errors) errorunmarshaller.unmarshal(new ByteArrayInputStream(message.getBytes()));
			if (unmarshalled == null || unmarshalled.getError() == null || unmarshalled.getError().isEmpty()) {
				return null;
			}
			final List<RcEntClientErrorMessage> messages = new ArrayList<>(unmarshalled.getError().size());
			for (Error error : unmarshalled.getError()) {
				messages.add(new RcEntClientErrorMessage(error));
			}
			return messages;
		}
		catch (JAXBException e) {
			LOGGER.error("Impossible de parser le message d'erreur revenu en tant que eVD-0004", e);
			return null;
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
				final Response response = wc.get();
				if (response.getStatus() >= 400) {
					throw new ServerWebApplicationException(response);
				}

				final JAXBContext jaxbContext = JAXBContext.newInstance(ch.vd.evd0023.v3.ObjectFactory.class);
				final Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

				//noinspection unchecked
				final JAXBElement<OrganisationsOfNotice> data = (JAXBElement<OrganisationsOfNotice>) unmarshaller.unmarshal((InputStream) response.getEntity());
				return data.getValue();
			}
			catch (ServerWebApplicationException e) {
				throw new RcEntClientException(e, parseErrors(e.getMessage()));
			}
		}
		catch (JAXBException e) {
			throw new RcEntClientException("Erreur lors du parsing de la réponse xml", e);
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
				throw new RcEntClientException(e, parseErrors(e.getMessage()));
			}
		}
		finally {
			wcPool.returnClient(wc);
		}
	}
}
