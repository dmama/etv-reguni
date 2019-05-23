package ch.vd.unireg.interfaces.securite;

import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.unireg.interfaces.infra.InfrastructureConnector;
import ch.vd.unireg.security.Operateur;
import ch.vd.unireg.security.ProfileOperateur;
import ch.vd.unireg.stats.ServiceTracing;
import ch.vd.unireg.stats.StatsService;

public class SecuriteConnectorTracing implements SecuriteConnector, InitializingBean, DisposableBean {

	private SecuriteConnector target;
	private StatsService statsService;

	private final ServiceTracing tracing = new ServiceTracing(SecuriteConnectorTracing.SERVICE_NAME);

	@Override
	public void afterPropertiesSet() throws Exception {
		if (statsService != null) {
			statsService.registerService(InfrastructureConnector.SERVICE_NAME, tracing);
		}
	}

	@Override
	public void destroy() throws Exception {
		if (statsService != null) {
			statsService.unregisterService(InfrastructureConnector.SERVICE_NAME);
		}
	}

	@Override
	@Nullable
	public Operateur getOperateur(@NotNull String visa) throws SecuriteConnectorException {
		Throwable t = null;
		final long time = tracing.start();
		try {
			return target.getOperateur(visa);
		}
		catch (RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getOperateur", () -> "visa=" + visa);
		}
	}

	@Override
	@Nullable
	public ProfileOperateur getProfileUtilisateur(String visaOperateur, int codeCollectivite) throws SecuriteConnectorException {
		Throwable t = null;
		final long time = tracing.start();
		try {
			return target.getProfileUtilisateur(visaOperateur, codeCollectivite);
		}
		catch (RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getProfileUtilisateur", () -> "visaOperateur=" + visaOperateur + ", codeCollectivite=" + codeCollectivite);
		}
	}

	@NotNull
	@Override
	public List<String> getUtilisateurs(int noCollAdmin) throws SecuriteConnectorException {
		Throwable t = null;
		final long time = tracing.start();
		try {
			return target.getUtilisateurs(noCollAdmin);
		}
		catch (RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getUtilisateurs", () -> "noCollAdmin=" + noCollAdmin);
		}
	}

	@Override
	@NotNull
	public Set<Integer> getCollectivitesOperateur(@NotNull String visaOperateur) throws SecuriteConnectorException {
		Throwable t = null;
		final long time = tracing.start();
		try {
			return target.getCollectivitesOperateur(visaOperateur);
		}
		catch (RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getCollectivitesOperateur", () -> "visaOperateur=" + visaOperateur);
		}
	}

	@Override
	public void ping() throws SecuriteConnectorException {
		target.ping();
	}

	public void setTarget(SecuriteConnector target) {
		this.target = target;
	}

	public void setStatsService(StatsService statsService) {
		this.statsService = statsService;
	}
}
