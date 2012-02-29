package ch.vd.uniregctb.interfaces.service;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adresse.HistoriqueCommune;
import ch.vd.uniregctb.common.DonneesCivilesException;
import ch.vd.uniregctb.common.NomPrenom;
import ch.vd.uniregctb.interfaces.IndividuDumper;
import ch.vd.uniregctb.interfaces.model.AdressesCivilesActives;
import ch.vd.uniregctb.interfaces.model.AdressesCivilesHistoriques;
import ch.vd.uniregctb.interfaces.model.AttributeIndividu;
import ch.vd.uniregctb.interfaces.model.EtatCivil;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.model.Nationalite;
import ch.vd.uniregctb.interfaces.model.Origine;
import ch.vd.uniregctb.interfaces.model.Permis;
import ch.vd.uniregctb.interfaces.model.Tutelle;
import ch.vd.uniregctb.stats.StatsService;

/**
 * Implémentation qui permet de comptabiliser le temps passé dans les appels du service.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class ServiceCivilTracing implements ServiceCivilService, InitializingBean, DisposableBean, ServiceCivilServiceWrapper {

	private static final Logger LOGGER = Logger.getLogger(ServiceCivilTracing.class);
	
	private ServiceCivilService target;
	private StatsService statsService;

	private final ServiceTracing tracing = new ServiceTracing(SERVICE_NAME);

	public void setTarget(ServiceCivilService target) {
		this.target = target;
	}

	public void setStatsService(StatsService statsService) {
		this.statsService = statsService;
	}

	@Override
	public AdressesCivilesActives getAdresses(final long noIndividu, final RegDate date, final boolean strict) throws DonneesCivilesException {
		Throwable t = null;
		final long time = tracing.start();
		try {
			return target.getAdresses(noIndividu, date, strict);
		}
		catch (DonneesCivilesException e) {
			t = e;
			throw e;
		}
		catch (RuntimeException e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getAdresses", new Object() {
				@Override
				public String toString() {
					return String.format("noIndividu=%d, date=%s, strict=%s", noIndividu, ServiceTracing.toString(date), strict);
				}
			});
		}
	}

	@Override
	public AdressesCivilesHistoriques getAdressesHisto(final long noIndividu, final boolean strict) throws DonneesCivilesException {
		Throwable t = null;
		final long time = tracing.start();
		try {
			return target.getAdressesHisto(noIndividu, strict);
		}
		catch (DonneesCivilesException e) {
			t = e;
			throw e;
		}
		catch (RuntimeException e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getAdressesHisto", new Object() {
				@Override
				public String toString() {
					return String.format("noIndividu=%d, strict=%s", noIndividu, strict);
				}
			});
		}
	}

	@Override
	public EtatCivil getEtatCivilActif(final long noIndividu, final RegDate date) {
		Throwable t = null;
		final long time = tracing.start();
		try {
			return target.getEtatCivilActif(noIndividu, date);
		}
		catch (RuntimeException e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getEtatCivilActif", new Object() {
				@Override
				public String toString() {
					return String.format("noIndividu=%d, date=%s", noIndividu, ServiceTracing.toString(date));
				}
			});
		}
	}

	@Override
	public Individu getIndividu(final long noIndividu, final RegDate date, final AttributeIndividu... parties) {
		Throwable t = null;
		final long time = tracing.start();
		try {
			final Individu individu = target.getIndividu(noIndividu, date, parties);
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace(String.format("getIndividu(noIndividu=%d, date=%s, parties=%s) => %s", noIndividu, ServiceTracing.toString(date), ServiceTracing.toString(parties),
						IndividuDumper.dump(individu, false, false)));
			}
			return individu;
		}
		catch (RuntimeException e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getIndividu", new Object() {
				@Override
				public String toString() {
					return String.format("noIndividu=%d, date=%s, parties=%s", noIndividu, ServiceTracing.toString(date), ServiceTracing.toString(parties));
				}
			});
		}
	}

	@Override
	public Individu getConjoint(final Long noIndividuPrincipal, @Nullable final RegDate date) {
		Throwable t = null;
		final long time = tracing.start();
		try {
			final Individu individu = target.getConjoint(noIndividuPrincipal, date);
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace(String.format("getConjoint(noIndividuPrincipal=%d, date=%s) => %s", noIndividuPrincipal, ServiceTracing.toString(date), IndividuDumper.dump(individu, false, false)));
			}
			return individu;
		}
		catch (RuntimeException e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getConjoint", new Object() {
				@Override
				public String toString() {
					return String.format("noIndividuPrincipal=%d, date=%s", noIndividuPrincipal, ServiceTracing.toString(date));
				}
			});
		}
	}

	@Override
	public Long getNumeroIndividuConjoint(final Long noIndividuPrincipal, final RegDate date) {
		Throwable t = null;
		final long time = tracing.start();
		try {
			return target.getNumeroIndividuConjoint(noIndividuPrincipal, date);
		}
		catch (RuntimeException e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getNumeroIndividuConjoint", new Object() {
				@Override
				public String toString() {
					return String.format("noIndividuPrincipal=%d, date=%s", noIndividuPrincipal, ServiceTracing.toString(date));
				}
			});
		}
	}

	@Override
	public Set<Long> getNumerosIndividusConjoint(final Long noIndividuPrincipal) {
		Throwable t = null;
		final long time = tracing.start();
		try {
			return target.getNumerosIndividusConjoint(noIndividuPrincipal);
		}
		catch (RuntimeException e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getNumerosIndividusConjoint", new Object() {
				@Override
				public String toString() {
					return String.format("noIndividuPrincipal=%d", noIndividuPrincipal);
				}
			});
		}
	}

	@Override
	public List<Individu> getIndividus(final Collection<Long> nosIndividus, final RegDate date, final AttributeIndividu... parties) {
		Throwable t = null;
		final long time = tracing.start();
		try {
			final List<Individu> individus = target.getIndividus(nosIndividus, date, parties);
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace(String.format("getIndividus(nosIndividus=%s, date=%s, parties=%s) => %s", ServiceTracing.toString(nosIndividus), ServiceTracing.toString(date),
						ServiceTracing.toString(parties), IndividuDumper.dump(individus, false, false)));
			}
			return individus;
		}
		catch (RuntimeException e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getIndividus", new Object() {
				@Override
				public String toString() {
					return String.format("nosIndividus=%s, date=%s, parties=%s", ServiceTracing.toString(nosIndividus), ServiceTracing.toString(date), ServiceTracing.toString(parties));
				}
			});
		}
	}

	@Override
	public Collection<Nationalite> getNationalites(final long noIndividu, final RegDate date) {
		Throwable t = null;
		final long time = tracing.start();
		try {
			return target.getNationalites(noIndividu, date);
		}
		catch (RuntimeException e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getNationalites", new Object() {
				@Override
				public String toString() {
					return String.format("noIndividu=%d, date=%s", noIndividu, ServiceTracing.toString(date));
				}
			});
		}
	}

	@Override
	public Collection<Origine> getOrigines(final long noIndividu, final RegDate date) {
		Throwable t = null;
		final long time = tracing.start();
		try {
			return target.getOrigines(noIndividu, date);
		}
		catch (RuntimeException e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getOrigines", new Object() {
				@Override
				public String toString() {
					return String.format("noIndividu=%d, date=%s", noIndividu, ServiceTracing.toString(date));
				}
			});
		}
	}

	@Override
	public Permis getPermis(final long noIndividu, @Nullable final RegDate date) {
		Throwable t = null;
		final long time = tracing.start();
		try {
			return target.getPermis(noIndividu, date);
		}
		catch (RuntimeException e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getPermis", new Object() {
				@Override
				public String toString() {
					return String.format("noIndividu=%d, date=%s", noIndividu, ServiceTracing.toString(date));
				}
			});
		}
	}

	@Override
	public Tutelle getTutelle(final long noIndividu, final RegDate date) {
		Throwable t = null;
		final long time = tracing.start();
		try {
			return target.getTutelle(noIndividu, date);
		}
		catch (RuntimeException e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getTutelle", new Object() {
				@Override
				public String toString() {
					return String.format("noIndividu=%d, date=%s", noIndividu, ServiceTracing.toString(date));
				}
			});
		}
	}

	@Override
	public String getNomPrenom(final Individu individu) {
		Throwable t = null;
		final long time = tracing.start();
		try {
			return target.getNomPrenom(individu);
		}
		catch (RuntimeException e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getNomPrenom", new Object() {
				@Override
				public String toString() {
					return String.format("individu=%s", individu != null ? individu.getNoTechnique() : null);
				}
			});
		}
	}

	@Override
	public NomPrenom getDecompositionNomPrenom(final Individu individu) {
		Throwable t = null;
		final long time = tracing.start();
		try {
			return target.getDecompositionNomPrenom(individu);
		}
		catch (RuntimeException e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getDecompositionNomPrenom", new Object() {
				@Override
				public String toString() {
					return String.format("individu=%s", individu != null ? individu.getNoTechnique() : null);
				}
			});
		}
	}

	@Override
	public List<HistoriqueCommune> getCommunesDomicileHisto(final RegDate depuis, final long noIndividu, final boolean strict, final boolean seulementVaud) throws DonneesCivilesException, ServiceInfrastructureException {
		Throwable t = null;
		final long time = tracing.start();
		try {
			return target.getCommunesDomicileHisto(depuis, noIndividu, strict, seulementVaud);
		}
		catch (DonneesCivilesException e) {
			t = e;
			throw e;
		}
		catch (ServiceInfrastructureException e) {
			t = e;
			throw e;
		}
		catch (RuntimeException e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getCommunesDomicileHisto", new Object() {
				@Override
				public String toString() {
					return String.format("depuis=%s, noIndividu=%d, strict=%s, seulementVaud=%s", ServiceTracing.toString(depuis), noIndividu, strict, seulementVaud);
				}
			});
		}
	}

	@Override
	public boolean isWarmable() {
		Throwable t = null;
		final long time = tracing.start();
		try {
			return target.isWarmable();
		}
		catch (RuntimeException e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "isWarmable", null);
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

	@Override
	public ServiceCivilService getTarget() {
		return target;
	}

	@Override
	public ServiceCivilService getUltimateTarget() {
		if (target instanceof ServiceCivilServiceWrapper) {
			return ((ServiceCivilServiceWrapper) target).getUltimateTarget();
		}
		else {
			return target;
		}
	}
}
