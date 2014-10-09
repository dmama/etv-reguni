package ch.vd.unireg.interfaces.infra.fidor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

import ch.vd.evd0007.v1.Country;
import ch.vd.evd0012.v1.CommuneFiscale;
import ch.vd.infrastructure.model.EnumTypeCollectivite;
import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.NotImplementedException;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureException;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureRaw;
import ch.vd.unireg.interfaces.infra.data.ApplicationFiscale;
import ch.vd.unireg.interfaces.infra.data.Canton;
import ch.vd.unireg.interfaces.infra.data.CollectiviteAdministrative;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.interfaces.infra.data.CommuneImpl;
import ch.vd.unireg.interfaces.infra.data.District;
import ch.vd.unireg.interfaces.infra.data.DistrictImpl;
import ch.vd.unireg.interfaces.infra.data.InstitutionFinanciere;
import ch.vd.unireg.interfaces.infra.data.Localite;
import ch.vd.unireg.interfaces.infra.data.Logiciel;
import ch.vd.unireg.interfaces.infra.data.LogicielImpl;
import ch.vd.unireg.interfaces.infra.data.OfficeImpot;
import ch.vd.unireg.interfaces.infra.data.Pays;
import ch.vd.unireg.interfaces.infra.data.PaysImpl;
import ch.vd.unireg.interfaces.infra.data.Region;
import ch.vd.unireg.interfaces.infra.data.RegionImpl;
import ch.vd.unireg.interfaces.infra.data.Rue;
import ch.vd.unireg.interfaces.infra.data.TypeEtatPM;
import ch.vd.unireg.interfaces.infra.data.TypeRegimeFiscal;
import ch.vd.uniregctb.cache.CacheStats;
import ch.vd.uniregctb.cache.SimpleCacheStats;
import ch.vd.uniregctb.cache.UniregCacheInterface;
import ch.vd.uniregctb.cache.UniregCacheManager;
import ch.vd.uniregctb.webservice.fidor.v5.FidorClient;
import ch.vd.uniregctb.webservice.fidor.v5.FidorClientException;

/**
 * Implémentation Fidor du service d'infrastructure [UNIREG-2187].
 */
public class ServiceInfrastructureFidor implements ServiceInfrastructureRaw, UniregCacheInterface, InitializingBean {

	private static final Logger LOGGER = LoggerFactory.getLogger(ServiceInfrastructureFidor.class);

	/**
	 * Cache des URLs de Fidor
	 */
	private Map<ApplicationFiscale, String> urlsApplication = null;
	private SimpleCacheStats urlsStats = new SimpleCacheStats();

	private long lastTentative = 0;
	private static final long fiveMinutes = 5L * 60L * 1000000000L; // en nanosecondes

	private FidorClient fidorClient;
	private UniregCacheManager uniregCacheManager;

	public void setFidorClientv5(FidorClient fidorClient) {
		this.fidorClient = fidorClient;
	}

	public void setUniregCacheManager(UniregCacheManager uniregCacheManager) {
		this.uniregCacheManager = uniregCacheManager;
	}

	@Override
	public String getName() {
		return "URLS-FIDOR";
	}

	@Override
	public String getDescription() {
		return "urls de fidor";
	}

	@Override
	public CacheStats buildStats() {
		return urlsStats;
	}

	@Override
	public void reset() {
		urlsApplication = null;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		uniregCacheManager.register(this);
	}

	@Override
	public List<Pays> getPays() throws ServiceInfrastructureException {
		try {
			final List<Country> list = fidorClient.getTousLesPays();
			if (list == null || list.isEmpty()) {
				return Collections.emptyList();
			}
			else {
				final List<Pays> pays = new ArrayList<>(list.size());
				for (Country o : list) {
					pays.add(PaysImpl.get(o));
				}
				return Collections.unmodifiableList(pays);
			}
		}
		catch (FidorClientException e) {
			throw new ServiceInfrastructureException(e);
		}
	}

	@Override
	public List<Pays> getPaysHisto(int numeroOFS) throws ServiceInfrastructureException {
		try {
			final List<Country> list = fidorClient.getPaysHisto(numeroOFS);
			if (list == null || list.isEmpty()) {
				return Collections.emptyList();
			}
			else {
				final List<Pays> pays = new ArrayList<>(list.size());
				for (Country o : list) {
					pays.add(PaysImpl.get(o));
				}
				Collections.sort(pays, new DateRangeComparator<Pays>());
				return Collections.unmodifiableList(pays);
			}
		}
		catch (FidorClientException e) {
			throw new ServiceInfrastructureException(e);
		}
	}

	@Override
	public Pays getPays(int numeroOFS, @Nullable RegDate date) throws ServiceInfrastructureException {
		try {
			final Country p = fidorClient.getPaysDetail(numeroOFS, date);
			return PaysImpl.get(p);
		}
		catch (FidorClientException e) {
			throw new ServiceInfrastructureException(e);
		}
	}

	@Override
	public Pays getPays(@NotNull String codePays, @Nullable RegDate date) throws ServiceInfrastructureException {
		try {
			final Country p = fidorClient.getPaysDetail(codePays, date);
			return PaysImpl.get(p);
		}
		catch (FidorClientException e) {
			throw new ServiceInfrastructureException(e);
		}
	}

	@Override
	public CollectiviteAdministrative getCollectivite(int noColAdm) throws ServiceInfrastructureException {
		throw new NotImplementedException("Pas encore implémenté dans Fidor");
	}

	@Override
	public List<Canton> getAllCantons() throws ServiceInfrastructureException {
		throw new NotImplementedException("Pas encore implémenté dans Fidor");
	}

	@Override
	public List<Commune> getListeCommunes(Canton canton) throws ServiceInfrastructureException {
		try {
			final List<CommuneFiscale> list = fidorClient.getCommunesParCanton(canton.getNoOFS(), null);
			if (list == null || list.isEmpty()) {
				return Collections.emptyList();
			}

			final List<Commune> communes = new ArrayList<>();
			for (CommuneFiscale commune : list) {
				communes.add(CommuneImpl.get(commune));
			}
			return communes;
		}
		catch (FidorClientException e) {
			throw new ServiceInfrastructureException(e);
		}
	}

	@Override
	public List<Commune> getListeFractionsCommunes() throws ServiceInfrastructureException {
		try {
			final List<CommuneFiscale> all = fidorClient.getToutesLesCommunes();
			if (all == null || all.isEmpty()) {
				return Collections.emptyList();
			}

			final List<Commune> communes = new ArrayList<>();
			for (CommuneFiscale commune : all) {
				if (!commune.isEstUneCommuneFaitiere() && ServiceInfrastructureRaw.SIGLE_CANTON_VD.equals(commune.getSigleCanton())) {
					communes.add(CommuneImpl.get(commune));
				}
			}
			return communes;
		}
		catch (FidorClientException e) {
			throw new ServiceInfrastructureException(e);
		}
	}

	@Override
	public List<Commune> getCommunes() throws ServiceInfrastructureException {
		try {
			final List<CommuneFiscale> all = fidorClient.getToutesLesCommunes();
			if (all == null || all.isEmpty()) {
				return Collections.emptyList();
			}

			final List<Commune> communes = new ArrayList<>(all.size());
			for (CommuneFiscale commune : all) {
				communes.add(CommuneImpl.get(commune));
			}
			return communes;
		}
		catch (FidorClientException e) {
			throw new ServiceInfrastructureException(e);
		}
	}

	@Override
	public List<Localite> getLocalites() throws ServiceInfrastructureException {
		throw new NotImplementedException("Pas encore implémenté dans Fidor");
	}

	@Override
	public Localite getLocaliteByONRP(int onrp) throws ServiceInfrastructureException {
		throw new NotImplementedException("Pas encore implémenté dans Fidor");
	}

	@Override
	public List<Rue> getRues(Localite localite) throws ServiceInfrastructureException {
		throw new NotImplementedException("Pas encore implémenté dans Fidor");
	}

	@Override
	public List<Rue> getRues(Canton canton) throws ServiceInfrastructureException {
		throw new NotImplementedException("Pas encore implémenté dans Fidor");
	}

	@Override
	public Rue getRueByNumero(int numero) throws ServiceInfrastructureException {
		throw new NotImplementedException("Pas encore implémenté dans Fidor");
	}

	@Override
	public List<Commune> getCommuneHistoByNumeroOfs(int noOfsCommune) throws ServiceInfrastructureException {
		try {
			final List<Commune> list = new ArrayList<>();
			final List<CommuneFiscale> l = fidorClient.getCommunesParNoOFS(noOfsCommune);
			if (l != null) {
				for (CommuneFiscale c : l) {
					list.add(CommuneImpl.get(c));
				}
			}
			return list;
		}
		catch (FidorClientException e) {
			throw new ServiceInfrastructureException(e);
		}
	}

	@Override
	public Integer getNoOfsCommuneByEgid(int egid, RegDate date) throws ServiceInfrastructureException {

		try {
			final CommuneFiscale commune = fidorClient.getCommuneParBatiment(egid, date);
			if (commune == null) {
				return null;
			}

			return commune.getNumeroOfs();
		}
		catch (FidorClientException e) {
			throw new ServiceInfrastructureException(e);
		}
	}

	@Override
	public Commune getCommuneByLocalite(Localite localite) throws ServiceInfrastructureException {
		throw new NotImplementedException("Pas encore implémenté dans Fidor");
	}

	@Override
	public List<OfficeImpot> getOfficesImpot() throws ServiceInfrastructureException {
		throw new NotImplementedException("Pas encore implémenté dans Fidor");
	}

	@Override
	public List<CollectiviteAdministrative> getCollectivitesAdministratives() throws ServiceInfrastructureException {
		throw new NotImplementedException("Pas encore implémenté dans Fidor");
	}

	@Override
	public List<CollectiviteAdministrative> getCollectivitesAdministratives(List<EnumTypeCollectivite> typesCollectivite) throws ServiceInfrastructureException {
		throw new NotImplementedException("Pas encore implémenté dans Fidor");
	}

	@Override
	public InstitutionFinanciere getInstitutionFinanciere(int id) throws ServiceInfrastructureException {
		throw new NotImplementedException("Pas encore implémenté dans Fidor");
	}

	@Override
	public List<InstitutionFinanciere> getInstitutionsFinancieres(String noClearing) throws ServiceInfrastructureException {
		throw new NotImplementedException("Pas encore implémenté dans Fidor");
	}

	@Override
	public Localite getLocaliteByNPA(int npa) throws ServiceInfrastructureException {
		throw new NotImplementedException("Pas encore implémenté dans Fidor");
	}

	@Override
	public List<TypeRegimeFiscal> getTypesRegimesFiscaux() throws ServiceInfrastructureException {
		throw new NotImplementedException("Pas encore implémenté dans Fidor");
	}

	@Override
	public TypeRegimeFiscal getTypeRegimeFiscal(String code) throws ServiceInfrastructureException {
		throw new NotImplementedException("Pas encore implémenté dans Fidor");
	}

	@Override
	public List<TypeEtatPM> getTypesEtatsPM() throws ServiceInfrastructureException {
		throw new NotImplementedException("Pas encore implémenté dans Fidor");
	}

	@Override
	public TypeEtatPM getTypeEtatPM(String code) throws ServiceInfrastructureException {
		throw new NotImplementedException("Pas encore implémenté dans Fidor");
	}

	@Override
	public String getUrlVers(ApplicationFiscale application, Long tiersId, Integer oid) {
		if (urlsApplication == null) {
			urlsStats.addMiss();
			initUrls();
		}
		else {
			urlsStats.addHit();
		}
		if (urlsApplication == null) {
			return null;
		}
		final String url = urlsApplication.get(application);
		return resolve(url, tiersId, oid);
	}

	private static String resolve(String url, Long numero, Integer oid) {
		if (url == null) {
			return null;
		}
		Assert.notNull(numero);
		Assert.notNull(oid);
		return url.replaceAll("\\{NOCTB\\}", numero.toString()).replaceAll("\\{OID\\}", oid.toString());
	}

	/**
	 * Initialise les URLs des applications fiscales.
	 * <p/>
	 * <b>Note:</b> il est absolument nécessaire d'initialiser le client <i>après</i> le contexte Spring, car il y a une dépendence implicite sur le bus CXF qui risque d'être initialisé plus tard que ce
	 * bean. Dans ce dernier, cas on reçoit une NPE dans le constructeur du service.
	 */
	private void initUrls() {
		final long now = System.nanoTime();
		if (lastTentative > 0 && lastTentative + fiveMinutes > now) {
			// on attend cinq minutes avant d'essayer de recontacter FiDoR, pour éviter de remplir les logs pour rien
			return;
		}
		synchronized (this) {
			try {
				if (urlsApplication == null) {
					final String patternTaoPP = getUrl("TAOPP", "synthese");
					final String patternTaoBA = getUrl("TAOBA", "dossier");
					final String patternTaoIS = getUrl("TAOIS", "default");
					final String patternSipf = getUrl("SIPF", "explorer");
					final String patternDPerm = getUrl("REPELEC", "contribuable");

					final Map<ApplicationFiscale, String> map = new EnumMap<>(ApplicationFiscale.class);
					map.put(ApplicationFiscale.TAO_PP, patternTaoPP);
					map.put(ApplicationFiscale.TAO_BA, patternTaoBA);
					map.put(ApplicationFiscale.TAO_IS, patternTaoIS);
					map.put(ApplicationFiscale.SIPF, patternSipf); // [UNIREG-2409]
					map.put(ApplicationFiscale.DPERM, patternDPerm);
					LOGGER.info("URLs externes (FiDoR) :\n" +
							" * TAOPP = " + patternTaoPP + '\n' +
							" * TAOBA = " + patternTaoBA + '\n' +
							" * TAOIS = " + patternTaoIS + '\n' +
							" * SIPF = " + patternSipf + '\n' +
							" * DPERM = " + patternDPerm);

					urlsApplication = map;
				}
			}
			catch (Exception e) {
				LOGGER.error("Impossible de contacter FiDoR : allez lui donner un coup de pied !");
				lastTentative = now;
			}
		}
	}

	private String getUrl(String app, String target) {
		final String url = fidorClient.getUrl(app, "INTERNE", target, null);
		if (url == null) {
			LOGGER.error(String.format("Il manque l'url d'accès à %s (target %s) dans FiDoR !", app, target));
		}
		return url;
	}

	@Override
	public Logiciel getLogiciel(Long id) throws ServiceInfrastructureException {
		if (id == null) {
			return null;
		}
		try {
			return LogicielImpl.get(fidorClient.getLogicielDetail(id));
		}
		catch (FidorClientException e) {
			throw new ServiceInfrastructureException(e);
		}
	}

	@Override
	public List<Logiciel> getTousLesLogiciels() throws ServiceInfrastructureException {
		try {
			final List<ch.vd.evd0012.v1.Logiciel> list = fidorClient.getTousLesLogiciels();
			if (list == null || list.isEmpty()) {
				return Collections.emptyList();
			}
			else {
				final List<Logiciel> logiciels = new ArrayList<>();
				for (ch.vd.evd0012.v1.Logiciel logicielFidor : list) {
					logiciels.add(LogicielImpl.get(logicielFidor));
				}
				return Collections.unmodifiableList(logiciels);
			}
		}
		catch (FidorClientException e) {
			throw new ServiceInfrastructureException(e.getMessage(), e);
		}
	}

	@Override
	public District getDistrict(int code) {
		try {
			return DistrictImpl.get(fidorClient.getDistrict(code));
		}
		catch (FidorClientException e) {
			throw new ServiceInfrastructureException(e);
		}
	}

	@Override
	public Region getRegion(int code) {
		try {
			return RegionImpl.get(fidorClient.getRegion(code));
		}
		catch (FidorClientException e) {
			throw new ServiceInfrastructureException(e);
		}
	}

	@Override
	public void ping() throws ServiceInfrastructureException {
		try {
			// appel vers la Suisse...
			fidorClient.getPaysDetail(ServiceInfrastructureRaw.noOfsSuisse, null);
		}
		catch (Exception e) {
			throw new ServiceInfrastructureException(e);
		}
	}
}
