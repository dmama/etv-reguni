package ch.vd.unireg.webservices.v6;

import java.util.Date;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.ws.ack.v6.OrdinaryTaxDeclarationAckRequest;
import ch.vd.unireg.ws.ack.v6.OrdinaryTaxDeclarationAckResponse;
import ch.vd.unireg.ws.deadline.v6.DeadlineRequest;
import ch.vd.unireg.ws.deadline.v6.DeadlineResponse;
import ch.vd.unireg.ws.modifiedtaxpayers.v6.PartyNumberList;
import ch.vd.unireg.ws.parties.v6.Parties;
import ch.vd.unireg.ws.security.v6.SecurityResponse;
import ch.vd.unireg.ws.taxoffices.v6.TaxOffices;
import ch.vd.unireg.xml.party.v4.Party;
import ch.vd.unireg.xml.party.v4.PartyInfo;
import ch.vd.unireg.xml.party.v4.PartyPart;
import ch.vd.unireg.xml.party.withholding.v1.DebtorCategory;
import ch.vd.unireg.xml.party.withholding.v1.DebtorInfo;
import ch.vd.unireg.avatar.ImageData;
import ch.vd.unireg.indexer.IndexerException;
import ch.vd.unireg.security.Role;
import ch.vd.unireg.security.SecurityProviderInterface;
import ch.vd.unireg.webservices.common.AccessDeniedException;
import ch.vd.unireg.webservices.common.UserLogin;
import ch.vd.unireg.webservices.common.WebServiceHelper;
import ch.vd.unireg.xml.ServiceException;

/**
 * Implémentation du service métier v6 qui délègue à une implémentation réelle après avoir vérifié que
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
