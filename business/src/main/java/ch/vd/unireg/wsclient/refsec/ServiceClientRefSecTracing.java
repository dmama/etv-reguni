package ch.vd.unireg.wsclient.refsec;


import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.unireg.stats.ServiceTracing;
import ch.vd.unireg.stats.StatsService;
import ch.vd.unireg.wsclient.refsec.model.Information;
import ch.vd.unireg.wsclient.refsec.model.RefSecProfilOperateur;
import ch.vd.unireg.wsclient.refsec.model.Value;

public class ServiceClientRefSecTracing implements ClientRefSec, InitializingBean, DisposableBean {

	public static final String SERVICE_NAME = "RefSecSecuriteClient";

	private ClientRefSec target;

	private StatsService statsService;

	private final ServiceTracing tracing = new ServiceTracing(SERVICE_NAME);

	public void setTarget(ClientRefSec target) {
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
	public List<RefSecProfilOperateur> getProfileUtilisateurs(@NotNull String visa) throws ClientRefSecException {
		Throwable t = null;
		final long time = tracing.start();
		try {
			return target.getProfileUtilisateurs(visa);
		}
		catch (ClientRefSecException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getProfileUtilisateurs", () -> String.format("visa=%s", visa));
		}
	}

	@Override
	public ProfilOperateurRefSec getAuthorizationsByCodeCollectivite(@NotNull String visa, @NotNull Integer codeCollectivite) throws ClientRefSecException {
		Throwable t = null;
		final long time = tracing.start();
		try {
			return target.getAuthorizationsByCodeCollectivite(visa, codeCollectivite);
		}
		catch (ClientRefSecException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getAuthorizationsByCodeCollectivite", () -> String.format("visa=%s , codeCollectivite=%d ", visa, codeCollectivite));
		}
	}

	@Override
	public List<Value> getCollectivitesUtilisateur(String visa) throws ClientRefSecException {
		Throwable t = null;
		final long time = tracing.start();
		try {
			return target.getCollectivitesUtilisateur(visa);
		}
		catch (ClientRefSecException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getCollectivitesUtilisateur", () -> String.format("visa=%s", visa));
		}
	}

	@Override
	public List<Integer> getCodesCollectivitesUtilisateur(String visa) throws ClientRefSecException {
		Throwable t = null;
		final long time = tracing.start();
		try {
			return target.getCodesCollectivitesUtilisateur(visa);
		}
		catch (ClientRefSecException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getCodesCollectivitesUtilisateur", () -> String.format("visa=%s", visa));
		}
	}

	@Override
	public Information ping() throws ClientRefSecException {
		Throwable t = null;
		final long time = tracing.start();
		try {
			return target.ping();
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
