package ch.vd.unireg.common;

import java.io.IOException;
import java.net.URL;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.unireg.stats.ServiceTracing;
import ch.vd.unireg.stats.StatsService;

public class HttpDocumentFetcherTracing implements HttpDocumentFetcher, InitializingBean, DisposableBean {

	public static final String SERVICE_NAME = "HttpDocumentFetcher";

	private final ServiceTracing tracing = new ServiceTracing(SERVICE_NAME);
	private StatsService statsService;
	private HttpDocumentFetcher target;

	public void setTarget(HttpDocumentFetcher target) {
		this.target = target;
	}

	public void setStatsService(StatsService statsService) {
		this.statsService = statsService;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		statsService.registerService(SERVICE_NAME, tracing);
	}

	@Override
	public void destroy() throws Exception {
		statsService.unregisterService(SERVICE_NAME);
	}

	@Override
	public HttpDocument fetch(final URL url, final Integer timeoutms) throws IOException, HttpDocumentException {
		final long start = tracing.start();
		Throwable t = null;
		try {
			return target.fetch(url, timeoutms);
		}
		catch (IOException | HttpDocumentException | RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(start, t, "fetch", () -> String.format("url=%s, timeoutms=%d", url, timeoutms));
		}
	}
}
