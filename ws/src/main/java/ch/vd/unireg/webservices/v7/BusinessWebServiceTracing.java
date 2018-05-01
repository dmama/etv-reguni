package ch.vd.unireg.webservices.v7;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.avatar.ImageData;
import ch.vd.unireg.indexer.IndexerException;
import ch.vd.unireg.stats.ServiceTracing;
import ch.vd.unireg.stats.StatsService;
import ch.vd.unireg.webservices.common.AccessDeniedException;
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
import ch.vd.unireg.ws.parties.v7.Entry;
import ch.vd.unireg.ws.parties.v7.Parties;
import ch.vd.unireg.ws.security.v7.SecurityListResponse;
import ch.vd.unireg.ws.security.v7.SecurityResponse;
import ch.vd.unireg.xml.ServiceException;
import ch.vd.unireg.xml.infra.taxoffices.v1.TaxOffices;
import ch.vd.unireg.xml.party.communityofheirs.v1.CommunityOfHeirs;
import ch.vd.unireg.xml.party.landregistry.v1.Building;
import ch.vd.unireg.xml.party.landregistry.v1.CommunityOfOwners;
import ch.vd.unireg.xml.party.landregistry.v1.ImmovableProperty;
import ch.vd.unireg.xml.party.v5.InternalPartyPart;
import ch.vd.unireg.xml.party.v5.Party;
import ch.vd.unireg.xml.party.v5.PartyInfo;
import ch.vd.unireg.xml.party.withholding.v1.DebtorCategory;
import ch.vd.unireg.xml.party.withholding.v1.DebtorInfo;

public class BusinessWebServiceTracing implements BusinessWebService, InitializingBean, DisposableBean {

	public static final String SERVICE_NAME = "WebService7";

	private BusinessWebService target;
	private StatsService statsService;

	private final ServiceTracing tracing = new ServiceTracing(SERVICE_NAME, false);

	public void setTarget(BusinessWebService target) {
		this.target = target;
	}

	public void setStatsService(StatsService statsService) {
		this.statsService = statsService;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (statsService != null) {
			statsService.registerService(SERVICE_NAME, tracing);
		}
	}

	@Override
	public void destroy() throws Exception {
		if (statsService != null) {
			statsService.unregisterService(SERVICE_NAME);
		}
	}

	@Override
	public SecurityResponse getSecurityOnParty(String user, int partyNo) {
		Throwable t = null;
		final long time = tracing.start();
		try {
			return target.getSecurityOnParty(user, partyNo);
		}
		catch (RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getSecurityOnParty", null);
		}
	}

	@Override
	public SecurityListResponse getSecurityOnParties(@NotNull String user, @NotNull List<Integer> partyNos) {
		Throwable t = null;
		final long time = tracing.start();
		int resultSize = 0;
		try {
			final SecurityListResponse list = target.getSecurityOnParties(user, partyNos);
			if (list != null) {
				resultSize = list.getPartyAccesses().size();
			}
			return list;
		}
		catch (RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getSecurityOnParties", resultSize, null);
		}
	}

	@Override
	public void setAutomaticRepaymentBlockingFlag(int partyNo, boolean blocked) throws AccessDeniedException {
		Throwable t = null;
		final long time = tracing.start();
		try {
			target.setAutomaticRepaymentBlockingFlag(partyNo, blocked);
		}
		catch (AccessDeniedException | RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "setAutomaticRepaymentBlockingFlag", null);
		}
	}

	@Override
	public boolean getAutomaticRepaymentBlockingFlag(int partyNo) throws AccessDeniedException {
		Throwable t = null;
		final long time = tracing.start();
		try {
			return target.getAutomaticRepaymentBlockingFlag(partyNo);
		}
		catch (AccessDeniedException | RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getAutomaticRepaymentBlockingFlag", null);
		}
	}

	@Override
	public OrdinaryTaxDeclarationAckResponse ackOrdinaryTaxDeclarations(OrdinaryTaxDeclarationAckRequest request) throws AccessDeniedException {
		Throwable t = null;
		final long time = tracing.start();
		try {
			return target.ackOrdinaryTaxDeclarations(request);
		}
		catch (AccessDeniedException | RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "ackOrdinaryTaxDeclarations", null);
		}
	}

	@Override
	public DeadlineResponse newOrdinaryTaxDeclarationDeadline(int partyNo, int pf, int seqNo, DeadlineRequest request) throws AccessDeniedException {
		Throwable t = null;
		final long time = tracing.start();
		try {
			return target.newOrdinaryTaxDeclarationDeadline(partyNo, pf, seqNo, request);
		}
		catch (AccessDeniedException | RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "newOrdinaryTaxDeclarationDeadline", null);
		}
	}

	@Override
	public TaxOffices getTaxOffices(int municipalityId, @Nullable RegDate date) {
		Throwable t = null;
		final long time = tracing.start();
		try {
			return target.getTaxOffices(municipalityId, date);
		}
		catch (RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getTaxOffices", null);
		}
	}

	@Override
	public PartyNumberList getModifiedTaxPayers(Date since, Date until) throws AccessDeniedException {
		Throwable t = null;
		final long time = tracing.start();
		try {
			return target.getModifiedTaxPayers(since, until);
		}
		catch (AccessDeniedException | RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getModifiedTaxPayers", null);
		}
	}

	@Override
	public DebtorInfo getDebtorInfo(int debtorNo, int pf) throws AccessDeniedException {
		Throwable t = null;
		final long time = tracing.start();
		try {
			return target.getDebtorInfo(debtorNo, pf);
		}
		catch (AccessDeniedException | RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getDebtorInfo", null);
		}
	}

	@Override
	public List<PartyInfo> searchParty(@Nullable String partyNo, @Nullable String name, SearchMode nameSearchMode,
	                                   @Nullable String townOrCountry, @Nullable RegDate dateOfBirth, @Nullable String socialInsuranceNumber, @Nullable String uidNumber,
	                                   @Nullable Integer taxResidenceFSOId, boolean onlyActiveMainTaxResidence, @Nullable Set<PartySearchType> partyTypes,
	                                   @Nullable DebtorCategory debtorCategory, @Nullable Boolean activeParty, @Nullable Long oldWithholdingNumber) throws AccessDeniedException, IndexerException {
		Throwable t = null;
		final long time = tracing.start();
		try {
			return target.searchParty(partyNo, name, nameSearchMode, townOrCountry, dateOfBirth, socialInsuranceNumber, uidNumber, taxResidenceFSOId, onlyActiveMainTaxResidence,
			                          partyTypes, debtorCategory, activeParty, oldWithholdingNumber);
		}
		catch (AccessDeniedException | RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "searchParty", null);
		}
	}

	@Nullable
	@Override
	public Party getParty(int partyNo, @Nullable Set<InternalPartyPart> parts) throws AccessDeniedException, ServiceException {
		Throwable t = null;
		final long time = tracing.start();
		try {
			return target.getParty(partyNo, parts);
		}
		catch (AccessDeniedException | ServiceException | RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getParty", null);
		}
	}

	@NotNull
	@Override
	public Parties getParties(List<Integer> partyNos, @Nullable Set<InternalPartyPart> parts) throws AccessDeniedException, ServiceException {
		Throwable t = null;
		final long time = tracing.start();
		int resultSize = 0;
		try {
			final Parties parties = target.getParties(partyNos, parts);
			if (parties.getEntries() != null) {
				for (Entry entry : parties.getEntries()) {
					if (entry.getParty() != null) {
						++ resultSize;
					}
				}
			}
			return parties;
		}
		catch (AccessDeniedException | ServiceException | RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getParties", resultSize, null);
		}
	}

	@Override
	public CommunityOfHeirs getCommunityOfHeirs(int deceasedId) throws AccessDeniedException, ServiceException {
		Throwable t = null;
		final long time = tracing.start();
		int resultSize = 0;
		try {
			final CommunityOfHeirs community = target.getCommunityOfHeirs(deceasedId);
			resultSize = (community == null ? 0 : 1);
			return community;
		}
		catch (AccessDeniedException | ServiceException | RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getCommunityOfHeirs", resultSize, null);
		}
	}

	@Override
	public ImageData getAvatar(int partyNo) throws ServiceException {
		Throwable t = null;
		final long time = tracing.start();
		try {
			return target.getAvatar(partyNo);
		}
		catch (ServiceException | RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getAvatar", null);
		}
	}

	@Override
	public FiscalEvents getFiscalEvents(int partyNo) throws AccessDeniedException {
		Throwable t = null;
		final long time = tracing.start();
		try {
			return target.getFiscalEvents(partyNo);
		}
		catch (RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getFiscalEvents", null);
		}
	}

	@Nullable
	@Override
	public ImmovableProperty getImmovableProperty(long immoId) throws AccessDeniedException {
		Throwable t = null;
		final long time = tracing.start();
		try {
			return target.getImmovableProperty(immoId);
		}
		catch (RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getImmovableProperty", null);
		}
	}

	@Nullable
	@Override
	public ImmovableProperty getImmovablePropertyByLocation(int municipalityFsoId, int parcelNumber, @Nullable Integer index1, @Nullable Integer index2, @Nullable Integer index3) throws AccessDeniedException {
		Throwable t = null;
		final long time = tracing.start();
		try {
			return target.getImmovablePropertyByLocation(municipalityFsoId, parcelNumber, index1, index2, index3);
		}
		catch (RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getImmovablePropertyByLocation", null);
		}
	}

	@NotNull
	@Override
	public ImmovablePropertySearchResult findImmovablePropertyByLocation(int municipalityFsoId, int parcelNumber, @Nullable Integer index1, @Nullable Integer index2, @Nullable Integer index3) throws AccessDeniedException {
		Throwable t = null;
		final long time = tracing.start();
		try {
			return target.findImmovablePropertyByLocation(municipalityFsoId, parcelNumber, index1, index2, index3);
		}
		catch (RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "findImmovablePropertyByLocation", null);
		}
	}

	@NotNull
	@Override
	public ImmovablePropertyList getImmovableProperties(List<Long> immoIds) throws AccessDeniedException {
		Throwable t = null;
		final long time = tracing.start();
		try {
			return target.getImmovableProperties(immoIds);
		}
		catch (RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getImmovableProperties", null);
		}
	}

	@Nullable
	@Override
	public Building getBuilding(long buildingId) throws AccessDeniedException {
		Throwable t = null;
		final long time = tracing.start();
		try {
			return target.getBuilding(buildingId);
		}
		catch (RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getBuilding", null);
		}
	}

	@NotNull
	@Override
	public BuildingList getBuildings(List<Long> buildingIds) throws AccessDeniedException {
		Throwable t = null;
		final long time = tracing.start();
		try {
			return target.getBuildings(buildingIds);
		}
		catch (RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getBuildings", null);
		}
	}

	@Nullable
	@Override
	public CommunityOfOwners getCommunityOfOwners(long communityId) throws AccessDeniedException {
		Throwable t = null;
		final long time = tracing.start();
		try {
			return target.getCommunityOfOwners(communityId);
		}
		catch (RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getCommunityOfOwners", null);
		}
	}

	@NotNull
	@Override
	public CommunityOfOwnersList getCommunitiesOfOwners(List<Long> communityIds) throws AccessDeniedException {
		Throwable t = null;
		final long time = tracing.start();
		try {
			return target.getCommunitiesOfOwners(communityIds);
		}
		catch (RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getCommunitiesOfOwners", null);
		}
	}

	@Override
	public @NotNull String getSwaggerJson() throws IOException {
		Throwable t = null;
		final long time = tracing.start();
		try {
			return target.getSwaggerJson();
		}
		catch (RuntimeException | Error | IOException e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getSwaggerJson", null);
		}
	}
}
