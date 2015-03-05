package ch.vd.uniregctb.webservices.v5;

import java.util.Date;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

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
import ch.vd.unireg.xml.party.corporation.v3.CorporationEvent;
import ch.vd.unireg.xml.party.v3.Party;
import ch.vd.unireg.xml.party.v3.PartyInfo;
import ch.vd.unireg.xml.party.v3.PartyPart;
import ch.vd.unireg.xml.party.withholding.v1.DebtorCategory;
import ch.vd.unireg.xml.party.withholding.v1.DebtorInfo;
import ch.vd.uniregctb.avatar.ImageData;
import ch.vd.uniregctb.indexer.EmptySearchCriteriaException;
import ch.vd.uniregctb.indexer.IndexerException;
import ch.vd.uniregctb.stats.ServiceTracing;
import ch.vd.uniregctb.stats.StatsService;
import ch.vd.uniregctb.webservices.common.AccessDeniedException;
import ch.vd.uniregctb.webservices.common.UserLogin;
import ch.vd.uniregctb.xml.ServiceException;

public class BusinessWebServiceTracing implements BusinessWebService, InitializingBean, DisposableBean {

	public static final String SERVICE_NAME = "WebService5";

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
		final long time = tracing.start();
		try {
			return target.getSecurityOnParty(user, partyNo);
		}
		finally {
			tracing.end(time, "getSecurityOnParty", null);
		}
	}

	@Override
	public void setAutomaticRepaymentBlockingFlag(int partyNo, UserLogin user, boolean blocked) throws AccessDeniedException {
		final long time = tracing.start();
		try {
			target.setAutomaticRepaymentBlockingFlag(partyNo, user, blocked);
		}
		finally {
			tracing.end(time, "setAutomaticRepaymentBlockingFlag", null);
		}
	}

	@Override
	public boolean getAutomaticRepaymentBlockingFlag(int partyNo, UserLogin user) throws AccessDeniedException {
		final long time = tracing.start();
		try {
			return target.getAutomaticRepaymentBlockingFlag(partyNo, user);
		}
		finally {
			tracing.end(time, "getAutomaticRepaymentBlockingFlag", null);
		}
	}

	@Override
	public OrdinaryTaxDeclarationAckResponse ackOrdinaryTaxDeclarations(UserLogin user, OrdinaryTaxDeclarationAckRequest request) throws AccessDeniedException {
		final long time = tracing.start();
		try {
			return target.ackOrdinaryTaxDeclarations(user, request);
		}
		finally {
			tracing.end(time, "ackOrdinaryTaxDeclarations", null);
		}
	}

	@Override
	public DeadlineResponse newOrdinaryTaxDeclarationDeadline(int partyNo, int pf, int seqNo, UserLogin user, DeadlineRequest request) throws AccessDeniedException {
		final long time = tracing.start();
		try {
			return target.newOrdinaryTaxDeclarationDeadline(partyNo, pf, seqNo, user, request);
		}
		finally {
			tracing.end(time, "newOrdinaryTaxDeclarationDeadline", null);
		}
	}

	@Override
	public TaxOffices getTaxOffices(int municipalityId, @Nullable RegDate date) {
		final long time = tracing.start();
		try {
			return target.getTaxOffices(municipalityId, date);
		}
		finally {
			tracing.end(time, "getTaxOffices", null);
		}
	}

	@Override
	public PartyNumberList getModifiedTaxPayers(UserLogin user, Date since, Date until) throws AccessDeniedException {
		final long time = tracing.start();
		try {
			return target.getModifiedTaxPayers(user, since, until);
		}
		finally {
			tracing.end(time, "getModifiedTaxPayers", null);
		}
	}

	@Override
	public DebtorInfo getDebtorInfo(UserLogin user, int debtorNo, int pf) throws AccessDeniedException {
		final long time = tracing.start();
		try {
			return target.getDebtorInfo(user, debtorNo, pf);
		}
		finally {
			tracing.end(time, "getDebtorInfo", null);
		}
	}

	@Override
	public List<PartyInfo> searchParty(UserLogin user, @Nullable String partyNo, @Nullable String name, SearchMode nameSearchMode,
	                                   @Nullable String townOrCountry, @Nullable RegDate dateOfBirth, @Nullable String socialInsuranceNumber, @Nullable String uidNumber,
	                                   @Nullable Integer taxResidenceFSOId, boolean onlyActiveMainTaxResidence, @Nullable Set<PartySearchType> partyTypes,
	                                   @Nullable DebtorCategory debtorCategory, @Nullable Boolean activeParty, @Nullable Long oldWithholdingNumber) throws AccessDeniedException, IndexerException {
		final long time = tracing.start();
		try {
			return target.searchParty(user, partyNo, name, nameSearchMode, townOrCountry, dateOfBirth, socialInsuranceNumber, uidNumber, taxResidenceFSOId, onlyActiveMainTaxResidence,
			                          partyTypes, debtorCategory, activeParty, oldWithholdingNumber);
		}
		finally {
			tracing.end(time, "searchParty", null);
		}
	}

	@Override
	public List<CorporationEvent> searchCorporationEvent(UserLogin user, @Nullable Integer corporationId, @Nullable String eventCode,
	                                                     @Nullable RegDate startDate, @Nullable RegDate endDate) throws AccessDeniedException, EmptySearchCriteriaException {
		final long time = tracing.start();
		try {
			return target.searchCorporationEvent(user, corporationId, eventCode, startDate, endDate);
		}
		finally {
			tracing.end(time, "searchCorporationEvent", null);
		}
	}

	@Override
	public Party getParty(UserLogin user, int partyNo, @Nullable Set<PartyPart> parts) throws AccessDeniedException, ServiceException {
		final long time = tracing.start();
		try {
			return target.getParty(user, partyNo, parts);
		}
		finally {
			tracing.end(time, "getParty", null);
		}
	}

	@Override
	public Parties getParties(UserLogin user, List<Integer> partyNos, @Nullable Set<PartyPart> parts) throws AccessDeniedException, ServiceException {
		final long time = tracing.start();
		int resultSize = 0;
		try {
			final Parties parties = target.getParties(user, partyNos, parts);
			if (parties != null && parties.getEntries() != null) {
				for (Entry entry : parties.getEntries()) {
					if (entry.getParty() != null) {
						++ resultSize;
					}
				}
			}
			return parties;
		}
		finally {
			tracing.end(time, null, "getParties", resultSize, null);
		}
	}

	@Override
	public ImageData getAvatar(int partyNo) throws ServiceException {
		final long time = tracing.start();
		try {
			return target.getAvatar(partyNo);
		}
		finally {
			tracing.end(time, "getAvatar", null);
		}
	}
}
