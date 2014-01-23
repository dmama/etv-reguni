package ch.vd.uniregctb.webservices.v5;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.ws.ack.v1.OrdinaryTaxDeclarationAckRequest;
import ch.vd.unireg.ws.ack.v1.OrdinaryTaxDeclarationAckResponse;
import ch.vd.unireg.ws.deadline.v1.DeadlineRequest;
import ch.vd.unireg.ws.deadline.v1.DeadlineResponse;
import ch.vd.unireg.ws.modifiedtaxpayers.v1.PartyNumberList;
import ch.vd.unireg.ws.security.v1.SecurityResponse;
import ch.vd.unireg.ws.taxoffices.v1.TaxOffices;
import ch.vd.unireg.xml.error.v1.Error;
import ch.vd.unireg.xml.party.corporation.v3.CorporationEvent;
import ch.vd.unireg.xml.party.v3.Party;
import ch.vd.unireg.xml.party.v3.PartyInfo;
import ch.vd.unireg.xml.party.v3.PartyPart;
import ch.vd.unireg.xml.party.v3.PartyType;
import ch.vd.unireg.xml.party.withholding.v1.DebtorCategory;
import ch.vd.unireg.xml.party.withholding.v1.DebtorInfo;
import ch.vd.uniregctb.common.ObjectNotFoundException;
import ch.vd.uniregctb.indexer.EmptySearchCriteriaException;
import ch.vd.uniregctb.indexer.IndexerException;
import ch.vd.uniregctb.load.DetailedLoadMeter;
import ch.vd.uniregctb.load.DetailedLoadMonitorable;
import ch.vd.uniregctb.load.LoadDetail;
import ch.vd.uniregctb.webservices.common.AccessDeniedException;
import ch.vd.uniregctb.webservices.common.UserLogin;
import ch.vd.uniregctb.webservices.common.WebServiceHelper;
import ch.vd.uniregctb.xml.ServiceException;

public class WebServiceEndPoint implements WebService, DetailedLoadMonitorable {

	private static final Logger LOGGER = Logger.getLogger(WebServiceEndPoint.class);
	private static final Logger READ_ACCESS_LOG = Logger.getLogger("ws.v5.read");
	private static final Logger WRITE_ACCESS_LOG = Logger.getLogger("ws.v5.write");

	private static final Pattern BOOLEAN_PATTERN = Pattern.compile("(true|false)", Pattern.CASE_INSENSITIVE);

	@Context
	private MessageContext messageContext;

	/**
	 * Moniteur des appels en cours
	 */
	private final DetailedLoadMeter<Object> loadMeter = new DetailedLoadMeter<>();

	private final ch.vd.unireg.ws.security.v1.ObjectFactory securityObjectFactory = new ch.vd.unireg.ws.security.v1.ObjectFactory();
	private final ch.vd.unireg.ws.ack.v1.ObjectFactory ackObjectFactory = new ch.vd.unireg.ws.ack.v1.ObjectFactory();
	private final ch.vd.unireg.ws.deadline.v1.ObjectFactory deadlineObjectFactory = new ch.vd.unireg.ws.deadline.v1.ObjectFactory();
	private final ch.vd.unireg.ws.taxoffices.v1.ObjectFactory taxOfficesObjectFactory = new ch.vd.unireg.ws.taxoffices.v1.ObjectFactory();
	private final ch.vd.unireg.ws.modifiedtaxpayers.v1.ObjectFactory modifiedTaxPayersFactory = new ch.vd.unireg.ws.modifiedtaxpayers.v1.ObjectFactory();
	private final ch.vd.unireg.ws.debtorinfo.v1.ObjectFactory debtorInfoFactory = new ch.vd.unireg.ws.debtorinfo.v1.ObjectFactory();
	private final ch.vd.unireg.ws.search.party.v1.ObjectFactory searchPartyObjectFactory = new ch.vd.unireg.ws.search.party.v1.ObjectFactory();
	private final ch.vd.unireg.ws.search.corpevent.v1.ObjectFactory searchCorpEventObjectFactory = new ch.vd.unireg.ws.search.corpevent.v1.ObjectFactory();
	private final ch.vd.unireg.ws.party.v1.ObjectFactory partyObjectFactory = new ch.vd.unireg.ws.party.v1.ObjectFactory();

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

	private static interface ExecutionCallback {
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
			r = WebServiceHelper.buildErrorResponse(Response.Status.FORBIDDEN, getAcceptableMediaTypes(), e);
		}
		catch (ObjectNotFoundException e) {
			t = e;
			LOGGER.error(e.getMessage());
			r = WebServiceHelper.buildErrorResponse(Response.Status.NOT_FOUND, getAcceptableMediaTypes(), e);
		}
		catch (Throwable e) {
			t = e;
			LOGGER.error(e, e);
			r = WebServiceHelper.buildErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, getAcceptableMediaTypes(), e);
		}
		finally {
			final long end = loadMeter.end();
			final Response.Status status = (r == null ? null : Response.Status.fromStatusCode(r.getStatus()));
			WebServiceHelper.logAccessInfo(accessLog, messageContext.getHttpServletRequest(), callDescription, end - start, getLoad() + 1, status, nbItems, t);
		}
		return r;
	}

	private static interface ExecutionCallbackWithUser {
		@NotNull
		ExecutionResult execute(UserLogin userLogin) throws Exception;
	}

	private Response execute(final String user, Object callDescription, Logger accessLog, final ExecutionCallbackWithUser callback) {
		return execute(callDescription, accessLog, new ExecutionCallback() {
			@NotNull
			@Override
			public ExecutionResult execute() throws Exception {
				final UserLogin userLogin = WebServiceHelper.parseLoginParameter(user);
				if (userLogin == null) {
					LOGGER.error("Missing/invalid user (" + user + ")");
					return ExecutionResult.with(WebServiceHelper.buildErrorResponse(Response.Status.BAD_REQUEST, getAcceptableMediaTypes(), "Missing/invalid user parameter."));
				}

				WebServiceHelper.login(userLogin);
				try {
					return callback.execute(userLogin);
				}
				finally {
					WebServiceHelper.logout();
				}
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
		return execute(user, params, WRITE_ACCESS_LOG, new ExecutionCallbackWithUser() {
			@NotNull
			@Override
			public ExecutionResult execute(UserLogin userLogin) throws Exception {
				if (value == null || !BOOLEAN_PATTERN.matcher(value).matches()) {
					LOGGER.error("Wrong or missing new flag value");
					return ExecutionResult.with(WebServiceHelper.buildErrorResponse(Response.Status.BAD_REQUEST, getAcceptableMediaTypes(), "Wrong or missing new flag value."));
				}

				final boolean blocked = Boolean.parseBoolean(value);
				target.setAutomaticRepaymentBlockingFlag(partyNo, userLogin, blocked);
				return ExecutionResult.with(Response.ok().build());
			}
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
		return execute(user, params, READ_ACCESS_LOG, new ExecutionCallbackWithUser() {
			@NotNull
			@Override
			public ExecutionResult execute(UserLogin userLogin) throws Exception {
				final boolean blocked = target.getAutomaticRepaymentBlockingFlag(partyNo, userLogin);
				return ExecutionResult.with(Response.ok(blocked, WebServiceHelper.APPLICATION_JSON_WITH_UTF8_CHARSET_TYPE).build());
			}
		});
	}

	@Override
	public Response ping() {
		return execute("ping", READ_ACCESS_LOG, new ExecutionCallback() {
			@NotNull
			@Override
			public ExecutionResult execute() throws Exception {
				return ExecutionResult.with(Response.ok(DateHelper.getCurrentDate().getTime(), WebServiceHelper.TEXT_PLAIN_WITH_UTF8_CHARSET_TYPE).build());
			}
		});
	}

	@Override
	public Response getSecurityOnParty(final String user, final int partyNo) {
		final Object params = new Object() {
			@Override
			public String toString() {
				return String.format("getSecurityOnParty{user=%s, partyNo=%d}", WebServiceHelper.enquote(user), partyNo);
			}
		};
		return execute(params, READ_ACCESS_LOG, new ExecutionCallback() {
			@NotNull
			@Override
			public ExecutionResult execute() throws Exception {
				final SecurityResponse response = target.getSecurityOnParty(user, partyNo);
				final MediaType preferred = getPreferredMediaTypeFromXmlOrJson();
				if (preferred == WebServiceHelper.APPLICATION_JSON_WITH_UTF8_CHARSET_TYPE) {
					return ExecutionResult.with(Response.ok(response, preferred).build());
				}
				else if (preferred == MediaType.APPLICATION_XML_TYPE) {
					return ExecutionResult.with(Response.ok(securityObjectFactory.createUserAccess(response), preferred).build());
				}
				return ExecutionResult.with(Response.status(Response.Status.UNSUPPORTED_MEDIA_TYPE).build());
			}
		});
	}

	@Override
	public Response getParty(final int partyNo, final String user, final Set<PartyPart> parts) {
		final Object params = new Object() {
			@Override
			public String toString() {
				return String.format("getParty{user=%s, partyNo=%d, parts=%s}", WebServiceHelper.enquote(user), partyNo, WebServiceHelper.toString(parts));
			}
		};
		return execute(user, params, READ_ACCESS_LOG, new ExecutionCallbackWithUser() {
			@NotNull
			@Override
			public ExecutionResult execute(UserLogin userLogin) throws Exception {
				try {
					final Party party = target.getParty(userLogin, partyNo, parts);
					if (party == null) {
						return ExecutionResult.with(WebServiceHelper.buildErrorResponse(Response.Status.NOT_FOUND, getAcceptableMediaTypes(), "Le tiers n'existe pas."));
					}
					final MediaType preferred = getPreferredMediaTypeFromXmlOrJson();
					if (preferred == WebServiceHelper.APPLICATION_JSON_WITH_UTF8_CHARSET_TYPE) {
						return ExecutionResult.with(Response.ok(PartyJsonContainer.fromValue(party), preferred).build());
					}
					else if (preferred == MediaType.APPLICATION_XML_TYPE) {
						return ExecutionResult.with(Response.ok(partyObjectFactory.createParty(party), preferred).build());
					}
					return ExecutionResult.with(Response.status(Response.Status.UNSUPPORTED_MEDIA_TYPE).build());
				}
				catch (ServiceException e) {
					return ExecutionResult.with(WebServiceHelper.buildErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, getAcceptableMediaTypes(), e));
				}
			}
		});
	}

	@Override
	public Response getParties(String user, List<Integer> partyNos, Set<PartyPart> parts) {
		return WebServiceHelper.buildErrorResponse(Response.Status.SERVICE_UNAVAILABLE, getAcceptableMediaTypes(), "Implémentation encore en cours...");
	}

	@Override
	public Response searchParty(final String user, final String partyNo, final String name, final SearchMode nameSearchMode, final String townOrCountry,
	                            final String dateOfBirthStr, final String socialInsuranceNumber, final Integer taxResidenceFSOId,
	                            final boolean onlyActiveMainTaxResidence, final Set<PartyType> partyTypes, final DebtorCategory debtorCategory, final Boolean activeParty,
	                            final Long oldWithholdingNumber) {

		final Object params = new Object() {
			@Override
			public String toString() {
				final String partyTypesStr = Arrays.toString(partyTypes.toArray(new PartyType[partyTypes.size()]));
				return String.format("searchParty{user=%s, partyNo=%s, name=%s, nameSearchMode=%s, townOrCountry=%s, dateOfBirth=%s, vn=%s, taxResidenceFSOId=%d, onlyActiveMainTaxResidence=%s, partyTypes=%s, debtorCategory=%s, activeParty=%s, oldWithholdingNumber=%d}",
				                     WebServiceHelper.enquote(user), WebServiceHelper.enquote(partyNo), WebServiceHelper.enquote(name), nameSearchMode, WebServiceHelper.enquote(townOrCountry),
				                     WebServiceHelper.enquote(dateOfBirthStr), WebServiceHelper.enquote(socialInsuranceNumber), taxResidenceFSOId,
				                     onlyActiveMainTaxResidence, partyTypesStr, debtorCategory, activeParty, oldWithholdingNumber);
			}
		};
		return execute(user, params, READ_ACCESS_LOG, new ExecutionCallbackWithUser() {
			@NotNull
			@Override
			public ExecutionResult execute(UserLogin userLogin) throws Exception {
				final RegDate dateNaissance;
				try {
					dateNaissance = dateFromString(dateOfBirthStr, true);
				}
				catch (ParseException | IllegalArgumentException e) {
					return ExecutionResult.with(WebServiceHelper.buildErrorResponse(Response.Status.BAD_REQUEST, getAcceptableMediaTypes(), e));
				}

				ch.vd.unireg.ws.search.party.v1.SearchResult result;
				try {
					final List<PartyInfo> infos = target.searchParty(userLogin, partyNo, name, nameSearchMode, townOrCountry, dateNaissance, socialInsuranceNumber, taxResidenceFSOId, onlyActiveMainTaxResidence,
					                                                 partyTypes, debtorCategory, activeParty, oldWithholdingNumber);
					result = new ch.vd.unireg.ws.search.party.v1.SearchResult(null, infos);
				}
				catch (IndexerException e) {
					result = new ch.vd.unireg.ws.search.party.v1.SearchResult(new Error(WebServiceHelper.buildExceptionMessage(e)), null);
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
			}
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
		return execute(params, READ_ACCESS_LOG, new ExecutionCallback() {
			@NotNull
			@Override
			public ExecutionResult execute() throws Exception {
				final RegDate date;
				try {
					date = dateFromString(dateStr, false);
				}
				catch (ParseException | IllegalArgumentException e) {
					return ExecutionResult.with(WebServiceHelper.buildErrorResponse(Response.Status.BAD_REQUEST, getAcceptableMediaTypes(), e));
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
			}
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
		return execute(user, params, WRITE_ACCESS_LOG, new ExecutionCallbackWithUser() {
			@NotNull
			@Override
			public ExecutionResult execute(UserLogin userLogin) throws Exception {
				final OrdinaryTaxDeclarationAckResponse response = target.ackOrdinaryTaxDeclarations(userLogin, request);
				return ExecutionResult.with(Response.ok(ackObjectFactory.createOrdinaryTaxDeclarationAckResponse(response)).build());
			}
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
		return execute(user, params, WRITE_ACCESS_LOG, new ExecutionCallbackWithUser() {
			@NotNull
			@Override
			public ExecutionResult execute(UserLogin userLogin) throws Exception {
				final DeadlineResponse response = target.newOrdinaryTaxDeclarationDeadline(partyNo, pf, seqNo, userLogin, request);
				return ExecutionResult.with(Response.ok(deadlineObjectFactory.createDeadlineResponse(response)).build());
			}
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
		return execute(user, params, READ_ACCESS_LOG, new ExecutionCallbackWithUser() {
			@NotNull
			@Override
			public ExecutionResult execute(UserLogin userLogin) throws Exception {
				if (since == null || until == null) {
					return ExecutionResult.with(WebServiceHelper.buildErrorResponse(Response.Status.BAD_REQUEST, getAcceptableMediaTypes(), "'since' and 'until' are required parameters."));
				}
				else if (since > until) {
					return ExecutionResult.with(WebServiceHelper.buildErrorResponse(Response.Status.BAD_REQUEST, getAcceptableMediaTypes(), "'since' should be before 'until'"));
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
			}
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
		return execute(user, params, READ_ACCESS_LOG, new ExecutionCallbackWithUser() {
			@NotNull
			@Override
			public ExecutionResult execute(UserLogin userLogin) throws Exception {
				final DebtorInfo info = target.getDebtorInfo(userLogin, debtorNo, pf);
				final MediaType preferred = getPreferredMediaTypeFromXmlOrJson();
				if (preferred == WebServiceHelper.APPLICATION_JSON_WITH_UTF8_CHARSET_TYPE) {
					return ExecutionResult.with(Response.ok(info, preferred).build());
				}
				else if (preferred == MediaType.APPLICATION_XML_TYPE) {
					return ExecutionResult.with(Response.ok(debtorInfoFactory.createDebtorInfo(info), preferred).build());
				}
				return ExecutionResult.with(Response.status(Response.Status.UNSUPPORTED_MEDIA_TYPE).build());
			}
		});
	}

	@Override
	public Response searchCorporationEvent(final String user, final Integer corporationId, final String eventCode, final String startDay, final String endDay) {
		final Object params = new Object() {
			@Override
			public String toString() {
				return String.format("searchCorporationEvent{user=%s, corporationId=%d, eventCode=%s, startDay=%s, endDay=%s}",
				                     WebServiceHelper.enquote(user), corporationId, WebServiceHelper.enquote(eventCode), WebServiceHelper.enquote(startDay), WebServiceHelper.enquote(endDay));
			}
		};
		return execute(user, params, READ_ACCESS_LOG, new ExecutionCallbackWithUser() {
			@NotNull
			@Override
			public ExecutionResult execute(UserLogin userLogin) throws Exception {
				final RegDate start;
				final RegDate end;
				try {
					start = dateFromString(startDay, false);
					end = dateFromString(endDay, false);
				}
				catch (ParseException | IllegalArgumentException e) {
					return ExecutionResult.with(WebServiceHelper.buildErrorResponse(Response.Status.BAD_REQUEST, getAcceptableMediaTypes(), e));
				}

				ch.vd.unireg.ws.search.corpevent.v1.SearchResult result;
				try {
					final List<CorporationEvent> events = target.searchCorporationEvent(userLogin, corporationId, eventCode, start, end);
					result = new ch.vd.unireg.ws.search.corpevent.v1.SearchResult(null, events);
				}
				catch (EmptySearchCriteriaException e) {
					result = new ch.vd.unireg.ws.search.corpevent.v1.SearchResult(new Error(WebServiceHelper.buildExceptionMessage(e)), null);
				}

				final int nbItems = result.getEvent() != null ? result.getEvent().size() : 0;
				final MediaType preferred = getPreferredMediaTypeFromXmlOrJson();
				if (preferred == WebServiceHelper.APPLICATION_JSON_WITH_UTF8_CHARSET_TYPE) {
					return ExecutionResult.with(Response.ok(result, preferred).build(), nbItems);
				}
				else if (preferred == MediaType.APPLICATION_XML_TYPE) {
					return ExecutionResult.with(Response.ok(searchCorpEventObjectFactory.createSearchResult(result), preferred).build(), nbItems);
				}
				return ExecutionResult.with(Response.status(Response.Status.UNSUPPORTED_MEDIA_TYPE).build());
			}
		});

	}
}
