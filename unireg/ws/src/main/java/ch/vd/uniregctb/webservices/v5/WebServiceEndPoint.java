package ch.vd.uniregctb.webservices.v5;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.log4j.Logger;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.unireg.ws.ack.v1.OrdinaryTaxDeclarationAckRequest;
import ch.vd.unireg.ws.ack.v1.OrdinaryTaxDeclarationAckResponse;
import ch.vd.unireg.ws.deadline.v1.DeadlineRequest;
import ch.vd.unireg.ws.deadline.v1.DeadlineResponse;
import ch.vd.unireg.ws.security.v1.SecurityResponse;
import ch.vd.uniregctb.common.ObjectNotFoundException;
import ch.vd.uniregctb.load.DetailedLoadMeter;
import ch.vd.uniregctb.load.DetailedLoadMonitorable;
import ch.vd.uniregctb.load.LoadDetail;
import ch.vd.uniregctb.webservices.common.AccessDeniedException;
import ch.vd.uniregctb.webservices.common.UserLogin;
import ch.vd.uniregctb.webservices.common.WebServiceHelper;

public class WebServiceEndPoint implements WebService, DetailedLoadMonitorable {

	private static final MediaType APPLICATION_JSON_WITH_UTF8_CHARSET_TYPE = MediaType.valueOf(APPLICATION_JSON_WITH_UTF8_CHARSET);
	private static final MediaType TEXT_PLAIN_WITH_UTF8_CHARSET_TYPE = MediaType.valueOf(TEXT_PLAIN_WITH_UTF8_CHARSET);

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
		Response execute(UserLogin userLogin) throws Exception;
	}

	private Response execute(String login, Object callDescription, Logger accessLog, ExecutionCallback callback) {
		Throwable t = null;
		final long start = loadMeter.start(callDescription);
		try {
			final UserLogin userLogin = WebServiceHelper.parseLoginParameter(login);
			if (userLogin == null) {
				LOGGER.error("Missing/invalid login (" + login + ")");
				return WebServiceHelper.buildErrorResponse(Response.Status.BAD_REQUEST, "Missing/invalid login parameter.");
			}

			WebServiceHelper.login(userLogin);
			try {
				return callback.execute(userLogin);
			}
			finally {
				WebServiceHelper.logout();
			}
		}
		catch (AccessDeniedException e) {
			t = e;
			LOGGER.error(e.getMessage());
			return WebServiceHelper.buildErrorResponse(Response.Status.FORBIDDEN, e);
		}
		catch (ObjectNotFoundException e) {
			t = e;
			LOGGER.error(e.getMessage());
			return WebServiceHelper.buildErrorResponse(Response.Status.NOT_FOUND, e);
		}
		catch (Throwable e) {
			t = e;
			LOGGER.error(e, e);
			return WebServiceHelper.buildErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, e);
		}
		finally {
			final long end = loadMeter.end();
			WebServiceHelper.logAccessInfo(accessLog, messageContext.getHttpServletRequest(), callDescription, end - start, getLoad() + 1, t);
		}
	}

	@Override
	public Response setAutomaticRepaymentBlockingFlag(final int partyNo, final String login, final String value) {

		final Object params = new Object() {
			@Override
			public String toString() {
				return String.format("setAutomaticRepaymentBlockingFlag{partyNo=%d, login='%s', value='%s'}", partyNo, login, value);
			}
		};
		return execute(login, params, WRITE_ACCESS_LOG, new ExecutionCallback() {
			@Override
			public Response execute(UserLogin userLogin) throws Exception {
				if (value == null || !BOOLEAN_PATTERN.matcher(value).matches()) {
					LOGGER.error("Wrong or missing new flag value");
					return WebServiceHelper.buildErrorResponse(Response.Status.BAD_REQUEST, "Wrong or missing new flag value.");
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
		return execute(login, params, READ_ACCESS_LOG, new ExecutionCallback() {
			@Override
			public Response execute(UserLogin userLogin) throws Exception {
				final boolean blocked = target.getAutomaticRepaymentBlockingFlag(partyNo, userLogin);
				return Response.ok(blocked, APPLICATION_JSON_WITH_UTF8_CHARSET_TYPE).build();
			}
		});
	}

	@Override
	public Response ping() {
		Throwable t = null;
		final long start = loadMeter.start("ping");
		try {
			// le nombre de millisecondes depuis le 01.01.1970 0:00:00 GMT
			return Response.ok(DateHelper.getCurrentDate().getTime(), TEXT_PLAIN_WITH_UTF8_CHARSET_TYPE).build();
		}
		catch (RuntimeException | Error e) {
			t = e;
			LOGGER.error(e, e);
			return WebServiceHelper.buildErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, e);
		}
		finally {
			final long end = loadMeter.end();
			// getLoad()+1 : +1 car le end() a déjà décompté l'appel courant
			WebServiceHelper.logAccessInfo(READ_ACCESS_LOG, messageContext.getHttpServletRequest(), "ping", end - start, getLoad() + 1, t);
		}
	}

	@Override
	public Response getSecurityOnParty(final String user, final int partyNo) {
		Throwable t = null;
		final Object params = new Object() {
			@Override
			public String toString() {
				return String.format("getSecurityOnParty{user='%s', partyNo=%d}", user, partyNo);
			}
		};

		final long start = loadMeter.start(params);
		try {

			final SecurityResponse response = target.getSecurityOnParty(user, partyNo);
			final MediaType preferred = WebServiceHelper.getPreferedMediaType(messageContext.getHttpHeaders().getAcceptableMediaTypes(),
			                                                                  new MediaType[] {MediaType.APPLICATION_XML_TYPE, APPLICATION_JSON_WITH_UTF8_CHARSET_TYPE});
			if (preferred == APPLICATION_JSON_WITH_UTF8_CHARSET_TYPE) {
				return Response.ok(response, preferred).build();
			}
			else if (preferred == MediaType.APPLICATION_XML_TYPE) {
				return Response.ok(securityObjectFactory.createUserAccess(response), preferred).build();
			}
			return Response.status(Response.Status.UNSUPPORTED_MEDIA_TYPE).build();
		}
		catch (ObjectNotFoundException e) {
			t = e;
			LOGGER.error(e.getMessage());
			return WebServiceHelper.buildErrorResponse(Response.Status.NOT_FOUND, e);
		}
		catch (RuntimeException | Error e) {
			t = e;
			LOGGER.error(e, e);
			return WebServiceHelper.buildErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, e);
		}
		finally {
			final long end = loadMeter.end();
			WebServiceHelper.logAccessInfo(READ_ACCESS_LOG, messageContext.getHttpServletRequest(), params, end - start, getLoad() + 1, t);
		}
	}

	@Override
	public Response getParty(int partyNo, String login,
	                         boolean withAddresses, boolean withTaxResidences, boolean withVirtualTaxResidences, boolean withManagingTaxResidences,
	                         boolean withHouseholdMembers, boolean withTaxLiabilities, boolean withSimplifiedTaxLiabilities, boolean withTaxationPeriods,
	                         boolean withRelationsBetweenParties, boolean withFamilyStatuses, boolean withTaxDeclarations, boolean withTaxDeclarationDeadlines,
	                         boolean withBankAccounts, boolean withLegalSeats, boolean withLegalForms, boolean withCapitals, boolean withTaxSystems,
	                         boolean withCorporationStatuses, boolean withDebtorPeriodicities, boolean withImmovableProperties,boolean withChildren,
	                         boolean withParents) {

		return WebServiceHelper.buildErrorResponse(Response.Status.SERVICE_UNAVAILABLE, "Implémentation encore en cours...");
	}

	@Override
	public Response getParties(String login, List<Integer> partyNos,
	                           boolean withAddresses, boolean withTaxResidences, boolean withVirtualTaxResidences, boolean withManagingTaxResidences,
	                           boolean withHouseholdMembers, boolean withTaxLiabilities, boolean withSimplifiedTaxLiabilities, boolean withTaxationPeriods,
	                           boolean withRelationsBetweenParties, boolean withFamilyStatuses, boolean withTaxDeclarations, boolean withTaxDeclarationDeadlines,
	                           boolean withBankAccounts, boolean withLegalSeats, boolean withLegalForms, boolean withCapitals, boolean withTaxSystems,
	                           boolean withCorporationStatuses, boolean withDebtorPeriodicities, boolean withImmovableProperties,boolean withChildren,
	                           boolean withParents) {

		return WebServiceHelper.buildErrorResponse(Response.Status.SERVICE_UNAVAILABLE, "Implémentation encore en cours...");
	}

	@Override
	public Response getTaxOffices(int ofsCommune) {
		return WebServiceHelper.buildErrorResponse(Response.Status.SERVICE_UNAVAILABLE, "Implémentation encore en cours...");
	}

	@Override
	public Response ackOrdinaryTaxDeclarations(final String login, final OrdinaryTaxDeclarationAckRequest request) {

		final Object params = new Object() {
			@Override
			public String toString() {
				return String.format("ackOrdinaryTaxDeclarations{login='%s', request='%s'}", login, request);
			}
		};
		return execute(login, params, WRITE_ACCESS_LOG, new ExecutionCallback() {
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
				return String.format("newOrdinaryTaxDeclarationDeadline{login='%s', partyNo=%d, pf=%d, seqNo=%d, request=%s", login, partyNo, pf, seqNo, request);
			}
		};
		return execute(login, params, WRITE_ACCESS_LOG, new ExecutionCallback() {
			@Override
			public Response execute(UserLogin userLogin) throws Exception {
				final DeadlineResponse response = target.newOrdinaryTaxDeclarationDeadline(partyNo, pf, seqNo, userLogin, request);
				return Response.ok(deadlineObjectFactory.createDeadlineResponse(response)).build();
			}
		});
	}
}
