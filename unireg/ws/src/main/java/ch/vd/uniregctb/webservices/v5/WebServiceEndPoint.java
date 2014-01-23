package ch.vd.uniregctb.webservices.v5;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.log4j.Logger;

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
import ch.vd.uniregctb.common.ObjectNotFoundException;
import ch.vd.uniregctb.load.DetailedLoadMeter;
import ch.vd.uniregctb.load.DetailedLoadMonitorable;
import ch.vd.uniregctb.load.LoadDetail;
import ch.vd.uniregctb.webservices.common.AccessDeniedException;
import ch.vd.uniregctb.webservices.common.UserLogin;
import ch.vd.uniregctb.webservices.common.WebServiceHelper;

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

	private static interface ExecutionCallback {
		Response execute() throws Exception;
	}

	private Response execute(Object callDescription, Logger accessLog, ExecutionCallback callback) {
		Throwable t = null;
		Response r = null;
		final long start = loadMeter.start(callDescription);
		try {
			r = callback.execute();
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
			WebServiceHelper.logAccessInfo(accessLog, messageContext.getHttpServletRequest(), callDescription, end - start, getLoad() + 1, status, t);
		}
		return r;
	}

	private static interface ExecutionWithLoginCallback {
		Response execute(UserLogin userLogin) throws Exception;
	}

	private Response execute(final String login, Object callDescription, Logger accessLog, final ExecutionWithLoginCallback callback) {
		return execute(callDescription, accessLog, new ExecutionCallback() {
			@Override
			public Response execute() throws Exception {
				final UserLogin userLogin = WebServiceHelper.parseLoginParameter(login);
				if (userLogin == null) {
					LOGGER.error("Missing/invalid login (" + login + ")");
					return WebServiceHelper.buildErrorResponse(Response.Status.BAD_REQUEST, getAcceptableMediaTypes(), "Missing/invalid login parameter.");
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
	public Response setAutomaticRepaymentBlockingFlag(final int partyNo, final String login, final String value) {

		final Object params = new Object() {
			@Override
			public String toString() {
				return String.format("setAutomaticRepaymentBlockingFlag{partyNo=%d, login='%s', value='%s'}", partyNo, login, value);
			}
		};
		return execute(login, params, WRITE_ACCESS_LOG, new ExecutionWithLoginCallback() {
			@Override
			public Response execute(UserLogin userLogin) throws Exception {
				if (value == null || !BOOLEAN_PATTERN.matcher(value).matches()) {
					LOGGER.error("Wrong or missing new flag value");
					return WebServiceHelper.buildErrorResponse(Response.Status.BAD_REQUEST, getAcceptableMediaTypes(), "Wrong or missing new flag value.");
				}

				final boolean blocked = Boolean.parseBoolean(value);
				target.setAutomaticRepaymentBlockingFlag(partyNo, userLogin, blocked);
				return Response.ok().build();
			}
		});
	}

	@Override
	public Response getAutomaticRepaymentBlockingFlag(final int partyNo, final String login) {
		final Object params = new Object() {
			@Override
			public String toString() {
				return String.format("getAutomaticRepaymentBlockingFlag{partyNo=%d, login='%s'}", partyNo, login);
			}
		};
		return execute(login, params, READ_ACCESS_LOG, new ExecutionWithLoginCallback() {
			@Override
			public Response execute(UserLogin userLogin) throws Exception {
				final boolean blocked = target.getAutomaticRepaymentBlockingFlag(partyNo, userLogin);
				return Response.ok(blocked, WebServiceHelper.APPLICATION_JSON_WITH_UTF8_CHARSET_TYPE).build();
			}
		});
	}

	@Override
	public Response ping() {
		return execute("ping", READ_ACCESS_LOG, new ExecutionCallback() {
			@Override
			public Response execute() throws Exception {
				return Response.ok(DateHelper.getCurrentDate().getTime(), WebServiceHelper.TEXT_PLAIN_WITH_UTF8_CHARSET_TYPE).build();
			}
		});
	}

	@Override
	public Response getSecurityOnParty(final String user, final int partyNo) {
		final Object params = new Object() {
			@Override
			public String toString() {
				return String.format("getSecurityOnParty{user='%s', partyNo=%d}", user, partyNo);
			}
		};
		return execute(params, READ_ACCESS_LOG, new ExecutionCallback() {
			@Override
			public Response execute() throws Exception {
				final SecurityResponse response = target.getSecurityOnParty(user, partyNo);
				final MediaType preferred = getPreferredMediaTypeFromXmlOrJson();
				if (preferred == WebServiceHelper.APPLICATION_JSON_WITH_UTF8_CHARSET_TYPE) {
					return Response.ok(response, preferred).build();
				}
				else if (preferred == MediaType.APPLICATION_XML_TYPE) {
					return Response.ok(securityObjectFactory.createUserAccess(response), preferred).build();
				}
				return Response.status(Response.Status.UNSUPPORTED_MEDIA_TYPE).build();
			}
		});
	}

	@Override
	public Response getParty(int partyNo, String login,
	                         boolean withAddresses, boolean withTaxResidences, boolean withVirtualTaxResidences, boolean withManagingTaxResidences,
	                         boolean withHouseholdMembers, boolean withTaxLiabilities, boolean withSimplifiedTaxLiabilities, boolean withTaxationPeriods,
	                         boolean withRelationsBetweenParties, boolean withFamilyStatuses, boolean withTaxDeclarations, boolean withTaxDeclarationDeadlines,
	                         boolean withBankAccounts, boolean withLegalSeats, boolean withLegalForms, boolean withCapitals, boolean withTaxSystems,
	                         boolean withCorporationStatuses, boolean withDebtorPeriodicities, boolean withImmovableProperties,boolean withChildren,
	                         boolean withParents) {

		return WebServiceHelper.buildErrorResponse(Response.Status.SERVICE_UNAVAILABLE, getAcceptableMediaTypes(), "Implémentation encore en cours...");
	}

	@Override
	public Response getParties(String login, List<Integer> partyNos,
	                           boolean withAddresses, boolean withTaxResidences, boolean withVirtualTaxResidences, boolean withManagingTaxResidences,
	                           boolean withHouseholdMembers, boolean withTaxLiabilities, boolean withSimplifiedTaxLiabilities, boolean withTaxationPeriods,
	                           boolean withRelationsBetweenParties, boolean withFamilyStatuses, boolean withTaxDeclarations, boolean withTaxDeclarationDeadlines,
	                           boolean withBankAccounts, boolean withLegalSeats, boolean withLegalForms, boolean withCapitals, boolean withTaxSystems,
	                           boolean withCorporationStatuses, boolean withDebtorPeriodicities, boolean withImmovableProperties,boolean withChildren,
	                           boolean withParents) {

		return WebServiceHelper.buildErrorResponse(Response.Status.SERVICE_UNAVAILABLE, getAcceptableMediaTypes(), "Implémentation encore en cours...");
	}

	@Override
	public Response getTaxOffices(final int municipalityId, final String dateStr) {
		final Object params = new Object() {
			@Override
			public String toString() {
				return String.format("getTaxOffices{municipalityId=%d, date='%s'}", municipalityId, dateStr);
			}
		};
		return execute(params, READ_ACCESS_LOG, new ExecutionCallback() {
			@Override
			public Response execute() throws Exception {
				final RegDate date;
				if (StringUtils.isNotBlank(dateStr)) {
					try {
						date = RegDateHelper.displayStringToRegDate(dateStr, false);
					}
					catch (ParseException | IllegalArgumentException e) {
						return WebServiceHelper.buildErrorResponse(Response.Status.BAD_REQUEST, getAcceptableMediaTypes(), e);
					}
				}
				else {
					date = null;
				}

				final TaxOffices taxOffices = target.getTaxOffices(municipalityId, date);
				final MediaType preferred = getPreferredMediaTypeFromXmlOrJson();
				if (preferred == WebServiceHelper.APPLICATION_JSON_WITH_UTF8_CHARSET_TYPE) {
					return Response.ok(taxOffices, preferred).build();
				}
				else if (preferred == MediaType.APPLICATION_XML_TYPE) {
					return Response.ok(taxOfficesObjectFactory.createTaxOffices(taxOffices), preferred).build();
				}
				return Response.status(Response.Status.UNSUPPORTED_MEDIA_TYPE).build();
			}
		});
	}

	/**
	 * @return {@link WebServiceHelper#APPLICATION_JSON_WITH_UTF8_CHARSET_TYPE} si JSON est préféré, ou {@link MediaType#APPLICATION_XML_TYPE} si c'est XML, ou <code>null</code>
	 * si aucun des types acceptés par le client n'est compatible avec JSON ou XML
	 */
	private MediaType getPreferredMediaTypeFromXmlOrJson() {
		return WebServiceHelper.getPreferedMediaType(getAcceptableMediaTypes(),
		                                             new MediaType[] {MediaType.APPLICATION_XML_TYPE, WebServiceHelper.APPLICATION_JSON_WITH_UTF8_CHARSET_TYPE});
	}

	private List<MediaType> getAcceptableMediaTypes() {
		return messageContext.getHttpHeaders().getAcceptableMediaTypes();
	}

	@Override
	public Response ackOrdinaryTaxDeclarations(final String login, final OrdinaryTaxDeclarationAckRequest request) {
		final Object params = new Object() {
			@Override
			public String toString() {
				return String.format("ackOrdinaryTaxDeclarations{login='%s', request='%s'}", login, request);
			}
		};
		return execute(login, params, WRITE_ACCESS_LOG, new ExecutionWithLoginCallback() {
			@Override
			public Response execute(final UserLogin userLogin) throws Exception {
				final OrdinaryTaxDeclarationAckResponse response = target.ackOrdinaryTaxDeclarations(userLogin, request);
				return Response.ok(ackObjectFactory.createOrdinaryTaxDeclarationAckResponse(response)).build();
			}
		});
	}

	@Override
	public Response newOrdinaryTaxDeclarationDeadline(final int partyNo, final int pf, final int seqNo,
	                                                  final String login, final DeadlineRequest request) {
		final Object params = new Object() {
			@Override
			public String toString() {
				return String.format("newOrdinaryTaxDeclarationDeadline{login='%s', partyNo=%d, pf=%d, seqNo=%d, request=%s}", login, partyNo, pf, seqNo, request);
			}
		};
		return execute(login, params, WRITE_ACCESS_LOG, new ExecutionWithLoginCallback() {
			@Override
			public Response execute(UserLogin userLogin) throws Exception {
				final DeadlineResponse response = target.newOrdinaryTaxDeclarationDeadline(partyNo, pf, seqNo, userLogin, request);
				return Response.ok(deadlineObjectFactory.createDeadlineResponse(response)).build();
			}
		});
	}

	@Override
	public Response getModifiedTaxPayers(final String login, final Long since, final Long until) {
		final Object params = new Object() {
			@Override
			public String toString() {
				return String.format("getModifiedTaxPayers{login='%s', since=%d, until=%d}", login, since, until);
			}
		};
		return execute(login, params, READ_ACCESS_LOG, new ExecutionWithLoginCallback() {
			@Override
			public Response execute(UserLogin userLogin) throws Exception {
				if (since == null || until == null) {
					return WebServiceHelper.buildErrorResponse(Response.Status.BAD_REQUEST, getAcceptableMediaTypes(), "'since' and 'until' are required parameters.");
				}
				else if (since > until) {
					return WebServiceHelper.buildErrorResponse(Response.Status.BAD_REQUEST, getAcceptableMediaTypes(), "'since' should be before 'until'");
				}

				final Date sinceTimestamp = new Date(since);
				final Date untilTimestamp = new Date(until);
				final PartyNumberList response = target.getModifiedTaxPayers(userLogin, sinceTimestamp, untilTimestamp);
				final MediaType preferred = getPreferredMediaTypeFromXmlOrJson();
				if (preferred == WebServiceHelper.APPLICATION_JSON_WITH_UTF8_CHARSET_TYPE) {
					return Response.ok(response, preferred).build();
				}
				else if (preferred == MediaType.APPLICATION_XML_TYPE) {
					return Response.ok(modifiedTaxPayersFactory.createModifiedTayPayers(response), preferred).build();
				}
				return Response.status(Response.Status.UNSUPPORTED_MEDIA_TYPE).build();
			}
		});
	}
}
