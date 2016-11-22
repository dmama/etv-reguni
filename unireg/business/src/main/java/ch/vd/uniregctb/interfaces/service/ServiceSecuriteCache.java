package ch.vd.uniregctb.interfaces.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import org.slf4j.Logger;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.registre.base.utils.Assert;
import ch.vd.unireg.interfaces.infra.data.CollectiviteAdministrative;
import ch.vd.unireg.interfaces.infra.data.CollectiviteAdministrativeUtilisateur;
import ch.vd.unireg.interfaces.infra.data.TypeCollectivite;
import ch.vd.uniregctb.cache.CacheHelper;
import ch.vd.uniregctb.cache.CacheStats;
import ch.vd.uniregctb.cache.EhCacheStats;
import ch.vd.uniregctb.cache.KeyDumpableCache;
import ch.vd.uniregctb.cache.KeyValueDumpableCache;
import ch.vd.uniregctb.cache.UniregCacheInterface;
import ch.vd.uniregctb.cache.UniregCacheManager;
import ch.vd.uniregctb.interfaces.service.host.Operateur;
import ch.vd.uniregctb.security.IfoSecProcedure;
import ch.vd.uniregctb.security.IfoSecProfil;
import ch.vd.uniregctb.stats.StatsService;
import ch.vd.uniregctb.utils.LogLevel;

public class ServiceSecuriteCache implements UniregCacheInterface, KeyDumpableCache, KeyValueDumpableCache, ServiceSecuriteService, InitializingBean, DisposableBean {

	private static final CacheHelper.ValueRendererFactory RENDERER_FACTORY;
	static
	{
		final CacheHelper.ValueRendererFactory factory = new CacheHelper.ValueRendererFactory();
		factory.addSpecificRenderer(CollectiviteAdministrative.class, coladm -> String.format("CollAdm{no=%d}", coladm.getNoColAdm()));
		factory.addSpecificRenderer(IfoSecProfil.class, profil -> String.format("{%s (%s %s)}", profil.getVisaOperateur(), profil.getPrenom(), profil.getNom()));
		factory.addSpecificRenderer(Operateur.class, operateur -> String.format("{%s (%s %s)}", operateur.getCode(), operateur.getPrenom(), operateur.getNom()));
		factory.addSpecificRenderer(IfoSecProcedure.class, IfoSecProcedure::getCode);
		RENDERER_FACTORY = factory;
	}


	private CacheManager cacheManager;
	private String cacheName;
	private ServiceSecuriteService target;
	private Ehcache cache;
	private UniregCacheManager uniregCacheManager;
	private StatsService statsService;

	public void setTarget(ServiceSecuriteService target) {
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

	public void setStatsService(StatsService statsService) {
		this.statsService = statsService;
	}

	@Override
	public CacheStats buildStats() {
		return new EhCacheStats(cache);
	}

	private void initCache() {
		if (cacheManager != null && cacheName != null) {
			cache = cacheManager.getCache(cacheName);
			Assert.notNull(cache);
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (statsService != null) {
			statsService.registerCache(SERVICE_NAME, this);
		}
		uniregCacheManager.register(this);
	}

	@Override
	public void destroy() throws Exception {
		if (statsService != null) {
			statsService.unregisterCache(SERVICE_NAME);
		}
		uniregCacheManager.unregister(this);
	}

	private static class KeyGetCollectivitesUtilisateurVisaOperateur {

		private final String visaOperateur;

		public KeyGetCollectivitesUtilisateurVisaOperateur(String visaOperateur) {
			this.visaOperateur = visaOperateur;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((visaOperateur == null) ? 0 : visaOperateur.hashCode());
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
			KeyGetCollectivitesUtilisateurVisaOperateur other = (KeyGetCollectivitesUtilisateurVisaOperateur) obj;
			if (visaOperateur == null) {
				if (other.visaOperateur != null)
					return false;
			}
			else if (!visaOperateur.equals(other.visaOperateur))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return "KeyGetCollectivitesUtilisateurVisaOperateur{" +
					"visaOperateur='" + visaOperateur + '\'' +
					'}';
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<CollectiviteAdministrativeUtilisateur> getCollectivitesUtilisateur(String visaOperateur) {
		final List<CollectiviteAdministrativeUtilisateur>  resultat;
		final KeyGetCollectivitesUtilisateurVisaOperateur key = new KeyGetCollectivitesUtilisateurVisaOperateur(visaOperateur);
		final Element element = cache.get(key);
		if (element == null) {
			resultat = target.getCollectivitesUtilisateur(visaOperateur);
			if (resultat != null && !resultat.isEmpty()) {      // on ne sauvegarde que les résultats non-vides
				cache.put(new Element(key, resultat));
			}
		}
		else {
			resultat = (List<CollectiviteAdministrativeUtilisateur>) element.getObjectValue();
		}

		return resultat;
	}


	private static class KeyGetProfileUtilisateurVisaOperateurCodeCollectivite {

		private final String visaOperateur;
		private final int codeCollectivite;

		public KeyGetProfileUtilisateurVisaOperateurCodeCollectivite(String visaOperateur, int codeCollectivite) {
			this.visaOperateur = visaOperateur;
			this.codeCollectivite = codeCollectivite;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + codeCollectivite;
			result = prime * result + ((visaOperateur == null) ? 0 : visaOperateur.hashCode());
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
			KeyGetProfileUtilisateurVisaOperateurCodeCollectivite other = (KeyGetProfileUtilisateurVisaOperateurCodeCollectivite) obj;
			if (codeCollectivite != other.codeCollectivite)
				return false;
			if (visaOperateur == null) {
				if (other.visaOperateur != null)
					return false;
			}
			else if (!visaOperateur.equals(other.visaOperateur))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return "KeyGetProfileUtilisateurVisaOperateurCodeCollectivite{" +
					"visaOperateur='" + visaOperateur + '\'' +
					", codeCollectivite=" + codeCollectivite +
					'}';
		}
	}

	@Override
	public IfoSecProfil getProfileUtilisateur(String visaOperateur, int codeCollectivite) {
		final IfoSecProfil  resultat;
		final KeyGetProfileUtilisateurVisaOperateurCodeCollectivite key = new KeyGetProfileUtilisateurVisaOperateurCodeCollectivite(visaOperateur, codeCollectivite);
		final Element element = cache.get(key);
		if (element == null) {
			resultat = target.getProfileUtilisateur(visaOperateur, codeCollectivite);
			if (resultat != null) {      // on ne sauvegarde que les résultats non-vides
				cache.put(new Element(key, resultat));
			}
		}
		else {
			resultat = (IfoSecProfil) element.getObjectValue();
		}

		return resultat;
	}


	private static class KeyGetUtilisateursTypesCollectivite {

		private final Set<TypeCollectivite> typesCollectivite;

		public KeyGetUtilisateursTypesCollectivite(List<TypeCollectivite> typesCollectivite) {
			this.typesCollectivite = new HashSet<>(typesCollectivite);
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((typesCollectivite == null) ? 0 : typesCollectivite.hashCode());
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
			KeyGetUtilisateursTypesCollectivite other = (KeyGetUtilisateursTypesCollectivite) obj;
			if (typesCollectivite == null) {
				if (other.typesCollectivite != null)
					return false;
			}
			else if (!typesCollectivite.equals(other.typesCollectivite))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return "KeyGetUtilisateursTypesCollectivite{" +
					"typesCollectivite=" + typesCollectivite +
					'}';
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<Operateur> getUtilisateurs(List<TypeCollectivite> typesCollectivite) {

		final  List<Operateur>  resultat;
		final KeyGetUtilisateursTypesCollectivite key = new KeyGetUtilisateursTypesCollectivite(typesCollectivite);
		final Element element = cache.get(key);
		if (element == null) {
			resultat = target.getUtilisateurs(typesCollectivite);
			if (resultat != null && !resultat.isEmpty()) {      // on ne sauvegarde que les résultats non-vides
				cache.put(new Element(key, resultat));
			}
		}
		else {
			resultat = (List<Operateur>) element.getObjectValue();
		}

		return resultat;
	}

	private static class KeyGetOperateurByNoIndividu {

		private final long noIndividu;

		public KeyGetOperateurByNoIndividu(long noIndividu) {
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
			KeyGetOperateurByNoIndividu other = (KeyGetOperateurByNoIndividu) obj;
			return noIndividu == other.noIndividu;
		}

		@Override
		public String toString() {
			return "KeyGetOperateurByNoIndividu{" +
					"noIndividu=" + noIndividu +
					'}';
		}
	}

	@Override
	public Operateur getOperateur(long individuNoTechnique) {
		final Operateur resultat;

		final KeyGetOperateurByNoIndividu key = new KeyGetOperateurByNoIndividu(individuNoTechnique);
		final Element element = cache.get(key);
		if (element == null) {
			resultat = target.getOperateur(individuNoTechnique);
			if (resultat != null) {      // on ne sauvegarde que les résultats non-vides
				cache.put(new Element(key, resultat));
			}
		}
		else {
			resultat = (Operateur) element.getObjectValue();
		}

		return resultat;
	}

	private static class KeyGetOperateurByVisa {

		private final String visa;

		public KeyGetOperateurByVisa(String visa) {
			this.visa = visa;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((visa == null) ? 0 : visa.hashCode());
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
			KeyGetOperateurByVisa other = (KeyGetOperateurByVisa) obj;
			if (visa == null) {
				if (other.visa != null)
					return false;
			}
			else if (!visa.equals(other.visa))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return "KeyGetOperateurByVisa{" +
					"visa='" + visa + '\'' +
					'}';
		}
	}

	@Override
	public Operateur getOperateur(String visa) {
		final Operateur resultat;

		final KeyGetOperateurByVisa key = new KeyGetOperateurByVisa(visa);
		final Element element = cache.get(key);
		if (element == null) {
			resultat = target.getOperateur(visa);
			if (resultat != null) {      // on ne sauvegarde que les résultats non-vides
				cache.put(new Element(key, resultat));
			}
		}
		else {
			resultat = (Operateur) element.getObjectValue();
		}

		return resultat;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getDescription() {
		return "service securité";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getName() {
		return "SECURITE";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void reset() {
		cache.removeAll();
	}

	@Override
	public void dumpCacheKeys(Logger logger, LogLevel.Level level) {
		CacheHelper.dumpCacheKeys(cache, logger, level);
	}

	@Override
	public void dumpCacheContent(Logger logger, LogLevel.Level level) {
		CacheHelper.dumpCacheKeysAndValues(cache, logger, level, RENDERER_FACTORY);
	}
}
