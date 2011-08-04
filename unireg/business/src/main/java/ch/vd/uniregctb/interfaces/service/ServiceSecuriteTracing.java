package ch.vd.uniregctb.interfaces.service;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.infrastructure.model.CollectiviteAdministrative;
import ch.vd.infrastructure.model.EnumTypeCollectivite;
import ch.vd.securite.model.Operateur;
import ch.vd.securite.model.ProfilOperateur;
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
		List<CollectiviteAdministrative> result;
		long time = tracing.start();
		try {
			result = target.getCollectivitesUtilisateur(visaOperateur);
		}
		finally {
			tracing.end(time, "getCollectivitesUtilisateur", new Object() {
				@Override
				public String toString() {
					return String.format("visaOperateur=%s", visaOperateur);
				}
			});
		}
		return result;
	}

	@Override
	public List<ProfilOperateur> getListeOperateursPourFonctionCollectivite(final String codeFonction, final int noCollectivite) {
		List<ProfilOperateur> result;
		long time = tracing.start();
		try {
			result = target.getListeOperateursPourFonctionCollectivite(codeFonction, noCollectivite);
		}
		finally {
			tracing.end(time, "getListeOperateursPourFonctionCollectivite", new Object() {
				@Override
				public String toString() {
					return String.format("codeFonction=%s, noCollectivite=%d", codeFonction, noCollectivite);
				}
			});
		}
		return result;
	}

	@Override
	public Operateur getOperateur(final long individuNoTechnique) {
		Operateur result;
		long time = tracing.start();
		try {
			result = target.getOperateur(individuNoTechnique);
		}
		finally {
			tracing.end(time, "getOperateur", new Object() {
				@Override
				public String toString() {
					return String.format("individuNoTechnique=%d", individuNoTechnique);
				}
			});
		}
		return result;
	}

	@Override
	public Operateur getOperateur(final String visa) {
		Operateur result;
		long time = tracing.start();
		try {
			result = target.getOperateur(visa);
		}
		finally {
			tracing.end(time, "getOperateur", new Object() {
				@Override
				public String toString() {
					return String.format("visa=%s", visa);
				}
			});
		}
		return result;
	}

	@Override
	public ProfilOperateur getProfileUtilisateur(final String visaOperateur, final int codeCollectivite) {
		ProfilOperateur result;
		long time = tracing.start();
		try {
			result = target.getProfileUtilisateur(visaOperateur, codeCollectivite);
		}
		finally {
			tracing.end(time, "getProfileUtilisateur", new Object() {
				@Override
				public String toString() {
					return String.format("visaOperateur=%s, codeCollectivite=%d", visaOperateur, codeCollectivite);
				}
			});
		}
		return result;
	}

	@Override
	public List<Operateur> getUtilisateurs(final List<EnumTypeCollectivite> typesCollectivite) {
		List<Operateur> result;
		long time = tracing.start();
		try {
			result = target.getUtilisateurs(typesCollectivite);
		}
		finally {
			tracing.end(time, "getUtilisateurs", new Object() {
				@Override
				public String toString() {
					return String.format("typesCollectivites=%s", ServiceTracing.toString(typesCollectivite));
				}
			});
		}
		return result;
	}
	
	public Map<String, ? extends ServiceTracingInterface> getDetailedData() {
		return null;
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
