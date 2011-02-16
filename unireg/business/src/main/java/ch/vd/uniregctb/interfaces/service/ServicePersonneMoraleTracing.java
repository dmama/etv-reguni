package ch.vd.uniregctb.interfaces.service;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adresse.AdressesPM;
import ch.vd.uniregctb.adresse.AdressesPMHisto;
import ch.vd.uniregctb.interfaces.model.Etablissement;
import ch.vd.uniregctb.interfaces.model.EvenementPM;
import ch.vd.uniregctb.interfaces.model.PartPM;
import ch.vd.uniregctb.interfaces.model.PersonneMorale;
import ch.vd.uniregctb.stats.StatsService;

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
			tracing.end(time, "getAllIds", null);
		}
		return result;
	}

	public PersonneMorale getPersonneMorale(final Long id, final PartPM... parts) {
		PersonneMorale result;
		long time = tracing.start();
		try {
			result = target.getPersonneMorale(id, parts);
		}
		finally {
			tracing.end(time, "getPersonneMorale", new Object() {
				@Override
				public String toString() {
					return String.format("id=%d, parts=%s", id, ServiceTracing.toString(parts));
				}
			});
		}
		return result;
	}

	public List<PersonneMorale> getPersonnesMorales(final List<Long> ids, final PartPM... parts) {
		List<PersonneMorale> result;
		long time = tracing.start();
		try {
			result = target.getPersonnesMorales(ids, parts);
		}
		finally {
			tracing.end(time, "getPersonnesMorales", new Object() {
				@Override
				public String toString() {
					return String.format("ids=%s, parts=%s", ServiceTracing.toString(ids), ServiceTracing.toString(parts));
				}
			});
		}
		return result;
	}

	public Etablissement getEtablissement(final long id) {
		Etablissement result;
		long time = tracing.start();
		try {
			result = target.getEtablissement(id);
		}
		finally {
			tracing.end(time, "getEtablissement", new Object() {
				@Override
				public String toString() {
					return String.format("id=%d", id);
				}
			});
		}
		return result;
	}

	public List<Etablissement> getEtablissements(final List<Long> ids) {
		List<Etablissement> result;
		long time = tracing.start();
		try {
			result = target.getEtablissements(ids);
		}
		finally {
			tracing.end(time, "getEtablissements", new Object() {
				@Override
				public String toString() {
					return String.format("ids=%s", ServiceTracing.toString(ids));
				}
			});
		}
		return result;
	}

	public AdressesPM getAdresses(final long noEntreprise, final RegDate date) {
		AdressesPM result;
		long time = tracing.start();
		try {
			result = target.getAdresses(noEntreprise, date);
		}
		finally {
			tracing.end(time, "getAdresses", new Object() {
				@Override
				public String toString() {
					return String.format("noEntreprise=%d, date=%s", noEntreprise, ServiceTracing.toString(date));
				}
			});
		}
		return result;
	}

	public AdressesPMHisto getAdressesHisto(final long noEntreprise) {
		AdressesPMHisto result;
		long time = tracing.start();
		try {
			result = target.getAdressesHisto(noEntreprise);
		}
		finally {
			tracing.end(time, "getAdressesHisto", new Object() {
				@Override
				public String toString() {
					return String.format("noEntreprise=%d", noEntreprise);
				}
			});
		}
		return result;
	}

	public List<EvenementPM> findEvenements(final long numeroEntreprise, final String code, final RegDate minDate, final RegDate maxDate) {
		List<EvenementPM> result;
		long time = tracing.start();
		try {
			result = target.findEvenements(numeroEntreprise, code, minDate, maxDate);
		}
		finally {
			tracing.end(time, "findEvenements", new Object() {
				@Override
				public String toString() {
					return String.format("numeroEntreprise=%d, code=%s, minDate=%s, maxDate=%s", numeroEntreprise, code, ServiceTracing.toString(minDate), ServiceTracing.toString(maxDate));
				}
			});
		}
		return result;
	}
}
