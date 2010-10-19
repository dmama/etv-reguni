package ch.vd.uniregctb.interfaces.service;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adresse.AdressesPM;
import ch.vd.uniregctb.adresse.AdressesPMHisto;
import ch.vd.uniregctb.interfaces.model.Etablissement;
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
public class ServicePersonneMoraleTracing implements ServicePersonneMoraleService, InitializingBean, DisposableBean {

	private ServicePersonneMoraleService target;
	private StatsService statsService;

	private final ServiceTracing tracing = new ServiceTracing(SERVICE_NAME);

	public void setTarget(ServicePersonneMoraleService target) {
		this.target = target;
	}

	public void setStatsService(StatsService statsService) {
		this.statsService = statsService;
	}

	public void afterPropertiesSet() throws Exception {
		if (statsService != null) {
			statsService.registerService(SERVICE_NAME, tracing);
		}
	}

	public void destroy() throws Exception {
		if (statsService != null) {
			statsService.unregisterService(SERVICE_NAME);
		}
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
			tracing.end(time, "getAllIds");
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
			tracing.end(time, "getPersonneMorale");
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
			tracing.end(time, "getPersonnesMorales");
		}
		return result;
	}

	public Etablissement getEtablissement(long id) {
		Etablissement result;
		long time = tracing.start();
		try {
			result = target.getEtablissement(id);
		}
		finally {
			tracing.end(time, "getEtablissement");
		}
		return result;
	}

	public List<Etablissement> getEtablissements(List<Long> ids) {
		List<Etablissement> result;
		long time = tracing.start();
		try {
			result = target.getEtablissements(ids);
		}
		finally {
			tracing.end(time, "getEtablissements");
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
			tracing.end(time, "getAdresses");
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
			tracing.end(time, "getAdressesHisto");
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
			tracing.end(time, "findEvenements");
		}
		return result;
	}
}
