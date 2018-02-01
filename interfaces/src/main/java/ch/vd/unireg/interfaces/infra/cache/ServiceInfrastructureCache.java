package ch.vd.unireg.interfaces.infra.cache;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
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

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Assert;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureException;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureRaw;
import ch.vd.unireg.interfaces.infra.data.ApplicationFiscale;
import ch.vd.unireg.interfaces.infra.data.Canton;
import ch.vd.unireg.interfaces.infra.data.CollectiviteAdministrative;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.interfaces.infra.data.District;
import ch.vd.unireg.interfaces.infra.data.GenreImpotMandataire;
import ch.vd.unireg.interfaces.infra.data.InstitutionFinanciere;
import ch.vd.unireg.interfaces.infra.data.Localite;
import ch.vd.unireg.interfaces.infra.data.Logiciel;
import ch.vd.unireg.interfaces.infra.data.OfficeImpot;
import ch.vd.unireg.interfaces.infra.data.Pays;
import ch.vd.unireg.interfaces.infra.data.Region;
import ch.vd.unireg.interfaces.infra.data.Rue;
import ch.vd.unireg.interfaces.infra.data.TypeCollectivite;
import ch.vd.unireg.interfaces.infra.data.TypeRegimeFiscal;
import ch.vd.unireg.cache.CacheHelper;
import ch.vd.unireg.cache.CacheStats;
import ch.vd.unireg.cache.EhCacheStats;
import ch.vd.unireg.cache.KeyDumpableCache;
import ch.vd.unireg.cache.KeyValueDumpableCache;
import ch.vd.unireg.cache.UniregCacheInterface;
import ch.vd.unireg.cache.UniregCacheManager;
import ch.vd.unireg.stats.StatsService;
import ch.vd.unireg.utils.LogLevel;

@SuppressWarnings({"SimplifiableIfStatement"})
public class ServiceInfrastructureCache implements ServiceInfrastructureRaw, UniregCacheInterface, KeyDumpableCache, KeyValueDumpableCache, InitializingBean, DisposableBean {

	//private static final Logger LOGGER = LoggerFactory.getLogger(ServiceInfrastructureCache.class);

	private CacheManager cacheManager;
	private String cacheName;
	private String shortLivedCacheName;
	private ServiceInfrastructureRaw target;
	private Ehcache cache;
	private Ehcache shortLivedCache;
	private UniregCacheManager uniregCacheManager;
	private StatsService statsService;

	public void setTarget(ServiceInfrastructureRaw target) {
		this.target = target;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setCacheManager(CacheManager manager) {
		this.cacheManager = manager;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setCacheName(String cacheName) {
		this.cacheName = cacheName;
	}

	public void setShortLivedCacheName(String shortLivedCacheName) {
		this.shortLivedCacheName = shortLivedCacheName;
	}

	@SuppressWarnings({"UnusedDeclaration"})
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
		cache = cacheManager.getCache(cacheName);
		Assert.notNull(cache);
		shortLivedCache = cacheManager.getCache(shortLivedCacheName);
		Assert.notNull(shortLivedCache);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		initCache();
		if (statsService != null) {
			statsService.registerCache(ServiceInfrastructureRaw.SERVICE_NAME, this);
		}
		uniregCacheManager.register(this);
	}

	@Override
	public void destroy() throws Exception {
		if (statsService != null) {
			statsService.unregisterCache(ServiceInfrastructureRaw.SERVICE_NAME);
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

		@Override
		public String toString() {
			return getClass().getSimpleName() + "{}";
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<Canton> getAllCantons() throws ServiceInfrastructureException {
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

	private static class KeyGetCollectivite {
		final int noColAdm;

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
			return noColAdm == other.noColAdm;
		}

		@Override
		public String toString() {
			return "KeyGetCollectivite{" +
					"noColAdm=" + noColAdm +
					'}';
		}
	}

	@Override
	public CollectiviteAdministrative getCollectivite(int noColAdm) throws ServiceInfrastructureException {
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

		@Override
		public String toString() {
			return getClass().getSimpleName() + "{}";
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<CollectiviteAdministrative> getCollectivitesAdministratives() throws ServiceInfrastructureException {
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

		@NotNull
		final Set<TypeCollectivite> types;

		public KeyGetCollectivitesAdministrativesByTypes(List<TypeCollectivite> types) {
			this.types = EnumSet.noneOf(TypeCollectivite.class);
			this.types.addAll(types);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			final KeyGetCollectivitesAdministrativesByTypes that = (KeyGetCollectivitesAdministrativesByTypes) o;
			return Objects.equals(types, that.types);
		}

		@Override
		public int hashCode() {
			return Objects.hash(types);
		}

		@Override
		public String toString() {
			return "KeyGetCollectivitesAdministrativesByTypes{" +
					"types=" + Arrays.toString(types.toArray(new TypeCollectivite[types.size()])) +
					'}';
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<CollectiviteAdministrative> getCollectivitesAdministratives(List<TypeCollectivite> typesCollectivite) throws ServiceInfrastructureException {
		final List<CollectiviteAdministrative> resultat;

		final KeyGetCollectivitesAdministrativesByTypes key = new KeyGetCollectivitesAdministrativesByTypes(typesCollectivite);
		final Element element = shortLivedCache.get(key);
		if (element == null) {
			resultat = target.getCollectivitesAdministratives(typesCollectivite);
			shortLivedCache.put(new Element(key, resultat));
		}
		else {
			resultat = (List<CollectiviteAdministrative>) element.getObjectValue();
		}

		return resultat;
	}

	private static class KeyGetCommuneByLocalite {
		final int numOrdre; // nécessaire pour distinguer les fractions de communes
		final int noCommune;

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

		@Override
		public String toString() {
			return "KeyGetCommuneByLocalite{" +
					"numOrdre=" + numOrdre +
					", noCommune=" + noCommune +
					'}';
		}
	}

	@Override
	public Commune getCommuneByLocalite(Localite localite) throws ServiceInfrastructureException {
		final Commune resultat;

		final KeyGetCommuneByLocalite key = new KeyGetCommuneByLocalite(localite);
		final Element element = shortLivedCache.get(key);
		if (element == null) {
			resultat = target.getCommuneByLocalite(localite);
			shortLivedCache.put(new Element(key, resultat));
		}
		else {
			resultat = (Commune) element.getObjectValue();
		}

		return resultat;
	}

	private static class KeyFindCommuneByNomOfficiel {

		@NotNull
		private final String nomOfficiel;
		private final boolean includeFaitieres;
		private final boolean includeFractions;
		@Nullable
		private final RegDate date;

		public KeyFindCommuneByNomOfficiel(@NotNull String nomOfficiel, boolean includeFaitieres, boolean includeFractions, @Nullable RegDate date) {
			this.nomOfficiel = nomOfficiel;
			this.includeFaitieres = includeFaitieres;
			this.includeFractions = includeFractions;
			this.date = date;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			final KeyFindCommuneByNomOfficiel that = (KeyFindCommuneByNomOfficiel) o;
			return includeFaitieres == that.includeFaitieres &&
					includeFractions == that.includeFractions &&
					Objects.equals(nomOfficiel, that.nomOfficiel) &&
					Objects.equals(date, that.date);
		}

		@Override
		public int hashCode() {
			return Objects.hash(nomOfficiel, includeFaitieres, includeFractions, date);
		}

		@Override
		public String toString() {
			return "KeyFindCommuneByNomOfficiel{" +
					"nomOfficiel='" + nomOfficiel + '\'' +
					", includeFaitieres=" + includeFaitieres +
					", includeFractions=" + includeFractions +
					", date=" + date +
					'}';
		}
	}

	@Nullable
	@Override
	public Commune findCommuneByNomOfficiel(@NotNull String nomOfficiel, boolean includeFaitieres, boolean includeFractions, @Nullable RegDate date) throws ServiceInfrastructureException {

		final Commune resultat;

		final KeyFindCommuneByNomOfficiel key = new KeyFindCommuneByNomOfficiel(nomOfficiel, includeFaitieres, includeFractions, date);
		final Element element = cache.get(key);
		if (element == null) {
			resultat = target.findCommuneByNomOfficiel(nomOfficiel, includeFaitieres, includeFractions, date);
			cache.put(new Element(key, resultat));
		}
		else {
			resultat = (Commune) element.getObjectValue();
		}

		return resultat;
	}

	private static class KeyGetCommuneHistoByNumeroOfs {
		final int noOfsCommune;

		private KeyGetCommuneHistoByNumeroOfs(int noOfsCommune) {
			this.noOfsCommune = noOfsCommune;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			final KeyGetCommuneHistoByNumeroOfs that = (KeyGetCommuneHistoByNumeroOfs) o;
			return noOfsCommune == that.noOfsCommune;
		}

		@Override
		public int hashCode() {
			return noOfsCommune;
		}

		@Override
		public String toString() {
			return "KeyGetCommuneHistoByNumeroOfs{" +
					"noOfsCommune=" + noOfsCommune +
					'}';
		}
	}

	@SuppressWarnings({"unchecked"})
	@Override
	public List<Commune> getCommuneHistoByNumeroOfs(int noOfsCommune) throws ServiceInfrastructureException {
		final List<Commune> resultat;

		final KeyGetCommuneHistoByNumeroOfs key = new KeyGetCommuneHistoByNumeroOfs(noOfsCommune);
		final Element element = cache.get(key);
		if (element == null) {
			resultat = target.getCommuneHistoByNumeroOfs(noOfsCommune);
			cache.put(new Element(key, resultat));
		}
		else {
			resultat = (List<Commune>) element.getObjectValue();
		}

		return resultat;
	}

	private static class KeyGetCommunesByEgid {
		final int egid;
		final RegDate date;

		private KeyGetCommunesByEgid(int egid, RegDate date) {
			this.egid = egid;
			this.date = date;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			final KeyGetCommunesByEgid that = (KeyGetCommunesByEgid) o;

			if (egid != that.egid) return false;
			return !(date != null ? !date.equals(that.date) : that.date != null);
		}

		@Override
		public int hashCode() {
			int result = egid;
			result = 31 * result + (date != null ? date.hashCode() : 0);
			return result;
		}

		@Override
		public String toString() {
			return "KeyGetCommunesByEgid{" +
					"egid=" + egid +
					", date=" + date +
					'}';
		}
	}

	@Override
	public Integer getNoOfsCommuneByEgid(int egid, RegDate date) throws ServiceInfrastructureException {
		final Integer resultat;

		final KeyGetCommunesByEgid key = new KeyGetCommunesByEgid(egid, date);
		final Element element = shortLivedCache.get(key);
		if (element == null) {
			resultat = target.getNoOfsCommuneByEgid(egid, date);
			shortLivedCache.put(new Element(key, resultat));
		}
		else {
			resultat = (Integer) element.getObjectValue();
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

		@Override
		public String toString() {
			return getClass().getSimpleName() + "{}";
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<Commune> getCommunes() throws ServiceInfrastructureException {
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
		final int noOfsCanton;

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

		@Override
		public String toString() {
			return "KeyGetListeCommunes{" +
					"noOfsCanton=" + noOfsCanton +
					'}';
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<Commune> getListeCommunes(Canton canton) throws ServiceInfrastructureException {
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


	private static class KeyGetCommunesVD {

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

		@Override
		public String toString() {
			return getClass().getSimpleName() + "{}";
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<Commune> getCommunesVD() throws ServiceInfrastructureException {
		final List<Commune> resultat;

		final KeyGetCommunesVD key = new KeyGetCommunesVD();
		final Element element = cache.get(key);
		if (element == null) {
			resultat = target.getCommunesVD();
			cache.put(new Element(key, resultat));
		}
		else {
			resultat = (List<Commune>) element.getObjectValue();
		}

		return resultat;
	}

	private static class KeyGetListeCommunesFaitieres {

		@Override
		public int hashCode() {
			return -483823;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			return getClass() == obj.getClass();
		}

		@Override
		public String toString() {
			return getClass().getSimpleName() + "{}";
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<Commune> getListeCommunesFaitieres() throws ServiceInfrastructureException {
		final List<Commune> resultat;

		final KeyGetListeCommunesFaitieres key = new KeyGetListeCommunesFaitieres();
		final Element element = cache.get(key);
		if (element == null) {
			resultat = target.getListeCommunesFaitieres();
			cache.put(new Element(key, resultat));
		}
		else {
			resultat = (List<Commune>) element.getObjectValue();
		}

		return resultat;
	}


	private static class KeyGetLocalitesByONRP {
		final int onrp;

		private KeyGetLocalitesByONRP(int onrp) {
			this.onrp = onrp;
		}

		@Override
		public int hashCode() {
			return onrp;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			final KeyGetLocalitesByONRP that = (KeyGetLocalitesByONRP) o;
			return onrp == that.onrp;
		}

		@Override
		public String toString() {
			return "KeyGetLocalitesByONRP{" +
					"onrp=" + onrp +
					'}';
		}
	}

	@Override
	public List<Localite> getLocalitesByONRP(int onrp) throws ServiceInfrastructureException {
		final List<Localite> resultat;

		final KeyGetLocalitesByONRP key = new KeyGetLocalitesByONRP(onrp);
		final Element element = cache.get(key);
		if (element == null) {
			resultat = target.getLocalitesByONRP(onrp);
			cache.put(new Element(key, resultat));
		}
		else {
			//noinspection unchecked
			resultat = (List<Localite>) element.getObjectValue();
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

		@Override
		public String toString() {
			return getClass().getSimpleName() + "{}";
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<Localite> getLocalites() throws ServiceInfrastructureException {
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

		@Override
		public String toString() {
			return getClass().getSimpleName() + "{}";
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<OfficeImpot> getOfficesImpot() throws ServiceInfrastructureException {
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

		@Override
		public String toString() {
			return getClass().getSimpleName() + "{}";
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<Pays> getPays() throws ServiceInfrastructureException {
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

	private static final class KeyGetPaysHisto {
		private final int numeroOFS;

		private KeyGetPaysHisto(int numeroOFS) {
			this.numeroOFS = numeroOFS;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			final KeyGetPaysHisto that = (KeyGetPaysHisto) o;
			return numeroOFS == that.numeroOFS;
		}

		@Override
		public int hashCode() {
			return numeroOFS;
		}

		@Override
		public String toString() {
			return getClass().getSimpleName() + "{" +
					"numeroOFS=" + numeroOFS +
					'}';
		}
	}

	@Override
	public List<Pays> getPaysHisto(int numeroOFS) throws ServiceInfrastructureException {
		final List<Pays> resultat;

		final KeyGetPaysHisto key = new KeyGetPaysHisto(numeroOFS);
		final Element element = cache.get(key);
		if (element == null) {
			resultat = target.getPaysHisto(numeroOFS);
			cache.put(new Element(key, resultat));
		}
		else {
			//noinspection unchecked
			resultat = (List<Pays>) element.getObjectValue();
		}

		return resultat;
	}

	/**
	 * Classe abstraite des clés utilisées pour le stockage des informations de pays
	 */
	private abstract static class KeyGetPaysByPeriod {
		private final DateRange validityRange;

		protected KeyGetPaysByPeriod(DateRange validityRange) {
			this.validityRange = validityRange;
		}

		@Override
		public boolean equals(Object o) {
			if (o == null) {
				return false;
			}
			else if (o instanceof KeyGetPaysByPeriod) {
				return DateRangeHelper.equals(validityRange, ((KeyGetPaysByPeriod) o).validityRange);
			}
			else if (o instanceof KeyGetPaysByDate) {
				return validityRange.isValidAt(((KeyGetPaysByDate) o).date);
			}
			else {
				return false;
			}
		}

		@Override
		public abstract int hashCode();

		@Override
		public final String toString() {
			return getClass().getSimpleName() + "{" +
					"validityRange=" + DateRangeHelper.toString(validityRange) +
					toStringPart() +
					'}';
		}

		protected abstract String toStringPart();
	}

	private interface KeyGetPaysByNoOfs {
		int getNoOfs();

		/**
		 * Il est impératif que ce calcul de hash soit le même dans toutes les sous-classes
		 * @return la valeur de {@link #getNoOfs()}
		 */
		@Override
		int hashCode();
	}

	private static final class KeyGetPaysByNoOfsAndPeriod extends KeyGetPaysByPeriod implements KeyGetPaysByNoOfs {
		private final int noOfs;

		private KeyGetPaysByNoOfsAndPeriod(int noOfs, DateRange validityRange) {
			super(validityRange);
			this.noOfs = noOfs;
		}

		@Override
		public int getNoOfs() {
			return noOfs;
		}

		@Override
		public boolean equals(Object o) {
			if (o == this) {
				return true;
			}
			else if (o == null) {
				return false;
			}
			else if (o instanceof KeyGetPaysByNoOfs) {
				return ((KeyGetPaysByNoOfs) o).getNoOfs() == noOfs && super.equals(o);
			}
			else {
				return false;
			}
		}

		@Override
		public int hashCode() {
			return getNoOfs();
		}

		@Override
		protected String toStringPart() {
			return ", noOfs=" + noOfs;
		}
	}

	/**
	 * Classe abstraite parente des clés utilisées pour la recherche dans le cache des informations de pays
	 */
	private abstract static class KeyGetPaysByDate {
		private final RegDate date;

		protected KeyGetPaysByDate(RegDate date) {
			// la valeur nulle dans la recherche signifie "date du jour" (c'est un comportement connu du service infrastructure),
			// mais si on conserve "null" ici, et que la date de fin du pays est dans le futur, alors le cache ne sera pas efficace
			// car "null" n'est dans aucun intervalle de temps fermé à droite.
			this.date = (date != null ? date : RegDate.get());
		}

		@Override
		public boolean equals(Object o) {
			if (o == null) {
				return false;
			}
			else if (o instanceof KeyGetPaysByDate) {
				return date == ((KeyGetPaysByDate) o).date;
			}
			else if (o instanceof KeyGetPaysByPeriod) {
				return ((KeyGetPaysByPeriod) o).validityRange.isValidAt(date);
			}
			else {
				return false;
			}
		}

		@Override
		public abstract int hashCode();

		@Override
		public final String toString() {
			return getClass().getSimpleName() + "{" +
					"date=" + date +
					toStringPart() +
					'}';
		}

		protected abstract String toStringPart();
	}

	private static final class KeyGetPaysByNoOfsAndDate extends KeyGetPaysByDate implements KeyGetPaysByNoOfs {
		private final int noOfs;

		private KeyGetPaysByNoOfsAndDate(int noOfs, RegDate date) {
			super(date);
			this.noOfs = noOfs;
		}

		@Override
		public int getNoOfs() {
			return noOfs;
		}

		@Override
		public boolean equals(Object o) {
			if (o == this) {
				return true;
			}
			else if (o == null) {
				return false;
			}
			else if (o instanceof KeyGetPaysByNoOfs) {
				return ((KeyGetPaysByNoOfs) o).getNoOfs() == noOfs && super.equals(o);
			}
			else {
				return false;
			}
		}

		@Override
		public int hashCode() {
			return getNoOfs();
		}

		@Override
		protected String toStringPart() {
			return ", noOfs=" + noOfs;
		}
	}

	@Override
	public Pays getPays(int numeroOFS, @Nullable RegDate date) throws ServiceInfrastructureException {
		final Pays resultat;

		final KeyGetPaysByNoOfsAndDate lookupKey = new KeyGetPaysByNoOfsAndDate(numeroOFS, date);
		final Element element = cache.get(lookupKey);
		if (element == null) {
			resultat = target.getPays(numeroOFS, date);
			if (resultat != null) {
				final KeyGetPaysByNoOfsAndPeriod storageKey = new KeyGetPaysByNoOfsAndPeriod(numeroOFS, new DateRangeHelper.Range(resultat));
				cache.put(new Element(storageKey, resultat));
			}
		}
		else {
			resultat = (Pays) element.getObjectValue();
		}

		return resultat;
	}

	private interface KeyGetPaysByCodeIso {
		@NotNull String getCodeIso();
		int hashCode();
	}

	private static final class KeyGetPaysByCodeIsoAndPeriod extends KeyGetPaysByPeriod implements KeyGetPaysByCodeIso {

		@NotNull
		private final String codeIso;

		private KeyGetPaysByCodeIsoAndPeriod(@NotNull String codeIso, DateRange validityRange) {
			super(validityRange);
			this.codeIso = codeIso;
		}

		@Override
		public boolean equals(Object o) {
			if (o == this) {
				return true;
			}
			else if (o == null) {
				return false;
			}
			else if (o instanceof KeyGetPaysByCodeIso) {
				return codeIso.equals(((KeyGetPaysByCodeIso) o).getCodeIso()) && super.equals(o);
			}
			else {
				return false;
			}
		}

		@Override
		@NotNull
		public String getCodeIso() {
			return codeIso;
		}

		@Override
		public int hashCode() {
			return codeIso.hashCode();
		}

		@Override
		protected String toStringPart() {
			return ", codeIso='" + codeIso + '\'';
		}
	}

	private static final class KeyGetPaysByCodeIsoAndDate extends KeyGetPaysByDate implements KeyGetPaysByCodeIso {

		@NotNull
		private final String codeIso;

		private KeyGetPaysByCodeIsoAndDate(@NotNull String codeIso, RegDate date) {
			super(date);
			this.codeIso = codeIso;
		}

		@NotNull
		@Override
		public String getCodeIso() {
			return codeIso;
		}

		@Override
		public boolean equals(Object o) {
			if (o == this) {
				return true;
			}
			else if (o == null) {
				return false;
			}
			else if (o instanceof KeyGetPaysByCodeIso) {
				return codeIso.equals(((KeyGetPaysByCodeIso) o).getCodeIso()) && super.equals(o);
			}
			else {
				return false;
			}
		}

		@Override
		public int hashCode() {
			return codeIso.hashCode();
		}

		@Override
		protected String toStringPart() {
			return ", codeIso='" + codeIso + '\'';
		}
	}

	@Override
	public Pays getPays(@NotNull String codePays, @Nullable RegDate date) throws ServiceInfrastructureException {
		final Pays resultat;
		final KeyGetPaysByCodeIsoAndDate lookupKey = new KeyGetPaysByCodeIsoAndDate(codePays, date);
		final Element element = cache.get(lookupKey);
		if (element == null) {
			resultat = target.getPays(codePays, date);
			if (resultat != null) {
				final KeyGetPaysByCodeIsoAndPeriod storageKey = new KeyGetPaysByCodeIsoAndPeriod(codePays, new DateRangeHelper.Range(resultat));
				cache.put(new Element(storageKey, resultat));
			}
		}
		else {
			resultat = (Pays) element.getObjectValue();
		}

		return resultat;
	}

	private static class KeyGetRueByNumeroEtDate {
		private final int numero;
		private final RegDate date;

		private KeyGetRueByNumeroEtDate(int numero, RegDate date) {
			this.numero = numero;
			this.date = date;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			final KeyGetRueByNumeroEtDate that = (KeyGetRueByNumeroEtDate) o;
			return date == that.date && numero == that.numero;
		}

		@Override
		public int hashCode() {
			int result = numero;
			result = 31 * result + (date != null ? date.hashCode() : 0);
			return result;
		}

		@Override
		public String toString() {
			return "KeyGetRueByNumeroEtDate{" +
					"numero=" + numero +
					", date=" + date +
					'}';
		}
	}

	@Override
	public Rue getRueByNumero(int numero, RegDate date) throws ServiceInfrastructureException {
		final Rue resultat;

		final KeyGetRueByNumeroEtDate key = new KeyGetRueByNumeroEtDate(numero, date);
		final Element element = shortLivedCache.get(key);
		if (element == null) {
			resultat = target.getRueByNumero(numero, date);
			shortLivedCache.put(new Element(key, resultat));
		}
		else {
			resultat = (Rue) element.getObjectValue();
		}

		return resultat;
	}

	private static class KeyGetRuesHisto {
		final int numeroRue;

		private KeyGetRuesHisto(int numeroRue) {
			this.numeroRue = numeroRue;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			final KeyGetRuesHisto that = (KeyGetRuesHisto) o;
			return numeroRue == that.numeroRue;
		}

		@Override
		public int hashCode() {
			return numeroRue;
		}

		@Override
		public String toString() {
			return "KeyGetRuesHisto{" +
					"numeroRue=" + numeroRue +
					'}';
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<Rue> getRuesHisto(int numero) throws ServiceInfrastructureException {
		final List<Rue> resultat;

		final KeyGetRuesHisto key = new KeyGetRuesHisto(numero);
		final Element element = shortLivedCache.get(key);
		if (element == null) {
			resultat = target.getRuesHisto(numero);
			shortLivedCache.put(new Element(key, resultat));
		}
		else {
			resultat = (List<Rue>) element.getObjectValue();
		}
		return resultat;
	}

	private static class KeyGetRueByLocalite {
		final int noOrdre;

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

		@Override
		public String toString() {
			return "KeyGetRueByLocalite{" +
					"noOrdre=" + noOrdre +
					'}';
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<Rue> getRues(Localite localite) throws ServiceInfrastructureException {
		final List<Rue> resultat;

		final KeyGetRueByLocalite key = new KeyGetRueByLocalite(localite);
		final Element element = shortLivedCache.get(key);
		if (element == null) {
			resultat = target.getRues(localite);
			shortLivedCache.put(new Element(key, resultat));
		}
		else {
			resultat = (List<Rue>) element.getObjectValue();
		}

		return resultat;
	}

	private static class KeyGetInstitutionFinanciere {

		final int id;

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

		@Override
		public String toString() {
			return "KeyGetInstitutionFinanciere{" +
					"id=" + id +
					'}';
		}
	}

	@Override
	public InstitutionFinanciere getInstitutionFinanciere(int id) throws ServiceInfrastructureException {

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

		final String noClearing;

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

		@Override
		public String toString() {
			return "KeyGetInstitutionsFinancieres{" +
					"noClearing='" + noClearing + '\'' +
					'}';
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<InstitutionFinanciere> getInstitutionsFinancieres(String noClearing) throws ServiceInfrastructureException {

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

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getDescription() {
		return "service infrastructure";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getName() {
		return "INFRA";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void reset() {
		cache.removeAll();
		shortLivedCache.removeAll();
	}
	/**
	 *  {@inheritDoc}
	 */
	@Override
	public List<Localite> getLocalitesByNPA(int npa, RegDate dateReference) throws ServiceInfrastructureException {
		return target.getLocalitesByNPA(npa, dateReference);
	}

	@Override
	public String getUrl(ApplicationFiscale application, @Nullable Map<String, String> parametres) {
		// on ne cache pas cette information parce que l'url est composée d'une partie statique auquelle est appondue le numéro d'id, et que le service concret fait ça de manière très efficace.
		return target.getUrl(application, parametres);
	}

	private static class KeyGetLogiciel {

		private final Long id;

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

		@Override
		public String toString() {
			return "KeyGetLogiciel{" +
					"id=" + id +
					'}';
		}
	}

	@Override
	public Logiciel getLogiciel(Long idLogiciel) throws ServiceInfrastructureException {
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

		@Override
		public String toString() {
			return getClass().getSimpleName() + "{}";
		}
	}

	@Override
	@SuppressWarnings({"unchecked"})
	public List<Logiciel> getTousLesLogiciels() throws ServiceInfrastructureException {
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

	private static class KeyGetDistrict {

		private final int code;

		private KeyGetDistrict(int code) {
			this.code = code;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			final KeyGetDistrict that = (KeyGetDistrict) o;
			return code == that.code;

		}

		@Override
		public int hashCode() {
			return code;
		}

		@Override
		public String toString() {
			return "KeyGetDistrict{" +
					"code=" + code +
					'}';
		}
	}

	@Override
	public District getDistrict(int code) {
		final District resultat;

		final KeyGetDistrict key = new KeyGetDistrict(code);
		final Element element = cache.get(key);
		if (element == null) {
			resultat = target.getDistrict(code);
			cache.put(new Element(key, resultat));
		}
		else {
			resultat = (District) element.getObjectValue();
		}

		return resultat;
	}

	private static class KeyGetRegion {

		private final int code;

		private KeyGetRegion(int code) {
			this.code = code;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			final KeyGetRegion that = (KeyGetRegion) o;
			return code == that.code;

		}

		@Override
		public int hashCode() {
			return code;
		}

		@Override
		public String toString() {
			return "KeyGetRegion{" +
					"code=" + code +
					'}';
		}
	}

	@Override
	public Region getRegion(int code) {
		final Region resultat;

		final KeyGetRegion key = new KeyGetRegion(code);
		final Element element = cache.get(key);
		if (element == null) {
			resultat = target.getRegion(code);
			cache.put(new Element(key, resultat));
		}
		else {
			resultat = (Region) element.getObjectValue();
		}

		return resultat;
	}

	private static class KeyGetTousRegimesFiscaux {
		@Override
		public int hashCode() {
			return 43278237;
		}

		@Override
		public boolean equals(Object o) {
			return this == o || getClass() == o.getClass();
		}

		@Override
		public String toString() {
			return getClass().getSimpleName() + "{}";
		}
	}

	@Override
	public List<TypeRegimeFiscal> getTousLesRegimesFiscaux() {
		final List<TypeRegimeFiscal> resultat;
		final KeyGetTousRegimesFiscaux key = new KeyGetTousRegimesFiscaux();
		final Element element = cache.get(key);
		if (element == null) {
			resultat = target.getTousLesRegimesFiscaux();
			cache.put(new Element(key, resultat));
		}
		else {
			//noinspection unchecked
			resultat = (List<TypeRegimeFiscal>) element.getValue();
		}
		return resultat;
	}

	private static class KeyGetTousLesGenresImpotMandataires {
		@Override
		public int hashCode() {
			return 784524687;
		}

		@Override
		public boolean equals(Object obj) {
			return this == obj || getClass() == obj.getClass();
		}

		@Override
		public String toString() {
			return getClass().getSimpleName() + "{}";
		}
	}

	@Override
	public List<GenreImpotMandataire> getTousLesGenresImpotMandataires() {
		final List<GenreImpotMandataire> resultat;
		final KeyGetTousLesGenresImpotMandataires key = new KeyGetTousLesGenresImpotMandataires();
		final Element element = cache.get(key);
		if (element == null) {
			resultat = target.getTousLesGenresImpotMandataires();
			cache.put(new Element(key, resultat));
		}
		else {
			//noinspection unchecked
			resultat = (List<GenreImpotMandataire>) element.getValue();
		}
		return resultat;
	}

	@Override
	public void dumpCacheKeys(Logger logger, LogLevel.Level level) {
		CacheHelper.dumpCacheKeys(cache, logger, level);
		CacheHelper.dumpCacheKeys(shortLivedCache, logger, level);
	}

	@Override
	public void dumpCacheContent(Logger logger, LogLevel.Level level) {
		CacheHelper.dumpCacheKeysAndValues(cache, logger, level, null);
		CacheHelper.dumpCacheKeysAndValues(shortLivedCache, logger, level, null);
	}

	@Override
	public void ping() throws ServiceInfrastructureException {
		// on ne cache bien-sûr pas cet appel...
		target.ping();
	}
}
