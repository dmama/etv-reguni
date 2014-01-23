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
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

import ch.vd.unireg.ws.ack.v1.OrdinaryTaxDeclarationAckRequest;
import ch.vd.unireg.ws.deadline.v1.DeadlineRequest;
import ch.vd.uniregctb.webservices.common.WebServiceHelper;

public interface WebService {

	@GET
	@Produces(WebServiceHelper.TEXT_PLAIN_WITH_UTF8_CHARSET)
	@Path("/status/ping")
	Response ping();

	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces({MediaType.APPLICATION_XML, WebServiceHelper.APPLICATION_JSON_WITH_UTF8_CHARSET})
	@Path("/repayment/{partyNo}/blocked")
	Response setAutomaticRepaymentBlockingFlag(@PathParam("partyNo") int partyNo, @QueryParam("user") String user, String value);

	@GET
	@Produces(WebServiceHelper.APPLICATION_JSON_WITH_UTF8_CHARSET)
	@Path("/repayment/{partyNo}/blocked")
	Response getAutomaticRepaymentBlockingFlag(@PathParam("partyNo") int partyNo, @QueryParam("user") String user);

	@GET
	@Produces({MediaType.APPLICATION_XML, WebServiceHelper.APPLICATION_JSON_WITH_UTF8_CHARSET})
	@Path("/security/{user}/{partyNo}")
	Response getSecurityOnParty(@PathParam("user") String user, @PathParam("partyNo") int partyNo);

	@GET
	@Produces(MediaType.APPLICATION_XML)
	@Path("/party/{partyNo}")
	Response getParty(@PathParam("partyNo") int partyNo,
	                  @QueryParam("user") String user,
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
	                  @QueryParam("withParents") @DefaultValue("false") boolean withParents);

	@GET
	@Produces(MediaType.APPLICATION_XML)
	@Path("/parties")
	Response getParties(@QueryParam("user") String user,
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
	                    @QueryParam("withParents") @DefaultValue("false") boolean withParents);


	@GET
	@Produces({MediaType.APPLICATION_XML, WebServiceHelper.APPLICATION_JSON_WITH_UTF8_CHARSET})
	@Path("/taxOffices/{municipalityId}")
	Response getTaxOffices(@PathParam("municipalityId") int municipalityId, @QueryParam("date") String date);

	@POST
	@Consumes(MediaType.APPLICATION_XML)
	@Produces(MediaType.APPLICATION_XML)
	@Path("/ackOrdinaryTaxDeclarations")
	Response ackOrdinaryTaxDeclarations(@QueryParam("user") String user, OrdinaryTaxDeclarationAckRequest request);

	@POST
	@Path("/newOrdinaryTaxDeclarationDeadline/{partyNo}/{fiscalPeriod}/{sequenceNo}")
	@Consumes(MediaType.APPLICATION_XML)
	@Produces(MediaType.APPLICATION_XML)
	Response newOrdinaryTaxDeclarationDeadline(@PathParam("partyNo") int partyNo,
	                                           @PathParam("fiscalPeriod") int pf,
	                                           @PathParam("sequenceNo") int seqNo,
	                                           @QueryParam("user") String user,
	                                           DeadlineRequest request);

	@GET
	@Path("/modifiedTaxPayers")
	@Produces({MediaType.APPLICATION_XML, WebServiceHelper.APPLICATION_JSON_WITH_UTF8_CHARSET})
	Response getModifiedTaxPayers(@QueryParam("user") String user, @QueryParam("since") Long since, @QueryParam("until") Long until);
}
