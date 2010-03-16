package ch.vd.uniregctb.interfaces.service;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Assert;
import ch.vd.registre.civil.model.EnumAttributeIndividu;
import ch.vd.uniregctb.cache.UniregCacheInterface;
import ch.vd.uniregctb.cache.UniregCacheManager;
import ch.vd.uniregctb.interfaces.model.EtatCivil;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.model.Nationalite;
import ch.vd.uniregctb.interfaces.model.Origine;
import ch.vd.uniregctb.interfaces.model.Permis;
import ch.vd.uniregctb.interfaces.model.Tutelle;
import ch.vd.uniregctb.stats.StatsService;

public class ServiceCivilCache extends ServiceCivilServiceBase implements UniregCacheInterface, InitializingBean, DisposableBean {

	private static final Logger LOGGER = Logger.getLogger(ServiceCivilCache.class);

	private CacheManager cacheManager;
	private String cacheName;
	private ServiceCivilService target;
	private Ehcache cache;
	private UniregCacheManager uniregCacheManager;

	public void setTarget(ServiceCivilService target) {
		this.target = target;
	}

	public void setCacheManager(CacheManager manager) {
		this.cacheManager = manager;
		initCache();
	}

	public void setCacheName(String cacheName) {
		this.cacheName = cacheName;
		initCache();
	}

	public void setUniregCacheManager(UniregCacheManager uniregCacheManager) {
		this.uniregCacheManager = uniregCacheManager;
	}

	private void initCache() {
		if (cacheManager != null && cacheName != null) {
			cache = cacheManager.getCache(cacheName);
			Assert.notNull(cache);
		}
	}

	public void afterPropertiesSet() throws Exception {
		StatsService.registerCachedService(SERVICE_NAME, cache);
		uniregCacheManager.register(this);
	}

	public void destroy() throws Exception {
		StatsService.unregisterCachedService(SERVICE_NAME);
	}

	private static abstract class KeyIndividu {

		private final long noIndividu;

		public KeyIndividu(long noIndividu) {
			this.noIndividu = noIndividu;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + (int) (noIndividu ^ (noIndividu >>> 32));
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			KeyIndividu other = (KeyIndividu) obj;
			if (noIndividu != other.noIndividu)
				return false;
			return true;
		}

	}

	private static class KeyGetEtatCivilActifNoIndividuDate extends KeyIndividu {

		private final RegDate date;

		public KeyGetEtatCivilActifNoIndividuDate(long noIndividu, RegDate date) {
			super(noIndividu);
			this.date = date;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + ((date == null) ? 0 : date.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (!super.equals(obj))
				return false;
			if (getClass() != obj.getClass())
				return false;
			KeyGetEtatCivilActifNoIndividuDate other = (KeyGetEtatCivilActifNoIndividuDate) obj;
			if (date == null) {
				if (other.date != null)
					return false;
			}
			else if (!date.equals(other.date))
				return false;
			return true;
		}
	}

	public EtatCivil getEtatCivilActif(long noIndividu, RegDate date) {

		final EtatCivil resultat;

		final KeyGetEtatCivilActifNoIndividuDate key = new KeyGetEtatCivilActifNoIndividuDate(noIndividu, date);
		final Element element = cache.get(key);
		if (element == null) {
			resultat = target.getEtatCivilActif(noIndividu, date);
			cache.put(new Element(key, resultat));
		}
		else {
			resultat = (EtatCivil) element.getObjectValue();
		}

		return resultat;
	}

	private static class KeyGetIndividuNoIndividuAnneeParties extends KeyIndividu {

		private final int annee;
		private final Set<EnumAttributeIndividu> parties;

		public KeyGetIndividuNoIndividuAnneeParties(long noIndividu, int annee, Set<EnumAttributeIndividu> parties) {
			super(noIndividu);
			this.annee = annee;
			this.parties = parties;
		}

		public KeyGetIndividuNoIndividuAnneeParties(long noIndividu, int annee, EnumAttributeIndividu[] parties) {
			super(noIndividu);
			this.annee = annee;
			if (parties == null) {
				this.parties = new HashSet<EnumAttributeIndividu>();
			}
			else {
				this.parties = new HashSet<EnumAttributeIndividu>(Arrays.asList(parties));
			}
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + annee;
			result = prime * result + ((parties == null) ? 0 : parties.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (!super.equals(obj))
				return false;
			if (getClass() != obj.getClass())
				return false;
			KeyGetIndividuNoIndividuAnneeParties other = (KeyGetIndividuNoIndividuAnneeParties) obj;
			if (annee != other.annee)
				return false;
			if (parties == null) {
				if (other.parties != null)
					return false;
			}
			else if (!parties.equals(other.parties))
				return false;
			return true;
		}
	}

	public Individu getIndividu(long noIndividu, int annee, EnumAttributeIndividu... parties) {

		final Individu resultat;

		final KeyGetIndividuNoIndividuAnneeParties key = new KeyGetIndividuNoIndividuAnneeParties(noIndividu, annee, parties);
		final Element element = cache.get(key);
		if (element == null) {
			resultat = target.getIndividu(noIndividu, annee, parties);
			cache.put(new Element(key, resultat));
		}
		else {
			resultat = (Individu) element.getObjectValue();
		}

		return resultat;
	}

	public List<Individu> getIndividus(Collection<Long> nosIndividus, RegDate date, EnumAttributeIndividu... parties) {
		// cette méthode n'est pas cachée pour l'instant. A faire si la nécessité se fait sentir...
		return target.getIndividus(nosIndividus, date, parties);
	}

	private static class KeyGetNationalitesNoIndividuAnnee extends KeyIndividu {

		private final int annee;

		public KeyGetNationalitesNoIndividuAnnee(long noIndividu, int annee) {
			super(noIndividu);
			this.annee = annee;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + annee;
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (!super.equals(obj))
				return false;
			if (getClass() != obj.getClass())
				return false;
			KeyGetNationalitesNoIndividuAnnee other = (KeyGetNationalitesNoIndividuAnnee) obj;
			if (annee != other.annee)
				return false;
			return true;
		}
	}

	@SuppressWarnings("unchecked")
	public Collection<Nationalite> getNationalites(long noIndividu, int annee) {

		final Collection<Nationalite> resultat;

		final KeyGetNationalitesNoIndividuAnnee key = new KeyGetNationalitesNoIndividuAnnee(noIndividu, annee);
		final Element element = cache.get(key);
		if (element == null) {
			resultat = target.getNationalites(noIndividu, annee);
			cache.put(new Element(key, resultat));
		}
		else {
			resultat = (Collection<Nationalite>) element.getObjectValue();
		}

		return resultat;
	}

	private static class KeyGetOrigineNoIndividuAnnee extends KeyIndividu {

		private final int annee;

		public KeyGetOrigineNoIndividuAnnee(long noIndividu, int annee) {
			super(noIndividu);
			this.annee = annee;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + annee;
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (!super.equals(obj))
				return false;
			if (getClass() != obj.getClass())
				return false;
			KeyGetOrigineNoIndividuAnnee other = (KeyGetOrigineNoIndividuAnnee) obj;
			if (annee != other.annee)
				return false;
			return true;
		}
	}

	public Origine getOrigine(long noIndividu, int annee) {

		final Origine resultat;

		final KeyGetOrigineNoIndividuAnnee key = new KeyGetOrigineNoIndividuAnnee(noIndividu, annee);
		final Element element = cache.get(key);
		if (element == null) {
			resultat = target.getOrigine(noIndividu, annee);
			cache.put(new Element(key, resultat));
		}
		else {
			resultat = (Origine) element.getObjectValue();
		}

		return resultat;
	}

	private static class KeyGetPermisNoIndividuAnnee extends KeyIndividu {

		private final int annee;

		public KeyGetPermisNoIndividuAnnee(long noIndividu, int annee) {
			super(noIndividu);
			this.annee = annee;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + annee;
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (!super.equals(obj))
				return false;
			if (getClass() != obj.getClass())
				return false;
			KeyGetPermisNoIndividuAnnee other = (KeyGetPermisNoIndividuAnnee) obj;
			if (annee != other.annee)
				return false;
			return true;
		}
	}

	@SuppressWarnings("unchecked")
	public Collection<Permis> getPermis(long noIndividu, int annee) {

		final Collection<Permis> resultat;

		final KeyGetPermisNoIndividuAnnee key = new KeyGetPermisNoIndividuAnnee(noIndividu, annee);
		final Element element = cache.get(key);
		if (element == null) {
			resultat = target.getPermis(noIndividu, annee);
			cache.put(new Element(key, resultat));
		}
		else {
			resultat = (Collection<Permis>) element.getObjectValue();
		}

		return resultat;
	}

	private static class KeyGetPermisActifNoIndividuDate extends KeyIndividu {

		private final RegDate date;

		public KeyGetPermisActifNoIndividuDate(long noIndividu, RegDate date) {
			super(noIndividu);
			this.date = date;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + ((date == null) ? 0 : date.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (!super.equals(obj))
				return false;
			if (getClass() != obj.getClass())
				return false;
			KeyGetPermisActifNoIndividuDate other = (KeyGetPermisActifNoIndividuDate) obj;
			if (date == null) {
				if (other.date != null)
					return false;
			}
			else if (!date.equals(other.date))
				return false;
			return true;
		}
	}

	public Permis getPermisActif(long noIndividu, RegDate date) {

		final Permis resultat;

		final KeyGetPermisActifNoIndividuDate key = new KeyGetPermisActifNoIndividuDate(noIndividu, date);
		final Element element = cache.get(key);
		if (element == null) {
			resultat = target.getPermisActif(noIndividu, date);
			cache.put(new Element(key, resultat));
		}
		else {
			resultat = (Permis) element.getObjectValue();
		}

		return resultat;
	}

	private static class KeyGetTutelleNoIndividuAnnee extends KeyIndividu {

		private final int annee;

		public KeyGetTutelleNoIndividuAnnee(long noIndividu, int annee) {
			super(noIndividu);
			this.annee = annee;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + annee;
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (!super.equals(obj))
				return false;
			if (getClass() != obj.getClass())
				return false;
			KeyGetTutelleNoIndividuAnnee other = (KeyGetTutelleNoIndividuAnnee) obj;
			if (annee != other.annee)
				return false;
			return true;
		}
	}

	public Tutelle getTutelle(long noIndividu, int annee) {

		final Tutelle resultat;

		final KeyGetTutelleNoIndividuAnnee key = new KeyGetTutelleNoIndividuAnnee(noIndividu, annee);
		final Element element = cache.get(key);
		if (element == null) {
			resultat = target.getTutelle(noIndividu, annee);
			cache.put(new Element(key, resultat));
		}
		else {
			resultat = (Tutelle) element.getObjectValue();
		}

		return resultat;
	}

	public void setUp(ServiceCivilService target) {
		Assert.fail("Not implemented");
	}

	public void tearDown() {
		Assert.fail("Not implemented");
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isWarmable() {
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	public void warmCache(List<Individu> individus, RegDate date, EnumAttributeIndividu... parties) {

		final int annee = (date == null ? 2400 : date.year());

		for (Individu i : individus) {
			final long noIndividu = i.getNoTechnique();
			final KeyGetIndividuNoIndividuAnneeParties key = new KeyGetIndividuNoIndividuAnneeParties(noIndividu, annee, parties);
			cache.put(new Element(key, i));
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onIndividuChange(long numero) {
		super.onIndividuChange(numero);

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Eviction des données cachées pour l'individu n° " + numero);
		}
		final List<?> keys = cache.getKeys();
		for (Object k : keys) {
			boolean remove = false;
			if (k instanceof KeyIndividu) {
				KeyIndividu ki = (KeyIndividu) k;
				remove = (ki.noIndividu == numero);
			}
			if (remove) {
				cache.remove(k);
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public String getDescription() {
		return "service civil";
	}

	/**
	 * {@inheritDoc}
	 */
	public String getName() {
		return "CIVIL";
	}

	/**
	 * {@inheritDoc}
	 */
	public void reset() {
		cache.removeAll();
	}
}
