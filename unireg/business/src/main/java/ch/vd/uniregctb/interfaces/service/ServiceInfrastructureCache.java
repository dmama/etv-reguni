package ch.vd.uniregctb.interfaces.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.infrastructure.model.EnumTypeCollectivite;
import ch.vd.infrastructure.service.InfrastructureException;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.cache.CacheStats;
import ch.vd.uniregctb.cache.EhCacheStats;
import ch.vd.uniregctb.cache.UniregCacheInterface;
import ch.vd.uniregctb.cache.UniregCacheManager;
import ch.vd.uniregctb.interfaces.model.ApplicationFiscale;
import ch.vd.uniregctb.interfaces.model.Canton;
import ch.vd.uniregctb.interfaces.model.CollectiviteAdministrative;
import ch.vd.uniregctb.interfaces.model.Commune;
import ch.vd.uniregctb.interfaces.model.InstitutionFinanciere;
import ch.vd.uniregctb.interfaces.model.Localite;
import ch.vd.uniregctb.interfaces.model.Logiciel;
import ch.vd.uniregctb.interfaces.model.OfficeImpot;
import ch.vd.uniregctb.interfaces.model.Pays;
import ch.vd.uniregctb.interfaces.model.Rue;
import ch.vd.uniregctb.interfaces.model.TypeEtatPM;
import ch.vd.uniregctb.interfaces.model.TypeRegimeFiscal;
import ch.vd.uniregctb.stats.StatsService;

public class ServiceInfrastructureCache extends ServiceInfrastructureBase implements UniregCacheInterface, InitializingBean, DisposableBean {

	//private static final Logger LOGGER = Logger.getLogger(ServiceInfrastructureCache.class);

	private CacheManager cacheManager;
	private String cacheName;
	private ServiceInfrastructureService target;
	private Ehcache cache;
	private UniregCacheManager uniregCacheManager;
	private StatsService statsService;

	public void setTarget(ServiceInfrastructureService target) {
		this.target = target;
	}

	public void setCacheManager(CacheManager manager) {
		this.cacheManager = manager;
		initCache();
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setCacheName(String cacheName) {
		this.cacheName = cacheName;
		initCache();
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setUniregCacheManager(UniregCacheManager uniregCacheManager) {
		this.uniregCacheManager = uniregCacheManager;
	}

	public void setStatsService(StatsService statsService) {
		this.statsService = statsService;
	}

	public CacheStats buildStats() {
		return new EhCacheStats(cache);
	}

	private void initCache() {
		if (cacheManager != null && cacheName != null) {
			cache = cacheManager.getCache(cacheName);
			Assert.notNull(cache);
		}
	}

	public void afterPropertiesSet() throws Exception {
		if (statsService != null) {
			statsService.registerCache(SERVICE_NAME, this);
		}
		uniregCacheManager.register(this);
	}

	public void destroy() throws Exception {
		if (statsService != null) {
			statsService.unregisterCache(SERVICE_NAME);
		}
		uniregCacheManager.unregister(this);
	}

	private static class KeyGetAllCantons {

		@Override
		public int hashCode() {
			return 192378437;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			return getClass() == obj.getClass();
		}
	}

	@SuppressWarnings("unchecked")
	public List<Canton> getAllCantons() throws InfrastructureException {
		final List<Canton> resultat;

		final KeyGetAllCantons key = new KeyGetAllCantons();
		final Element element = cache.get(key);
		if (element == null) {
			resultat = target.getAllCantons();
			cache.put(new Element(key, resultat));
		}
		else {
			resultat = (List<Canton>) element.getObjectValue();
		}

		return resultat;
	}

	private static class KeyGetCanton {
		int cantonOFS;

		private KeyGetCanton(int cantonOFS) {
			this.cantonOFS = cantonOFS;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + cantonOFS;
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
			KeyGetCanton other = (KeyGetCanton) obj;
			return cantonOFS == other.cantonOFS;
		}
	}

	@Override
	public Canton getCanton(int cantonOFS) throws InfrastructureException {
		final Canton resultat;

		final KeyGetCanton key = new KeyGetCanton(cantonOFS);
		final Element element = cache.get(key);
		if (element == null) {
			resultat = target.getCanton(cantonOFS);
			cache.put(new Element(key, resultat));
		}
		else {
			resultat = (Canton) element.getObjectValue();
		}

		return resultat;
	}

	private static class KeyGetCantonByCommune {
		int noOfsCommune;

		private KeyGetCantonByCommune(int noOfsCommune) {
			this.noOfsCommune = noOfsCommune;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + noOfsCommune;
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
			KeyGetCantonByCommune other = (KeyGetCantonByCommune) obj;
			return noOfsCommune == other.noOfsCommune;
		}
	}

	@Override
	public Canton getCantonByCommune(int noOfsCommune) throws InfrastructureException {
		final Canton resultat;

		final KeyGetCantonByCommune key = new KeyGetCantonByCommune(noOfsCommune);
		final Element element = cache.get(key);
		if (element == null) {
			resultat = target.getCantonByCommune(noOfsCommune);
			cache.put(new Element(key, resultat));
		}
		else {
			resultat = (Canton) element.getObjectValue();
		}

		return resultat;
	}

	private static class KeyGetCantonBySigle {
		String sigle;

		private KeyGetCantonBySigle(String sigle) {
			this.sigle = sigle;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((sigle == null) ? 0 : sigle.hashCode());
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
			KeyGetCantonBySigle other = (KeyGetCantonBySigle) obj;
			if (sigle == null) {
				if (other.sigle != null)
					return false;
			}
			else if (!sigle.equals(other.sigle))
				return false;
			return true;
		}
	}

	@Override
	public Canton getCantonBySigle(String sigle) throws InfrastructureException {
		final Canton resultat;

		final KeyGetCantonBySigle key = new KeyGetCantonBySigle(sigle);
		final Element element = cache.get(key);
		if (element == null) {
			resultat = target.getCantonBySigle(sigle);
			cache.put(new Element(key, resultat));
		}
		else {
			resultat = (Canton) element.getObjectValue();
		}

		return resultat;
	}

	private static class KeyGetCollectivite {
		int noColAdm;

		private KeyGetCollectivite(int noColAdm) {
			this.noColAdm = noColAdm;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + noColAdm;
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
			KeyGetCollectivite other = (KeyGetCollectivite) obj;
			if (noColAdm == other.noColAdm) {
				return true;
			}
			return false;
		}

	}

	public CollectiviteAdministrative getCollectivite(int noColAdm) throws InfrastructureException {
		final CollectiviteAdministrative resultat;

		final KeyGetCollectivite key = new KeyGetCollectivite(noColAdm);
		final Element element = cache.get(key);
		if (element == null) {
			resultat = target.getCollectivite(noColAdm);
			cache.put(new Element(key, resultat));
		}
		else {
			resultat = (CollectiviteAdministrative) element.getObjectValue();
		}

		return resultat;
	}

	private static class KeyGetCollectivitesAdministratives {

		@Override
		public int hashCode() {
			return 463528445;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			return getClass() == obj.getClass();
		}
	}

	@SuppressWarnings("unchecked")
	public List<CollectiviteAdministrative> getCollectivitesAdministratives() throws InfrastructureException {
		final List<CollectiviteAdministrative> resultat;

		final KeyGetCollectivitesAdministratives key = new KeyGetCollectivitesAdministratives();
		final Element element = cache.get(key);
		if (element == null) {
			resultat = target.getCollectivitesAdministratives();
			cache.put(new Element(key, resultat));
		}
		else {
			resultat = (List<CollectiviteAdministrative>) element.getObjectValue();
		}

		return resultat;
	}

	private static class KeyGetCollectivitesAdministrativesByTypes {

		Set<EnumTypeCollectivite> types;

		public KeyGetCollectivitesAdministrativesByTypes(List<EnumTypeCollectivite> types) {
			this.types = new HashSet<EnumTypeCollectivite>(types);
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((types == null) ? 0 : types.hashCode());
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
			KeyGetCollectivitesAdministrativesByTypes other = (KeyGetCollectivitesAdministrativesByTypes) obj;
			if (types == null) {
				if (other.types != null)
					return false;
			}
			else if (!types.equals(other.types))
				return false;
			return true;
		}

	}

	@SuppressWarnings("unchecked")
	public List<CollectiviteAdministrative> getCollectivitesAdministratives(List<EnumTypeCollectivite> typesCollectivite) throws InfrastructureException {
		final List<CollectiviteAdministrative> resultat;

		final KeyGetCollectivitesAdministrativesByTypes key = new KeyGetCollectivitesAdministrativesByTypes(typesCollectivite);
		final Element element = cache.get(key);
		if (element == null) {
			resultat = target.getCollectivitesAdministratives(typesCollectivite);
			cache.put(new Element(key, resultat));
		}
		else {
			resultat = (List<CollectiviteAdministrative>) element.getObjectValue();
		}

		return resultat;
	}

	private static class KeyGetCommuneByLocalite {
		int numOrdre; // nécessaire pour distinguer les fractions de communes
		int noCommune;

		public KeyGetCommuneByLocalite(Localite localite) {
			this.numOrdre = localite.getNoOrdre();
			this.noCommune = localite.getNoCommune();
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + noCommune;
			result = prime * result + numOrdre;
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
			KeyGetCommuneByLocalite other = (KeyGetCommuneByLocalite) obj;
			if (noCommune != other.noCommune)
				return false;
			return numOrdre == other.numOrdre;
		}

	}

	public Commune getCommuneByLocalite(Localite localite) throws InfrastructureException {
		final Commune resultat;

		final KeyGetCommuneByLocalite key = new KeyGetCommuneByLocalite(localite);
		final Element element = cache.get(key);
		if (element == null) {
			resultat = target.getCommuneByLocalite(localite);
			cache.put(new Element(key, resultat));
		}
		else {
			resultat = (Commune) element.getObjectValue();
		}

		return resultat;
	}

	private static class KeyGetCommunesByNumeroOfsEtendu {
		int noCommune;

		private KeyGetCommunesByNumeroOfsEtendu(int noCommune) {
			this.noCommune = noCommune;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + noCommune;
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
			final KeyGetCommunesByNumeroOfsEtendu other = (KeyGetCommunesByNumeroOfsEtendu) obj;
			return noCommune == other.noCommune;
		}

	}

	@SuppressWarnings({"unchecked"})
	public Commune getCommuneByNumeroOfsEtendu(int noCommune, RegDate date) throws InfrastructureException {

		final KeyGetCommunesByNumeroOfsEtendu key = new KeyGetCommunesByNumeroOfsEtendu(noCommune);
		final Element element = cache.get(key);

		final List<Commune> candidats;
		if (element == null) {
			candidats = buildListOfCommunesByNumeroOfsEtendu(key);
		}
		else {
			candidats = (List<Commune>) element.getObjectValue();
		}

		return choisirCommune(candidats, date);
	}

	@SuppressWarnings({"unchecked"})
	private synchronized List<Commune> buildListOfCommunesByNumeroOfsEtendu(KeyGetCommunesByNumeroOfsEtendu key) throws InfrastructureException {
		Element elt = cache.get(key);
		if (elt == null) {

			List<Commune> results = new ArrayList<Commune>(2);
			final int noCommune = key.noCommune;
			final List<Commune> communes = getCommunes();
			for (Commune commune : communes) {
				if (commune.getNoOFSEtendu() == noCommune) {
					results.add(commune);
				}
			}
			results = results.size() > 0 ? results : Collections.<Commune>emptyList();
			cache.put(new Element(key, results));

			return results;
		}
		else {
			return (List<Commune>) elt.getObjectValue();
		}
	}

	private static class KeyGetCommunesDeVaud {

		@Override
		public int hashCode() {
			return 276594334;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			return getClass() == obj.getClass();
		}
	}

	@SuppressWarnings("unchecked")
	public List<Commune> getCommunesDeVaud() throws InfrastructureException {
		final List<Commune> resultat;

		final KeyGetCommunesDeVaud key = new KeyGetCommunesDeVaud();
		final Element element = cache.get(key);
		if (element == null) {
			resultat = target.getCommunesDeVaud();
			cache.put(new Element(key, resultat));
		}
		else {
			resultat = (List<Commune>) element.getObjectValue();
		}

		return resultat;
	}

	private static class KeyGetCommunesHorsCanton {

		@Override
		public int hashCode() {
			return 45764637;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			return getClass() == obj.getClass();
		}
	}

	@SuppressWarnings("unchecked")
	public List<Commune> getCommunesHorsCanton() throws InfrastructureException {
		final List<Commune> resultat;

		final KeyGetCommunesHorsCanton key = new KeyGetCommunesHorsCanton();
		final Element element = cache.get(key);
		if (element == null) {
			resultat = target.getCommunesHorsCanton();
			cache.put(new Element(key, resultat));
		}
		else {
			resultat = (List<Commune>) element.getObjectValue();
		}

		return resultat;
	}

	private static class KeyGetCommunes {

		@Override
		public int hashCode() {
			return 45764638;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			return getClass() == obj.getClass();
		}
	}

	@SuppressWarnings("unchecked")
	public List<Commune> getCommunes() throws InfrastructureException {
		final List<Commune> resultat;

		final KeyGetCommunes key = new KeyGetCommunes();
		final Element element = cache.get(key);
		if (element == null) {
			resultat = target.getCommunes();
			cache.put(new Element(key, resultat));
		}
		else {
			resultat = (List<Commune>) element.getObjectValue();
		}

		return resultat;
	}

	private static class KeyGetListeCommunes {
		int noOfsCanton;

		public KeyGetListeCommunes(Canton canton) {
			this.noOfsCanton = canton.getNoOFS();
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + noOfsCanton;
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
			KeyGetListeCommunes other = (KeyGetListeCommunes) obj;
			return noOfsCanton == other.noOfsCanton;
		}

	}

	@SuppressWarnings("unchecked")
	public List<Commune> getListeCommunes(Canton canton) throws InfrastructureException {
		final List<Commune> resultat;

		final KeyGetListeCommunes key = new KeyGetListeCommunes(canton);
		final Element element = cache.get(key);
		if (element == null) {
			resultat = target.getListeCommunes(canton);
			cache.put(new Element(key, resultat));
		}
		else {
			resultat = (List<Commune>) element.getObjectValue();
		}

		return resultat;
	}


	private static class KeyGetListeFractionsCommunes {

		@Override
		public int hashCode() {
			return 57372894;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			return getClass() == obj.getClass();
		}
	}

	@SuppressWarnings("unchecked")
	public List<Commune> getListeFractionsCommunes() throws InfrastructureException {
		final List<Commune> resultat;

		final KeyGetListeFractionsCommunes key = new KeyGetListeFractionsCommunes();
		final Element element = cache.get(key);
		if (element == null) {
			resultat = target.getListeFractionsCommunes();
			cache.put(new Element(key, resultat));
		}
		else {
			resultat = (List<Commune>) element.getObjectValue();
		}

		return resultat;
	}


	private static class KeyGetLocaliteByONRP {
		int onrp;

		private KeyGetLocaliteByONRP(int onrp) {
			this.onrp = onrp;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + onrp;
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
			KeyGetLocaliteByONRP other = (KeyGetLocaliteByONRP) obj;
			return onrp == other.onrp;
		}

	}

	public Localite getLocaliteByONRP(int onrp) throws InfrastructureException {
		final Localite resultat;

		final KeyGetLocaliteByONRP key = new KeyGetLocaliteByONRP(onrp);
		final Element element = cache.get(key);
		if (element == null) {
			resultat = target.getLocaliteByONRP(onrp);
			cache.put(new Element(key, resultat));
		}
		else {
			resultat = (Localite) element.getObjectValue();
		}

		return resultat;
	}

	private static class KeyGetLocalites {

		@Override
		public int hashCode() {
			return 44535622;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			return getClass() == obj.getClass();
		}
	}

	@SuppressWarnings("unchecked")
	public List<Localite> getLocalites() throws InfrastructureException {
		final List<Localite> resultat;

		final KeyGetLocalites key = new KeyGetLocalites();
		final Element element = cache.get(key);
		if (element == null) {
			resultat = target.getLocalites();
			cache.put(new Element(key, resultat));
		}
		else {
			resultat = (List<Localite>) element.getObjectValue();
		}

		return resultat;
	}

	private static class KeyGetOfficeImpotDeCommune {
		int noCommune;

		private KeyGetOfficeImpotDeCommune(int noCommune) {
			this.noCommune = noCommune;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + noCommune;
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
			KeyGetOfficeImpotDeCommune other = (KeyGetOfficeImpotDeCommune) obj;
			return noCommune == other.noCommune;
		}
	}

	public OfficeImpot getOfficeImpotDeCommune(int noCommune) throws InfrastructureException {
		final OfficeImpot resultat;

		final KeyGetOfficeImpotDeCommune key = new KeyGetOfficeImpotDeCommune(noCommune);
		final Element element = cache.get(key);
		if (element == null) {
			resultat = target.getOfficeImpotDeCommune(noCommune);
			cache.put(new Element(key, resultat));
		}
		else {
			resultat = (OfficeImpot) element.getObjectValue();
		}

		return resultat;
	}

	private static class KeyGetOfficeImpot {
		int noColAdm;

		private KeyGetOfficeImpot(int noColAdm) {
			this.noColAdm = noColAdm;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + noColAdm;
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
			KeyGetOfficeImpot other = (KeyGetOfficeImpot) obj;
			return noColAdm == other.noColAdm;
		}
	}

	public OfficeImpot getOfficeImpot(int noColAdm) throws InfrastructureException {
		final OfficeImpot resultat;

		final KeyGetOfficeImpot key = new KeyGetOfficeImpot(noColAdm);
		final Element element = cache.get(key);
		if (element == null) {
			resultat = target.getOfficeImpot(noColAdm);
			cache.put(new Element(key, resultat));
		}
		else {
			resultat = (OfficeImpot) element.getObjectValue();
		}

		return resultat;
	}

	private static class KeyGetOfficesImpot {

		@Override
		public int hashCode() {
			return 464762920;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			return getClass() == obj.getClass();
		}
	}

	@SuppressWarnings("unchecked")
	public List<OfficeImpot> getOfficesImpot() throws InfrastructureException {
		final List<OfficeImpot> resultat;

		final KeyGetOfficesImpot key = new KeyGetOfficesImpot();
		final Element element = cache.get(key);
		if (element == null) {
			resultat = target.getOfficesImpot();
			cache.put(new Element(key, resultat));
		}
		else {
			resultat = (List<OfficeImpot>) element.getObjectValue();
		}

		return resultat;
	}

	private static class KeyGetPays {

		@Override
		public int hashCode() {
			return 57489228;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			return getClass() == obj.getClass();
		}
	}

	@SuppressWarnings("unchecked")
	public List<Pays> getPays() throws InfrastructureException {
		final List<Pays> resultat;

		final KeyGetPays key = new KeyGetPays();
		final Element element = cache.get(key);
		if (element == null) {
			resultat = target.getPays();
			cache.put(new Element(key, resultat));
		}
		else {
			resultat = (List<Pays>) element.getObjectValue();
		}

		return resultat;
	}

	private static class KeyGetPaysByNumeroOfs {
		int numeroOFS;

		private KeyGetPaysByNumeroOfs(int numeroOFS) {
			this.numeroOFS = numeroOFS;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + numeroOFS;
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
			KeyGetPaysByNumeroOfs other = (KeyGetPaysByNumeroOfs) obj;
			return numeroOFS == other.numeroOFS;
		}
	}

	@Override
	public Pays getPays(int numeroOFS) throws InfrastructureException {
		final Pays resultat;

		final KeyGetPaysByNumeroOfs key = new KeyGetPaysByNumeroOfs(numeroOFS);
		final Element element = cache.get(key);
		if (element == null) {
			resultat = target.getPays(numeroOFS);
			cache.put(new Element(key, resultat));
		}
		else {
			resultat = (Pays) element.getObjectValue();
		}

		return resultat;
	}

	private static class KeyGetPaysByCode {
		final String codePays;

		private KeyGetPaysByCode(String codePays) {
			this.codePays = codePays;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((codePays == null) ? 0 : codePays.hashCode());
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
			KeyGetPaysByCode other = (KeyGetPaysByCode) obj;
			if (codePays == null) {
				if (other.codePays != null)
					return false;
			}
			else if (!codePays.equals(other.codePays))
				return false;
			return true;
		}
	}

	@Override
	public Pays getPays(String codePays) throws InfrastructureException {
		final Pays resultat;

		final KeyGetPaysByCode key = new KeyGetPaysByCode(codePays);
		final Element element = cache.get(key);
		if (element == null) {
			resultat = target.getPays(codePays);
			cache.put(new Element(key, resultat));
		}
		else {
			resultat = (Pays) element.getObjectValue();
		}

		return resultat;
	}

	private static class KeyGetPaysInconnu {

		@Override
		public int hashCode() {
			return 4637292;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			return getClass() == obj.getClass();
		}
	}

	public Pays getPaysInconnu() throws InfrastructureException {
		final Pays resultat;

		final KeyGetPaysInconnu key = new KeyGetPaysInconnu();
		final Element element = cache.get(key);
		if (element == null) {
			resultat = target.getPaysInconnu();
			cache.put(new Element(key, resultat));
		}
		else {
			resultat = (Pays) element.getObjectValue();
		}

		return resultat;
	}

	private static class KeyGetRueByNumero {
		int numero;

		private KeyGetRueByNumero(int numero) {
			this.numero = numero;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + numero;
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
			KeyGetRueByNumero other = (KeyGetRueByNumero) obj;
			return numero == other.numero;
		}
	}

	public Rue getRueByNumero(int numero) throws InfrastructureException {
		final Rue resultat;

		final KeyGetRueByNumero key = new KeyGetRueByNumero(numero);
		final Element element = cache.get(key);
		if (element == null) {
			resultat = target.getRueByNumero(numero);
			cache.put(new Element(key, resultat));
		}
		else {
			resultat = (Rue) element.getObjectValue();
		}

		return resultat;
	}

	private static class KeyGetRueByLocalite {
		int noOrdre;

		public KeyGetRueByLocalite(Localite localite) {
			this.noOrdre = localite.getNoOrdre();
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + noOrdre;
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
			KeyGetRueByLocalite other = (KeyGetRueByLocalite) obj;
			return noOrdre == other.noOrdre;
		}
	}

	@SuppressWarnings("unchecked")
	public List<Rue> getRues(Localite localite) throws InfrastructureException {
		final List<Rue> resultat;

		final KeyGetRueByLocalite key = new KeyGetRueByLocalite(localite);
		final Element element = cache.get(key);
		if (element == null) {
			resultat = target.getRues(localite);
			cache.put(new Element(key, resultat));
		}
		else {
			resultat = (List<Rue>) element.getObjectValue();
		}

		return resultat;
	}

	private static class KeyGetRueByCanton {
		int noOfs;

		public KeyGetRueByCanton(Canton canton) {
			this.noOfs = canton.getNoOFS();
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + noOfs;
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
			KeyGetRueByCanton other = (KeyGetRueByCanton) obj;
			return noOfs == other.noOfs;
		}
	}

	@SuppressWarnings("unchecked")
	public List<Rue> getRues(Canton canton) throws InfrastructureException {
		final List<Rue> resultat;

		final KeyGetRueByCanton key = new KeyGetRueByCanton(canton);
		final Element element = cache.get(key);
		if (element == null) {
			resultat = target.getRues(canton);
			cache.put(new Element(key, resultat));
		}
		else {
			resultat = (List<Rue>) element.getObjectValue();
		}

		return resultat;
	}

	private static class KeyGetSuisse {

		@Override
		public int hashCode() {
			return 476372892;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			return getClass() == obj.getClass();
		}
	}

	public Pays getSuisse() throws ServiceInfrastructureException {
		final Pays resultat;

		final KeyGetSuisse key = new KeyGetSuisse();
		final Element element = cache.get(key);
		if (element == null) {
			resultat = target.getSuisse();
			cache.put(new Element(key, resultat));
		}
		else {
			resultat = (Pays) element.getObjectValue();
		}

		return resultat;
	}

	private static class KeyGetACI {

		@Override
		public int hashCode() {
			return 98732642;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			return getClass() == obj.getClass();
		}
	}

	private static class KeyGetACISuccessions {

		@Override
		public int hashCode() {
			return 98742653;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			return getClass() == obj.getClass();
		}
	}

	private static class KeyGetACIImpotSource {

		@Override
		public int hashCode() {
			return 98712659;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			return getClass() == obj.getClass();
		}
	}


	public CollectiviteAdministrative getACI() throws InfrastructureException {
		final CollectiviteAdministrative resultat;

		final KeyGetACI key = new KeyGetACI();
		final Element element = cache.get(key);
		if (element == null) {
			resultat = target.getACI();
			cache.put(new Element(key, resultat));
		}
		else {
			resultat = (CollectiviteAdministrative) element.getObjectValue();
		}

		return resultat;
	}

	public CollectiviteAdministrative getACIImpotSource() throws InfrastructureException {
		final CollectiviteAdministrative resultat;

		final KeyGetACIImpotSource key = new KeyGetACIImpotSource();
		final Element element = cache.get(key);
		if (element == null) {
			resultat = target.getACIImpotSource();
			cache.put(new Element(key, resultat));
		}
		else {
			resultat = (CollectiviteAdministrative) element.getObjectValue();
		}

		return resultat;
	}

	public CollectiviteAdministrative getACISuccessions() throws InfrastructureException {
		final CollectiviteAdministrative resultat;

		final KeyGetACISuccessions key = new KeyGetACISuccessions();
		final Element element = cache.get(key);
		if (element == null) {
			resultat = target.getACISuccessions();
			cache.put(new Element(key, resultat));
		}
		else {
			resultat = (CollectiviteAdministrative) element.getObjectValue();
		}

		return resultat;
	}


	private static class KeyGetCEDI {

		@Override
		public int hashCode() {
			return 98732643;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			return getClass() == obj.getClass();
		}
	}

	public CollectiviteAdministrative getCEDI() throws InfrastructureException {
		final CollectiviteAdministrative resultat;

		final KeyGetCEDI key = new KeyGetCEDI();
		final Element element = cache.get(key);
		if (element == null) {
			resultat = target.getCEDI();
			cache.put(new Element(key, resultat));
		}
		else {
			resultat = (CollectiviteAdministrative) element.getObjectValue();
		}

		return resultat;
	}

	private static class KeyGetCAT {

		@Override
		public int hashCode() {
			return 98732644;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			return getClass() == obj.getClass();
		}
	}

	public CollectiviteAdministrative getCAT() throws InfrastructureException {
		final CollectiviteAdministrative resultat;

		final KeyGetCAT key = new KeyGetCAT();
		final Element element = cache.get(key);
		if (element == null) {
			resultat = target.getCAT();
			cache.put(new Element(key, resultat));
		}
		else {
			resultat = (CollectiviteAdministrative) element.getObjectValue();
		}

		return resultat;
	}

	private static class KeyGetVaud {

		@Override
		public int hashCode() {
			return 463628289;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			return getClass() == obj.getClass();
		}
	}

	public Canton getVaud() throws InfrastructureException {
		final Canton resultat;

		final KeyGetVaud key = new KeyGetVaud();
		final Element element = cache.get(key);
		if (element == null) {
			resultat = target.getVaud();
			cache.put(new Element(key, resultat));
		}
		else {
			resultat = (Canton) element.getObjectValue();
		}

		return resultat;
	}

	private static class KeyGetInstitutionFinanciere {

		int id;

		public KeyGetInstitutionFinanciere(int id) {
			this.id = id;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + id;
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
			KeyGetInstitutionFinanciere other = (KeyGetInstitutionFinanciere) obj;
			return id == other.id;
		}
	}

	public InstitutionFinanciere getInstitutionFinanciere(int id) throws InfrastructureException {

		final InstitutionFinanciere resultat;

		final KeyGetInstitutionFinanciere key = new KeyGetInstitutionFinanciere(id);
		final Element element = cache.get(key);
		if (element == null) {
			resultat = target.getInstitutionFinanciere(id);
			cache.put(new Element(key, resultat));
		}
		else {
			resultat = (InstitutionFinanciere) element.getObjectValue();
		}

		return resultat;
	}

	private static class KeyGetInstitutionsFinancieres {

		String noClearing;

		public KeyGetInstitutionsFinancieres(String noClearing) {
			this.noClearing = noClearing;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((noClearing == null) ? 0 : noClearing.hashCode());
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
			KeyGetInstitutionsFinancieres other = (KeyGetInstitutionsFinancieres) obj;
			if (noClearing == null) {
				if (other.noClearing != null)
					return false;
			}
			else if (!noClearing.equals(other.noClearing))
				return false;
			return true;
		}
	}

	@SuppressWarnings("unchecked")
	public List<InstitutionFinanciere> getInstitutionsFinancieres(String noClearing) throws InfrastructureException {

		final List<InstitutionFinanciere> resultat;

		final KeyGetInstitutionsFinancieres key = new KeyGetInstitutionsFinancieres(noClearing);
		final Element element = cache.get(key);
		if (element == null) {
			resultat = target.getInstitutionsFinancieres(noClearing);
			cache.put(new Element(key, resultat));
		}
		else {
			resultat = (List<InstitutionFinanciere>) element.getObjectValue();
		}

		return resultat;
	}

	private static class KeyGetCodesRegimesFiscaux {

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			return true;
		}

		@Override
		public int hashCode() {
			return 5874894;
		}
	}

	@SuppressWarnings({"unchecked"})
	public List<TypeRegimeFiscal> getTypesRegimesFiscaux() throws InfrastructureException {

		final List<TypeRegimeFiscal> resultat;

		final KeyGetCodesRegimesFiscaux key = new KeyGetCodesRegimesFiscaux();
		final Element element = cache.get(key);
		if (element == null) {
			resultat = target.getTypesRegimesFiscaux();
			cache.put(new Element(key, resultat));
		}
		else {
			resultat = (List<TypeRegimeFiscal>) element.getObjectValue();
		}

		return resultat;
	}

	private static class KeyGetTypeRegimeFiscal {

		private String code;

		private KeyGetTypeRegimeFiscal(String code) {
			this.code = code;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			final KeyGetTypeRegimeFiscal that = (KeyGetTypeRegimeFiscal) o;
			return code.equals(that.code);

		}

		@Override
		public int hashCode() {
			return code.hashCode();
		}
	}

	public TypeRegimeFiscal getTypeRegimeFiscal(String code) throws InfrastructureException {

		final TypeRegimeFiscal resultat;

		final KeyGetTypeRegimeFiscal key = new KeyGetTypeRegimeFiscal(code);
		final Element element = cache.get(key);
		if (element == null) {
			resultat = target.getTypeRegimeFiscal(code);
			cache.put(new Element(key, resultat));
		}
		else {
			resultat = (TypeRegimeFiscal) element.getObjectValue();
		}

		return resultat;
	}

	private static class KeyGetCodesEtatsPM {

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			return true;
		}

		@Override
		public int hashCode() {
			return 333211;
		}
	}

	@SuppressWarnings({"unchecked"})
	public List<TypeEtatPM> getTypesEtatsPM() throws InfrastructureException {

		final List<TypeEtatPM> resultat;

		final KeyGetCodesEtatsPM key = new KeyGetCodesEtatsPM();
		final Element element = cache.get(key);
		if (element == null) {
			resultat = target.getTypesEtatsPM();
			cache.put(new Element(key, resultat));
		}
		else {
			resultat = (List<TypeEtatPM>) element.getObjectValue();
		}

		return resultat;
	}

	private static class KeyGetTypeEtatPM {

		private String code;

		private KeyGetTypeEtatPM(String code) {
			this.code = code;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			final KeyGetTypeEtatPM that = (KeyGetTypeEtatPM) o;
			return code.equals(that.code);

		}

		@Override
		public int hashCode() {
			return code.hashCode();
		}
	}

	public TypeEtatPM getTypeEtatPM(String code) throws InfrastructureException {

		final TypeEtatPM resultat;

		final KeyGetTypeEtatPM key = new KeyGetTypeEtatPM(code);
		final Element element = cache.get(key);
		if (element == null) {
			resultat = target.getTypeEtatPM(code);
			cache.put(new Element(key, resultat));
		}
		else {
			resultat = (TypeEtatPM) element.getObjectValue();
		}

		return resultat;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public String getDescription() {
		return "service infrastructure";
	}

	/**
	 * {@inheritDoc}
	 */
	public String getName() {
		return "INFRA";
	}

	/**
	 * {@inheritDoc}
	 */
	public void reset() {
		cache.removeAll();
	}
	/**
	 *  {@inheritDoc}
	 */
	public Localite getLocaliteByNPA(int npa) throws InfrastructureException {
		return target.getLocaliteByNPA(npa);
	}

	public String getUrlVers(ApplicationFiscale application, Long tiersId) {
		// on ne cache pas cette information parce que l'url est composée d'une partie statique auquelle est appondue le numéro d'id, et que le service concret fait ça de manière très efficace.
		return target.getUrlVers(application, tiersId);
	}

	private static class KeyGetLogiciel {

		private Long id;

		private KeyGetLogiciel(Long id) {
			this.id = id;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			final KeyGetLogiciel that = (KeyGetLogiciel) o;
			return id.equals(that.id);
		}

		@Override
		public int hashCode() {
			return id.hashCode();
		}
	}

	public Logiciel getLogiciel(Long idLogiciel) {
		final Logiciel resultat;

		final KeyGetLogiciel key = new KeyGetLogiciel(idLogiciel);
		final Element element = cache.get(key);
		if (element == null) {
			resultat = target.getLogiciel(idLogiciel);
			cache.put(new Element(key, resultat));
		}
		else {
			resultat = (Logiciel) element.getObjectValue();
		}

		return resultat;
	}

	private static class KeyGetTousLesLogiciels {

		@Override
		public boolean equals(Object o) {
			return this == o || getClass() == o.getClass();
		}

		@Override
		public int hashCode() {
			return 5308472;
		}
	}

	@SuppressWarnings({"unchecked"})
	public List<Logiciel> getTousLesLogiciels() {
		final List<Logiciel> resultat;

		final KeyGetTousLesLogiciels key = new KeyGetTousLesLogiciels();
		final Element element = cache.get(key);
		if (element == null) {
			resultat = target.getTousLesLogiciels();
			cache.put(new Element(key, resultat));
		}
		else {
			resultat = (List<Logiciel>) element.getObjectValue();
		}

		return resultat;
	}
}
