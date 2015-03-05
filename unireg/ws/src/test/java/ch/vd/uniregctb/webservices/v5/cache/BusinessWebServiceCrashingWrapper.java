package ch.vd.uniregctb.webservices.v5.cache;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.ws.ack.v1.OrdinaryTaxDeclarationAckRequest;
import ch.vd.unireg.ws.ack.v1.OrdinaryTaxDeclarationAckResponse;
import ch.vd.unireg.ws.deadline.v1.DeadlineRequest;
import ch.vd.unireg.ws.deadline.v1.DeadlineResponse;
import ch.vd.unireg.ws.modifiedtaxpayers.v1.PartyNumberList;
import ch.vd.unireg.ws.parties.v1.Entry;
import ch.vd.unireg.ws.parties.v1.Parties;
import ch.vd.unireg.ws.security.v1.SecurityResponse;
import ch.vd.unireg.ws.taxoffices.v1.TaxOffices;
import ch.vd.unireg.xml.error.v1.ErrorType;
import ch.vd.unireg.xml.party.corporation.v3.CorporationEvent;
import ch.vd.unireg.xml.party.v3.Party;
import ch.vd.unireg.xml.party.v3.PartyInfo;
import ch.vd.unireg.xml.party.v3.PartyPart;
import ch.vd.unireg.xml.party.withholding.v1.DebtorCategory;
import ch.vd.unireg.xml.party.withholding.v1.DebtorInfo;
import ch.vd.uniregctb.avatar.ImageData;
import ch.vd.uniregctb.indexer.EmptySearchCriteriaException;
import ch.vd.uniregctb.indexer.IndexerException;
import ch.vd.uniregctb.webservices.common.AccessDeniedException;
import ch.vd.uniregctb.webservices.common.UserLogin;
import ch.vd.uniregctb.webservices.v5.BusinessWebService;
import ch.vd.uniregctb.webservices.v5.PartySearchType;
import ch.vd.uniregctb.webservices.v5.SearchMode;
import ch.vd.uniregctb.xml.ServiceException;

class BusinessWebServiceCrashingWrapper implements BusinessWebService {

	public static final String EXCEPTION_TEXT = "Boom badaboom !!";

	private final Set<Integer> crashingNos;
	private final BusinessWebService target;

	BusinessWebServiceCrashingWrapper(@NotNull BusinessWebService target, Integer... crashingNos) {
		this.target = target;
		this.crashingNos = (crashingNos != null ? new HashSet<>(Arrays.asList(crashingNos)) : Collections.<Integer>emptySet());
	}

	@Override
	public DebtorInfo getDebtorInfo(UserLogin user, int debtorNo, int pf) throws AccessDeniedException {
		check(debtorNo);
		return target.getDebtorInfo(user, debtorNo, pf);
	}

	@Override
	public Party getParty(UserLogin user, int partyNo, @Nullable Set<PartyPart> parts) throws AccessDeniedException, ServiceException {
		check(partyNo);
		return target.getParty(user, partyNo, parts);
	}

	@Override
	public SecurityResponse getSecurityOnParty(String user, int partyNo) {
		return target.getSecurityOnParty(user, partyNo);
	}

	@Override
	public void setAutomaticRepaymentBlockingFlag(int partyNo, UserLogin user, boolean blocked) throws AccessDeniedException {
		check(partyNo);
		target.setAutomaticRepaymentBlockingFlag(partyNo, user, blocked);
	}

	@Override
	public boolean getAutomaticRepaymentBlockingFlag(int partyNo, UserLogin user) throws AccessDeniedException {
		check(partyNo);
		return target.getAutomaticRepaymentBlockingFlag(partyNo, user);
	}

	@Override
	public OrdinaryTaxDeclarationAckResponse ackOrdinaryTaxDeclarations(UserLogin user, OrdinaryTaxDeclarationAckRequest request) throws AccessDeniedException {
		return target.ackOrdinaryTaxDeclarations(user, request);
	}

	@Override
	public DeadlineResponse newOrdinaryTaxDeclarationDeadline(int partyNo, int pf, int seqNo, UserLogin user, DeadlineRequest request) throws AccessDeniedException {
		check(partyNo);
		return target.newOrdinaryTaxDeclarationDeadline(partyNo, pf, seqNo, user, request);
	}

	@Override
	public TaxOffices getTaxOffices(int municipalityId, @Nullable RegDate date) {
		return target.getTaxOffices(municipalityId, date);
	}

	@Override
	public PartyNumberList getModifiedTaxPayers(UserLogin user, java.util.Date since, java.util.Date until) throws AccessDeniedException {
		return target.getModifiedTaxPayers(user, since, until);
	}

	@Override
	public List<PartyInfo> searchParty(UserLogin user, @Nullable String partyNo, @Nullable String name, SearchMode nameSearchMode,
	                                   @Nullable String townOrCountry, @Nullable RegDate dateOfBirth, @Nullable String socialInsuranceNumber, @Nullable String uidNumber,
	                                   @Nullable Integer taxResidenceFSOId, boolean onlyActiveMainTaxResidence, @Nullable Set<PartySearchType> partyTypes,
	                                   @Nullable DebtorCategory debtorCategory, @Nullable Boolean activeParty, @Nullable Long oldWithholdingNumber) throws AccessDeniedException, IndexerException {
		return target.searchParty(user, partyNo, name, nameSearchMode, townOrCountry, dateOfBirth, socialInsuranceNumber, uidNumber, taxResidenceFSOId, onlyActiveMainTaxResidence, partyTypes,
		                          debtorCategory, activeParty, oldWithholdingNumber);
	}

	@Override
	public List<CorporationEvent> searchCorporationEvent(UserLogin user, @Nullable Integer corporationId, @Nullable String eventCode,
	                                                     @Nullable RegDate startDate, @Nullable RegDate endDate) throws AccessDeniedException, EmptySearchCriteriaException {
		return target.searchCorporationEvent(user, corporationId, eventCode, startDate, endDate);
	}

	@Override
	public Parties getParties(UserLogin user, List<Integer> partyNos, @Nullable Set<PartyPart> parts) throws AccessDeniedException, ServiceException {
		final List<Integer> nonCrashing = new ArrayList<>(partyNos.size());
		final List<Integer> indeedCrashing = new ArrayList<>(partyNos.size());
		for (int partyNo : partyNos) {
			if (crashingNos.contains(partyNo)) {
				indeedCrashing.add(partyNo);
			}
			else {
				nonCrashing.add(partyNo);
			}
		}
		final Parties result = target.getParties(user, nonCrashing, parts);
		for (int partyNo : indeedCrashing) {
			result.getEntries().add(new Entry(partyNo, null, new ch.vd.unireg.xml.error.v1.Error(ErrorType.TECHNICAL, EXCEPTION_TEXT)));
		}
		return result;
	}

	@Override
	public ImageData getAvatar(int partyNo) throws ServiceException {
		check(partyNo);
		return target.getAvatar(partyNo);
	}

	private void check(int partyNo) {
		if (crashingNos.contains(partyNo)) {
			throw new RuntimeException(EXCEPTION_TEXT);
		}
	}
}
