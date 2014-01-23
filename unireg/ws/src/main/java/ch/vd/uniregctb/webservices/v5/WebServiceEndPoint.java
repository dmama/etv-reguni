package ch.vd.uniregctb.webservices.v5;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.text.SimpleDateFormat;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.log4j.Logger;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.shared.batchtemplate.BatchResults;
import ch.vd.shared.batchtemplate.BatchWithResultsCallback;
import ch.vd.shared.batchtemplate.Behavior;
import ch.vd.unireg.ws.ack.v1.AckStatus;
import ch.vd.unireg.ws.ack.v1.OrdinaryTaxDeclarationAckRequest;
import ch.vd.unireg.ws.ack.v1.OrdinaryTaxDeclarationAckResponse;
import ch.vd.unireg.ws.ack.v1.OrdinaryTaxDeclarationAckResult;
import ch.vd.unireg.ws.ack.v1.OrdinaryTaxDeclarationKey;
import ch.vd.unireg.ws.deadline.v1.DeadlineRequest;
import ch.vd.unireg.ws.deadline.v1.DeadlineResponse;
import ch.vd.unireg.ws.deadline.v1.DeadlineStatus;
import ch.vd.unireg.ws.security.v1.SecurityResponse;
import ch.vd.uniregctb.common.BatchTransactionTemplateWithResults;
import ch.vd.uniregctb.common.ObjectNotFoundException;
import ch.vd.uniregctb.common.TiersNotFoundException;
import ch.vd.uniregctb.declaration.Declaration;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.DelaiDeclaration;
import ch.vd.uniregctb.declaration.ordinaire.DeclarationImpotService;
import ch.vd.uniregctb.jms.BamMessageHelper;
import ch.vd.uniregctb.jms.BamMessageSender;
import ch.vd.uniregctb.load.DetailedLoadMeter;
import ch.vd.uniregctb.load.DetailedLoadMonitorable;
import ch.vd.uniregctb.load.LoadDetail;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityProviderInterface;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.transaction.TransactionTemplate;
import ch.vd.uniregctb.type.Niveau;
import ch.vd.uniregctb.type.TypeEtatDeclaration;
import ch.vd.uniregctb.webservices.common.AccessDeniedException;
import ch.vd.uniregctb.webservices.common.UserLogin;
import ch.vd.uniregctb.webservices.common.WebServiceHelper;

public class WebServiceEndPoint implements DetailedLoadMonitorable {

	private static final Logger LOGGER = Logger.getLogger(WebServiceEndPoint.class);
	private static final Logger READ_ACCESS_LOG = Logger.getLogger("ws.v5.read");
	private static final Logger WRITE_ACCESS_LOG = Logger.getLogger("ws.v5.write");

	private static final int DECLARATION_ACK_BATCH_SIZE = 50;

	@Context
	private MessageContext messageContext;

	/**
	 * Moniteur des appels en cours
	 */
	private final DetailedLoadMeter<Object> loadMeter = new DetailedLoadMeter<>();

	private final ch.vd.unireg.ws.security.v1.ObjectFactory securityObjectFactory = new ch.vd.unireg.ws.security.v1.ObjectFactory();
	private final ch.vd.unireg.ws.ack.v1.ObjectFactory ackObjectFactory = new ch.vd.unireg.ws.ack.v1.ObjectFactory();
	private final ch.vd.unireg.ws.deadline.v1.ObjectFactory deadlineObjectFactory = new ch.vd.unireg.ws.deadline.v1.ObjectFactory();

	private SecurityProviderInterface securityProvider;
	private PlatformTransactionManager transactionManager;
	private TiersService tiersService;
	private DeclarationImpotService diService;
	private BamMessageSender bamSender;

	@Override
	public List<LoadDetail> getLoadDetails() {
		return loadMeter.getLoadDetails();
	}

	@Override
	public int getLoad() {
		return loadMeter.getLoad();
	}

	public void setSecurityProvider(SecurityProviderInterface securityProvider) {
		this.securityProvider = securityProvider;
	}

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	public void setDiService(DeclarationImpotService diService) {
		this.diService = diService;
	}

	public void setBamSender(BamMessageSender bamSender) {
		this.bamSender = bamSender;
	}

	private <T> T doInTransaction(boolean readonly, TransactionCallback<T> callback) {
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(readonly);
		return template.execute(callback);
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

	@PUT
	@Path("/repayment/{partyNo}/blocked")
	public Response setBlocageRemboursementAuto(@PathParam("partyNo") final int partyNo,
	                                            @QueryParam("login") final String login,
	                                            @QueryParam("value") final Boolean blocked) {

		final Object params = new Object() {
			@Override
			public String toString() {
				return String.format("setBlockageRemboursementAuto{partyNo=%d, login='%s', blocked=%s}", partyNo, login, blocked);
			}
		};
		return execute(login, params, WRITE_ACCESS_LOG, new ExecutionCallback() {
			@Override
			public Response execute(UserLogin userLogin) throws Exception {

				if (blocked == null) {
					LOGGER.error("Missing 'value' parameter");
					return WebServiceHelper.buildErrorResponse(Response.Status.BAD_REQUEST, "Missing 'value' parameter.");
				}

				WebServiceHelper.checkAccess(securityProvider, userLogin, Role.VISU_ALL);
				WebServiceHelper.checkPartyReadWriteAccess(securityProvider, userLogin, partyNo);

				doInTransaction(false, new TransactionCallbackWithoutResult() {
					@Override
					protected void doInTransactionWithoutResult(TransactionStatus status) {
						final Tiers tiers = tiersService.getTiers(partyNo);
						tiers.setBlocageRemboursementAutomatique(blocked);
					}
				});

				return Response.ok().build();
			}
		});
	}

	@GET
	@Produces("text/plain;charset=UTF-8")
	@Path("/status/ping")
	public Response ping() {
		Throwable t = null;
		final long start = loadMeter.start("ping");
		try {
			// le nombre de millisecondes depuis le 01.01.1970 0:00:00 GMT
			return Response.ok(DateHelper.getCurrentDate().getTime()).build();
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

	@GET
	@Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
	@Path("/security/{user}/{partyNo}")
	public Response getSecurityOnParty(@PathParam("user") final String user, @PathParam("partyNo") final int partyNo) {
		Throwable t = null;
		final Object params = new Object() {
			@Override
			public String toString() {
				return String.format("getSecurityOnParty{user='%s', partyNo=%d}", user, partyNo);
			}
		};

		final long start = loadMeter.start(params);
		try {
			final Niveau niveau = securityProvider.getDroitAcces(user, partyNo);
			final SecurityResponse response = new SecurityResponse(user, partyNo, EnumHelper.toXml(niveau));

			final MediaType preferred = WebServiceHelper.getPreferedMediaType(messageContext.getHttpHeaders().getAcceptableMediaTypes(),
			                                                                  new MediaType[] {MediaType.APPLICATION_XML_TYPE, MediaType.APPLICATION_JSON_TYPE});
			if (preferred == MediaType.APPLICATION_JSON_TYPE) {
				return Response.ok(response, MediaType.APPLICATION_JSON_TYPE).build();
			}
			else if (preferred == MediaType.APPLICATION_XML_TYPE) {
				return Response.ok(securityObjectFactory.createUserAccess(response), MediaType.APPLICATION_XML_TYPE).build();
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

	@GET
	@Produces(MediaType.APPLICATION_XML)
	@Path("/party/{partyNo}")
	public Response getParty(@PathParam("partyNo") int partyNo,
	                         @QueryParam("login") String login,
	                         @QueryParam("withAddresses") @DefaultValue("false") boolean withAddresses,
	                         @QueryParam("withTaxResidences") @DefaultValue("false") boolean withTaxResidences,
	                         @QueryParam("withVirtualTaxResidences") @DefaultValue("false") boolean withVirtualTaxResidences,
	                         @QueryParam("withManagingTaxResidences") @DefaultValue("false") boolean withManagingTaxResidences,
	                         @QueryParam("withHouseholdMembers") @DefaultValue("false") boolean withHouseholdMembers,
	                         @QueryParam("withTaxLiabilities") @DefaultValue("false") boolean withTaxLiabilities,
	                         @QueryParam("withSimplifiedTaxLiabilities") @DefaultValue("false") boolean withSimplifiedTaxLiabilities,
	                         @QueryParam("withTaxationPeriods") @DefaultValue("false") boolean withTaxationPeriods,
	                         @QueryParam("withRelationsBetweenParties") @DefaultValue("false") boolean withRelationsBetweenParties,
	                         @QueryParam("withFamilyStatuses") @DefaultValue("false") boolean withFamilyStatuses,
	                         @QueryParam("withTaxDeclarations") @DefaultValue("false") boolean withTaxDeclarations,
	                         @QueryParam("withTaxDeclarationDeadlines") @DefaultValue("false") boolean withTaxDeclarationDeadlines,
	                         @QueryParam("withBankAccounts") @DefaultValue("false") boolean withBankAccounts,
	                         @QueryParam("withLegalSeats") @DefaultValue("false") boolean withLegalSeats,
	                         @QueryParam("withLegalForms") @DefaultValue("false") boolean withLegalForms,
	                         @QueryParam("withCapitals") @DefaultValue("false") boolean withCapitals,
	                         @QueryParam("withTaxSystems") @DefaultValue("false") boolean withTaxSystems,
	                         @QueryParam("withCorporationStatuses") @DefaultValue("false") boolean withCorporationStatuses,
	                         @QueryParam("withDebtorPeriodicities") @DefaultValue("false") boolean withDebtorPeriodicities,
	                         @QueryParam("withImmovableProperties") @DefaultValue("false") boolean withImmovableProperties,
	                         @QueryParam("withChildren") @DefaultValue("false") boolean withChildren,
	                         @QueryParam("withParents") @DefaultValue("false") boolean withParents) {

		return WebServiceHelper.buildErrorResponse(Response.Status.SERVICE_UNAVAILABLE, "Implémentation encore en cours...");
	}

	@GET
	@Produces(MediaType.APPLICATION_XML)
	@Path("/parties")
	public Response getParties(@QueryParam("login") String login,
	                           @QueryParam("partyNo") List<Integer> partyNos,
	                           @QueryParam("withAddresses") @DefaultValue("false") boolean withAddresses,
	                           @QueryParam("withTaxResidences") @DefaultValue("false") boolean withTaxResidences,
	                           @QueryParam("withVirtualTaxResidences") @DefaultValue("false") boolean withVirtualTaxResidences,
	                           @QueryParam("withManagingTaxResidences") @DefaultValue("false") boolean withManagingTaxResidences,
	                           @QueryParam("withHouseholdMembers") @DefaultValue("false") boolean withHouseholdMembers,
	                           @QueryParam("withTaxLiabilities") @DefaultValue("false") boolean withTaxLiabilities,
	                           @QueryParam("withSimplifiedTaxLiabilities") @DefaultValue("false") boolean withSimplifiedTaxLiabilities,
	                           @QueryParam("withTaxationPeriods") @DefaultValue("false") boolean withTaxationPeriods,
	                           @QueryParam("withRelationsBetweenParties") @DefaultValue("false") boolean withRelationsBetweenParties,
	                           @QueryParam("withFamilyStatuses") @DefaultValue("false") boolean withFamilyStatuses,
	                           @QueryParam("withTaxDeclarations") @DefaultValue("false") boolean withTaxDeclarations,
	                           @QueryParam("withTaxDeclarationDeadlines") @DefaultValue("false") boolean withTaxDeclarationDeadlines,
	                           @QueryParam("withBankAccounts") @DefaultValue("false") boolean withBankAccounts,
	                           @QueryParam("withLegalSeats") @DefaultValue("false") boolean withLegalSeats,
	                           @QueryParam("withLegalForms") @DefaultValue("false") boolean withLegalForms,
	                           @QueryParam("withCapitals") @DefaultValue("false") boolean withCapitals,
	                           @QueryParam("withTaxSystems") @DefaultValue("false") boolean withTaxSystems,
	                           @QueryParam("withCorporationStatuses") @DefaultValue("false") boolean withCorporationStatuses,
	                           @QueryParam("withDebtorPeriodicities") @DefaultValue("false") boolean withDebtorPeriodicities,
	                           @QueryParam("withImmovableProperties") @DefaultValue("false") boolean withImmovableProperties,
	                           @QueryParam("withChildren") @DefaultValue("false") boolean withChildren,
	                           @QueryParam("withParents") @DefaultValue("false") boolean withParents) {

		return WebServiceHelper.buildErrorResponse(Response.Status.SERVICE_UNAVAILABLE, "Implémentation encore en cours...");
	}

	@GET
	@Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
	@Path("/taxOffices/{municipalityId}")
	public Response getTaxOffices(@PathParam("municipalityId") int ofsCommune) {
		return WebServiceHelper.buildErrorResponse(Response.Status.SERVICE_UNAVAILABLE, "Implémentation encore en cours...");
	}

	/**
	 * Classe de résultats pour l'utilisation du {@link ch.vd.uniregctb.common.BatchTransactionTemplateWithResults} dans le quittancement de DI
	 */
	private static final class OrdinaryTaxDeclarationAckBatchResult implements BatchResults<OrdinaryTaxDeclarationKey, OrdinaryTaxDeclarationAckBatchResult> {

		private final List<OrdinaryTaxDeclarationAckResult> list = new LinkedList<>();

		@Override
		public void addErrorException(OrdinaryTaxDeclarationKey key, Exception e) {
			list.add(new OrdinaryTaxDeclarationAckResult(key, AckStatus.UNEXPECTED_ERROR, WebServiceHelper.buildExceptionMessage(e)));
		}

		public void addCasTraite(OrdinaryTaxDeclarationKey key, AckStatus status, String message) {
			list.add(new OrdinaryTaxDeclarationAckResult(key, status, message));
		}

		@Override
		public void addAll(OrdinaryTaxDeclarationAckBatchResult ordinaryTaxDeclarationAckBatchResult) {
			list.addAll(ordinaryTaxDeclarationAckBatchResult.getList());
		}

		public List<OrdinaryTaxDeclarationAckResult> getList() {
			return list;
		}
	}

	/**
	 * Envoi du quittancement vers le BAM
	 * @param di la DI quittancée
	 * @param dateQuittancement date de quittancement de la DI
	 */
	private void sendQuittancementToBam(DeclarationImpotOrdinaire di, RegDate dateQuittancement) {
		final long ctbId = di.getTiers().getNumero();
		final int annee = di.getPeriode().getAnnee();
		final int noSequence = di.getNumero();
		try {
			final Map<String, String> bamHeaders = BamMessageHelper.buildCustomBamHeadersForQuittancementDeclaration(di, dateQuittancement, null);
			final String businessId = String.format("%d-%d-%d-%s", ctbId, annee, noSequence, new SimpleDateFormat("yyyyMMddHHmmssSSS").format(DateHelper.getCurrentDate()));
			final String processDefinitionId = BamMessageHelper.PROCESS_DEFINITION_ID_PAPIER;       // pour le moment, tous les quittancements par le WS concenent les DI "papier"
			final String processInstanceId = BamMessageHelper.buildProcessInstanceId(di);
			bamSender.sendBamMessageQuittancementDi(processDefinitionId, processInstanceId, businessId, ctbId, annee, bamHeaders);
		}
		catch (RuntimeException e) {
			throw e;
		}
		catch (Exception e) {
			throw new RuntimeException(String.format("Erreur à la notification au BAM du quittancement de la DI %d (%d) du contribuable %d", annee, noSequence, ctbId), e);
		}
	}

	/**
	 * Recherche la declaration pour une année et un numéro de déclaration dans l'année
	 *
	 * @param contribuable     un contribuable
	 * @param annee            une période fiscale complète (ex. 2010)
	 * @param numeroSequenceDI le numéro de séquence de la déclaration pour le contribuable et la période considérés
	 * @return une déclaration d'impôt ordinaire, ou <b>null</b> si aucune déclaration correspondant aux critère n'est trouvée.
	 */
	private static DeclarationImpotOrdinaire findDeclaration(Contribuable contribuable, int annee, int numeroSequenceDI) {

		// [SIFISC-1227] Nous avons des cas où le numéro de séquence a été ré-utilisé après annulation d'une DI précédente
		// -> on essaie toujours de renvoyer la déclaration non-annulée qui correspond et, s'il n'y en a pas, de renvoyer
		// la dernière déclaration annulée trouvée

		DeclarationImpotOrdinaire declaration = null;
		DeclarationImpotOrdinaire declarationAnnuleeTrouvee = null;
		final List<Declaration> declarations = contribuable.getDeclarationsForPeriode(annee, true);
		if (declarations != null && !declarations.isEmpty()) {
			for (Declaration d : declarations) {
				final DeclarationImpotOrdinaire di = (DeclarationImpotOrdinaire) d;
				if (numeroSequenceDI == 0) {
					// Dans le cas où le numero dans l'année n'est pas spécifié on prend la dernière DI trouvée sur la période
					declaration = di;
				}
				else if (di.getNumero() == numeroSequenceDI) {
					if (di.isAnnule()) {
						declarationAnnuleeTrouvee = di;
					}
					else {
						declaration = di;
						break;
					}
				}
			}
		}

		return declaration != null ? declaration : declarationAnnuleeTrouvee;
	}

	@POST
	@Consumes(MediaType.APPLICATION_XML)
	@Produces(MediaType.APPLICATION_XML)
	@Path("/ackOrdinaryTaxDeclarations")
	public Response quittancerDeclarations(@QueryParam("login") final String login, final OrdinaryTaxDeclarationAckRequest request) {

		final Object params = new Object() {
			@Override
			public String toString() {
				return String.format("quittancerDeclarations{login='%s', request='%s'}", login, request);
			}
		};
		return execute(login, params, WRITE_ACCESS_LOG, new ExecutionCallback() {
			@Override
			public Response execute(final UserLogin userLogin) throws Exception {
				WebServiceHelper.checkAccess(securityProvider, userLogin, Role.DI_QUIT_PP);

				final RegDate dateRetour = ch.vd.uniregctb.xml.DataHelper.xmlToCore(request.getDate());
				final String source = request.getSource();

				final OrdinaryTaxDeclarationAckBatchResult result = new OrdinaryTaxDeclarationAckBatchResult();
				final BatchTransactionTemplateWithResults<OrdinaryTaxDeclarationKey, OrdinaryTaxDeclarationAckBatchResult> template = new BatchTransactionTemplateWithResults<>(request.getDeclaration(),
				                                                                                                                                                                DECLARATION_ACK_BATCH_SIZE,
				                                                                                                                                                                Behavior.REPRISE_AUTOMATIQUE,
				                                                                                                                                                                transactionManager, null);
				template.execute(result, new BatchWithResultsCallback<OrdinaryTaxDeclarationKey, OrdinaryTaxDeclarationAckBatchResult>() {
					@Override
					public boolean doInTransaction(List<OrdinaryTaxDeclarationKey> keys, OrdinaryTaxDeclarationAckBatchResult result) throws Exception {
						for (OrdinaryTaxDeclarationKey key : keys) {
							quittancerDeclaration(userLogin, key, source, dateRetour, result);
						}
						return true;
					}

					@Override
					public OrdinaryTaxDeclarationAckBatchResult createSubRapport() {
						return new OrdinaryTaxDeclarationAckBatchResult();
					}
				}, null);

				final OrdinaryTaxDeclarationAckResponse response = new OrdinaryTaxDeclarationAckResponse(result.getList());
				return Response.ok(ackObjectFactory.createOrdinaryTaxDeclarationAckResponse(response)).build();
			}
		});
	}

	private void quittancerDeclaration(UserLogin userLogin, OrdinaryTaxDeclarationKey key, String source, RegDate dateRetour, OrdinaryTaxDeclarationAckBatchResult result) {
		try {
			final int partyNo = key.getPartyNo();
			final int pf = key.getTaxPeriod();
			final int noSeq = key.getSequenceNo();

			// TODO JDE : faut-il faire ce test ? Il n'était pas fait dans les versions précédentes du service...
			WebServiceHelper.checkPartyReadWriteAccess(securityProvider, userLogin, partyNo);

			final Tiers tiers = tiersService.getTiers(partyNo);
			if (tiers == null) {
				result.addCasTraite(key, AckStatus.ERROR_UNKNOWN_PARTY, null);
			}
			else if (tiers.isDebiteurInactif()) {
				result.addCasTraite(key, AckStatus.ERROR_INACTIVE_DEBTOR, null);
			}
			else if (tiers.getDernierForFiscalPrincipal() == null) {
				result.addCasTraite(key, AckStatus.ERROR_TAX_LIABILITY, "Aucun for principal.");
			}
			else if (tiers instanceof Contribuable) {
				final Contribuable ctb = (Contribuable) tiers;
				final DeclarationImpotOrdinaire di = findDeclaration(ctb, pf, noSeq);
				if (di == null) {
					result.addCasTraite(key, AckStatus.ERROR_UNKNOWN_TAX_DECLARATION, null);
				}
				else if (di.isAnnule()) {
					result.addCasTraite(key, AckStatus.ERROR_CANCELED_TAX_DECLARATION, null);
				}
				else if (RegDateHelper.isBeforeOrEqual(dateRetour, di.getDateExpedition(), NullDateBehavior.EARLIEST)) {
					result.addCasTraite(key, AckStatus.ERROR_INVALID_ACK_DATE, "Date donnée antérieure à la date d'émission de la déclaration.");
				}
				else if (RegDateHelper.isAfter(dateRetour, RegDate.get(), NullDateBehavior.LATEST)) {
					result.addCasTraite(key, AckStatus.ERROR_INVALID_ACK_DATE, "Date donnée dans le futur de la date du jour.");
				}
				else {
					// envoie le quittancement au BAM
					sendQuittancementToBam(di, dateRetour);

					// procéde au quittancement
					diService.quittancementDI(ctb, di, dateRetour, source, true);

					// tout est bon...
					result.addCasTraite(key, AckStatus.OK, null);
				}
			}
			else {
				result.addCasTraite(key, AckStatus.ERROR_WRONG_PARTY_TYPE, "Le tiers donné n'est pas un contribuable.");
			}
		}
		catch (AccessDeniedException e) {
			result.addCasTraite(key, AckStatus.ERROR_ACCESS_DENIED, WebServiceHelper.buildExceptionMessage(e));
		}
		catch (ObjectNotFoundException e) {
			result.addCasTraite(key, AckStatus.ERROR_UNKNOWN_PARTY, WebServiceHelper.buildExceptionMessage(e));
		}
		catch (Throwable e) {
			result.addCasTraite(key, AckStatus.UNEXPECTED_ERROR, WebServiceHelper.buildExceptionMessage(e));
		}
	}

	@POST
	@Path("/newOrdinaryTaxDeclarationDeadline/{partyNo}/{fiscalPeriod}/{sequenceNo}")
	@Consumes(MediaType.APPLICATION_XML)
	@Produces(MediaType.APPLICATION_XML)
	public Response nouveauDelaiPourDeclarationOrdinaire(@PathParam("partyNo") final int partyNo,
	                                                     @PathParam("fiscalPeriod") final int pf,
	                                                     @PathParam("sequenceNo") final int seqNo,
	                                                     @QueryParam("login") final String login,
	                                                     final DeadlineRequest request) {

		final Object params = new Object() {
			@Override
			public String toString() {
				return String.format("nouveauDelaiPourDeclarationOrdinaire{login='%s', partyNo=%d, pf=%d, seqNo=%d, request=%s", login, partyNo, pf, seqNo, request);
			}
		};
		return execute(login, params, WRITE_ACCESS_LOG, new ExecutionCallback() {
			@Override
			public Response execute(UserLogin userLogin) throws Exception {
				WebServiceHelper.checkAccess(securityProvider, userLogin, Role.DI_DELAI_PP);

				final RegDate nouveauDelai = ch.vd.uniregctb.xml.DataHelper.xmlToCore(request.getNewDeadline());
				final RegDate dateObtention = ch.vd.uniregctb.xml.DataHelper.xmlToCore(request.getGrantedOn());
				final RegDate today = RegDate.get();

				return doInTransaction(false, new TransactionCallback<Response>() {
					@Override
					public Response doInTransaction(TransactionStatus status) {

						final Tiers tiers = tiersService.getTiers(partyNo);
						if (tiers == null) {
							throw new TiersNotFoundException(partyNo);
						}
						else if (tiers instanceof Contribuable) {
							final Contribuable ctb = (Contribuable) tiers;
							final DeclarationImpotOrdinaire di = findDeclaration(ctb, pf, seqNo);
							if (di == null) {
								return WebServiceHelper.buildErrorResponse(Response.Status.NOT_FOUND, "Déclaration d'impôt inexistante.");
							}
							else {
								final DeadlineResponse response;
								if (di.isAnnule()) {
									response = new DeadlineResponse(DeadlineStatus.ERROR_CANCELLED_DECLARATION, null);
								}
								else if (di.getDernierEtat().getEtat() != TypeEtatDeclaration.EMISE) {
									response = new DeadlineResponse(DeadlineStatus.ERROR_BAD_DECLARATION_STATUS, "La déclaration n'est pas dans l'état 'EMISE'.");
								}
								else if (RegDateHelper.isAfter(dateObtention, today, NullDateBehavior.LATEST)) {
									response = new DeadlineResponse(DeadlineStatus.ERROR_INVALID_GRANTED_ON, "La date d'obtention du délai ne peut pas être dans le futur de la date du jour.");
								}
								else if (RegDateHelper.isBefore(nouveauDelai, today, NullDateBehavior.LATEST)) {
									response = new DeadlineResponse(DeadlineStatus.ERROR_INVALID_DEADLINE, "Un nouveau délai ne peut pas être demandé dans le passé de la date du jour.");
								}
								else {
									final RegDate delaiActuel = di.getDernierDelais().getDelaiAccordeAu();
									if (RegDateHelper.isBeforeOrEqual(nouveauDelai, delaiActuel, NullDateBehavior.LATEST)) {
										response = new DeadlineResponse(DeadlineStatus.ERROR_INVALID_DEADLINE, "Un délai plus lointain existe déjà.");
									}
									else {
										final DelaiDeclaration delai = new DelaiDeclaration();
										delai.setDateTraitement(RegDate.get());
										delai.setConfirmationEcrite(false);
										delai.setDateDemande(dateObtention);
										delai.setDelaiAccordeAu(nouveauDelai);
										di.addDelai(delai);

										response = new DeadlineResponse(DeadlineStatus.OK, null);
									}
								}

								return Response.ok(deadlineObjectFactory.createDeadlineResponse(response)).build();
							}
						}
						else {
							return WebServiceHelper.buildErrorResponse(Response.Status.NOT_FOUND, "Le tiers donné n'est pas un contribuable.");
						}
					}
				});
			}
		});
	}
}
