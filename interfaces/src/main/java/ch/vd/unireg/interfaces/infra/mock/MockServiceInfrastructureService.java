package ch.vd.unireg.interfaces.infra.mock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.NotImplementedException;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureException;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureRaw;
import ch.vd.unireg.interfaces.infra.data.ApplicationFiscale;
import ch.vd.unireg.interfaces.infra.data.Canton;
import ch.vd.unireg.interfaces.infra.data.CollectiviteAdministrative;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.interfaces.infra.data.District;
import ch.vd.unireg.interfaces.infra.data.GenreImpotMandataire;
import ch.vd.unireg.interfaces.infra.data.Localite;
import ch.vd.unireg.interfaces.infra.data.Logiciel;
import ch.vd.unireg.interfaces.infra.data.OfficeImpot;
import ch.vd.unireg.interfaces.infra.data.Pays;
import ch.vd.unireg.interfaces.infra.data.Region;
import ch.vd.unireg.interfaces.infra.data.Rue;
import ch.vd.unireg.interfaces.infra.data.TypeCollectivite;
import ch.vd.unireg.interfaces.infra.data.TypeRegimeFiscal;

/**
 * Mock du Host Infrastructure Service.
 * <p/>
 * Pour utiliser cette classe, le plus simple est d'instancier une classe anonyme et d'implémenter la méthode init() de manière à charger les données de test voulues.
 * <p/>
 * Par exemple:
 * <p/>
 * <pre>
 *  ServiceInfrastructureService service = new MockServiceInfrastructureService() {
 *  protected void init() {
 *  // Pays
 *  pays.add(MockPays.Suisse);
 *  pays.add(MockPays.France);
 *  // Cantons
 *  cantons.add(MockCanton.Vaud);
 *  cantons.add(MockCanton.Geneve);
 *  ...
 *  }
 *  };
 * </pre>
 */
public abstract class MockServiceInfrastructureService implements ServiceInfrastructureRaw {

	// private static final Logger LOGGER = LoggerFactory.getLogger(MockServiceInfrastructureService.class);

	protected final List<Pays> pays = new ArrayList<>();
	protected final List<Canton> cantons = new ArrayList<>();
	protected final List<Localite> localites = new ArrayList<>();
	protected final List<Commune> communesVaud = new ArrayList<>();
	protected final List<Commune> communesHorsCanton = new ArrayList<>();
	protected final List<Commune> communes = new ArrayList<>();
	protected final List<Rue> rues = new ArrayList<>();
	protected final Map<Integer, CollectiviteAdministrative> collectivitesAdministrative = new HashMap<>();
	protected final List<OfficeImpot> officesImpot = new ArrayList<>();
	protected Map<Integer, List<MockLienCommuneBatiment>> batimentsParEgid = null;

	public MockServiceInfrastructureService() {
		init();
	}

	protected abstract void init();

	protected void copyFrom(MockServiceInfrastructureService right) {
		this.pays.addAll(right.pays);
		this.cantons.addAll(right.cantons);
		this.localites.addAll(right.localites);
		this.communesVaud.addAll(right.communesVaud);
		this.communesHorsCanton.addAll(right.communesHorsCanton);
		this.communes.addAll(right.communes);
		this.rues.addAll(right.rues);
		for (Map.Entry<Integer, CollectiviteAdministrative> entry : right.collectivitesAdministrative.entrySet()) {
			add((MockCollectiviteAdministrative) entry.getValue());
		}
	}

	protected void add(MockLocalite loc) {
		localites.add(loc);
	}

	protected void add(MockCanton c) {
		cantons.add(c);
	}

	protected void add(MockPays p) {
		pays.add(p);
	}

	protected void add(MockRue r) {
		rues.add(r);
	}

	protected void add(MockCommune c) {
		if (c.isVaudoise()) {
			communesVaud.add(c);
		}
		else {
			communesHorsCanton.add(c);
		}
		communes.add(c);
	}

	private Map<Integer, List<MockLienCommuneBatiment>> getBatimentsParEgid() {
		initBatiments();
		return batimentsParEgid;
	}

	private void initBatiments() {
		if (batimentsParEgid == null) {
			loadBatiments();
		}
	}

	private synchronized void loadBatiments() {
		if (batimentsParEgid == null) {
			final HashMap<Integer, List<MockLienCommuneBatiment>> map = new HashMap<>();
			for (Commune c : communes) {
				addLiensBatiments(map, (MockCommune) c);
			}
			batimentsParEgid = map;
		}
	}

	protected void addLiensBatiments(Map<Integer, List<MockLienCommuneBatiment>> map, MockCommune c) {
		for (MockLienCommuneBatiment lien : c.getLiensBatiments()) {
			final Integer egid = lien.getBatiment().getEgid();
			final List<MockLienCommuneBatiment> list = map.computeIfAbsent(egid, k -> new ArrayList<>());
			list.add(lien);
		}
	}

	protected void add(MockCollectiviteAdministrative coladm) {
		final Integer noColAdm = coladm.getNoColAdm();
		if (collectivitesAdministrative.containsKey(noColAdm)) {
			throw new RuntimeException("La collectivité avec le numéro [" + noColAdm + "] existe déjà.");
		}
		collectivitesAdministrative.put(noColAdm, coladm);
		if (coladm instanceof OfficeImpot) {
			officesImpot.add((OfficeImpot) coladm);
		}
	}


	@Override
	public List<Canton> getAllCantons() throws ServiceInfrastructureException {
		return cantons;
	}

	public List<Commune> getCommunesDeVaud() throws ServiceInfrastructureException {
		return communesVaud;
	}

	public List<Commune> getCommunesHorsCanton() throws ServiceInfrastructureException {
		return communesHorsCanton;
	}

	@Override
	public List<Commune> getCommunes() throws ServiceInfrastructureException {
		return communes;
	}

	@Override
	public List<Commune> getListeCommunes(Canton canton) throws ServiceInfrastructureException {
		return communesVaud;
	}

	@Override
	public List<Commune> getCommunesVD() throws ServiceInfrastructureException {
		return communesVaud;
	}

	@Override
	public List<Commune> getListeCommunesFaitieres() throws ServiceInfrastructureException {
		throw new NotImplementedException();
	}

	public List<Commune> getListeCommunes(String cantonAsString) throws ServiceInfrastructureException {
		if ("VD".equals(cantonAsString))
			return communesVaud;
		else
			return communesHorsCanton;
	}

	@Override
	public List<CollectiviteAdministrative> findCollectivitesAdministratives(List<Integer> codeCollectivites, boolean inactif) {
		throw new org.apache.commons.lang3.NotImplementedException("findCollectivitesAdministratives");
	}

	@Nullable
	@Override
	public Commune findCommuneByNomOfficiel(@NotNull String nomOfficiel, boolean includeFaitieres, boolean includeFractions, @Nullable RegDate date) throws ServiceInfrastructureException {
		return communes.stream()
				.filter(c -> c.getNomOfficiel().equals(nomOfficiel))
				.filter(c -> c.isValidAt(date))
				.findFirst()
				.orElse(null);
	}

	@Override
	public List<Localite> getLocalites() throws ServiceInfrastructureException {
		return localites;
	}

	@Override
	public List<Localite> getLocalitesByONRP(int onrp) throws ServiceInfrastructureException {
		Localite localite = null;
		for (Localite loc : getLocalites()) {
			if (loc.getNoOrdre() == onrp) {
				localite = loc;
				break;
			}
		}
		// TODO le jour où on introduira la notion d'historique dans le Mock, il ne faudra pas oublier de trier les valeurs ici...
		return localite == null ? Collections.emptyList() : Collections.singletonList(localite);
	}

	@Override
	public Localite getLocaliteByONRP(int onrp, RegDate dateReference) throws ServiceInfrastructureException {
		for (Localite loc : getLocalites()) {
			if (loc.getNoOrdre() == onrp) {
				return loc;
			}
		}
		return null;
	}

	@Override
	public List<Localite> getLocalitesByNPA(int npa, RegDate dateReference) throws ServiceInfrastructureException {
		final List<Localite> localites = new ArrayList<>();
		for (Localite loc : getLocalites()) {
			if (loc.getNPA() == npa && loc.isValidAt(dateReference)) {
				localites.add(loc);
			}
		}
		return localites;
	}

	@Override
	public List<Pays> getPays() throws ServiceInfrastructureException {
		return pays;
	}

	@Override
	public Pays getPays(int numeroOFS, @Nullable RegDate date) throws ServiceInfrastructureException {
		for (Pays p : pays) {
			if (p.getNoOFS() == numeroOFS) {
				return p;
			}
		}
		return null;
	}

	@Override
	public List<Pays> getPaysHisto(int numeroOFS) throws ServiceInfrastructureException {
		final List<Pays> histo = new ArrayList<>();
		for (Pays p : pays) {
			if (p.getNoOFS() == numeroOFS) {
				histo.add(p);
			}
		}
		return histo.isEmpty() ? Collections.emptyList() : histo;
	}

	@Override
	public Pays getPays(@NotNull String codePays, @Nullable RegDate date) throws ServiceInfrastructureException {
		for (Pays p : pays) {
			if (p.getCodeIso2() != null && p.getCodeIso2().equals(codePays)) {
				return p;
			}
		}
		return null;
	}

	@Override
	public List<Rue> getRues(Localite localite) throws ServiceInfrastructureException {
		List<Rue> locRues = new ArrayList<>();
		for (Rue r : rues) {
			if (r.getNoLocalite().equals(localite.getNoOrdre())) {
				locRues.add(r);
			}
		}
		return locRues;
	}

	@Override
	public List<Rue> getRuesHisto(int numero) throws ServiceInfrastructureException {
		List<Rue> locRues = new ArrayList<>();
		for (Rue r : rues) {
			if (r.getNoRue().equals(numero)) {
				locRues.add(r);
			}
		}
		return locRues;
	}

	@Override
	public Rue getRueByNumero(int numero, RegDate date) throws ServiceInfrastructureException {
		Rue rue = null;
		for (Rue r : rues) {
			if (r.getNoRue().equals(numero)) {
				rue = r;
			}
		}
		return rue;
	}

	public Pays getSuisse() throws ServiceInfrastructureException {
		return MockPays.Suisse;
	}

	public Canton getVaud() throws ServiceInfrastructureException {
		return MockCanton.Vaud;
	}

	public CollectiviteAdministrative getACI() throws ServiceInfrastructureException {
		return MockCollectiviteAdministrative.ACI;
	}

	public CollectiviteAdministrative getACIImpotSource() throws ServiceInfrastructureException {
		return MockCollectiviteAdministrative.ACIIMPOTSOURCE;
	}

	public CollectiviteAdministrative getACINouvelleEntite() throws ServiceInfrastructureException {
		return MockCollectiviteAdministrative.ACI_SECTION_DE_TAXATION;
	}

	public CollectiviteAdministrative getCEDI() throws ServiceInfrastructureException {
		return MockCollectiviteAdministrative.CEDI;
	}

	public CollectiviteAdministrative getCAT() throws ServiceInfrastructureException {
		return MockCollectiviteAdministrative.CAT;
	}

	@Override
	public List<Commune> getCommuneHistoByNumeroOfs(int noOfsCommune) throws ServiceInfrastructureException {
		final List<Commune> list = new ArrayList<>(2);
		for (Commune c : communesVaud) {
			int no = c.getNoOFS();
			if (no == noOfsCommune) {
				list.add(c);
			}
		}
		for (Commune c : communesHorsCanton) {
			if (c.getNoOFS() == noOfsCommune) {
				list.add(c);
			}
		}
		return list;
	}

	@Override
	public Commune getCommuneByLocalite(Localite localite) throws ServiceInfrastructureException {
		Commune commune = null;
		int numOrdreP = localite.getNoOrdre();
		int noCommune = localite.getNoCommune();
		if (numOrdreP == 540) {//1341 Orient -> fraction l'orient 8002
			noCommune = 8002;
		}
		else if (numOrdreP == 541) {//1346 les Bioux -> fraction les Bioux 8012
			noCommune = 8012;
		}
		else if (numOrdreP == 542) {//1344 l'Abbaye -> commune de l'Abbaye 5871
			noCommune = 5871;
		}
		else if (numOrdreP == 543) {//1342 le Pont -> commune de l'Abbaye 5871
			noCommune = 5871;
		}
		else if (numOrdreP == 546) {//1347 le Sentier -> fraction le Sentier 8000
			noCommune = 8000;
		}
		else if (numOrdreP == 547) {//1347 le Solliat -> commune Chenit 5872
			noCommune = 5872;
		}
		else if (numOrdreP == 550) {//1348 le Brassus -> fraction le Brassus 8001
			noCommune = 8001;
		}

		for (Commune c : communesVaud) {
			int no = c.getNoOFS();
			if (no == noCommune) {
				commune = c;
				break;
			}
		}
		if (commune == null) {
			for (Commune c : communesHorsCanton) {
				if (c.getNoOFS() == localite.getNoCommune()) {
					commune = c;
				}
			}
		}
		return commune;
	}

	@Override
	public Integer getNoOfsCommuneByEgid(int egid, RegDate date) throws ServiceInfrastructureException {
		final List<MockLienCommuneBatiment> liens = getBatimentsParEgid().get(egid);
		if (liens == null || liens.isEmpty()) {
			return null;
		}

		MockCommune commune = null;
		for (MockLienCommuneBatiment lien : liens) {
			if (lien.isValidAt(date)) {
				if (commune != null) {
					throw new RuntimeException(
							"Le bâtiment avec l'egid [" + egid + "] est enregistré dans deux communes en même temps : numéro Ofs [" + commune.getNoOFS() + "] et numéro Ofs [" +
									lien.getCommune().getNoOFS() + ']');
				}
				commune = lien.getCommune();
			}
		}

		if (commune == null) {
			return null;
		}

		return commune.getNoOFS();
	}

	@Override
	public CollectiviteAdministrative getCollectivite(int noColAdm) throws ServiceInfrastructureException {
		return collectivitesAdministrative.get(noColAdm);
	}

	@Override
	public List<OfficeImpot> getOfficesImpot() throws ServiceInfrastructureException {
		return new ArrayList<>(officesImpot);
	}

	@Override
	public List<CollectiviteAdministrative> getCollectivitesAdministratives() throws ServiceInfrastructureException {
		return new ArrayList<>(collectivitesAdministrative.values());
	}

	@Override
	public List<CollectiviteAdministrative> getCollectivitesAdministratives(List<TypeCollectivite> typesCollectivite) throws ServiceInfrastructureException {

		final Set<String> sigles = new HashSet<>();
		for (TypeCollectivite e : typesCollectivite) {
			sigles.add(e.getCode());
		}

		final List<CollectiviteAdministrative> list = new ArrayList<>();
		for (CollectiviteAdministrative ca : collectivitesAdministrative.values()) {
			if (sigles.contains(ca.getSigle())) {
				list.add(ca);
			}
		}
		return list;
	}

	@Override
	public String getUrl(ApplicationFiscale application, @Nullable Map<String, String> parametres) {
		return null;
	}

	@Override
	public Logiciel getLogiciel(Long idLogiciel) throws ServiceInfrastructureException {
		return null;
	}

	@Override
	public List<Logiciel> getTousLesLogiciels() throws ServiceInfrastructureException {
		return null;
	}

	@Override
	public List<TypeRegimeFiscal> getTousLesRegimesFiscaux() {
		return Arrays.asList(MockTypeRegimeFiscal.ALL);
	}

	@Override
	public List<GenreImpotMandataire> getTousLesGenresImpotMandataires() {
		return Arrays.asList(MockGenreImpotMandataire.ALL);
	}

	@Override
	public District getDistrict(int code) {
		return null;
	}

	@Override
	public Region getRegion(int code) {
		return null;
	}

	@Override
	public void ping() throws ServiceInfrastructureException {
	}
}
