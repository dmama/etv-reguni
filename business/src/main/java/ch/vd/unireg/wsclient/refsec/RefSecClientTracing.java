package ch.vd.unireg.wsclient.refsec;

import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.unireg.stats.ServiceTracing;
import ch.vd.unireg.stats.StatsService;
import ch.vd.unireg.wsclient.refsec.model.ProfilOperateur;

public class RefSecClientTracing implements RefSecClient, InitializingBean, DisposableBean {

	public static final String SERVICE_NAME = "RefSecSecuriteClient";
	private final ServiceTracing tracing = new ServiceTracing(SERVICE_NAME);

	private RefSecClient target;
	private StatsService statsService;

	public void setTarget(RefSecClient target) {
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
	public @Nullable ProfilOperateur getProfilOperateur(@NotNull String visa, int collectivite) throws RefSecClientException {
		Throwable t = null;
		final long time = tracing.start();
		try {
			return target.getProfilOperateur(visa, collectivite);
		}
		catch (RefSecClientException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getProfilOperateur", () -> String.format("visa=%s , collectivite=%d ", visa, collectivite));
		}
	}

	@Override
	public Set<Integer> getCollectivitesOperateur(@NotNull String visa) throws RefSecClientException {
		Throwable t = null;
		final long time = tracing.start();
		try {
			return target.getCollectivitesOperateur(visa);
		}
		catch (RefSecClientException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getCollectivitesOperateur", () -> String.format("visa=%s", visa));
		}
	}

	@Override
	public void ping() throws RefSecClientException {
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
