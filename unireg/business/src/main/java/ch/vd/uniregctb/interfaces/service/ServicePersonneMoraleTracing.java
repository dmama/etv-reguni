package ch.vd.uniregctb.interfaces.service;

import java.util.List;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adresse.AdressesPM;
import ch.vd.uniregctb.adresse.AdressesPMHisto;
import ch.vd.uniregctb.interfaces.model.Etablissement;
import ch.vd.uniregctb.interfaces.model.EvenementPM;
import ch.vd.uniregctb.interfaces.model.PartPM;
import ch.vd.uniregctb.interfaces.model.PersonneMorale;
import ch.vd.uniregctb.stats.ServiceTracing;
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

	@Override
	public List<Long> getAllIds() {
		Throwable t = null;
		final long time = tracing.start();
		try {
			return target.getAllIds();
		}
		catch (RuntimeException e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getAllIds", null);
		}
	}

	@Override
	public PersonneMorale getPersonneMorale(final Long id, final PartPM... parts) {
		Throwable t = null;
		final long time = tracing.start();
		try {
			return target.getPersonneMorale(id, parts);
		}
		catch (RuntimeException e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getPersonneMorale", new Object() {
				@Override
				public String toString() {
					return String.format("id=%d, parts=%s", id, ServiceTracing.toString(parts));
				}
			});
		}
	}

	@Override
	public List<PersonneMorale> getPersonnesMorales(final List<Long> ids, final PartPM... parts) {
		Throwable t = null;
		int items = 0;
		final long time = tracing.start();
		try {
			final List<PersonneMorale> list = target.getPersonnesMorales(ids, parts);
			items = list == null ? 0 : list.size();
			return list;
		}
		catch (RuntimeException e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getPersonnesMorales", items, new Object() {
				@Override
				public String toString() {
					return String.format("ids=%s, parts=%s", ServiceTracing.toString(ids), ServiceTracing.toString(parts));
				}
			});
		}
	}

	@Override
	public Etablissement getEtablissement(final long id) {
		Throwable t = null;
		final long time = tracing.start();
		try {
			return target.getEtablissement(id);
		}
		catch (RuntimeException e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getEtablissement", new Object() {
				@Override
				public String toString() {
					return String.format("id=%d", id);
				}
			});
		}
	}

	@Override
	public List<Etablissement> getEtablissements(final List<Long> ids) {
		Throwable t = null;
		int items = 0;
		final long time = tracing.start();
		try {
			final List<Etablissement> list = target.getEtablissements(ids);
			items = list == null ? 0 : list.size();
			return list;
		}
		catch (RuntimeException e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getEtablissements", items, new Object() {
				@Override
				public String toString() {
					return String.format("ids=%s", ServiceTracing.toString(ids));
				}
			});
		}
	}

	@Override
	public AdressesPM getAdresses(final long noEntreprise, final RegDate date) {
		Throwable t = null;
		final long time = tracing.start();
		try {
			return target.getAdresses(noEntreprise, date);
		}
		catch (RuntimeException e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getAdresses", new Object() {
				@Override
				public String toString() {
					return String.format("noEntreprise=%d, date=%s", noEntreprise, ServiceTracing.toString(date));
				}
			});
		}
	}

	@Override
	public AdressesPMHisto getAdressesHisto(final long noEntreprise) {
		Throwable t = null;
		final long time = tracing.start();
		try {
			return target.getAdressesHisto(noEntreprise);
		}
		catch (RuntimeException e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getAdressesHisto", new Object() {
				@Override
				public String toString() {
					return String.format("noEntreprise=%d", noEntreprise);
				}
			});
		}
	}

	@Override
	public List<EvenementPM> findEvenements(final long numeroEntreprise, final String code, final RegDate minDate, final RegDate maxDate) {
		Throwable t = null;
		int items = 0;
		final long time = tracing.start();
		try {
			final List<EvenementPM> list = target.findEvenements(numeroEntreprise, code, minDate, maxDate);
			items = list == null ? 0 : list.size();
			return list;
		}
		catch (RuntimeException e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "findEvenements", items, new Object() {
				@Override
				public String toString() {
					return String.format("numeroEntreprise=%d, code=%s, minDate=%s, maxDate=%s", numeroEntreprise, code, ServiceTracing.toString(minDate), ServiceTracing.toString(maxDate));
				}
			});
		}
	}
}
