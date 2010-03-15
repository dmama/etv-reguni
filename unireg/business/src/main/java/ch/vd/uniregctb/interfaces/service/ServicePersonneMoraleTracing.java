package ch.vd.uniregctb.interfaces.service;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adresse.AdressesPM;
import ch.vd.uniregctb.adresse.AdressesPMHisto;
import ch.vd.uniregctb.interfaces.model.EvenementPM;
import ch.vd.uniregctb.interfaces.model.PersonneMorale;
import ch.vd.uniregctb.stats.StatsService;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import java.util.List;
import java.util.Map;

/**
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class ServicePersonneMoraleTracing implements ServicePersonneMoraleService, ServiceTracingInterface, InitializingBean, DisposableBean {

	private ServicePersonneMoraleService target;
	private StatsService statsService;

	private final ServiceTracing tracing = new ServiceTracing();

	public void setTarget(ServicePersonneMoraleService target) {
		this.target = target;
	}

	public void setStatsService(StatsService statsService) {
		this.statsService = statsService;
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

	public long getLastCallTime() {
		return tracing.getLastCallTime();
	}

	public long getTotalTime() {
		return tracing.getTotalTime();
	}

	public long getTotalPing() {
		return tracing.getTotalPing();
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

	public List<Long> getAllIds() {
		List<Long> result;
		long time = tracing.start();
		try {
			result = target.getAllIds();
		}
		finally {
			tracing.end(time);
		}
		return result;
	}

	public PersonneMorale getPersonneMorale(Long id, PartPM... parts) {
		PersonneMorale result;
		long time = tracing.start();
		try {
			result = target.getPersonneMorale(id, parts);
		}
		finally {
			tracing.end(time);
		}
		return result;
	}

	public List<PersonneMorale> getPersonnesMorales(List<Long> ids, PartPM... parts) {
		List<PersonneMorale> result;
		long time = tracing.start();
		try {
			result = target.getPersonnesMorales(ids, parts);
		}
		finally {
			tracing.end(time);
		}
		return result;
	}

	public AdressesPM getAdresses(long noEntreprise, RegDate date) {
		AdressesPM result;
		long time = tracing.start();
		try {
			result = target.getAdresses(noEntreprise, date);
		}
		finally {
			tracing.end(time);
		}
		return result;
	}

	public AdressesPMHisto getAdressesHisto(long noEntreprise) {
		AdressesPMHisto result;
		long time = tracing.start();
		try {
			result = target.getAdressesHisto(noEntreprise);
		}
		finally {
			tracing.end(time);
		}
		return result;
	}

	public List<EvenementPM> findEvenements(Long numeroEntreprise, String code, RegDate minDate, RegDate maxDate) {
		List<EvenementPM> result;
		long time = tracing.start();
		try {
			result = target.findEvenements(numeroEntreprise, code, minDate, maxDate);
		}
		finally {
			tracing.end(time);
		}
		return result;
	}
}
