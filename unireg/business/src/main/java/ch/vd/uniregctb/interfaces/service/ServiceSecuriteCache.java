package ch.vd.uniregctb.interfaces.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ch.vd.uniregctb.stats.StatsService;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.infrastructure.model.CollectiviteAdministrative;
import ch.vd.infrastructure.model.EnumTypeCollectivite;
import ch.vd.registre.base.utils.Assert;
import ch.vd.securite.model.Operateur;
import ch.vd.securite.model.ProfilOperateur;
import ch.vd.uniregctb.cache.UniregCacheInterface;
import ch.vd.uniregctb.cache.UniregCacheManager;

public class ServiceSecuriteCache implements UniregCacheInterface, ServiceSecuriteService, InitializingBean, DisposableBean {

	//private static final Logger LOGGER = Logger.getLogger(ServiceSecuriteCache.class);

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

	public Ehcache getEhCache() {
		return cache;
	}

	private void initCache() {
		if (cacheManager != null && cacheName != null) {
			cache = cacheManager.getCache(cacheName);
			Assert.notNull(cache);
		}
	}

	public void afterPropertiesSet() throws Exception {
		if (statsService != null) {
			statsService.registerCache(SERVICE_NAME, cache);
		}
		uniregCacheManager.register(this);
	}

	public void destroy() throws Exception {
		if (statsService != null) {
			statsService.unregisterCache(SERVICE_NAME);
		}
		uniregCacheManager.unregister(this);
	}


	public void setUp(ServiceCivilService target) {
		Assert.fail("Not implemented");
	}

	public void tearDown() {
		Assert.fail("Not implemented");
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

	}

	@SuppressWarnings("unchecked")
	public List<CollectiviteAdministrative> getCollectivitesUtilisateur(String visaOperateur) {
		final List<CollectiviteAdministrative>  resultat;
		final KeyGetCollectivitesUtilisateurVisaOperateur key = new KeyGetCollectivitesUtilisateurVisaOperateur(visaOperateur);
		final Element element = cache.get(key);
		if (element == null) {
			resultat = target.getCollectivitesUtilisateur(visaOperateur);
			cache.put(new Element(key, resultat));
		}
		else {
			resultat = (List<CollectiviteAdministrative>) element.getObjectValue();
		}

		return resultat;
	}


	private static class KeyGetListeOperateursPourFonctionCollectivite {

		private final String codeFonction;
		private final int noCollectivite;

		public KeyGetListeOperateursPourFonctionCollectivite(String codeFonction, int noCollectivite) {
			this.codeFonction = codeFonction;
			this.noCollectivite = noCollectivite;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((codeFonction == null) ? 0 : codeFonction.hashCode());
			result = prime * result + noCollectivite;
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
			KeyGetListeOperateursPourFonctionCollectivite other = (KeyGetListeOperateursPourFonctionCollectivite) obj;
			if (codeFonction == null) {
				if (other.codeFonction != null)
					return false;
			}
			else if (!codeFonction.equals(other.codeFonction))
				return false;
			return noCollectivite == other.noCollectivite;
		}
	}

	@SuppressWarnings("unchecked")
	public List<ProfilOperateur> getListeOperateursPourFonctionCollectivite(String codeFonction, int noCollectivite) {
		final List<ProfilOperateur>  resultat;
		final KeyGetListeOperateursPourFonctionCollectivite key = new KeyGetListeOperateursPourFonctionCollectivite(codeFonction, noCollectivite);
		final Element element = cache.get(key);
		if (element == null) {
			resultat = target.getListeOperateursPourFonctionCollectivite(codeFonction, noCollectivite);
			cache.put(new Element(key, resultat));
		}
		else {
			resultat = (List<ProfilOperateur>) element.getObjectValue();
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

	}

	public ProfilOperateur getProfileUtilisateur(String visaOperateur, int codeCollectivite) {
		final ProfilOperateur  resultat;
		final KeyGetProfileUtilisateurVisaOperateurCodeCollectivite key = new KeyGetProfileUtilisateurVisaOperateurCodeCollectivite(visaOperateur, codeCollectivite);
		final Element element = cache.get(key);
		if (element == null) {
			resultat = target.getProfileUtilisateur(visaOperateur, codeCollectivite);
			cache.put(new Element(key, resultat));
		}
		else {
			resultat = (ProfilOperateur) element.getObjectValue();
		}

		return resultat;
	}


	private static class KeyGetUtilisateursTypesCollectivite {

		private final Set<EnumTypeCollectivite> typesCollectivite;

		public KeyGetUtilisateursTypesCollectivite(List<EnumTypeCollectivite> typesCollectivite) {
			this.typesCollectivite = new HashSet<EnumTypeCollectivite>(typesCollectivite);
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

	}

	@SuppressWarnings("unchecked")
	public List<Operateur> getUtilisateurs(List<EnumTypeCollectivite> typesCollectivite) {

		final  List<Operateur>  resultat;
		final KeyGetUtilisateursTypesCollectivite key = new KeyGetUtilisateursTypesCollectivite(typesCollectivite);
		final Element element = cache.get(key);
		if (element == null) {
			resultat = target.getUtilisateurs(typesCollectivite);
			cache.put(new Element(key, resultat));
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

	}

	public Operateur getOperateur(long individuNoTechnique) {
		final Operateur resultat;

		final KeyGetOperateurByNoIndividu key = new KeyGetOperateurByNoIndividu(individuNoTechnique);
		final Element element = cache.get(key);
		if (element == null) {
			resultat = target.getOperateur(individuNoTechnique);
			cache.put(new Element(key, resultat));
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
	}

	public Operateur getOperateur(String visa) {
		final Operateur resultat;

		final KeyGetOperateurByVisa key = new KeyGetOperateurByVisa(visa);
		final Element element = cache.get(key);
		if (element == null) {
			resultat = target.getOperateur(visa);
			cache.put(new Element(key, resultat));
		}
		else {
			resultat = (Operateur) element.getObjectValue();
		}

		return resultat;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getDescription() {
		return "service securité";
	}

	/**
	 * {@inheritDoc}
	 */
	public String getName() {
		return "SECURITE";
	}

	/**
	 * {@inheritDoc}
	 */
	public void reset() {
		cache.removeAll();
	}
}
