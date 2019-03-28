package ch.vd.unireg.wsclient.refsec;

import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.unireg.stats.ServiceTracing;
import ch.vd.unireg.stats.StatsService;
import ch.vd.unireg.wsclient.refsec.model.ProfilOperateur;
import ch.vd.unireg.wsclient.refsec.model.User;

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

	@Nullable
	@Override
	public User getUser(@NotNull String visa) throws RefSecClientException {
		Throwable t = null;
		final long time = tracing.start();
		try {
			return target.getUser(visa);
		}
		catch (RefSecClientException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getUser", () -> String.format("visa=%s ", visa));
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
	public List<User> getUsersFromCollectivite(@NotNull Integer collectivite) throws RefSecClientException {
		Throwable t = null;
		final long time = tracing.start();
		try {
			return target.getUsersFromCollectivite(collectivite);
		}
		catch (RefSecClientException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getUsersFromCollectivite", () -> String.format("visa=%d ", collectivite));
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
