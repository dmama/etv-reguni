package ch.vd.unireg.interfaces.entreprise.mock;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;

import ch.vd.evd0022.v3.NoticeRequest;
import ch.vd.evd0022.v3.NoticeRequestReport;
import ch.vd.evd0022.v3.OrganisationData;
import ch.vd.evd0022.v3.OrganisationsOfNotice;
import ch.vd.evd0023.v3.ListOfNoticeRequest;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.wsclient.rcent.RcEntClient;
import ch.vd.unireg.wsclient.rcent.RcEntClientException;
import ch.vd.unireg.wsclient.rcent.RcEntNoticeQuery;

/**
 * Mock du client RCEnt qui ne fait rien et retourne toujours null aux appels de méthodes.
 */
public class MockRcEntClient implements RcEntClient {
	@Override
	public OrganisationData getOrganisation(long id, RegDate referenceDate, boolean withHistory) throws RcEntClientException {
		return null;
	}

	@Override
	public OrganisationsOfNotice getOrganisationsOfNotice(long noticeId, OrganisationState when) throws RcEntClientException {
		return null;
	}

	@Nullable
	@Override
	public OrganisationData getOrganisationByNoIDE(String noide, RegDate referenceDate, boolean withHistory) throws RcEntClientException {
		return null;
	}

	@Override
	public NoticeRequestReport validateNoticeRequest(NoticeRequest noticeRequest) throws RcEntClientException {
		return null;
	}

	@Override
	public ListOfNoticeRequest getNoticeRequest(String noticeRequestId) throws RcEntClientException {
		return null;
	}

	@Override
	public Page<NoticeRequestReport> findNotices(@NotNull RcEntNoticeQuery query, @Nullable Sort.Order order, int pageNumber, int resultsPerPage) throws RcEntClientException {
		return null;
	}

	@Override
	public void ping() throws RcEntClientException {

	}
}
