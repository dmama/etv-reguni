package ch.vd.uniregctb.interfaces.service;

import java.util.List;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.infrastructure.model.CollectiviteAdministrative;
import ch.vd.infrastructure.model.EnumTypeCollectivite;
import ch.vd.securite.model.Operateur;
import ch.vd.securite.model.ProfilOperateur;
import ch.vd.uniregctb.security.IfoSecProfil;
import ch.vd.uniregctb.stats.ServiceTracing;
import ch.vd.uniregctb.stats.StatsService;

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
		final long time = tracing.start();
		try {
			return target.getCollectivitesUtilisateur(visaOperateur);
		}
		catch (RuntimeException e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getCollectivitesUtilisateur", new Object() {
				@Override
				public String toString() {
					return String.format("visaOperateur=%s", visaOperateur);
				}
			});
		}
	}

	@Override
	public List<ProfilOperateur> getListeOperateursPourFonctionCollectivite(final String codeFonction, final int noCollectivite) {
		Throwable t = null;
		final long time = tracing.start();
		try {
			return target.getListeOperateursPourFonctionCollectivite(codeFonction, noCollectivite);
		}
		catch (RuntimeException e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getListeOperateursPourFonctionCollectivite", new Object() {
				@Override
				public String toString() {
					return String.format("codeFonction=%s, noCollectivite=%d", codeFonction, noCollectivite);
				}
			});
		}
	}

	@Override
	public Operateur getOperateur(final long individuNoTechnique) {
		Throwable t = null;
		final long time = tracing.start();
		try {
			return target.getOperateur(individuNoTechnique);
		}
		catch (RuntimeException e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getOperateur", new Object() {
				@Override
				public String toString() {
					return String.format("individuNoTechnique=%d", individuNoTechnique);
				}
			});
		}
	}

	@Override
	public Operateur getOperateur(final String visa) {
		Throwable t = null;
		final long time = tracing.start();
		try {
			return target.getOperateur(visa);
		}
		catch (RuntimeException e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getOperateur", new Object() {
				@Override
				public String toString() {
					return String.format("visa=%s", visa);
				}
			});
		}
	}

	@Override
	public IfoSecProfil getProfileUtilisateur(final String visaOperateur, final int codeCollectivite) {
		Throwable t = null;
		final long time = tracing.start();
		try {
			return target.getProfileUtilisateur(visaOperateur, codeCollectivite);
		}
		catch (RuntimeException e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getProfileUtilisateur", new Object() {
				@Override
				public String toString() {
					return String.format("visaOperateur=%s, codeCollectivite=%d", visaOperateur, codeCollectivite);
				}
			});
		}
	}

	@Override
	public List<Operateur> getUtilisateurs(final List<EnumTypeCollectivite> typesCollectivite) {
		Throwable t = null;
		final long time = tracing.start();
		try {
			return target.getUtilisateurs(typesCollectivite);
		}
		catch (RuntimeException e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getUtilisateurs", new Object() {
				@Override
				public String toString() {
					return String.format("typesCollectivites=%s", ServiceTracing.toString(typesCollectivite));
				}
			});
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
