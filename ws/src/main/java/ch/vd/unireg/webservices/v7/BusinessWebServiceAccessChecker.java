package ch.vd.unireg.webservices.v7;

import java.util.Date;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.avatar.ImageData;
import ch.vd.unireg.indexer.IndexerException;
import ch.vd.unireg.security.Role;
import ch.vd.unireg.security.SecurityProviderInterface;
import ch.vd.unireg.webservices.common.AccessDeniedException;
import ch.vd.unireg.webservices.common.UserLogin;
import ch.vd.unireg.webservices.common.WebServiceHelper;
import ch.vd.unireg.ws.ack.v7.OrdinaryTaxDeclarationAckRequest;
import ch.vd.unireg.ws.ack.v7.OrdinaryTaxDeclarationAckResponse;
import ch.vd.unireg.ws.deadline.v7.DeadlineRequest;
import ch.vd.unireg.ws.deadline.v7.DeadlineResponse;
import ch.vd.unireg.ws.fiscalevents.v7.FiscalEvents;
import ch.vd.unireg.ws.landregistry.v7.BuildingList;
import ch.vd.unireg.ws.landregistry.v7.CommunityOfOwnersList;
import ch.vd.unireg.ws.landregistry.v7.ImmovablePropertyList;
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

/**
 * Implémentation du service métier v7 qui délègue à une implémentation réelle après avoir vérifié que
 * les rôles IFO-Sec étaient correctement assignés
 */
public class BusinessWebServiceAccessChecker implements BusinessWebService {

	private SecurityProviderInterface securityProvider;
	private BusinessWebService target;

	public void setTarget(BusinessWebService target) {
		this.target = target;
	}

	public void setSecurityProvider(SecurityProviderInterface securityProvider) {
		this.securityProvider = securityProvider;
	}

	@Override
	public void setAutomaticRepaymentBlockingFlag(int partyNo, UserLogin user, boolean blocked) throws AccessDeniedException {
		WebServiceHelper.checkAccess(securityProvider, user, Role.VISU_ALL);
		WebServiceHelper.checkPartyReadWriteAccess(securityProvider, user, partyNo);
		target.setAutomaticRepaymentBlockingFlag(partyNo, user, blocked);
	}

	@Override
	public boolean getAutomaticRepaymentBlockingFlag(int partyNo, UserLogin user) throws AccessDeniedException {
		WebServiceHelper.checkAccess(securityProvider, user, Role.VISU_ALL);
		WebServiceHelper.checkPartyReadAccess(securityProvider, user, partyNo);
		return target.getAutomaticRepaymentBlockingFlag(partyNo, user);
	}

	@Override
	public OrdinaryTaxDeclarationAckResponse ackOrdinaryTaxDeclarations(UserLogin user, OrdinaryTaxDeclarationAckRequest request) throws AccessDeniedException {
		// le droit PP/PM dépend du type de contribuable, le check est donc fait plus bas (en plus, ici, il peut y en avoir plusieurs...)
		return target.ackOrdinaryTaxDeclarations(user, request);
	}

	@Override
	public DeadlineResponse newOrdinaryTaxDeclarationDeadline(int partyNo, int pf, int seqNo, UserLogin user, DeadlineRequest request) throws AccessDeniedException {
		// le droit PP/PM dépend du type de contribuable, le check est donc fait plus bas...
		return target.newOrdinaryTaxDeclarationDeadline(partyNo, pf, seqNo, user, request);
	}

	@Override
	public PartyNumberList getModifiedTaxPayers(UserLogin user, Date since, Date until) throws AccessDeniedException {
		WebServiceHelper.checkAccess(securityProvider, user, Role.VISU_ALL);
		return target.getModifiedTaxPayers(user, since, until);
	}

	@Override
	public DebtorInfo getDebtorInfo(UserLogin user, int debtorNo, int pf) throws AccessDeniedException {
		WebServiceHelper.checkAccess(securityProvider, user, Role.VISU_ALL);
		return target.getDebtorInfo(user, debtorNo, pf);
	}

	@Override
	public List<PartyInfo> searchParty(UserLogin user, @Nullable String partyNo, @Nullable String name, SearchMode nameSearchMode, @Nullable String townOrCountry, @Nullable RegDate dateOfBirth,
	                                   @Nullable String socialInsuranceNumber, @Nullable String uidNumber, @Nullable Integer taxResidenceFSOId,
	                                   boolean onlyActiveMainTaxResidence, @Nullable Set<PartySearchType> partyTypes, @Nullable DebtorCategory debtorCategory, @Nullable Boolean activeParty,
	                                   @Nullable Long oldWithholdingNumber) throws AccessDeniedException, IndexerException {
		WebServiceHelper.checkAnyAccess(securityProvider, user, Role.VISU_ALL, Role.VISU_LIMITE);
		return target.searchParty(user, partyNo, name, nameSearchMode, townOrCountry, dateOfBirth, socialInsuranceNumber, uidNumber, taxResidenceFSOId, onlyActiveMainTaxResidence, partyTypes,
		                          debtorCategory, activeParty, oldWithholdingNumber);
	}

	@Override
	public Party getParty(UserLogin user, int partyNo, @Nullable Set<PartyPart> parts) throws AccessDeniedException, ServiceException {
		WebServiceHelper.checkAccess(securityProvider, user, Role.VISU_ALL);
		WebServiceHelper.checkPartyReadAccess(securityProvider, user, partyNo);
		return target.getParty(user, partyNo, parts);
	}

	@Override
	public Parties getParties(UserLogin user, List<Integer> partyNos, @Nullable Set<PartyPart> parts) throws AccessDeniedException, ServiceException {
		WebServiceHelper.checkAccess(securityProvider, user, Role.VISU_ALL);
		// note: le contrôle d'accès à chaque tiers est fait dans l'implémentation
		return target.getParties(user, partyNos, parts);
	}

	@Override
	public CommunityOfHeirs getCommunityOfHeirs(UserLogin user, int deceasedId) throws AccessDeniedException, ServiceException {
		WebServiceHelper.checkAccess(securityProvider, user, Role.VISU_ALL);
		WebServiceHelper.checkPartyReadAccess(securityProvider, user, deceasedId);
		return target.getCommunityOfHeirs(user, deceasedId);
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
	public TaxOffices getTaxOffices(int municipalityId, @Nullable RegDate date) {
		return target.getTaxOffices(municipalityId, date);
	}

	@Override
	public ImageData getAvatar(int partyNo) throws ServiceException {
		return target.getAvatar(partyNo);
	}

	@Override
	public FiscalEvents getFiscalEvents(UserLogin user, int partyNo) throws AccessDeniedException {
		WebServiceHelper.checkAccess(securityProvider, user, Role.VISU_ALL);
		WebServiceHelper.checkPartyReadAccess(securityProvider, user, partyNo);
		return target.getFiscalEvents(user, partyNo);
	}

	@Nullable
	@Override
	public ImmovableProperty getImmovableProperty(@NotNull UserLogin user, long immoId) throws AccessDeniedException {
		WebServiceHelper.checkAnyAccess(securityProvider, user, Role.VISU_IMMEUBLES, Role.VISU_ALL);
		return target.getImmovableProperty(user, immoId);
	}

	@Override
	public @Nullable ImmovableProperty getImmovablePropertyByLocation(UserLogin user, int municipalityFsoId, int parcelNumber, @Nullable Integer index1, @Nullable Integer index2, @Nullable Integer index3) throws AccessDeniedException {
		WebServiceHelper.checkAnyAccess(securityProvider, user, Role.VISU_IMMEUBLES, Role.VISU_ALL);
		return target.getImmovablePropertyByLocation(user, municipalityFsoId, parcelNumber, index1, index2, index3);
	}

	@NotNull
	@Override
	public ImmovablePropertyList getImmovableProperties(UserLogin user, List<Long> immoIds) throws AccessDeniedException {
		WebServiceHelper.checkAnyAccess(securityProvider, user, Role.VISU_IMMEUBLES, Role.VISU_ALL);
		return target.getImmovableProperties(user, immoIds);
	}

	@Nullable
	@Override
	public Building getBuilding(@NotNull UserLogin user, long buildingId) throws AccessDeniedException {
		WebServiceHelper.checkAnyAccess(securityProvider, user, Role.VISU_IMMEUBLES, Role.VISU_ALL);
		return target.getBuilding(user, buildingId);
	}

	@NotNull
	@Override
	public BuildingList getBuildings(@NotNull UserLogin user, List<Long> buildingIds) throws AccessDeniedException {
		WebServiceHelper.checkAnyAccess(securityProvider, user, Role.VISU_IMMEUBLES, Role.VISU_ALL);
		return target.getBuildings(user, buildingIds);
	}

	@Nullable
	@Override
	public CommunityOfOwners getCommunityOfOwners(@NotNull UserLogin user, long communityId) throws AccessDeniedException {
		WebServiceHelper.checkAnyAccess(securityProvider, user, Role.VISU_IMMEUBLES, Role.VISU_ALL);
		return target.getCommunityOfOwners(user, communityId);
	}

	@Override
	public @NotNull CommunityOfOwnersList getCommunitiesOfOwners(@NotNull UserLogin user, List<Long> communityIds) throws AccessDeniedException {
		WebServiceHelper.checkAnyAccess(securityProvider, user, Role.VISU_IMMEUBLES, Role.VISU_ALL);
		return target.getCommunitiesOfOwners(user, communityIds);
	}
}
