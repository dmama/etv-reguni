package ch.vd.unireg.webservices.v7;

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
import java.util.Set;

import ch.vd.unireg.ws.ack.v7.OrdinaryTaxDeclarationAckRequest;
import ch.vd.unireg.ws.deadline.v7.DeadlineRequest;
import ch.vd.unireg.xml.party.v5.PartyPart;
import ch.vd.unireg.xml.party.withholding.v1.DebtorCategory;
import ch.vd.unireg.webservices.common.WebServiceHelper;

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
	@Produces({MediaType.APPLICATION_XML, WebServiceHelper.APPLICATION_JSON_WITH_UTF8_CHARSET})
	@Path("/securityOnParties")
	Response getSecurityOnParties(@QueryParam("user") String user, @QueryParam("partyNo") List<Integer> partyNos);

	@GET
	@Produces({MediaType.APPLICATION_XML, WebServiceHelper.APPLICATION_JSON_WITH_UTF8_CHARSET})
	@Path("/party/{partyNo}")
	Response getParty(@PathParam("partyNo") int partyNo, @QueryParam("user") String user, @QueryParam("part") Set<PartyPart> parts);

	@GET
	@Produces(MediaType.APPLICATION_XML)
	@Path("/parties")
	Response getParties(@QueryParam("user") String user, @QueryParam("partyNo") List<Integer> partyNos, @QueryParam("part") Set<PartyPart> parts);

	@GET
	@Produces(MediaType.APPLICATION_XML)
	@Path("/communityOfHeirs/{deceasedId}")
	Response getCommunityOfHeirs(@PathParam("deceasedId") int deceasedId, @QueryParam("user") String user);

	@GET
	@Produces({MediaType.APPLICATION_XML, WebServiceHelper.APPLICATION_JSON_WITH_UTF8_CHARSET})
	@Path("/searchParty")
	Response searchParty(@QueryParam("user") String user,
	                     @QueryParam("partyNo") String partyNo,
	                     @QueryParam("name") String name,
	                     @QueryParam("nameSearchMode") @DefaultValue("IS_EXACTLY") SearchMode nameSearchMode,
	                     @QueryParam("townOrCountry") String townOrCountry,
	                     @QueryParam("dateOfBirth") String dateOfBirth,
	                     @QueryParam("vn") String socialInsuranceNumber,
	                     @QueryParam("uid") String uidNumber,
	                     @QueryParam("taxResidenceFSOId") Integer taxResidenceFSOId,
	                     @QueryParam("onlyActiveMainTaxResidence") @DefaultValue("false") boolean onlyActiveMainTaxResidence,
	                     @QueryParam("partyType") Set<PartySearchType> partyTypes,
	                     @QueryParam("debtorCategory") DebtorCategory debtorCategory,
	                     @QueryParam("activeParty") Boolean activeParty,
	                     @QueryParam("oldWithholdingNumber") Long oldWithholdingNumber);

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
	@Path("/newOrdinaryTaxDeclarationDeadline/{partyNo}/{taxPeriod}/{sequenceNo}")
	@Consumes(MediaType.APPLICATION_XML)
	@Produces(MediaType.APPLICATION_XML)
	Response newOrdinaryTaxDeclarationDeadline(@PathParam("partyNo") int partyNo,
	                                           @PathParam("taxPeriod") int pf,
	                                           @PathParam("sequenceNo") int seqNo,
	                                           @QueryParam("user") String user,
	                                           DeadlineRequest request);

	@GET
	@Path("/modifiedTaxPayers")
	@Produces({MediaType.APPLICATION_XML, WebServiceHelper.APPLICATION_JSON_WITH_UTF8_CHARSET})
	Response getModifiedTaxPayers(@QueryParam("user") String user, @QueryParam("since") Long since, @QueryParam("until") Long until);

	@GET
	@Path("/debtor/{debtorNo}/{taxPeriod}")
	@Produces({MediaType.APPLICATION_XML, WebServiceHelper.APPLICATION_JSON_WITH_UTF8_CHARSET})
	Response getDebtorInfo(@PathParam("debtorNo") int debtorNo, @PathParam("taxPeriod") int pf, @QueryParam("user") String user);

	@GET
	@Path("/avatar/{partyNo}")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	Response getAvatar(@PathParam("partyNo") int partyNo);

	@GET
	@Produces(MediaType.APPLICATION_XML)
	@Path("/fiscalEvents/{partyNo}")
	Response getFiscalEvents(@PathParam("partyNo") int partyNo, @QueryParam("user") String user);

	@GET
	@Produces(MediaType.APPLICATION_XML)
	@Path("/landRegistry/immovableProperty/{immoId}")
	Response getImmovableProperty(@PathParam("immoId") long immoId, @QueryParam("user") String user);

	@GET
	@Produces(MediaType.APPLICATION_XML)
	@Path("/landRegistry/immovableProperties")
	Response getImmovableProperties(@QueryParam("immoId") List<Long> immoIds, @QueryParam("user") String user);

	@GET
	@Produces(MediaType.APPLICATION_XML)
	@Path("/landRegistry/building/{buildingId}")
	Response getBuilding(@PathParam("buildingId") long buildingNo, @QueryParam("user") String user);

	@GET
	@Produces(MediaType.APPLICATION_XML)
	@Path("/landRegistry/buildings")
	Response getBuildings(@QueryParam("buildingId") List<Long> buildingId, @QueryParam("user") String user);

	@GET
	@Produces(MediaType.APPLICATION_XML)
	@Path("/landRegistry/communityOfOwners/{communityId}")
	Response getCommunityOfOwners(@PathParam("communityId") long communityId, @QueryParam("user") String user);

	@GET
	@Produces(MediaType.APPLICATION_XML)
	@Path("/landRegistry/communitiesOfOwners")
	Response getCommunitiesOfOwners(@QueryParam("communityId") List<Long> communityId, @QueryParam("user") String user);
}
