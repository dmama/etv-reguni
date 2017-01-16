package ch.vd.uniregctb.webservices.v7;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.io.ByteArrayOutputStream;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.NotImplementedException;
import ch.vd.unireg.ws.ack.v7.OrdinaryTaxDeclarationAckRequest;
import ch.vd.unireg.ws.ack.v7.OrdinaryTaxDeclarationAckResponse;
import ch.vd.unireg.ws.deadline.v7.DeadlineRequest;
import ch.vd.unireg.ws.deadline.v7.DeadlineResponse;
import ch.vd.unireg.ws.fiscalevents.v7.FiscalEvents;
import ch.vd.unireg.ws.modifiedtaxpayers.v7.PartyNumberList;
import ch.vd.unireg.ws.parties.v7.Entry;
import ch.vd.unireg.ws.parties.v7.Parties;
import ch.vd.unireg.ws.search.party.v7.SearchResult;
import ch.vd.unireg.ws.security.v7.SecurityResponse;
import ch.vd.unireg.xml.error.v1.Error;
import ch.vd.unireg.xml.error.v1.ErrorType;
import ch.vd.unireg.xml.exception.v1.AccessDeniedExceptionInfo;
import ch.vd.unireg.xml.exception.v1.BusinessExceptionInfo;
import ch.vd.unireg.xml.infra.taxoffices.v1.TaxOffices;
import ch.vd.unireg.xml.party.landregistry.v1.Building;
import ch.vd.unireg.xml.party.landregistry.v1.ImmovableProperty;
import ch.vd.unireg.xml.party.v5.Party;
import ch.vd.unireg.xml.party.v5.PartyInfo;
import ch.vd.unireg.xml.party.v5.PartyPart;
import ch.vd.unireg.xml.party.withholding.v1.DebtorCategory;
import ch.vd.unireg.xml.party.withholding.v1.DebtorInfo;
import ch.vd.uniregctb.avatar.ImageData;
import ch.vd.uniregctb.common.ObjectNotFoundException;
import ch.vd.uniregctb.indexer.IndexerException;
import ch.vd.uniregctb.load.DetailedLoadMeter;
import ch.vd.uniregctb.stats.DetailedLoadMonitorable;
import ch.vd.uniregctb.stats.LoadDetail;
import ch.vd.uniregctb.webservices.common.AccessDeniedException;
import ch.vd.uniregctb.webservices.common.UserLogin;
import ch.vd.uniregctb.webservices.common.WebServiceHelper;
import ch.vd.uniregctb.xml.ServiceException;

@SuppressWarnings("Duplicates")
public class WebServiceEndPoint implements WebService, DetailedLoadMonitorable {

	private static final Logger LOGGER = LoggerFactory.getLogger(WebServiceEndPoint.class);
	private static final Logger READ_ACCESS_LOG = LoggerFactory.getLogger("ws.v7.read");
	private static final Logger WRITE_ACCESS_LOG = LoggerFactory.getLogger("ws.v7.write");

	private static final Pattern BOOLEAN_PATTERN = Pattern.compile("(true|false)", Pattern.CASE_INSENSITIVE);

	@Context
	private MessageContext messageContext;

	/**
	 * Moniteur des appels en cours
	 */
	private final DetailedLoadMeter<Object> loadMeter = new DetailedLoadMeter<>();

	private final ch.vd.unireg.ws.security.v7.ObjectFactory securityObjectFactory = new ch.vd.unireg.ws.security.v7.ObjectFactory();
	private final ch.vd.unireg.ws.ack.v7.ObjectFactory ackObjectFactory = new ch.vd.unireg.ws.ack.v7.ObjectFactory();
	private final ch.vd.unireg.ws.deadline.v7.ObjectFactory deadlineObjectFactory = new ch.vd.unireg.ws.deadline.v7.ObjectFactory();
	private final ch.vd.unireg.ws.taxoffices.v7.ObjectFactory taxOfficesObjectFactory = new ch.vd.unireg.ws.taxoffices.v7.ObjectFactory();
	private final ch.vd.unireg.ws.modifiedtaxpayers.v7.ObjectFactory modifiedTaxPayersFactory = new ch.vd.unireg.ws.modifiedtaxpayers.v7.ObjectFactory();
	private final ch.vd.unireg.ws.debtorinfo.v7.ObjectFactory debtorInfoFactory = new ch.vd.unireg.ws.debtorinfo.v7.ObjectFactory();
	private final ch.vd.unireg.ws.search.party.v7.ObjectFactory searchPartyObjectFactory = new ch.vd.unireg.ws.search.party.v7.ObjectFactory();
	private final ch.vd.unireg.ws.party.v7.ObjectFactory partyObjectFactory = new ch.vd.unireg.ws.party.v7.ObjectFactory();
	private final ch.vd.unireg.ws.fiscalevents.v7.ObjectFactory fiscalEventsObjectFactory = new ch.vd.unireg.ws.fiscalevents.v7.ObjectFactory();
	private final ch.vd.unireg.ws.landregistry.v7.ObjectFactory landRegistryObjectFactory = new ch.vd.unireg.ws.landregistry.v7.ObjectFactory();

	private BusinessWebService target;

	@Override
	public List<LoadDetail> getLoadDetails() {
		return loadMeter.getLoadDetails();
	}

	@Override
	public int getLoad() {
		return loadMeter.getLoad();
	}

	public void setTarget(BusinessWebService target) {
		this.target = target;
	}

	private static class ExecutionResult {
		public final Response response;
		public final Integer nbItems;

		private ExecutionResult(@NotNull Response response, @Nullable Integer nbItems) {
			this.response = response;
			this.nbItems = nbItems;
		}

		public static ExecutionResult with(@NotNull Response response) {
			return new ExecutionResult(response, null);
		}

		public static ExecutionResult with(@NotNull Response response, int nbItems) {
			return new ExecutionResult(response, nbItems);
		}
	}

	private interface ExecutionCallback {
		@NotNull
		ExecutionResult execute() throws Exception;
	}

	private Response execute(Object callDescription, Logger accessLog, ExecutionCallback callback) {
		Throwable t = null;
		Response r = null;
		Integer nbItems = null;
		final long start = loadMeter.start(callDescription);
		try {
			final ExecutionResult er = callback.execute();
			r = er.response;
			nbItems = er.nbItems;
		}
		catch (AccessDeniedException e) {
			t = e;
			LOGGER.error(e.getMessage());
			r = WebServiceHelper.buildErrorResponse(Response.Status.FORBIDDEN, getAcceptableMediaTypes(), ErrorType.ACCESS, e);
		}
		catch (ObjectNotFoundException e) {
			t = e;
			LOGGER.error(e.getMessage());
			r = WebServiceHelper.buildErrorResponse(Response.Status.NOT_FOUND, getAcceptableMediaTypes(), ErrorType.BUSINESS, e);
		}
		catch (BadRequestException e) {
			t = e;
			LOGGER.error(e.getMessage());
			r = WebServiceHelper.buildErrorResponse(Response.Status.BAD_REQUEST, getAcceptableMediaTypes(), ErrorType.TECHNICAL, e);
		}
		catch (Throwable e) {
			t = e;
			LOGGER.error(e.getMessage(), e);
			r = WebServiceHelper.buildErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, getAcceptableMediaTypes(), ErrorType.TECHNICAL, e);
		}
		finally {
			final long end = loadMeter.end();
			final Response.Status status = (r == null ? null : Response.Status.fromStatusCode(r.getStatus()));
			final String type = extractContentType(r);
			WebServiceHelper.logAccessInfo(accessLog, messageContext.getHttpServletRequest(), callDescription, end - start, getLoad() + 1, type, status, nbItems, t);
		}
		return r;
	}

	@Nullable
	private static String extractContentType(@Nullable Response r) {
		if (r != null) {
			final MultivaluedMap<String, Object> metadata = r.getMetadata();
			if (metadata != null) {
				final List<Object> typeList = metadata.get(HttpHeaders.CONTENT_TYPE);
				if (typeList != null && !typeList.isEmpty()) {
					return (String) typeList.get(0);
				}
			}
		}
		return null;
	}

	private interface ExecutionCallbackWithUser {
		@NotNull
		ExecutionResult execute(UserLogin userLogin) throws Exception;
	}

	private Response execute(final String user, Object callDescription, Logger accessLog, final ExecutionCallbackWithUser callback) {
		return execute(callDescription, accessLog, () -> {
			final UserLogin userLogin = WebServiceHelper.parseLoginParameter(user);
			if (userLogin == null) {
				LOGGER.error("Missing/invalid user (" + user + ")");
				return ExecutionResult.with(WebServiceHelper.buildErrorResponse(Response.Status.BAD_REQUEST, getAcceptableMediaTypes(), ErrorType.TECHNICAL, "Missing/invalid user parameter."));
			}

			WebServiceHelper.login(userLogin);
			try {
				return callback.execute(userLogin);
			}
			finally {
				WebServiceHelper.logout();
			}
		});
	}

	@Override
	public Response setAutomaticRepaymentBlockingFlag(final int partyNo, final String user, final String value) {

		final Object params = new Object() {
			@Override
			public String toString() {
				return String.format("setAutomaticRepaymentBlockingFlag{partyNo=%d, user=%s, value=%s}", partyNo, WebServiceHelper.enquote(user), WebServiceHelper.enquote(value));
			}
		};
		return execute(user, params, WRITE_ACCESS_LOG, userLogin -> {
			if (value == null || !BOOLEAN_PATTERN.matcher(value).matches()) {
				LOGGER.error("Wrong or missing new flag value");
				return ExecutionResult.with(WebServiceHelper.buildErrorResponse(Response.Status.BAD_REQUEST, getAcceptableMediaTypes(), ErrorType.TECHNICAL, "Wrong or missing new flag value."));
			}

			final boolean blocked = Boolean.parseBoolean(value);
			target.setAutomaticRepaymentBlockingFlag(partyNo, userLogin, blocked);
			return ExecutionResult.with(Response.ok().build());
		});
	}

	@Override
	public Response getAutomaticRepaymentBlockingFlag(final int partyNo, final String user) {
		final Object params = new Object() {
			@Override
			public String toString() {
				return String.format("getAutomaticRepaymentBlockingFlag{partyNo=%d, user=%s}", partyNo, WebServiceHelper.enquote(user));
			}
		};
		return execute(user, params, READ_ACCESS_LOG, userLogin -> {
			final boolean blocked = target.getAutomaticRepaymentBlockingFlag(partyNo, userLogin);
			return ExecutionResult.with(Response.ok(blocked, WebServiceHelper.APPLICATION_JSON_WITH_UTF8_CHARSET_TYPE).build());
		});
	}

	@Override
	public Response ping() {
		return execute("ping", READ_ACCESS_LOG, () -> ExecutionResult.with(Response.ok(DateHelper.getCurrentDate().getTime(), WebServiceHelper.TEXT_PLAIN_WITH_UTF8_CHARSET_TYPE).build()));
	}

	@Override
	public Response getSecurityOnParty(final String user, final int partyNo) {
		final Object params = new Object() {
			@Override
			public String toString() {
				return String.format("getSecurityOnParty{user=%s, partyNo=%d}", WebServiceHelper.enquote(user), partyNo);
			}
		};
		return execute(params, READ_ACCESS_LOG, () -> {
			final SecurityResponse response = target.getSecurityOnParty(user, partyNo);
			final MediaType preferred = getPreferredMediaTypeFromXmlOrJson();
			if (preferred == WebServiceHelper.APPLICATION_JSON_WITH_UTF8_CHARSET_TYPE) {
				return ExecutionResult.with(Response.ok(response, preferred).build());
			}
			else if (preferred == MediaType.APPLICATION_XML_TYPE) {
				return ExecutionResult.with(Response.ok(securityObjectFactory.createUserAccess(response), preferred).build());
			}
			return ExecutionResult.with(Response.status(Response.Status.UNSUPPORTED_MEDIA_TYPE).build());
		});
	}

	@Override
	public Response getParty(final int partyNo, final String user, final Set<PartyPart> parts) {
		final Object params = new Object() {
			@Override
			public String toString() {
				// petite combine pour que les modalités de l'énum soient toujours logguées dans le même ordre...
				final Set<PartyPart> sortedParts = parts == null || parts.isEmpty() ? Collections.<PartyPart>emptySet() : EnumSet.copyOf(parts);
				return String.format("getParty{user=%s, partyNo=%d, parts=%s}", WebServiceHelper.enquote(user), partyNo, WebServiceHelper.toString(sortedParts));
			}
		};
		return execute(user, params, READ_ACCESS_LOG, userLogin -> {
			try {
				final Party party = target.getParty(userLogin, partyNo, parts);
				if (party == null) {
					return ExecutionResult.with(WebServiceHelper.buildErrorResponse(Response.Status.NOT_FOUND, getAcceptableMediaTypes(), ErrorType.BUSINESS, "Le tiers n'existe pas."));
				}
				final MediaType preferred = getPreferredMediaTypeFromXmlOrJson();
				if (preferred == WebServiceHelper.APPLICATION_JSON_WITH_UTF8_CHARSET_TYPE) {
					return ExecutionResult.with(Response.ok(PartyJsonContainer.fromValue(party), preferred).build(), 1);
				}
				else if (preferred == MediaType.APPLICATION_XML_TYPE) {
					return ExecutionResult.with(Response.ok(partyObjectFactory.createParty(party), preferred).build(), 1);
				}
				return ExecutionResult.with(Response.status(Response.Status.UNSUPPORTED_MEDIA_TYPE).build());
			}
			catch (ServiceException e) {
				final ErrorType errorType;
				final Response.Status status;
				if (e.getInfo() instanceof AccessDeniedExceptionInfo) {
					errorType = ErrorType.ACCESS;
					status = Response.Status.FORBIDDEN;
				}
				else {
					errorType = e.getInfo() instanceof BusinessExceptionInfo ? ErrorType.BUSINESS : ErrorType.TECHNICAL;
					status = Response.Status.INTERNAL_SERVER_ERROR;
				}
				return ExecutionResult.with(WebServiceHelper.buildErrorResponse(status, getAcceptableMediaTypes(), errorType, e));
			}
		});
	}

	@Override
	public Response getParties(final String user, final List<Integer> partyNos, final Set<PartyPart> parts) {
		final Object params = new Object() {
			@Override
			public String toString() {
				// petite combine pour que les modalités de l'énum soient toujours logguées dans le même ordre...
				final Set<PartyPart> sortedParts = parts == null || parts.isEmpty() ? Collections.<PartyPart>emptySet() : EnumSet.copyOf(parts);
				return String.format("getParties{user=%s, partyNo=%s, parts=%s}", WebServiceHelper.enquote(user), WebServiceHelper.toString(partyNos), WebServiceHelper.toString(sortedParts));
			}
		};
		return execute(user, params, READ_ACCESS_LOG, userLogin -> {
			try {
				final Parties parties = target.getParties(userLogin, partyNos, parts);
				if (parties == null) {
					return ExecutionResult.with(Response.noContent().build(), 0);
				}
				final int nbItems = countParties(parties.getEntries());
				final MediaType preferred = getPreferredMediaTypeFromXmlOrJson();
				if (preferred == WebServiceHelper.APPLICATION_JSON_WITH_UTF8_CHARSET_TYPE) {
					// TODO quel format utiliser pour le retour JSON ?
					throw new NotImplementedException();
				}
				else if (preferred == MediaType.APPLICATION_XML_TYPE) {
					return ExecutionResult.with(Response.ok(parties, preferred).build(), nbItems);
				}
				return ExecutionResult.with(Response.status(Response.Status.UNSUPPORTED_MEDIA_TYPE).build());
			}
			catch (ServiceException e) {
				return ExecutionResult.with(WebServiceHelper.buildErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, getAcceptableMediaTypes(), ErrorType.TECHNICAL, e));
			}
		});
	}

	private static int countParties(Collection<Entry> col) {
		int count = 0;
		for (Entry item : col) {
			if (item.getParty() != null) {
				++ count;
			}
		}
		return count;
	}

	@Override
	public Response searchParty(final String user, final String partyNo, final String name, final SearchMode nameSearchMode, final String townOrCountry,
	                            final String dateOfBirthStr, final String socialInsuranceNumber, final String uidNumber, final Integer taxResidenceFSOId,
	                            final boolean onlyActiveMainTaxResidence, final Set<PartySearchType> partyTypes, final DebtorCategory debtorCategory, final Boolean activeParty,
	                            final Long oldWithholdingNumber) {

		final Object params = new Object() {
			@Override
			public String toString() {
				final String partyTypesStr = Arrays.toString(partyTypes.toArray(new PartySearchType[partyTypes.size()]));
				return String.format("searchParty{user=%s, partyNo=%s, name=%s, nameSearchMode=%s, townOrCountry=%s, dateOfBirth=%s, vn=%s, uidNumber=%s, taxResidenceFSOId=%d, onlyActiveMainTaxResidence=%s, partyTypes=%s, debtorCategory=%s, activeParty=%s, oldWithholdingNumber=%d}",
				                     WebServiceHelper.enquote(user), WebServiceHelper.enquote(partyNo), WebServiceHelper.enquote(name), nameSearchMode, WebServiceHelper.enquote(townOrCountry),
				                     WebServiceHelper.enquote(dateOfBirthStr), WebServiceHelper.enquote(socialInsuranceNumber), WebServiceHelper.enquote(uidNumber), taxResidenceFSOId,
				                     onlyActiveMainTaxResidence, partyTypesStr, debtorCategory, activeParty, oldWithholdingNumber);
			}
		};
		return execute(user, params, READ_ACCESS_LOG, userLogin -> {
			final RegDate dateNaissance;
			try {
				dateNaissance = dateFromString(dateOfBirthStr, true);
			}
			catch (ParseException | IllegalArgumentException e) {
				return ExecutionResult.with(WebServiceHelper.buildErrorResponse(Response.Status.BAD_REQUEST, getAcceptableMediaTypes(), ErrorType.TECHNICAL, e));
			}

			SearchResult result;
			try {
				final List<PartyInfo> infos = target.searchParty(userLogin, partyNo, name, nameSearchMode, townOrCountry, dateNaissance, socialInsuranceNumber, uidNumber, taxResidenceFSOId,
				                                                 onlyActiveMainTaxResidence, partyTypes, debtorCategory, activeParty, oldWithholdingNumber);
				result = new SearchResult(null, infos);
			}
			catch (IndexerException e) {
				result = new SearchResult(new Error(ErrorType.BUSINESS, WebServiceHelper.buildExceptionMessage(e)), null);
			}

			final int nbItems = result.getParty() != null ? result.getParty().size() : 0;
			final MediaType preferred = getPreferredMediaTypeFromXmlOrJson();
			if (preferred == WebServiceHelper.APPLICATION_JSON_WITH_UTF8_CHARSET_TYPE) {
				return ExecutionResult.with(Response.ok(result, preferred).build(), nbItems);
			}
			else if (preferred == MediaType.APPLICATION_XML_TYPE) {
				return ExecutionResult.with(Response.ok(searchPartyObjectFactory.createSearchResult(result), preferred).build(), nbItems);
			}
			return ExecutionResult.with(Response.status(Response.Status.UNSUPPORTED_MEDIA_TYPE).build());
		});
	}

	@Nullable
	private static RegDate dateFromString(@Nullable String str, boolean partialAllowed) throws ParseException, IllegalArgumentException {
		if (StringUtils.isNotBlank(str)) {
			return RegDateHelper.displayStringToRegDate(str, partialAllowed);
		}
		else {
			return null;
		}
	}

	@Override
	public Response getTaxOffices(final int municipalityId, final String dateStr) {
		final Object params = new Object() {
			@Override
			public String toString() {
				return String.format("getTaxOffices{municipalityId=%d, date=%s}", municipalityId, WebServiceHelper.enquote(dateStr));
			}
		};
		return execute(params, READ_ACCESS_LOG, () -> {
			final RegDate date;
			try {
				date = dateFromString(dateStr, false);
			}
			catch (ParseException | IllegalArgumentException e) {
				return ExecutionResult.with(WebServiceHelper.buildErrorResponse(Response.Status.BAD_REQUEST, getAcceptableMediaTypes(), ErrorType.TECHNICAL, e));
			}

			final TaxOffices taxOffices = target.getTaxOffices(municipalityId, date);
			final MediaType preferred = getPreferredMediaTypeFromXmlOrJson();
			if (preferred == WebServiceHelper.APPLICATION_JSON_WITH_UTF8_CHARSET_TYPE) {
				return ExecutionResult.with(Response.ok(taxOffices, preferred).build());
			}
			else if (preferred == MediaType.APPLICATION_XML_TYPE) {
				return ExecutionResult.with(Response.ok(taxOfficesObjectFactory.createTaxOffices(taxOffices), preferred).build());
			}
			return ExecutionResult.with(Response.status(Response.Status.UNSUPPORTED_MEDIA_TYPE).build());
		});
	}

	/**
	 * @return {@link WebServiceHelper#APPLICATION_JSON_WITH_UTF8_CHARSET_TYPE} si JSON est préféré, ou {@link MediaType#APPLICATION_XML_TYPE} si c'est XML, ou <code>null</code>
	 * si aucun des types acceptés par le client n'est compatible avec JSON ou XML
	 */
	private MediaType getPreferredMediaTypeFromXmlOrJson() {
		return WebServiceHelper.getPreferedMediaType(getAcceptableMediaTypes(),
		                                             new MediaType[]{MediaType.APPLICATION_XML_TYPE, WebServiceHelper.APPLICATION_JSON_WITH_UTF8_CHARSET_TYPE});
	}

	private List<MediaType> getAcceptableMediaTypes() {
		return messageContext.getHttpHeaders().getAcceptableMediaTypes();
	}

	@Override
	public Response ackOrdinaryTaxDeclarations(final String user, final OrdinaryTaxDeclarationAckRequest request) {
		final Object params = new Object() {
			@Override
			public String toString() {
				return String.format("ackOrdinaryTaxDeclarations{user=%s, request=%s}", WebServiceHelper.enquote(user), request);
			}
		};
		return execute(user, params, WRITE_ACCESS_LOG, userLogin -> {
			final OrdinaryTaxDeclarationAckResponse response = target.ackOrdinaryTaxDeclarations(userLogin, request);
			return ExecutionResult.with(Response.ok(ackObjectFactory.createOrdinaryTaxDeclarationAckResponse(response)).build());
		});
	}

	@Override
	public Response newOrdinaryTaxDeclarationDeadline(final int partyNo, final int pf, final int seqNo,
	                                                  final String user, final DeadlineRequest request) {
		final Object params = new Object() {
			@Override
			public String toString() {
				return String.format("newOrdinaryTaxDeclarationDeadline{user=%s, partyNo=%d, pf=%d, seqNo=%d, request=%s}", WebServiceHelper.enquote(user), partyNo, pf, seqNo, request);
			}
		};
		return execute(user, params, WRITE_ACCESS_LOG, userLogin -> {
			final DeadlineResponse response = target.newOrdinaryTaxDeclarationDeadline(partyNo, pf, seqNo, userLogin, request);
			return ExecutionResult.with(Response.ok(deadlineObjectFactory.createDeadlineResponse(response)).build());
		});
	}

	@Override
	public Response getModifiedTaxPayers(final String user, final Long since, final Long until) {
		final Object params = new Object() {
			@Override
			public String toString() {
				return String.format("getModifiedTaxPayers{user=%s, since=%d, until=%d}", WebServiceHelper.enquote(user), since, until);
			}
		};
		return execute(user, params, READ_ACCESS_LOG, userLogin -> {
			if (since == null || until == null) {
				return ExecutionResult.with(WebServiceHelper.buildErrorResponse(Response.Status.BAD_REQUEST, getAcceptableMediaTypes(), ErrorType.TECHNICAL, "'since' and 'until' are required parameters."));
			}
			else if (since > until) {
				return ExecutionResult.with(WebServiceHelper.buildErrorResponse(Response.Status.BAD_REQUEST, getAcceptableMediaTypes(), ErrorType.TECHNICAL, "'since' should be before 'until'"));
			}

			final Date sinceTimestamp = new Date(since);
			final Date untilTimestamp = new Date(until);
			final PartyNumberList response = target.getModifiedTaxPayers(userLogin, sinceTimestamp, untilTimestamp);
			final MediaType preferred = getPreferredMediaTypeFromXmlOrJson();
			if (preferred == WebServiceHelper.APPLICATION_JSON_WITH_UTF8_CHARSET_TYPE) {
				return ExecutionResult.with(Response.ok(response, preferred).build(), response.getPartyNo().size());
			}
			else if (preferred == MediaType.APPLICATION_XML_TYPE) {
				return ExecutionResult.with(Response.ok(modifiedTaxPayersFactory.createModifiedTayPayers(response), preferred).build(), response.getPartyNo().size());
			}
			return ExecutionResult.with(Response.status(Response.Status.UNSUPPORTED_MEDIA_TYPE).build());
		});
	}

	@Override
	public Response getDebtorInfo(final int debtorNo, final int pf, final String user) {
		final Object params = new Object() {
			@Override
			public String toString() {
				return String.format("getDebtorInfo{debtorNo=%d, fiscalPeriod=%d, user=%s}", debtorNo, pf, WebServiceHelper.enquote(user));
			}
		};
		return execute(user, params, READ_ACCESS_LOG, userLogin -> {
			final DebtorInfo info = target.getDebtorInfo(userLogin, debtorNo, pf);
			final MediaType preferred = getPreferredMediaTypeFromXmlOrJson();
			if (preferred == WebServiceHelper.APPLICATION_JSON_WITH_UTF8_CHARSET_TYPE) {
				return ExecutionResult.with(Response.ok(info, preferred).build());
			}
			else if (preferred == MediaType.APPLICATION_XML_TYPE) {
				return ExecutionResult.with(Response.ok(debtorInfoFactory.createDebtorInfo(info), preferred).build());
			}
			return ExecutionResult.with(Response.status(Response.Status.UNSUPPORTED_MEDIA_TYPE).build());
		});
	}

	@Override
	public Response getAvatar(final int partyNo) {
		final Object params = new Object() {
			@Override
			public String toString() {
				return String.format("getAvatar{partyNo=%d}", partyNo);
			}
		};
		return execute(params, READ_ACCESS_LOG, () -> {
			try (ImageData data = target.getAvatar(partyNo); ByteArrayOutputStream bos = new ByteArrayOutputStream(16 * 1024)) {
				IOUtils.copy(data.getDataStream(), bos);
				return ExecutionResult.with(Response.ok(bos.toByteArray(), data.getMimeType()).build());
			}
		});
	}

	@Override
	public Response getFiscalEvents(final int partyNo, final String user) {
		final Object params = new Object() {
			@Override
			public String toString() {
				return String.format("getFiscalEvents{partyNo=%d, user=%s}", partyNo, WebServiceHelper.enquote(user));
			}
		};
		return execute(user, params, READ_ACCESS_LOG, userLogin -> {
			final FiscalEvents events = target.getFiscalEvents(userLogin, partyNo);
			final MediaType preferred = getPreferredMediaTypeFromXmlOrJson();
			if (preferred == MediaType.APPLICATION_XML_TYPE) {
				return ExecutionResult.with(Response.ok(fiscalEventsObjectFactory.createFiscalEvents(events)).build());
			}
			return ExecutionResult.with(Response.status(Response.Status.UNSUPPORTED_MEDIA_TYPE).build());

		});
	}

	@Override
	public Response getImmovableProperty(long immNo, String user) {
		final Object params = new Object() {
			@Override
			public String toString() {
				return String.format("getImmovableProperty{immNo=%d, user=%s}", immNo, WebServiceHelper.enquote(user));
			}
		};
		return execute(user, params, READ_ACCESS_LOG, userLogin -> {
			final ImmovableProperty immovable = target.getImmovablePropery(userLogin, immNo);
			final MediaType preferred = getPreferredMediaTypeFromXmlOrJson();
			if (preferred == MediaType.APPLICATION_XML_TYPE) {
				return ExecutionResult.with(Response.ok(landRegistryObjectFactory.createImmovableProperty(immovable)).build());
			}
			return ExecutionResult.with(Response.status(Response.Status.UNSUPPORTED_MEDIA_TYPE).build());

		});
	}

	@Override
	public Response getBuilding(long buildingNo, String user) {
		final Object params = new Object() {
			@Override
			public String toString() {
				return String.format("getBuilding{buildingNo=%d, user=%s}", buildingNo, WebServiceHelper.enquote(user));
			}
		};
		return execute(user, params, READ_ACCESS_LOG, userLogin -> {
			final Building building = target.getBuilding(userLogin, buildingNo);
			final MediaType preferred = getPreferredMediaTypeFromXmlOrJson();
			if (preferred == MediaType.APPLICATION_XML_TYPE) {
				return ExecutionResult.with(Response.ok(landRegistryObjectFactory.createBuilding(building)).build());
			}
			return ExecutionResult.with(Response.status(Response.Status.UNSUPPORTED_MEDIA_TYPE).build());

		});
	}
}
