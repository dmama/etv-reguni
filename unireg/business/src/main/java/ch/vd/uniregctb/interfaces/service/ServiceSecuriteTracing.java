package ch.vd.uniregctb.interfaces.service;

import java.util.List;
import java.util.Map;

import ch.vd.uniregctb.stats.StatsService;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.infrastructure.model.CollectiviteAdministrative;
import ch.vd.infrastructure.model.EnumTypeCollectivite;
import ch.vd.securite.model.Operateur;
import ch.vd.securite.model.ProfilOperateur;

/**
 * Implémentation qui permet de comptabiliser le temps passé dans les appels du service.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class ServiceSecuriteTracing implements ServiceSecuriteService, ServiceTracingInterface, InitializingBean, DisposableBean {

	private ServiceSecuriteService target;
	private StatsService statsService;

	private final ServiceTracing tracing = new ServiceTracing();

	public void setTarget(ServiceSecuriteService target) {
		this.target = target;
	}

	public void setStatsService(StatsService statsService) {
		this.statsService = statsService;
	}

	public List<CollectiviteAdministrative> getCollectivitesUtilisateur(String visaOperateur) {
		List<CollectiviteAdministrative> result;
		long time = tracing.start();
		try {
			result = target.getCollectivitesUtilisateur(visaOperateur);
		}
		finally {
			tracing.end(time);
		}
		return result;
	}

	public List<ProfilOperateur> getListeOperateursPourFonctionCollectivite(String codeFonction, int noCollectivite) {
		List<ProfilOperateur> result;
		long time = tracing.start();
		try {
			result = target.getListeOperateursPourFonctionCollectivite(codeFonction, noCollectivite);
		}
		finally {
			tracing.end(time);
		}
		return result;
	}

	public Operateur getOperateur(long individuNoTechnique) {
		Operateur result;
		long time = tracing.start();
		try {
			result = target.getOperateur(individuNoTechnique);
		}
		finally {
			tracing.end(time);
		}
		return result;
	}

	public Operateur getOperateur(String visa) {
		Operateur result;
		long time = tracing.start();
		try {
			result = target.getOperateur(visa);
		}
		finally {
			tracing.end(time);
		}
		return result;
	}

	public ProfilOperateur getProfileUtilisateur(String visaOperateur, int codeCollectivite) {
		ProfilOperateur result;
		long time = tracing.start();
		try {
			result = target.getProfileUtilisateur(visaOperateur, codeCollectivite);
		}
		finally {
			tracing.end(time);
		}
		return result;
	}

	public List<Operateur> getUtilisateurs(List<EnumTypeCollectivite> typesCollectivite) {
		List<Operateur> result;
		long time = tracing.start();
		try {
			result = target.getUtilisateurs(typesCollectivite);
		}
		finally {
			tracing.end(time);
		}
		return result;
	}

	public long getLastCallTime() {
		return tracing.getLastCallTime();
	}

	public long getTotalPing() {
		return tracing.getTotalPing();
	}

	public long getTotalTime() {
		return tracing.getTotalTime();
	}

	public long getRecentTime() {
		return tracing.getRecentTime();
	}

	public long getRecentPing() {
		return tracing.getRecentPing();
	}

	public Map<String, ? extends ServiceTracingInterface> getDetailedData() {
		return null;
	}

	public void afterPropertiesSet() throws Exception {
		if (statsService != null) {
			statsService.registerRaw(SERVICE_NAME, this);
		}
	}

	public void destroy() throws Exception {
		if (statsService != null) {
			statsService.unregisterRaw(SERVICE_NAME);
		}
	}
}
