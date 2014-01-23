package ch.vd.uniregctb.webservices.v5;

import java.util.Date;
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
import ch.vd.unireg.ws.parties.v1.Parties;
import ch.vd.unireg.ws.security.v1.SecurityResponse;
import ch.vd.unireg.ws.taxoffices.v1.TaxOffices;
import ch.vd.unireg.xml.party.corporation.v3.CorporationEvent;
import ch.vd.unireg.xml.party.v3.Party;
import ch.vd.unireg.xml.party.v3.PartyInfo;
import ch.vd.unireg.xml.party.v3.PartyPart;
import ch.vd.unireg.xml.party.v3.PartyType;
import ch.vd.unireg.xml.party.withholding.v1.DebtorCategory;
import ch.vd.unireg.xml.party.withholding.v1.DebtorInfo;
import ch.vd.uniregctb.indexer.EmptySearchCriteriaException;
import ch.vd.uniregctb.indexer.IndexerException;
import ch.vd.uniregctb.webservices.common.AccessDeniedException;
import ch.vd.uniregctb.webservices.common.UserLogin;
import ch.vd.uniregctb.xml.ServiceException;

public abstract class BusinessWebServiceWrapper implements BusinessWebService {

	private BusinessWebService target;

	public final void setTarget(BusinessWebService target) {
		this.target = target;
	}

	@NotNull
	protected final BusinessWebService getTarget() {
		if (target == null) {
			throw new IllegalStateException("Target was not set prior to use...");
		}
		return target;
	}

	@Override
	public SecurityResponse getSecurityOnParty(String user, int partyNo) {
		return getTarget().getSecurityOnParty(user, partyNo);
	}

	@Override
	public void setAutomaticRepaymentBlockingFlag(int partyNo, UserLogin user, boolean blocked) throws AccessDeniedException {
		getTarget().setAutomaticRepaymentBlockingFlag(partyNo, user, blocked);
	}

	@Override
	public boolean getAutomaticRepaymentBlockingFlag(int partyNo, UserLogin user) throws AccessDeniedException {
		return getTarget().getAutomaticRepaymentBlockingFlag(partyNo, user);
	}

	@Override
	public OrdinaryTaxDeclarationAckResponse ackOrdinaryTaxDeclarations(UserLogin user, OrdinaryTaxDeclarationAckRequest request) throws AccessDeniedException {
		return getTarget().ackOrdinaryTaxDeclarations(user, request);
	}

	@Override
	public DeadlineResponse newOrdinaryTaxDeclarationDeadline(int partyNo, int pf, int seqNo, UserLogin user, DeadlineRequest request) throws AccessDeniedException {
		return getTarget().newOrdinaryTaxDeclarationDeadline(partyNo, pf, seqNo, user, request);
	}

	@Override
	public TaxOffices getTaxOffices(int municipalityId, @Nullable RegDate date) {
		return getTarget().getTaxOffices(municipalityId, date);
	}

	@Override
	public PartyNumberList getModifiedTaxPayers(UserLogin user, Date since, Date until) throws AccessDeniedException {
		return getTarget().getModifiedTaxPayers(user, since, until);
	}

	@Override
	public DebtorInfo getDebtorInfo(UserLogin user, int debtorNo, int pf) throws AccessDeniedException {
		return getTarget().getDebtorInfo(user, debtorNo, pf);
	}

	@Override
	public List<PartyInfo> searchParty(UserLogin user, @Nullable String partyNo, @Nullable String name, SearchMode nameSearchMode,
	                                   @Nullable String townOrCountry, @Nullable RegDate dateOfBirth, @Nullable String socialInsuranceNumber,
	                                   @Nullable Integer taxResidenceFSOId, boolean onlyActiveMainTaxResidence, @Nullable Set<PartyType> partyTypes,
	                                   @Nullable DebtorCategory debtorCategory, @Nullable Boolean activeParty, @Nullable Long oldWithholdingNumber) throws AccessDeniedException, IndexerException {
		return getTarget().searchParty(user, partyNo, name, nameSearchMode, townOrCountry, dateOfBirth, socialInsuranceNumber, taxResidenceFSOId, onlyActiveMainTaxResidence, partyTypes,
		                               debtorCategory,
		                               activeParty, oldWithholdingNumber);
	}

	@Override
	public List<CorporationEvent> searchCorporationEvent(UserLogin user, @Nullable Integer corporationId, @Nullable String eventCode,
	                                                     @Nullable RegDate startDate, @Nullable RegDate endDate) throws AccessDeniedException, EmptySearchCriteriaException {
		return getTarget().searchCorporationEvent(user, corporationId, eventCode, startDate, endDate);
	}

	@Override
	public Party getParty(UserLogin user, int partyNo, @Nullable Set<PartyPart> parts) throws AccessDeniedException, ServiceException {
		return getTarget().getParty(user, partyNo, parts);
	}

	@Override
	public Parties getParties(UserLogin user, List<Integer> partyNos, @Nullable Set<PartyPart> parts) throws AccessDeniedException, ServiceException {
		return getTarget().getParties(user, partyNos, parts);
	}
}
