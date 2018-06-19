package ch.vd.unireg.wsclient.rcent;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;

import ch.vd.evd0022.v3.NoticeRequest;
import ch.vd.evd0022.v3.NoticeRequestReport;
import ch.vd.evd0022.v3.OrganisationData;
import ch.vd.evd0022.v3.OrganisationsOfNotice;
import ch.vd.evd0023.v3.ListOfNoticeRequest;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.stats.ServiceTracing;
import ch.vd.unireg.stats.StatsService;

public class RcEntClientTracing implements RcEntClient, InitializingBean, DisposableBean {

	public static final String SERVICE_NAME = "RcEntClient";

	private RcEntClient target;

	private StatsService statsService;

	private final ServiceTracing tracing = new ServiceTracing(SERVICE_NAME);

	public void setTarget(RcEntClient target) {
		this.target = target;
	}

	public void setStatsService(StatsService statsService) {
		this.statsService = statsService;
	}

	@Override
	public void destroy() throws Exception {
		if (statsService != null) {
			statsService.unregisterService(SERVICE_NAME);
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (statsService != null) {
			statsService.registerService(SERVICE_NAME, tracing);
		}
	}

	@Override
	public OrganisationData getOrganisation(final long id, final RegDate referenceDate, final boolean withHistory) throws RcEntClientException {
		Throwable t = null;
		int items = 0;
		final long time = tracing.start();
		try {
			final OrganisationData data = target.getOrganisation(id, referenceDate, withHistory);
			if (data != null) {
				items = 1;
			}
			return data;
		}
		catch (RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getEntrepriseCivile", items, () -> String.format("id=%d, referenceDate=%s, history=%s", id, ServiceTracing.toString(referenceDate), withHistory));
		}
	}

	@Override
	public OrganisationsOfNotice getOrganisationsOfNotice(final long noticeId, final OrganisationState when) throws RcEntClientException {
		Throwable t = null;
		int items = 0;
		final long time = tracing.start();
		try {
			final OrganisationsOfNotice data = target.getOrganisationsOfNotice(noticeId, when);
			if (data != null) {
				items = data.getOrganisation().size();
			}
			return data;
		}
		catch (RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getOrganisationsOfNotice", items, () -> String.format("noticeId=%d, when=%s", noticeId, when));
		}
	}

	@Override
	public OrganisationData getOrganisationByNoIDE(final String noide, final RegDate referenceDate, final boolean withHistory) throws RcEntClientException {
		Throwable t = null;
		int items = 0;
		final long time = tracing.start();
		try {
			final OrganisationData data = target.getOrganisationByNoIDE(noide, referenceDate, withHistory);
			if (data != null) {
				items = 1;
			}
			return data;
		}
		catch (RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getOrganisationByNoIDE", items, () -> String.format("ide=%s, referenceDate=%s, withHistory=%s", noide, ServiceTracing.toString(referenceDate), withHistory));
		}
	}

	@Override
	public NoticeRequestReport validateNoticeRequest(final NoticeRequest noticeRequest) throws RcEntClientException {
		Throwable t = null;
		int items = 0;
		final long time = tracing.start();
		try {
			final NoticeRequestReport data = target.validateNoticeRequest(noticeRequest);
			if (data != null) {
				items = 1;
			}
			return data;
		}
		catch (RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "validateNoticeRequest", items, () -> String.format("noticeRequest=%s", noticeRequest.getNoticeRequestHeader().getNoticeRequestIdentification().getNoticeRequestId()));
		}
	}

	@Override
	public ListOfNoticeRequest getNoticeRequest(final String id) throws RcEntClientException {
		Throwable t = null;
		int items = 0;
		final long time = tracing.start();
		try {
			final ListOfNoticeRequest data = target.getNoticeRequest(id);
			if (data != null && data.getNumberOfResults() != 0) {
				items = data.getNumberOfResults();
			}
			return data;
		}
		catch (RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getNoticeRequest", items, () -> String.format("id=%s", id));
		}
	}

	@Override
	public Page<NoticeRequestReport> findNotices(@NotNull final RcEntNoticeQuery query, @Nullable final Sort.Order order, final int pageNumber, final int resultsPerPage) throws RcEntClientException {
		Throwable t = null;
		int items = 0;
		final long time = tracing.start();
		try {
			final Page<NoticeRequestReport> page = target.findNotices(query, order, pageNumber, resultsPerPage);
			if (page != null && page.getNumberOfElements() != 0) {
				items = page.getNumberOfElements();
			}
			return page;
		}
		catch (RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getNoticeRequest", items, () -> String.format("query=%s, order=%s, pageNumber=%d, resultsPerPage=%d", query, order, pageNumber, resultsPerPage));
		}
	}

	@Override
	public void ping() throws RcEntClientException {
		Throwable t = null;
		final long time = tracing.start();
		try {
			target.ping();
		}
		catch (RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "ping", null);
		}
	}
}
