package ch.vd.unireg.interfaces.infra.fidor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.evd0007.v1.Country;
import ch.vd.evd0007.v1.ExtendedCanton;
import ch.vd.evd0012.v1.CommuneFiscale;
import ch.vd.fidor.xml.impotspecial.v1.ImpotSpecial;
import ch.vd.fidor.xml.post.v1.PostalLocality;
import ch.vd.fidor.xml.post.v1.Street;
import ch.vd.fidor.xml.regimefiscal.v1.RegimeFiscal;
import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.NotImplementedException;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureException;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureRaw;
import ch.vd.unireg.interfaces.infra.data.ApplicationFiscale;
import ch.vd.unireg.interfaces.infra.data.Canton;
import ch.vd.unireg.interfaces.infra.data.CantonImpl;
import ch.vd.unireg.interfaces.infra.data.CollectiviteAdministrative;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.interfaces.infra.data.CommuneImpl;
import ch.vd.unireg.interfaces.infra.data.District;
import ch.vd.unireg.interfaces.infra.data.DistrictImpl;
import ch.vd.unireg.interfaces.infra.data.GenreImpotMandataire;
import ch.vd.unireg.interfaces.infra.data.GenreImpotMandataireImpl;
import ch.vd.unireg.interfaces.infra.data.InstitutionFinanciere;
import ch.vd.unireg.interfaces.infra.data.Localite;
import ch.vd.unireg.interfaces.infra.data.LocaliteImpl;
import ch.vd.unireg.interfaces.infra.data.Logiciel;
import ch.vd.unireg.interfaces.infra.data.LogicielImpl;
import ch.vd.unireg.interfaces.infra.data.OfficeImpot;
import ch.vd.unireg.interfaces.infra.data.Pays;
import ch.vd.unireg.interfaces.infra.data.PaysImpl;
import ch.vd.unireg.interfaces.infra.data.Region;
import ch.vd.unireg.interfaces.infra.data.RegionImpl;
import ch.vd.unireg.interfaces.infra.data.Rue;
import ch.vd.unireg.interfaces.infra.data.RueImpl;
import ch.vd.unireg.interfaces.infra.data.TypeCollectivite;
import ch.vd.unireg.interfaces.infra.data.TypeRegimeFiscal;
import ch.vd.unireg.interfaces.infra.data.TypeRegimeFiscalImpl;
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

	public void setFidorClient(FidorClient fidorClient) {
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
				Collections.sort(pays, new DateRangeComparator<>());
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
		try {
			final List<ExtendedCanton> cantons = fidorClient.getTousLesCantons();
			if (cantons != null && !cantons.isEmpty()) {
				final List<Canton> list = new ArrayList<>(cantons.size());
				for (ExtendedCanton c : cantons) {
					list.add(CantonImpl.get(c));
				}
				return list;
			}
			else {
				return Collections.emptyList();
			}
		}
		catch (FidorClientException e) {
			throw new ServiceInfrastructureException(e);
		}
	}

	@Override
	public List<Commune> getListeCommunes(Canton canton) throws ServiceInfrastructureException {
		try {
			final List<CommuneFiscale> list = fidorClient.getCommunesParCanton(canton.getNoOFS(), null);
			if (list == null || list.isEmpty()) {
				return Collections.emptyList();
			}

			final List<Commune> communes = new ArrayList<>(list.size());
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

			final List<Commune> communes = new LinkedList<>();
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

	private static Map<Integer, List<Commune>> buildHistoMap(List<Commune> liste) {
		final Map<Integer, List<Commune>> map = new HashMap<>(liste.size());

		// rassemblement par numéro OFS
		for (Commune commune : liste) {
			final List<Commune> slot = map.computeIfAbsent(commune.getNoOFS(), k -> new ArrayList<>());
			slot.add(commune);
		}

		// c'est fini
		return map;
	}

	@Override
	public List<Localite> getLocalites() throws ServiceInfrastructureException {
		try {
			final List<PostalLocality> fidorLocalites = fidorClient.getLocalitesPostales(null, null, null, null, null);
			if (fidorLocalites != null) {
				final Map<Integer, List<Commune>> map = buildHistoMap(getCommunes());
				final List<Localite> localites = new ArrayList<>(fidorLocalites.size());
				for (PostalLocality fidorLocalite : fidorLocalites) {
					localites.add(LocaliteImpl.get(fidorLocalite, map));
				}
				return localites;
			}
			return Collections.emptyList();
		}
		catch (FidorClientException e) {
			throw new ServiceInfrastructureException(e);
		}
	}

	@Override
	public List<Localite> getLocalitesByONRP(int onrp) throws ServiceInfrastructureException {
		try {
			final List<PostalLocality> fidorLocalites = fidorClient.getLocalitesPostalesHisto(onrp);
			if (fidorLocalites != null && !fidorLocalites.isEmpty()) {
				final Map<Integer, List<Commune>> map = buildHistoMap(getCommunes());
				final List<Localite> localites = new ArrayList<>(fidorLocalites.size());
				for (PostalLocality fidorLocalite : fidorLocalites) {
					localites.add(LocaliteImpl.get(fidorLocalite, map));
				}
				Collections.sort(localites, new DateRangeComparator<>());
				return localites;
			}
			else {
				return Collections.emptyList();
			}
		}
		catch (FidorClientException e) {
			throw new ServiceInfrastructureException(e);
		}
	}

	@Override
	public List<Rue> getRues(Localite localite) throws ServiceInfrastructureException {
		try {
			final List<Street> streets = fidorClient.getRuesParNumeroOrdrePosteEtDate(localite.getNoOrdre(), localite.getDateFin());
			if (streets != null && !streets.isEmpty()) {
				final List<Rue> rues = new ArrayList<>(streets.size());
				for (Street st : streets) {
					rues.add(RueImpl.get(st));
				}
				return rues;
			}
			else {
				return Collections.emptyList();
			}
		}
		catch (FidorClientException e) {
			throw new ServiceInfrastructureException(e);
		}
	}

	@Override
	public List<Rue> getRuesHisto(int numero) throws ServiceInfrastructureException {
		try {
			final List<Street> streets = fidorClient.getRuesParEstrid(numero, null);
			if (streets != null && !streets.isEmpty()) {
				final List<Rue> rues = new ArrayList<>(streets.size());
				for (Street st : streets) {
					rues.add(RueImpl.get(st));
				}
				return rues;
			}
			else {
				return Collections.emptyList();
			}
		}
		catch (FidorClientException e) {
			throw new ServiceInfrastructureException(e);
		}
	}

	@Override
	public Rue getRueByNumero(int numero, RegDate date) throws ServiceInfrastructureException {
		try {
			final RegDate refDate = date != null ? date : RegDate.get();
			final List<Street> streets = fidorClient.getRuesParEstrid(numero, refDate);
			if (streets == null || streets.isEmpty()) {
				return null;
			}
			if (streets.size() == 1) {
				return RueImpl.get(streets.get(0));
			}
			throw new ServiceInfrastructureException("Plusieurs rues retournée par FiDoR à un appel par estrid (" + numero + ") et date (" + date + ")");
		}
		catch (FidorClientException e) {
			throw new ServiceInfrastructureException(e);
		}
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
		return localite.getCommuneLocalite();
	}

	@Nullable
	@Override
	public Commune findCommuneByNomOfficiel(@NotNull String nomOfficiel, boolean includeFaitieres, boolean includeFractions, @Nullable RegDate date) throws ServiceInfrastructureException {
		try {

			final List<CommuneFiscale> communes = fidorClient.findCommuneByNomOfficiel(nomOfficiel, date);
			if (communes.isEmpty()) {
				return null;
			}

			// on filtre les communes faîtières/fractions si nécessaire
			final List<CommuneFiscale> communesFiltrees = communes.stream()
					.filter(c -> filterCommune(c, includeFaitieres, includeFractions))
					.collect(Collectors.toList());

			if (communesFiltrees.size() > 1) {
				throw new ServiceInfrastructureException("Plusieurs communes (" + communesFiltrees.size() + ") avec le nom [" + nomOfficiel + "] ont été trouvées.");
			}

			if (communesFiltrees.isEmpty()) {
				return null;
			}

			return CommuneImpl.get(communesFiltrees.get(0));
		}
		catch (FidorClientException e) {
			throw new ServiceInfrastructureException(e);
		}
	}

	private static boolean filterCommune(@NotNull CommuneFiscale c, boolean includeFaitieres, boolean includeFractions) {
		if (!c.isEstUneCommuneFaitiere() && !c.isEstUneFractionDeCommune()) {
			// la commune n'est pas du tout fractionnée, on la garde dans tous les cas
			return true;
		}
		else if (includeFaitieres && c.isEstUneCommuneFaitiere()) {
			// c'est une faîtière et on veut les faîtières
			return true;
		}
		else //noinspection RedundantIfStatement
			if (includeFractions && c.isEstUneFractionDeCommune()) {
			// c'est une fraction et on veut les fractions
			return true;
		}
		else {
			// dans tous les autres cas, on ne veut pas la commune
			return false;
		}
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
	public List<CollectiviteAdministrative> getCollectivitesAdministratives(List<TypeCollectivite> typesCollectivite) throws ServiceInfrastructureException {
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
	public List<Localite> getLocalitesByNPA(int npa, RegDate dateReference) throws ServiceInfrastructureException {
		try {
			final List<PostalLocality> postalLocalities = fidorClient.getLocalitesPostales(dateReference, npa, null, null, null);
			if (postalLocalities != null && !postalLocalities.isEmpty()) {
				final Map<Integer, List<Commune>> map = buildHistoMap(getCommunes());
				final List<Localite> localites = new ArrayList<>(postalLocalities.size());
				for (PostalLocality pl : postalLocalities) {
					localites.add(LocaliteImpl.get(pl, map));
				}
				return localites;
			}
			else {
				return Collections.emptyList();
			}
		}
		catch (FidorClientException e) {
			throw new ServiceInfrastructureException(e);
		}
	}

	private String getUrlApplication(ApplicationFiscale application) {
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
		return urlsApplication.get(application);
	}

	@Override
	public String getUrl(ApplicationFiscale application, @Nullable Map<String, String> parametres) {
		final String url = getUrlApplication(application);
		if (parametres == null) {
			return url;
		}
		else {
			return resolve(url, parametres);
		}
	}

	private static String resolve(String url, Map<String, String> replacements) {
		if (url == null) {
			return null;
		}

		final Pattern pattern = Pattern.compile("\\{([A-Za-z_][A-Za-z_0-9]*)\\}");
		final Matcher matcher = pattern.matcher(url);

		final StringBuilder b = new StringBuilder();
		int start = 0;
		while (matcher.find()) {
			final String varName = matcher.group(1);
			final String replacement = replacements.getOrDefault(varName, StringUtils.EMPTY);
			b.append(url.substring(start, matcher.start()));
			b.append(replacement);
			start = matcher.end();
		}

		b.append(url.substring(start, url.length()));
		return b.toString();
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
					final String patternTaoISDebiteur = getUrl("TAOIS", "debiteur");
					final String patternTaoPM = getUrl("TAOPM", "synthese");
					final String patternTaoICIIFONC = getUrl("TAOICIIFONC", "synthese");
					final String patternSipf = getUrl("SIPF", "explorer");
					final String patternDPerm = getUrl("REPELEC", "contribuable");
					final String patternDPermDoc = getUrl("REPELEC", "dossierDoc");
					final String patternCapitastra = getUrl("CAPITASTRA", "immeuble");

					final Map<ApplicationFiscale, String> map = new EnumMap<>(ApplicationFiscale.class);
					map.put(ApplicationFiscale.TAO_PP, patternTaoPP);
					map.put(ApplicationFiscale.TAO_BA, patternTaoBA);
					map.put(ApplicationFiscale.TAO_IS, patternTaoIS);
					map.put(ApplicationFiscale.TAO_IS_DEBITEUR, patternTaoISDebiteur);
					map.put(ApplicationFiscale.TAO_PM, patternTaoPM);
					map.put(ApplicationFiscale.TAO_ICI_IFONC, patternTaoICIIFONC);
					map.put(ApplicationFiscale.SIPF, patternSipf); // [UNIREG-2409]
					map.put(ApplicationFiscale.DPERM, patternDPerm);
					map.put(ApplicationFiscale.DPERM_DOCUMENT, patternDPermDoc);
					map.put(ApplicationFiscale.CAPITASTRA, patternCapitastra);

					LOGGER.info(map.entrySet().stream()
							.map(entry -> String.format(" * %s = %s", entry.getKey(), entry.getValue()))
							.collect(Collectors.joining("\n", "URLs externes (FiDoR) :\n", StringUtils.EMPTY)));

					urlsApplication = map;
				}
			}
			catch (Exception e) {
				LOGGER.error("Impossible de contacter FiDoR : allez lui donner un coup de pied !", e);
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
				final List<Logiciel> logiciels = new ArrayList<>(list.size());
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
	public List<TypeRegimeFiscal> getTousLesRegimesFiscaux() {
		try {
			final List<RegimeFiscal> liste = fidorClient.getRegimesFiscaux();
			if (liste == null || liste.isEmpty()) {
				return Collections.emptyList();
			}

			final List<TypeRegimeFiscal> regimes = new ArrayList<>(liste.size());
			for (RegimeFiscal regime : liste) {
				regimes.add(TypeRegimeFiscalImpl.get(regime));
			}
			return Collections.unmodifiableList(regimes);
		}
		catch (FidorClientException e) {
			throw new ServiceInfrastructureException(e);
		}
	}

	@Override
	public List<GenreImpotMandataire> getTousLesGenresImpotMandataires() {
		try {
			final List<ImpotSpecial> liste = fidorClient.getImpotsSpeciaux();
			if (liste == null || liste.isEmpty()) {
				return Collections.emptyList();
			}

			final List<GenreImpotMandataire> genres = new ArrayList<>(liste.size());
			for (ImpotSpecial type : liste) {
				genres.add(GenreImpotMandataireImpl.get(type));
			}
			return Collections.unmodifiableList(genres);
		}
		catch (FidorClientException e) {
			throw new ServiceInfrastructureException(e);
		}
	}

	@Override
	public void ping() throws ServiceInfrastructureException {
		try {
			fidorClient.ping();
		}
		catch (Exception e) {
			throw new ServiceInfrastructureException(e);
		}
	}
}
