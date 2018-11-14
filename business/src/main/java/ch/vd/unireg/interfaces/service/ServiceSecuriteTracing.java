package ch.vd.unireg.interfaces.service;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.unireg.interfaces.infra.data.CollectiviteAdministrative;
import ch.vd.unireg.interfaces.infra.data.TypeCollectivite;
import ch.vd.unireg.interfaces.service.host.Operateur;
import ch.vd.unireg.security.IfoSecProfil;
import ch.vd.unireg.stats.ServiceTracing;
import ch.vd.unireg.stats.StatsService;

/**
 * Implémentation qui permet de comptabiliser le temps passé dans les appels du service.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class ServiceSecuriteTracing implements ServiceSecuriteService, InitializingBean, DisposableBean {

	private ServiceSecuriteService target;
	private StatsService statsService;

	private final ServiceTracing tracing = new ServiceTracing(SERVICE_NAME);

	public void setTarget(ServiceSecuriteService target) {
		this.target = target;
	}

	public void setStatsService(StatsService statsService) {
		this.statsService = statsService;
	}

	@Override
	public List<CollectiviteAdministrative> getCollectivitesUtilisateur(final String visaOperateur) {
		Throwable t = null;
		int items = 0;
		final long time = tracing.start();
		try {
			final List<CollectiviteAdministrative> list = target.getCollectivitesUtilisateur(visaOperateur);
			items = list == null ? 0 : list.size();
			return list;
		}
		catch (RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getCollectivitesUtilisateur", items, () -> String.format("visaOperateur=%s", visaOperateur));
		}
	}

	@Override
	public Operateur getOperateur(final long individuNoTechnique) {
		Throwable t = null;
		int items = 0;
		final long time = tracing.start();
		try {
			final Operateur operateur = target.getOperateur(individuNoTechnique);
			items = operateur != null ? 1 : 0;
			return operateur;
		}
		catch (RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getOperateur", items, () -> String.format("individuNoTechnique=%d", individuNoTechnique));
		}
	}

	@Override
	public Operateur getOperateur(@NotNull final String visa) {
		Throwable t = null;
		int items = 0;
		final long time = tracing.start();
		try {
			final Operateur operateur = target.getOperateur(visa);
			items = operateur != null ? 1 : 0;
			return operateur;
		}
		catch (RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getOperateur", items, () -> String.format("visa=%s", visa));
		}
	}

	@Override
	public IfoSecProfil getProfileUtilisateur(final String visaOperateur, final int codeCollectivite) {
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
			tracing.end(time, t, "getProfileUtilisateur", () -> String.format("visaOperateur=%s, codeCollectivite=%d", visaOperateur, codeCollectivite));
		}
	}

	@Override
	public List<Operateur> getUtilisateurs(final List<TypeCollectivite> typesCollectivite) {
		Throwable t = null;
		int items = 0;
		final long time = tracing.start();
		try {
			final List<Operateur> list = target.getUtilisateurs(typesCollectivite);
			items = list == null ? 0 : list.size();
			return list;
		}
		catch (RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getUtilisateurs", items, () -> String.format("typesCollectivites=%s", ServiceTracing.toString(typesCollectivite)));
		}
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
}
