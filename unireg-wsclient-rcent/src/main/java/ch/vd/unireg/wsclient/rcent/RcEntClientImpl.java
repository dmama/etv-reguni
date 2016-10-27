package ch.vd.unireg.wsclient.rcent;

import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.apache.cxf.jaxrs.client.ServerWebApplicationException;
import org.apache.cxf.jaxrs.client.WebClient;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import ch.vd.evd0004.v3.Error;
import ch.vd.evd0004.v3.Errors;
import ch.vd.evd0022.v3.NoticeRequest;
import ch.vd.evd0022.v3.NoticeRequestReport;
import ch.vd.evd0022.v3.NoticeRequestStatusCode;
import ch.vd.evd0022.v3.OrganisationData;
import ch.vd.evd0022.v3.OrganisationsOfNotice;
import ch.vd.evd0022.v3.TypeOfNoticeRequest;
import ch.vd.evd0023.v3.ListOfNoticeRequest;
import ch.vd.evd0023.v3.ObjectFactory;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;

public class RcEntClientImpl implements RcEntClient, InitializingBean {

	private static final int RECEIVE_TIMEOUT = 600000; // 10 minutes

	private static final Logger LOGGER = LoggerFactory.getLogger(RcEntClientImpl.class);

	private JAXBContext jaxbContext;

	private Unmarshaller errorunmarshaller;

	private final WebClientPool wcPool = new WebClientPool();

	private String organisationPath = "organisation/CT.VD.PARTY";
	private String organisationsOfNoticePath = "organisationsOfNotice";
	private String pingPath = "infrastructure/ping";
	private String findByNoIDEPath = "organisation/CH.IDE";
	private String noticeRequestValidatePath = "noticeRequestValidate/"; // Le '/' supplémentaire est nécessaire au bon fonctionnement de l'appel.
	private String noticeRequestListPath = "noticeRequestList";

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

	public void setNoticeRequestValidatePath(String noticeRequestValidatePath) {
		this.noticeRequestValidatePath = noticeRequestValidatePath;
	}

	public void setNoticeRequestListPath(String noticeRequestListPath) {
		this.noticeRequestListPath = noticeRequestListPath;
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

		jaxbContext = JAXBContext.newInstance(ch.vd.evd0023.v3.ObjectFactory.class);
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

	@Nullable
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
				// au contraire de la récupération par numéro cantonal, la récupération par
				// numéro IDE peut ne pas aboutir, et ce de manière tout-à-fait légitime
				// (en particulier car les numéros IDE peuvent être/avoir été saisis à la main dans Unireg/RegPM)
				if (e.getStatus() == HttpURLConnection.HTTP_NOT_FOUND) {
					return null;
				}
				throw new RcEntClientException(e, parseErrors(e.getMessage()));
			}
		}
		finally {
			wcPool.returnClient(wc);
		}
	}

	@Override
	public NoticeRequestReport validateNoticeRequest(NoticeRequest noticeRequest) throws RcEntClientException {
		final WebClient wc = wcPool.borrowClient(RECEIVE_TIMEOUT);
		try {
			wc.path(noticeRequestValidatePath);

			try {
				final Response response = wc.post(new ObjectFactory().createNoticeRequest(noticeRequest));
				if (response.getStatus() >= 400) {
					throw new ServerWebApplicationException(response);
				}

				final Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

				//noinspection unchecked
				final JAXBElement<NoticeRequestReport> data = (JAXBElement<NoticeRequestReport>) unmarshaller.unmarshal((InputStream) response.getEntity());
				return data.getValue();
			}
			catch (ServerWebApplicationException e) {
				throw new RcEntClientException(e, parseErrors(e.getMessage()));
			}
			catch (JAXBException e) {
				throw new RcEntClientException("Erreur lors du parsing de la réponse xml", e);
			}
		}
		finally {
			wcPool.returnClient(wc);
		}
	}

	@Nullable
	@Override
	public ListOfNoticeRequest getNoticeRequest(String noticeRequestId) throws RcEntClientException {
		final WebClient wc = wcPool.borrowClient(RECEIVE_TIMEOUT);
		try {
			wc.path(noticeRequestListPath);

			if (noticeRequestId == null) {
				throw new IllegalArgumentException("L'identifiant de la demande d'annonce doit être fourni.");
			}
			wc.query("noticeRequestId", noticeRequestId);

			try {
				return wc.get(ListOfNoticeRequest.class);
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
	public Page<NoticeRequestReport> findNotices(@NotNull RcEntNoticeQuery query, @Nullable Sort.Order order, int pageNumber, int resultsPerPage) throws
			RcEntClientException {

		final WebClient wc = wcPool.borrowClient(RECEIVE_TIMEOUT);
		try {
			// on construit la requête
			wc.path(noticeRequestListPath);

			addParam(wc, "noticeRequestId", query.getNoticeId());
			addParam(wc, "typeOfNoticeRequest", query.getType());
			addParam(wc, "status", query.getStatus());
			addParam(wc, "cantonalId", query.getCantonalId());
			addParam(wc, "userId", query.getUserId());
			addParam(wc, "name", query.getName());
			addParam(wc, "dateFrom", query.getDateFrom());
			addParam(wc, "dateTo", query.getDateTo());
			addParam(wc, "contains", query.getContainsForName());
			if (order != null) {
				addParam(wc, "orderBy", order.getProperty());
				addParam(wc, "order", order.getDirection());
			}
			wc.query("numberOfResultsPerPage", resultsPerPage);
			wc.query("page", pageNumber);

			try {
				// on fait l'appel
				final ListOfNoticeRequest list = wc.get(ListOfNoticeRequest.class);

				// on retourne les résultats
				final Sort sort = (order == null ? null : new Sort(order));
				final PageRequest pageable = new PageRequest(pageNumber - 1, resultsPerPage, sort);
				return new PageImpl<>(list.getResults(), pageable, (long) list.getTotalNumberOfResults());
			}
			catch (ServerWebApplicationException e) {
				final List<RcEntClientErrorMessage> errors = parseErrors(e.getMessage());
				throw new RcEntClientException(e, errors);
			}
		}
		finally {
			wcPool.returnClient(wc);
		}
	}

	private static void addParam(@NotNull WebClient wc, @NotNull String name, @Nullable Object value) {
		if (value != null) {
			wc.query(name, value);
		}
	}

	private static void addParam(@NotNull WebClient wc, @NotNull String name, @Nullable String value) {
		if (StringUtils.isNotBlank(value)) {
			wc.query(name, value);
		}
	}

	private static void addParam(@NotNull WebClient wc, @NotNull String name, @Nullable RegDate value) {
		if (value != null) {
			wc.query(name, RegDateHelper.dateToDisplayString(value));
		}
	}

	private static void addParam(@NotNull WebClient wc, @NotNull String name, @Nullable TypeOfNoticeRequest type) {
		if (type != null) {
			wc.query(name, type.value());
		}
	}

	private static void addParam(@NotNull WebClient wc, @NotNull String name, @Nullable NoticeRequestStatusCode statuses[]) {
		if (statuses != null) {
			// [SIFISC-21627] RCEnt utilise des virgules pour séparer les types de statuts, au lieu de répéter le paramètre.
			final String value = String.join(",", Arrays.stream(statuses).map(s -> s.value()).collect(Collectors.toList()));
			wc.query(name, value);
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
		catch (JAXBException | RuntimeException e) {
			LOGGER.trace("Impossible de parser le message d'erreur revenu en tant que eVD-0004", e);
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
