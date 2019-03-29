package ch.vd.unireg.interfaces.service;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.unireg.cache.CacheHelper;
import ch.vd.unireg.cache.CacheStats;
import ch.vd.unireg.cache.EhCacheStats;
import ch.vd.unireg.cache.KeyDumpableCache;
import ch.vd.unireg.cache.KeyValueDumpableCache;
import ch.vd.unireg.cache.UniregCacheInterface;
import ch.vd.unireg.cache.UniregCacheManager;
import ch.vd.unireg.interfaces.infra.data.CollectiviteAdministrative;
import ch.vd.unireg.interfaces.infra.data.TypeCollectivite;
import ch.vd.unireg.interfaces.service.host.Operateur;
import ch.vd.unireg.security.ProcedureSecurite;
import ch.vd.unireg.security.ProfileOperateur;
import ch.vd.unireg.stats.StatsService;
import ch.vd.unireg.utils.LogLevel;

public class ServiceSecuriteCache implements UniregCacheInterface, KeyDumpableCache, KeyValueDumpableCache, ServiceSecuriteService, InitializingBean, DisposableBean {

	private static final CacheHelper.ValueRendererFactory RENDERER_FACTORY;
	static
	{
		final CacheHelper.ValueRendererFactory factory = new CacheHelper.ValueRendererFactory();
		factory.addSpecificRenderer(CollectiviteAdministrative.class, coladm -> String.format("CollAdm{no=%d}", coladm.getNoColAdm()));
		factory.addSpecificRenderer(ProfileOperateur.class, profil -> String.format("{%s (%s %s)}", profil.getVisaOperateur(), profil.getPrenom(), profil.getNom()));
		factory.addSpecificRenderer(Operateur.class, operateur -> String.format("{%s (%s %s)}", operateur.getCode(), operateur.getPrenom(), operateur.getNom()));
		factory.addSpecificRenderer(ProcedureSecurite.class, ProcedureSecurite::getCode);
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
			if (cache == null) {
				throw new IllegalArgumentException("Le cache avec le nom [" + cacheName + "] est inconnu.");
			}
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

	@NotNull
	@Override
	@SuppressWarnings("unchecked")
	public List<CollectiviteAdministrative> getCollectivitesUtilisateur(String visaOperateur) throws ServiceSecuriteException {
		final List<CollectiviteAdministrative>  resultat;
		final KeyGetCollectivitesUtilisateurVisaOperateur key = new KeyGetCollectivitesUtilisateurVisaOperateur(visaOperateur);
		final Element element = cache.get(key);
		if (element == null) {
			resultat = target.getCollectivitesUtilisateur(visaOperateur);
			if (resultat != null && !resultat.isEmpty()) {      // on ne sauvegarde que les résultats non-vides
				cache.put(new Element(key, resultat));
			}
		}
		else {
			resultat = (List<CollectiviteAdministrative>) element.getObjectValue();
		}

		return resultat;
	}

	private static class KeyGetCollectiviteParDefaut {
		@NotNull
		private final String visaOperateur;

		public KeyGetCollectiviteParDefaut(@NotNull String visaOperateur) {
			this.visaOperateur = visaOperateur;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			final KeyGetCollectiviteParDefaut that = (KeyGetCollectiviteParDefaut) o;
			return Objects.equals(visaOperateur, that.visaOperateur);
		}

		@Override
		public int hashCode() {
			return Objects.hash(visaOperateur);
		}
	}

	@Nullable
	@Override
	public Integer getCollectiviteParDefaut(@NotNull String visaOperateur) throws ServiceSecuriteException {
		final Integer resultat;
		final KeyGetCollectiviteParDefaut key = new KeyGetCollectiviteParDefaut(visaOperateur);
		final Element element = cache.get(key);
		if (element == null) {
			resultat = target.getCollectiviteParDefaut(visaOperateur);
			cache.put(new Element(key, resultat));
		}
		else {
			resultat = (Integer) element.getObjectValue();
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

	@Nullable
	@Override
	public ProfileOperateur getProfileUtilisateur(String visaOperateur, int codeCollectivite) throws ServiceSecuriteException {
		final ProfileOperateur resultat;
		final KeyGetProfileUtilisateurVisaOperateurCodeCollectivite key = new KeyGetProfileUtilisateurVisaOperateurCodeCollectivite(visaOperateur, codeCollectivite);
		final Element element = cache.get(key);
		if (element == null) {
			resultat = target.getProfileUtilisateur(visaOperateur, codeCollectivite);
			if (resultat != null) {      // on ne sauvegarde que les résultats non-vides
				cache.put(new Element(key, resultat));
			}
		}
		else {
			resultat = (ProfileOperateur) element.getObjectValue();
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

	@NotNull
	@Override
	@SuppressWarnings("unchecked")
	public List<Operateur> getUtilisateurs(List<TypeCollectivite> typesCollectivite) throws ServiceSecuriteException {

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

	@Nullable
	@Override
	public Operateur getOperateur(@NotNull String visa) throws ServiceSecuriteException {
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
