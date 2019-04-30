package ch.vd.unireg.webservices.v6;

import java.util.Date;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.avatar.ImageData;
import ch.vd.unireg.indexer.IndexerException;
import ch.vd.unireg.stats.ServiceTracing;
import ch.vd.unireg.stats.StatsService;
import ch.vd.unireg.webservices.common.AccessDeniedException;
import ch.vd.unireg.webservices.common.UserLogin;
import ch.vd.unireg.ws.ack.v6.OrdinaryTaxDeclarationAckRequest;
import ch.vd.unireg.ws.ack.v6.OrdinaryTaxDeclarationAckResponse;
import ch.vd.unireg.ws.deadline.v6.DeadlineRequest;
import ch.vd.unireg.ws.deadline.v6.DeadlineResponse;
import ch.vd.unireg.ws.modifiedtaxpayers.v6.PartyNumberList;
import ch.vd.unireg.ws.parties.v6.Entry;
import ch.vd.unireg.ws.parties.v6.Parties;
import ch.vd.unireg.ws.security.v6.SecurityResponse;
import ch.vd.unireg.ws.taxoffices.v6.TaxOffices;
import ch.vd.unireg.xml.ServiceException;
import ch.vd.unireg.xml.party.v4.Party;
import ch.vd.unireg.xml.party.v4.PartyInfo;
import ch.vd.unireg.xml.party.v4.PartyPart;
import ch.vd.unireg.xml.party.withholding.v1.DebtorCategory;
import ch.vd.unireg.xml.party.withholding.v1.DebtorInfo;

public class BusinessWebServiceTracing implements BusinessWebService, InitializingBean, DisposableBean {

	public static final String SERVICE_NAME = "WebService6";

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
	public void setAutomaticRepaymentBlockingFlag(int partyNo, UserLogin user, boolean blocked) throws AccessDeniedException {
		Throwable t = null;
		final long time = tracing.start();
		try {
			target.setAutomaticRepaymentBlockingFlag(partyNo, user, blocked);
		}
		catch (RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "setAutomaticRepaymentBlockingFlag", null);
		}
	}

	@Override
	public boolean getAutomaticRepaymentBlockingFlag(int partyNo, UserLogin user) throws AccessDeniedException {
		Throwable t = null;
		final long time = tracing.start();
		try {
			return target.getAutomaticRepaymentBlockingFlag(partyNo, user);
		}
		catch (RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getAutomaticRepaymentBlockingFlag", null);
		}
	}

	@Override
	public OrdinaryTaxDeclarationAckResponse ackOrdinaryTaxDeclarations(UserLogin user, OrdinaryTaxDeclarationAckRequest request) throws AccessDeniedException {
		Throwable t = null;
		final long time = tracing.start();
		try {
			return target.ackOrdinaryTaxDeclarations(user, request);
		}
		catch (RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "ackOrdinaryTaxDeclarations", null);
		}
	}

	@Override
	public DeadlineResponse newOrdinaryTaxDeclarationDeadline(int partyNo, int pf, int seqNo, UserLogin user, DeadlineRequest request) throws AccessDeniedException {
		Throwable t = null;
		final long time = tracing.start();
		try {
			return target.newOrdinaryTaxDeclarationDeadline(partyNo, pf, seqNo, user, request);
		}
		catch (RuntimeException | Error e) {
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
	public PartyNumberList getModifiedTaxPayers(UserLogin user, Date since, Date until) throws AccessDeniedException {
		Throwable t = null;
		final long time = tracing.start();
		try {
			return target.getModifiedTaxPayers(user, since, until);
		}
		catch (RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getModifiedTaxPayers", null);
		}
	}

	@Override
	public DebtorInfo getDebtorInfo(UserLogin user, int debtorNo, int pf) throws AccessDeniedException {
		Throwable t = null;
		final long time = tracing.start();
		try {
			return target.getDebtorInfo(user, debtorNo, pf);
		}
		catch (RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getDebtorInfo", null);
		}
	}

	@Override
	public List<PartyInfo> searchParty(UserLogin user, @Nullable String partyNo, @Nullable String name, SearchMode nameSearchMode,
	                                   @Nullable String townOrCountry, @Nullable RegDate dateOfBirth, @Nullable String socialInsuranceNumber, @Nullable String uidNumber,
	                                   @Nullable Integer taxResidenceFSOId, boolean onlyActiveMainTaxResidence, @Nullable Set<PartySearchType> partyTypes,
	                                   @Nullable DebtorCategory debtorCategory, @Nullable Boolean activeParty, @Nullable Long oldWithholdingNumber) throws AccessDeniedException, IndexerException {
		Throwable t = null;
		final long time = tracing.start();
		try {
			return target.searchParty(user, partyNo, name, nameSearchMode, townOrCountry, dateOfBirth, socialInsuranceNumber, uidNumber, taxResidenceFSOId, onlyActiveMainTaxResidence,
			                          partyTypes, debtorCategory, activeParty, oldWithholdingNumber);
		}
		catch (RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "searchParty", null);
		}
	}

	@Override
	public Party getParty(UserLogin user, int partyNo, @Nullable Set<PartyPart> parts) throws AccessDeniedException, ServiceException {
		Throwable t = null;
		final long time = tracing.start();
		try {
			return target.getParty(user, partyNo, parts);
		}
		catch (RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getParty", null);
		}
	}

	@Override
	public Parties getParties(UserLogin user, List<Integer> partyNos, @Nullable Set<PartyPart> parts) throws AccessDeniedException, ServiceException {
		Throwable t = null;
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
		catch (RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getParties", resultSize, null);
		}
	}

	@Override
	public ImageData getAvatar(int partyNo) throws ServiceException {
		Throwable t = null;
		final long time = tracing.start();
		try {
			return target.getAvatar(partyNo);
		}
		catch (RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getAvatar", null);
		}
	}
}
