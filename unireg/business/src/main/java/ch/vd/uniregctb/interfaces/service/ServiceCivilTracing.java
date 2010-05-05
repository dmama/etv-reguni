package ch.vd.uniregctb.interfaces.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import ch.vd.infrastructure.service.InfrastructureException;
import ch.vd.uniregctb.adresse.HistoriqueCommune;
import ch.vd.uniregctb.common.DonneesCivilesException;
import ch.vd.uniregctb.stats.StatsService;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.civil.model.EnumAttributeIndividu;
import ch.vd.uniregctb.adresse.AdressesCiviles;
import ch.vd.uniregctb.adresse.AdressesCivilesHisto;
import ch.vd.uniregctb.common.ProgrammingException;
import ch.vd.uniregctb.interfaces.model.Adresse;
import ch.vd.uniregctb.interfaces.model.EtatCivil;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.model.Nationalite;
import ch.vd.uniregctb.interfaces.model.Origine;
import ch.vd.uniregctb.interfaces.model.Permis;
import ch.vd.uniregctb.interfaces.model.Tutelle;

/**
 * Implémentation qui permet de comptabiliser le temps passé dans les appels du service.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class ServiceCivilTracing implements ServiceCivilService, InitializingBean, DisposableBean {

	private ServiceCivilService target;
	private StatsService statsService;

	private final ServiceTracing tracing = new ServiceTracing();

	public void setTarget(ServiceCivilService target) {
		this.target = target;
	}

	public void setStatsService(StatsService statsService) {
		this.statsService = statsService;
	}

	public AdressesCiviles getAdresses(long noIndividu, RegDate date, boolean strict) throws DonneesCivilesException {
		AdressesCiviles result;
		long time = tracing.start();
		try {
			result = target.getAdresses(noIndividu, date, strict);
		}
		finally {
			tracing.end(time, "getAdresses");
		}
		return result;
	}

	public Collection<Adresse> getAdresses(long noIndividu, int annee) {
		Collection<Adresse> result;
		long time = tracing.start();
		try {
			result = target.getAdresses(noIndividu, annee);
		}
		finally {
			tracing.end(time, "getAdresses");
		}

		return result;
	}

	public AdressesCivilesHisto getAdressesHisto(long noIndividu, boolean strict) throws DonneesCivilesException {
		AdressesCivilesHisto result;
		long time = tracing.start();
		try {
			result = target.getAdressesHisto(noIndividu, strict);
		}
		finally {
			tracing.end(time, "getAdressesHisto");
		}

		return result;
	}

	public EtatCivil getEtatCivilActif(long noIndividu, RegDate date) {
		EtatCivil result;
		long time = tracing.start();
		try {
			result = target.getEtatCivilActif(noIndividu, date);
		}
		finally {
			tracing.end(time, "getEtatCivilActif");
		}

		return result;
	}

	public Individu getIndividu(long noIndividu, int annee) {
		Individu result;
		long time = tracing.start();
		try {
			result = target.getIndividu(noIndividu, annee);
		}
		finally {
			tracing.end(time, "getIndividu");
		}

		return result;
	}

	public Individu getIndividu(long noIndividu, int annee, EnumAttributeIndividu... parties) {
		Individu result;
		long time = tracing.start();
		try {
			result = target.getIndividu(noIndividu, annee, parties);
		}
		finally {
			tracing.end(time, "getIndividu");
		}

		return result;
	}

	public Individu getIndividu(long noIndividu, RegDate date) {
		Individu result;
		long time = tracing.start();
		try {
			result = target.getIndividu(noIndividu, date);
		}
		finally {
			tracing.end(time, "getIndividu");
		}

		return result;
	}

	public Individu getIndividu(long noIndividu, RegDate date, EnumAttributeIndividu... parties) {
		Individu result;
		long time = tracing.start();
		try {
			result = target.getIndividu(noIndividu, date, parties);
		}
		finally {
			tracing.end(time, "getIndividu");
		}

		return result;
	}

	public Individu getConjoint(Long noIndividuPrincipal, RegDate date) {
		Individu result;
		long time = tracing.start();
		try {
			result = target.getConjoint(noIndividuPrincipal,date);
		}
		finally {
			tracing.end(time, "getConjoint");
		}

		return result;
	}

	public Long getNumeroIndividuConjoint(Long noIndividuPrincipal, RegDate date) {
		Long result;
		long time = tracing.start();
		try {
			result = target.getNumeroIndividuConjoint(noIndividuPrincipal,date);
		}
		finally {
			tracing.end(time, "getNumeroIndividuConjoint");
		}

		return result;
	}

	public List<Individu> getIndividus(Collection<Long> nosIndividus, RegDate date, EnumAttributeIndividu... parties) {
		List<Individu> result;
		long time = tracing.start();
		try {
			result = target.getIndividus(nosIndividus, date, parties);
		}
		finally {
			tracing.end(time, "getIndividus");
		}

		return result;
	}

	public List<Individu> getIndividus(Collection<Long> nosIndividus, int annee, EnumAttributeIndividu... parties) {
		List<Individu> result;
		long time = tracing.start();
		try {
			result = target.getIndividus(nosIndividus, annee, parties);
		}
		finally {
			tracing.end(time, "getIndividus");
		}

		return result;
	}

	public Collection<Nationalite> getNationalites(long noIndividu, int annee) {
		Collection<Nationalite> result;
		long time = tracing.start();
		try {
			result = target.getNationalites(noIndividu, annee);
		}
		finally {
			tracing.end(time, "getNationalites");
		}

		return result;
	}

	public Origine getOrigine(long noIndividu, int annee) {
		Origine result;
		long time = tracing.start();
		try {
			result = target.getOrigine(noIndividu, annee);
		}
		finally {
			tracing.end(time, "getOrigine");
		}

		return result;
	}

	public Collection<Permis> getPermis(long noIndividu, int annee) {
		Collection<Permis> result;
		long time = tracing.start();
		try {
			result = target.getPermis(noIndividu, annee);
		}
		finally {
			tracing.end(time, "getPermis");
		}

		return result;
	}

	public Permis getPermisActif(long noIndividu, RegDate date) {
		Permis result;
		long time = tracing.start();
		try {
			result = target.getPermisActif(noIndividu, date);
		}
		finally {
			tracing.end(time, "getPermisActif");
		}

		return result;
	}

	public Tutelle getTutelle(long noIndividu, int annee) {
		Tutelle result;
		long time = tracing.start();
		try {
			result = target.getTutelle(noIndividu, annee);
		}
		finally {
			tracing.end(time, "getTutelle");
		}

		return result;
	}

	public String getNomPrenom(Individu individu) {
		String result;
		long time = tracing.start();
		try {
			result = target.getNomPrenom(individu);
		}
		finally {
			tracing.end(time, "getNomPrenom");
		}

		return result;
	}

	public List<HistoriqueCommune> getCommunesDomicileHisto(RegDate depuis, long noIndividu, boolean strict, boolean seulementVaud) throws DonneesCivilesException, InfrastructureException {
		final List<HistoriqueCommune> result;
		final long time = tracing.start();
		try {
			result = target.getCommunesDomicileHisto(depuis, noIndividu, strict, seulementVaud);
		}
		finally {
			tracing.end(time, "getCommunesDomicileHisto");
		}
		return result;
	}

	public void setUp(ServiceCivilService target) {
		throw new ProgrammingException();
	}

	public void tearDown() {
		throw new ProgrammingException();
	}

	public boolean isWarmable() {
		long time = tracing.start();
		try {
			return target.isWarmable();
		}
		finally {
			tracing.end(time, "isWarmable");
		}
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
}
