package ch.vd.uniregctb.webservices.v5;

import java.util.Date;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.avatars.ImageData;
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
import ch.vd.unireg.xml.party.withholding.v1.DebtorCategory;
import ch.vd.unireg.xml.party.withholding.v1.DebtorInfo;
import ch.vd.uniregctb.indexer.EmptySearchCriteriaException;
import ch.vd.uniregctb.indexer.IndexerException;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityProviderInterface;
import ch.vd.uniregctb.webservices.common.AccessDeniedException;
import ch.vd.uniregctb.webservices.common.UserLogin;
import ch.vd.uniregctb.webservices.common.WebServiceHelper;
import ch.vd.uniregctb.xml.ServiceException;

/**
 * Implémentation du service métier v5 qui délègue à une implémentation réelle après avoir vérifié que
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
		WebServiceHelper.checkAccess(securityProvider, user, Role.DI_QUIT_PP);
		return target.ackOrdinaryTaxDeclarations(user, request);
	}

	@Override
	public DeadlineResponse newOrdinaryTaxDeclarationDeadline(int partyNo, int pf, int seqNo, UserLogin user, DeadlineRequest request) throws AccessDeniedException {
		WebServiceHelper.checkAccess(securityProvider, user, Role.DI_DELAI_PP);
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
	public List<CorporationEvent> searchCorporationEvent(UserLogin user, @Nullable Integer corporationId, @Nullable String eventCode,
	                                                     @Nullable RegDate startDate, @Nullable RegDate endDate) throws AccessDeniedException, EmptySearchCriteriaException {
		WebServiceHelper.checkAccess(securityProvider, user, Role.VISU_ALL);
		return target.searchCorporationEvent(user, corporationId, eventCode, startDate, endDate);
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
		return target.getParties(user, partyNos, parts);
	}

	@Override
	public SecurityResponse getSecurityOnParty(String user, int partyNo) {
		return target.getSecurityOnParty(user, partyNo);
	}

	@Override
	public TaxOffices getTaxOffices(int municipalityId, @Nullable RegDate date) {
		return target.getTaxOffices(municipalityId, date);
	}

	@Override
	public ImageData getAvatar(int partyNo) throws ServiceException {
		return target.getAvatar(partyNo);
	}
}
