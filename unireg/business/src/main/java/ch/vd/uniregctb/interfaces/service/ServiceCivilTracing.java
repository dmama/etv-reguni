package ch.vd.uniregctb.interfaces.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adresse.HistoriqueCommune;
import ch.vd.uniregctb.common.DonneesCivilesException;
import ch.vd.uniregctb.common.NomPrenom;
import ch.vd.uniregctb.interfaces.model.Adresse;
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
		AdressesCivilesActives result;
		long time = tracing.start();
		try {
			result = target.getAdresses(noIndividu, date, strict);
		}
		finally {
			tracing.end(time, "getAdresses", new Object() {
				@Override
				public String toString() {
					return String.format("noIndividu=%d, date=%s, strict=%s", noIndividu, ServiceTracing.toString(date), strict);
				}
			});
		}
		return result;
	}

	@Override
	public Collection<Adresse> getAdresses(final long noIndividu, final int annee) {
		Collection<Adresse> result;
		long time = tracing.start();
		try {
			result = target.getAdresses(noIndividu, annee);
		}
		finally {
			tracing.end(time, "getAdresses", new Object() {
				@Override
				public String toString() {
					return String.format("noIndividu=%d, annee=%d", noIndividu, annee);
				}
			});
		}

		return result;
	}

	@Override
	public AdressesCivilesHistoriques getAdressesHisto(final long noIndividu, final boolean strict) throws DonneesCivilesException {
		AdressesCivilesHistoriques result;
		long time = tracing.start();
		try {
			result = target.getAdressesHisto(noIndividu, strict);
		}
		finally {
			tracing.end(time, "getAdressesHisto", new Object() {
				@Override
				public String toString() {
					return String.format("noIndividu=%d, strict=%s", noIndividu, strict);
				}
			});
		}

		return result;
	}

	@Override
	public EtatCivil getEtatCivilActif(final long noIndividu, final RegDate date) {
		EtatCivil result;
		long time = tracing.start();
		try {
			result = target.getEtatCivilActif(noIndividu, date);
		}
		finally {
			tracing.end(time, "getEtatCivilActif", new Object() {
				@Override
				public String toString() {
					return String.format("noIndividu=%d, date=%s", noIndividu, ServiceTracing.toString(date));
				}
			});
		}

		return result;
	}

	@Override
	public Individu getIndividu(final long noIndividu, final int annee) {
		Individu result;
		long time = tracing.start();
		try {
			result = target.getIndividu(noIndividu, annee);
		}
		finally {
			tracing.end(time, "getIndividu", new Object() {
				@Override
				public String toString() {
					return String.format("noIndividu=%d, annee=%d", noIndividu, annee);
				}
			});
		}

		return result;
	}

	@Override
	public Individu getIndividu(final long noIndividu, final int annee, final AttributeIndividu... parties) {
		Individu result;
		long time = tracing.start();
		try {
			result = target.getIndividu(noIndividu, annee, parties);
		}
		finally {
			tracing.end(time, "getIndividu", new Object() {
				@Override
				public String toString() {
					return String.format("noIndividu=%d, annee=%d, parties=%s", noIndividu, annee, ServiceTracing.toString(parties));
				}
			});
		}

		return result;
	}

	@Override
	public Individu getIndividu(final long noIndividu, final RegDate date, final AttributeIndividu... parties) {
		Individu result;
		long time = tracing.start();
		try {
			result = target.getIndividu(noIndividu, date, parties);
		}
		finally {
			tracing.end(time, "getIndividu", new Object() {
				@Override
				public String toString() {
					return String.format("noIndividu=%d, date=%s, parties=%s", noIndividu, ServiceTracing.toString(date), ServiceTracing.toString(parties));
				}
			});
		}

		return result;
	}

	@Override
	public Individu getConjoint(final Long noIndividuPrincipal, final RegDate date) {
		Individu result;
		long time = tracing.start();
		try {
			result = target.getConjoint(noIndividuPrincipal,date);
		}
		finally {
			tracing.end(time, "getConjoint", new Object() {
				@Override
				public String toString() {
					return String.format("noIndividuPrincipal=%d, date=%s", noIndividuPrincipal, ServiceTracing.toString(date));
				}
			});
		}

		return result;
	}

	@Override
	public Long getNumeroIndividuConjoint(final Long noIndividuPrincipal, final RegDate date) {
		Long result;
		long time = tracing.start();
		try {
			result = target.getNumeroIndividuConjoint(noIndividuPrincipal,date);
		}
		finally {
			tracing.end(time, "getNumeroIndividuConjoint", new Object() {
				@Override
				public String toString() {
					return String.format("noIndividuPrincipal=%d, date=%s", noIndividuPrincipal, ServiceTracing.toString(date));
				}
			});
		}

		return result;
	}

	@Override
	public List<Individu> getIndividus(final Collection<Long> nosIndividus, final RegDate date, final AttributeIndividu... parties) {
		List<Individu> result;
		long time = tracing.start();
		try {
			result = target.getIndividus(nosIndividus, date, parties);
		}
		finally {
			tracing.end(time, "getIndividus", new Object() {
				@Override
				public String toString() {
					return String.format("nosIndividus=%s, date=%s, parties=%s", ServiceTracing.toString(nosIndividus), ServiceTracing.toString(date), ServiceTracing.toString(parties));
				}
			});
		}

		return result;
	}

	@Override
	public List<Individu> getIndividus(final Collection<Long> nosIndividus, final int annee, final AttributeIndividu... parties) {
		List<Individu> result;
		long time = tracing.start();
		try {
			result = target.getIndividus(nosIndividus, annee, parties);
		}
		finally {
			tracing.end(time, "getIndividus", new Object() {
				@Override
				public String toString() {
					return String.format("nosIndividus=%s, annee=%d, parties=%s", ServiceTracing.toString(nosIndividus), annee, ServiceTracing.toString(parties));
				}
			});
		}

		return result;
	}

	@Override
	public Collection<Nationalite> getNationalites(final long noIndividu, final int annee) {
		Collection<Nationalite> result;
		long time = tracing.start();
		try {
			result = target.getNationalites(noIndividu, annee);
		}
		finally {
			tracing.end(time, "getNationalites", new Object() {
				@Override
				public String toString() {
					return String.format("noIndividu=%d, annee=%d", noIndividu, annee);
				}
			});
		}

		return result;
	}

	@Override
	public Origine getOrigine(final long noIndividu, final int annee) {
		Origine result;
		long time = tracing.start();
		try {
			result = target.getOrigine(noIndividu, annee);
		}
		finally {
			tracing.end(time, "getOrigine", new Object() {
				@Override
				public String toString() {
					return String.format("noIndividu=%d, annee=%d", noIndividu, annee);
				}
			});
		}

		return result;
	}

	@Override
	public Collection<Permis> getPermis(final long noIndividu, final int annee) {
		Collection<Permis> result;
		long time = tracing.start();
		try {
			result = target.getPermis(noIndividu, annee);
		}
		finally {
			tracing.end(time, "getPermis", new Object() {
				@Override
				public String toString() {
					return String.format("noIndividu=%d, annee=%d", noIndividu, annee);
				}
			});
		}

		return result;
	}

	@Override
	public Permis getPermisActif(final long noIndividu, final RegDate date) {
		Permis result;
		long time = tracing.start();
		try {
			result = target.getPermisActif(noIndividu, date);
		}
		finally {
			tracing.end(time, "getPermisActif", new Object() {
				@Override
				public String toString() {
					return String.format("noIndividu=%d, date=%s", noIndividu, ServiceTracing.toString(date));
				}
			});
		}

		return result;
	}

	@Override
	public Tutelle getTutelle(final long noIndividu, final int annee) {
		Tutelle result;
		long time = tracing.start();
		try {
			result = target.getTutelle(noIndividu, annee);
		}
		finally {
			tracing.end(time, "getTutelle", new Object() {
				@Override
				public String toString() {
					return String.format("noIndividu=%d, annee=%d", noIndividu, annee);
				}
			});
		}

		return result;
	}

	@Override
	public String getNomPrenom(final Individu individu) {
		String result;
		long time = tracing.start();
		try {
			result = target.getNomPrenom(individu);
		}
		finally {
			tracing.end(time, "getNomPrenom", new Object() {
				@Override
				public String toString() {
					return String.format("individu=%s", individu != null ? individu.getNoTechnique() : null);
				}
			});
		}

		return result;
	}

	@Override
	public NomPrenom getDecompositionNomPrenom(final Individu individu) {
		NomPrenom result;
		long time = tracing.start();
		try {
			result = target.getDecompositionNomPrenom(individu);
		}
		finally {
			tracing.end(time, "getDecompositionNomPrenom", new Object() {
				@Override
				public String toString() {
					return String.format("individu=%s", individu != null ? individu.getNoTechnique() : null);
				}
			});
		}
		return result;
	}

	@Override
	public List<HistoriqueCommune> getCommunesDomicileHisto(final RegDate depuis, final long noIndividu, final boolean strict, final boolean seulementVaud) throws DonneesCivilesException, ServiceInfrastructureException {
		final List<HistoriqueCommune> result;
		final long time = tracing.start();
		try {
			result = target.getCommunesDomicileHisto(depuis, noIndividu, strict, seulementVaud);
		}
		finally {
			tracing.end(time, "getCommunesDomicileHisto", new Object() {
				@Override
				public String toString() {
					return String.format("depuis=%s, noIndividu=%d, strict=%s, seulementVaud=%s", ServiceTracing.toString(depuis), noIndividu, strict, seulementVaud);
				}
			});
		}
		return result;
	}

	@Override
	public boolean isWarmable() {
		long time = tracing.start();
		try {
			return target.isWarmable();
		}
		finally {
			tracing.end(time, "isWarmable", null);
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

	public Map<String, ? extends ServiceTracingInterface> getDetailedData() {
		return null;
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
