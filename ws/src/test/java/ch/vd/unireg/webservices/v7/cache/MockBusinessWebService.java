package ch.vd.unireg.webservices.v7.cache;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.avatar.ImageData;
import ch.vd.unireg.indexer.IndexerException;
import ch.vd.unireg.webservices.common.AccessDeniedException;
import ch.vd.unireg.webservices.v7.BusinessWebService;
import ch.vd.unireg.webservices.v7.PartySearchType;
import ch.vd.unireg.webservices.v7.SearchMode;
import ch.vd.unireg.ws.ack.v7.OrdinaryTaxDeclarationAckRequest;
import ch.vd.unireg.ws.ack.v7.OrdinaryTaxDeclarationAckResponse;
import ch.vd.unireg.ws.deadline.v7.DeadlineRequest;
import ch.vd.unireg.ws.deadline.v7.DeadlineResponse;
import ch.vd.unireg.ws.fiscalevents.v7.FiscalEvents;
import ch.vd.unireg.ws.landregistry.v7.BuildingList;
import ch.vd.unireg.ws.landregistry.v7.CommunityOfOwnersList;
import ch.vd.unireg.ws.landregistry.v7.ImmovablePropertyList;
import ch.vd.unireg.ws.landregistry.v7.ImmovablePropertySearchResult;
import ch.vd.unireg.ws.modifiedtaxpayers.v7.PartyNumberList;
import ch.vd.unireg.ws.parties.v7.Parties;
import ch.vd.unireg.ws.security.v7.SecurityListResponse;
import ch.vd.unireg.ws.security.v7.SecurityResponse;
import ch.vd.unireg.xml.ServiceException;
import ch.vd.unireg.xml.infra.taxoffices.v1.TaxOffices;
import ch.vd.unireg.xml.party.communityofheirs.v1.CommunityOfHeirs;
import ch.vd.unireg.xml.party.landregistry.v1.Building;
import ch.vd.unireg.xml.party.landregistry.v1.CommunityOfOwners;
import ch.vd.unireg.xml.party.landregistry.v1.ImmovableProperty;
import ch.vd.unireg.xml.party.v5.Party;
import ch.vd.unireg.xml.party.v5.PartyInfo;
import ch.vd.unireg.xml.party.v5.PartyPart;
import ch.vd.unireg.xml.party.withholding.v1.DebtorCategory;
import ch.vd.unireg.xml.party.withholding.v1.DebtorInfo;

public class MockBusinessWebService implements BusinessWebService {

	private final BusinessWebService target;

	public MockBusinessWebService(BusinessWebService target) {
		this.target = target;
	}

	@Override
	public SecurityResponse getSecurityOnParty(String user, int partyNo) {
		return target.getSecurityOnParty(user, partyNo);
	}

	@Override
	public SecurityListResponse getSecurityOnParties(@NotNull String user, @NotNull List<Integer> partyNos) {
		return target.getSecurityOnParties(user, partyNos);
	}

	@Override
	public void setAutomaticRepaymentBlockingFlag(int partyNo, boolean blocked) throws AccessDeniedException {
		target.setAutomaticRepaymentBlockingFlag(partyNo, blocked);
	}

	@Override
	public boolean getAutomaticRepaymentBlockingFlag(int partyNo) throws AccessDeniedException {
		return target.getAutomaticRepaymentBlockingFlag(partyNo);
	}

	@Override
	public OrdinaryTaxDeclarationAckResponse ackOrdinaryTaxDeclarations(OrdinaryTaxDeclarationAckRequest request) throws AccessDeniedException {
		return target.ackOrdinaryTaxDeclarations(request);
	}

	@Override
	public DeadlineResponse newOrdinaryTaxDeclarationDeadline(int partyNo, int pf, int seqNo, DeadlineRequest request) throws AccessDeniedException {
		return target.newOrdinaryTaxDeclarationDeadline(partyNo, pf, seqNo, request);
	}

	@Override
	public TaxOffices getTaxOffices(int municipalityId, @Nullable RegDate date) {
		return target.getTaxOffices(municipalityId, date);
	}

	@Override
	public PartyNumberList getModifiedTaxPayers(Date since, Date until) throws AccessDeniedException {
		return target.getModifiedTaxPayers(since, until);
	}

	@Override
	public DebtorInfo getDebtorInfo(int debtorNo, int pf) throws AccessDeniedException {
		return target.getDebtorInfo(debtorNo, pf);
	}

	@Override
	public List<PartyInfo> searchParty(@Nullable String partyNo, @Nullable String name,
	                                   SearchMode nameSearchMode, @Nullable String townOrCountry,
	                                   @Nullable RegDate dateOfBirth, @Nullable String socialInsuranceNumber,
	                                   @Nullable String uidNumber, @Nullable Integer taxResidenceFSOId, boolean onlyActiveMainTaxResidence,
	                                   @Nullable Set<PartySearchType> partyTypes,
	                                   @Nullable DebtorCategory debtorCategory, @Nullable Boolean activeParty,
	                                   @Nullable Long oldWithholdingNumber) throws AccessDeniedException, IndexerException {
		return target.searchParty(partyNo, name, nameSearchMode, townOrCountry, dateOfBirth, socialInsuranceNumber, uidNumber, taxResidenceFSOId, onlyActiveMainTaxResidence, partyTypes, debtorCategory, activeParty, oldWithholdingNumber);
	}

	@Nullable
	@Override
	public Party getParty(int partyNo, @Nullable Set<PartyPart> parts) throws AccessDeniedException, ServiceException {
		return target.getParty(partyNo, parts);
	}

	@NotNull
	@Override
	public Parties getParties(List<Integer> partyNos, @Nullable Set<PartyPart> parts) throws AccessDeniedException, ServiceException {
		return target.getParties(partyNos, parts);
	}

	@Override
	public CommunityOfHeirs getCommunityOfHeirs(int deceasedId) throws AccessDeniedException, ServiceException {
		return target.getCommunityOfHeirs(deceasedId);
	}

	@Override
	public ImageData getAvatar(int partyNo) throws ServiceException {
		return target.getAvatar(partyNo);
	}

	@Override
	public FiscalEvents getFiscalEvents(int partyNo) throws AccessDeniedException {
		return target.getFiscalEvents(partyNo);
	}

	@Nullable
	@Override
	public ImmovableProperty getImmovableProperty(long immoId) throws AccessDeniedException {
		return target.getImmovableProperty(immoId);
	}

	@Nullable
	@Override
	public ImmovableProperty getImmovablePropertyByLocation(int municipalityFsoId, int parcelNumber, @Nullable Integer index1, @Nullable Integer index2, @Nullable Integer index3) throws AccessDeniedException {
		return target.getImmovablePropertyByLocation(municipalityFsoId, parcelNumber, index1, index2, index3);
	}

	@NotNull
	@Override
	public ImmovablePropertySearchResult findImmovablePropertyByLocation(int municipalityFsoId, int parcelNumber, @Nullable Integer index1, @Nullable Integer index2, @Nullable Integer index3) throws AccessDeniedException {
		return target.findImmovablePropertyByLocation(municipalityFsoId, parcelNumber, index1, index2, index3);
	}

	@NotNull
	@Override
	public ImmovablePropertyList getImmovableProperties(List<Long> immoIds) throws AccessDeniedException {
		return target.getImmovableProperties(immoIds);
	}

	@Nullable
	@Override
	public Building getBuilding(long buildingId) throws AccessDeniedException {
		return target.getBuilding(buildingId);
	}

	@NotNull
	@Override
	public BuildingList getBuildings(List<Long> buildingIds) throws AccessDeniedException {
		return target.getBuildings(buildingIds);
	}

	@Nullable
	@Override
	public CommunityOfOwners getCommunityOfOwners(long communityId) throws AccessDeniedException {
		return target.getCommunityOfOwners(communityId);
	}

	@NotNull
	@Override
	public CommunityOfOwnersList getCommunitiesOfOwners(List<Long> communityIds) throws AccessDeniedException {
		return target.getCommunitiesOfOwners(communityIds);
	}

	@Override
	public @NotNull String getSwaggerJson() throws IOException {
		return target.getSwaggerJson();
	}
}
